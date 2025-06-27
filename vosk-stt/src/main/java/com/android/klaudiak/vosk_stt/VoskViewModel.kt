package com.android.klaudiak.vosk_stt

import android.Manifest
import android.app.Application
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.klaudiak.audioplayer.AudioPlaybackListener
import com.android.klaudiak.audioplayer.model.AudioFileData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.SpeechStreamService
import org.vosk.android.StorageService
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class VoskViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    private val _resultText = MutableStateFlow("Preparing...")
    val resultText: StateFlow<String> = _resultText

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    private val _finalResultWithTimestamps = MutableStateFlow<Map<String, Long>>(emptyMap())
    val finalResultWithTimestamps: StateFlow<Map<String, Long>> = _finalResultWithTimestamps

    private val _currentFileName = MutableStateFlow<String?>(null)
    val currentFileName = _currentFileName.asStateFlow()

    private val _isPlaybackComplete = MutableStateFlow(false)
    val isPlaybackComplete = _isPlaybackComplete.asStateFlow()

    private val _files = MutableStateFlow<List<AudioFileData>>(emptyList())
    val files = _files.asStateFlow()

    private var playbackListener: AudioPlaybackListener? = null

    private var model: Model? = null
    private var speechService: SpeechService? = null
    private var speechStreamService: SpeechStreamService? = null

    private var previousTranslation: String = ""
    private var translation: String = ""

    fun initModel(context: Context) {
        viewModelScope.launch {
            StorageService.unpack(context, "model-en-us", "model",
                { loadedModel -> model = loadedModel },
                { exception -> _resultText.value = "Error loading model: ${exception.message}" }
            )
        }
    }

    fun requestPermission(activity: ComponentActivity) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            1
        )
    }

    fun toggleMicrophone(updateAudioFileTranslation: (String) -> Unit) {
        if (speechService != null) {
            stopListening()
        } else {
            startListening(updateAudioFileTranslation)
        }
    }

    private fun stopListening() {
        speechService?.stop()
        speechService = null
        _isRecording.value = false
        _resultText.value = "Stopped listening."
    }

    private fun startListening(updateAudioFileTranslation: (String) -> Unit) {
        val recognizer = createRecognizer() ?: return

        try {
            speechService = SpeechService(recognizer, 16000f).apply {
                startListening(object : RecognitionListener {
                    private val wordTimestamps = mutableMapOf<String, Long>()
                    private var lastPartialResultTime = System.currentTimeMillis()
                    private var lastAccumulatedLength =
                        0 // Track the length of the previously accumulated text

                    override fun onResult(hypothesis: String) {
                        //   _resultText.value += hypothesis
                        //    updatePartialResult(hypothesis)
                        updatePartialResult(hypothesis)
                        updateAudioFileTranslation(resultText.value)
                    }

                    override fun onFinalResult(hypothesis: String) {
                        _isRecording.value = false
                        speechStreamService = null
                        //    parseAndStoreTimestamps(hypothesis, isFinal = true)
                        //    _finalResultWithTimestamps.value = wordTimestamps.toMap() // Expose the final timestamps
                        //    _resultText.value = parseResultFinalContent(hypothesis)
                        //  _resultText.value += hypothesis
                        wordTimestamps.clear()
                        lastAccumulatedLength = 0
                        //    updatePartialResult(hypothesis)
                        //   Log.d("TAG", "Final result: $hypothesis")
                    }

                    override fun onPartialResult(hypothesis: String) {
                        // parseAndStoreTimestamps(hypothesis, isFinal = false)

                        //   _resultText.value += hypothesis
                        //    lastPartialResultTime = System.currentTimeMillis()

                        //    updatePartialResult(hypothesis)
                        //   _resultText.value = parseResultPartialContent(hypothesis)
                        //   Log.d("TAG", "Partial result: $hypothesis")
                    }

                    override fun onError(e: Exception) {
                        handleError("Error: ${e.localizedMessage}")
                        wordTimestamps.clear()
                        lastAccumulatedLength = 0
                    }

                    override fun onTimeout() {
                        wordTimestamps.clear()
                        lastAccumulatedLength = 0
                    }

                    private fun parseAndStoreTimestamps(hypothesis: String, isFinal: Boolean) {
                        val currentResult =
                            if (isFinal) parseResultFinalContent(hypothesis) else parseResultPartialContent(
                                hypothesis
                            )

                        if (currentResult.isNotBlank()) {
                            val newText = currentResult.substring(lastAccumulatedLength).trim()
                            val newWords = newText.split(" ").filter { it.isNotBlank() }
                            val currentTime = System.currentTimeMillis() // Capture the current time

                            newWords.forEach { word ->
                                if (!wordTimestamps.containsKey(word)) {
                                    wordTimestamps[word] =
                                        currentTime // Record the current time for each new word
                                }
                            }
                            lastPartialResultTime = currentTime
                            lastAccumulatedLength = currentResult.length
                        }
                    }
                })
            }
            _isRecording.value = true
        } catch (e: IOException) {
            handleError("Error initializing microphone: ${e.localizedMessage}")
        }
    }

    private fun createRecognizer(): Recognizer? {
        return try {
            model?.let { Recognizer(it, 16000f) } ?: run {
                handleError("Model is not initialized.")
                null
            }
        } catch (e: Exception) {
            handleError("Failed to create recognizer: ${e.localizedMessage}")
            null
        }
    }

    /*
    private fun updatePartialResult(hypothesis: String) {
            parseResultPartialContent(hypothesis).takeIf { it.isNotEmpty() }?.let {
                _resultText.value = it
            }
        }
     */

    private fun updatePartialResult(hypothesis: String) {
        parseResultFinalContent(hypothesis).takeIf { it.isNotEmpty() }?.let { currentPartial ->
            if (_resultText.value == "Preparing...") {
                _resultText.value = currentPartial
                lastPartialResult = currentPartial
            } else {
                val newWords = currentPartial.removePrefix(lastPartialResult).trim()
                if (newWords.isNotEmpty()) {
                    _resultText.value = "${_resultText.value} $newWords".trim()
                    lastPartialResult = currentPartial
                }
            }
        }
    }

    private var lastPartialResult: String = ""

    private fun updateFinalResult(hypothesis: String) {
        parseResultFinalContent(hypothesis).takeIf { it.isNotEmpty() }?.let {
            _resultText.value = it
        }
    }

    private fun handleError(message: String) {
        _resultText.value = message
    }

    private fun parseResultPartialContent(hypothesis: String): String =
        hypothesis.parseJsonContent("partial")

    private fun parseResultFinalContent(hypothesis: String): String =
        hypothesis.parseJsonContent("text")

    private fun String.parseJsonContent(key: String): String = runCatching {
        JSONObject(this).getString(key)
    }.getOrElse { this }

}
