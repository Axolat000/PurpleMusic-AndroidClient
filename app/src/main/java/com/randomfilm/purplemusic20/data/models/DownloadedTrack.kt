package com.randomfilm.purplemusic20.data.models

data class DownloadedTrack(
    val id: String,
    val title: String,
    val artist: String,
    val genre: String,
    val filePath: String,
    val coverPath: String
)
