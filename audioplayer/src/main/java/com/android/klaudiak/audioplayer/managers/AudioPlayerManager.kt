package com.android.klaudiak.audioplayer.managers

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.android.klaudiak.audioplayer.presentation.AudioPlayerViewModel.Companion.TAG
import java.io.File

class AudioPlayerManager(
    private val folderName: String,
    private val audioFileExtensions: List<String> = listOf("mp3", "wav", "flac", "ogg")
) {

    fun createExoPlayer(
        context: Context,
        onFileNameUpdate: (String) -> Unit,
        onDurationUpdate: (String, Long) -> Unit
    ): ExoPlayer = ExoPlayer.Builder(context).build().apply {
        setupPlayer(context, this, onFileNameUpdate, onDurationUpdate)
    }

    @OptIn(UnstableApi::class)
    private fun setupPlayer(
        context: Context,
        exoPlayer: ExoPlayer,
        onFileNameUpdate: (String) -> Unit,
        onDurationUpdate: (String, Long) -> Unit
    ) {
        val audioFiles = getAudioFiles()
        if (audioFiles.isEmpty()) {
            Log.e(TAG, "No audio files found in folder: ${getFolderPath()}")
            return
        }

        val mediaSources = createMediaSources(context, audioFiles, onDurationUpdate)
        exoPlayer.apply {
            setMediaSources(mediaSources)
            prepare()
            addPlayerListener(this, onFileNameUpdate)
        }
    }

    private fun getFolderPath(): String = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
        folderName
    ).path

    private fun getAudioFiles(): List<File> = File(getFolderPath())
        .listFiles { file -> file.extension in audioFileExtensions }
        ?.toList()
        ?: emptyList()

    @OptIn(UnstableApi::class)
    private fun createMediaSources(
        context: Context,
        audioFiles: List<File>,
        onDurationUpdate: (String, Long) -> Unit
    ): List<ProgressiveMediaSource> = audioFiles.map { file ->
        val uri = file.toUri()
        Log.d(TAG, "Adding file to playlist: ${file.name}")

        val duration = getAudioFileDuration(context, uri)
        onDurationUpdate(file.name, duration)
        Log.d(TAG, "Duration of ${file.name}: $duration ms")

        ProgressiveMediaSource.Factory(DefaultDataSource.Factory(context))
            .createMediaSource(
                MediaItem.Builder()
                    .setUri(uri)
                    .setMediaId(file.name)
                    .build()
            )
    }

    private fun addPlayerListener(
        exoPlayer: ExoPlayer,
        onFileNameUpdate: (String) -> Unit
    ) {
        exoPlayer.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                mediaItem?.mediaId?.let { mediaId ->
                    Log.i(TAG, "Transitioning to: $mediaId, time: ${System.currentTimeMillis()}")
                    onFileNameUpdate(mediaId)
                }
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    exoPlayer.currentMediaItem?.mediaId?.let { mediaId ->
                        Log.i(TAG, "First file ready: $mediaId")
                        onFileNameUpdate(mediaId)
                    }
                }
            }
        })
    }

    private fun getAudioFileDuration(context: Context, uri: Uri): Long =
        MediaMetadataRetriever().use { retriever ->
            try {
                retriever.setDataSource(context, uri)
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLong() ?: 0L
            } catch (e: Exception) {
                Log.e(TAG, "Error getting duration for $uri: ${e.message}")
                0L
            }
        }
}