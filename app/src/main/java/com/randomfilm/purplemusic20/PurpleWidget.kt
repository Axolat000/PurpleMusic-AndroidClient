package com.randomfilm.purplemusic20

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.media3.common.Player

class PurpleWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PurpleWidget()
}

class PurpleWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<androidx.datastore.preferences.core.Preferences>()
            val title = prefs[WidgetKeys.title] ?: "Purple Music"
            val artist = prefs[WidgetKeys.artist] ?: "Prêt à écouter"
            val isPlaying = prefs[WidgetKeys.isPlaying] ?: false
            val shuffle = prefs[WidgetKeys.shuffle] ?: false
            val repeat = prefs[WidgetKeys.repeat] ?: Player.REPEAT_MODE_OFF
            val coverPath = prefs[WidgetKeys.coverPath] ?: ""

            val pos = prefs[WidgetKeys.currentPos] ?: 0L
            val dur = prefs[WidgetKeys.duration] ?: 1L
            val progress = if(dur > 0) (pos.toFloat() / dur.toFloat()).coerceIn(0f, 1f) else 0f

            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ColorProvider(Color(0xFF1B1429)))
                    .cornerRadius(16.dp)
                    .clickable(actionRunCallback<OpenAppAction>())
            ) {
                Row(
                    modifier = GlanceModifier.fillMaxSize().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // IMAGE
                    if (coverPath.isNotEmpty()) {
                        val bitmap = BitmapFactory.decodeFile(coverPath)
                        if (bitmap != null) {
                            Image(
                                provider = ImageProvider(bitmap),
                                contentDescription = "Cover",
                                contentScale = ContentScale.Crop,
                                modifier = GlanceModifier.size(70.dp).cornerRadius(12.dp)
                            )
                        } else EmptyCover()
                    } else EmptyCover()

                    Spacer(GlanceModifier.width(12.dp))

                    // INFO + CONTROLS
                    Column(
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = GlanceModifier.fillMaxWidth()) {
                            Text(
                                text = title,
                                style = TextStyle(color = ColorProvider(Color.White), fontSize = 15.sp, fontWeight = FontWeight.Bold),
                                maxLines = 1
                            )
                            Text(
                                text = artist,
                                style = TextStyle(color = ColorProvider(Color(0xFFBB86FC)), fontSize = 12.sp),
                                maxLines = 1
                            )
                        }

                        Spacer(GlanceModifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = progress,
                            modifier = GlanceModifier.fillMaxWidth().height(4.dp).cornerRadius(2.dp),
                            color = ColorProvider(Color(0xFF8E44AD)),
                            backgroundColor = ColorProvider(Color.White.copy(alpha=0.2f))
                        )

                        Spacer(GlanceModifier.height(8.dp))

                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // SHUFFLE
                            Image(
                                provider = ImageProvider(R.drawable.ic_shuffle_widget),
                                contentDescription = "Shuffle",
                                colorFilter = ColorFilter.tint(ColorProvider(if(shuffle) Color(0xFFBB86FC) else Color.Gray)),
                                modifier = GlanceModifier.size(20.dp).clickable(actionRunCallback<ShuffleAction>())
                            )

                            Spacer(GlanceModifier.width(16.dp))

                            // PREV
                            Image(
                                provider = ImageProvider(R.drawable.ic_prev_widget),
                                contentDescription = "Prev",
                                colorFilter = ColorFilter.tint(ColorProvider(Color.White)),
                                modifier = GlanceModifier.size(28.dp).clickable(actionRunCallback<PrevAction>())
                            )

                            Spacer(GlanceModifier.width(16.dp))

                            // --- PLAY/PAUSE (CORRIGÉ AVEC ICÔNE) ---
                            Box(
                                modifier = GlanceModifier
                                    .size(44.dp)
                                    .background(ColorProvider(Color.White))
                                    .cornerRadius(22.dp)
                                    .clickable(actionRunCallback<PlayPauseAction>()),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    provider = ImageProvider(if (isPlaying) R.drawable.ic_pause_widget else R.drawable.ic_play_widget),
                                    contentDescription = "Play/Pause",
                                    colorFilter = ColorFilter.tint(ColorProvider(Color(0xFF0F0C1D))), // Icône noire sur fond blanc
                                    modifier = GlanceModifier.size(24.dp)
                                )
                            }

                            Spacer(GlanceModifier.width(16.dp))

                            // NEXT
                            Image(
                                provider = ImageProvider(R.drawable.ic_next_widget),
                                contentDescription = "Next",
                                colorFilter = ColorFilter.tint(ColorProvider(Color.White)),
                                modifier = GlanceModifier.size(28.dp).clickable(actionRunCallback<NextAction>())
                            )

                            Spacer(GlanceModifier.width(16.dp))

                            // REPEAT
                            val repeatIcon = if(repeat == Player.REPEAT_MODE_ONE) R.drawable.ic_repeat_one_widget else R.drawable.ic_repeat_widget
                            val repeatColor = if(repeat != Player.REPEAT_MODE_OFF) Color(0xFFBB86FC) else Color.Gray
                            Image(
                                provider = ImageProvider(repeatIcon),
                                contentDescription = "Repeat",
                                colorFilter = ColorFilter.tint(ColorProvider(repeatColor)),
                                modifier = GlanceModifier.size(20.dp).clickable(actionRunCallback<RepeatAction>())
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun EmptyCover() {
        Box(
            modifier = GlanceModifier.size(70.dp).background(ColorProvider(Color(0xFF2E2445))).cornerRadius(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("♫", style = TextStyle(color = ColorProvider(Color.White), fontSize = 24.sp))
        }
    }
}

object WidgetKeys {
    val title = androidx.datastore.preferences.core.stringPreferencesKey("title")
    val artist = androidx.datastore.preferences.core.stringPreferencesKey("artist")
    val isPlaying = androidx.datastore.preferences.core.booleanPreferencesKey("isPlaying")
    val shuffle = androidx.datastore.preferences.core.booleanPreferencesKey("shuffle")
    val repeat = androidx.datastore.preferences.core.intPreferencesKey("repeat")
    val coverPath = androidx.datastore.preferences.core.stringPreferencesKey("coverPath")
    val currentPos = androidx.datastore.preferences.core.longPreferencesKey("currentPos")
    val duration = androidx.datastore.preferences.core.longPreferencesKey("duration")
}

class OpenAppAction : androidx.glance.appwidget.action.ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val intent = Intent(context, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP }
        context.startActivity(intent)
    }
}
class PlayPauseAction : androidx.glance.appwidget.action.ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        context.sendBroadcast(Intent(context, MusicWidgetReceiver::class.java).apply { action = "PLAY_PAUSE" })
    }
}
class NextAction : androidx.glance.appwidget.action.ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        context.sendBroadcast(Intent(context, MusicWidgetReceiver::class.java).apply { action = "NEXT" })
    }
}
class PrevAction : androidx.glance.appwidget.action.ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        context.sendBroadcast(Intent(context, MusicWidgetReceiver::class.java).apply { action = "PREV" })
    }
}
class ShuffleAction : androidx.glance.appwidget.action.ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        context.sendBroadcast(Intent(context, MusicWidgetReceiver::class.java).apply { action = "SHUFFLE" })
    }
}
class RepeatAction : androidx.glance.appwidget.action.ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        context.sendBroadcast(Intent(context, MusicWidgetReceiver::class.java).apply { action = "REPEAT" })
    }
}