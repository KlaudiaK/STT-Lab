package com.android.klaudiak.sherpa_onnx

import androidx.compose.runtime.compositionLocalOf
import com.android.klaudiak.audioplayer.presentation.AudioPlayerViewModel

object Providers {

    val LocalSherpaOnnxViewModelProvider = compositionLocalOf<SherpaOnnxViewModel> {
        error("No SherpaOnnxViewModel provided")
    }

    val LocalAudioPlayerViewModelProvider = compositionLocalOf<AudioPlayerViewModel> {
        error("No AudioPlayerViewModel provided")
    }
}
