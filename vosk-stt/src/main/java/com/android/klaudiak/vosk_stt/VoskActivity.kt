package com.android.klaudiak.vosk_stt

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.android.klaudiak.audioplayer.AudioPlaybackListener
import com.android.klaudiak.audioplayer.presentation.AudioPlayerViewModel
import com.android.klaudiak.vosk_stt.Providers.LocalAudioPlayerViewModelProvider
import com.android.klaudiak.vosk_stt.Providers.LocalVoskViewModelProvider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.vosk.LibVosk
import org.vosk.LogLevel

@AndroidEntryPoint
class VoskActivity : ComponentActivity(), AudioPlaybackListener {

    private val voskViewModel: VoskViewModel by viewModels()
    private val audioPlayerViewModel: AudioPlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LibVosk.setLogLevel(LogLevel.INFO)

        setContent {
            enableEdgeToEdge()

            CompositionLocalProvider(
                LocalVoskViewModelProvider provides voskViewModel,
                LocalAudioPlayerViewModelProvider provides audioPlayerViewModel
            ) {
                VoskScreen { finish() }
            }
        }

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            voskViewModel.requestPermission(this)
        } else {
            voskViewModel.initModel(this)
        }

        collectTimestamps()
    }

    override fun onNewAudioFileStarted(fileName: String) {
        Log.i(TAG, "New audio file started: $fileName")
        audioPlayerViewModel.updateFileName(fileName)
    }

    private fun collectTimestamps() {
        lifecycleScope.launch {
            voskViewModel.finalResultWithTimestamps.collect { timestamps ->
                if (timestamps.isNotEmpty()) {
                    timestamps.forEach { (word, timestamp) ->
                        Log.d("WordTimestamp", "Word: $word, Timestamp: $timestamp ms")
                    }
                }
            }
        }
    }

    companion object {
        const val TAG = "vosk"
    }
}
