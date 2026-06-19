package com.randomfilm.purplemusic20.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.randomfilm.purplemusic20.*
import com.randomfilm.purplemusic20.ui.theme.AccentPurple
import com.randomfilm.purplemusic20.ui.theme.BgPanel
import kotlinx.coroutines.launch

@Composable
fun ManagePlaylistDialog(playlist: Playlist, allTracks: List<Track>, session: SessionManager, onDismiss: () -> Unit, onSuccess: () -> Unit) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(playlist.name) }
    val ids = playlist.song_ids.split(",").mapNotNull { it.trim().toIntOrNull() }
    val pTracks = allTracks.filter { it.id in ids }

    AlertDialog(
        onDismissRequest = onDismiss, containerColor = BgPanel,
        title = { Text("Gérer Playlist", color = Color.White) },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nom") }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White))
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
                TextButton(onClick = { scope.launch { ApiClient.service.modPlaylist(playlist.id, session.getUsername(), session.getPassword(), "delete", null, null); onSuccess() } }) { Text("Supprimer", color = Color.Red) }
                TextButton(onClick = { scope.launch { ApiClient.service.modPlaylist(playlist.id, session.getUsername(), session.getPassword(), "rename", null, name); onSuccess() } }) { Text("Sauver", color = AccentPurple) }
            }
        }
    )
}
