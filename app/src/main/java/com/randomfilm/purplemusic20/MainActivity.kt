package com.randomfilm.purplemusic20

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.core.content.ContextCompat
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.graphics.toArgb
import androidx.palette.graphics.Palette
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.*
import androidx.media3.session.*
import androidx.navigation.compose.*
import androidx.navigation.NavType
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.google.common.util.concurrent.MoreExecutors
import com.randomfilm.purplemusic20.ui.theme.PurpleMusic20Theme
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import androidx.compose.ui.viewinterop.AndroidView
import androidx.mediarouter.app.MediaRouteButton
import com.google.android.gms.cast.framework.CastButtonFactory
import java.io.File
import java.io.FileOutputStream
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset

val BgDark = Color(0xFF0F0C1D)
val BgPanel = Color(0xFF1B1429)
val PrimaryPurple = Color(0xFF8E44AD)
val AccentPurple = Color(0xFFBB86FC)
val TextGray = Color(0xFFA196B4)
val NavBg = Color(0xFF151020)

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PurpleMusic20Theme { Surface(color = BgDark, modifier = Modifier.fillMaxSize()) { MainApp() } } }
    }
}

fun uriToFile(uri: Uri, context: Context, isAudio: Boolean = false): File {
    var fileName = "temp_${System.currentTimeMillis()}." + (if (isAudio) "mp3" else "jpg")
    try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1 && cursor.moveToFirst()) {
                val realName = cursor.getString(nameIndex)
                if (!realName.isNullOrEmpty()) fileName = realName
            }
        }
    } catch (e: Exception) { e.printStackTrace() }
    val file = File(context.cacheDir, fileName)
    context.contentResolver.openInputStream(uri)?.use { input ->
        FileOutputStream(file).use { output -> input.copyTo(output) }
    }
    return file
}

data class DownloadedTrack(
    val id: String,
    val title: String,
    val artist: String,
    val genre: String,
    val filePath: String,
    val coverPath: String
)

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

object DownloadManager {
    fun getDownloadDir(context: Context): File {
        val dir = File(context.filesDir, "downloads")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }
    fun getCoversDir(context: Context): File {
        val dir = File(context.filesDir, "covers")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }
    fun isDownloaded(context: Context, trackId: Int): Boolean {
        val prefs = context.getSharedPreferences("purple_dl", Context.MODE_PRIVATE)
        return prefs.contains("track_$trackId")
    }
    fun getDownloadedTracks(context: Context): List<DownloadedTrack> {
        val prefs = context.getSharedPreferences("purple_dl", Context.MODE_PRIVATE)
        return prefs.all.keys.filter { it.startsWith("track_") }.mapNotNull { key ->
            val value = prefs.getString(key, null) ?: return@mapNotNull null
            val parts = value.split("|")
            if (parts.size < 5) return@mapNotNull null
            DownloadedTrack(parts[0], parts[1], parts[2], parts[3], parts[4], if (parts.size > 5) parts[5] else "")
        }
    }
    fun saveDownloadedTrack(context: Context, track: Track, filePath: String, coverPath: String) {
        val prefs = context.getSharedPreferences("purple_dl", Context.MODE_PRIVATE)
        prefs.edit().putString("track_${track.id}", "${track.id}|${track.title}|${track.artist}|${track.genre ?: "Autre"}|$filePath|$coverPath").apply()
    }
    fun deleteDownloadedTrack(context: Context, trackId: String) {
        val prefs = context.getSharedPreferences("purple_dl", Context.MODE_PRIVATE)
        val value = prefs.getString("track_$trackId", null)
        if (value != null) {
            val parts = value.split("|")
            if (parts.size >= 5) File(parts[4]).delete()
            if (parts.size >= 6 && parts[5].isNotEmpty()) File(parts[5]).delete()
        }
        prefs.edit().remove("track_$trackId").apply()
    }
    suspend fun downloadTrack(context: Context, track: Track, onProgress: (Int) -> Unit): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val dlDir = getDownloadDir(context)
                val audioFile = File(dlDir, "track_${track.id}.mp3")
                val url = java.net.URL(track.stream_url)
                val connection = url.openConnection().apply { connect() }
                val totalBytes = connection.contentLength
                var downloadedBytes = 0
                connection.getInputStream().use { input ->
                    FileOutputStream(audioFile).use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead
                            if (totalBytes > 0) onProgress((downloadedBytes * 100 / totalBytes).coerceIn(0, 99))
                        }
                    }
                }
                var coverPath = ""
                if (track.cover_url.isNotEmpty()) {
                    try {
                        val coverFile = File(getCoversDir(context), "cover_${track.id}.jpg")
                        java.net.URL(track.cover_url).openStream().use { input ->
                            FileOutputStream(coverFile).use { output -> input.copyTo(output) }
                        }
                        coverPath = coverFile.absolutePath
                    } catch (e: Exception) { e.printStackTrace() }
                }
                saveDownloadedTrack(context, track, audioFile.absolutePath, coverPath)
                onProgress(100)
                true
            } catch (e: Exception) { false }
        }
    }
}

@Composable
fun MainApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val session = remember { SessionManager(context) }
    val prefs = context.getSharedPreferences("purple_music_state", Context.MODE_PRIVATE)

    var hasCompletedTutorial by remember { mutableStateOf(prefs.getBoolean("tutorial_v2_completed", false)) }

    var mediaController by remember { mutableStateOf<MediaController?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentTrackTitle by remember { mutableStateOf("Prêt à écouter") }
    var currentTrackArtist by remember { mutableStateOf("Arrêté") }
    var currentCoverUrl by remember { mutableStateOf("") }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var trackDuration by remember { mutableLongStateOf(0L) }
    var shuffleMode by remember { mutableStateOf(false) }
    var repeatMode by remember { mutableIntStateOf(Player.REPEAT_MODE_OFF) }
    var currentMediaIndex by remember { mutableIntStateOf(0) }
    var queueVersion by remember { mutableIntStateOf(0) }
    var appVolume by remember { mutableFloatStateOf(session.getVolume()) }
    var appSortMode by remember { mutableStateOf(session.getSortMode()) }

    var allTracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var allPlaylists by remember { mutableStateOf<List<Playlist>>(emptyList()) }

    var isGlobalQueue by remember { mutableStateOf(true) }
    var currentGlobalQueueIds by remember { mutableStateOf<List<String>>(emptyList()) }

    var currentSyncedLyrics by remember { mutableStateOf<List<LyricLine>?>(null) }
    var currentPlainLyrics by remember { mutableStateOf<String?>(null) }
    var isLyricsLoading by remember { mutableStateOf(false) }

    val reloadData: () -> Unit = {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                if (session.hasServerUrl()) {
                    allTracks = ApiClient.service.getTracks()
                    allPlaylists = ApiClient.service.getPlaylists()
                }
            } catch (e: Exception) {
            }
        }
    }

    LaunchedEffect(Unit) {
        if (session.hasServerUrl()) {
            ApiClient.init(session.getServerUrl()); reloadData()
        }
        val token = SessionToken(context, ComponentName(context, MusicService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({
            mediaController = future.get()
            mediaController?.let { mc ->
                isPlaying = mc.isPlaying
                shuffleMode = mc.shuffleModeEnabled
                repeatMode = mc.repeatMode
                currentTrackTitle = mc.mediaMetadata.title?.toString() ?: "Prêt"
                currentTrackArtist = mc.mediaMetadata.artist?.toString() ?: ""
                currentCoverUrl = mc.mediaMetadata.artworkUri?.toString() ?: ""
                trackDuration = mc.duration.coerceAtLeast(0)
                currentMediaIndex = mc.currentMediaItemIndex
                mc.volume = appVolume
            }
            mediaController?.addListener(object : Player.Listener {
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    currentTrackTitle = mediaItem?.mediaMetadata?.title?.toString() ?: "Lecture..."
                    currentTrackArtist = mediaItem?.mediaMetadata?.artist?.toString() ?: ""
                    currentCoverUrl = mediaItem?.mediaMetadata?.artworkUri?.toString() ?: ""
                    trackDuration = mediaController?.duration?.coerceAtLeast(0) ?: 0L
                    currentMediaIndex = mediaController?.currentMediaItemIndex ?: 0
                    mediaItem?.mediaId?.toIntOrNull()?.let { trackId ->
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                ApiClient.service.incrementPlay(
                                    trackId,
                                    session.getUsername(),
                                    session.getPassword()
                                )
                            } catch (e: Exception) {
                            }
                        }
                    }
                }

                override fun onIsPlayingChanged(p: Boolean) {
                    isPlaying = p
                }

                override fun onShuffleModeEnabledChanged(e: Boolean) {
                    shuffleMode = e
                }

                override fun onRepeatModeChanged(m: Int) {
                    repeatMode = m
                }

                override fun onPlaybackStateChanged(s: Int) {
                    if (s == Player.STATE_READY) trackDuration =
                        mediaController?.duration?.coerceAtLeast(0) ?: 0L
                }

                override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                    queueVersion++; currentMediaIndex = mediaController?.currentMediaItemIndex ?: 0
                    trackDuration = mediaController?.duration?.coerceAtLeast(0) ?: 0L
                }
            })
        }, MoreExecutors.directExecutor())
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            mediaController?.let { currentPosition = it.currentPosition }; delay(500L)
        }
    }

    LaunchedEffect(currentTrackTitle, currentTrackArtist) {
        if (currentTrackTitle == "Prêt à écouter" || currentTrackTitle.isBlank()) return@LaunchedEffect
        isLyricsLoading = true
        currentSyncedLyrics = null
        currentPlainLyrics = null
        withContext(Dispatchers.IO) {
            try {
                val response = LrcApiClient.service.getLyrics(currentTrackTitle, currentTrackArtist)
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    if (!body.syncedLyrics.isNullOrBlank()) {
                        val parsed = parseLrc(body.syncedLyrics)
                        withContext(Dispatchers.Main) { currentSyncedLyrics = parsed }
                    } else if (!body.plainLyrics.isNullOrBlank()) {
                        withContext(Dispatchers.Main) { currentPlainLyrics = body.plainLyrics }
                    } else {
                        withContext(Dispatchers.Main) { currentPlainLyrics = "Paroles introuvables pour ce titre." }
                    }
                } else {
                    withContext(Dispatchers.Main) { currentPlainLyrics = "Paroles introuvables." }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { currentPlainLyrics = "Erreur de connexion (LRCLib)." }
            }
        }
        isLyricsLoading = false
    }

    fun playMusic(tracks: List<Track>, startTrack: Track, isGlobal: Boolean) {
        isGlobalQueue = isGlobal
        if (isGlobal) currentGlobalQueueIds = tracks.map { it.id.toString() }
        mediaController?.let { player ->
            val index = tracks.indexOfFirst { it.id == startTrack.id }
            if (index != -1) {
                CoroutineScope(Dispatchers.Default).launch {
                    val items = tracks.map { t ->
                        MediaItem.Builder().setUri(t.stream_url).setMediaId(t.id.toString())
                            .setMediaMetadata(
                                MediaMetadata.Builder().setTitle(t.title).setArtist(t.artist)
                                    .setArtworkUri(Uri.parse(t.cover_url)).build()
                            ).build()
                    }
                    withContext(Dispatchers.Main) {
                        player.setMediaItems(items, index, 0)
                        player.prepare(); player.play()
                    }
                }
            }
        }
    }

    fun playDownloadedTracks(tracks: List<DownloadedTrack>, startTrack: DownloadedTrack) {
        mediaController?.let { player ->
            val index = tracks.indexOfFirst { it.id == startTrack.id }
            if (index != -1) {
                CoroutineScope(Dispatchers.Default).launch {
                    val items = tracks.map { t ->
                        MediaItem.Builder().setUri(Uri.fromFile(File(t.filePath))).setMediaId(t.id)
                            .setMediaMetadata(
                                MediaMetadata.Builder().setTitle(t.title).setArtist(t.artist)
                                    .setArtworkUri(if (t.coverPath.isNotEmpty()) Uri.fromFile(File(t.coverPath)) else null).build()
                            ).build()
                    }
                    withContext(Dispatchers.Main) {
                        player.setMediaItems(items, index, 0)
                        player.prepare(); player.play()
                    }
                }
            }
        }
    }

    val startDest = when {
        !session.hasServerUrl() -> "server_setup"
        session.getUserId() == -1 -> "login"
        !hasCompletedTutorial -> "tutorial"
        else -> "home"
    }

    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val showBottomBar = currentRoute in listOf(
        "home",
        "mixs",
        "create",
        "downloads",
        "queue"
    ) && session.getUserId() != -1

    Scaffold(
        containerColor = BgDark,
        bottomBar = {
            // Liste des écrans où la barre peut potentiellement s'afficher
            val navScreens = listOf("home", "mixs", "create", "downloads", "queue")

            if (currentRoute in navScreens) {
                val isLoggedIn = session.getUserId() != -1

                Column {
                    // Affichage du MiniPlayer (si une musique est chargée/joue)
                    if (mediaController != null && currentTrackTitle != "Prêt à écouter") {
                        val progress =
                            if (trackDuration > 0) currentPosition.toFloat() / trackDuration.toFloat() else 0f
                        MiniPlayer(
                            currentTrackTitle,
                            currentTrackArtist,
                            currentCoverUrl,
                            isPlaying,
                            progress,
                            { if (isPlaying) mediaController?.pause() else mediaController?.play() },
                            { navController.navigate("full_player") },
                            session
                        )
                    }

                    // Affichage de la barre de navigation complète uniquement si connecté
                    if (isLoggedIn) {
                        BottomNavBar(currentRoute) {
                            navController.navigate(it) {
                                popUpTo("home") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController, startDest, Modifier.padding(padding),
            enterTransition = { fadeIn(tween(300)) },
            exitTransition = { fadeOut(tween(300)) }
        ) {

                composable("server_setup") {
                    ServerSetupScreen(session) {
                        navController.navigate("login") {
                            popUpTo("server_setup") {
                                inclusive = true
                            }
                        }
                    }
                }

                composable("login") {
                    LoginScreenImpl(
                        session,
                        onOfflineMode = { navController.navigate("downloads") },
                        onSuccess = {
                            if (!hasCompletedTutorial) {
                                navController.navigate("tutorial") {
                                    popUpTo("login") { inclusive = true }
                                }
                            } else {
                                reloadData()
                                navController.navigate("home") {
                                    popUpTo("login") { inclusive = true }
                                }
                            }
                        }
                    )
                }

                composable("tutorial") {
                    TutorialScreen(session, prefs) {
                        hasCompletedTutorial = true
                        reloadData()
                        navController.navigate("home") {
                            popUpTo("tutorial") { inclusive = true }
                        }
                    }
                }

                composable("home") {
                    HomeScreenImpl(
                        tracks = allTracks,
                        playlists = allPlaylists,
                        session = session,
                        currentVolume = appVolume,
                        currentSortMode = appSortMode,
                        onVolumeChange = { vol ->
                            appVolume = vol; mediaController?.volume = vol; session.saveVolume(vol)
                        },
                        onSortChange = { mode -> appSortMode = mode; session.saveSortMode(mode) },
                        onPlay = { t, currentList -> playMusic(currentList, t, true) },
                        onListUpdated = { newList ->
                            val newIds = newList.map { it.id.toString() }
                            if (isGlobalQueue && currentGlobalQueueIds.isNotEmpty() && newIds != currentGlobalQueueIds) {
                                currentGlobalQueueIds = newIds
                                mediaController?.let { player ->
                                    if (player.mediaItemCount == 0) return@let
                                    val currentMediaId = player.currentMediaItem?.mediaId
                                    val newItems = newList.map { t ->
                                        MediaItem.Builder().setUri(t.stream_url)
                                            .setMediaId(t.id.toString()).setMediaMetadata(
                                            MediaMetadata.Builder().setTitle(t.title)
                                                .setArtist(t.artist)
                                                .setArtworkUri(Uri.parse(t.cover_url)).build()
                                        ).build()
                                    }
                                    if (newItems.any { it.mediaId == currentMediaId }) player.replaceMediaItems(
                                        0,
                                        player.mediaItemCount,
                                        newItems
                                    )
                                    else {
                                        if (newItems.isNotEmpty()) {
                                            player.setMediaItems(
                                                newItems,
                                                0,
                                                0
                                            ); player.prepare(); player.play()
                                        } else player.clearMediaItems()
                                    }
                                }
                            }
                        },
                        onRefresh = reloadData,
                        onLogout = {
                            session.logout(); navController.navigate("login") {
                            popUpTo("home") {
                                inclusive = true
                            }
                        }
                        },
                        onAddToPlaylist = { track -> navController.navigate("select_playlist/${track.id}") },
                        onRedoTutorial = { navController.navigate("tutorial") }
                    )
                }

                composable("mixs") {
                    PlaylistsScreenImpl(
                        allPlaylists, allTracks, session, reloadData,
                        onPlay = { idsStr ->
                            val ids = idsStr.split(",").mapNotNull { it.trim().toIntOrNull() }
                            val hidden = session.getHiddenGenres()
                            val pTracks = ids.mapNotNull { id -> allTracks.find { it.id == id } }
                                .filter { !(hidden.contains(it.genre ?: "Autre")) }
                            if (pTracks.isNotEmpty()) playMusic(pTracks, pTracks[0], false)
                            else Toast.makeText(context, "Mix vide ou filtré", Toast.LENGTH_SHORT)
                                .show()
                        },
                        onNavigateCreate = { navController.navigate("build_playlist") }
                    )
                }

                composable("create") { UploadScreenImpl(session, reloadData) }

                composable("downloads") {
                    DownloadsScreen(
                        session = session,
                        onPlay = { track, list -> playDownloadedTracks(list, track) },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(
                    "full_player",
                    enterTransition = { slideInVertically(initialOffsetY = { it }) },
                    exitTransition = { slideOutVertically(targetOffsetY = { it }) }) {
                    FullPlayerScreen(
                        currentTrackTitle,
                        currentTrackArtist,
                        currentCoverUrl,
                        isPlaying,
                        currentPosition,
                        trackDuration,
                        shuffleMode,
                        repeatMode,
                        session,
                        isLyricsLoading,
                        currentSyncedLyrics,
                        currentPlainLyrics,
                        { navController.popBackStack() },
                        { if (isPlaying) mediaController?.pause() else mediaController?.play() },
                        { mediaController?.seekToNext() },
                        { mediaController?.seekToPrevious() },
                        { mediaController?.seekTo(it); currentPosition = it },
                        { mediaController?.shuffleModeEnabled = !shuffleMode },
                        {
                            mediaController?.repeatMode = when (repeatMode) {
                                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL; Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE; else -> Player.REPEAT_MODE_OFF
                            }
                        },
                        { navController.navigate("queue") }
                    )
                }

                composable(
                    "queue",
                    enterTransition = { slideInVertically(initialOffsetY = { it }) },
                    exitTransition = { slideOutVertically(targetOffsetY = { it }) }) {
                    QueueScreen(
                        mediaController,
                        currentMediaIndex,
                        queueVersion,
                        session,
                        { idx -> mediaController?.seekTo(idx, 0); mediaController?.play() },
                        { navController.popBackStack() })
                }

                composable(
                    "select_playlist/{trackId}",
                    arguments = listOf(navArgument("trackId") { type = NavType.IntType })
                ) { backStack ->
                    val tid = backStack.arguments?.getInt("trackId") ?: return@composable
                    SelectPlaylistScreen(
                        trackId = tid, allPlaylists = allPlaylists, session = session,
                        onBack = { navController.popBackStack() },
                        onNewPlaylist = {
                            navController.navigate("build_playlist?trackId=$tid") {
                                popUpTo(
                                    "select_playlist/$tid"
                                ) { inclusive = true }
                            }
                        },
                        onSuccess = { reloadData(); navController.popBackStack() }
                    )
                }

                composable(
                    "build_playlist?trackId={trackId}",
                    arguments = listOf(navArgument("trackId") {
                        type = NavType.StringType; nullable = true
                    })
                ) { backStack ->
                    val preSelectedId = backStack.arguments?.getString("trackId")?.toIntOrNull()
                    PlaylistBuilderScreen(
                        allTracks = allTracks, session = session, preSelectedId = preSelectedId,
                        onBack = { navController.popBackStack() },
                        onSuccess = { reloadData(); navController.popBackStack() }
                    )
                }
            }
        }
    }




// ─── Image Builder Helper pour le cache Coil ───────────────────────────────────
fun buildImageRequest(context: Context, url: Any, cacheEnabled: Boolean): ImageRequest {
    val policy = if (cacheEnabled) CachePolicy.ENABLED else CachePolicy.DISABLED
    return ImageRequest.Builder(context)
        .data(url)
        .diskCachePolicy(policy)
        .memoryCachePolicy(policy)
        .crossfade(true)
        .error(R.drawable.default_cover)
        .fallback(R.drawable.default_cover)
        .placeholder(R.drawable.default_cover)
        .build()
}

// ─── Ecrans Setup & Auth ───────────────────────────────────────────────────────
@Composable
fun ServerSetupScreen(session: SessionManager, onConfirm: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var serverUrl by remember { mutableStateOf("") }
    var isChecking by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().background(BgDark).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("PURPLE", color = AccentPurple, fontSize = 48.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(8.dp))
        Text("Configuration du serveur", color = TextGray, fontSize = 14.sp)
        Spacer(Modifier.height(48.dp))
        OutlinedTextField(value = serverUrl, onValueChange = { serverUrl = it; errorMsg = "" }, placeholder = { Text("https://exemple.com/music/", color = TextGray) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri), colors = outlinedFieldColors(), modifier = Modifier.fillMaxWidth())
        if (errorMsg.isNotEmpty()) { Spacer(Modifier.height(8.dp)); Text(errorMsg, color = Color.Red, fontSize = 13.sp) }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                val url = serverUrl.trim()
                if (url.isEmpty()) { errorMsg = "Veuillez entrer une adresse."; return@Button }
                scope.launch {
                    isChecking = true; errorMsg = ""
                    try { ApiClient.init(url); ApiClient.service.getTracks(); session.saveServerUrl(url); onConfirm() }
                    catch (e: Exception) { errorMsg = "Impossible de joindre le serveur." }
                    isChecking = false
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple), shape = RoundedCornerShape(12.dp), enabled = !isChecking
        ) { if (isChecking) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp)) else Text("CONFIRMER", fontWeight = FontWeight.Bold) }
    }
}

@Composable
fun LoginScreenImpl(session: SessionManager, onOfflineMode: () -> Unit, onSuccess: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }
    var loginUser by remember { mutableStateOf("") }; var loginPass by remember { mutableStateOf("") }; var loginLoading by remember { mutableStateOf(false) }
    var regUser by remember { mutableStateOf("") }; var regPass by remember { mutableStateOf("") }; var regPass2 by remember { mutableStateOf("") }; var regLoading by remember { mutableStateOf(false) }

    // --- RESTAURATION: Changement de serveur ---
    var showServerDialog by remember { mutableStateOf(false) }
    var serverUrlInput by remember { mutableStateOf(session.getServerUrl()) }
    var serverChecking by remember { mutableStateOf(false) }
    var serverError by remember { mutableStateOf("") }

    if (showServerDialog) {
        AlertDialog(
            onDismissRequest = { showServerDialog = false }, containerColor = BgPanel,
            title = { Text("Changer de serveur", color = Color.White) },
            text = {
                Column {
                    Text("Adresse du serveur :", color = TextGray, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = serverUrlInput, onValueChange = { serverUrlInput = it; serverError = "" },
                        placeholder = { Text("https://exemple.com/music/", color = TextGray) }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri), colors = outlinedFieldColors(), modifier = Modifier.fillMaxWidth()
                    )
                    if (serverError.isNotEmpty()) { Spacer(Modifier.height(6.dp)); Text(serverError, color = Color.Red, fontSize = 12.sp) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val url = serverUrlInput.trim()
                    if (url.isEmpty()) { serverError = "Adresse vide."; return@TextButton }
                    scope.launch {
                        serverChecking = true; serverError = ""
                        try {
                            ApiClient.init(url)
                            ApiClient.service.getTracks()
                            session.saveServerUrl(url)
                            showServerDialog = false
                            Toast.makeText(context, "Serveur mis à jour ✓", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) { serverError = "Serveur inaccessible." }
                        serverChecking = false
                    }
                }) { if (serverChecking) CircularProgressIndicator(color = AccentPurple, modifier = Modifier.size(18.dp)) else Text("Confirmer", color = AccentPurple) }
            },
            dismissButton = { TextButton(onClick = { showServerDialog = false }) { Text("Annuler", color = TextGray) } }
        )
    }

    Column(Modifier.fillMaxSize().background(BgDark).padding(horizontal = 32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("PURPLE", color = AccentPurple, fontSize = 48.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(6.dp))

        // --- RESTAURATION: Affichage du serveur actuel ---
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = session.getServerUrl().removePrefix("https://").removePrefix("http://").trimEnd('/'),
                color = TextGray, fontSize = 12.sp, maxLines = 1, modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(Modifier.width(6.dp))
            TextButton(
                onClick = { serverUrlInput = session.getServerUrl(); showServerDialog = true },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) { Text("Changer", color = AccentPurple, fontSize = 12.sp) }
        }

        Spacer(Modifier.height(32.dp))

        TabRow(
            selectedTabIndex = selectedTab, containerColor = BgPanel, contentColor = AccentPurple, modifier = Modifier.fillMaxWidth(),
            indicator = { t -> TabRowDefaults.Indicator(modifier = Modifier.tabIndicatorOffset(t[selectedTab]), color = AccentPurple) }
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Connexion", color = if (selectedTab == 0) AccentPurple else TextGray) })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Inscription", color = if (selectedTab == 1) AccentPurple else TextGray) })
        }
        Spacer(Modifier.height(28.dp))

        if (selectedTab == 0) {
            OutlinedTextField(value = loginUser, onValueChange = { loginUser = it }, label = { Text("Utilisateur") }, singleLine = true, colors = outlinedFieldColors(), modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(value = loginPass, onValueChange = { loginPass = it }, label = { Text("Mot de passe") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), colors = outlinedFieldColors(), modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(28.dp))
            Button(
                onClick = {
                    scope.launch {
                        loginLoading = true
                        try {
                            val res = ApiClient.service.login(loginUser, loginPass)
                            if (res.status == "success") { session.saveUser(res.user_id ?: 0, res.username ?: "", loginPass, res.is_admin == true); onSuccess() }
                            else Toast.makeText(context, res.message ?: "Erreur", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {}
                        loginLoading = false
                    }
                }, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple), shape = RoundedCornerShape(12.dp), enabled = !loginLoading
            ) { if (loginLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp)) else Text("CONNEXION", fontWeight = FontWeight.Bold) }
        } else {
            OutlinedTextField(value = regUser, onValueChange = { regUser = it }, label = { Text("Utilisateur") }, singleLine = true, colors = outlinedFieldColors(), modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(value = regPass, onValueChange = { regPass = it }, label = { Text("Mot de passe") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), colors = outlinedFieldColors(), modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(value = regPass2, onValueChange = { regPass2 = it }, label = { Text("Confirmer") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), colors = outlinedFieldColors(), modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(28.dp))
            Button(
                onClick = {
                    if (regPass == regPass2 && regPass.length >= 6) {
                        scope.launch {
                            regLoading = true
                            try {
                                val res = ApiClient.service.register(regUser, regPass)
                                if (res.status == "success") { session.saveUser(ApiClient.service.login(regUser, regPass).user_id?:0, regUser, regPass, false); onSuccess() }
                            } catch (e: Exception) {}
                            regLoading = false
                        }
                    }
                }, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple), shape = RoundedCornerShape(12.dp), enabled = !regLoading
            ) { if (regLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp)) else Text("S'INSCRIRE", fontWeight = FontWeight.Bold) }
        }

        Spacer(Modifier.height(30.dp))
        TextButton(onClick = onOfflineMode) {
            Icon(Icons.Rounded.DownloadForOffline, null, tint = AccentPurple, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Musiques téléchargées (Hors-ligne)", color = AccentPurple, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun outlinedFieldColors() = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.Gray, focusedBorderColor = AccentPurple, unfocusedBorderColor = TextGray, cursorColor = AccentPurple, focusedLabelColor = AccentPurple, unfocusedLabelColor = TextGray)

// ─── Settings Dialog ──────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(session: SessionManager, currentHidden: Set<String>, currentVolume: Float, currentSortMode: String, onSave: (Set<String>, String) -> Unit, onVolumeChange: (Float) -> Unit, onSetSleepTimer: (Int) -> Unit, onRedoTutorial: () -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var localHidden by remember { mutableStateOf(currentHidden) }
    var localVolume by remember { mutableFloatStateOf(currentVolume) }
    var localSortMode by remember { mutableStateOf(currentSortMode) }
    var localSleepTimer by remember { mutableIntStateOf(0) }
    var coverCacheEnabled by remember { mutableStateOf(session.isCoverCacheEnabled()) }
    var customGenreInput by remember { mutableStateOf("") }

    val prefs = context.getSharedPreferences("purple_music_state", Context.MODE_PRIVATE)
    var visualizerEnabled by remember { mutableStateOf(prefs.getBoolean("visualizer_enabled", false)) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        visualizerEnabled = granted
        prefs.edit().putBoolean("visualizer_enabled", granted).apply()
        if (!granted) {
            Toast.makeText(context, "Permission refusée pour le visualiseur", Toast.LENGTH_SHORT).show()
        }
    }

    // --- RESTAURATION: Noms lisibles pour les options de tri ---
    val sortOptions = mapOf("popular" to "Les plus écoutés", "date_desc" to "Plus récent", "date_asc" to "Plus ancien", "alpha_asc" to "Nom (A-Z)", "alpha_desc" to "Nom (Z-A)", "artist" to "Par Artiste")

    val predefinedGenres = listOf("Phonk/Funk", "Rap", "Pop", "Rock", "Electro", "Hyperpop", "Nightcore", "Qualité inférieure", "Autre")
    val displayGenres = (predefinedGenres + localHidden).distinct()
    var sortExpanded by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Général", "Filtres & Tri", "Égaliseur", "Avancé")

    var eqEnabled by remember { mutableStateOf(prefs.getBoolean("eq_enabled", false)) }
    var eqBands by remember { mutableStateOf<List<Short>>(emptyList()) }
    var eqLevels by remember { mutableStateOf<Map<Short, Short>>(emptyMap()) }
    var eqMinLevel by remember { mutableStateOf<Short>(0) }
    var eqMaxLevel by remember { mutableStateOf<Short>(0) }
    var eqCenterFreqs by remember { mutableStateOf<Map<Short, Int>>(emptyMap()) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val audioSessionId = prefs.getInt("audio_session_id", 0)
                val tempEq = android.media.audiofx.Equalizer(0, audioSessionId)
                val bandsList = (0 until tempEq.numberOfBands).map { it.toShort() }
                val minL = tempEq.bandLevelRange[0]
                val maxL = tempEq.bandLevelRange[1]
                val freqs = mutableMapOf<Short, Int>()
                val initialLevels = mutableMapOf<Short, Short>()
                for (i in bandsList) {
                    freqs[i] = tempEq.getCenterFreq(i)
                    initialLevels[i] = prefs.getInt("eq_band_$i", tempEq.getBandLevel(i).toInt()).toShort()
                }
                tempEq.release()
                withContext(Dispatchers.Main) {
                    eqBands = bandsList
                    eqMinLevel = minL
                    eqMaxLevel = maxL
                    eqCenterFreqs = freqs
                    eqLevels = initialLevels
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Dialog(
        onDismissRequest = {
            onSave(localHidden, localSortMode)
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = BgPanel)
        ) {
            Column(Modifier.fillMaxSize().padding(16.dp)) {
                Text("Paramètres", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))

                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = BgPanel,
                    contentColor = AccentPurple,
                    edgePadding = 0.dp,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = AccentPurple
                        )
                    },
                    divider = { HorizontalDivider(color = Color.White.copy(alpha = 0.1f)) }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, color = if (selectedTab == index) AccentPurple else TextGray) }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                    when (selectedTab) {
                        0 -> { // Général
                            item {
                                Text("Volume interne :", color = TextGray, fontSize = 14.sp)
                                Slider(value = localVolume, onValueChange = { localVolume = it; onVolumeChange(it) }, colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = AccentPurple, inactiveTrackColor = Color.White.copy(0.2f)))
                                Spacer(Modifier.height(15.dp))
                                
                                Text("Minuteur de sommeil :", color = TextGray, fontSize = 14.sp)
                                var sleepTimerExpanded by remember { mutableStateOf(false) }
                                val timerOptions = listOf(0, 15, 30, 45, 60, 90, 120)
                                ExposedDropdownMenuBox(expanded = sleepTimerExpanded, onExpandedChange = { sleepTimerExpanded = it }) {
                                    OutlinedTextField(
                                        value = if (localSleepTimer == 0) "Désactivé" else "$localSleepTimer minutes",
                                        onValueChange = {}, readOnly = true,
                                        modifier = Modifier.fillMaxWidth().menuAnchor(), trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sleepTimerExpanded) },
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                    )
                                    ExposedDropdownMenu(expanded = sleepTimerExpanded, onDismissRequest = { sleepTimerExpanded = false }, modifier = Modifier.background(BgPanel)) {
                                        timerOptions.forEach { mins -> DropdownMenuItem(text = { Text(if (mins == 0) "Désactivé" else "$mins minutes", color = Color.White) }, onClick = { localSleepTimer = mins; sleepTimerExpanded = false; onSetSleepTimer(mins) }) }
                                    }
                                }
                                Spacer(Modifier.height(15.dp))

                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                    Text("Thème dynamique (Lecteur)", color = Color.White, fontSize = 14.sp)
                                    var dynamicTheme by remember { mutableStateOf(session.isDynamicThemeEnabled()) }
                                    Switch(checked = dynamicTheme, onCheckedChange = { dynamicTheme = it; session.setDynamicThemeEnabled(it) }, colors = SwitchDefaults.colors(checkedThumbColor = AccentPurple, checkedTrackColor = PrimaryPurple))
                                }
                                Spacer(Modifier.height(15.dp))

                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                    Text("Visualiseur Audio (Spectre)", color = Color.White, fontSize = 14.sp)
                                    Switch(checked = visualizerEnabled, onCheckedChange = { 
                                        if (it) {
                                            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                                                permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                            } else {
                                                visualizerEnabled = true
                                                prefs.edit().putBoolean("visualizer_enabled", true).apply()
                                            }
                                        } else {
                                            visualizerEnabled = false
                                            prefs.edit().putBoolean("visualizer_enabled", false).apply()
                                        }
                                    }, colors = SwitchDefaults.colors(checkedThumbColor = AccentPurple, checkedTrackColor = PrimaryPurple))
                                }
                            }
                        }
                        1 -> { // Filtres & Tri
                            item {
                                ExposedDropdownMenuBox(expanded = sortExpanded, onExpandedChange = { sortExpanded = it }) {
                                    OutlinedTextField(
                                        value = sortOptions[localSortMode] ?: "Plus récent",
                                        onValueChange = {}, readOnly = true, label = { Text("Trier par défaut") },
                                        modifier = Modifier.fillMaxWidth().menuAnchor(), trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sortExpanded) },
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                    )
                                    ExposedDropdownMenu(expanded = sortExpanded, onDismissRequest = { sortExpanded = false }, modifier = Modifier.background(BgPanel)) {
                                        sortOptions.forEach { (key, label) -> DropdownMenuItem(text = { Text(label, color = Color.White) }, onClick = { localSortMode = key; sortExpanded = false }) }
                                    }
                                }
                                Spacer(Modifier.height(20.dp))
                                Text("Masquer les genres :", color = TextGray, fontSize = 14.sp)
                            }
                            items(displayGenres) { genre ->
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { localHidden = if (localHidden.contains(genre)) localHidden - genre else localHidden + genre }) {
                                    Checkbox(checked = localHidden.contains(genre), onCheckedChange = { c -> localHidden = if (c) localHidden + genre else localHidden - genre }, colors = CheckboxDefaults.colors(checkedColor = Color.Red, uncheckedColor = TextGray))
                                    Text(genre, color = Color.White)
                                }
                            }
                            item {
                                Spacer(Modifier.height(10.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(
                                        value = customGenreInput, onValueChange = { customGenreInput = it },
                                        label = { Text("Autre genre à masquer", fontSize = 12.sp) },
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White), modifier = Modifier.weight(1f).height(60.dp)
                                    )
                                    IconButton(onClick = { if (customGenreInput.isNotBlank()) { localHidden = localHidden + customGenreInput; customGenreInput = "" } }) { Icon(Icons.Rounded.Add, "Ajouter", tint = AccentPurple) }
                                }
                            }
                        }
                        2 -> { // Égaliseur
                            item {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                    Text("Activer l'égaliseur", color = Color.White, fontSize = 14.sp)
                                    Switch(checked = eqEnabled, onCheckedChange = { 
                                        eqEnabled = it
                                        prefs.edit().putBoolean("eq_enabled", it).apply()
                                    }, colors = SwitchDefaults.colors(checkedThumbColor = AccentPurple, checkedTrackColor = PrimaryPurple))
                                }
                                Spacer(Modifier.height(15.dp))
                            }
                            if (eqBands.isNotEmpty()) {
                                items(eqBands) { band ->
                                    val freqHz = eqCenterFreqs[band] ?: 0
                                    val freqText = if (freqHz >= 1000000) "${freqHz / 1000000} kHz" else "${freqHz / 1000} Hz"
                                    val level = eqLevels[band] ?: 0
                                    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(freqText, color = Color.White, fontSize = 12.sp)
                                            Text("${level / 100} dB", color = TextGray, fontSize = 12.sp)
                                        }
                                        Slider(
                                            value = level.toFloat(),
                                            onValueChange = { newVal ->
                                                val newLevel = newVal.toInt().toShort()
                                                eqLevels = eqLevels.toMutableMap().apply { put(band, newLevel) }
                                                prefs.edit().putInt("eq_band_$band", newLevel.toInt()).apply()
                                            },
                                            valueRange = eqMinLevel.toFloat()..eqMaxLevel.toFloat(),
                                            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = AccentPurple, inactiveTrackColor = Color.White.copy(0.2f)),
                                            enabled = eqEnabled
                                        )
                                    }
                                }
                            } else {
                                item {
                                    Text("Égaliseur non disponible", color = TextGray)
                                }
                            }
                        }
                        3 -> { // Avancé / Opti
                            item {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                    Text("Mettre en cache les covers", color = Color.White, fontSize = 14.sp)
                                    Switch(checked = coverCacheEnabled, onCheckedChange = { coverCacheEnabled = it; session.setCoverCacheEnabled(it) }, colors = SwitchDefaults.colors(checkedThumbColor = AccentPurple, checkedTrackColor = PrimaryPurple))
                                }
                                Spacer(Modifier.height(10.dp))
                                Button(onClick = { context.imageLoader.diskCache?.clear(); context.imageLoader.memoryCache?.clear(); Toast.makeText(context, "Cache vidé", Toast.LENGTH_SHORT).show() }, colors = ButtonDefaults.buttonColors(containerColor = BgDark), modifier = Modifier.fillMaxWidth()) { Text("Vider le cache des covers", color = AccentPurple) }
                                Spacer(Modifier.height(10.dp))
                                Button(onClick = onRedoTutorial, colors = ButtonDefaults.buttonColors(containerColor = BgDark), modifier = Modifier.fillMaxWidth()) { Text("Refaire le tutoriel", color = AccentPurple) }
                            }
                        }
                    }
                }
                
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { onSave(localHidden, localSortMode); onDismiss() }) { Text("Fermer", color = AccentPurple) }
                }
            }
        }
    }
}

// ─── Home Screen ──────────────────────────────────────────────────────────────
@Composable
fun HomeScreenImpl(tracks: List<Track>, playlists: List<Playlist>, session: SessionManager, currentVolume: Float, currentSortMode: String, onVolumeChange: (Float) -> Unit, onSortChange: (String) -> Unit, onPlay: (Track, List<Track>) -> Unit, onListUpdated: (List<Track>) -> Unit, onRefresh: () -> Unit, onLogout: () -> Unit, onAddToPlaylist: (Track) -> Unit, onRedoTutorial: () -> Unit) {
    var search by remember { mutableStateOf("") }
    var hiddenGenres by remember { mutableStateOf(session.getHiddenGenres()) }
    var showSettings by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val baseList = remember(tracks, hiddenGenres, currentSortMode) {
        tracks.filter { !hiddenGenres.contains(it.genre ?: "Autre") }.let { list ->
            when (currentSortMode) {
                "popular" -> list.sortedWith(compareByDescending<Track> { it.play_count ?: 0 }.thenByDescending { it.id })
                "date_desc" -> list.sortedByDescending { it.id }
                "date_asc" -> list.sortedBy { it.id }
                "alpha_asc" -> list.sortedBy { it.title.lowercase() }
                "alpha_desc" -> list.sortedByDescending { it.title.lowercase() }
                "artist" -> list.sortedBy { it.artist.lowercase() }
                else -> list.sortedByDescending { it.id }
            }
        }
    }

    val displayList = remember(baseList, search) { baseList.filter { it.title.contains(search, true) || it.artist.contains(search, true) } }
    LaunchedEffect(baseList) { onListUpdated(baseList) }

    var editTrack by remember { mutableStateOf<Track?>(null) }

    if (showSettings) {
        SettingsDialog(session, hiddenGenres, currentVolume, currentSortMode,
            onSave = { newHidden, newSort -> hiddenGenres = newHidden; session.saveHiddenGenres(newHidden); onSortChange(newSort) },
            onVolumeChange = onVolumeChange,
            onSetSleepTimer = { mins ->
                val intent = android.content.Intent(context, MusicService::class.java).apply {
                    action = "SLEEP_TIMER"
                    putExtra("minutes", mins)
                }
                context.startService(intent)
                Toast.makeText(context, if (mins > 0) "Minuteur défini sur $mins min" else "Minuteur désactivé", Toast.LENGTH_SHORT).show()
            },
            onRedoTutorial = {
                showSettings = false
                onRedoTutorial()
            },
            onDismiss = { showSettings = false }
        )
    }
    if (editTrack != null) EditTrackDialog(editTrack!!, session, { editTrack = null }, { editTrack = null; onRefresh() })

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(16.dp))
        Box(Modifier.fillMaxWidth()) {
            IconButton(onClick = { showSettings = true }, modifier = Modifier.align(Alignment.CenterStart)) { Icon(Icons.Rounded.Settings, "Paramètres", tint = TextGray) }
            Text("PURPLE", color = AccentPurple, fontSize = 20.sp, fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.Center))
            IconButton(onClick = onLogout, modifier = Modifier.align(Alignment.CenterEnd)) { Icon(Icons.Rounded.ExitToApp, "Logout", tint = TextGray) }
        }
        Spacer(Modifier.height(20.dp))
        TextField(value = search, onValueChange = { search = it }, placeholder = { Text("Rechercher...", color = TextGray) }, colors = TextFieldDefaults.colors(focusedContainerColor = Color(0xFF241B36), unfocusedContainerColor = Color(0xFF241B36), focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent), shape = RoundedCornerShape(50), modifier = Modifier.fillMaxWidth().height(55.dp), trailingIcon = { Icon(Icons.Rounded.FilterList, null, tint = AccentPurple) })
        Spacer(Modifier.height(20.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(displayList, key = { it.id }) { track ->
                TrackRowWithMenu(
                    track = track, session = session,
                    canEdit = session.isAdmin() || track.uploader_id == session.getUserId(),
                    onClick = { onPlay(track, baseList) },
                    onEdit = { editTrack = track },
                    onAddToPlaylist = { onAddToPlaylist(track) }
                )
            }
        }
    }
}

@Composable
fun TutorialScreen(
    session: SessionManager,
    prefs: android.content.SharedPreferences,
    onFinish: () -> Unit
) {
    var step by remember { mutableIntStateOf(0) }
    val totalSteps = 6

    var dynamicTheme by remember { mutableStateOf(session.isDynamicThemeEnabled()) }
    var visualizer by remember { mutableStateOf(prefs.getBoolean("visualizer_enabled", false)) }
    var eqEnabled by remember { mutableStateOf(prefs.getBoolean("eq_enabled", false)) }
    var coverCache by remember { mutableStateOf(session.isCoverCacheEnabled()) }
    var sortMode by remember { mutableStateOf(session.getSortMode()) }
    var sortExpanded by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        visualizer = granted
        prefs.edit().putBoolean("visualizer_enabled", granted).apply()
        if (!granted) {
            Toast.makeText(context, "Permission refusée pour le visualiseur", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        Modifier.fillMaxSize().background(BgDark).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInHorizontally(initialOffsetX = { it }) + fadeIn()).togetherWith(slideOutHorizontally(targetOffsetX = { -it }) + fadeOut())
                } else {
                    (slideInHorizontally(initialOffsetX = { -it }) + fadeIn()).togetherWith(slideOutHorizontally(targetOffsetX = { it }) + fadeOut())
                }
            }, label = "tutorial_animation",
            modifier = Modifier.weight(1f)
        ) { targetStep ->
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                when (targetStep) {
                    0 -> {
                        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                        val alpha by infiniteTransition.animateFloat(
                            initialValue = 0.3f, targetValue = 1f,
                            animationSpec = infiniteRepeatable(animation = tween(1000, easing = LinearEasing), repeatMode = RepeatMode.Reverse), label = "alpha"
                        )
                        val scale by infiniteTransition.animateFloat(
                            initialValue = 0.95f, targetValue = 1.05f,
                            animationSpec = infiniteRepeatable(animation = tween(1000, easing = LinearEasing), repeatMode = RepeatMode.Reverse), label = "scale"
                        )

                        Column(
                            modifier = Modifier.fillMaxSize().clickable(
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                indication = null
                            ) { step++ },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("Bienvenue sur", color = TextGray, fontSize = 24.sp)
                            Text("PURPLE MUSIC", color = AccentPurple, fontSize = 40.sp, fontWeight = FontWeight.Black)
                            Spacer(Modifier.height(64.dp))
                            Text("Cliquez pour commencer", color = Color.White.copy(alpha = alpha), fontSize = 18.sp, modifier = Modifier.scale(scale))
                        }
                    }
                    1 -> {
                        Text("Thème Dynamique", color = AccentPurple, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        Text("Adapte les couleurs du lecteur en fonction de la pochette de la musique écoutée. (Par défaut: Non)", color = Color.White, textAlign = TextAlign.Center, fontSize = 16.sp)
                        Spacer(Modifier.height(32.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text("Activer le thème dynamique", color = Color.White, modifier = Modifier.weight(1f))
                            Switch(
                                checked = dynamicTheme,
                                onCheckedChange = { dynamicTheme = it; session.setDynamicThemeEnabled(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = AccentPurple, checkedTrackColor = PrimaryPurple)
                            )
                        }
                    }
                    2 -> {
                        Text("Visualiseur Audio", color = AccentPurple, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        Text("Affiche des barres réagissant à la musique sur l'écran du lecteur. Nécessite l'autorisation d'enregistrer l'audio (le micro n'est pas utilisé pour vous écouter).", color = Color.White, textAlign = TextAlign.Center, fontSize = 16.sp)
                        Spacer(Modifier.height(32.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text("Activer le visualiseur", color = Color.White, modifier = Modifier.weight(1f))
                            Switch(
                                checked = visualizer,
                                onCheckedChange = {
                                    if (it) {
                                        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                                            permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                        } else {
                                            visualizer = true
                                            prefs.edit().putBoolean("visualizer_enabled", true).apply()
                                        }
                                    } else {
                                        visualizer = false
                                        prefs.edit().putBoolean("visualizer_enabled", false).apply()
                                    }
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = AccentPurple, checkedTrackColor = PrimaryPurple)
                            )
                        }
                    }
                    3 -> {
                        Text("Filtres & Tri", color = AccentPurple, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        Text("Choisissez l'ordre d'affichage par défaut de vos musiques dans la bibliothèque.", color = Color.White, textAlign = TextAlign.Center, fontSize = 16.sp)
                        Spacer(Modifier.height(32.dp))
                        
                        val sortOptions = mapOf("popular" to "Les plus écoutés", "date_desc" to "Plus récent", "date_asc" to "Plus ancien", "alpha_asc" to "Nom (A-Z)", "alpha_desc" to "Nom (Z-A)", "artist" to "Par Artiste")
                        
                        @OptIn(ExperimentalMaterial3Api::class)
                        ExposedDropdownMenuBox(expanded = sortExpanded, onExpandedChange = { sortExpanded = it }) {
                            OutlinedTextField(
                                value = sortOptions[sortMode] ?: "Plus récent",
                                onValueChange = {}, readOnly = true, label = { Text("Trier par défaut") },
                                modifier = Modifier.fillMaxWidth().menuAnchor(), trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sortExpanded) },
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )
                            ExposedDropdownMenu(expanded = sortExpanded, onDismissRequest = { sortExpanded = false }, modifier = Modifier.background(BgPanel)) {
                                sortOptions.forEach { (key, label) -> DropdownMenuItem(text = { Text(label, color = Color.White) }, onClick = { sortMode = key; session.saveSortMode(key); sortExpanded = false }) }
                            }
                        }
                    }
                    4 -> {
                        Text("Égaliseur", color = AccentPurple, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        Text("Activez l'égaliseur audio intégré pour modifier les fréquences et ajuster le son à votre écoute.", color = Color.White, textAlign = TextAlign.Center, fontSize = 16.sp)
                        Spacer(Modifier.height(32.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text("Activer l'égaliseur", color = Color.White, modifier = Modifier.weight(1f))
                            Switch(
                                checked = eqEnabled,
                                onCheckedChange = { eqEnabled = it; prefs.edit().putBoolean("eq_enabled", it).apply() },
                                colors = SwitchDefaults.colors(checkedThumbColor = AccentPurple, checkedTrackColor = PrimaryPurple)
                            )
                        }
                    }
                    5 -> {
                        Text("Mise en Cache", color = AccentPurple, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        Text("Sauvegardez les pochettes des musiques sur votre appareil pour les charger instantanément la prochaine fois et économiser vos données.", color = Color.White, textAlign = TextAlign.Center, fontSize = 16.sp)
                        Spacer(Modifier.height(32.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text("Activer le cache des covers", color = Color.White, modifier = Modifier.weight(1f))
                            Switch(
                                checked = coverCache,
                                onCheckedChange = { coverCache = it; session.setCoverCacheEnabled(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = AccentPurple, checkedTrackColor = PrimaryPurple)
                            )
                        }
                    }
                }
            }
        }

        if (step > 0) {
            Spacer(Modifier.height(16.dp))

            // Navigation Buttons
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { step-- }) {
                    Text("Retour", color = TextGray)
                }
                
                Row(horizontalArrangement = Arrangement.Center) {
                    for (i in 1 until totalSteps) {
                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (i == step) AccentPurple else TextGray.copy(alpha = 0.5f))
                        )
                    }
                }
                
                if (step < totalSteps - 1) {
                    TextButton(onClick = { step++ }) {
                        Text("Suivant", color = AccentPurple, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = {
                            prefs.edit().putBoolean("tutorial_v2_completed", true).apply()
                            onFinish()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                    ) {
                        Text("Terminer", color = Color.White)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrackRowWithMenu(
    track: Track,
    session: SessionManager,
    canEdit: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onAddToPlaylist: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showMenu by remember { mutableStateOf(false) }
    var isDownloaded by remember { mutableStateOf(DownloadManager.isDownloaded(context, track.id)) }
    var downloadProgress by remember { mutableIntStateOf(-1) }

    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onClick() }.padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = buildImageRequest(context, track.cover_url, session.isCoverCacheEnabled()),
            contentDescription = null,
            modifier = Modifier.size(50.dp).clip(RoundedCornerShape(10.dp)).background(Color.DarkGray),
            contentScale = ContentScale.Crop
        )

        Spacer(Modifier.width(16.dp))

        Column(Modifier.weight(1f)) {
            // Titre
            Text(
                track.title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                maxLines = 1,
                modifier = Modifier.basicMarquee()
            )

            // Ligne Artiste (scrollable si trop long) + Genre/Vues (fixe)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = track.artist,
                    color = TextGray,
                    fontSize = 13.sp,
                    maxLines = 1,
                    // fill=false permet de coller au genre tant que c'est court
                    // weight(1f) permet au marquee de s'activer si le texte dépasse
                    modifier = Modifier.weight(1f, fill = false).basicMarquee()
                )

                Text(
                    text = " • ${track.genre ?: "Autre"} • ▶ ${track.play_count ?: 0}",
                    color = TextGray,
                    fontSize = 13.sp,
                    maxLines = 1,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            if (downloadProgress in 0..99) {
                LinearProgressIndicator(
                    progress = { downloadProgress / 100f },
                    modifier = Modifier.fillMaxWidth().height(2.dp).padding(top = 2.dp),
                    color = AccentPurple
                )
            }
        }

        Box {
            IconButton(onClick = { showMenu = true }) { Icon(Icons.Rounded.MoreVert, null, tint = TextGray) }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }, modifier = Modifier.background(BgPanel)) {
                DropdownMenuItem(text = { Text("Ajouter à un mix", color = Color.White) }, leadingIcon = { Icon(Icons.Rounded.PlaylistAdd, null, tint = Color.White) }, onClick = { showMenu = false; onAddToPlaylist() })
                if (!isDownloaded) {
                    DropdownMenuItem(text = { Text("Télécharger", color = Color.White) }, leadingIcon = { Icon(Icons.Rounded.Download, null, tint = AccentPurple) }, onClick = {
                        showMenu = false
                        scope.launch { downloadProgress = 0; if (DownloadManager.downloadTrack(context, track) { downloadProgress = it }) isDownloaded = true; downloadProgress = -1 }
                    })
                } else {
                    DropdownMenuItem(text = { Text("Supprimer hors-ligne", color = Color.Red) }, onClick = { showMenu = false; DownloadManager.deleteDownloadedTrack(context, track.id.toString()); isDownloaded = false })
                }
                if (canEdit) DropdownMenuItem(text = { Text("Modifier", color = Color.White) }, onClick = { showMenu = false; onEdit() })
            }
        }
    }
    HorizontalDivider(color = Color.White.copy(alpha = 0.05f), thickness = 1.dp)
}

// ─── Ecrans de création/sélection de Playlist (FULL SCREEN) ───────────────────
@Composable
fun SelectPlaylistScreen(trackId: Int, allPlaylists: List<Playlist>, session: SessionManager, onBack: () -> Unit, onNewPlaylist: () -> Unit, onSuccess: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val myPlaylists = remember(allPlaylists) { allPlaylists.filter { it.creator_id == session.getUserId() || session.isAdmin() } }

    Column(Modifier.fillMaxSize().background(BgDark).padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, null, tint = Color.White) }
            Text("Ajouter à un Mix", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = onNewPlaylist, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)) {
            Icon(Icons.Rounded.Add, null, tint = Color.White); Spacer(Modifier.width(8.dp)); Text("NOUVEAU MIX", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(myPlaylists, key = { it.id }) { p ->
                Card(colors = CardDefaults.cardColors(containerColor = BgPanel), modifier = Modifier.fillMaxWidth().clickable {
                    scope.launch {
                        try { ApiClient.service.modPlaylist(p.id, session.getUsername(), session.getPassword(), "add", trackId, null); Toast.makeText(context, "Ajouté à ${p.name}", Toast.LENGTH_SHORT).show(); onSuccess() } catch (e: Exception) {}
                    }
                }) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Rounded.Album, null, tint = AccentPurple); Spacer(Modifier.width(16.dp)); Text(p.name, color = Color.White, fontSize = 16.sp) } }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlaylistBuilderScreen(allTracks: List<Track>, session: SessionManager, preSelectedId: Int?, onBack: () -> Unit, onSuccess: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var search by remember { mutableStateOf("") }
    val selectedIds = remember { mutableStateListOf<Int>() }
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { preSelectedId?.let { selectedIds.add(it) } }

    val displayList = remember(search, allTracks) { allTracks.filter { it.title.contains(search, true) || it.artist.contains(search, true) } }

    Column(Modifier.fillMaxSize().background(BgDark)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, null, tint = Color.White) }
            Text("Créer un Mix", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp).weight(1f))
            if (name.isNotBlank() && selectedIds.isNotEmpty() && !isSaving) {
                TextButton(onClick = {
                    scope.launch {
                        isSaving = true
                        try {
                            ApiClient.service.createPlaylist(name, session.getUsername(), session.getPassword())
                            val updatedPlaylists = ApiClient.service.getPlaylists()
                            val newPlaylist = updatedPlaylists.filter { it.creator_id == session.getUserId() && it.name == name }.maxByOrNull { it.id }
                            if (newPlaylist != null) {
                                selectedIds.forEach { tid -> ApiClient.service.modPlaylist(newPlaylist.id, session.getUsername(), session.getPassword(), "add", tid, null) }
                                Toast.makeText(context, "Mix créé avec ${selectedIds.size} titres", Toast.LENGTH_SHORT).show()
                                onSuccess()
                            }
                        } catch (e: Exception) {}
                        isSaving = false
                    }
                }) { Text("Sauver", color = AccentPurple, fontWeight = FontWeight.Bold) }
            } else if (isSaving) { CircularProgressIndicator(color = AccentPurple, modifier = Modifier.size(24.dp)) }
        }

        OutlinedTextField(value = name, onValueChange = { name = it }, placeholder = { Text("Nom du Mix...", color = TextGray) }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
        Spacer(Modifier.height(10.dp))
        TextField(value = search, onValueChange = { search = it }, placeholder = { Text("Rechercher des musiques...", color = TextGray) }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(50.dp), shape = RoundedCornerShape(25.dp), colors = TextFieldDefaults.colors(focusedContainerColor = BgPanel, unfocusedContainerColor = BgPanel, focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent))
        Spacer(Modifier.height(16.dp))

        LazyColumn(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp)) {
            items(displayList, key = { it.id }) { track ->
                val isSelected = selectedIds.contains(track.id)
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { if (isSelected) selectedIds.remove(track.id) else selectedIds.add(track.id) }.background(if (isSelected) PrimaryPurple.copy(alpha=0.3f) else Color.Transparent).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(model = buildImageRequest(context, track.cover_url, session.isCoverCacheEnabled()), contentDescription = null, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(track.title, color = Color.White, fontSize = 14.sp, maxLines = 1, modifier = Modifier.basicMarquee())
                        Text(track.artist, color = TextGray, fontSize = 12.sp, maxLines = 1)
                    }
                    Checkbox(checked = isSelected, onCheckedChange = null, colors = CheckboxDefaults.colors(checkedColor = AccentPurple, uncheckedColor = TextGray))
                }
            }
        }
    }
}

// ─── Playlists / Mixs Tab ─────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlaylistsScreenImpl(playlists: List<Playlist>, allTracks: List<Track>, session: SessionManager, onRefresh: () -> Unit, onPlay: (String) -> Unit, onNavigateCreate: () -> Unit) {
    var managePlaylist by remember { mutableStateOf<Playlist?>(null) }
    if (managePlaylist != null) ManagePlaylistDialog(managePlaylist!!, allTracks, session, { managePlaylist = null }, { managePlaylist = null; onRefresh() })

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("| Tes Mixs", color = AccentPurple, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = onNavigateCreate, modifier = Modifier.size(40.dp).background(PrimaryPurple, CircleShape)) { Icon(Icons.Rounded.Add, "Créer", tint = Color.White) }
        }
        Spacer(Modifier.height(20.dp))
        LazyVerticalGrid(columns = GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(playlists, key = { it.id }) { p ->
                val canManage = session.isAdmin() || p.creator_id == session.getUserId()
                Card(colors = CardDefaults.cardColors(containerColor = BgPanel), modifier = Modifier.height(140.dp).clickable { onPlay(p.song_ids) }) {
                    Box(Modifier.fillMaxSize()) {
                        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Rounded.Album, null, tint = AccentPurple, modifier = Modifier.size(40.dp))
                            Spacer(Modifier.height(10.dp))
                            Text(p.name, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                        if (canManage) IconButton(onClick = { managePlaylist = p }, modifier = Modifier.align(Alignment.TopEnd)) { Icon(Icons.Rounded.MoreVert, null, tint = TextGray) }
                    }
                }
            }
        }
    }
}

@Composable
fun ManagePlaylistDialog(playlist: Playlist, allTracks: List<Track>, session: SessionManager, onDismiss: () -> Unit, onSuccess: () -> Unit) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(playlist.name) }
    val ids = playlist.song_ids.split(",").mapNotNull { it.trim().toIntOrNull() }
    val pTracks = allTracks.filter { it.id in ids }

    AlertDialog(
        onDismissRequest = onDismiss, containerColor = BgPanel,
        title = { Text("Gérer Playlist", color = Color.White) },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nom") }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White))
                Spacer(Modifier.height(10.dp))
                LazyColumn(Modifier.height(150.dp)) {
                    items(pTracks, key = { it.id }) { t ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(t.title, color = Color.White, maxLines = 1, modifier = Modifier.weight(1f))
                            IconButton(onClick = { scope.launch { ApiClient.service.modPlaylist(playlist.id, session.getUsername(), session.getPassword(), "remove", t.id, null); onSuccess() } }) { Icon(Icons.Rounded.Close, null, tint = Color.Red) }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = { scope.launch { ApiClient.service.modPlaylist(playlist.id, session.getUsername(), session.getPassword(), "delete", null, null); onSuccess() } }) { Text("Supprimer", color = Color.Red) }
                TextButton(onClick = { scope.launch { ApiClient.service.modPlaylist(playlist.id, session.getUsername(), session.getPassword(), "rename", null, name); onSuccess() } }) { Text("Sauver", color = AccentPurple) }
            }
        }
    )
}

// ─── Écran Téléchargements ────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DownloadsScreen(session: SessionManager, onPlay: (DownloadedTrack, List<DownloadedTrack>) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    var downloadedTracks by remember { mutableStateOf(DownloadManager.getDownloadedTracks(context)) }
    var search by remember { mutableStateOf("") }
    val displayList = remember(downloadedTracks, search) { downloadedTracks.filter { it.title.contains(search, true) || it.artist.contains(search, true) } }
    val isOfflineMode = session.getUserId() == -1

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (isOfflineMode) IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, null, tint = Color.White) }
            Text("| Hors-ligne", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("${downloadedTracks.size} titre(s)", color = TextGray, fontSize = 13.sp)
        }
        Spacer(Modifier.height(16.dp))
        TextField(value = search, onValueChange = { search = it }, placeholder = { Text("Rechercher...", color = TextGray) }, colors = TextFieldDefaults.colors(focusedContainerColor = Color(0xFF241B36), unfocusedContainerColor = Color(0xFF241B36), focusedTextColor = Color.White, unfocusedIndicatorColor = Color.Transparent, focusedIndicatorColor = Color.Transparent), shape = RoundedCornerShape(50), modifier = Modifier.fillMaxWidth().height(55.dp))
        Spacer(Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(displayList, key = { it.id }) { track ->
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onPlay(track, displayList) }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (track.coverPath.isNotEmpty()) AsyncImage(model = buildImageRequest(context, File(track.coverPath), session.isCoverCacheEnabled()), contentDescription = null, modifier = Modifier.size(50.dp).clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop)
                    else Box(Modifier.size(50.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF2E2445)), contentAlignment = Alignment.Center) { Text("♫", color = AccentPurple, fontSize = 20.sp) }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(track.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, modifier = Modifier.basicMarquee())
                        Text(track.artist, color = TextGray, fontSize = 13.sp, maxLines = 1)
                    }
                    IconButton(onClick = { DownloadManager.deleteDownloadedTrack(context, track.id); downloadedTracks = DownloadManager.getDownloadedTracks(context) }) { Icon(Icons.Rounded.DeleteOutline, null, tint = Color.Red) }
                }
            }
        }
    }
}

// ─── Lecteurs & Autres ────────────────────────────────────────────────────────
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MiniPlayer(t: String, a: String, c: String, p: Boolean, prog: Float, pp: () -> Unit, clk: () -> Unit, session: SessionManager) {
    Card(colors = CardDefaults.cardColors(containerColor = BgPanel), modifier = Modifier.fillMaxWidth().height(85.dp).clickable { clk() }, shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)) {
        Column {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(model = buildImageRequest(LocalContext.current, c.ifEmpty { R.drawable.default_cover }, session.isCoverCacheEnabled()), contentDescription = null, modifier = Modifier.size(50.dp).clip(RoundedCornerShape(10.dp)).background(Color.DarkGray), contentScale = ContentScale.Crop)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(t, color = Color.White, maxLines = 1, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.basicMarquee())
                    Text(a, color = AccentPurple, maxLines = 1, fontSize = 12.sp, modifier = Modifier.basicMarquee())
                }
                IconButton(onClick = pp, Modifier.size(45.dp).background(Color.White, CircleShape)) { Icon(if (p) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = BgDark, modifier = Modifier.size(28.dp)) }
            }
            LinearProgressIndicator(progress = { prog }, modifier = Modifier.fillMaxWidth().height(2.dp), color = AccentPurple)
        }
    }
}

@Composable
fun QueueScreen(controller: MediaController?, currentIndex: Int, queueVersion: Int, session: SessionManager, onPlay: (Int) -> Unit, onClose: () -> Unit) {
    val queue = remember { mutableStateListOf<MediaItem>() }
    LaunchedEffect(controller, queueVersion) {
        queue.clear()
        controller?.let { for (i in 0 until it.mediaItemCount) queue.add(it.getMediaItemAt(i)) }
    }
    Column(Modifier.fillMaxSize().background(BgDark).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.Rounded.KeyboardArrowDown, null, tint = Color.White) }
            Text("File d'attente", color = AccentPurple, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 10.dp))
        }
        Spacer(Modifier.height(10.dp))
        LazyColumn {
            itemsIndexed(queue, key = { _, item -> item.mediaId }) { idx, item ->
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { onPlay(idx) }, verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(model = buildImageRequest(LocalContext.current, item.mediaMetadata.artworkUri ?: R.drawable.default_cover, session.isCoverCacheEnabled()), contentDescription = null, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item.mediaMetadata.title.toString(), color = if (idx == currentIndex) AccentPurple else Color.White, fontWeight = FontWeight.SemiBold)
                        Text(item.mediaMetadata.artist.toString(), color = TextGray, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun BottomNavBar(curr: String?, nav: (String) -> Unit) {
    NavigationBar(containerColor = NavBg) {
        listOf(
            Triple("home", "Biblio", Icons.Rounded.Home),
            Triple("mixs", "Mixs", Icons.Rounded.LibraryMusic),
            Triple("create", "Upload", Icons.Rounded.AddBox),
            Triple("downloads", "Hors-ligne", Icons.Rounded.DownloadForOffline),
            Triple("queue", "File", Icons.Rounded.QueueMusic)
        ).forEach { (r, l, i) ->
            NavigationBarItem(
                icon = { Icon(i, null, Modifier.size(26.dp)) }, label = { Text(l, fontSize = 10.sp) },
                selected = curr == r, onClick = { nav(r) },
                colors = NavigationBarItemDefaults.colors(selectedIconColor = AccentPurple, selectedTextColor = AccentPurple, indicatorColor = Color.Transparent, unselectedIconColor = TextGray, unselectedTextColor = TextGray)
            )
        }
    }
}

// ─── UPLOAD SCREEN (Toujours accessible via le + de la NavBar) ────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadScreenImpl(session: SessionManager, onUploadSuccess: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf("") }
    var genre by remember { mutableStateOf("Autre") }
    var genreExpanded by remember { mutableStateOf(false) }
    var audioUri by remember { mutableStateOf<Uri?>(null) }
    var coverUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val audioLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { audioUri = it }
    val coverLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { coverUri = it }

    Column(Modifier.fillMaxSize().padding(24.dp).background(BgDark)) {
        Spacer(Modifier.height(20.dp))
        Text("Publier un Titre", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(30.dp))
        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Titre") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(value = artist, onValueChange = { artist = it }, label = { Text("Artiste") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
        Spacer(Modifier.height(10.dp))
        ExposedDropdownMenuBox(expanded = genreExpanded, onExpandedChange = { genreExpanded = it }) {
            OutlinedTextField(value = genre, onValueChange = { genre = it }, label = { Text("Genre") }, modifier = Modifier.fillMaxWidth().menuAnchor(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White), trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genreExpanded) })
            ExposedDropdownMenu(expanded = genreExpanded, onDismissRequest = { genreExpanded = false }, modifier = Modifier.background(BgPanel)) {
                listOf("Phonk/Funk", "Rap", "Pop", "Rock", "Electro", "Hyperpop", "Nightcore", "Autre").forEach { s -> DropdownMenuItem(text = { Text(s, color = Color.White) }, onClick = { genre = s; genreExpanded = false }) }
            }
        }
        Spacer(Modifier.height(20.dp))
        Button(onClick = { audioLauncher.launch("audio/*") }, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = BgPanel)) { Text(if (audioUri != null) "✅ Audio OK" else "Choisir MP3", color = if (audioUri != null) AccentPurple else Color.White) }
        Spacer(Modifier.height(10.dp))
        Button(onClick = { coverLauncher.launch("image/*") }, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = BgPanel)) { Text(if (coverUri != null) "✅ Cover OK" else "Choisir Cover", color = if (coverUri != null) AccentPurple else Color.White) }
        Spacer(Modifier.height(30.dp))
        Button(
            onClick = {
                if (audioUri == null) return@Button
                scope.launch {
                    isLoading = true
                    val audioFile = uriToFile(audioUri!!, context, isAudio = true)
                    val audioPart = MultipartBody.Part.createFormData("music", audioFile.name, audioFile.asRequestBody("audio/mpeg".toMediaTypeOrNull()))
                    var coverPart: MultipartBody.Part? = null
                    if (coverUri != null) { val f = uriToFile(coverUri!!, context); coverPart = MultipartBody.Part.createFormData("cover", f.name, f.asRequestBody("image/*".toMediaTypeOrNull())) }
                    try {
                        ApiClient.service.uploadTrack(title.toRequestBody("text/plain".toMediaTypeOrNull()), artist.toRequestBody("text/plain".toMediaTypeOrNull()), session.getUsername().toRequestBody("text/plain".toMediaTypeOrNull()), session.getPassword().toRequestBody("text/plain".toMediaTypeOrNull()), genre.toRequestBody("text/plain".toMediaTypeOrNull()), audioPart, coverPart)
                        Toast.makeText(context, "Succès", Toast.LENGTH_SHORT).show()
                        title = ""; artist = ""; genre = "Autre"; audioUri = null; coverUri = null; onUploadSuccess()
                    } catch (e: Exception) {}
                    isLoading = false
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple), enabled = !isLoading
        ) { if (isLoading) CircularProgressIndicator(color = Color.White) else Text("PUBLIER", fontWeight = FontWeight.Bold) }
    }
}

// --- RESTAURATION: Menu de modification complet ---
@Composable
fun EditTrackDialog(track: Track, session: SessionManager, onDismiss: () -> Unit, onSuccess: () -> Unit) {
    var title by remember { mutableStateOf(track.title) }
    var artist by remember { mutableStateOf(track.artist) }
    var genre by remember { mutableStateOf(track.genre ?: "Autre") }
    var newCoverUri by remember { mutableStateOf<Uri?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { newCoverUri = it }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss, containerColor = BgPanel,
        title = { Text("Modifier", color = Color.White) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(100.dp).clip(RoundedCornerShape(12.dp)).background(Color.DarkGray).clickable { launcher.launch("image/*") }) {
                    AsyncImage(model = newCoverUri ?: track.cover_url, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    Icon(Icons.Rounded.Edit, null, tint = Color.White, modifier = Modifier.align(Alignment.Center))
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Titre") }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                OutlinedTextField(value = artist, onValueChange = { artist = it }, label = { Text("Artiste") }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                OutlinedTextField(value = genre, onValueChange = { genre = it }, label = { Text("Genre") }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
            }
        },
        confirmButton = {
            TextButton(onClick = {
                scope.launch {
                    val tid = track.id.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                    val u = session.getUsername().toRequestBody("text/plain".toMediaTypeOrNull())
                    val p = session.getPassword().toRequestBody("text/plain".toMediaTypeOrNull())
                    val t = title.toRequestBody("text/plain".toMediaTypeOrNull())
                    val a = artist.toRequestBody("text/plain".toMediaTypeOrNull())
                    val g = genre.toRequestBody("text/plain".toMediaTypeOrNull())
                    var cp: MultipartBody.Part? = null
                    if (newCoverUri != null) {
                        val f = uriToFile(newCoverUri!!, context)
                        cp = MultipartBody.Part.createFormData("new_cover", f.name, f.asRequestBody("image/*".toMediaTypeOrNull()))
                    }
                    try { ApiClient.service.editTrack(tid, u, p, t, a, g, cp); onSuccess() } catch (e: Exception) {}
                }
            }) { Text("Sauver", color = AccentPurple) }
        },
        dismissButton = {
            TextButton(onClick = {
                scope.launch {
                    try { ApiClient.service.deleteTrack(track.id, session.getUsername(), session.getPassword()); onSuccess() } catch(e:Exception){}
                }
            }) { Text("Supprimer", color = Color.Red) }
        }
    )
}

fun formatTime(ms: Long) = "%d:%02d".format(ms / 1000 / 60, ms / 1000 % 60)

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
