package com.android.klaudiak.vosk_stt

import androidx.compose.runtime.compositionLocalOf
import com.android.klaudiak.audioplayer.presentation.AudioPlayerViewModel

object Providers {

    val LocalVoskViewModelProvider = compositionLocalOf<VoskViewModel> {
        error("No SherpaNcnnViewModel provided")
    }

    val LocalAudioPlayerViewModelProvider = compositionLocalOf<AudioPlayerViewModel> {
        error("No AudioPlayerViewModel provided")
    }
}
