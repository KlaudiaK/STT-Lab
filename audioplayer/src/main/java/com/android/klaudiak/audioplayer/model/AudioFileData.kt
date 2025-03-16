package com.android.klaudiak.audioplayer.model

data class AudioFileData(
    val filename: String,
    val length: Long,
    val transcription: String? = null,
    val transcriptionTime: Long? = null
)
