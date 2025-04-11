package com.android.klaudiak.audioplayer

import android.content.Context
import com.android.klaudiak.audioplayer.managers.AudioPlayerManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
object AudioPlayerModule {

    @Provides
    @ViewModelScoped
    fun provideAudioPlayerManager(
        @ApplicationContext context: Context
    ): AudioPlayerManager {
        return AudioPlayerManager(
            folderName = "stt_audiofiles",
            audioFileExtensions = listOf("mp3", "wav", "flac", "ogg"),
            context = context
        )
    }
}
