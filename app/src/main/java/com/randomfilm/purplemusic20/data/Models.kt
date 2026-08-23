package com.randomfilm.purplemusic20.data

data class Track(val id: Int, val title: String, val artist: String, val cover_url: String, val stream_url: String, val uploader_id: Int, val genre: String? = "Autre", val play_count: Int? = 0, val like_count: Int? = 0, val album: String? = null)
data class Playlist(val id: Int, val name: String, val song_ids: String, val creator: String, val creator_id: Int, val cover: String? = null)
// terms_accepted / terms_url ne sont renvoyés que par les serveurs à jour supportant les CGU
// (action=login) -- absents (null) sur un ancien serveur, à traiter comme "pas de CGU à afficher".
data class SimpleResponse(val status: String, val message: String?, val user_id: Int?, val username: String?, val is_admin: Boolean?, val terms_accepted: Boolean? = null, val terms_url: String? = null)

// Réponse de action=report_listen -- "counted" indique si le serveur a validé le seuil de 10s
// (il revalide lui-même côté serveur, ne fait pas confiance au client), non consommé côté app
// pour l'instant mais utile de le typer correctement.
data class ReportListenResponse(val status: String?, val counted: Boolean?)

// Réponse de action=toggle_like
data class ToggleLikeResponse(val status: String?, val liked: Boolean?, val like_count: Int?)

// Réponse de action=my_likes -- juste les IDs des morceaux likés par l'utilisateur connecté
data class MyLikesResponse(val liked_ids: List<Int>?)

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
