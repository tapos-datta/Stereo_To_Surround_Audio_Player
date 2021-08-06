package com.td.stereotosurround.util

/**
 * Created by TAPOS DATTA on 04,August,2021
 */

class CustomBuffer(var capacity: Int) {

    private var bufferSize: Int = 0
    private val buffer = FloatArray(capacity)

    fun getSamples(sampleSize: Int): FloatArray {
        if (sampleSize > bufferSize || bufferSize < 1) {
            throw IndexOutOfBoundsException("Couldn't fetch sample data.")
        }
        val data = FloatArray(sampleSize)
        System.arraycopy(buffer, 0, data, 0, sampleSize)
        updateSampleArray(sampleSize)
        return data
    }

    private fun updateSampleArray(sampleSize: Int) {
        // remove fetched data from buffer
        // i.e. number of #sampleSize of data will be replaced by remaining data
        // by following FIFO approach
        val remainingDataSize: Int = bufferSize - sampleSize
        if (remainingDataSize > 0) {
            System.arraycopy(buffer, sampleSize, buffer, 0, remainingDataSize)
        }
        bufferSize -= sampleSize
    }

    fun addSamples(data: FloatArray?) {
        if (data == null) return
        if (bufferSize + data.size >= capacity) {
            throw IndexOutOfBoundsException("Sample size is not enough to add data.")
        }
        System.arraycopy(data, 0, buffer, bufferSize, data.size)
        bufferSize += data.size
    }

    fun getSize(): Int {
        return bufferSize
    }

    fun getSamples(dest: FloatArray, sampleSize: Int) {
        if (dest.size < sampleSize
            || sampleSize > bufferSize
        ) {
            throw IndexOutOfBoundsException("Destination is not capable to load samples.")
        }
        System.arraycopy(buffer, 0, dest, 0, sampleSize)
        updateSampleArray(sampleSize)
    }

    fun clear() {
        bufferSize = 0
    }

}