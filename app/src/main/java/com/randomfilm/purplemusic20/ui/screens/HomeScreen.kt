package com.randomfilm.purplemusic20.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.randomfilm.purplemusic20.MusicService
import com.randomfilm.purplemusic20.R
import com.randomfilm.purplemusic20.data.*
import com.randomfilm.purplemusic20.ui.components.EditTrackDialog
import com.randomfilm.purplemusic20.ui.components.TrackRowWithMenu
import com.randomfilm.purplemusic20.ui.theme.*
import com.randomfilm.purplemusic20.util.buildImageRequest

// ─── Home Screen ──────────────────────────────────────────────────────────────
@Composable
fun HomeScreenImpl(tracks: List<Track>, playlists: List<Playlist>, session: SessionManager, currentVolume: Float, currentSortMode: String, currentThemePreset: String, currentMaterialYouEnabled: Boolean, onVolumeChange: (Float) -> Unit, onSortChange: (String) -> Unit, onThemeChange: (String) -> Unit, onMaterialYouChange: (Boolean) -> Unit, onPlay: (Track, List<Track>) -> Unit, onListUpdated: (List<Track>) -> Unit, onRefresh: () -> Unit, onLogout: () -> Unit, onAddToPlaylist: (Track) -> Unit, onOpenPlaylist: (Playlist) -> Unit, onRedoTutorial: () -> Unit) {
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

    val recentTracks = remember(tracks) { tracks.sortedByDescending { it.id }.take(10) }
    val popularTracks = remember(tracks) { tracks.filter { (it.play_count ?: 0) > 0 }.sortedByDescending { it.play_count ?: 0 }.take(10) }

    var editTrack by remember { mutableStateOf<Track?>(null) }

    if (showSettings) {
        SettingsDialog(session, hiddenGenres, currentVolume, currentSortMode, currentThemePreset, currentMaterialYouEnabled,
            onSave = { newHidden, newSort -> hiddenGenres = newHidden; session.saveHiddenGenres(newHidden); onSortChange(newSort) },
            onVolumeChange = onVolumeChange,
            onThemeChange = onThemeChange,
            onMaterialYouChange = onMaterialYouChange,
            onSetSleepTimer = { mins ->
                val intent = Intent(context, MusicService::class.java).apply {
                    action = "SLEEP_TIMER"
                    putExtra("minutes", mins)
                }
                context.startService(intent)
                Toast.makeText(context, if (mins > 0) context.getString(R.string.settings_sleep_timer_set, mins) else context.getString(R.string.settings_sleep_timer_off), Toast.LENGTH_SHORT).show()
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
            IconButton(onClick = { showSettings = true }, modifier = Modifier.align(Alignment.CenterStart)) { Icon(Icons.Rounded.Settings, stringResource(R.string.home_settings_content_description), tint = LocalAppColors.current.textSecondary) }
            Text(stringResource(R.string.app_brand_name), color = LocalAppColors.current.accent, fontSize = 20.sp, fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.Center))
            IconButton(onClick = onLogout, modifier = Modifier.align(Alignment.CenterEnd)) { Icon(Icons.Rounded.ExitToApp, stringResource(R.string.home_logout_content_description), tint = LocalAppColors.current.textSecondary) }
        }
        Spacer(Modifier.height(20.dp))
        TextField(value = search, onValueChange = { search = it }, placeholder = { Text(stringResource(R.string.search_placeholder), color = LocalAppColors.current.textSecondary) }, colors = TextFieldDefaults.colors(focusedContainerColor = Color(0xFF241B36), unfocusedContainerColor = Color(0xFF241B36), focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent), shape = RoundedCornerShape(50), modifier = Modifier.fillMaxWidth().height(55.dp), trailingIcon = { Icon(Icons.Rounded.FilterList, null, tint = LocalAppColors.current.accent) })
        Spacer(Modifier.height(20.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (search.isBlank()) {
                if (recentTracks.isNotEmpty()) {
                    item {
                        HomeSectionTitle(stringResource(R.string.home_section_recent))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(recentTracks, key = { "recent_${it.id}" }) { track ->
                                TrackCoverCard(track, session) { onPlay(track, recentTracks) }
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                    }
                }
                if (popularTracks.isNotEmpty()) {
                    item {
                        HomeSectionTitle(stringResource(R.string.sort_popular))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(popularTracks, key = { "popular_${it.id}" }) { track ->
                                TrackCoverCard(track, session) { onPlay(track, popularTracks) }
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                    }
                }
                if (playlists.isNotEmpty()) {
                    item {
                        HomeSectionTitle(stringResource(R.string.home_section_playlists))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(playlists, key = { "mix_${it.id}" }) { playlist ->
                                PlaylistCoverCard(playlist) { onOpenPlaylist(playlist) }
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                    }
                }
                item { HomeSectionTitle(stringResource(R.string.home_section_library)) }
            }
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
private fun HomeSectionTitle(text: String) {
    Text(text, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 10.dp))
}

@Composable
private fun TrackCoverCard(track: Track, session: SessionManager, onClick: () -> Unit) {
    Column(Modifier.width(110.dp).clickable { onClick() }) {
        AsyncImage(
            model = buildImageRequest(LocalContext.current, track.cover_url, session.isCoverCacheEnabled()),
            contentDescription = null,
            modifier = Modifier.size(110.dp).clip(RoundedCornerShape(12.dp)).background(Color.DarkGray),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.height(6.dp))
        Text(track.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        Text(track.artist, color = LocalAppColors.current.textSecondary, fontSize = 12.sp, maxLines = 1)
    }
}

@Composable
private fun PlaylistCoverCard(playlist: Playlist, onClick: () -> Unit) {
    Column(Modifier.width(110.dp).clickable { onClick() }) {
        Box(
            Modifier.size(110.dp).clip(RoundedCornerShape(12.dp)).background(LocalAppColors.current.panel),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Album, null, tint = LocalAppColors.current.accent, modifier = Modifier.size(36.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(playlist.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}
