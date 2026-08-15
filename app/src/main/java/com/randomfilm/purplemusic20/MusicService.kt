package com.randomfilm.purplemusic20

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.audiofx.Equalizer
import android.os.Handler
import android.os.Looper
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.cast.CastPlayer
import androidx.media3.cast.SessionAvailabilityListener
import com.google.android.gms.cast.framework.CastContext
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import org.json.JSONArray
import org.json.JSONObject

class MusicService : MediaSessionService(), SharedPreferences.OnSharedPreferenceChangeListener {
    private var mediaSession: MediaSession? = null
    private var localPlayer: ExoPlayer? = null
    private var castPlayer: CastPlayer? = null
    private var currentPlayer: Player? = null

    private var equalizer: Equalizer? = null

    // Pour gérer la boucle de mise à jour
    private var progressJob: Job? = null
    private var lastCoverUrl: String = ""

    // Minuteur de sommeil
    private var sleepTimerJob: Job? = null
    private var preFadeVolume: Float? = null

    override fun onCreate() {
        super.onCreate()
        localPlayer = ExoPlayer.Builder(this).build()
        currentPlayer = localPlayer

        try {
            val castContext = CastContext.getSharedInstance(this)
            castPlayer = CastPlayer(castContext)
            castPlayer?.setSessionAvailabilityListener(object : SessionAvailabilityListener {
                override fun onCastSessionAvailable() {
                    setCurrentPlayer(castPlayer)
                }

                override fun onCastSessionUnavailable() {
                    setCurrentPlayer(localPlayer)
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }

        mediaSession = MediaSession.Builder(this, currentPlayer!!).build()

        val prefs = getSharedPreferences("purple_music_state", Context.MODE_PRIVATE)
        prefs.registerOnSharedPreferenceChangeListener(this)

        // Force la sauvegarde de l'ID audio dès la création
        prefs.edit().putInt("audio_session_id", localPlayer!!.audioSessionId).apply()
        setupEqualizer(localPlayer!!.audioSessionId)

        val playerListener = object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                // Nouvelle musique : on met à jour tout (image comprise)
                savePlayerState()
                updateWidget(forceImageUpdate = true)
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                savePlayerState()
                if (isPlaying) {
                    startProgressLoop()
                } else {
                    stopProgressLoop()
                }
                updateWidget(forceImageUpdate = false) // Juste màj état bouton
            }

            override fun onPlaybackStateChanged(state: Int) {
                savePlayerState()
                updateWidget(forceImageUpdate = false)
            }

            override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
                savePlayerState()
            }

            override fun onShuffleModeEnabledChanged(s: Boolean) { updateWidget(false) }
            override fun onRepeatModeChanged(r: Int) { updateWidget(false) }
        }
        
        localPlayer?.addListener(playerListener)
        castPlayer?.addListener(playerListener)

        localPlayer?.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                prefs.edit().putInt("audio_session_id", audioSessionId).apply()
                setupEqualizer(audioSessionId)
            }
        })
    }

    private fun setupEqualizer(audioSessionId: Int) {
        try {
            equalizer?.release()
            equalizer = null
            if (audioSessionId != 0) {
                val newEq = Equalizer(0, audioSessionId)
                equalizer = newEq
                val prefs = getSharedPreferences("purple_music_state", Context.MODE_PRIVATE)
                newEq.enabled = prefs.getBoolean("eq_enabled", false)
                val numBands = newEq.numberOfBands
                for (i in 0 until numBands) {
                    val level = prefs.getInt("eq_band_$i", newEq.getBandLevel(i.toShort()).toInt())
                    newEq.setBandLevel(i.toShort(), level.toShort())
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (sharedPreferences == null || key == null) return
        if (key == "eq_enabled") {
            try {
                equalizer?.enabled = sharedPreferences.getBoolean(key, false)
            } catch (e: Exception) { e.printStackTrace() }
        } else if (key.startsWith("eq_band_")) {
            try {
                val bandIndex = key.removePrefix("eq_band_").toShort()
                val level = sharedPreferences.getInt(key, 0).toShort()
                equalizer?.setBandLevel(bandIndex, level)
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun setCurrentPlayer(newPlayer: Player?) {
        if (newPlayer === currentPlayer || newPlayer == null) return

        val previousPlayer = currentPlayer
        val playbackPosition = previousPlayer?.currentPosition ?: 0L
        val windowIndex = previousPlayer?.currentMediaItemIndex ?: 0
        val playWhenReady = previousPlayer?.playWhenReady ?: false

        currentPlayer = newPlayer

        val mediaItems = mutableListOf<androidx.media3.common.MediaItem>()
        previousPlayer?.let {
            for (i in 0 until it.mediaItemCount) {
                mediaItems.add(it.getMediaItemAt(i))
            }
        }

        currentPlayer?.setMediaItems(mediaItems, windowIndex, playbackPosition)
        currentPlayer?.playWhenReady = playWhenReady
        currentPlayer?.prepare()

        mediaSession?.player = currentPlayer!!

        previousPlayer?.stop()
        previousPlayer?.clearMediaItems()
    }

    // Lance une mise à jour toutes les secondes pour la barre de progression
    private fun startProgressLoop() {
        progressJob?.cancel()
        progressJob = CoroutineScope(Dispatchers.Main).launch {
            while (currentPlayer?.isPlaying == true) {
                updateWidget(forceImageUpdate = false) // Màj légère (temps)
                delay(1000) // Attendre 1 seconde
            }
        }
    }

    private fun stopProgressLoop() {
        progressJob?.cancel()
        updateWidget(forceImageUpdate = false) // Màj finale pour figer l'état
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Restaurer l'état si le service vient de démarrer et que le lecteur est vide
        if (currentPlayer?.mediaItemCount == 0) {
            restorePlayerState()
        }

        if (intent?.action != null) {
            Handler(Looper.getMainLooper()).post {
                when (intent.action) {
                    "PLAY_PAUSE" -> {
                        if (currentPlayer?.isPlaying == true) currentPlayer?.pause() else currentPlayer?.play()
                    }
                    "NEXT" -> currentPlayer?.seekToNext()
                    "PREV" -> currentPlayer?.seekToPrevious()
                    "SHUFFLE" -> currentPlayer?.shuffleModeEnabled = !(currentPlayer?.shuffleModeEnabled ?: false)
                    "REPEAT" -> {
                        val next = when (currentPlayer?.repeatMode) {
                            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                            else -> Player.REPEAT_MODE_OFF
                        }
                        currentPlayer?.repeatMode = next
                    }
                    "SLEEP_TIMER" -> {
                        val minutes = intent.getIntExtra("minutes", 0)
                        startSleepTimer(minutes)
                    }
                }
                savePlayerState()
                // Petite sécurité pour l'UI
                Handler(Looper.getMainLooper()).postDelayed({ updateWidget(false) }, 100)
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun startSleepTimer(minutes: Int) {
        // Annule un minuteur précédent s'il existe
        sleepTimerJob?.cancel()
        
        // Restaure le volume s'il était en cours de diminution
        preFadeVolume?.let { 
            currentPlayer?.volume = it 
            preFadeVolume = null
        }
        
        if (minutes <= 0) return
        
        sleepTimerJob = CoroutineScope(Dispatchers.Main).launch {
            val totalMs = minutes * 60 * 1000L
            val fadeOutDuration = 30 * 1000L // Fondu sur 30 secondes
            
            if (totalMs > fadeOutDuration) {
                delay(totalMs - fadeOutDuration)
                preFadeVolume = currentPlayer?.volume ?: 1.0f
                val steps = 30
                for (i in steps downTo 1) {
                    if (currentPlayer?.isPlaying != true) break
                    currentPlayer?.volume = (preFadeVolume ?: 1.0f) * (i.toFloat() / steps)
                    delay(fadeOutDuration / steps)
                }
            } else {
                delay(totalMs)
            }
            
            currentPlayer?.pause()
            preFadeVolume?.let { currentPlayer?.volume = it } // Remet le volume normal une fois en pause
            preFadeVolume = null
            sleepTimerJob = null
        }
    }

    private fun savePlayerState() {
        val player = currentPlayer ?: return
        val prefs = getSharedPreferences("purple_music_state", Context.MODE_PRIVATE)
        try {
            val array = JSONArray()
            // Limiter à 100 pour éviter de saturer la mémoire flash
            val count = minOf(player.mediaItemCount, 100)
            for (i in 0 until count) {
                val item = player.getMediaItemAt(i)
                val obj = JSONObject()
                obj.put("id", item.mediaId)
                obj.put("uri", item.localConfiguration?.uri?.toString() ?: continue)
                obj.put("title", item.mediaMetadata.title?.toString() ?: "")
                obj.put("artist", item.mediaMetadata.artist?.toString() ?: "")
                obj.put("art", item.mediaMetadata.artworkUri?.toString() ?: "")
                array.put(obj)
            }
            prefs.edit()
                .putString("queue", array.toString())
                .putInt("index", player.currentMediaItemIndex)
                .putLong("position", player.currentPosition)
                .apply()
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun restorePlayerState() {
        val player = currentPlayer ?: return
        if (player.mediaItemCount > 0) return
        val prefs = getSharedPreferences("purple_music_state", Context.MODE_PRIVATE)
        try {
            val queueStr = prefs.getString("queue", null) ?: return
            val array = JSONArray(queueStr)
            val items = mutableListOf<androidx.media3.common.MediaItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val artUrl = obj.optString("art", "")
                val mediaItem = androidx.media3.common.MediaItem.Builder()
                    .setMediaId(obj.getString("id"))
                    .setUri(android.net.Uri.parse(obj.getString("uri")))
                    .setMediaMetadata(
                        androidx.media3.common.MediaMetadata.Builder()
                            .setTitle(obj.optString("title", ""))
                            .setArtist(obj.optString("artist", ""))
                            .setArtworkUri(if (artUrl.isNotEmpty()) android.net.Uri.parse(artUrl) else null)
                            .build()
                    ).build()
                items.add(mediaItem)
            }
            if (items.isNotEmpty()) {
                val index = prefs.getInt("index", 0).coerceIn(0, items.size - 1)
                val position = prefs.getLong("position", 0L)
                player.setMediaItems(items, index, position)
                player.prepare()
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun updateWidget(forceImageUpdate: Boolean) {
        // Exécution sur le Main Thread (Obligatoire pour lire ExoPlayer)
        Handler(Looper.getMainLooper()).post {
            if (currentPlayer == null) return@post

            val title = currentPlayer?.currentMediaItem?.mediaMetadata?.title?.toString() ?: getString(R.string.widget_default_title)
            val artist = currentPlayer?.currentMediaItem?.mediaMetadata?.artist?.toString() ?: getString(R.string.player_ready_to_listen)
            val isPlaying = currentPlayer?.isPlaying ?: false
            val shuffle = currentPlayer?.shuffleModeEnabled ?: false
            val repeat = currentPlayer?.repeatMode ?: Player.REPEAT_MODE_OFF
            val currentCoverUrl = currentPlayer?.currentMediaItem?.mediaMetadata?.artworkUri?.toString() ?: ""

            val currentPos = currentPlayer?.currentPosition ?: 0L
            val duration = currentPlayer?.duration ?: 1L

            val context = this@MusicService

            // On lance le travail de fond (IO) pour Glance
            CoroutineScope(Dispatchers.IO).launch {
                var coverPath = ""

                // On ne télécharge l'image que si elle a changé ou si on force la màj
                if (currentCoverUrl.isNotEmpty() && (currentCoverUrl != lastCoverUrl || forceImageUpdate)) {
                    try {
                        val fileName = "widget_cover_${currentCoverUrl.hashCode()}.png"
                        val file = File(context.cacheDir, fileName)

                        if (!file.exists()) {
                            val bitmap = BitmapFactory.decodeStream(URL(currentCoverUrl).openConnection().getInputStream())
                            FileOutputStream(file).use { out ->
                                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                            }
                        }
                        coverPath = file.absolutePath
                        lastCoverUrl = currentCoverUrl // Mémoriser l'URL traitée
                    } catch (e: Exception) { e.printStackTrace() }
                } else if (currentCoverUrl.isNotEmpty()) {
                    // Si l'URL est la même, on reprend le chemin existant sans retélécharger
                    val fileName = "widget_cover_${currentCoverUrl.hashCode()}.png"
                    val file = File(context.cacheDir, fileName)
                    if (file.exists()) coverPath = file.absolutePath
                }

                val manager = GlanceAppWidgetManager(context)
                val glanceIds = manager.getGlanceIds(PurpleWidget::class.java)

                glanceIds.forEach { glanceId ->
                    updateAppWidgetState(context, glanceId) { prefs ->
                        prefs[WidgetKeys.title] = title
                        prefs[WidgetKeys.artist] = artist
                        prefs[WidgetKeys.isPlaying] = isPlaying
                        prefs[WidgetKeys.shuffle] = shuffle
                        prefs[WidgetKeys.repeat] = repeat
                        prefs[WidgetKeys.currentPos] = currentPos
                        prefs[WidgetKeys.duration] = duration

                        // Ne mettre à jour le chemin de l'image que si on en a un nouveau valide
                        if (coverPath.isNotEmpty()) {
                            prefs[WidgetKeys.coverPath] = coverPath
                        }
                    }
                    PurpleWidget().update(context, glanceId)
                }
            }
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Arrêter le service si l'app est tuée et qu'on ne joue pas
        savePlayerState()
        if (currentPlayer?.playWhenReady == false || currentPlayer?.mediaItemCount == 0) {
            progressJob?.cancel()
            stopSelf()
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = mediaSession

    override fun onDestroy() {
        savePlayerState()
        getSharedPreferences("purple_music_state", Context.MODE_PRIVATE).unregisterOnSharedPreferenceChangeListener(this)
        try { equalizer?.release() } catch(e: Exception){}
        progressJob?.cancel()
        localPlayer?.release()
        castPlayer?.release()
        mediaSession?.run { release(); mediaSession = null }
        super.onDestroy()
    }
}