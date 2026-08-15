package com.randomfilm.purplemusic20.ui.components

import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.mediarouter.app.MediaRouteButton
import androidx.media3.common.Player
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import com.google.android.gms.cast.framework.CastButtonFactory
import com.randomfilm.purplemusic20.R
import com.randomfilm.purplemusic20.data.*
import com.randomfilm.purplemusic20.ui.theme.*
import com.randomfilm.purplemusic20.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ─── Lecteurs & Autres ────────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FullPlayerScreen(
    t: String, a: String, c: String, playing: Boolean, posState: State<Long>, dur: Long, shuff: Boolean, rep: Int, session: SessionManager,
    isLyricsLoading: Boolean, syncedLyrics: List<LyricLine>?, plainLyrics: String?,
    back: () -> Unit, pp: () -> Unit, nxt: () -> Unit, prv: () -> Unit, seek: (Long) -> Unit, shuffT: () -> Unit, repT: () -> Unit, q: () -> Unit
) {
    val context = LocalContext.current
    var showLyrics by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val dynamicThemeEnabled = remember { session.isDynamicThemeEnabled() }
    val themeAccent = LocalAppColors.current.accent
    var dominantColor by remember { mutableStateOf(Color(0xFF2E2445)) }
    var vibrantColor by remember { mutableStateOf(themeAccent) }

    LaunchedEffect(c, dynamicThemeEnabled, themeAccent) {
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
                    val vib = palette.getVibrantColor(palette.getLightVibrantColor(themeAccent.toArgb()))
                    withContext(Dispatchers.Main) {
                        dominantColor = Color(dom)
                        vibrantColor = Color(vib)
                    }
                }
            }
        } else {
            dominantColor = Color(0xFF2E2445)
            vibrantColor = themeAccent
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

    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(animatedBgColor, LocalAppColors.current.background))).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = back) { Icon(Icons.Rounded.KeyboardArrowDown, null, tint = Color.White, modifier = Modifier.size(32.dp)) }
            Text(stringResource(R.string.player_now_playing_header), color = LocalAppColors.current.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                AndroidView(
                    factory = { ctx ->
                        MediaRouteButton(android.view.ContextThemeWrapper(ctx, androidx.appcompat.R.style.Theme_AppCompat_NoActionBar)).apply {
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
                    SyncedLyricsView(posState = posState, lines = syncedLyrics, listState = listState)
                } else {
                    Text(text = plainLyrics ?: stringResource(R.string.player_lyrics_not_found), color = LocalAppColors.current.textSecondary, fontSize = 18.sp, textAlign = TextAlign.Center, modifier = Modifier.verticalScroll(rememberScrollState()).align(Alignment.Center))
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
        PlayerSeekBar(posState = posState, dur = dur, accentColor = animatedAccentColor, onSeek = { percent -> seek((percent * dur).toLong()) })
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = shuffT) { Icon(Icons.Rounded.Shuffle, null, tint = if (shuff) animatedAccentColor else LocalAppColors.current.textSecondary) }
            IconButton(onClick = prv, Modifier.size(48.dp)) { Icon(Icons.Rounded.SkipPrevious, null, tint = Color.White, modifier = Modifier.fillMaxSize()) }
            Box(Modifier.size(70.dp).background(Color.White, CircleShape).clickable { pp() }, contentAlignment = Alignment.Center) { Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = LocalAppColors.current.background, modifier = Modifier.size(40.dp)) }
            IconButton(onClick = nxt, Modifier.size(48.dp)) { Icon(Icons.Rounded.SkipNext, null, tint = Color.White, modifier = Modifier.fillMaxSize()) }
            IconButton(onClick = repT) { Icon(if (rep == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat, null, tint = if(rep!=Player.REPEAT_MODE_OFF) animatedAccentColor else LocalAppColors.current.textSecondary) }
        }

        Spacer(Modifier.height(24.dp))
        TextButton(
            onClick = { showLyrics = !showLyrics },
            modifier = Modifier.background(if (showLyrics) animatedAccentColor.copy(alpha = 0.2f) else Color.Transparent, RoundedCornerShape(50))
        ) {
            Icon(Icons.Rounded.Subject, contentDescription = null, tint = if (showLyrics) animatedAccentColor else LocalAppColors.current.textSecondary)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.player_lyrics_button), color = if (showLyrics) animatedAccentColor else LocalAppColors.current.textSecondary, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.weight(0.5f))
    }
}

// Composable dédié : seul ce sous-arbre se recompose au rythme du tick de position (500ms),
// pas tout FullPlayerScreen (pochette, boutons, etc.), grâce à la lecture de posState.value ici.
@Composable
private fun PlayerSeekBar(posState: State<Long>, dur: Long, accentColor: Color, onSeek: (Float) -> Unit) {
    val pos = posState.value
    val sliderValue = if (dur > 0) (pos.toFloat() / dur.toFloat()).coerceIn(0f, 1f) else 0f
    Column {
        Slider(value = sliderValue, onValueChange = onSeek, colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = accentColor, inactiveTrackColor = Color.White.copy(0.2f)))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime(pos), color = LocalAppColors.current.textSecondary, fontSize = 12.sp)
            Text(formatTime(dur), color = LocalAppColors.current.textSecondary, fontSize = 12.sp)
        }
    }
}

// Idem : scope la recomposition liée au tick de position à la seule vue des paroles synchronisées.
@Composable
private fun SyncedLyricsView(posState: State<Long>, lines: List<LyricLine>, listState: androidx.compose.foundation.lazy.LazyListState) {
    val pos = posState.value
    val activeIndex = lines.indexOfLast { it.timeMs <= pos }
    LaunchedEffect(activeIndex) { if (activeIndex >= 0) listState.animateScrollToItem(maxOf(0, activeIndex - 2)) }
    LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        itemsIndexed(lines) { index, line ->
            val isActive = index == activeIndex
            Text(text = line.text, color = if (isActive) Color.White else LocalAppColors.current.textSecondary, fontSize = if (isActive) 22.sp else 18.sp, fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 8.dp))
        }
    }
}

@Composable
fun AudioVisualizer(audioSessionId: Int, isPlaying: Boolean, color: Color, modifier: Modifier = Modifier) {
    var magnitudesState by remember { mutableStateOf(FloatArray(0)) }
    val context = LocalContext.current
    val hasPermission = ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    val visualizer = remember(audioSessionId, hasPermission) {
        if (audioSessionId == 0 || !hasPermission) null
        else {
            try {
                val bands = 32
                val currentMagnitudes = FloatArray(bands)
                android.media.audiofx.Visualizer(audioSessionId).apply {
                    captureSize = android.media.audiofx.Visualizer.getCaptureSizeRange()[1]
                    setDataCaptureListener(object : android.media.audiofx.Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(v: android.media.audiofx.Visualizer?, waveform: ByteArray?, samplingRate: Int) {}
                        override fun onFftDataCapture(v: android.media.audiofx.Visualizer?, fft: ByteArray?, samplingRate: Int) {
                            if (fft != null && isPlaying) {
                                val newMagnitudes = FloatArray(bands)
                                val maxBin = fft.size / 2
                                val logMax = kotlin.math.log2(maxBin.toDouble())
                                var currentBin = 1
                                for (i in 0 until bands) {
                                    val nextBin = kotlin.math.max(currentBin + 1, java.lang.Math.pow(2.0, (i + 1) * logMax / bands).toInt()).coerceAtMost(maxBin)
                                    var maxMagnitude = 0f
                                    for (j in currentBin until nextBin) {
                                        if (j * 2 + 1 < fft.size) {
                                            val rfk = fft[j * 2]
                                            val ifk = fft[j * 2 + 1]
                                            val mag = kotlin.math.hypot(rfk.toFloat(), ifk.toFloat())
                                            if (mag > maxMagnitude) maxMagnitude = mag
                                        }
                                    }
                                    val multiplier = 1f + (i.toFloat() / bands) * 5f
                                    newMagnitudes[i] = maxMagnitude * multiplier
                                    currentBin = nextBin
                                }

                                for (i in 0 until bands) {
                                    val old = currentMagnitudes[i]
                                    val new = newMagnitudes[i]
                                    currentMagnitudes[i] = if (new > old) new * 0.6f + old * 0.4f else old * 0.8f + new * 0.2f
                                }
                                magnitudesState = currentMagnitudes.copyOf()
                            } else if (!isPlaying) {
                                var active = false
                                for (i in 0 until bands) {
                                    currentMagnitudes[i] *= 0.8f
                                    if (currentMagnitudes[i] > 1f) active = true
                                }
                                if (active) magnitudesState = currentMagnitudes.copyOf()
                            }
                        }
                    }, android.media.audiofx.Visualizer.getMaxCaptureRate(), false, true)
                    enabled = true
                }
            } catch(e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    DisposableEffect(visualizer) {
        onDispose {
            visualizer?.enabled = false
            visualizer?.release()
        }
    }

    Canvas(modifier = modifier) {
        if (magnitudesState.isEmpty()) return@Canvas
        val bands = magnitudesState.size
        val barWidth = size.width / bands
        val cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)

        for (i in 0 until bands) {
            val magnitude = magnitudesState[i]
            val height = (magnitude * size.height / 256f).coerceIn(0f, size.height)

            val x = i * barWidth + barWidth * 0.1f
            val w = barWidth * 0.8f
            drawRoundRect(
                color = color,
                topLeft = Offset(x, size.height - height),
                size = Size(w, height.coerceAtLeast(barWidth * 0.8f)), // Assure un point minimum
                cornerRadius = cornerRadius
            )
        }
    }
}
