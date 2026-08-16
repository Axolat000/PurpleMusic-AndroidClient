package com.randomfilm.purplemusic20.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object DownloadManager {
    fun getDownloadDir(context: Context): File {
        val dir = File(context.filesDir, "downloads")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }
    fun getCoversDir(context: Context): File {
        val dir = File(context.filesDir, "covers")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }
    fun isDownloaded(context: Context, trackId: Int): Boolean {
        val prefs = context.getSharedPreferences("purple_dl", Context.MODE_PRIVATE)
        return prefs.contains("track_$trackId")
    }
    fun getDownloadedTracks(context: Context): List<DownloadedTrack> {
        val prefs = context.getSharedPreferences("purple_dl", Context.MODE_PRIVATE)
        return prefs.all.keys.filter { it.startsWith("track_") }.mapNotNull { key ->
            val value = prefs.getString(key, null) ?: return@mapNotNull null
            val parts = value.split("|")
            if (parts.size < 5) return@mapNotNull null
            DownloadedTrack(parts[0], parts[1], parts[2], parts[3], parts[4], if (parts.size > 5) parts[5] else "")
        }
    }
    fun saveDownloadedTrack(context: Context, track: Track, filePath: String, coverPath: String) {
        val prefs = context.getSharedPreferences("purple_dl", Context.MODE_PRIVATE)
        prefs.edit().putString("track_${track.id}", "${track.id}|${track.title}|${track.artist}|${track.genre ?: "Autre"}|$filePath|$coverPath").apply()
    }
    fun deleteDownloadedTrack(context: Context, trackId: String) {
        val prefs = context.getSharedPreferences("purple_dl", Context.MODE_PRIVATE)
        val value = prefs.getString("track_$trackId", null)
        if (value != null) {
            val parts = value.split("|")
            if (parts.size >= 5) File(parts[4]).delete()
            if (parts.size >= 6 && parts[5].isNotEmpty()) File(parts[5]).delete()
        }
        prefs.edit().remove("track_$trackId").apply()
    }
    suspend fun downloadTrack(context: Context, track: Track, onProgress: (Int) -> Unit): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val dlDir = getDownloadDir(context)
                val audioFile = File(dlDir, "track_${track.id}.mp3")
                val url = java.net.URL(track.stream_url)
                val connection = url.openConnection().apply { connect() }
                val totalBytes = connection.contentLength
                var downloadedBytes = 0
                connection.getInputStream().use { input ->
                    FileOutputStream(audioFile).use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead
                            if (totalBytes > 0) onProgress((downloadedBytes * 100 / totalBytes).coerceIn(0, 99))
                        }
                    }
                }
                var coverPath = ""
                if (track.cover_url.isNotEmpty()) {
                    try {
                        val coverFile = File(getCoversDir(context), "cover_${track.id}.jpg")
                        java.net.URL(track.cover_url).openStream().use { input ->
                            FileOutputStream(coverFile).use { output -> input.copyTo(output) }
                        }
                        coverPath = coverFile.absolutePath
                    } catch (e: Exception) { e.printStackTrace() }
                }
                saveDownloadedTrack(context, track, audioFile.absolutePath, coverPath)
                onProgress(100)
                true
            } catch (e: Exception) { false }
        }
    }
}
