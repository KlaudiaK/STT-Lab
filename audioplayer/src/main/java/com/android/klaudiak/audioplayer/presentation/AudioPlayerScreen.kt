package com.android.klaudiak.audioplayer.presentation

import android.Manifest
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.android.klaudiak.audioplayer.R
import com.android.klaudiak.audioplayer.model.AudioFileData

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
            .wrapContentHeight()
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Header()

        PlayAudioButton(isPlaying) {
            isPlaying = !isPlaying
        }

        PlayFromBeginningButton {
            viewModel.moveSeekToStart()
            isPlaying = !isPlaying
        }
    }
}

@Composable
private fun Header() {
    Text(
        text = stringResource(R.string.audio_player_header),
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(bottom = dimensionResource(R.dimen.padding_small))
    )
}

@Composable
private fun PlayAudioButton(isPlaying: Boolean, onClick: () -> Unit) {
    val buttonSize = 72.dp
    val iconSize = 36.dp
    val borderWidth = 2.dp

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(buttonSize)
    ) {
        Card(
            modifier = Modifier
                .size(buttonSize)
                .clickable { onClick() },
            shape = CircleShape,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 6.dp,
                pressedElevation = 12.dp
            ),
            border = BorderStroke(
                width = borderWidth,
                color = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                if (isPlaying) {
                    Icon(
                        imageVector = Icons.Default.Pause,
                        contentDescription = "Pause",
                        modifier = Modifier.size(iconSize),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        modifier = Modifier.size(iconSize),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun SaveTranscriptionContent(
    viewModel: AudioPlayerViewModel = hiltViewModel()
) {
    var filename by remember { mutableStateOf("") }
    val files by viewModel.files.collectAsState()

    Column {
        SectionDividerWithText(stringResource(R.string.export_section_title))
        TranscriptionsTextInput(filename = filename) { filename = it }
        SaveTranscriptionsToFileButton(viewModel, files, filename)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TranscriptionsTextInput(filename: String, onFilenameChanged: (String) -> Unit) {
    OutlinedTextField(
        value = filename,
        onValueChange = { onFilenameChanged(it) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Filename", color = MaterialTheme.colorScheme.onSurfaceVariant) },
        placeholder = {
            Text(
                "Enter filename without extension",
                color = MaterialTheme.colorScheme.outline
            )
        },
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        colors = TextFieldDefaults.outlinedTextFieldColors(
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            cursorColor = MaterialTheme.colorScheme.primary
        )
    )
}


@Composable
private fun SaveTranscriptionsToFileButton(
    viewModel: AudioPlayerViewModel,
    files: List<AudioFileData>,
    filename: String
) {
    Button(
        onClick = { viewModel.exportTranscriptionsToTxt(files, filename) },
        modifier = Modifier.fillMaxWidth(),
        enabled = filename.isNotBlank(),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        )
    ) {
        Icon(
            imageVector = Icons.Default.Save,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(stringResource(R.string.save_transcription))
    }
}

@Composable
fun PlayFromBeginningButton(onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(0.8f),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Icon(
            imageVector = Icons.Default.RestartAlt,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(stringResource(R.string.play_from_beginning))
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

@Composable
fun SectionDividerWithText(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = dimensionResource(R.dimen.padding_medium)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outline
        )
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.padding_small)),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outline
        )
    }
}
