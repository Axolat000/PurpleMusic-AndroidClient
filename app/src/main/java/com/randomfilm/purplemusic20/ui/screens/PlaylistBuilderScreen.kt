package com.randomfilm.purplemusic20.ui.screens

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
fun PlaylistBuilderScreen(allTracks: List<Track>, session: SessionManager, preSelectedId: Int?, onBack: () -> Unit, onSuccess: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var search by remember { mutableStateOf("") }
    val selectedIds = remember { mutableStateListOf<Int>() }
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { preSelectedId?.let { selectedIds.add(it) } }

    val displayList = remember(search, allTracks) { allTracks.filter { it.title.contains(search, true) || it.artist.contains(search, true) } }

    Column(Modifier.fillMaxSize().background(LocalAppColors.current.background)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, null, tint = Color.White) }
            Text(stringResource(R.string.playlist_builder_title), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp).weight(1f))
            if (name.isNotBlank() && selectedIds.isNotEmpty() && !isSaving) {
                TextButton(onClick = {
                    scope.launch {
                        isSaving = true
                        try {
                            ApiClient.service.createPlaylist(name, session.getUsername(), session.getPassword())
                            val updatedPlaylists = ApiClient.service.getPlaylists()
                            val newPlaylist = updatedPlaylists.filter { it.creator_id == session.getUserId() && it.name == name }.maxByOrNull { it.id }
                            if (newPlaylist != null) {
                                selectedIds.forEach { tid -> ApiClient.service.modPlaylist(newPlaylist.id, session.getUsername(), session.getPassword(), "add", tid, null) }
                                Toast.makeText(context, context.getString(R.string.playlist_created_with_tracks, selectedIds.size), Toast.LENGTH_SHORT).show()
                                onSuccess()
                            }
                        } catch (e: Exception) {}
                        isSaving = false
                    }
                }) { Text(stringResource(R.string.action_save), color = LocalAppColors.current.accent, fontWeight = FontWeight.Bold) }
            } else if (isSaving) { CircularProgressIndicator(color = LocalAppColors.current.accent, modifier = Modifier.size(24.dp)) }
        }

        OutlinedTextField(value = name, onValueChange = { name = it }, placeholder = { Text(stringResource(R.string.playlist_name_placeholder), color = LocalAppColors.current.textSecondary) }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
        Spacer(Modifier.height(10.dp))
        TextField(value = search, onValueChange = { search = it }, placeholder = { Text(stringResource(R.string.playlist_search_tracks_placeholder), color = LocalAppColors.current.textSecondary) }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(50.dp), shape = RoundedCornerShape(25.dp), colors = TextFieldDefaults.colors(focusedContainerColor = LocalAppColors.current.panel, unfocusedContainerColor = LocalAppColors.current.panel, focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent))
        Spacer(Modifier.height(16.dp))

        LazyColumn(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp)) {
            items(displayList, key = { it.id }) { track ->
                val isSelected = selectedIds.contains(track.id)
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { if (isSelected) selectedIds.remove(track.id) else selectedIds.add(track.id) }.background(if (isSelected) LocalAppColors.current.primary.copy(alpha=0.3f) else Color.Transparent).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(model = buildImageRequest(context, track.cover_url, session.isCoverCacheEnabled()), contentDescription = null, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(track.title, color = Color.White, fontSize = 14.sp, maxLines = 1, modifier = Modifier.basicMarquee())
                        Text(track.artist, color = LocalAppColors.current.textSecondary, fontSize = 12.sp, maxLines = 1)
                    }
                    Checkbox(checked = isSelected, onCheckedChange = null, colors = CheckboxDefaults.colors(checkedColor = LocalAppColors.current.accent, uncheckedColor = LocalAppColors.current.textSecondary))
                }
            }
        }
    }
}
