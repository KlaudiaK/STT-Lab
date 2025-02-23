package com.android.klaudiak.vosk_stt

import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Telephony.Mms.Part.FILENAME
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
import com.android.klaudiak.audioplayer.AudioPlayerScreen
import com.android.klaudiak.audioplayer.AudioPlayerViewModel
import dagger.hilt.android.AndroidEntryPoint
import org.vosk.LibVosk
import org.vosk.LogLevel

@AndroidEntryPoint
class STTVoskActivity : ComponentActivity() {

    private val viewModel: STTViewModel by viewModels()
    private val audioPlayerViewModel: AudioPlayerViewModel by viewModels()

    private val testFilename = "10001-90210-01803.wav"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LibVosk.setLogLevel(LogLevel.INFO)

        setContent {
            val resultText by viewModel.resultText.collectAsState()

            SpeechToTextScreen(
                viewModel = audioPlayerViewModel,
                resultText = resultText,
                onRecognizeFile = { viewModel.recognizeFile(this, testFilename) },
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
}

@Composable
fun SpeechToTextScreen(
    viewModel: AudioPlayerViewModel,
    resultText: String,
    onRecognizeFile: () -> Unit,
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

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onRecognizeFile) {
                Text("Recognize File")
            }

            Button(onClick = onRecognizeMic) {
                Text(if (isListening) "Stop" else "Recognize Mic")
            }
        }
    }
}


/*
class STTVoskActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LibVosk.setLogLevel(LogLevel.INFO)
        setContent {
            SpeechToTextScreen()
        }
    }
}

@Composable
fun SpeechToTextScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var resultText by remember { mutableStateOf("Preparing...") }
    var isListening by remember { mutableStateOf(false) }
    var model by remember { mutableStateOf<Model?>(null) }
    var speechService by remember { mutableStateOf<SpeechService?>(null) }
    var speechStreamService by remember { mutableStateOf<SpeechStreamService?>(null) }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) {
                coroutineScope.launch(Dispatchers.IO) {
                    StorageService.unpack(context, "model-en-us", "model",
                        { loadedModel -> model = loadedModel },
                        { exception -> resultText = "Error loading model: ${exception.message}" }
                    )
                }
            } else {
                resultText = "Permission denied"
            }
        }
    )

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
        } else {
            coroutineScope.launch(Dispatchers.IO) {
                StorageService.unpack(context, "model-en-us", "model",
                    { loadedModel -> model = loadedModel },
                    { exception -> resultText = "Error loading model: ${exception.message}" }
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Speech to Text", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))

        SelectionContainer {
            BasicText(text = resultText)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { recognizeFile(model, speechStreamService, resultText, context) }) {
                Text("Recognize File")
            }

            Button(onClick = {
                isListening = !isListening
                if (isListening) {
                    recognizeMicrophone(model, speechService, resultText) {
                        isListening = false
                    }
                } else {
                    speechService?.stop()
                    speechService = null
                    resultText = "Stopped listening"
                }
            }) {
                Text(if (isListening) "Stop" else "Recognize Mic")
            }
        }
    }
}

fun recognizeFile(model: Model?, speechStreamService: SpeechStreamService?, resultText: MutableState<String>, context: Context) {
    if (speechStreamService != null) {
        speechStreamService.stop()
    } else {
        try {
            val recognizer = model?.let { Recognizer(it, 16000f) }
            val ais = context.assets.open("10001-90210-01803.wav")
            if (ais.skip(44) != 44L) throw IOException("File too short")

            recognizer?.let {
                val streamService = SpeechStreamService(it, ais, 16000F)
                streamService.start(object : RecognitionListener {
                    override fun onResult(hypothesis: String) {
                        resultText.value += "$hypothesis\n"
                    }
                    override fun onFinalResult(hypothesis: String) {
                        resultText.value += "$hypothesis\n"
                    }
                    override fun onPartialResult(hypothesis: String) {}
                    override fun onError(e: Exception) {
                        resultText.value = "Error: ${e.message}"
                    }
                    override fun onTimeout() {}
                })
            }
        } catch (e: IOException) {
            resultText.value = "Error: ${e.message}"
        }
    }
}

fun recognizeMicrophone(
    model: Model?,
    speechService: SpeechService?,
    resultText: String,
    onStop: () -> Unit
) {
    if (speechService != null) {
        speechService.stop()
        onStop()
    } else {
        try {
            val recognizer = model?.let { Recognizer(it, 16000f) }
            recognizer?.let {
                val service = SpeechService(it, 16000f)
                service.startListening(object : RecognitionListener {
                    override fun onResult(hypothesis: String) {
                        resultText += "$hypothesis\n"
                    }
                    override fun onFinalResult(hypothesis: String) {
                        resultText += "$hypothesis\n"
                    }
                    override fun onPartialResult(hypothesis: String) {}
                    override fun onError(e: Exception) {
                        resultText = "Error: ${e.message}"
                        onStop()
                    }
                    override fun onTimeout() {
                        onStop()
                    }
                })
            }
        } catch (e: IOException) {
            resultText = "Error: ${e.message}"
            onStop()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewSpeechToTextScreen() {
    SpeechToTextScreen()
}

 */