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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.randomfilm.purplemusic20.ApiClient
import com.randomfilm.purplemusic20.SessionManager
import com.randomfilm.purplemusic20.ui.theme.AccentPurple
import com.randomfilm.purplemusic20.ui.theme.BgDark
import com.randomfilm.purplemusic20.ui.theme.BgPanel
import com.randomfilm.purplemusic20.ui.theme.PrimaryPurple
import com.randomfilm.purplemusic20.util.uriToFile
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadScreen(session: SessionManager, onUploadSuccess: () -> Unit) {
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

    Column(Modifier.fillMaxSize().padding(24.dp).background(BgDark)) {
        Spacer(Modifier.height(20.dp))
        Text("Publier un Titre", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(30.dp))
        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Titre") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(value = artist, onValueChange = { artist = it }, label = { Text("Artiste") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
        Spacer(Modifier.height(10.dp))
        ExposedDropdownMenuBox(expanded = genreExpanded, onExpandedChange = { genreExpanded = it }) {
            OutlinedTextField(value = genre, onValueChange = { genre = it }, label = { Text("Genre") }, modifier = Modifier.fillMaxWidth().menuAnchor(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White), trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genreExpanded) })
            ExposedDropdownMenu(expanded = genreExpanded, onDismissRequest = { genreExpanded = false }, modifier = Modifier.background(BgPanel)) {
                listOf("Phonk/Funk", "Rap", "Pop", "Rock", "Electro", "Hyperpop", "Nightcore", "Autre").forEach { s -> DropdownMenuItem(text = { Text(s, color = Color.White) }, onClick = { genre = s; genreExpanded = false }) }
            }
        }
        Spacer(Modifier.height(20.dp))
        Button(onClick = { audioLauncher.launch("audio/*") }, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = BgPanel)) { Text(if (audioUri != null) "✅ Audio OK" else "Choisir MP3", color = if (audioUri != null) AccentPurple else Color.White) }
        Spacer(Modifier.height(10.dp))
        Button(onClick = { coverLauncher.launch("image/*") }, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = BgPanel)) { Text(if (coverUri != null) "✅ Cover OK" else "Choisir Cover", color = if (coverUri != null) AccentPurple else Color.White) }
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
                        Toast.makeText(context, "Succès", Toast.LENGTH_SHORT).show()
                        title = ""; artist = ""; genre = "Autre"; audioUri = null; coverUri = null; onUploadSuccess()
                    } catch (e: Exception) {}
                    isLoading = false
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple), enabled = !isLoading
        ) { if (isLoading) CircularProgressIndicator(color = Color.White) else Text("PUBLIER", fontWeight = FontWeight.Bold) }
    }
}
