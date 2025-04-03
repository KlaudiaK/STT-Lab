package com.android.klaudiak.sherpa_ncnn

import androidx.compose.runtime.compositionLocalOf
import com.android.klaudiak.audioplayer.presentation.AudioPlayerViewModel

object Providers {

    val LocalSherpaNcnnViewModelProvider = compositionLocalOf<SherpaNcnnViewModel> {
        error("No SherpaNcnnViewModel provided")
    }

    val LocalAudioPlayerViewModelProvider = compositionLocalOf<AudioPlayerViewModel> {
        error("No AudioPlayerViewModel provided")
    }
}
