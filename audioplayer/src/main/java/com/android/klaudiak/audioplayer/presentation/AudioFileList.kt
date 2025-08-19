package com.android.klaudiak.audioplayer.presentation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.klaudiak.audioplayer.R
import com.android.klaudiak.audioplayer.model.AudioFileData

@Composable
fun AudioFileList(
    files: List<AudioFileData>,
    currentFileName: String?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(R.dimen.padding_medium))
    ) {
        files.sortedBy { it.filename }.forEach { file ->
            val isCurrent = file.filename == currentFileName

            Text(
                text = file.filename,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = dimensionResource(R.dimen.padding_small))
                    .then(
                        if (isCurrent) Modifier.background(
                            MaterialTheme.colorScheme.primary.copy(
                                alpha = 0.1f
                            )
                        ) else Modifier
                    ),
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = if (isCurrent)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (isCurrent) FontWeight.Bold else null
                )
            )
        }
    }
}

@Composable
fun AnimatedAudioFileStrip(
    files: List<AudioFileData>,
    currentFileName: String?,
    animationTime: Int = 300
) {
    val index = files.indexOfFirst { it.filename == currentFileName }
    val displayFiles = listOfNotNull(
        files.getOrNull(index - 1),
        files.getOrNull(index),
        files.getOrNull(index + 1)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = dimensionResource(R.dimen.padding_medium)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        displayFiles.forEachIndexed { i, file ->
            val isCurrent = file.filename == currentFileName
            val scale by animateFloatAsState(
                targetValue = if (isCurrent) 1.2f else 0.9f,
                animationSpec = tween(durationMillis = animationTime), label = "scale"
            )
            val alpha by animateFloatAsState(
                targetValue = if (isCurrent) 1f else 0.5f,
                animationSpec = tween(durationMillis = animationTime), label = "alpha"
            )

            Text(
                text = file.filename,
                modifier = Modifier
                    .padding(vertical = dimensionResource(R.dimen.padding_tiny))
                    .scale(scale)
                    .alpha(alpha),
                fontSize = if (isCurrent) 20.sp else 16.sp,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
