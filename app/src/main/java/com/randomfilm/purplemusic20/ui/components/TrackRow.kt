package com.randomfilm.purplemusic20.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.randomfilm.purplemusic20.R
import com.randomfilm.purplemusic20.data.*
import com.randomfilm.purplemusic20.ui.theme.*
import com.randomfilm.purplemusic20.util.*
import kotlinx.coroutines.launch

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
                    color = LocalAppColors.current.textSecondary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    // fill=false permet de coller au genre tant que c'est court
                    // weight(1f) permet au marquee de s'activer si le texte dépasse
                    modifier = Modifier.weight(1f, fill = false).basicMarquee()
                )

                Text(
                    text = " • ${track.genre ?: "Autre"} • ▶ ${track.play_count ?: 0}",
                    color = LocalAppColors.current.textSecondary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            if (downloadProgress in 0..99) {
                LinearProgressIndicator(
                    progress = { downloadProgress / 100f },
                    modifier = Modifier.fillMaxWidth().height(2.dp).padding(top = 2.dp),
                    color = LocalAppColors.current.accent
                )
            }
        }

        Box {
            IconButton(onClick = { showMenu = true }) { Icon(Icons.Rounded.MoreVert, null, tint = LocalAppColors.current.textSecondary) }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }, modifier = Modifier.background(LocalAppColors.current.panel)) {
                DropdownMenuItem(text = { Text(stringResource(R.string.action_add_to_playlist), color = Color.White) }, leadingIcon = { Icon(Icons.Rounded.PlaylistAdd, null, tint = Color.White) }, onClick = { showMenu = false; onAddToPlaylist() })
                if (!isDownloaded) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.action_download), color = Color.White) }, leadingIcon = { Icon(Icons.Rounded.Download, null, tint = LocalAppColors.current.accent) }, onClick = {
                        showMenu = false
                        scope.launch { downloadProgress = 0; if (DownloadManager.downloadTrack(context, track) { downloadProgress = it }) isDownloaded = true; downloadProgress = -1 }
                    })
                } else {
                    DropdownMenuItem(text = { Text(stringResource(R.string.action_delete_offline), color = Color.Red) }, onClick = { showMenu = false; DownloadManager.deleteDownloadedTrack(context, track.id.toString()); isDownloaded = false })
                }
                if (canEdit) DropdownMenuItem(text = { Text(stringResource(R.string.action_edit), color = Color.White) }, onClick = { showMenu = false; onEdit() })
            }
        }
    }
    HorizontalDivider(color = Color.White.copy(alpha = 0.05f), thickness = 1.dp)
}
