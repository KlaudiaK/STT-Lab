package com.android.klaudiak.sherpa_ncnn

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
import com.android.klaudiak.audioplayer.AudioPlayerScreen
import com.android.klaudiak.audioplayer.AudioPlayerViewModel
import com.k2fsa.sherpa.ncnn.RecognizerConfig
import com.k2fsa.sherpa.ncnn.SherpaNcnn
import com.k2fsa.sherpa.ncnn.getDecoderConfig
import com.k2fsa.sherpa.ncnn.getFeatureExtractorConfig
import com.k2fsa.sherpa.ncnn.getModelConfig

import dagger.hilt.android.AndroidEntryPoint
import kotlin.concurrent.thread

private const val REQUEST_RECORD_AUDIO_PERMISSION = 200
private const val TAG = "sherpa-ncnn"

@AndroidEntryPoint
class SherpaNcnnActivity : ComponentActivity(), AudioPlaybackListener {

    private lateinit var model: SherpaNcnn
    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null

    private val audioPlayerViewModel: AudioPlayerViewModel by viewModels()

    val loggedTexts = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SherpaNcnnScreen(audioPlayerViewModel) }

        audioPlayerViewModel.setPlaybackListener(this)
    }

    @Composable
    fun SherpaNcnnScreen(viewModel: AudioPlayerViewModel) {
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
                processSamples(
                    { newText ->
                        viewModel.updateTranscriptionText(newText)
                    },
                    { length ->
                        viewModel.updateFileTranscriptionDuration(length)
                    },
                    {
                        viewModel.updateAudioFileTranslation(it)
                    }
                )
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

    private fun processSamples(
        updateText: (String) -> Unit,
        updateFileTranscriptionDuration: (Long) -> Unit,
        updateAudioFileTranslation: (List<String>) -> Unit
    ) {

        var started = false
        Log.i(TAG, "Processing samples")

        val bufferSize = (0.1 * 16000).toInt()
        val buffer = ShortArray(bufferSize)
//        val startTime = System.nanoTime()
        val wordTimestamps = LinkedHashMap<String, Long>()
        while (audioPlayerViewModel.isRecording.value) {
            val ret = audioRecord?.read(buffer, 0, buffer.size)
            if (ret != null && ret > 0) {
                val samples = FloatArray(ret) { buffer[it] / 32768.0f }
                model.acceptSamples(samples)
                synchronized(model) {
                    while (model.isReady()) {
                        model.decode()
                        var startTime: Long = 0

                        with(model.text) {
                            if (isNotBlank() && this != " ") {
                                if (!started) {
                                    Log.i("Playing", model.text)
                                    started = true
                                }
                                startTime = System.currentTimeMillis()

                                split(" ")
                                    .filter { it.isNotBlank() }
                                    .forEachIndexed { index, word ->
                                        if (!wordTimestamps.containsKey(word + "_$index")) {
                                            val timestamp = System.currentTimeMillis() - startTime
                                            wordTimestamps[word + "_$index"] = startTime
                                        }
                                    }
                            }
                        }
                    }
                }

                val isEndpoint = model.isEndpoint()
                val text = model.text

                if (isEndpoint) {
                    //stopRecordingTime()
                    model.reset()
                }
                updateText(text)
                logTextIfNotLogged(text)
            }
        }

        val filteredWordTimestamps = removeDuplicates(wordTimestamps)

        filteredWordTimestamps.forEach { (word, timestamp) ->
            Log.i(
                "WordTimestamp",
                "Word: ${word}, Timestamp: $timestamp ms"
            )
        }
        updateAudioFileTranslation(filteredWordTimestamps.keys.toList())

        with(filteredWordTimestamps.values) {
            val diff = last() - first()
            Log.i(
                "WordTimestamp",
                "Duration: $diff ms"
            )

            updateFileTranscriptionDuration(diff)

            //  firstNotNullOfOrNull { (_, timestamp) -> timestamp }
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

    private fun removeDuplicates(wordTimestamps: LinkedHashMap<String, Long>): LinkedHashMap<String, Long> {

        val groupedMap = mutableMapOf<String, MutableList<Pair<String, Long>>>()

        for ((key, timestamp) in wordTimestamps) {
            val identifier =
                key.substringAfterLast("_")
            groupedMap.getOrPut(identifier) { mutableListOf() }.add(key to timestamp)
        }

        val filteredMap = LinkedHashMap<String, Long>()

        for ((_, entries) in groupedMap) {
            val entryWithHighestTimestamp =
                entries.maxByOrNull { it.second }
            if (entryWithHighestTimestamp != null) {
                filteredMap[entryWithHighestTimestamp.first] = entryWithHighestTimestamp.second
            }
        }

        return filteredMap
    }

    private fun logTextIfNotLogged(text: String) {
        if (loggedTexts.add(text)) {
            Log.i(
                "WordTimestamp",
                "Text: $text, Timestamp: ${System.currentTimeMillis()} ms"
            )
        }
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

    private fun initModel(useGPU: Boolean = true): SherpaNcnn {
        Log.i(TAG, "Initializing model")
        val featConfig = getFeatureExtractorConfig(
            sampleRate = 16000.0f,
            featureDim = 80
        )

        //Please change the argument "type" if you use a different model
        val modelConfig = getModelConfig(type = 7, useGPU = useGPU)!!
        val decoderConfig = getDecoderConfig(method = "greedy_search", numActivePaths = 4)
        val config = RecognizerConfig(
            featConfig = featConfig,
            modelConfig = modelConfig,
            decoderConfig = decoderConfig,
            enableEndpoint = true,
            rule1MinTrailingSilence = 2.0f,
            rule2MinTrailingSilence = 0.8f,
            rule3MinUtteranceLength = 20.0f,
        )


        return SherpaNcnn(assetManager = assets, config = config)
    }

    override fun onNewAudioFileStarted(fileName: String) {
        Log.i(TAG, "New audio file started: $fileName")
        audioPlayerViewModel.updateFileName(fileName)
        model.reset(true)
    }
}
