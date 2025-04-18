package com.android.klaudiak.vosk_stt

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.android.klaudiak.audioplayer.AudioPlaybackListener
import com.android.klaudiak.audioplayer.presentation.AudioPlayerScreen
import com.android.klaudiak.audioplayer.presentation.AudioPlayerViewModel
import dagger.hilt.android.AndroidEntryPoint
import org.vosk.LibVosk
import org.vosk.LogLevel

@AndroidEntryPoint
class STTVoskActivity : ComponentActivity(), AudioPlaybackListener {

    private val viewModel: STTViewModel by viewModels()
    private val audioPlayerViewModel: AudioPlayerViewModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LibVosk.setLogLevel(LogLevel.INFO)

        setContent {
            val resultText by viewModel.resultText.collectAsState()

            SpeechToTextScreen(
                viewModel = audioPlayerViewModel,
                resultText = resultText,
                onRecognizeMic = { viewModel.toggleMicrophone() },
                isListening = viewModel.isListening.collectAsState().value
            )
        }

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            viewModel.requestPermission(this)
        } else {
            viewModel.initModel(this)
        }
    }

    override fun onNewAudioFileStarted(fileName: String) {
        Log.i(TAG, "New audio file started: $fileName")
        audioPlayerViewModel.updateFileName(fileName)
    }

    companion object {
        const val TAG = "vosk"
    }
}

@Composable
fun SpeechToTextScreen(
    viewModel: AudioPlayerViewModel,
    resultText: String,
    onRecognizeMic: () -> Unit,
    isListening: Boolean
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Speech to Text with Vosk",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(16.dp)
        )

        AudioPlayerScreen(viewModel)

        SelectionContainer(
            modifier = Modifier
                .heightIn(max = 400.dp)
                .verticalScroll(scrollState)
        ) {

            BasicText(text = resultText)
        }

        Spacer(modifier = Modifier.height(16.dp))


        Button(onClick = onRecognizeMic) {
            Text(if (isListening) "Stop" else "Recognize Mic")
        }
    }
}
