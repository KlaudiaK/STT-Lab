package com.android.klaudiak.audioplayer.managers

sealed class AudioFileType {
    class Single(val fileName: String, val path: String) : AudioFileType()
    class Folder(val folderName: String) : AudioFileType()
}
