package com.randomfilm.purplemusic20.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExitToApp
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.randomfilm.purplemusic20.Playlist
import com.randomfilm.purplemusic20.SessionManager
import com.randomfilm.purplemusic20.Track
import com.randomfilm.purplemusic20.ui.components.EditTrackDialog
import com.randomfilm.purplemusic20.ui.components.SettingsDialog
import com.randomfilm.purplemusic20.ui.components.TrackRowWithMenu
import com.randomfilm.purplemusic20.ui.theme.AccentPurple
import com.randomfilm.purplemusic20.ui.theme.TextGray
import com.randomfilm.purplemusic20.MusicService

@Composable
fun HomeScreen(tracks: List<Track>, playlists: List<Playlist>, session: SessionManager, currentVolume: Float, currentSortMode: String, onVolumeChange: (Float) -> Unit, onSortChange: (String) -> Unit, onPlay: (Track, List<Track>) -> Unit, onListUpdated: (List<Track>) -> Unit, onRefresh: () -> Unit, onLogout: () -> Unit, onAddToPlaylist: (Track) -> Unit, onRedoTutorial: () -> Unit) {
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
