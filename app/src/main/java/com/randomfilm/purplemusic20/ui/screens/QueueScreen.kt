package com.randomfilm.purplemusic20.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import coil.compose.AsyncImage
import com.randomfilm.purplemusic20.R
import com.randomfilm.purplemusic20.SessionManager
import com.randomfilm.purplemusic20.ui.theme.AccentPurple
import com.randomfilm.purplemusic20.ui.theme.BgDark
import com.randomfilm.purplemusic20.ui.theme.TextGray
import com.randomfilm.purplemusic20.util.buildImageRequest

@Composable
fun QueueScreen(controller: MediaController?, currentIndex: Int, queueVersion: Int, session: SessionManager, onPlay: (Int) -> Unit, onClose: () -> Unit) {
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
            itemsIndexed(queue, key = { _, item -> item.mediaId }) { idx, item ->
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { onPlay(idx) }, verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(model = buildImageRequest(LocalContext.current, item.mediaMetadata.artworkUri ?: R.drawable.default_cover, session.isCoverCacheEnabled()), contentDescription = null, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
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
