package com.randomfilm.purplemusic20.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface LrcLibApi {
    @GET("api/get")
    suspend fun getLyrics(
        @Query("track_name") trackName: String,
        @Query("artist_name") artistName: String
    ): Response<LrcResponse>
}

object LrcApiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            // LRCLib bloque désormais le User-Agent par défaut d'OkHttp (retourne 520).
            val request = chain.request().newBuilder()
                .header("User-Agent", "PurpleMusic20/1.0 (Android)")
                .build()
            chain.proceed(request)
        }
        .build()

    val service: LrcLibApi by lazy {
        Retrofit.Builder().baseUrl("https://lrclib.net/").client(client)
            .addConverterFactory(GsonConverterFactory.create()).build().create(LrcLibApi::class.java)
    }
}

data class LyricLine(val timeMs: Long, val text: String)

private val lrcRegex = Regex("""\[(\d{2}):(\d{2})\.(\d{2,3})\](.*)""")

suspend fun parseLrc(lrc: String): List<LyricLine> = withContext(Dispatchers.Default) {
    val lines = mutableListOf<LyricLine>()
    lrc.lines().forEach { line ->
        val match = lrcRegex.find(line)
        if (match != null) {
            val m = match.groupValues[1].toLong()
            val s = match.groupValues[2].toLong()
            val ms = match.groupValues[3].toLong()
            val text = match.groupValues[4].trim()
            val totalMs = m * 60000 + s * 1000 + (if (match.groupValues[3].length == 2) ms * 10 else ms)
            lines.add(LyricLine(totalMs, text))
        }
    }
    lines
}
