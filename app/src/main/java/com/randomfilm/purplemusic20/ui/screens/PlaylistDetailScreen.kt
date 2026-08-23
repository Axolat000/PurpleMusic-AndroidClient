package com.randomfilm.purplemusic20.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.randomfilm.purplemusic20.ui.components.EditTrackDialog
import com.randomfilm.purplemusic20.ui.components.TrackRowWithMenu
import com.randomfilm.purplemusic20.ui.theme.LocalAppColors
import com.randomfilm.purplemusic20.util.buildImageRequest
import com.randomfilm.purplemusic20.util.playlistCoverUrl

// ─── Détail d'une Playlist : liste des morceaux, aucune lecture auto à l'ouverture ─
@Composable
fun PlaylistDetailScreen(
    playlist: Playlist,
    allTracks: List<Track>,
    session: SessionManager,
    likedTrackIds: Set<Int>,
    onBack: () -> Unit,
    onPlayFrom: (List<Track>, Track) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    onToggleLike: (Track) -> Unit,
    onRefresh: () -> Unit,
    onOpenArtist: (String) -> Unit = {}
) {
    var showManage by remember { mutableStateOf(false) }
    var editTrack by remember { mutableStateOf<Track?>(null) }
    val ids = remember(playlist.song_ids) { playlist.song_ids.split(",").mapNotNull { it.trim().toIntOrNull() } }
    val hidden = session.getHiddenGenres()
    val pTracks = remember(ids, allTracks, hidden) {
        ids.mapNotNull { id -> allTracks.find { it.id == id } }.filter { !hidden.contains(it.genre ?: "Autre") }
    }
    val canManage = session.isAdmin() || playlist.creator_id == session.getUserId()

    if (showManage) {
        ManagePlaylistDialog(
            playlist = playlist,
            allTracks = allTracks,
            session = session,
            onDismiss = { showManage = false },
            onSuccess = { showManage = false; onRefresh() }
        )
    }
    if (editTrack != null) EditTrackDialog(editTrack!!, session, { editTrack = null }, { editTrack = null; onRefresh() })

    Column(Modifier.fillMaxSize().background(LocalAppColors.current.background).padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = Color.White) }
            if (!playlist.cover.isNullOrBlank()) {
                AsyncImage(
                    model = buildImageRequest(LocalContext.current, playlistCoverUrl(session, playlist.cover), session.isCoverCacheEnabled()),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp))
                )
                Spacer(Modifier.width(10.dp))
            }
            Text(playlist.name, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.padding(start = 8.dp).weight(1f))
            if (canManage) IconButton(onClick = { showManage = true }) { Icon(Icons.Rounded.MoreVert, null, tint = LocalAppColors.current.textSecondary) }
        }
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.track_count, pTracks.size), color = LocalAppColors.current.textSecondary, fontSize = 13.sp)
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { if (pTracks.isNotEmpty()) onPlayFrom(pTracks, pTracks.first()) },
            enabled = pTracks.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = LocalAppColors.current.primary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Rounded.PlayArrow, null, tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.playlist_play_all), fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(16.dp))

        if (pTracks.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.Album, null, tint = LocalAppColors.current.textSecondary, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.playlist_empty), color = LocalAppColors.current.textSecondary)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(pTracks, key = { it.id }) { track ->
                    TrackRowWithMenu(
                        track = track,
                        session = session,
                        canEdit = session.isAdmin() || track.uploader_id == session.getUserId(),
                        isLiked = likedTrackIds.contains(track.id),
                        onClick = { onPlayFrom(pTracks, track) },
                        onEdit = { editTrack = track },
                        onAddToPlaylist = { onAddToPlaylist(track) },
                        onToggleLike = { onToggleLike(track) },
                        onArtistClick = onOpenArtist
                    )
                }
            }
        }
    }
}
