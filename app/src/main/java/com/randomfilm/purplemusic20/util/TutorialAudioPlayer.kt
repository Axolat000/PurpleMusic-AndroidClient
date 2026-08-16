package com.randomfilm.purplemusic20.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool

// Musique de fond + SFX du tutoriel d'accueil. Les ressources sont cherchées par NOM
// (res/raw/tutorial_bgm.*, res/raw/sfx_step.*) plutôt que par référence R.raw directe :
// tant qu'elles sont absentes du projet, tout reste silencieux sans planter et sans
// bloquer la compilation — dès qu'un fichier de ce nom est ajouté dans res/raw/, il est
// pris en compte automatiquement, sans changement de code.
class TutorialAudioPlayer(private val context: Context) {
    private var bgmPlayer: MediaPlayer? = null
    private var soundPool: SoundPool? = null
    private var sfxStepId: Int = 0
    private var sfxLoaded = false

    private fun rawResId(name: String): Int =
        context.resources.getIdentifier(name, "raw", context.packageName)

    fun init() {
        // USAGE_MEDIA (pas USAGE_ASSISTANCE_SONIFICATION) : ce dernier route vers le flux
        // "sons système", coupé par défaut sur beaucoup de téléphones (réglage "sons tactiles")
        // et indépendant du volume média — ce qui rendait le SFX inaudible même quand la
        // musique de fond (flux média, comme n'importe quelle lecture audio) fonctionnait.
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val pool = SoundPool.Builder().setMaxStreams(2).setAudioAttributes(attrs).build()
        soundPool = pool
        val id = rawResId("sfx_step")
        if (id != 0) {
            try {
                pool.setOnLoadCompleteListener { _, sampleId, status -> if (sampleId == sfxStepId && status == 0) sfxLoaded = true }
                sfxStepId = pool.load(context, id, 1)
            } catch (e: Exception) { /* pas de SFX, tant pis */ }
        }
    }

    fun startBgm(volume: Float) {
        if (bgmPlayer != null) { setVolume(volume); return }
        val id = rawResId("tutorial_bgm")
        if (id == 0) return
        try {
            bgmPlayer = MediaPlayer.create(context, id)?.apply {
                isLooping = true
                setVolume(volume, volume)
                start()
            }
        } catch (e: Exception) {
            bgmPlayer = null
        }
    }

    fun setVolume(volume: Float) {
        try { bgmPlayer?.setVolume(volume, volume) } catch (e: Exception) { }
    }

    fun playStepSfx() {
        if (sfxLoaded && sfxStepId != 0) {
            try { soundPool?.play(sfxStepId, 1f, 1f, 0, 0, 1f) } catch (e: Exception) { }
        }
    }

    fun release() {
        try { bgmPlayer?.stop() } catch (e: Exception) { }
        bgmPlayer?.release()
        bgmPlayer = null
        soundPool?.release()
        soundPool = null
    }
}
