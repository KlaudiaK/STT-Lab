package com.android.klaudiak.core.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.klaudiak.core.R

@Composable
fun TranscriptionDisplay(
    transcriptionText: String,
    isRecording: Boolean,
    onToggleRecording: () -> Unit,
    modifier: Modifier = Modifier
) {

    val scrollState = rememberScrollState()

    Column(modifier = modifier.scrollable(scrollState, Orientation.Vertical)) {
        TranscriptionCard(transcriptionText = transcriptionText)
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.padding_medium)))
        RecordingButton(isRecording = isRecording, onToggleRecording = onToggleRecording)
    }
}

@Composable
fun TranscriptionCard(transcriptionText: String, modifier: Modifier = Modifier) {
    SelectionContainer(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 150.dp)
    ) {
        Card(
            modifier = Modifier
                .padding(vertical = dimensionResource(R.dimen.padding_small)),
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
}

@Composable
fun RecordingButton(
    isRecording: Boolean,
    onToggleRecording: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                vertical = dimensionResource(R.dimen.padding_medium)
            ),
        contentAlignment = Alignment.Center
    ) {
        val buttonColors = ButtonDefaults.buttonColors(
            containerColor = if (isRecording) MaterialTheme.colorScheme.errorContainer
            else MaterialTheme.colorScheme.primaryContainer,
            contentColor = if (isRecording) MaterialTheme.colorScheme.onErrorContainer
            else MaterialTheme.colorScheme.onPrimaryContainer
        )

        Button(
            onClick = onToggleRecording,
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensionResource(R.dimen.default_button_height)),
            colors = buttonColors,
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = dimensionResource(R.dimen.default_button_elevation),
                pressedElevation = dimensionResource(R.dimen.pressed_button_elevation)
            ),
            border = BorderStroke(
                width = dimensionResource(R.dimen.default_button_border_width),
                color = if (isRecording) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = if (isRecording) stringResource(R.string.stop) else stringResource(
                        R.string.start
                    ),
                    modifier = Modifier
                        .size(dimensionResource(R.dimen.icon_medium_size)),
                )
                Spacer(modifier = Modifier.width(dimensionResource(R.dimen.padding_small)))
                Text(
                    text = if (isRecording) stringResource(R.string.stop)
                    else stringResource(R.string.start),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}
