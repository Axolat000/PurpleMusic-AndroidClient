package com.randomfilm.purplemusic20.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.randomfilm.purplemusic20.SessionManager
import com.randomfilm.purplemusic20.data.DownloadManager
import com.randomfilm.purplemusic20.data.models.DownloadedTrack
import com.randomfilm.purplemusic20.ui.theme.AccentPurple
import com.randomfilm.purplemusic20.ui.theme.TextGray
import com.randomfilm.purplemusic20.util.buildImageRequest
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DownloadsScreen(session: SessionManager, onPlay: (DownloadedTrack, List<DownloadedTrack>) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    var downloadedTracks by remember { mutableStateOf(DownloadManager.getDownloadedTracks(context)) }
    var search by remember { mutableStateOf("") }
    val displayList = remember(downloadedTracks, search) { downloadedTracks.filter { it.title.contains(search, true) || it.artist.contains(search, true) } }
    val isOfflineMode = session.getUserId() == -1

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (isOfflineMode) IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, null, tint = Color.White) }
            Text("| Hors-ligne", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("${downloadedTracks.size} titre(s)", color = TextGray, fontSize = 13.sp)
        }
        Spacer(Modifier.height(16.dp))
        TextField(value = search, onValueChange = { search = it }, placeholder = { Text("Rechercher...", color = TextGray) }, colors = TextFieldDefaults.colors(focusedContainerColor = Color(0xFF241B36), unfocusedContainerColor = Color(0xFF241B36), focusedTextColor = Color.White, unfocusedIndicatorColor = Color.Transparent, focusedIndicatorColor = Color.Transparent), shape = RoundedCornerShape(50), modifier = Modifier.fillMaxWidth().height(55.dp))
        Spacer(Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(displayList, key = { it.id }) { track ->
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onPlay(track, displayList) }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (track.coverPath.isNotEmpty()) AsyncImage(model = buildImageRequest(context, File(track.coverPath), session.isCoverCacheEnabled()), contentDescription = null, modifier = Modifier.size(50.dp).clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop)
                    else Box(Modifier.size(50.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF2E2445)), contentAlignment = Alignment.Center) { Text("♫", color = AccentPurple, fontSize = 20.sp) }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(track.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, modifier = Modifier.basicMarquee())
                        Text(track.artist, color = TextGray, fontSize = 13.sp, maxLines = 1)
                    }
                    IconButton(onClick = { DownloadManager.deleteDownloadedTrack(context, track.id); downloadedTracks = DownloadManager.getDownloadedTracks(context) }) { Icon(Icons.Rounded.DeleteOutline, null, tint = Color.Red) }
                }
            }
        }
    }
}
