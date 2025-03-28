package com.android.klaudiak.audioplayer.presentation

import android.Manifest
import android.content.Context
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.android.klaudiak.audioplayer.AudioFileUtils.getAudioFileDuration
import com.android.klaudiak.audioplayer.presentation.AudioPlayerViewModel.Companion.AUDIO_FILE_FOLDER_NAME
import com.android.klaudiak.audioplayer.presentation.AudioPlayerViewModel.Companion.TAG
import com.android.klaudiak.audioplayer.R
import java.io.File

@Composable
fun AudioPlayerScreen(
    viewModel: AudioPlayerViewModel = hiltViewModel(),
    toggleRecording: () -> Unit,
) {
    var isPlaying by remember { mutableStateOf(false) }

    val requestPermissionLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                Log.i("AudioPlayer", "Permission denied.")
            }
        }

    LaunchedEffect(Unit) {
        viewModel.initializePlayer()
        requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    LaunchedEffect(isPlaying) {
        viewModel.setPlayWhenReady(isPlaying)
    }

    Column(
        modifier = Modifier
            .padding(dimensionResource(R.dimen.padding_medium))
            .fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        PlayAudioButton(isPlaying) {
            isPlaying = !isPlaying
            toggleRecording()
        }

        Button(
            onClick = {
               viewModel.moveSeekToStart()
                isPlaying = !isPlaying
            },
            modifier = Modifier.padding(top = dimensionResource(R.dimen.padding_small))
        ) {
            Text("Play from Beginning")
            Icon(
                imageVector = Icons.Default.RestartAlt,
                contentDescription = null
            )
        }
    }
}

@Composable
fun PlayAudioButton(isPlaying: Boolean, onClick: () -> Unit) {
    Button(
        onClick = { onClick() }
    ) {
        Text(text = if (isPlaying) "Pause audio" else "Play audio")
    }
}

@Composable
fun PlayAudioIconButton(isPlaying: Boolean, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.testTag("PlayPauseButton"),
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = Color(0xFFD7E4FF)
        )
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Outlined.PauseCircle else Icons.Outlined.PlayCircle,
            modifier = Modifier.size(64.dp),
            contentDescription = "PlayPause"
        )
    }
}
