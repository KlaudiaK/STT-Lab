package com.android.klaudiak.sherpa_onnx

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.core.app.ActivityCompat
import com.android.klaudiak.audioplayer.presentation.AudioPlayerScreen
import com.android.klaudiak.audioplayer.presentation.SaveTranscriptionContent
import com.android.klaudiak.audioplayer.presentation.SectionDividerWithText
import com.android.klaudiak.core.presentation.TranscriptionDisplay
import com.android.klaudiak.sherpa_onnx.Providers.LocalAudioPlayerViewModelProvider
import com.android.klaudiak.sherpa_onnx.Providers.LocalSherpaOnnxViewModelProvider
import com.android.klaudiak.sherpa_onnx.SherpaOnnxActivity.Companion.TAG

@Composable
fun SherpaOnnxScreen(
    finish: () -> Unit
) {
    val context = LocalContext.current
    val audioPlayerViewModel = LocalAudioPlayerViewModelProvider.current
    val sherpaOnnxViewModel = LocalSherpaOnnxViewModelProvider.current

    val isRecording by sherpaOnnxViewModel.isRecording.collectAsState()
    val transcriptionText by audioPlayerViewModel.transcriptionText.collectAsState()

    val isPlaybackComplete by audioPlayerViewModel.isPlaybackComplete.collectAsState()

    val scrollState = rememberScrollState()

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

        Log.i(TAG, "Initializing model")
        with(sherpaOnnxViewModel) {
            updateModel(initModel())
        }
        Log.i(TAG, "Model initialized")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .scrollable(scrollState, Orientation.Vertical)
            .padding(dimensionResource(R.dimen.padding_medium)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = stringResource(R.string.sherpa_onnx_stt_title),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = MaterialTheme.typography.headlineLarge.fontWeight,
            modifier = Modifier.padding(bottom = dimensionResource(R.dimen.padding_medium))
        )

        AudioPlayerScreen()

        SectionDividerWithText(text = stringResource(com.android.klaudiak.audioplayer.R.string.audio_transcription_title))


        TranscriptionDisplay(
            transcriptionText = transcriptionText,
            isRecording = isRecording,
            onToggleRecording = {
                with(audioPlayerViewModel) {
                    sherpaOnnxViewModel.toggleRecording(
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