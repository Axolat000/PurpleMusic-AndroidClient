package com.randomfilm.purplemusic20.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.randomfilm.purplemusic20.R
import com.randomfilm.purplemusic20.data.*
import com.randomfilm.purplemusic20.ui.theme.*
import com.randomfilm.purplemusic20.util.buildImageRequest
import kotlinx.coroutines.launch

// ─── Playlists / Mixs Tab ─────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlaylistsScreenImpl(playlists: List<Playlist>, allTracks: List<Track>, session: SessionManager, onRefresh: () -> Unit, onOpenPlaylist: (Playlist) -> Unit, onNavigateCreate: () -> Unit) {
    var managePlaylist by remember { mutableStateOf<Playlist?>(null) }
    if (managePlaylist != null) ManagePlaylistDialog(managePlaylist!!, allTracks, session, { managePlaylist = null }, { managePlaylist = null; onRefresh() })

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.playlists_screen_title), color = LocalAppColors.current.accent, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = onNavigateCreate, modifier = Modifier.size(40.dp).background(LocalAppColors.current.primary, CircleShape)) { Icon(Icons.Rounded.Add, stringResource(R.string.playlists_create_content_description), tint = Color.White) }
        }
        Spacer(Modifier.height(20.dp))
        LazyVerticalGrid(columns = GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(playlists, key = { it.id }) { p ->
                val canManage = session.isAdmin() || p.creator_id == session.getUserId()
                Card(colors = CardDefaults.cardColors(containerColor = LocalAppColors.current.panel), modifier = Modifier.height(140.dp).clickable { onOpenPlaylist(p) }) {
                    Box(Modifier.fillMaxSize()) {
                        // Image réelle (uploadée depuis l'app ou le site web) en fond de carte avec un
                        // léger voile pour garder le titre lisible, sinon repli sur l'icône générique
                        // centrée comme avant -- même convention que la carte d'accueil (voir HomeScreen.kt).
                        if (!p.cover.isNullOrBlank()) {
                            AsyncImage(
                                model = buildImageRequest(LocalContext.current, session.getServerUrl() + "covers/" + p.cover, session.isCoverCacheEnabled()),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f)))))
                            Text(p.name, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.align(Alignment.BottomStart).padding(10.dp))
                        } else {
                            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Rounded.Album, null, tint = LocalAppColors.current.accent, modifier = Modifier.size(40.dp))
                                Spacer(Modifier.height(10.dp))
                                Text(p.name, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                        }
                        if (canManage) IconButton(onClick = { managePlaylist = p }, modifier = Modifier.align(Alignment.TopEnd)) { Icon(Icons.Rounded.MoreVert, null, tint = LocalAppColors.current.textSecondary) }
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
        onDismissRequest = onDismiss, containerColor = LocalAppColors.current.panel,
        title = { Text(stringResource(R.string.manage_playlist_title), color = Color.White) },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.playlist_name_label)) }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White))
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
                TextButton(onClick = { scope.launch { ApiClient.service.modPlaylist(playlist.id, session.getUsername(), session.getPassword(), "delete", null, null); onSuccess() } }) { Text(stringResource(R.string.action_delete), color = Color.Red) }
                TextButton(onClick = { scope.launch { ApiClient.service.modPlaylist(playlist.id, session.getUsername(), session.getPassword(), "rename", null, name); onSuccess() } }) { Text(stringResource(R.string.action_save), color = LocalAppColors.current.accent) }
            }
        }
    )
}
