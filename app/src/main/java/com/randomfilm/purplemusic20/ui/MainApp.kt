package com.randomfilm.purplemusic20.ui

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.common.util.concurrent.MoreExecutors
import com.randomfilm.purplemusic20.MusicService
import com.randomfilm.purplemusic20.R
import com.randomfilm.purplemusic20.data.*
import com.randomfilm.purplemusic20.ui.components.BottomNavBar
import com.randomfilm.purplemusic20.ui.components.FullPlayerScreen
import com.randomfilm.purplemusic20.ui.components.MiniPlayer
import com.randomfilm.purplemusic20.ui.screens.*
import com.randomfilm.purplemusic20.ui.theme.LocalAppColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun MainApp(
    currentThemePreset: String,
    currentMaterialYouEnabled: Boolean,
    onThemeChange: (String) -> Unit,
    onMaterialYouChange: (Boolean) -> Unit
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val session = remember { SessionManager(context) }
    val prefs = context.getSharedPreferences("purple_music_state", Context.MODE_PRIVATE)

    var hasCompletedTutorial by remember { mutableStateOf(prefs.getBoolean("tutorial_v2_completed", false)) }

    val readyToListenText = stringResource(R.string.player_ready_to_listen)
    val stoppedText = stringResource(R.string.player_stopped)
    val readyText = stringResource(R.string.player_ready)
    val loadingText = stringResource(R.string.player_loading)
    val lyricsNotFoundForTrackText = stringResource(R.string.player_lyrics_not_found_for_track)
    val lyricsNotFoundText = stringResource(R.string.player_lyrics_not_found)
    val lyricsConnectionErrorText = stringResource(R.string.player_lyrics_connection_error)

    var mediaController by remember { mutableStateOf<MediaController?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentTrackTitle by remember { mutableStateOf(readyToListenText) }
    var currentTrackArtist by remember { mutableStateOf(stoppedText) }
    var currentCoverUrl by remember { mutableStateOf("") }
    // Exposé comme State<Long> stable à FullPlayerScreen pour que le tick de position (500ms)
    // ne recompose que le sous-arbre qui affiche réellement la position, pas tout l'écran.
    val currentPositionState = remember { mutableLongStateOf(0L) }
    var currentPosition by currentPositionState
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
                currentTrackTitle = mc.mediaMetadata.title?.toString() ?: readyText
                currentTrackArtist = mc.mediaMetadata.artist?.toString() ?: ""
                currentCoverUrl = mc.mediaMetadata.artworkUri?.toString() ?: ""
                trackDuration = mc.duration.coerceAtLeast(0)
                currentMediaIndex = mc.currentMediaItemIndex
                mc.volume = appVolume
            }
            mediaController?.addListener(object : Player.Listener {
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    currentTrackTitle = mediaItem?.mediaMetadata?.title?.toString() ?: loadingText
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
        if (currentTrackTitle == readyToListenText || currentTrackTitle.isBlank()) return@LaunchedEffect
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
                        withContext(Dispatchers.Main) { currentPlainLyrics = lyricsNotFoundForTrackText }
                    }
                } else {
                    withContext(Dispatchers.Main) { currentPlainLyrics = lyricsNotFoundText }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { currentPlainLyrics = lyricsConnectionErrorText }
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
        containerColor = LocalAppColors.current.background,
        bottomBar = {
            // Liste des écrans où la barre peut potentiellement s'afficher
            val navScreens = listOf("home", "mixs", "create", "downloads", "queue")

            if (currentRoute in navScreens) {
                val isLoggedIn = session.getUserId() != -1

                Column {
                    // Affichage du MiniPlayer (si une musique est chargée/joue)
                    if (mediaController != null && currentTrackTitle != readyToListenText) {
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
                        currentThemePreset = currentThemePreset,
                        currentMaterialYouEnabled = currentMaterialYouEnabled,
                        onThemeChange = onThemeChange,
                        onMaterialYouChange = onMaterialYouChange,
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
                        onOpenPlaylist = { playlist -> navController.navigate("playlist_detail/${playlist.id}") },
                        onRedoTutorial = { navController.navigate("tutorial") }
                    )
                }

                composable("mixs") {
                    PlaylistsScreenImpl(
                        allPlaylists, allTracks, session, reloadData,
                        onOpenPlaylist = { playlist -> navController.navigate("playlist_detail/${playlist.id}") },
                        onNavigateCreate = { navController.navigate("build_playlist") }
                    )
                }

                composable(
                    "playlist_detail/{playlistId}",
                    arguments = listOf(navArgument("playlistId") { type = NavType.IntType })
                ) { backStack ->
                    val pid = backStack.arguments?.getInt("playlistId") ?: return@composable
                    val playlist = allPlaylists.find { it.id == pid }
                    if (playlist != null) {
                        PlaylistDetailScreen(
                            playlist = playlist,
                            allTracks = allTracks,
                            session = session,
                            onBack = { navController.popBackStack() },
                            onPlayFrom = { tracks, startTrack -> playMusic(tracks, startTrack, false) },
                            onAddToPlaylist = { track -> navController.navigate("select_playlist/${track.id}") },
                            onRefresh = reloadData
                        )
                    }
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
                        currentPositionState,
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
