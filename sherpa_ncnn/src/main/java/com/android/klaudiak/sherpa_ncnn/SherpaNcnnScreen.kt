package com.android.klaudiak.sherpa_ncnn

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.core.app.ActivityCompat
import com.android.klaudiak.audioplayer.presentation.AudioPlayerScreen
import com.android.klaudiak.audioplayer.presentation.AudioPlayerViewModel
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
            .padding(dimensionResource(R.dimen.padding_medium)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AudioPlayerScreen(viewModel, { toggleRecording(viewModel)})

        SelectionContainer {
            Text(
                text = transcriptionText,
                modifier = Modifier
                    .weight(2.5f)
                    .fillMaxWidth()
                    .padding(dimensionResource(R.dimen.padding_medium))
                    .verticalScroll(rememberScrollState())
            )
        }

        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.padding_medium)))

        Button(onClick = { toggleRecording(viewModel) }) {
            Text(if (isRecording) "Stop" else "Start")
        }
    }
}
