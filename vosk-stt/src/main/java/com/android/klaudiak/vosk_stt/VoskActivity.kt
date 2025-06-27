package com.android.klaudiak.vosk_stt

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.android.klaudiak.audioplayer.AudioPlaybackListener
import com.android.klaudiak.audioplayer.presentation.AudioPlayerScreen
import com.android.klaudiak.audioplayer.presentation.AudioPlayerViewModel
import com.android.klaudiak.audioplayer.presentation.SaveTranscriptionContent
import com.android.klaudiak.audioplayer.presentation.SectionDividerWithText
import com.android.klaudiak.core.presentation.TranscriptionDisplay
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
            val resultText by voskViewModel.resultText.collectAsState()

            SpeechToTextScreen(
                audioPlayerViewModel = audioPlayerViewModel,
                voskViewModel = voskViewModel,
                resultText = resultText,
                isListening = voskViewModel.isRecording.collectAsState().value,
                // onRecognizeMic = { viewModel.toggleMicrophone({ audioPlayerViewModel.updateAudioFileTranslation(it) }) },
            )
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

@Composable
fun SpeechToTextScreen(
    audioPlayerViewModel: AudioPlayerViewModel,
    voskViewModel: VoskViewModel,
    resultText: String,
    isListening: Boolean
) {

    val isRecording by voskViewModel.isRecording.collectAsState()

    val scrollState = rememberScrollState()

    val isPlaybackComplete by audioPlayerViewModel.isPlaybackComplete.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(dimensionResource(R.dimen.padding_medium))
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.vosk_stt_title),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = MaterialTheme.typography.headlineLarge.fontWeight,
            modifier = Modifier.padding(bottom = dimensionResource(R.dimen.padding_medium))
        )

        AudioPlayerScreen(audioPlayerViewModel)


        SectionDividerWithText(text = stringResource(R.string.audio_transcription_title))


        TranscriptionDisplay(
            transcriptionText = resultText,
            isRecording = isRecording,
            onToggleRecording = {
                with(audioPlayerViewModel) {
                    voskViewModel.toggleMicrophone {
                        updateAudioFileTranslation(it)
                    }
                  /*  voskViewModel.toggleRecording(
                        updateTranscriptionText = {
                            updateTranscriptionText(it)
                        },
                        updateFileTranscriptionDuration = {
                            updateFileTranscriptionDuration(it)
                        },
                        updateAudioFileTranslation = {
                            updateAudioFileTranslation(it)
                        }
                    )*/
                }
            },
            modifier = Modifier
                .wrapContentHeight()
                .padding(horizontal = dimensionResource(R.dimen.padding_medium))
        )

        if (isPlaybackComplete) {
            SaveTranscriptionContent()
        }
    }
}
