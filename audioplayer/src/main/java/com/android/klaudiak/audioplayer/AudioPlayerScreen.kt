package com.android.klaudiak.audioplayer

import android.Manifest
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import java.io.File

@Composable
fun AudioPlayerScreen(viewModel: AudioPlayerViewModel = hiltViewModel()) {
    val context = LocalContext.current


    var isPlaying by remember { mutableStateOf(false) }
    val exoPlayer = remember { createExoPlayer(context, "stt", { viewModel.updateFileName(it) }) }

    val requestPermissionLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                Log.i("AudioPlayer", "Permission denied.")
            }
        }

    LaunchedEffect(Unit) {
        requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    LaunchedEffect(isPlaying) {
        exoPlayer.playWhenReady = isPlaying
    }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        IconButton(
            onClick = { isPlaying = !isPlaying },

            colors = IconButtonDefaults.iconButtonColors(
                contentColor = Color(0xFFD7E4FF)
            )
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Outlined.PauseCircle else Icons.Outlined.PlayCircle,
                modifier = Modifier.size(64.dp),
                contentDescription = null
            )
        }

        Button(
            onClick = {
                if (exoPlayer.mediaItemCount > 0) {
                    exoPlayer.seekTo(0, 0) // Move to the first item in the playlist
                    exoPlayer.playWhenReady = true
                }
            },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Play from Beginning")
            Icon(
                imageVector = Icons.Default.RestartAlt,
                contentDescription = null
            )
        }
    }
}

@OptIn(UnstableApi::class)
fun createExoPlayer(
    context: Context,
    folderName: String,
    updateFileName: (String) -> Unit
): ExoPlayer {
    val exoPlayer = ExoPlayer.Builder(context).build()

    val folder = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
        folderName
    )
    val audioFiles =
        folder.listFiles { file -> file.extension in listOf("mp3", "wav", "flac", "ogg") }

    if (audioFiles.isNullOrEmpty()) {
        Log.e("AudioPlayer", "No audio files found in folder: ${folder.path}")
        return exoPlayer
    }

    val mediaSources = audioFiles.map { file ->
        val uri: Uri = file.toUri()
        Log.d("AudioPlayer", "Adding file to playlist: ${file.name}")

        val duration = getAudioFileDuration(context, uri)
        Log.d("AudioPlayer", "Duration of ${file.name}: $duration ms")

        val mediaItem = MediaItem
            .Builder()
            .setUri(uri)
            .setMediaId(file.name)
            .build()

        val dataSourceFactory = DefaultDataSource.Factory(context)
        ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
    }

    exoPlayer.setMediaSources(mediaSources)
    exoPlayer.prepare()

    exoPlayer.addListener(object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            mediaItem?.let {
                Log.i("AudioPlayer", "Transitioning to: ${it.mediaId}")
                updateFileName(it.mediaId)
            }

        }

        override fun onPlaybackStateChanged(state: Int) {
            if (state == Player.STATE_READY && exoPlayer.currentMediaItem != null) {
                val firstItem = exoPlayer.currentMediaItem
                firstItem?.let {
                    Log.i("AudioPlayer", "First file ready: ${it.mediaId}")
                    updateFileName(it.mediaId)
                }
            }
        }
    })

    return exoPlayer
}

fun getAudioFileDuration(context: Context, uri: Uri): Long {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, uri)
        val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        duration?.toLongOrNull() ?: 0L
    } catch (e: Exception) {
        Log.e("AudioPlayer", "Error retrieving duration for URI: $uri", e)
        0L
    } finally {
        retriever.release()
    }
}

/*

@OptIn(UnstableApi::class)
fun createExoPlayer(context: Context): ExoPlayer {
    val exoPlayer = ExoPlayer.Builder(context).build()

    val folder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    val filePath = folder.path + "/84-121550-0000.flac"

    val uri: Uri = filePath.toUri()

    val mediaItem = MediaItem.fromUri(uri)
    val dataSourceFactory = DefaultDataSource.Factory(context)
    val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)

    exoPlayer.setMediaSource(mediaSource)
    exoPlayer.prepare()

    return exoPlayer
}*/
