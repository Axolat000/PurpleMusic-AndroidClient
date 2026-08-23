package com.randomfilm.purplemusic20.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.randomfilm.purplemusic20.data.ArtistBio
import com.randomfilm.purplemusic20.data.SessionManager
import com.randomfilm.purplemusic20.data.Track
import com.randomfilm.purplemusic20.data.WikipediaClient
import com.randomfilm.purplemusic20.ui.components.EditTrackDialog
import com.randomfilm.purplemusic20.ui.components.TrackRowWithMenu
import com.randomfilm.purplemusic20.ui.theme.LocalAppColors
import com.randomfilm.purplemusic20.util.ArtistUtils
import com.randomfilm.purplemusic20.util.buildImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Groups every track whose artist field contains [artistName] (see ArtistUtils.splitArtistNames,
 * matches the web app's grouping rule) and shows a bio fetched live from Wikipedia. No dedicated
 * artist entity server-side -- everything here is derived client-side from [allTracks].
 */
@Composable
fun ArtistScreen(
    artistName: String,
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
    val aTracks = remember(artistName, allTracks) {
        allTracks.filter { ArtistUtils.trackBelongsToArtist(it.artist, artistName) }.sortedByDescending { it.id }
    }
    val heroCover = aTracks.firstOrNull()?.cover_url

    var bio by remember(artistName) { mutableStateOf<ArtistBio?>(null) }
    var bioLoading by remember(artistName) { mutableStateOf(true) }
    LaunchedEffect(artistName) {
        bioLoading = true
        bio = withContext(Dispatchers.IO) {
            WikipediaClient.fetchBio(artistName, Locale.getDefault().language)
        }
        bioLoading = false
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
                    modifier = Modifier.size(56.dp).clip(CircleShape)
                )
                Spacer(Modifier.width(10.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(artistName, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(stringResource(R.string.track_count, aTracks.size), color = LocalAppColors.current.textSecondary, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(12.dp))

        when {
            bioLoading -> Text(stringResource(R.string.artist_bio_loading), color = LocalAppColors.current.textSecondary, fontSize = 13.sp)
            bio != null -> Text(bio!!.extract, color = LocalAppColors.current.textSecondary, fontSize = 13.sp, lineHeight = 19.sp)
            else -> Text(stringResource(R.string.artist_bio_unavailable), color = LocalAppColors.current.textSecondary, fontSize = 13.sp)
        }
        Spacer(Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(aTracks, key = { it.id }) { track ->
                TrackRowWithMenu(
                    track = track,
                    session = session,
                    canEdit = session.isAdmin() || track.uploader_id == session.getUserId(),
                    isLiked = likedTrackIds.contains(track.id),
                    onClick = { onPlayFrom(aTracks, track) },
                    onEdit = { editTrack = track },
                    onAddToPlaylist = { onAddToPlaylist(track) },
                    onToggleLike = { onToggleLike(track) },
                    onArtistClick = onOpenArtist
                )
            }
        }
    }
}
