package com.randomfilm.purplemusic20

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class MusicWidgetReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Au lieu de se connecter (ce qui fait planter), on envoie juste l'ordre au Service
        val commandIntent = Intent(context, MusicService::class.java).apply {
            action = intent.action // "PLAY_PAUSE", "NEXT", "PREV"
        }

        // On utilise startForegroundService pour être sûr que le service réponde même si l'app est en fond
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(commandIntent)
            } else {
                context.startService(commandIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}