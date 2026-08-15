package com.randomfilm.purplemusic20.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.randomfilm.purplemusic20.R
import com.randomfilm.purplemusic20.data.*
import com.randomfilm.purplemusic20.ui.theme.*
import kotlinx.coroutines.launch

// ─── Ecrans de création/sélection de Playlist (FULL SCREEN) ───────────────────
@Composable
fun SelectPlaylistScreen(trackId: Int, allPlaylists: List<Playlist>, session: SessionManager, onBack: () -> Unit, onNewPlaylist: () -> Unit, onSuccess: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val myPlaylists = remember(allPlaylists) { allPlaylists.filter { it.creator_id == session.getUserId() || session.isAdmin() } }

    Column(Modifier.fillMaxSize().background(LocalAppColors.current.background).padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, null, tint = Color.White) }
            Text(stringResource(R.string.select_playlist_title), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = onNewPlaylist, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = LocalAppColors.current.primary)) {
            Icon(Icons.Rounded.Add, null, tint = Color.White); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.select_playlist_new_mix), fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(myPlaylists, key = { it.id }) { p ->
                Card(colors = CardDefaults.cardColors(containerColor = LocalAppColors.current.panel), modifier = Modifier.fillMaxWidth().clickable {
                    scope.launch {
                        try { ApiClient.service.modPlaylist(p.id, session.getUsername(), session.getPassword(), "add", trackId, null); Toast.makeText(context, context.getString(R.string.playlist_added_to, p.name), Toast.LENGTH_SHORT).show(); onSuccess() } catch (e: Exception) {}
                    }
                }) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Rounded.Album, null, tint = LocalAppColors.current.accent); Spacer(Modifier.width(16.dp)); Text(p.name, color = Color.White, fontSize = 16.sp) } }
            }
        }
    }
}
