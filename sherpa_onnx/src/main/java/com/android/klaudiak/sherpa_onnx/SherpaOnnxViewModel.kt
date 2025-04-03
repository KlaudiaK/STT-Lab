package com.android.klaudiak.sherpa_onnx

import android.app.Application
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.lifecycle.ViewModel
import com.android.klaudiak.domain.permissions.PermissionRepository
import com.android.klaudiak.sherpa_onnx.SherpaOnnxActivity.Companion.TAG
import com.k2fsa.sherpa.onnx.OnlineRecognizerConfig
import com.k2fsa.sherpa.onnx.SherpaOnnx
import com.k2fsa.sherpa.onnx.getEndpointConfig
import com.k2fsa.sherpa.onnx.getFeatureConfig
import com.k2fsa.sherpa.onnx.getModelConfig
import com.k2fsa.sherpa.onnx.getOnlineLMConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.LinkedHashMap
import javax.inject.Inject
import kotlin.concurrent.thread

@HiltViewModel
class SherpaOnnxViewModel @Inject constructor(
    private val permissionRepository: PermissionRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private lateinit var model: SherpaOnnx
    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null

    private val _isRecording = MutableStateFlow(false)
    val isRecording = _isRecording.asStateFlow()

    private val loggedTexts = mutableSetOf<String>()

    fun initModel(): SherpaOnnx {
        Log.i(TAG, "Initializing model")

        val config = OnlineRecognizerConfig(
            featConfig = getFeatureConfig(sampleRate = 16000, featureDim = 80),
            modelConfig = getModelConfig(type = 9)!!,
            lmConfig = getOnlineLMConfig(type = 9),
            endpointConfig = getEndpointConfig(),
            enableEndpoint = true,
        )

        return SherpaOnnx(assetManager = context.assets, config = config)
    }

    fun updateModel(model: SherpaOnnx) {
        this.model = model
    }

    fun toggleRecording(
        updateTranscriptionText: (String) -> Unit,
        updateFileTranscriptionDuration: (Long) -> Unit,
        updateAudioFileTranslation: (String) -> Unit
    ) {
        if (!isRecording.value) {
            startRecording(
                updateTranscriptionText = updateTranscriptionText,
                updateFileTranscriptionDuration = updateFileTranscriptionDuration,
                updateAudioFileTranslation = updateAudioFileTranslation
            )
        } else {
            stopRecording()
        }
    }

    private fun startRecording(
        updateTranscriptionText: (String) -> Unit,
        updateFileTranscriptionDuration: (Long) -> Unit,
        updateAudioFileTranslation: (String) -> Unit
    ) {
        if (!initMicrophone()) {
            Log.e(TAG, "Failed to initialize microphone")
            return
        }

        try {
            audioRecord?.startRecording()
            _isRecording.value = true
            model.reset(true)

            recordingThread = thread(start = true) {
                processSamples(
                    { updateTranscriptionText(it) },
                    { updateFileTranscriptionDuration(it) },
                    { updateAudioFileTranslation(it) }
                )
            }
            Log.i(TAG, "Recording started")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting recording: ${e.localizedMessage}")
        }
    }

    private fun stopRecording() {
        try {
            _isRecording.value = false
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
        updateAudioFileTranslation: (String) -> Unit
    ) {

        Log.i(TAG, "Started processing samples")
        val sampleRateInHz = 16000
        val bufferSize = AudioRecord.getMinBufferSize(
            sampleRateInHz, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )

        val buffer = ShortArray(bufferSize)
        val wordTimestamps = LinkedHashMap<String, Long>()

        var totalProcessingTimeNs: Long = 0
        var totalAudioDurationSec: Double = 0.0
        val chunkDurationSec = bufferSize / sampleRateInHz.toDouble()

        while (true) {
            val ret = audioRecord?.read(buffer, 0, buffer.size) ?: 0
            if (ret > 0) {
                val samples = FloatArray(ret) { buffer[it] / 32768.0f }

                val startTimeNs = System.nanoTime()

                model.acceptWaveform(samples, sampleRate = sampleRateInHz)
                synchronized(model) {
                    while (model.isReady()) {
                        model.decode()
                        if (model.text.isNotBlank()) {
                            val startTime = System.currentTimeMillis()
                            model.text.split(" ").filter { it.isNotBlank() }
                                .forEachIndexed { index, word ->
                                    if (!wordTimestamps.containsKey(word + "_$index")) {
                                        wordTimestamps[word + "_$index"] = startTime
                                    }
                                }
                        }
                    }
                }

                totalProcessingTimeNs += System.nanoTime() - startTimeNs
                totalAudioDurationSec += chunkDurationSec

                val rtf = (totalProcessingTimeNs / 1e9) / totalAudioDurationSec
                Log.i(TAG, "RTF: $rtf")

                updateText(model.text)
                logTextIfNotLogged(model.text, updateAudioFileTranslation)

                if (model.isEndpoint()) {
                    model.reset()
                }
            }
        }
    }


    private fun logTextIfNotLogged(text: String, updateAudioFileTranslation: (String) -> Unit) {
        if (loggedTexts.add(text)) {
            Log.i("WordTimestamp", "Text: $text, Timestamp: ${System.currentTimeMillis()} ms")
            updateAudioFileTranslation(text)
        }
    }

    private fun initMicrophone(): Boolean {
        val bufferSize = AudioRecord.getMinBufferSize(
            16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )

        if (checkAudioPermission()) {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                16000,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize * 2
            )
            return audioRecord?.state == AudioRecord.STATE_INITIALIZED
        } else {
            Log.e(TAG, "Permission denied.")
            return false
        }
    }

    private fun checkAudioPermission() = permissionRepository.hasRecordAudioPermission()
}
