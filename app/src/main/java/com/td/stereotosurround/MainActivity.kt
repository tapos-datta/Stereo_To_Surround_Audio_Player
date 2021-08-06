package com.td.stereotosurround

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.kbeanie.multipicker.api.AudioPicker
import com.kbeanie.multipicker.api.callbacks.AudioPickerCallback
import com.kbeanie.multipicker.api.entity.ChosenAudio
import com.td.stereotosurround.visualizer.BarVisualizer

class MainActivity : AppCompatActivity(), AudioPickerCallback, MainContract.View {

    private lateinit var chooser: Button
    private lateinit var canvas: BarVisualizer
    private lateinit var title: TextView
    private lateinit var presenter: MainContract.Presenter
    private var isPlayIconShow = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        presenter = MainPresenter(this, this)

        chooser = findViewById(R.id.pick_audio)
        title = findViewById(R.id.text_view)
        canvas = findViewById(R.id.drawing_view)
        canvas.hide()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                9777 -> {
                    presenter.setAudioUri(Uri.parse(data?.dataString))
                }
                else -> {
                }
            }
        }
    }

    override fun onAudiosChosen(p0: MutableList<ChosenAudio>?) {
        TODO("Not yet implemented")
    }

    fun onPickAudio(view: View) {
        val picker = AudioPicker(this)
        picker.setAudioPickerCallback(this)
        picker.pickAudio()
    }

    @SuppressLint("UseCompatLoadingForDrawables", "SetTextI18n")
    fun onPlayAction(view: View) {
        if (title.text.isEmpty()) {
            // no track selected
            return
        }

        (view as Button).apply {
            if (isPlayIconShow) {
                text = "Stop"
                isPlayIconShow = false
                chooser.isEnabled = isPlayIconShow
            } else {
                text = "play"
                isPlayIconShow = true
                chooser.isEnabled = isPlayIconShow
            }
        }
        presenter.onPlayAction()
    }

    override fun visualize(data: FloatArray?) {
        if (data != null)
            canvas.update(data)
        else
            canvas.hide()
    }

    override fun updateTitleView(titleText: String?) {
        title.text = titleText?.let { titleText }
        canvas.show()
    }

    override fun onError(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    override fun onPlayStart() {
        TODO("Not yet implemented")
    }

    override fun onPlayStop() {
        TODO("Not yet implemented")
    }


}