package com.android.klaudiak.audioplayer

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import com.android.klaudiak.audioplayer.presentation.AudioPlayerViewModel.Companion.TAG

object AudioFileUtils {
    fun getAudioFileDuration(context: Context, uri: Uri): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            duration?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving duration for URI: $uri", e)
            0L
        } finally {
            retriever.release()
        }
    }
}