package com.android.klaudiak.sherpa_ncnn

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.android.klaudiak.audioplayer.AudioPlaybackListener
import com.android.klaudiak.audioplayer.presentation.AudioPlayerViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SherpaNcnnActivity : ComponentActivity(), AudioPlaybackListener {

    private val audioPlayerViewModel: AudioPlayerViewModel by viewModels()
    private val sherpaNcnnViewModel: SherpaNcnnViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SherpaNcnnScreen(
                audioPlayerViewModel,
                { sherpaNcnnViewModel.initModel(application = application) },
                { sherpaNcnnViewModel.toggleRecording(audioPlayerViewModel) },
                ::finish,
                { sherpaNcnnViewModel.updateModel(it) }
            )
        }

        audioPlayerViewModel.setPlaybackListener(this)
    }


    override fun onNewAudioFileStarted(fileName: String) {
        Log.i(TAG, "New audio file started: $fileName")
        audioPlayerViewModel.updateFileName(fileName)
    }

    companion object {
        const val TAG = "sherpa-ncnn"
    }
}
