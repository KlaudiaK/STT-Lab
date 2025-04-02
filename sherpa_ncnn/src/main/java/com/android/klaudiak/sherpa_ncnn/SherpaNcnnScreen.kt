package com.android.klaudiak.sherpa_ncnn

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import com.android.klaudiak.audioplayer.presentation.AudioPlayerScreen
import com.android.klaudiak.audioplayer.presentation.AudioPlayerViewModel
import com.android.klaudiak.audioplayer.presentation.SaveTranscriptionContent
import com.android.klaudiak.audioplayer.presentation.SectionDividerWithText
import com.android.klaudiak.sherpa_ncnn.SherpaNcnnActivity.Companion.TAG
import com.k2fsa.sherpa.ncnn.SherpaNcnn

@Composable
fun SherpaNcnnScreen(
    viewModel: AudioPlayerViewModel,
    initModel: () -> SherpaNcnn,
    toggleRecording: (AudioPlayerViewModel) -> Unit,
    finish: () -> Unit,
    onModelChanged: (SherpaNcnn) -> Unit
) {
    val context = LocalContext.current
    val isRecording by viewModel.isRecording.collectAsState()
    val transcriptionText by viewModel.transcriptionText.collectAsState()

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Log.e(TAG, "Audio recording permission denied")
            finish()
        } else {
            Log.i(TAG, "Audio recording permission granted")
        }
    }

    LaunchedEffect(Unit) {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    LaunchedEffect(Unit) {
        Log.i(TAG, "Initializing model")
        onModelChanged(initModel())
        Log.i(TAG, "Model initialized")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AudioPlayerScreen(viewModel)

        SectionDividerWithText(text = stringResource(R.string.audio_transcription_title))

        TranscriptionWithModelContent(
            transcriptionText = transcriptionText,
            isRecording = isRecording,
            toggleRecording = { toggleRecording(viewModel) },
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = dimensionResource(R.dimen.padding_medium))
        )

        SaveTranscriptionContent(viewModel)
    }
}


@Composable
private fun TranscriptionWithModelContent(
    transcriptionText: String,
    isRecording: Boolean,
    toggleRecording: () -> Unit,
    modifier: Modifier
) {
    SelectionContainer(
        modifier
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Text(
                text = transcriptionText,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(dimensionResource(R.dimen.padding_medium))
                    .verticalScroll(rememberScrollState()),
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 22.sp
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        val buttonColors = ButtonDefaults.buttonColors(
            containerColor = if (isRecording) MaterialTheme.colorScheme.errorContainer
            else MaterialTheme.colorScheme.primaryContainer,
            contentColor = if (isRecording) MaterialTheme.colorScheme.onErrorContainer
            else MaterialTheme.colorScheme.onPrimaryContainer
        )

        Button(
            onClick = { toggleRecording() },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = buttonColors,
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 4.dp,
                pressedElevation = 8.dp
            ),
            border = BorderStroke(
                width = 1.dp,
                color = if (isRecording) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = if (isRecording) Icons.Default.Stop
                    else Icons.Default.Mic,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isRecording) stringResource(R.string.stop)
                    else stringResource(R.string.start),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}
