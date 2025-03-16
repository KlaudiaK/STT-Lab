package com.android.klaudiak.audioplayer

import android.app.Application
import android.os.Environment
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.android.klaudiak.audioplayer.model.AudioFileData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.time.Duration
import java.time.LocalDateTime
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

    private val _startRecording = MutableStateFlow<LocalDateTime?>(null)
    val startRecording = _startRecording.asStateFlow()


    private val _endRecording = MutableStateFlow<LocalDateTime?>(null)
    val endRecording = _startRecording.asStateFlow()

    private val _files = MutableStateFlow<List<AudioFileData>>(emptyList())
    val files = _files.asStateFlow()

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

    fun startRecordingTime() {
        _startRecording.value = LocalDateTime.now()
    }

    fun endRecordingTime() {
        _endRecording.value = LocalDateTime.now()
        if (_startRecording.value != null && _endRecording.value != null) {
            val difference =
                Duration.between(_startRecording.value, _endRecording.value).seconds * 1000
            Log.i("Audio Player", "Recording time: $difference ms")
        }
    }

    fun updateFileDuration(filename: String, length: Long) {
        _files.update { files ->
            val updatedFiles = files.toMutableList()
            val index = updatedFiles.indexOfFirst { it.filename == filename }
            if (index != -1) {
                updatedFiles[index] = AudioFileData(filename, length, null, null)
            } else {
                updatedFiles.add(AudioFileData(filename, length, null, null))
            }
            updatedFiles
        }
    }

    fun updateFileTranscriptionDuration(transcriptionLength: Long) {
        _files.update { files ->
            val updatedFiles = files.toMutableList()
            val index = updatedFiles.indexOfFirst { it.filename == currentFileName.value }
            if (index != -1) {
                updatedFiles[index] =
                    updatedFiles[index].copy(transcriptionTime = transcriptionLength)
            }
            updatedFiles
        }

        Log.i("AudioFileData", _files.value.joinToString { it.toString() })
    }

    fun updateAudioFileTranslation(translations: List<String>) {
        _files.update { files ->
            val updatedFiles = files.toMutableList()
            val index = updatedFiles.indexOfFirst { it.filename == currentFileName.value }
            if (index != -1) {
                val finalTranslation = translations.map { it.substringBefore("_") }.joinToString( " " )
                updatedFiles[index] = updatedFiles[index].copy(transcription = finalTranslation)
            }
            updatedFiles
        }

        Log.i("AudioFileData", _files.value.joinToString { it.toString() })
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
