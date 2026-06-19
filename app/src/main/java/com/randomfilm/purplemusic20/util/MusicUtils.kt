package com.randomfilm.purplemusic20.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.randomfilm.purplemusic20.R
import com.randomfilm.purplemusic20.data.models.LyricLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

private val lrcRegex = Regex("""\[(\d{2}):(\d{2})\.(\d{2,3})\](.*)""")

suspend fun parseLrc(lrc: String): List<LyricLine> = withContext(Dispatchers.Default) {
    val lines = mutableListOf<LyricLine>()
    lrc.lines().forEach { line ->
        val match = lrcRegex.find(line)
        if (match != null) {
            val m = match.groupValues[1].toLong()
            val s = match.groupValues[2].toLong()
            val ms = match.groupValues[3].toLong()
            val text = match.groupValues[4].trim()
            val totalMs = m * 60000 + s * 1000 + (if (match.groupValues[3].length == 2) ms * 10 else ms)
            lines.add(LyricLine(totalMs, text))
        }
    }
    lines
}

fun buildImageRequest(context: Context, url: Any, cacheEnabled: Boolean): ImageRequest {
    val policy = if (cacheEnabled) CachePolicy.ENABLED else CachePolicy.DISABLED
    return ImageRequest.Builder(context)
        .data(url)
        .diskCachePolicy(policy)
        .memoryCachePolicy(policy)
        .crossfade(true)
        .error(R.drawable.default_cover)
        .fallback(R.drawable.default_cover)
        .placeholder(R.drawable.default_cover)
        .build()
}

fun formatTime(ms: Long) = "%d:%02d".format(ms / 1000 / 60, ms / 1000 % 60)
