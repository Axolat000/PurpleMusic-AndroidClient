package com.randomfilm.purplemusic20

import android.content.Context
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

data class Track(val id: Int, val title: String, val artist: String, val cover_url: String, val stream_url: String, val uploader_id: Int, val genre: String? = "Autre", val play_count: Int? = 0)
data class Playlist(val id: Int, val name: String, val song_ids: String, val creator: String, val creator_id: Int)
data class SimpleResponse(val status: String, val message: String?, val user_id: Int?, val username: String?, val is_admin: Boolean?)

interface PurpleApi {
    @GET("api.php?action=list") suspend fun getTracks(): List<Track>
    @GET("api.php?action=playlists") suspend fun getPlaylists(): List<Playlist>

    @FormUrlEncoded @POST("api.php?action=increment_play")
    suspend fun incrementPlay(
        @Field("track_id") tid: Int,
        @Field("username") u: String,
        @Field("password") p: String
    ): SimpleResponse

    @FormUrlEncoded @POST("api.php?action=login")
    suspend fun login(@Field("username") u: String, @Field("password") p: String): SimpleResponse

    @FormUrlEncoded @POST("api.php?action=register")
    suspend fun register(@Field("username") u: String, @Field("password") p: String): SimpleResponse

    @Multipart @POST("api.php?action=upload")
    suspend fun uploadTrack(
        @Part("title") t: RequestBody?, @Part("artist") a: RequestBody?,
        @Part("username") u: RequestBody, @Part("password") p: RequestBody,
        @Part("genre") g: RequestBody?, @Part m: MultipartBody.Part, @Part c: MultipartBody.Part?
    ): SimpleResponse

    @Multipart @POST("api.php?action=edit_track")
    suspend fun editTrack(
        @Part("track_id") tid: RequestBody,
        @Part("username") u: RequestBody, @Part("password") p: RequestBody,
        @Part("title") t: RequestBody, @Part("artist") a: RequestBody, @Part("new_genre") g: RequestBody?,
        @Part c: MultipartBody.Part?
    ): SimpleResponse

    @FormUrlEncoded @POST("api.php?action=delete_track")
    suspend fun deleteTrack(
        @Field("track_id") tid: Int, @Field("username") u: String, @Field("password") p: String
    ): SimpleResponse

    @FormUrlEncoded @POST("api.php?action=playlist_create")
    suspend fun createPlaylist(
        @Field("name") n: String, @Field("username") u: String, @Field("password") p: String
    ): SimpleResponse

    @FormUrlEncoded @POST("api.php?action=playlist_mod")
    suspend fun modPlaylist(
        @Field("playlist_id") pid: Int, @Field("username") u: String, @Field("password") p: String,
        @Field("mode") mode: String, @Field("track_id") tid: Int?, @Field("new_name") nn: String?
    ): SimpleResponse
}

// ApiClient dynamique : reconstruit Retrofit quand l'URL change
object ApiClient {
    private var _baseUrl: String = ""
    private var _service: PurpleApi? = null

    val service: PurpleApi
        get() = _service ?: error("ApiClient non initialisé. Appelle ApiClient.init(url) d'abord.")

    fun init(baseUrl: String) {
        val normalized = normalizeUrl(baseUrl)
        if (normalized != _baseUrl || _service == null) {
            _baseUrl = normalized
            _service = Retrofit.Builder()
                .baseUrl(_baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(PurpleApi::class.java)
        }
    }

    fun normalizeUrl(url: String): String {
        var u = url.trim()
        if (!u.endsWith("/")) u += "/"
        return u
    }

    fun isInitialized() = _service != null
}

class SessionManager(context: Context) {
    private val prefs = context.getSharedPreferences("purple", Context.MODE_PRIVATE)

    // --- Serveur ---
    fun getServerUrl(): String = prefs.getString("server_url", "") ?: ""
    fun saveServerUrl(url: String) = prefs.edit().putString("server_url", url).apply()
    fun hasServerUrl(): Boolean = getServerUrl().isNotEmpty()

    // --- Utilisateur ---
    fun saveUser(id: Int, n: String, p: String, adm: Boolean) =
        prefs.edit().putInt("id", id).putString("n", n).putString("p", p).putBoolean("adm", adm).apply()

    fun getUserId() = prefs.getInt("id", -1)
    fun getUsername() = prefs.getString("n", "") ?: ""
    fun getPassword() = prefs.getString("p", "") ?: ""
    fun isAdmin() = prefs.getBoolean("adm", false)

    // Déconnexion : efface les credentials mais conserve le serveur
    fun logout() = prefs.edit().remove("id").remove("n").remove("p").remove("adm").apply()

    fun getHiddenGenres(): Set<String> = prefs.getStringSet("hidden_genres", emptySet()) ?: emptySet()
    fun saveHiddenGenres(genres: Set<String>) = prefs.edit().putStringSet("hidden_genres", genres).apply()

    fun saveVolume(vol: Float) = prefs.edit().putFloat("app_volume", vol).apply()
    fun getVolume(): Float = prefs.getFloat("app_volume", 1.0f)

    fun saveSortMode(mode: String) = prefs.edit().putString("sort_mode", mode).apply()
    fun getSortMode(): String = prefs.getString("sort_mode", "date_desc") ?: "date_desc"
}