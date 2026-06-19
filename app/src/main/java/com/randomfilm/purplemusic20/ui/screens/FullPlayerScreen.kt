package com.randomfilm.purplemusic20.ui.screens

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.mediarouter.app.MediaRouteButton
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import com.google.android.gms.cast.framework.CastButtonFactory
import androidx.media3.common.Player
import com.randomfilm.purplemusic20.R
import com.randomfilm.purplemusic20.SessionManager
import com.randomfilm.purplemusic20.data.models.LyricLine
import com.randomfilm.purplemusic20.ui.components.AudioVisualizer
import com.randomfilm.purplemusic20.ui.theme.AccentPurple
import com.randomfilm.purplemusic20.ui.theme.BgDark
import com.randomfilm.purplemusic20.ui.theme.TextGray
import com.randomfilm.purplemusic20.util.buildImageRequest
import com.randomfilm.purplemusic20.util.formatTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FullPlayerScreen(
    t: String, a: String, c: String, playing: Boolean, pos: Long, dur: Long, shuff: Boolean, rep: Int, session: SessionManager,
    isLyricsLoading: Boolean, syncedLyrics: List<LyricLine>?, plainLyrics: String?,
    back: () -> Unit, pp: () -> Unit, nxt: () -> Unit, prv: () -> Unit, seek: (Long) -> Unit, shuffT: () -> Unit, repT: () -> Unit, q: () -> Unit
) {
    val context = LocalContext.current
    var showLyrics by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val dynamicThemeEnabled = remember { session.isDynamicThemeEnabled() }
    var dominantColor by remember { mutableStateOf(Color(0xFF2E2445)) }
    var vibrantColor by remember { mutableStateOf(AccentPurple) }

    LaunchedEffect(c, dynamicThemeEnabled) {
        if (dynamicThemeEnabled && c.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                val request = ImageRequest.Builder(context)
                    .data(c)
                    .size(128) // On réduit la taille pour un calcul quasi-instantané
                    .allowHardware(false) // Nécessaire pour que Palette lise les pixels
                    .build()
                val result = context.imageLoader.execute(request)
                (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap?.let { bmp ->
                    val palette = Palette.from(bmp).generate()
                    // Couleurs adaptées aux dark modes
                    val dom = palette.getDarkMutedColor(palette.getDarkVibrantColor(palette.getDominantColor(0xFF2E2445.toInt())))
                    val vib = palette.getVibrantColor(palette.getLightVibrantColor(AccentPurple.toArgb()))
                    withContext(Dispatchers.Main) {
                        dominantColor = Color(dom)
                        vibrantColor = Color(vib)
                    }
                }
            }
        } else {
            dominantColor = Color(0xFF2E2445)
            vibrantColor = AccentPurple
        }
    }

    val animatedBgColor by animateColorAsState(targetValue = dominantColor, animationSpec = tween(1000), label = "bgColor")
    val animatedAccentColor by animateColorAsState(targetValue = vibrantColor, animationSpec = tween(1000), label = "accentColor")

    val prefs = context.getSharedPreferences("purple_music_state", Context.MODE_PRIVATE)
    var visualizerEnabled by remember { mutableStateOf(prefs.getBoolean("visualizer_enabled", false)) }
    var audioSessionId by remember { mutableIntStateOf(prefs.getInt("audio_session_id", 0)) }

    DisposableEffect(Unit) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
            if (key == "audio_session_id") {
                audioSessionId = sharedPreferences.getInt(key, 0)
            } else if (key == "visualizer_enabled") {
                visualizerEnabled = sharedPreferences.getBoolean(key, false)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    val sliderValue = if (dur > 0) (pos.toFloat() / dur.toFloat()).coerceIn(0f, 1f) else 0f
    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(animatedBgColor, BgDark))).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = back) { Icon(Icons.Rounded.KeyboardArrowDown, null, tint = Color.White, modifier = Modifier.size(32.dp)) }
            Text("LECTURE EN COURS", color = TextGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                AndroidView(
                    factory = { ctx ->
                        MediaRouteButton(androidx.appcompat.view.ContextThemeWrapper(ctx, androidx.appcompat.R.style.Theme_AppCompat_NoActionBar)).apply {
                            CastButtonFactory.setUpMediaRouteButton(ctx, this)
                        }
                    }
                )
                IconButton(onClick = q) { Icon(Icons.Rounded.QueueMusic, null, tint = Color.White) }
            }
        }
        Spacer(Modifier.weight(0.5f))

        if (showLyrics) {
            Box(Modifier.weight(4f).fillMaxWidth()) {
                if (isLyricsLoading) {
                    CircularProgressIndicator(color = animatedAccentColor, modifier = Modifier.align(Alignment.Center))
                } else if (syncedLyrics != null) {
                    val activeIndex = syncedLyrics.indexOfLast { it.timeMs <= pos }
                    LaunchedEffect(activeIndex) { if (activeIndex >= 0) listState.animateScrollToItem(maxOf(0, activeIndex - 2)) }
                    LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                        itemsIndexed(syncedLyrics) { index, line ->
                            val isActive = index == activeIndex
                            Text(text = line.text, color = if (isActive) Color.White else TextGray, fontSize = if (isActive) 22.sp else 18.sp, fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 8.dp))
                        }
                    }
                } else {
                    Text(text = plainLyrics ?: "Paroles introuvables.", color = TextGray, fontSize = 18.sp, textAlign = TextAlign.Center, modifier = Modifier.verticalScroll(rememberScrollState()).align(Alignment.Center))
                }
            }
        } else {
            Card(shape = RoundedCornerShape(24.dp), elevation = CardDefaults.cardElevation(20.dp), modifier = Modifier.aspectRatio(1f).weight(4f)) {
                Box(Modifier.fillMaxSize()) {
                    AsyncImage(model = buildImageRequest(LocalContext.current, c.ifEmpty { R.drawable.default_cover }, session.isCoverCacheEnabled()), contentDescription = null, modifier = Modifier.fillMaxSize().background(Color.DarkGray), contentScale = ContentScale.Crop)
                    
                    if (visualizerEnabled && audioSessionId != 0) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))))
                        )
                        AudioVisualizer(
                            audioSessionId = audioSessionId,
                            isPlaying = playing,
                            color = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.fillMaxSize().align(Alignment.BottomCenter).padding(horizontal = 16.dp, vertical = 16.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(30.dp))
        Column(Modifier.fillMaxWidth()) {
            Text(t, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.basicMarquee())
            Text(a, color = animatedAccentColor, fontSize = 18.sp, fontWeight = FontWeight.Medium, maxLines = 1, modifier = Modifier.basicMarquee())
        }
        Spacer(Modifier.height(10.dp))
        Slider(value = sliderValue, onValueChange = { percent -> seek((percent * dur).toLong()) }, colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = animatedAccentColor, inactiveTrackColor = Color.White.copy(0.2f)))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(formatTime(pos), color = TextGray, fontSize = 12.sp); Text(formatTime(dur), color = TextGray, fontSize = 12.sp) }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = shuffT) { Icon(Icons.Rounded.Shuffle, null, tint = if (shuff) animatedAccentColor else TextGray) }
            IconButton(onClick = prv, Modifier.size(48.dp)) { Icon(Icons.Rounded.SkipPrevious, null, tint = Color.White, modifier = Modifier.fillMaxSize()) }
            Box(Modifier.size(70.dp).background(Color.White, CircleShape).clickable { pp() }, contentAlignment = Alignment.Center) { Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = BgDark, modifier = Modifier.size(40.dp)) }
            IconButton(onClick = nxt, Modifier.size(48.dp)) { Icon(Icons.Rounded.SkipNext, null, tint = Color.White, modifier = Modifier.fillMaxSize()) }
            IconButton(onClick = repT) { Icon(if (rep == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat, null, tint = if(rep!=Player.REPEAT_MODE_OFF) animatedAccentColor else TextGray) }
        }
        
        Spacer(Modifier.height(24.dp))
        TextButton(
            onClick = { showLyrics = !showLyrics },
            modifier = Modifier.background(if (showLyrics) animatedAccentColor.copy(alpha = 0.2f) else Color.Transparent, RoundedCornerShape(50))
        ) {
            Icon(Icons.Rounded.Subject, contentDescription = null, tint = if (showLyrics) animatedAccentColor else TextGray)
            Spacer(Modifier.width(8.dp))
            Text("PAROLES", color = if (showLyrics) animatedAccentColor else TextGray, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.weight(0.5f))
    }
}
