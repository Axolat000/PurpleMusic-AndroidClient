package com.randomfilm.purplemusic20

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

// Second widget, volontairement minimal : juste la pochette + play/pause (pas de titre/progression/
// shuffle/repeat comme PurpleWidget) pour les écrans d'accueil où seule une petite case est disponible.
// Réutilise le même état partagé (WidgetKeys) et les mêmes Actions (PlayPauseAction, etc. -- voir
// PurpleWidget.kt) : les deux widgets reflètent toujours la même lecture en cours, juste avec un rendu
// différent. Choisi comme "nouveau widget" plutôt qu'une variante de taille du même layout, pour donner un
// vrai second choix distinct dans le sélecteur de widgets.
class PurpleWidgetCompactReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PurpleWidgetCompact()
}

class PurpleWidgetCompact : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<androidx.datastore.preferences.core.Preferences>()
            val title = prefs[WidgetKeys.title] ?: context.getString(R.string.widget_default_title)
            val isPlaying = prefs[WidgetKeys.isPlaying] ?: false
            val coverPath = prefs[WidgetKeys.coverPath] ?: ""

            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ColorProvider(Color(0xFF1B1429)))
                    .cornerRadius(16.dp)
                    .clickable(actionRunCallback<OpenAppAction>())
            ) {
                Row(
                    modifier = GlanceModifier.fillMaxSize().padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (coverPath.isNotEmpty()) {
                        val bitmap = BitmapFactory.decodeFile(coverPath)
                        if (bitmap != null) {
                            Image(
                                provider = ImageProvider(bitmap),
                                contentDescription = context.getString(R.string.widget_cover_content_description),
                                contentScale = ContentScale.Crop,
                                modifier = GlanceModifier.fillMaxHeight().cornerRadius(12.dp)
                            )
                        } else CompactEmptyCover()
                    } else CompactEmptyCover()

                    Spacer(GlanceModifier.width(10.dp))

                    Text(
                        text = title,
                        style = TextStyle(color = ColorProvider(Color.White), fontSize = 13.sp, fontWeight = FontWeight.Bold),
                        maxLines = 2,
                        modifier = GlanceModifier.defaultWeight()
                    )

                    Spacer(GlanceModifier.width(8.dp))

                    Box(
                        modifier = GlanceModifier
                            .size(40.dp)
                            .background(ColorProvider(Color.White))
                            .cornerRadius(20.dp)
                            .clickable(actionRunCallback<PlayPauseAction>()),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            provider = ImageProvider(if (isPlaying) R.drawable.ic_pause_widget else R.drawable.ic_play_widget),
                            contentDescription = context.getString(R.string.widget_play_pause_content_description),
                            colorFilter = ColorFilter.tint(ColorProvider(Color(0xFF0F0C1D))),
                            modifier = GlanceModifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun CompactEmptyCover() {
        Box(
            modifier = GlanceModifier.fillMaxHeight().width(48.dp).background(ColorProvider(Color(0xFF2E2445))).cornerRadius(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("♫", style = TextStyle(color = ColorProvider(Color.White), fontSize = 18.sp))
        }
    }
}
