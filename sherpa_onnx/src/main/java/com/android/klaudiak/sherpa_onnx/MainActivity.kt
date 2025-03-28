package com.android.klaudiak.sherpa_onnx

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.android.klaudiak.audioplayer.AudioPlaybackListener
import com.android.klaudiak.audioplayer.presentation.AudioPlayerScreen
import com.android.klaudiak.audioplayer.presentation.AudioPlayerViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlin.concurrent.thread

private const val REQUEST_RECORD_AUDIO_PERMISSION = 200
private const val TAG = "sherpa-onnx"

@AndroidEntryPoint
class SherpaOnnxActivity : ComponentActivity(), AudioPlaybackListener {

    private lateinit var model: SherpaOnnx
    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null

    private val audioPlayerViewModel: AudioPlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SherpaOnnxScreen(audioPlayerViewModel) }

        audioPlayerViewModel.setPlaybackListener(this)
    }

    @Composable
    fun SherpaOnnxScreen(viewModel: AudioPlayerViewModel) {
        val context = LocalContext.current
        val isRecording by viewModel.isRecording.collectAsState()
        val transcriptionText by viewModel.transcriptionText.collectAsState()

        val requestPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (!isGranted) {
                Log.e(TAG, "Audio recording permission denied")
                finish()
            } else {
                Log.i(TAG, "Audio recording permission granted")
            }
        }

        LaunchedEffect(Unit) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }

        LaunchedEffect(Unit) {
            Log.i(TAG, "Initializing model")
            model = initModel()
            Log.i(TAG, "Model initialized")
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AudioPlayerScreen(viewModel)

            SelectionContainer {
                Text(
                    text = transcriptionText,
                    modifier = Modifier
                        .weight(2.5f)
                        .fillMaxWidth()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = { toggleRecording(viewModel) }) {
                Text(if (isRecording) "Stop" else "Start")
            }
        }
    }

    private fun toggleRecording(viewModel: AudioPlayerViewModel) {
        if (!viewModel.isRecording.value) {
            startRecording(viewModel)
        } else {
            stopRecording(viewModel)
        }
    }

    private fun startRecording(viewModel: AudioPlayerViewModel) {
        if (!initMicrophone()) {
            Log.e(TAG, "Failed to initialize microphone")
            return
        }

        try {
            audioRecord?.startRecording()
            viewModel.startRecording()
            model.reset(true)

            recordingThread = thread(start = true) {
                processSamples { newText -> viewModel.updateTranscriptionText(newText) }
            }

            Log.i(TAG, "Recording started")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting recording: ${e.localizedMessage}")
        }
    }

    private fun stopRecording(viewModel: AudioPlayerViewModel) {
        try {
            viewModel.stopRecording()
            audioRecord?.apply {
                stop()
                release()
            }
            audioRecord = null
            recordingThread?.interrupt()
            Log.i(TAG, "Recording stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recording: ${e.localizedMessage}")
        }
    }

    private fun processSamples(updateText: (String) -> Unit) {
        Log.i(TAG, "Processing samples")

        val bufferSize = (0.1 * 16000).toInt()
        val buffer = ShortArray(bufferSize)
//        val startTime = System.nanoTime()

        while (audioPlayerViewModel.isRecording.value) {
            val ret = audioRecord?.read(buffer, 0, buffer.size)
            if (ret != null && ret > 0) {
                val samples = FloatArray(ret) { buffer[it] / 32768.0f }
                model.acceptWaveform(samples, sampleRate = 16000)
                synchronized(model) {
                    while (model.isReady()) {
                        model.decode()
                    }
                }

                val isEndpoint = model.isEndpoint()
                val text = model.text

                if (isEndpoint) {
                    model.reset()
                }

                updateText(text)
            }
        }

//        val endTime = System.nanoTime()
//        val processingTime = (endTime - startTime) / 1_000_000_000.0
//        val audioDuration = bufferSize.toDouble() / 16000
//        val rtf = processingTime / audioDuration

//        Log.i(
//            TAG,
//            "Processing Time: $processingTime sec, Audio Duration: $audioDuration sec, RTF: $rtf"
//        )
    }

    private fun initMicrophone(): Boolean {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }

        val bufferSize = AudioRecord.getMinBufferSize(
            16000,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        Log.i(TAG, "Buffer size: ${bufferSize * 1000.0f / 16000} ms")

//        val minBufferSize = AudioRecord.getMinBufferSize(
//            16000,
//            AudioFormat.CHANNEL_IN_MONO,
//            AudioFormat.ENCODING_PCM_16BIT
//        )
//
//        val bufferSize = if (minBufferSize != AudioRecord.ERROR_BAD_VALUE && minBufferSize != AudioRecord.ERROR) {
//            minBufferSize * 2
//        } else {
//            Log.e(TAG, "Invalid buffer size!")
//            return false
//        }

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            16000,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize * 2
        )
        return true
    }

    private fun initModel(): SherpaOnnx {
        Log.i(TAG, "Initializing model")

        val config = OnlineRecognizerConfig(
            featConfig = getFeatureConfig(sampleRate = 16000, featureDim = 80),
            modelConfig = getModelConfig(type = 9)!!,
            lmConfig = getOnlineLMConfig(type = 9),
            endpointConfig = getEndpointConfig(),
            enableEndpoint = true,
        )

        return SherpaOnnx(assetManager = assets, config = config)
    }

    override fun onNewAudioFileStarted(fileName: String) {
        Log.i(TAG, "New audio file started: $fileName")
        audioPlayerViewModel.updateFileName(fileName)
        model.reset(true)
    }
}
