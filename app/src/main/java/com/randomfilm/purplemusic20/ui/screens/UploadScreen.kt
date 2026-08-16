package com.randomfilm.purplemusic20.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.randomfilm.purplemusic20.util.*
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody

// ─── UPLOAD SCREEN (Toujours accessible via le + de la NavBar) ────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadScreenImpl(session: SessionManager, onUploadSuccess: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf("") }
    var genre by remember { mutableStateOf("Autre") }
    var genreExpanded by remember { mutableStateOf(false) }
    var audioUri by remember { mutableStateOf<Uri?>(null) }
    var coverUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val audioLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { audioUri = it }
    val coverLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { coverUri = it }

    Column(Modifier.fillMaxSize().padding(24.dp).background(LocalAppColors.current.background)) {
        Spacer(Modifier.height(20.dp))
        Text(stringResource(R.string.upload_title), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(30.dp))
        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text(stringResource(R.string.track_title_label)) }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(value = artist, onValueChange = { artist = it }, label = { Text(stringResource(R.string.track_artist_label)) }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
        Spacer(Modifier.height(10.dp))
        ExposedDropdownMenuBox(expanded = genreExpanded, onExpandedChange = { genreExpanded = it }) {
            OutlinedTextField(value = genre, onValueChange = { genre = it }, label = { Text(stringResource(R.string.track_genre_label)) }, modifier = Modifier.fillMaxWidth().menuAnchor(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White), trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genreExpanded) })
            ExposedDropdownMenu(expanded = genreExpanded, onDismissRequest = { genreExpanded = false }, modifier = Modifier.background(LocalAppColors.current.panel)) {
                listOf("Phonk/Funk", "Rap", "Pop", "Rock", "Electro", "Hyperpop", "Nightcore", "Autre").forEach { s -> DropdownMenuItem(text = { Text(s, color = Color.White) }, onClick = { genre = s; genreExpanded = false }) }
            }
        }
        Spacer(Modifier.height(20.dp))
        Button(onClick = { audioLauncher.launch("audio/*") }, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = LocalAppColors.current.panel)) { Text(if (audioUri != null) stringResource(R.string.upload_audio_selected) else stringResource(R.string.upload_choose_mp3), color = if (audioUri != null) LocalAppColors.current.accent else Color.White) }
        Spacer(Modifier.height(10.dp))
        Button(onClick = { coverLauncher.launch("image/*") }, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = LocalAppColors.current.panel)) { Text(if (coverUri != null) stringResource(R.string.upload_cover_selected) else stringResource(R.string.upload_choose_cover), color = if (coverUri != null) LocalAppColors.current.accent else Color.White) }
        Spacer(Modifier.height(30.dp))
        Button(
            onClick = {
                if (audioUri == null) return@Button
                scope.launch {
                    isLoading = true
                    val audioFile = uriToFile(audioUri!!, context, isAudio = true)
                    val audioPart = MultipartBody.Part.createFormData("music", audioFile.name, audioFile.asRequestBody("audio/mpeg".toMediaTypeOrNull()))
                    var coverPart: MultipartBody.Part? = null
                    if (coverUri != null) { val f = uriToFile(coverUri!!, context); coverPart = MultipartBody.Part.createFormData("cover", f.name, f.asRequestBody("image/*".toMediaTypeOrNull())) }
                    try {
                        ApiClient.service.uploadTrack(title.toRequestBody("text/plain".toMediaTypeOrNull()), artist.toRequestBody("text/plain".toMediaTypeOrNull()), session.getUsername().toRequestBody("text/plain".toMediaTypeOrNull()), session.getPassword().toRequestBody("text/plain".toMediaTypeOrNull()), genre.toRequestBody("text/plain".toMediaTypeOrNull()), audioPart, coverPart)
                        Toast.makeText(context, context.getString(R.string.upload_success_toast), Toast.LENGTH_SHORT).show()
                        title = ""; artist = ""; genre = "Autre"; audioUri = null; coverUri = null; onUploadSuccess()
                    } catch (e: Exception) {}
                    isLoading = false
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = LocalAppColors.current.primary), enabled = !isLoading
        ) { if (isLoading) CircularProgressIndicator(color = Color.White) else Text(stringResource(R.string.upload_publish_button), fontWeight = FontWeight.Bold) }
    }
}
