package com.td.stereotosurround.composer


interface IAudioListener {
    fun onSetBufferData(data: ShortArray, outputSamplingRate: Int, outputChannels: Int)

    fun onPrepared(inputSamplingRate: Int, inputChannels: Int)

    fun onFailed(msg: String?)

    fun onComplete()
}
