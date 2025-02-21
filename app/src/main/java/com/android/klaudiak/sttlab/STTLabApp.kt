package com.android.klaudiak.sttlab

import android.app.Application
import com.android.klaudiak.audioplayer.AudioPlayerViewModel
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class STTLabApp : Application() {
    lateinit var audioPlayerViewModel: AudioPlayerViewModel

    override fun onCreate() {
        super.onCreate()
        audioPlayerViewModel = AudioPlayerViewModel(this)
    }
}