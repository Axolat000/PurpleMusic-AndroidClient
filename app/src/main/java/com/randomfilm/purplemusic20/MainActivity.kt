package com.randomfilm.purplemusic20

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.media3.common.*
import androidx.media3.session.*
import androidx.navigation.compose.*
import coil.compose.AsyncImage
import com.google.common.util.concurrent.MoreExecutors
import com.randomfilm.purplemusic20.ui.theme.PurpleMusic20Theme
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset

// COLORS
val BgDark = Color(0xFF0F0C1D)
val BgPanel = Color(0xFF1B1429)
val PrimaryPurple = Color(0xFF8E44AD)
val AccentPurple = Color(0xFFBB86FC)
val TextGray = Color(0xFFA196B4)
val NavBg = Color(0xFF151020)

class MainActivity : ComponentActivity() {
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

@Composable
fun MainApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val session = remember { SessionManager(context) }

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

    val reloadData: () -> Unit = {
        val scope = CoroutineScope(Dispatchers.Main)
        scope.launch {
            try {
                allTracks = ApiClient.service.getTracks()
                allPlaylists = ApiClient.service.getPlaylists()
            } catch (e: Exception) {}
        }
    }

    LaunchedEffect(Unit) {
        // Initialise ApiClient si un serveur est déjà enregistré
        if (session.hasServerUrl()) {
            ApiClient.init(session.getServerUrl())
            reloadData()
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
                                ApiClient.service.incrementPlay(trackId, session.getUsername(), session.getPassword())
                            } catch (e: Exception) {}
                        }
                    }
                }
                override fun onIsPlayingChanged(p: Boolean) { isPlaying = p }
                override fun onShuffleModeEnabledChanged(e: Boolean) { shuffleMode = e }
                override fun onRepeatModeChanged(m: Int) { repeatMode = m }
                override fun onPlaybackStateChanged(s: Int) {
                    if (s == Player.STATE_READY) trackDuration = mediaController?.duration?.coerceAtLeast(0) ?: 0L
                }
                override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                    queueVersion++
                    currentMediaIndex = mediaController?.currentMediaItemIndex ?: 0
                    trackDuration = mediaController?.duration?.coerceAtLeast(0) ?: 0L
                }
            })
        }, MoreExecutors.directExecutor())
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) { mediaController?.let { currentPosition = it.currentPosition }; delay(500L) }
    }

    fun playMusic(tracks: List<Track>, startTrack: Track, isGlobal: Boolean) {
        isGlobalQueue = isGlobal
        if (isGlobal) currentGlobalQueueIds = tracks.map { it.id.toString() }
        mediaController?.let { player ->
            val index = tracks.indexOfFirst { it.id == startTrack.id }
            if (index != -1) {
                val items = tracks.map { t ->
                    MediaItem.Builder().setUri(t.stream_url).setMediaId(t.id.toString())
                        .setMediaMetadata(MediaMetadata.Builder().setTitle(t.title).setArtist(t.artist).setArtworkUri(Uri.parse(t.cover_url)).build())
                        .build()
                }
                player.setMediaItems(items, index, 0)
                player.prepare(); player.play()
            }
        }
    }

    // Destination de départ selon l'état
    val startDest = when {
        !session.hasServerUrl()   -> "server_setup"
        session.getUserId() != -1 -> "home"
        else                      -> "login"
    }

    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    Scaffold(
        containerColor = BgDark,
        bottomBar = {
            if (currentRoute != "login" && currentRoute != "full_player" && currentRoute != "server_setup") {
                Column {
                    if (mediaController != null && currentTrackTitle != "Prêt à écouter") {
                        val progress = if (trackDuration > 0) currentPosition.toFloat() / trackDuration.toFloat() else 0f
                        MiniPlayer(
                            currentTrackTitle, currentTrackArtist, currentCoverUrl, isPlaying, progress,
                            { if (isPlaying) mediaController?.pause() else mediaController?.play() },
                            { navController.navigate("full_player") }
                        )
                    }
                    BottomNavBar(currentRoute) { navController.navigate(it) { popUpTo("home"); launchSingleTop = true } }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController, startDest, Modifier.padding(padding),
            enterTransition = { fadeIn(tween(300)) },
            exitTransition = { fadeOut(tween(300)) }
        ) {
            // Écran configuration serveur (premier lancement)
            composable("server_setup") {
                ServerSetupScreen(session) {
                    navController.navigate("login") { popUpTo("server_setup") { inclusive = true } }
                }
            }

            // Écran login / inscription
            composable("login") {
                LoginScreenImpl(session) {
                    reloadData()
                    navController.navigate("home") { popUpTo("login") { inclusive = true } }
                }
            }

            composable("home") {
                HomeScreenImpl(
                    tracks = allTracks,
                    playlists = allPlaylists,
                    session = session,
                    currentVolume = appVolume,
                    currentSortMode = appSortMode,
                    onVolumeChange = { vol -> appVolume = vol; mediaController?.volume = vol; session.saveVolume(vol) },
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
                                    MediaItem.Builder().setUri(t.stream_url).setMediaId(t.id.toString())
                                        .setMediaMetadata(MediaMetadata.Builder().setTitle(t.title).setArtist(t.artist).setArtworkUri(Uri.parse(t.cover_url)).build())
                                        .build()
                                }
                                val isCurrentStillThere = newItems.any { it.mediaId == currentMediaId }
                                if (isCurrentStillThere) {
                                    player.replaceMediaItems(0, player.mediaItemCount, newItems)
                                } else {
                                    if (newItems.isNotEmpty()) { player.setMediaItems(newItems, 0, 0); player.prepare(); player.play() }
                                    else player.clearMediaItems()
                                }
                            }
                        }
                    },
                    onRefresh = reloadData,
                    onLogout = {
                        session.logout()
                        navController.navigate("login") { popUpTo("home") { inclusive = true } }
                    }
                )
            }

            composable("mixs") {
                PlaylistsScreenImpl(allPlaylists, allTracks, session, reloadData) { idsStr ->
                    val ids = idsStr.split(",").mapNotNull { it.trim().toIntOrNull() }
                    val hidden = session.getHiddenGenres()
                    val pTracks = ids.mapNotNull { id -> allTracks.find { it.id == id } }
                        .filter { !(hidden.contains(it.genre ?: "Autre")) }
                    if (pTracks.isNotEmpty()) playMusic(pTracks, pTracks[0], false)
                    else Toast.makeText(context, "Mix vide ou musiques filtrées", Toast.LENGTH_SHORT).show()
                }
            }

            composable("create") { CreatePlaylistScreenImpl { reloadData(); navController.navigate("mixs") } }
            composable("upload") { UploadScreenImpl(reloadData) }

            composable("full_player",
                enterTransition = { slideInVertically(initialOffsetY = { it }, animationSpec = tween(400)) },
                exitTransition = { slideOutVertically(targetOffsetY = { it }, animationSpec = tween(400)) }
            ) {
                FullPlayerScreen(
                    currentTrackTitle, currentTrackArtist, currentCoverUrl,
                    isPlaying, currentPosition, trackDuration, shuffleMode, repeatMode,
                    { navController.popBackStack() },
                    { if (isPlaying) mediaController?.pause() else mediaController?.play() },
                    { mediaController?.seekToNext() }, { mediaController?.seekToPrevious() },
                    { mediaController?.seekTo(it); currentPosition = it },
                    { mediaController?.shuffleModeEnabled = !shuffleMode },
                    {
                        mediaController?.repeatMode = when (repeatMode) {
                            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                            else -> Player.REPEAT_MODE_OFF
                        }
                    },
                    { navController.navigate("queue") }
                )
            }

            composable("queue",
                enterTransition = { slideInVertically(initialOffsetY = { it }, animationSpec = tween(300)) },
                exitTransition = { slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300)) }
            ) {
                QueueScreen(mediaController, currentMediaIndex, queueVersion,
                    { idx -> mediaController?.seekTo(idx, 0); mediaController?.play() },
                    { navController.popBackStack() }
                )
            }
        }
    }
}

// ─── Écran configuration serveur (premier lancement) ─────────────────────────

@Composable
fun ServerSetupScreen(session: SessionManager, onConfirm: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var serverUrl by remember { mutableStateOf("") }
    var isChecking by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().background(BgDark).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("PURPLE", color = AccentPurple, fontSize = 48.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(8.dp))
        Text("Configuration du serveur", color = TextGray, fontSize = 14.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(48.dp))

        Text("Adresse du serveur PurpleMusic", color = TextGray, fontSize = 13.sp, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = serverUrl,
            onValueChange = { serverUrl = it; errorMsg = "" },
            placeholder = { Text("https://exemple.com/music/", color = TextGray) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            colors = outlinedFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )

        if (errorMsg.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(errorMsg, color = Color.Red, fontSize = 13.sp)
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                val url = serverUrl.trim()
                if (url.isEmpty()) { errorMsg = "Veuillez entrer une adresse."; return@Button }
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    errorMsg = "L'adresse doit commencer par http:// ou https://"; return@Button
                }
                scope.launch {
                    isChecking = true; errorMsg = ""
                    try {
                        ApiClient.init(url)
                        ApiClient.service.getTracks() // test de connexion
                        session.saveServerUrl(url)
                        onConfirm()
                    } catch (e: Exception) {
                        errorMsg = "Impossible de joindre le serveur. Vérifiez l'adresse."
                    }
                    isChecking = false
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
            shape = RoundedCornerShape(12.dp),
            enabled = !isChecking
        ) {
            if (isChecking) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            else Text("CONFIRMER", fontWeight = FontWeight.Bold)
        }
    }
}

// ─── Écran login + inscription ────────────────────────────────────────────────

@Composable
fun LoginScreenImpl(session: SessionManager, onSuccess: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) }

    // Login
    var loginUser by remember { mutableStateOf("") }
    var loginPass by remember { mutableStateOf("") }
    var loginPassVisible by remember { mutableStateOf(false) }
    var loginLoading by remember { mutableStateOf(false) }

    // Inscription
    var regUser by remember { mutableStateOf("") }
    var regPass by remember { mutableStateOf("") }
    var regPass2 by remember { mutableStateOf("") }
    var regPassVisible by remember { mutableStateOf(false) }
    var regLoading by remember { mutableStateOf(false) }

    // Dialog changer de serveur
    var showServerDialog by remember { mutableStateOf(false) }
    var serverUrlInput by remember { mutableStateOf(session.getServerUrl()) }
    var serverChecking by remember { mutableStateOf(false) }
    var serverError by remember { mutableStateOf("") }

    if (showServerDialog) {
        AlertDialog(
            onDismissRequest = { showServerDialog = false },
            containerColor = BgPanel,
            title = { Text("Changer de serveur", color = Color.White) },
            text = {
                Column {
                    Text("Adresse du serveur :", color = TextGray, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = serverUrlInput,
                        onValueChange = { serverUrlInput = it; serverError = "" },
                        placeholder = { Text("https://exemple.com/music/", color = TextGray) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        colors = outlinedFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (serverError.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Text(serverError, color = Color.Red, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val url = serverUrlInput.trim()
                    if (url.isEmpty()) { serverError = "Adresse vide."; return@TextButton }
                    if (!url.startsWith("http://") && !url.startsWith("https://")) {
                        serverError = "Doit commencer par http:// ou https://"; return@TextButton
                    }
                    scope.launch {
                        serverChecking = true; serverError = ""
                        try {
                            ApiClient.init(url)
                            ApiClient.service.getTracks()
                            session.saveServerUrl(url)
                            showServerDialog = false
                            Toast.makeText(context, "Serveur mis à jour ✓", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            serverError = "Serveur inaccessible."
                        }
                        serverChecking = false
                    }
                }) {
                    if (serverChecking) CircularProgressIndicator(color = AccentPurple, modifier = Modifier.size(18.dp))
                    else Text("Confirmer", color = AccentPurple)
                }
            },
            dismissButton = { TextButton(onClick = { showServerDialog = false }) { Text("Annuler", color = TextGray) } }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().background(BgDark).padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("PURPLE", color = AccentPurple, fontSize = 48.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(6.dp))

        // URL actuelle + bouton changer
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = session.getServerUrl().removePrefix("https://").removePrefix("http://").trimEnd('/'),
                color = TextGray, fontSize = 12.sp, maxLines = 1,
                modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(Modifier.width(6.dp))
            TextButton(
                onClick = { serverUrlInput = session.getServerUrl(); showServerDialog = true },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) { Text("Changer", color = AccentPurple, fontSize = 12.sp) }
        }

        Spacer(Modifier.height(32.dp))

        // Onglets
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = BgPanel,
            contentColor = AccentPurple,
            modifier = Modifier.fillMaxWidth(),
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = AccentPurple
                )
            }
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 },
                text = { Text("Connexion", color = if (selectedTab == 0) AccentPurple else TextGray) })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 },
                text = { Text("Créer un compte", color = if (selectedTab == 1) AccentPurple else TextGray) })
        }

        Spacer(Modifier.height(28.dp))

        // ── Onglet Connexion ──────────────────────────────────────────────────
        if (selectedTab == 0) {
            OutlinedTextField(
                value = loginUser, onValueChange = { loginUser = it },
                label = { Text("Utilisateur") }, singleLine = true,
                colors = outlinedFieldColors(), modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = loginPass, onValueChange = { loginPass = it },
                label = { Text("Mot de passe") }, singleLine = true,
                visualTransformation = if (loginPassVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { loginPassVisible = !loginPassVisible }) {
                        Icon(if (loginPassVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, null, tint = TextGray)
                    }
                },
                colors = outlinedFieldColors(), modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(28.dp))
            Button(
                onClick = {
                    scope.launch {
                        loginLoading = true
                        try {
                            val res = ApiClient.service.login(loginUser, loginPass)
                            if (res.status == "success") {
                                session.saveUser(res.user_id ?: 0, res.username ?: "", loginPass, res.is_admin == true)
                                onSuccess()
                            } else Toast.makeText(context, res.message ?: "Erreur", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) { Toast.makeText(context, "Erreur réseau", Toast.LENGTH_SHORT).show() }
                        loginLoading = false
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                shape = RoundedCornerShape(12.dp), enabled = !loginLoading
            ) {
                if (loginLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                else Text("CONNEXION", fontWeight = FontWeight.Bold)
            }
        }

        // ── Onglet Créer un compte ────────────────────────────────────────────
        else {
            OutlinedTextField(
                value = regUser, onValueChange = { regUser = it },
                label = { Text("Nom d'utilisateur") }, singleLine = true,
                colors = outlinedFieldColors(), modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = regPass, onValueChange = { regPass = it },
                label = { Text("Mot de passe") }, singleLine = true,
                visualTransformation = if (regPassVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { regPassVisible = !regPassVisible }) {
                        Icon(if (regPassVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, null, tint = TextGray)
                    }
                },
                colors = outlinedFieldColors(), modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = regPass2, onValueChange = { regPass2 = it },
                label = { Text("Confirmer le mot de passe") }, singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                isError = regPass2.isNotEmpty() && regPass != regPass2,
                supportingText = {
                    if (regPass2.isNotEmpty() && regPass != regPass2)
                        Text("Les mots de passe ne correspondent pas", color = Color.Red)
                },
                colors = outlinedFieldColors(), modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(28.dp))
            Button(
                onClick = {
                    if (regUser.isBlank()) { Toast.makeText(context, "Nom d'utilisateur requis", Toast.LENGTH_SHORT).show(); return@Button }
                    if (regPass.length < 6) { Toast.makeText(context, "Mot de passe trop court (6 min)", Toast.LENGTH_SHORT).show(); return@Button }
                    if (regPass != regPass2) { Toast.makeText(context, "Les mots de passe ne correspondent pas", Toast.LENGTH_SHORT).show(); return@Button }
                    scope.launch {
                        regLoading = true
                        try {
                            val res = ApiClient.service.register(regUser, regPass)
                            if (res.status == "success") {
                                // Connexion automatique après inscription
                                val loginRes = ApiClient.service.login(regUser, regPass)
                                if (loginRes.status == "success") {
                                    session.saveUser(loginRes.user_id ?: 0, loginRes.username ?: "", regPass, loginRes.is_admin == true)
                                    onSuccess()
                                } else {
                                    Toast.makeText(context, "Compte créé ! Connectez-vous.", Toast.LENGTH_LONG).show()
                                    selectedTab = 0
                                }
                            } else Toast.makeText(context, res.message ?: "Erreur", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) { Toast.makeText(context, "Erreur réseau", Toast.LENGTH_SHORT).show() }
                        regLoading = false
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                shape = RoundedCornerShape(12.dp), enabled = !regLoading
            ) {
                if (regLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                else Text("CRÉER LE COMPTE", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun outlinedFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White, unfocusedTextColor = Color.Gray,
    focusedBorderColor = AccentPurple, unfocusedBorderColor = TextGray,
    cursorColor = AccentPurple, focusedLabelColor = AccentPurple, unfocusedLabelColor = TextGray
)

// --- SETTINGS DIALOG ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(currentHidden: Set<String>, currentVolume: Float, currentSortMode: String, onSave: (Set<String>, String) -> Unit, onVolumeChange: (Float) -> Unit, onDismiss: () -> Unit) {
    var localHidden by remember { mutableStateOf(currentHidden) }
    var localVolume by remember { mutableFloatStateOf(currentVolume) }
    var localSortMode by remember { mutableStateOf(currentSortMode) }
    var customGenreInput by remember { mutableStateOf("") }
    val predefinedGenres = listOf("Phonk/Funk", "Rap", "Pop", "Rock", "Electro", "Hyperpop", "Nightcore", "Qualité inférieure", "Autre")
    val displayGenres = (predefinedGenres + localHidden).distinct()
    val sortOptions = mapOf("popular" to "Les plus écoutés", "date_desc" to "Plus récent", "date_asc" to "Plus ancien", "alpha_asc" to "Nom (A-Z)", "alpha_desc" to "Nom (Z-A)", "artist" to "Par Artiste")
    var sortExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss, containerColor = BgPanel,
        title = { Text("Filtres & Paramètres", color = Color.White) },
        text = {
            Column {
                Text("Volume de l'application :", color = TextGray, fontSize = 14.sp)
                Slider(value = localVolume, onValueChange = { localVolume = it; onVolumeChange(it) }, valueRange = 0f..1f, colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = AccentPurple, inactiveTrackColor = Color.White.copy(0.2f)))
                Spacer(Modifier.height(10.dp))
                ExposedDropdownMenuBox(expanded = sortExpanded, onExpandedChange = { sortExpanded = it }) {
                    OutlinedTextField(value = sortOptions[localSortMode] ?: "Plus récent", onValueChange = {}, readOnly = true, label = { Text("Trier par défaut") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sortExpanded) }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White), modifier = Modifier.fillMaxWidth().menuAnchor())
                    ExposedDropdownMenu(expanded = sortExpanded, onDismissRequest = { sortExpanded = false }, modifier = Modifier.background(BgPanel)) {
                        sortOptions.forEach { (key, label) -> DropdownMenuItem(text = { Text(label, color = Color.White) }, onClick = { localSortMode = key; sortExpanded = false }) }
                    }
                }
                Spacer(Modifier.height(15.dp))
                Text("Cochez les genres à masquer :", color = TextGray, fontSize = 14.sp)
                LazyColumn(Modifier.heightIn(max = 160.dp)) {
                    items(displayGenres) { genre ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { localHidden = if (localHidden.contains(genre)) localHidden - genre else localHidden + genre }) {
                            Checkbox(checked = localHidden.contains(genre), onCheckedChange = { checked -> localHidden = if (checked) localHidden + genre else localHidden - genre }, colors = CheckboxDefaults.colors(checkedColor = Color.Red, uncheckedColor = TextGray))
                            Text(genre, color = Color.White)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = customGenreInput, onValueChange = { customGenreInput = it }, label = { Text("Autre genre à masquer", fontSize = 12.sp) }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White), modifier = Modifier.weight(1f).height(60.dp))
                    IconButton(onClick = { if (customGenreInput.isNotBlank()) { localHidden = localHidden + customGenreInput; customGenreInput = "" } }) { Icon(Icons.Rounded.Add, "Ajouter", tint = AccentPurple) }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(localHidden, localSortMode); onDismiss() }) { Text("Fermer", color = AccentPurple) } }
    )
}

// --- HOME & TRACK MANAGEMENT ---
@Composable
fun HomeScreenImpl(tracks: List<Track>, playlists: List<Playlist>, session: SessionManager, currentVolume: Float, currentSortMode: String, onVolumeChange: (Float) -> Unit, onSortChange: (String) -> Unit, onPlay: (Track, List<Track>) -> Unit, onListUpdated: (List<Track>) -> Unit, onRefresh: () -> Unit, onLogout: () -> Unit) {
    var search by remember { mutableStateOf("") }
    var hiddenGenres by remember { mutableStateOf(session.getHiddenGenres()) }
    var showSettings by remember { mutableStateOf(false) }

    val baseList = tracks.filter { track -> !hiddenGenres.contains(track.genre ?: "Autre") }.let { list ->
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

    val displayList = baseList.filter { track -> track.title.contains(search, true) || track.artist.contains(search, true) }
    LaunchedEffect(baseList) { onListUpdated(baseList) }

    var editTrack by remember { mutableStateOf<Track?>(null) }
    var addToPlaylistTrack by remember { mutableStateOf<Track?>(null) }

    if (showSettings) {
        SettingsDialog(hiddenGenres, currentVolume, currentSortMode,
            onSave = { newHidden, newSort -> hiddenGenres = newHidden; session.saveHiddenGenres(newHidden); onSortChange(newSort) },
            onVolumeChange = onVolumeChange,
            onDismiss = { showSettings = false }
        )
    }
    if (editTrack != null) EditTrackDialog(editTrack!!, { editTrack = null }, { editTrack = null; onRefresh() })
    if (addToPlaylistTrack != null) {
        val myPlaylists = playlists.filter { it.creator_id == session.getUserId() }
        AddToPlaylistDialog(addToPlaylistTrack!!, myPlaylists, { addToPlaylistTrack = null })
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(16.dp))
        Box(Modifier.fillMaxWidth()) {
            IconButton(onClick = { showSettings = true }, modifier = Modifier.align(Alignment.CenterStart)) { Icon(Icons.Rounded.Settings, "Paramètres", tint = TextGray) }
            Text("PURPLE", color = AccentPurple, fontSize = 20.sp, fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.Center))
            IconButton(onClick = onLogout, modifier = Modifier.align(Alignment.CenterEnd)) { Icon(Icons.Rounded.ExitToApp, "Logout", tint = TextGray) }
        }
        Spacer(Modifier.height(20.dp))
        Text("| Toutes les pistes", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(15.dp))
        TextField(value = search, onValueChange = { search = it }, placeholder = { Text("Rechercher...", color = TextGray) }, colors = TextFieldDefaults.colors(focusedContainerColor = Color(0xFF241B36), unfocusedContainerColor = Color(0xFF241B36), focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent), shape = RoundedCornerShape(50), modifier = Modifier.fillMaxWidth().height(55.dp), trailingIcon = { Icon(Icons.Rounded.FilterList, null, tint = AccentPurple) })
        Spacer(Modifier.height(20.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(displayList) { track ->
                TrackRowWithMenu(track, session.isAdmin() || track.uploader_id == session.getUserId(), { onPlay(track, baseList) }, { editTrack = track }, { addToPlaylistTrack = track })
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrackRowWithMenu(track: Track, canEdit: Boolean, onClick: () -> Unit, onEdit: () -> Unit, onAddToPlaylist: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onClick() }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(
            model = coil.request.ImageRequest.Builder(LocalContext.current).data(track.cover_url).error(R.drawable.default_cover).fallback(R.drawable.default_cover).placeholder(R.drawable.default_cover).crossfade(true).build(),
            contentDescription = null, modifier = Modifier.size(50.dp).clip(RoundedCornerShape(10.dp)).background(Color.DarkGray), contentScale = ContentScale.Crop
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(track.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, modifier = Modifier.basicMarquee())
            Text("${track.artist} • ${track.genre ?: "Autre"} • ▶ ${track.play_count ?: 0}", color = TextGray, fontSize = 13.sp, maxLines = 1, modifier = Modifier.basicMarquee())
        }
        Box {
            IconButton(onClick = { showMenu = true }) { Icon(Icons.Rounded.MoreVert, null, tint = TextGray) }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }, modifier = Modifier.background(BgPanel)) {
                DropdownMenuItem(text = { Text("Ajouter à une playlist", color = Color.White) }, onClick = { showMenu = false; onAddToPlaylist() })
                if (canEdit) DropdownMenuItem(text = { Text("Modifier / Supprimer", color = Color.White) }, onClick = { showMenu = false; onEdit() })
            }
        }
    }
    HorizontalDivider(color = Color.White.copy(alpha = 0.05f), thickness = 1.dp)
}

// --- DIALOGUES ---
@Composable
fun EditTrackDialog(track: Track, onDismiss: () -> Unit, onSuccess: () -> Unit) {
    var title by remember { mutableStateOf(track.title) }
    var artist by remember { mutableStateOf(track.artist) }
    var genre by remember { mutableStateOf(track.genre ?: "Autre") }
    var newCoverUri by remember { mutableStateOf<Uri?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { newCoverUri = it }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val session = SessionManager(context)

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
                    if (newCoverUri != null) { val f = uriToFile(newCoverUri!!, context); cp = MultipartBody.Part.createFormData("new_cover", f.name, f.asRequestBody("image/*".toMediaTypeOrNull())) }
                    try { ApiClient.service.editTrack(tid, u, p, t, a, g, cp); onSuccess() } catch (e: Exception) {}
                }
            }) { Text("Sauver", color = AccentPurple) }
        },
        dismissButton = {
            TextButton(onClick = {
                scope.launch {
                    try { ApiClient.service.deleteTrack(track.id, session.getUsername(), session.getPassword()); onSuccess() } catch (e: Exception) {}
                }
            }) { Text("Supprimer", color = Color.Red) }
        }
    )
}

@Composable
fun AddToPlaylistDialog(track: Track, playlists: List<Playlist>, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val session = SessionManager(context)

    AlertDialog(
        onDismissRequest = onDismiss, containerColor = BgPanel,
        title = { Text("Ajouter à...", color = Color.White) },
        text = {
            if (playlists.isEmpty()) Text("Aucune playlist créée.", color = TextGray)
            else LazyColumn {
                items(playlists) { p ->
                    Row(Modifier.fillMaxWidth().clickable {
                        scope.launch {
                            try { ApiClient.service.modPlaylist(p.id, session.getUsername(), session.getPassword(), "add", track.id, null); Toast.makeText(context, "Ajouté !", Toast.LENGTH_SHORT).show(); onDismiss() } catch (e: Exception) {}
                        }
                    }.padding(10.dp)) { Text(p.name, color = Color.White, fontSize = 16.sp) }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}

// --- PLAYLISTS ---
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlaylistsScreenImpl(playlists: List<Playlist>, allTracks: List<Track>, session: SessionManager, onRefresh: () -> Unit, onPlay: (String) -> Unit) {
    var managePlaylist by remember { mutableStateOf<Playlist?>(null) }
    if (managePlaylist != null) ManagePlaylistDialog(managePlaylist!!, allTracks, { managePlaylist = null }, { managePlaylist = null; onRefresh() })

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Spacer(Modifier.height(16.dp))
        Text("| Tes Mixs", color = AccentPurple, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 20.dp))
        LazyVerticalGrid(columns = GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(playlists) { p ->
                val canManage = session.isAdmin() || p.creator_id == session.getUserId()
                Card(colors = CardDefaults.cardColors(containerColor = BgPanel), modifier = Modifier.height(140.dp).clickable { onPlay(p.song_ids) }) {
                    Box(Modifier.fillMaxSize()) {
                        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Rounded.Album, null, tint = AccentPurple, modifier = Modifier.size(40.dp))
                            Spacer(Modifier.height(10.dp))
                            Text(p.name, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.basicMarquee())
                            Text("by ${p.creator}", color = TextGray, fontSize = 10.sp, maxLines = 1, modifier = Modifier.basicMarquee())
                        }
                        if (canManage) { IconButton(onClick = { managePlaylist = p }, modifier = Modifier.align(Alignment.TopEnd)) { Icon(Icons.Rounded.MoreVert, null, tint = TextGray) } }
                    }
                }
            }
        }
    }
}

@Composable
fun ManagePlaylistDialog(playlist: Playlist, allTracks: List<Track>, onDismiss: () -> Unit, onSuccess: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val session = SessionManager(context)
    var name by remember { mutableStateOf(playlist.name) }
    var showAddTracks by remember { mutableStateOf(false) }
    val ids = playlist.song_ids.split(",").mapNotNull { it.trim().toIntOrNull() }
    val pTracks = allTracks.filter { it.id in ids }
    val availableTracks = allTracks.filter { it.id !in ids }

    AlertDialog(
        onDismissRequest = onDismiss, containerColor = BgPanel,
        title = { Text(if (showAddTracks) "Ajouter titres" else "Gérer Playlist", color = Color.White) },
        text = {
            Column {
                if (!showAddTracks) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nom") }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                    Spacer(Modifier.height(10.dp))
                    Text("Contenu:", color = TextGray, fontSize = 12.sp)
                    LazyColumn(Modifier.height(150.dp)) {
                        items(pTracks) { t ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(t.title, color = Color.White, maxLines = 1, modifier = Modifier.weight(1f))
                                IconButton(onClick = { scope.launch { ApiClient.service.modPlaylist(playlist.id, session.getUsername(), session.getPassword(), "remove", t.id, null); onSuccess() } }) { Icon(Icons.Rounded.Close, null, tint = Color.Red, modifier = Modifier.size(16.dp)) }
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = { showAddTracks = true }, colors = ButtonDefaults.buttonColors(containerColor = BgDark), modifier = Modifier.fillMaxWidth()) { Icon(Icons.Rounded.Add, null, tint = AccentPurple); Spacer(Modifier.width(5.dp)); Text("Ajouter des titres", color = AccentPurple) }
                } else {
                    LazyColumn(Modifier.height(250.dp)) {
                        items(availableTracks) { t ->
                            Row(Modifier.fillMaxWidth().clickable { scope.launch { ApiClient.service.modPlaylist(playlist.id, session.getUsername(), session.getPassword(), "add", t.id, null); onSuccess() } }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.AddCircleOutline, null, tint = Color.Green); Spacer(Modifier.width(10.dp)); Text(t.title, color = Color.White, maxLines = 1)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!showAddTracks) Row {
                TextButton(onClick = { scope.launch { ApiClient.service.modPlaylist(playlist.id, session.getUsername(), session.getPassword(), "delete", null, null); onSuccess() } }) { Text("Supprimer", color = Color.Red) }
                TextButton(onClick = { scope.launch { ApiClient.service.modPlaylist(playlist.id, session.getUsername(), session.getPassword(), "rename", null, name); onSuccess() } }) { Text("Sauver", color = AccentPurple) }
            } else TextButton(onClick = { showAddTracks = false }) { Text("Retour") }
        }
    )
}

// --- PLAYER ---
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FullPlayerScreen(t: String, a: String, c: String, playing: Boolean, pos: Long, dur: Long, shuff: Boolean, rep: Int, back: () -> Unit, pp: () -> Unit, nxt: () -> Unit, prv: () -> Unit, seek: (Long) -> Unit, shuffT: () -> Unit, repT: () -> Unit, q: () -> Unit) {
    val sliderValue = if (dur > 0) (pos.toFloat() / dur.toFloat()).coerceIn(0f, 1f) else 0f
    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF2E2445), BgDark))).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = back) { Icon(Icons.Rounded.KeyboardArrowDown, null, tint = Color.White, modifier = Modifier.size(32.dp)) }
            Text("LECTURE EN COURS", color = TextGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = q) { Icon(Icons.Rounded.QueueMusic, null, tint = Color.White) }
        }
        Spacer(Modifier.weight(1f))
        Card(shape = RoundedCornerShape(24.dp), elevation = CardDefaults.cardElevation(20.dp), modifier = Modifier.aspectRatio(1f)) {
            AsyncImage(model = coil.request.ImageRequest.Builder(LocalContext.current).data(c.ifEmpty { R.drawable.default_cover }).error(R.drawable.default_cover).fallback(R.drawable.default_cover).build(), contentDescription = null, modifier = Modifier.fillMaxSize().background(Color.DarkGray), contentScale = ContentScale.Crop)
        }
        Spacer(Modifier.height(40.dp))
        Column(Modifier.fillMaxWidth()) {
            Text(t, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.basicMarquee())
            Text(a, color = AccentPurple, fontSize = 18.sp, fontWeight = FontWeight.Medium, maxLines = 1, modifier = Modifier.basicMarquee())
        }
        Spacer(Modifier.height(20.dp))
        Slider(value = sliderValue, onValueChange = { percent -> seek((percent * dur).toLong()) }, colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = AccentPurple, inactiveTrackColor = Color.White.copy(0.2f)))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(formatTime(pos), color = TextGray, fontSize = 12.sp); Text(formatTime(dur), color = TextGray, fontSize = 12.sp) }
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = shuffT) { Icon(Icons.Rounded.Shuffle, null, tint = if (shuff) AccentPurple else TextGray) }
            IconButton(onClick = prv, Modifier.size(48.dp)) { Icon(Icons.Rounded.SkipPrevious, null, tint = Color.White, modifier = Modifier.fillMaxSize()) }
            Box(Modifier.size(70.dp).background(Color.White, CircleShape).clickable { pp() }, contentAlignment = Alignment.Center) { Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = BgDark, modifier = Modifier.size(40.dp)) }
            IconButton(onClick = nxt, Modifier.size(48.dp)) { Icon(Icons.Rounded.SkipNext, null, tint = Color.White, modifier = Modifier.fillMaxSize()) }
            IconButton(onClick = repT) {
                val (ic, co) = when (rep) { Player.REPEAT_MODE_ONE -> Icons.Rounded.RepeatOne to AccentPurple; Player.REPEAT_MODE_ALL -> Icons.Rounded.Repeat to AccentPurple; else -> Icons.Rounded.Repeat to TextGray }
                Icon(ic, null, tint = co)
            }
        }
        Spacer(Modifier.weight(1f))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MiniPlayer(t: String, a: String, c: String, p: Boolean, prog: Float, pp: () -> Unit, clk: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = BgPanel), modifier = Modifier.fillMaxWidth().height(85.dp).clickable { clk() }, shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)) {
        Column {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(model = coil.request.ImageRequest.Builder(LocalContext.current).data(c.ifEmpty { R.drawable.default_cover }).error(R.drawable.default_cover).fallback(R.drawable.default_cover).build(), contentDescription = null, modifier = Modifier.size(50.dp).clip(RoundedCornerShape(10.dp)).background(Color.DarkGray), contentScale = ContentScale.Crop)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(t, color = Color.White, maxLines = 1, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.basicMarquee())
                    Text(a, color = AccentPurple, maxLines = 1, fontSize = 12.sp, modifier = Modifier.basicMarquee())
                }
                IconButton(onClick = pp, Modifier.size(45.dp).background(Color.White, CircleShape)) { Icon(if (p) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = BgDark, modifier = Modifier.size(28.dp)) }
            }
            LinearProgressIndicator(progress = { prog }, modifier = Modifier.fillMaxWidth().height(2.dp), color = AccentPurple, trackColor = Color.White.copy(0.1f))
        }
    }
}

@Composable
fun QueueScreen(controller: MediaController?, currentIndex: Int, queueVersion: Int, onPlay: (Int) -> Unit, onClose: () -> Unit) {
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
            itemsIndexed(queue) { idx, item ->
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { onPlay(idx) }, verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(model = item.mediaMetadata.artworkUri, null, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
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
            Triple("create", "Créer", Icons.Rounded.AddBox),
            Triple("queue", "File", Icons.Rounded.QueueMusic),
            Triple("upload", "Upload", Icons.Rounded.CloudUpload)
        ).forEach { (r, l, i) ->
            NavigationBarItem(
                icon = { Icon(i, null, Modifier.size(26.dp)) },
                label = { Text(l, fontSize = 10.sp) },
                selected = curr == r,
                onClick = { nav(r) },
                colors = NavigationBarItemDefaults.colors(selectedIconColor = AccentPurple, selectedTextColor = AccentPurple, indicatorColor = Color.Transparent, unselectedIconColor = TextGray, unselectedTextColor = TextGray)
            )
        }
    }
}

// --- UPLOAD ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadScreenImpl(onUploadSuccess: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val session = SessionManager(context)
    var title by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf("") }
    var genre by remember { mutableStateOf("Autre") }
    var genreExpanded by remember { mutableStateOf(false) }
    val predefinedGenres = listOf("Phonk/Funk", "Rap", "Pop", "Rock", "Electro", "Hyperpop", "Nightcore", "Qualité inférieure", "Autre")
    var audioUri by remember { mutableStateOf<Uri?>(null) }
    var coverUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val audioLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { audioUri = it }
    val coverLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { coverUri = it }

    Column(Modifier.fillMaxSize().padding(24.dp).background(BgDark)) {
        Spacer(Modifier.height(20.dp))
        Text("Upload", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(30.dp))
        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Titre") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(value = artist, onValueChange = { artist = it }, label = { Text("Artiste") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
        Spacer(Modifier.height(10.dp))
        ExposedDropdownMenuBox(expanded = genreExpanded, onExpandedChange = { genreExpanded = it }) {
            OutlinedTextField(value = genre, onValueChange = { genre = it }, label = { Text("Genre") }, modifier = Modifier.fillMaxWidth().menuAnchor(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White), trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genreExpanded) })
            ExposedDropdownMenu(expanded = genreExpanded, onDismissRequest = { genreExpanded = false }, modifier = Modifier.background(BgPanel)) {
                predefinedGenres.forEach { s -> DropdownMenuItem(text = { Text(s, color = Color.White) }, onClick = { genre = s; genreExpanded = false }) }
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
                    val u = session.getUsername().toRequestBody("text/plain".toMediaTypeOrNull())
                    val p = session.getPassword().toRequestBody("text/plain".toMediaTypeOrNull())
                    try {
                        ApiClient.service.uploadTrack(title.toRequestBody("text/plain".toMediaTypeOrNull()), artist.toRequestBody("text/plain".toMediaTypeOrNull()), u, p, genre.toRequestBody("text/plain".toMediaTypeOrNull()), audioPart, coverPart)
                        Toast.makeText(context, "Succès", Toast.LENGTH_SHORT).show()
                        title = ""; artist = ""; genre = "Autre"; audioUri = null; coverUri = null
                        onUploadSuccess()
                    } catch (e: Exception) { Toast.makeText(context, "Erreur réseau", Toast.LENGTH_SHORT).show() }
                    isLoading = false
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
            enabled = !isLoading
        ) { if (isLoading) CircularProgressIndicator(color = Color.White) else Text("PUBLIER", fontWeight = FontWeight.Bold) }
    }
}

// --- CREATE PLAYLIST ---
@Composable
fun CreatePlaylistScreenImpl(onSuccess: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val session = SessionManager(context)
    var name by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("Créer un Mix", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(30.dp))
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nom du Mix") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
        Spacer(Modifier.height(30.dp))
        Button(
            onClick = { scope.launch { ApiClient.service.createPlaylist(name, session.getUsername(), session.getPassword()); onSuccess() } },
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) { Text("CRÉER") }
    }
}

fun formatTime(ms: Long) = "%d:%02d".format(ms / 1000 / 60, ms / 1000 % 60)