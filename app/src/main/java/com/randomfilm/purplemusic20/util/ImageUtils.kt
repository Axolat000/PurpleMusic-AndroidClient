package com.randomfilm.purplemusic20.util

import android.content.Context
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.randomfilm.purplemusic20.R
import com.randomfilm.purplemusic20.data.ApiClient
import com.randomfilm.purplemusic20.data.SessionManager

// URL d'une cover de playlist : servie en fichier statique direct (covers/<nom>), pas via l'action
// api.php?action=cover&q=<id> réservée aux pistes -- même convention que le site web. Passe TOUJOURS par
// ApiClient.normalizeUrl() plutôt que session.getServerUrl() brut : ce dernier peut ne pas avoir de "/"
// final (ex. "https://exemple.fr/music" saisi sans slash à l'installation), ce qui donnait une URL
// concaténée invalide ("...musiccovers/xxx.webp") -> 404 -> Coil affichait son image par défaut même pour
// une playlist ayant réellement une cover (bug signalé). Retrofit s'en sort car ApiClient.init() normalise
// déjà en interne pour son propre usage, mais ne réexposait pas cette version normalisée avant.
fun playlistCoverUrl(session: SessionManager, cover: String): String =
    ApiClient.normalizeUrl(session.getServerUrl()) + "covers/" + cover

// ─── Image Builder Helper pour le cache Coil ───────────────────────────────────
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
