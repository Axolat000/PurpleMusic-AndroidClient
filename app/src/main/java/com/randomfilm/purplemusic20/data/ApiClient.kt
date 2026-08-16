package com.randomfilm.purplemusic20.data

import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

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

object ApiClient {
    private var _baseUrl: String = ""
    private var _service: PurpleApi? = null

    // Client HTTP optimisé pour éviter les timeouts par défaut trop longs
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    val service: PurpleApi
        get() = _service ?: error("ApiClient non initialisé. Appelle ApiClient.init(url) d'abord.")

    fun init(baseUrl: String) {
        val normalized = normalizeUrl(baseUrl)
        if (normalized != _baseUrl || _service == null) {
            _baseUrl = normalized
            _service = Retrofit.Builder()
                .baseUrl(_baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(PurpleApi::class.java)
        }
    }

    fun normalizeUrl(url: String): String {
        var u = url.trim()
        if (u.isNotEmpty() && !u.endsWith("/")) u += "/"
        return u
    }

    fun isInitialized() = _service != null
}
