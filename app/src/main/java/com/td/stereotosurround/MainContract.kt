package com.td.stereotosurround

import android.net.Uri

/**
 * Created by TAPOS DATTA on 01,August,2021
 */

interface MainContract {

    interface View{
        fun visualize(data : FloatArray?)
        fun updateTitleView(title : String?)
        fun onError(msg:String)
        fun onPlayStart()
        fun onPlayStop()
    }

    interface Presenter{
        fun onPlayAction()
        fun setAudioUri(uri: Uri)
    }

}