package com.randomfilm.purplemusic20.util

import android.content.Context
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.randomfilm.purplemusic20.R

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
