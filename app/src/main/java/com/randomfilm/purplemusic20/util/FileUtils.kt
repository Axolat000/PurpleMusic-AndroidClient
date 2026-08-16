package com.randomfilm.purplemusic20.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream

fun uriToFile(uri: Uri, context: Context, isAudio: Boolean = false): File {
    var fileName = "temp_${System.currentTimeMillis()}." + (if (isAudio) "mp3" else "jpg")
    try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1 && cursor.moveToFirst()) {
                val realName = cursor.getString(nameIndex)
                if (!realName.isNullOrEmpty()) fileName = realName
            }
        }
    } catch (e: Exception) { e.printStackTrace() }
    val file = File(context.cacheDir, fileName)
    context.contentResolver.openInputStream(uri)?.use { input ->
        FileOutputStream(file).use { output -> input.copyTo(output) }
    }
    return file
}
