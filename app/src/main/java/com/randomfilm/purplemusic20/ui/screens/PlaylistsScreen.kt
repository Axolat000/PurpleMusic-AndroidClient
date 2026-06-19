package com.randomfilm.purplemusic20.ui.screens

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.MoreVert
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
import com.randomfilm.purplemusic20.ui.components.ManagePlaylistDialog
import com.randomfilm.purplemusic20.ui.theme.AccentPurple
import com.randomfilm.purplemusic20.ui.theme.BgPanel
import com.randomfilm.purplemusic20.ui.theme.PrimaryPurple
import com.randomfilm.purplemusic20.ui.theme.TextGray

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlaylistsScreen(playlists: List<Playlist>, allTracks: List<Track>, session: SessionManager, onRefresh: () -> Unit, onPlay: (String) -> Unit, onNavigateCreate: () -> Unit) {
    var managePlaylist by remember { mutableStateOf<Playlist?>(null) }
    val context = LocalContext.current
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
