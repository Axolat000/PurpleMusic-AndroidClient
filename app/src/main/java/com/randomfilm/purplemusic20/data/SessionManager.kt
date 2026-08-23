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

    // URL des CGU du serveur actuellement connecté (voir action=login/api/auth.php côté serveur) --
    // null si ce serveur n'a pas la fonctionnalité ou ne l'a pas activée. Rafraîchi à chaque login
    // réussi (pas seulement à l'inscription) pour rester à jour après un changement de serveur, et lu
    // par SettingsDialog pour afficher (ou non) le bouton "Voir les CGU".
    fun saveTermsUrl(url: String?) = prefs.edit().putString("terms_url", url).apply()
    fun getTermsUrl(): String? = prefs.getString("terms_url", null)

    fun getHiddenGenres(): Set<String> = prefs.getStringSet("hidden_genres", emptySet()) ?: emptySet()
    fun saveHiddenGenres(genres: Set<String>) = prefs.edit().putStringSet("hidden_genres", genres).apply()

    fun saveVolume(vol: Float) = prefs.edit().putFloat("app_volume", vol).apply()
    fun getVolume(): Float = prefs.getFloat("app_volume", 1.0f)

    fun saveSortMode(mode: String) = prefs.edit().putString("sort_mode", mode).apply()
    // Défaut "recommended" pour une install neuve / aucun choix explicite (le tri par recommandation
    // complète devient le tri par défaut, comme côté client web) -- mais ce défaut doit rester
    // dégradable : si le classement complet (action=recommendations&full=1) échoue au chargement
    // (serveur trop ancien/modifié sans cette fonctionnalité), MainApp.kt bascule appSortMode en
    // mémoire sur SORT_MODE_FALLBACK ci-dessous SANS jamais appeler saveSortMode, pour retenter au
    // prochain lancement plutôt que d'écraser durablement la préférence utilisateur.
    fun getSortMode(): String = prefs.getString("sort_mode", "recommended") ?: "recommended"

    companion object {
        // Ancien défaut hardcodé, conservé comme repli de session lorsque "recommended" est
        // indisponible (voir getSortMode ci-dessus et le repli dans MainApp.kt).
        const val SORT_MODE_FALLBACK = "date_desc"
    }

    fun isCoverCacheEnabled(): Boolean = prefs.getBoolean("cover_cache", true)
    fun setCoverCacheEnabled(enabled: Boolean) = prefs.edit().putBoolean("cover_cache", enabled).apply()

    fun isDynamicThemeEnabled(): Boolean = prefs.getBoolean("dynamic_theme", false)
    fun setDynamicThemeEnabled(enabled: Boolean) = prefs.edit().putBoolean("dynamic_theme", enabled).apply()

    // App-wide color preset (Violet/Amoled/Midnight/Forest/Crimson) — distinct from
    // isDynamicThemeEnabled above, which controls the now-playing album-art accent color.
    fun getThemePreset(): String = prefs.getString("theme_preset", "violet") ?: "violet"
    fun saveThemePreset(preset: String) = prefs.edit().putString("theme_preset", preset).apply()

    // Thème dynamique D'APPLICATION (distinct de isDynamicThemeEnabled ci-dessus, qui ne touche que
    // l'accent du grand lecteur) : recolore l'appli entière (fond/panneaux/texte/accent/navBg) à partir de
    // la pochette de la piste en cours, via ThemeUtils.generateAppColors() -- voir MainApp.kt, qui possède
    // déjà l'état "piste en cours" nécessaire à l'extraction.
    fun isAppDynamicThemeEnabled(): Boolean = prefs.getBoolean("app_dynamic_theme", false)
    fun setAppDynamicThemeEnabled(enabled: Boolean) = prefs.edit().putBoolean("app_dynamic_theme", enabled).apply()

    // Material You (Android 12+ wallpaper-based dynamic color) opt-in for the app-wide theme.
    fun isMaterialYouEnabled(): Boolean = prefs.getBoolean("material_you_enabled", false)
    fun setMaterialYouEnabled(enabled: Boolean) = prefs.edit().putBoolean("material_you_enabled", enabled).apply()
}
