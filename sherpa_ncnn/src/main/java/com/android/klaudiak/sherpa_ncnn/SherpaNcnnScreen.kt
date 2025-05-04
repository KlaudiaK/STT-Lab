package com.android.klaudiak.sherpa_ncnn

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
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
import androidx.core.app.ActivityCompat
import com.android.klaudiak.audioplayer.presentation.AudioPlayerScreen
import com.android.klaudiak.audioplayer.presentation.SaveTranscriptionContent
import com.android.klaudiak.audioplayer.presentation.SectionDividerWithText
import com.android.klaudiak.core.presentation.TranscriptionDisplay
import com.android.klaudiak.sherpa_ncnn.Providers.LocalAudioPlayerViewModelProvider
import com.android.klaudiak.sherpa_ncnn.Providers.LocalSherpaNcnnViewModelProvider
import com.android.klaudiak.sherpa_ncnn.SherpaNcnnActivity.Companion.TAG

@Composable
fun SherpaNcnnScreen(
    finish: () -> Unit
) {
    val context = LocalContext.current
    val audioPlayerViewModel = LocalAudioPlayerViewModelProvider.current
    val sherpaNcnnViewModel = LocalSherpaNcnnViewModelProvider.current

    val isRecording by sherpaNcnnViewModel.isRecording.collectAsState()
    val transcriptionText by audioPlayerViewModel.transcriptionText.collectAsState()

    val isPlaybackComplete by audioPlayerViewModel.isPlaybackComplete.collectAsState()

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
        with(sherpaNcnnViewModel) {
            updateModel(initModel())
        }
        Log.i(TAG, "Model initialized")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(dimensionResource(R.dimen.padding_medium)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AudioPlayerScreen()

        SectionDividerWithText(text = stringResource(R.string.audio_transcription_title))

        TranscriptionDisplay(
            transcriptionText = transcriptionText,
            isRecording = isRecording,
            onToggleRecording = {
                with(audioPlayerViewModel) {
                    sherpaNcnnViewModel.toggleRecording(
                        updateTranscriptionText = {
                            updateTranscriptionText(it)
                        },
                        updateFileTranscriptionDuration = {
                            updateFileTranscriptionDuration(it)
                        },
                        updateAudioFileTranslation = {
                            updateAudioFileTranslation(it)
                        }
                    )
                }
            },
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = dimensionResource(R.dimen.padding_medium))
        )

        if (isPlaybackComplete) {
            SaveTranscriptionContent()
        }
    }
}
