package com.android.klaudiak.vosk_stt

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.NonCancellable.start
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.SpeechStreamService
import org.vosk.android.StorageService
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class STTViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    private val _resultText = MutableStateFlow("Preparing...")
    val resultText: StateFlow<String> = _resultText

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening

    private var model: Model? = null
    private var speechService: SpeechService? = null
    private var speechStreamService: SpeechStreamService? = null

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

    fun recognizeFile(context: Context, filename: String) {
        speechStreamService?.let {
            it.stop()
            speechStreamService = null
            _resultText.value = "Stopped file recognition."
            return
        }

        val localModel = model ?: run {
            _resultText.value = "Model is not initialized."
            return
        }

        try {
            context.assets.open(filename).use { ais ->
                if (ais.skip(44) != 44L) {
                    throw IOException("Invalid WAV file: insufficient header data.")
                }

                val recognizer = Recognizer(localModel, 16000f)
                speechStreamService = SpeechStreamService(recognizer, ais, 16000f).apply {
                    start(object : RecognitionListener {
                        override fun onResult(hypothesis: String) {
                            _resultText.update { it + "$hypothesis\n" }
                        }

                        override fun onFinalResult(hypothesis: String) {
                            _resultText.update { it + "$hypothesis\n" }
                            speechStreamService = null
                        }

                        override fun onPartialResult(hypothesis: String) {}

                        override fun onError(e: Exception) {
                            _resultText.value = "Error: ${e.localizedMessage ?: "Unknown error"}"
                        }

                        override fun onTimeout() {}
                    })
                }
            }
        } catch (e: IOException) {
            _resultText.value = "I/O Error: ${e.localizedMessage}"
        } catch (e: Exception) {
            _resultText.value = "Unexpected Error: ${e.localizedMessage}"
        }
    }


    fun toggleMicrophone() {
        if (speechService != null) {
            stopListening()
        } else {
            startListening()
        }
    }

    private fun stopListening() {
        speechService?.stop()
        speechService = null
        _isListening.value = false
        _resultText.value = "Stopped listening."
    }

    private fun startListening() {
        val recognizer = createRecognizer() ?: return

        try {
            speechService = SpeechService(recognizer, 16000f).apply {
                startListening(object : RecognitionListener {
                    override fun onResult(hypothesis: String) {
                        updateResult(hypothesis)
                    }

                    override fun onFinalResult(hypothesis: String) {
                        updateResult(hypothesis)
                    }

                    override fun onPartialResult(hypothesis: String) {
                        if (hypothesis.isNotEmpty()) updateResult(hypothesis)
                    }

                    override fun onError(e: Exception) {
                        handleError("Error: ${e.localizedMessage}")
                    }

                    override fun onTimeout() {}
                })
            }
            _isListening.value = true
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

    private fun updateResult(text: String) {
        _resultText.value += "$text\n"
    }

    private fun handleError(message: String) {
        _resultText.value = message
    }

}
