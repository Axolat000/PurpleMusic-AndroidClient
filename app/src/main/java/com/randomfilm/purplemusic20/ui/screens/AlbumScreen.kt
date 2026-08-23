package com.randomfilm.purplemusic20.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
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
import com.randomfilm.purplemusic20.R
import com.randomfilm.purplemusic20.data.SessionManager
import com.randomfilm.purplemusic20.data.Track
import com.randomfilm.purplemusic20.ui.components.EditTrackDialog
import com.randomfilm.purplemusic20.ui.components.TrackRowWithMenu
import com.randomfilm.purplemusic20.ui.theme.LocalAppColors
import com.randomfilm.purplemusic20.util.ArtistUtils
import com.randomfilm.purplemusic20.util.buildImageRequest

/**
 * Groups every track whose album field matches [albumName] (case-insensitive) -- no dedicated
 * albums table server-side, the name itself is the grouping key (mirrors the web app's approach).
 */
@Composable
fun AlbumScreen(
    albumName: String,
    allTracks: List<Track>,
    session: SessionManager,
    likedTrackIds: Set<Int>,
    onBack: () -> Unit,
    onPlayFrom: (List<Track>, Track) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    onToggleLike: (Track) -> Unit,
    onOpenArtist: (String) -> Unit,
    onRefresh: () -> Unit
) {
    var editTrack by remember { mutableStateOf<Track?>(null) }
    val albTracks = remember(albumName, allTracks) {
        allTracks.filter { it.album?.trim()?.equals(albumName.trim(), ignoreCase = true) == true }.sortedByDescending { it.id }
    }
    val heroCover = albTracks.firstOrNull()?.cover_url
    val artistNames = remember(albTracks) {
        val seen = LinkedHashMap<String, String>()
        albTracks.forEach { t ->
            ArtistUtils.splitArtistNames(t.artist).forEach { n ->
                seen.putIfAbsent(n.lowercase(), n)
            }
        }
        seen.values.toList()
    }

    if (editTrack != null) EditTrackDialog(editTrack!!, session, { editTrack = null }, { editTrack = null; onRefresh() })

    Column(Modifier.fillMaxSize().background(LocalAppColors.current.background).padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = Color.White) }
            if (heroCover != null) {
                AsyncImage(
                    model = buildImageRequest(LocalContext.current, heroCover, session.isCoverCacheEnabled()),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(10.dp))
                )
                Spacer(Modifier.width(10.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(albumName, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(stringResource(R.string.track_count, albTracks.size), color = LocalAppColors.current.textSecondary, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(6.dp))

        Row(Modifier.fillMaxWidth()) {
            artistNames.forEachIndexed { i, name ->
                Text(
                    text = name + if (i < artistNames.lastIndex) ", " else "",
                    color = LocalAppColors.current.accent,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable { onOpenArtist(name) }
                )
            }
        }
        Spacer(Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(albTracks, key = { it.id }) { track ->
                TrackRowWithMenu(
                    track = track,
                    session = session,
                    canEdit = session.isAdmin() || track.uploader_id == session.getUserId(),
                    isLiked = likedTrackIds.contains(track.id),
                    onClick = { onPlayFrom(albTracks, track) },
                    onEdit = { editTrack = track },
                    onAddToPlaylist = { onAddToPlaylist(track) },
                    onToggleLike = { onToggleLike(track) },
                    onArtistClick = onOpenArtist
                )
            }
        }
    }
}
