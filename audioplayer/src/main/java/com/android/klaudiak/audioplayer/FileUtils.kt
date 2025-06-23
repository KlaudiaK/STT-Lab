package com.android.klaudiak.audioplayer

import android.os.Environment
import java.io.File

object FileUtils {

    fun getExternalDownloadFolderPath(folderName: String): String = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
        folderName
    ).path
}
