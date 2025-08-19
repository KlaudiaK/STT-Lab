package com.android.klaudiak.audioplayer.presentation

import android.app.Application
import android.os.Environment
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.exoplayer.ExoPlayer
import com.android.klaudiak.audioplayer.AccuracyCalculator.calculateWERBasedOnReferenceFile
import com.android.klaudiak.audioplayer.AudioPlaybackListener
import com.android.klaudiak.audioplayer.FileUtils
import com.android.klaudiak.audioplayer.managers.AudioFileType
import com.android.klaudiak.audioplayer.managers.AudioPlayerManager
import com.android.klaudiak.audioplayer.model.AudioFileData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.time.Duration
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class AudioPlayerViewModel @Inject constructor(
    application: Application,
    private val audioPlayerManager: AudioPlayerManager
) : AndroidViewModel(application) {

    private val _currentFileName = MutableStateFlow<String?>(null)
    val currentFileName = _currentFileName.asStateFlow()

    private val _previousFilename = MutableStateFlow<String?>(null)
    val previousFilename = _previousFilename.asStateFlow()

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

    private val _isPlaybackComplete = MutableStateFlow(false)
    val isPlaybackComplete = _isPlaybackComplete.asStateFlow()

    private val _filenameWithTranscription = MutableStateFlow<String?>(null)
    val filenameWithTranscription = _filenameWithTranscription.asStateFlow()

    private var previousTranslation: String = ""
    private var translation: String = ""

    private var exoPlayer: ExoPlayer? = null

    fun updateFileName(newFileName: String, resetModel: () -> Unit = {}) {
        viewModelScope.launch {
            delay(2000)
            updateTranslation()
            previousTranslation = translation
            resetModel()
        }

        viewModelScope.launch {
            delay(3000)
            val filenameWithoutExtension = newFileName.substringBeforeLast(".")
            if (filenameWithoutExtension == "PLAYBACK_COMPLETE") {
                _isPlaybackComplete.value = true
            } else if (_currentFileName.value != filenameWithoutExtension) {
                _currentFileName.value = filenameWithoutExtension
                _isPlaybackComplete.value = false
                playbackListener?.onNewAudioFileStarted(filenameWithoutExtension)
            }
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

    private fun updateFileDuration(filename: String, length: Long) {
        val filenameWithoutExtension = filename.substringBeforeLast(".")
        _files.update { files ->
            val updatedFiles = files.toMutableList()
            val index = updatedFiles.indexOfFirst { it.filename == filenameWithoutExtension }
            if (index != -1) {
                updatedFiles[index] = AudioFileData(filenameWithoutExtension, length, null, null)
            } else {
                updatedFiles.add(AudioFileData(filenameWithoutExtension, length, null, null))
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

    fun updateAudioFileTranslation(translations: String) {
        translation = translations.substringAfter(previousTranslation)
    }

    private fun updateTranslation() {
        _files.update { files ->
            Log.d("AudioPlayerViewModel", "Current filename : ${currentFileName.value}")
            val updatedFiles = files.toMutableList()
            val index = updatedFiles.indexOfFirst { it.filename == currentFileName.value }
            if (index != -1) {
                updatedFiles[index] = updatedFiles[index].copy(transcription = translation)
            }
            updatedFiles
        }

        Log.i("AudioFileData", _files.value.joinToString { it.toString() })
        // TODO uncomment for saving transcription - WER calculation
        //  exportTranscriptionsToTxt(_files.value, "transcriptions_s23-commonvoice_sherpa_ncnn_phone")
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

    fun initializePlayer() {
        viewModelScope.launch {
            exoPlayer = audioPlayerManager.createExoPlayer(
                context = getApplication(),
                onFileNameUpdate = { filename ->
                    updateFileName(filename)
                },
                onDurationUpdate = { filename, duration ->
                    updateFileDuration(filename, duration)
                },
                fileSource = AudioFileType.Folder(
                    folderName = FileUtils.getExternalDownloadFolderPath("stt_audiofiles/resources_usage/long"),
                )
                /*fileSource = AudioFileType.Single(
                    fileName = "2300-131720-0035.flac",
                    path = FileUtils.getExternalDownloadFolderPath(
                        "stt_audiofiles/resources_usage/long"
                    )
                )*/
            )
        }
    }

    fun copyFilesFromExternalToInternalStorage() {
        audioPlayerManager.copyFilesToInternalStorage()
    }

    fun play() {
        exoPlayer?.play()
    }

    fun pause() {
        exoPlayer?.pause()
    }

    fun setPlayWhenReady(playWhenReady: Boolean) {
        exoPlayer?.playWhenReady = playWhenReady
    }

    fun moveSeekToStart() {
        exoPlayer?.let {
            if (it.mediaItemCount > 0) {
                it.seekTo(0, 0) // Move to the first item in the playlist
                it.playWhenReady = true
            }
        }

    }

    fun exportTranscriptionsToTxt(audioFiles: List<AudioFileData>, outputFilePath: String) {
        try {
            val path =
                "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/transcriptions"

            val dir = File(path)
            if (!dir.exists()) {
                dir.mkdirs()
            }

            val filename = "$path/$outputFilePath.txt"

            _filenameWithTranscription.update { filename }

            File(filename).bufferedWriter().use { writer ->
                audioFiles.forEach { file ->
                    writer.write("${file.filename} ${file.transcription ?: ""}\n")
                }
            }
            Log.d(TAG, "Successfully exported transcriptions to $outputFilePath")

            getWERMetric()
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting transcriptions: ${e.message}")
        }
    }

    fun getWERMetric() {
        _filenameWithTranscription.value?.let { filename ->
            val refFile =
                File("${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/combined_transcriptions.txt")
            val file = File(filename)
            if (file.exists()) {
                Log.d(TAG, "Transcription file exists: $filename")
                calculateWERBasedOnReferenceFile(
                    refFile,
                    file
                )
            } else {
                Log.d(TAG, "Transcription file does not exist: $filename")
            }
        }

    }

    fun loadAudioFileFromPath(filePath: String) {
        val file = File(filePath)
        if (file.exists()) {
            viewModelScope.launch {
                exoPlayer?.let {
                    it.stop()
                    it.clearMediaItems()
                    audioPlayerManager.loadSingleFile(it, file)
                }
            }
            updateFileName(file.name)
        } else {
            Log.e(TAG, "File at $filePath does not exist.")
        }
    }

    override fun onCleared() {
        super.onCleared()
        exoPlayer?.release()
        exoPlayer = null
    }

    companion object {
        const val AUDIO_FILE_FOLDER_NAME = "stt"
        const val TAG = "AudioPlayer"
    }
}
