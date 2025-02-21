package com.android.klaudiak.audioplayer

import android.app.Application
import android.os.Environment
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileWriter
import java.io.IOException
import javax.inject.Inject


@HiltViewModel
class AudioPlayerViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    private val _currentFileName = MutableStateFlow<String?>(null)
    val currentFileName = _currentFileName.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording = _isRecording.asStateFlow()

    private val _transcriptionText = MutableStateFlow("")
    val transcriptionText = _transcriptionText.asStateFlow()

    private var playbackListener: AudioPlaybackListener? = null

    fun updateFileName(newFileName: String) {
        if (_currentFileName.value != newFileName) { // Prevent redundant updates
            _currentFileName.value = newFileName
            playbackListener?.onNewAudioFileStarted(newFileName) // Notify listener
        }
    }

    fun setPlaybackListener(listener: AudioPlaybackListener) {
        playbackListener = listener
    }

    fun startRecording() {
        _isRecording.value = true
    }

    fun stopRecording() {
        _isRecording.value = false
    }

    fun updateTranscriptionText(text: String) {
        _transcriptionText.value = text
    }

    fun saveTranscriptionToFile(audioFileName: String, transcription: String) {
        try {
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "transcriptions"
            )
            if (!dir.exists()) dir.mkdirs()

            val file = File(dir, "transcriptions.txt")
            val writer = FileWriter(file, true)
            writer.append("$audioFileName $transcription\n")
            writer.flush()
            writer.close()
        } catch (e: IOException) {
            Log.e("Audio Player", "Error saving transcription: ${e.message}")
        }
    }
}
