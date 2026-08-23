package com.randomfilm.purplemusic20.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.randomfilm.purplemusic20.R
import com.randomfilm.purplemusic20.data.*
import com.randomfilm.purplemusic20.ui.theme.*
import com.randomfilm.purplemusic20.util.*
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody

// --- RESTAURATION: Menu de modification complet ---
@Composable
fun EditTrackDialog(track: Track, session: SessionManager, onDismiss: () -> Unit, onSuccess: () -> Unit) {
    var title by remember { mutableStateOf(track.title) }
    var artist by remember { mutableStateOf(track.artist) }
    var album by remember { mutableStateOf(track.album ?: "") }
    var genre by remember { mutableStateOf(track.genre ?: "Autre") }
    var newCoverUri by remember { mutableStateOf<Uri?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { newCoverUri = it }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss, containerColor = LocalAppColors.current.panel,
        title = { Text(stringResource(R.string.action_edit), color = Color.White) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(100.dp).clip(RoundedCornerShape(12.dp)).background(Color.DarkGray).clickable { launcher.launch("image/*") }) {
                    AsyncImage(model = newCoverUri ?: track.cover_url, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    Icon(Icons.Rounded.Edit, null, tint = Color.White, modifier = Modifier.align(Alignment.Center))
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text(stringResource(R.string.track_title_label)) }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                OutlinedTextField(value = artist, onValueChange = { artist = it }, label = { Text(stringResource(R.string.track_artist_label)) }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                OutlinedTextField(value = album, onValueChange = { album = it }, label = { Text(stringResource(R.string.track_album_label)) }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                OutlinedTextField(value = genre, onValueChange = { genre = it }, label = { Text(stringResource(R.string.track_genre_label)) }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
            }
        },
        confirmButton = {
            TextButton(onClick = {
                scope.launch {
                    val tid = track.id.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                    val u = session.getUsername().toRequestBody("text/plain".toMediaTypeOrNull())
                    val p = session.getPassword().toRequestBody("text/plain".toMediaTypeOrNull())
                    val t = title.toRequestBody("text/plain".toMediaTypeOrNull())
                    val a = artist.toRequestBody("text/plain".toMediaTypeOrNull())
                    val g = genre.toRequestBody("text/plain".toMediaTypeOrNull())
                    val al = album.toRequestBody("text/plain".toMediaTypeOrNull())
                    var cp: MultipartBody.Part? = null
                    if (newCoverUri != null) {
                        val f = uriToFile(newCoverUri!!, context)
                        cp = MultipartBody.Part.createFormData("new_cover", f.name, f.asRequestBody("image/*".toMediaTypeOrNull()))
                    }
                    try { ApiClient.service.editTrack(tid, u, p, t, a, g, al, cp); onSuccess() } catch (e: Exception) {}
                }
            }) { Text(stringResource(R.string.action_save), color = LocalAppColors.current.accent) }
        },
        dismissButton = {
            TextButton(onClick = {
                scope.launch {
                    try { ApiClient.service.deleteTrack(track.id, session.getUsername(), session.getPassword()); onSuccess() } catch(e:Exception){}
                }
            }) { Text(stringResource(R.string.action_delete), color = Color.Red) }
        }
    )
}
