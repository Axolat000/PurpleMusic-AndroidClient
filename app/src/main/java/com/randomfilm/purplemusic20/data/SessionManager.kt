package com.randomfilm.purplemusic20.data

import android.content.Context

class SessionManager(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("purple", Context.MODE_PRIVATE)

    fun getServerUrl(): String = prefs.getString("server_url", "") ?: ""
    fun saveServerUrl(url: String) = prefs.edit().putString("server_url", url).apply()
    fun hasServerUrl(): Boolean = getServerUrl().isNotEmpty()

    fun saveUser(id: Int, n: String, p: String, adm: Boolean) =
        prefs.edit().putInt("id", id).putString("n", n).putString("p", p).putBoolean("adm", adm).apply()

    fun getUserId() = prefs.getInt("id", -1)
    fun getUsername() = prefs.getString("n", "") ?: ""
    fun getPassword() = prefs.getString("p", "") ?: ""
    fun isAdmin() = prefs.getBoolean("adm", false)

    fun logout() = prefs.edit().remove("id").remove("n").remove("p").remove("adm").apply()

    fun getHiddenGenres(): Set<String> = prefs.getStringSet("hidden_genres", emptySet()) ?: emptySet()
    fun saveHiddenGenres(genres: Set<String>) = prefs.edit().putStringSet("hidden_genres", genres).apply()

    fun saveVolume(vol: Float) = prefs.edit().putFloat("app_volume", vol).apply()
    fun getVolume(): Float = prefs.getFloat("app_volume", 1.0f)

    fun saveSortMode(mode: String) = prefs.edit().putString("sort_mode", mode).apply()
    fun getSortMode(): String = prefs.getString("sort_mode", "date_desc") ?: "date_desc"

    fun isCoverCacheEnabled(): Boolean = prefs.getBoolean("cover_cache", true)
    fun setCoverCacheEnabled(enabled: Boolean) = prefs.edit().putBoolean("cover_cache", enabled).apply()

    fun isDynamicThemeEnabled(): Boolean = prefs.getBoolean("dynamic_theme", false)
    fun setDynamicThemeEnabled(enabled: Boolean) = prefs.edit().putBoolean("dynamic_theme", enabled).apply()

    // App-wide color preset (Violet/Amoled/Midnight/Forest/Crimson) — distinct from
    // isDynamicThemeEnabled above, which controls the now-playing album-art accent color.
    fun getThemePreset(): String = prefs.getString("theme_preset", "violet") ?: "violet"
    fun saveThemePreset(preset: String) = prefs.edit().putString("theme_preset", preset).apply()

    // Material You (Android 12+ wallpaper-based dynamic color) opt-in for the app-wide theme.
    fun isMaterialYouEnabled(): Boolean = prefs.getBoolean("material_you_enabled", false)
    fun setMaterialYouEnabled(enabled: Boolean) = prefs.edit().putBoolean("material_you_enabled", enabled).apply()
}
