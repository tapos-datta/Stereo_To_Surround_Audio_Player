package com.td.stereotosurround

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.td.stereotosurround.composer.AudioComposer
import com.td.stereotosurround.composer.IAudioListener
import com.td.stereotosurround.util.Constants
import com.td.stereotosurround.util.CustomBuffer
import com.td.stereotosurround.util.PathUtil
import kotlin.math.sqrt

/**
 * Created by TAPOS DATTA on 02,August,2021
 */

class MainPresenter(var context: Context, var view: MainContract.View) : MainContract.Presenter,
    IAudioListener {


    private val uiHandler: Handler = Handler(Looper.getMainLooper())
    private val composer: AudioComposer = AudioComposer(context, this)
    private val flashBufferSize = Constants.DEFAULT_VISUALIZER_BUFFER_SIZE
    private var audioUri: Uri? = null
    private var rmsBuffer: CustomBuffer? = null

    private var isPlaying = false
    private var binSize: Int = 0
    private var pendingSquareSum: Float = 0f
    private var sampleCounter: Int = 0


    override fun onPlayAction() {
        if (!isPlaying) {
            isPlaying = true
            composer.start(audioUri!!)
        } else {
            isPlaying = false
            composer.stop()
        }
    }

    override fun setAudioUri(uri: Uri) {
        audioUri = uri
        view.updateTitleView(PathUtil.getDisplayName(context, uri))
    }

    override fun onSetBufferData(data: ShortArray, outputSamplingRate: Int, outputChannels: Int) {
        val totalData = data.size / outputChannels

        var ind = 0
        var sum = 0f
        while (ind < totalData) {
            var j = 0
            var value: Int = 0
            while (j < outputChannels) {
                value += (data[ind * outputChannels + j].toInt() + 32768)
                j++
            }
            ind++

            var x = ((value / outputChannels).toFloat())
            x /= 65535f //normalized data
            x *= x  // square value
            pendingSquareSum += x
            sampleCounter++
            if (sampleCounter == binSize) {
                val rms = sqrt(pendingSquareSum / sampleCounter)
                rmsBuffer!!.addSamples(FloatArray(1) { rms })
                sampleCounter = 0
                pendingSquareSum = 0f
            }
        }

        rmsBuffer?.run {
            if (getSize() >= flashBufferSize) {
                val values = rmsBuffer!!.getSamples(flashBufferSize)
                uiHandler.post {
                    view.visualize(values)
                }
            }
        }
    }

    override fun onPrepared(inputSamplingRate: Int, inputChannels: Int) {
        rmsBuffer?.clear()
        if (rmsBuffer == null) {
            rmsBuffer = CustomBuffer(inputSamplingRate)
        }
        pendingSquareSum = 0f
        sampleCounter = 0
        binSize = inputSamplingRate / Constants.DEFAULT_BIN_WIDTH
    }

    override fun onFailed(msg: String?) {
        msg?.let {
            uiHandler.post {
                view.onError(msg)
            }
        }
    }

    override fun onComplete() {
        uiHandler.post {
            view.visualize(FloatArray(flashBufferSize) { 0f })
        }
    }

}