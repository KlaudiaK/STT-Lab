package com.android.klaudiak.sherpa_ncnn

import android.app.Application
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.lifecycle.ViewModel
import com.android.klaudiak.audioplayer.presentation.AudioPlayerViewModel
import com.android.klaudiak.domain.permissions.PermissionRepository
import com.k2fsa.sherpa.ncnn.RecognizerConfig
import com.k2fsa.sherpa.ncnn.SherpaNcnn
import com.k2fsa.sherpa.ncnn.getDecoderConfig
import com.k2fsa.sherpa.ncnn.getFeatureExtractorConfig
import com.k2fsa.sherpa.ncnn.getModelConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.LinkedHashMap
import javax.inject.Inject
import kotlin.concurrent.thread

@HiltViewModel
class SherpaNcnnViewModel @Inject constructor(
    private val permissionRepository: PermissionRepository
) : ViewModel() {

    private lateinit var model: SherpaNcnn
    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null

    val loggedTexts = mutableSetOf<String>()

    fun updateModel(model: SherpaNcnn) {
        this.model = model
    }

    fun resetModel(recreate: Boolean = false) {
        model.reset(recreate)
    }

    fun initModel(useGPU: Boolean = true, application: Application): SherpaNcnn {
        Log.i(TAG, "Initializing model")
        val featConfig = getFeatureExtractorConfig(16000.0f, 80)
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

        return SherpaNcnn(
            assetManager = application.assets,
            config = config
        )
    }

    fun toggleRecording(viewModel: AudioPlayerViewModel) {
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
                    { newText -> viewModel.updateTranscriptionText(newText) },
                    { length -> viewModel.updateFileTranscriptionDuration(length) },
                    { viewModel.updateAudioFileTranslation(it) }
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

        Log.i(TAG, "Started processing samples")
        val bufferSize = (0.1 * 16000).toInt()
        val buffer = ShortArray(bufferSize)
        val wordTimestamps = LinkedHashMap<String, Long>()

        while (true) {
            val ret = audioRecord?.read(buffer, 0, buffer.size) ?: 0
            if (ret > 0) {
                val samples = FloatArray(ret) { buffer[it] / 32768.0f }
                model.acceptSamples(samples)
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

                val text = model.text
                updateText(text)
                logTextIfNotLogged(text)

                if (model.isEndpoint()) {
                    model.reset()
                }
            }
        }
    }

    private fun logTextIfNotLogged(text: String) {
        if (loggedTexts.add(text)) {
            Log.i("WordTimestamp", "Text: $text, Timestamp: ${System.currentTimeMillis()} ms")
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

    companion object {
        const val TAG = "SherpaNcnnViewModel"
    }
}
