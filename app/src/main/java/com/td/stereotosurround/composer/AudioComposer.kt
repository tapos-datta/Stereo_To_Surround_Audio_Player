package com.td.stereotosurround.composer

import android.content.Context
import android.media.*
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import uk.me.berndporr.iirj.Butterworth
import java.lang.Exception
import java.lang.IllegalArgumentException
import java.lang.RuntimeException
import java.nio.ByteBuffer
import java.util.*
import kotlin.math.floor

/**
 * Created by TAPOS DATTA on 02,July,2021
 */

class AudioComposer(private var context: Context, private val listener: IAudioListener) {

    private companion object {
        val TIME_UNIT_MICRO: Long = 1000000
        val DRAIN_STATE_CONSUMED = -2
        val DRAIN_STATE_NONE = -1

        //create three low pass filter
        val lfeLowPass = Butterworth()
        val centerBandPass = Butterworth()
        val rearLowPass = Butterworth()
        var shiftedData = LinkedList<Float>() // delay create by shifting
    }

    private val intervalOfStepsUs: Long = 5000000 // 5 secs in micro-secs
    private var stopRequest: Boolean = false
    private var totalSample: Long = 0;
    private var inputSamplingRate: Int = 0
    private var shiftedPos: Int = 0
    private val bufferInfo: MediaCodec.BufferInfo = MediaCodec.BufferInfo()
    private var isDecoderEOS: Boolean = false
    private lateinit var decoder: MediaCodec
    private lateinit var player: AudioTrack
    private lateinit var extractor: MediaExtractor
    private var isExtractorEOS: Boolean = false
    private var inputChannels: Int = 2
    private var shiftCounter: Long = 0


    fun start(uri: Uri) {
        initialize(uri)
        CoroutineScope(Dispatchers.IO).launch {
            runPipeline()
        }
    }

    fun stop() {
        synchronized(this) {
            stopRequest = true
        }
    }

    private fun runPipeline() {
        isDecoderEOS = false
        stopRequest = false
        listener.onPrepared(inputSamplingRate, inputChannels)
        try {
            while (!isDecoderEOS && !stopRequest) {
                var status = false
                while (drainDecoder(0) != DRAIN_STATE_NONE)
                    status = true
                while (drainExtractor(0) != DRAIN_STATE_NONE)
                    status = true
            }

            if (player.state == AudioTrack.PLAYSTATE_PLAYING) {
                player.apply {
                    stop()
                    release()
                }
            }
        } catch (ex: Exception) {
            listener.onFailed(ex.message)
        } finally {
            decoder.stop()
            extractor.release()
            decoder.release()
            player.release()
        }
        listener.onComplete()
    }

    private fun drainDecoder(timeOutUs: Long): Int {
        if (isDecoderEOS) {
            return DRAIN_STATE_NONE
        }
        val result: Int = decoder.dequeueOutputBuffer(bufferInfo, timeOutUs)
        when (result) {
            MediaCodec.INFO_TRY_AGAIN_LATER -> return DRAIN_STATE_NONE
            MediaCodec.INFO_OUTPUT_FORMAT_CHANGED, MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> return DRAIN_STATE_CONSUMED
        }
        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
            //Here, task finished for current decoder ,need a update for next decoder
            isDecoderEOS = true
            bufferInfo.size = 0
        } else if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
            return DRAIN_STATE_CONSUMED
        } else {
            processBuffer(decoder.getOutputBuffer(result)!!, inputChannels, inputSamplingRate)
        }
        decoder.releaseOutputBuffer(result, false)

        return DRAIN_STATE_CONSUMED
    }

    private fun drainExtractor(timeOutUs: Long): Int {
        if (isExtractorEOS) return DRAIN_STATE_NONE
        val trackIndex: Int = extractor.sampleTrackIndex
        val result: Int = decoder.dequeueInputBuffer(timeOutUs)
        if (result < 0) return DRAIN_STATE_NONE
        if (trackIndex < 0) {
            isExtractorEOS = true
            decoder.queueInputBuffer(result, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            return DRAIN_STATE_NONE
        }
        val sampleSizeCompat: Int = extractor.readSampleData(decoder.getInputBuffer(result)!!, 0)
        decoder.queueInputBuffer(
            result,
            0,
            sampleSizeCompat,
            extractor.sampleTime,
            0
        )
        extractor.advance()
        return DRAIN_STATE_CONSUMED
    }

    private fun processBuffer(outputBuffer: ByteBuffer, inputChannels: Int, samplingRate: Int) {
        if (inputChannels != 2) {
            throw IllegalArgumentException("Can't process except stereo input")
        }

        val original = ShortArray((outputBuffer.limit() - outputBuffer.position()) / 2)
        outputBuffer.asShortBuffer().get(original)

        //A determiner of 7.1 which determines how much data will process for each channel by a factor of ranged 0.0 to 1.0
        //However, the LFE will not be affected by the determiner.
        val activeRatio = FloatArray(7) { 0.0f }

        totalSample += original.size    //total consumed data.
        generatePeriodicProgress(
            activeRatio,
            samplingRate,
            totalSample,
            intervalOfStepsUs,
            activeRatio.size
        )

        //convert stereo stream to 7.1 stream
        val shortData = ShortArray((original.size / inputChannels) * 8)
        for ((pos, i) in (shortData.indices step 8).withIndex()) {
            val left: Short = original[2 * pos]
            val right: Short = original[2 * pos + 1]
            val center = (mixSignal(left.toInt(), right.toInt()) * 0.7071068f)
            //FL
            shortData[i] = (left * 0.7071068f * activeRatio[1]).toInt().toShort()
            //FR
            shortData[i + 1] = (right * 0.7071068f * activeRatio[6]).toInt().toShort()
            //C
            shortData[i + 2] =
                centerBandPass.filter(center.toDouble() * activeRatio[0]).toInt().toShort()
            //LFE
            shortData[i + 3] = lfeLowPass.filter(center.toDouble()).toInt().toShort()
            //LS
            shortData[i + 4] = (left * activeRatio[2]).toInt().toShort()
            //RS
            shortData[i + 5] = (right * activeRatio[5]).toInt().toShort()

            shiftedData.add(center)
            shiftCounter++
            if (shiftCounter < shiftedPos) {
                continue
            } else {
                val value = mixSignal(
                    (left.toInt() - right.toInt()),
                    rearLowPass.filter(shiftedData.removeFirst().toDouble()).toInt()
                ).toDouble()

                //BL
                shortData[i + 6] =
                    rearLowPass.filter(value * activeRatio[3]).toInt().toShort()
                //BR
                shortData[i + 7] =
                    rearLowPass.filter(value * activeRatio[4]).toInt().toShort()
            }
        }
        player.write(shortData, 0, shortData.size)


        listener.onSetBufferData(original, samplingRate, 2)
    }

    private fun generatePeriodicProgress(
        outputRatio: FloatArray,
        samplingRate: Int,
        totalSample: Long,
        stepInterval: Long,
        steps: Int
    ) {
        if (steps > outputRatio.size)
            throw IllegalArgumentException("Steps should not greater than the size of output array.")

        //progress calculate of two adjacent indices in one step interval
        val half = stepInterval / 2.0f
        val curPst = (TIME_UNIT_MICRO * totalSample / (samplingRate * 2))
        val curPickPos = (curPst / half).toInt() % steps
        val nextPickPos = if (curPickPos == (steps - 1)) 0 else curPickPos + 1
        val flip = ((curPst / half).toInt() % 2) != 0
        var frac = (curPst % stepInterval).toFloat() / half
        frac = if (frac > 1.0) 2.0f - frac else frac


        for (i in outputRatio.indices) {
            when (i) {
                curPickPos -> outputRatio[i] = if (flip) frac else 1f - frac
                nextPickPos -> outputRatio[i] = if (flip) 1.0f - frac else frac
                else -> outputRatio[i] = 0.0f
            }
        }
    }

    private fun initialize(uri: Uri) {
        extractor = MediaExtractor().apply {
            setDataSource(context, uri, null)
        }
        val mediaFormat = getTrackFormat(extractor)
        extractor.seekTo(0, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
        inputChannels = mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        inputSamplingRate = mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)

        if (inputChannels != 2) throw RuntimeException("Input is not a stereo stream.!!")

        decoder = MediaCodec.createDecoderByType(mediaFormat.getString(MediaFormat.KEY_MIME)!!)
        decoder.configure(mediaFormat, null, null, 0)

        player = createAudioTrack(inputSamplingRate)

        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.mode = AudioManager.MODE_NORMAL

        player.play()
        decoder.start()

        totalSample = 0
        //position calcualate for 15ms time-delay
        shiftedPos = (inputSamplingRate / 1000 * 15.0).toInt()
        //design two low pass filter
        lfeLowPass.lowPass(2, inputSamplingRate.toDouble(), 200.0)  // emphasize  low  frequency
        rearLowPass.lowPass(2, inputSamplingRate.toDouble(), 7000.0) // high-frequency absorption
        // one band pass filter from 100Hz to 4000Hz
        centerBandPass.bandPass(2, inputSamplingRate.toDouble(), 1950.0, 1950.0)


    }

    private fun createAudioTrack(samplingRate: Int): AudioTrack {
        return AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(samplingRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_7POINT1_SURROUND)
                    .build()
            )
            .setBufferSizeInBytes(
                AudioTrack.getMinBufferSize(
                    samplingRate,
                    AudioFormat.CHANNEL_OUT_7POINT1_SURROUND,
                    AudioFormat.ENCODING_PCM_16BIT
                ) * 2
            )
            .build()
    }

    private fun getTrackFormat(extractor: MediaExtractor): MediaFormat {
        var format: MediaFormat? = null
        for (i in 0 until extractor.trackCount) {
            format = extractor.getTrackFormat(i)
            if (format.containsKey(MediaFormat.KEY_MIME)
                && format.getString(MediaFormat.KEY_MIME)!!.startsWith("audio/")
            ) {
                extractor.selectTrack(i)
                break
            }
        }
        return format!!
    }

    private fun mixSignal(signalA: Int, signalB: Int): Int {
        val outputSignal: Float
        val signalTemA: Float = (signalA + 32768f) / 65535f
        val signalTemB: Float = (signalB + 32768f) / 65535f
        outputSignal =
            if (signalTemA < 0.5 && signalTemB < 0.5) {
                2f * signalTemA * signalTemB
            } else {
                2f * (signalTemA + signalTemB) - 2f * signalTemA * signalTemB - 1f
            }
        return (floor((outputSignal * 65535f).toDouble()) - 32768f).toInt()
    }

}