package com.randomfilm.purplemusic20.util

/**
 * There's no dedicated artist entity in the backend -- [com.randomfilm.purplemusic20.data.Track.artist]
 * is a free-text string that can hold one or several names ("Drake, Travis Scott", "Coldplay & Rihanna").
 * These helpers derive per-artist grouping client-side from that string, matching the splitting rules
 * used by the web app (js/library.js) so artist pages agree across platforms.
 */
object ArtistUtils {
    // Order matters: longer/more-specific separators must be tried before shorter ones that could be a
    // prefix of them (e.g. "&amp;" before "&"). Word separators require whitespace on both sides so names
    // like "Sixx", "Andy", "AC/DC", "Foxx" are never split. Both "&amp;" and raw "&" are covered since
    // stored artist values already carry htmlspecialchars() entities from the server (same convention as
    // the web app), so either form can appear depending on how the field was originally saved.
    private val SPLIT_REGEX = Regex(
        "\\s*,\\s*|\\s*&amp;\\s*|\\s*&\\s*|\\s+feat\\.?\\s+|\\s+ft\\.?\\s+|\\s+featuring\\s+|\\s+vs\\.?\\s+|\\s+x\\s+|\\s+et\\s+",
        RegexOption.IGNORE_CASE
    )

    fun splitArtistNames(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split(SPLIT_REGEX).map { it.trim() }.filter { it.isNotEmpty() }
    }

    /** Case-insensitive match of a single artist name against a track's raw artist field. */
    fun trackBelongsToArtist(trackArtist: String, artistName: String): Boolean {
        val target = artistName.trim().lowercase()
        return splitArtistNames(trackArtist).any { it.lowercase() == target }
    }
}
