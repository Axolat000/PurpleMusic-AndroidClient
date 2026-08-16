package com.randomfilm.purplemusic20.data

data class Track(val id: Int, val title: String, val artist: String, val cover_url: String, val stream_url: String, val uploader_id: Int, val genre: String? = "Autre", val play_count: Int? = 0)
data class Playlist(val id: Int, val name: String, val song_ids: String, val creator: String, val creator_id: Int)
data class SimpleResponse(val status: String, val message: String?, val user_id: Int?, val username: String?, val is_admin: Boolean?)

data class LrcResponse(
    val trackName: String?,
    val artistName: String?,
    val plainLyrics: String?,
    val syncedLyrics: String?
)

data class DownloadedTrack(
    val id: String,
    val title: String,
    val artist: String,
    val genre: String,
    val filePath: String,
    val coverPath: String
)
