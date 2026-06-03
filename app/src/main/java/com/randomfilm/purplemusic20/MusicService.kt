package com.randomfilm.purplemusic20

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.net.URL

class MusicService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null

    // Pour gérer la boucle de mise à jour
    private var progressJob: Job? = null
    private var lastCoverUrl: String = ""

    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this).build()
        mediaSession = MediaSession.Builder(this, player!!).build()

        player?.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                // Nouvelle musique : on met à jour tout (image comprise)
                updateWidget(forceImageUpdate = true)
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    startProgressLoop()
                } else {
                    stopProgressLoop()
                }
                updateWidget(forceImageUpdate = false) // Juste màj état bouton
            }

            override fun onPlaybackStateChanged(state: Int) {
                updateWidget(forceImageUpdate = false)
            }

            override fun onShuffleModeEnabledChanged(s: Boolean) { updateWidget(false) }
            override fun onRepeatModeChanged(r: Int) { updateWidget(false) }
        })
    }

    // Lance une mise à jour toutes les secondes pour la barre de progression
    private fun startProgressLoop() {
        progressJob?.cancel()
        progressJob = CoroutineScope(Dispatchers.Main).launch {
            while (player?.isPlaying == true) {
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
        if (intent?.action != null) {
            Handler(Looper.getMainLooper()).post {
                when (intent.action) {
                    "PLAY_PAUSE" -> {
                        if (player?.isPlaying == true) player?.pause() else player?.play()
                    }
                    "NEXT" -> player?.seekToNext()
                    "PREV" -> player?.seekToPrevious()
                    "SHUFFLE" -> player?.shuffleModeEnabled = !(player?.shuffleModeEnabled ?: false)
                    "REPEAT" -> {
                        val next = when (player?.repeatMode) {
                            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                            else -> Player.REPEAT_MODE_OFF
                        }
                        player?.repeatMode = next
                    }
                }
                // Petite sécurité pour l'UI
                Handler(Looper.getMainLooper()).postDelayed({ updateWidget(false) }, 100)
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun updateWidget(forceImageUpdate: Boolean) {
        // Exécution sur le Main Thread (Obligatoire pour lire ExoPlayer)
        Handler(Looper.getMainLooper()).post {
            if (player == null) return@post

            val title = player?.currentMediaItem?.mediaMetadata?.title?.toString() ?: "Purple Music"
            val artist = player?.currentMediaItem?.mediaMetadata?.artist?.toString() ?: "Prêt à écouter"
            val isPlaying = player?.isPlaying ?: false
            val shuffle = player?.shuffleModeEnabled ?: false
            val repeat = player?.repeatMode ?: Player.REPEAT_MODE_OFF
            val currentCoverUrl = player?.currentMediaItem?.mediaMetadata?.artworkUri?.toString() ?: ""

            val currentPos = player?.currentPosition ?: 0L
            val duration = player?.duration ?: 1L

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
        val player = mediaSession?.player
        if (player?.playWhenReady == false || player?.mediaItemCount == 0) {
            progressJob?.cancel()
            stopSelf()
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = mediaSession

    override fun onDestroy() {
        progressJob?.cancel()
        mediaSession?.run { player.release(); release(); mediaSession = null }
        super.onDestroy()
    }
}