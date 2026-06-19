package com.randomfilm.purplemusic20.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.randomfilm.purplemusic20.ApiClient
import com.randomfilm.purplemusic20.SessionManager
import com.randomfilm.purplemusic20.ui.theme.AccentPurple
import com.randomfilm.purplemusic20.ui.theme.BgDark
import com.randomfilm.purplemusic20.ui.theme.PrimaryPurple
import com.randomfilm.purplemusic20.ui.theme.TextGray
import kotlinx.coroutines.launch

@Composable
fun ServerSetupScreen(session: SessionManager, onConfirm: () -> Unit) {
    val scope = rememberCoroutineScope()
    var serverUrl by remember { mutableStateOf("") }
    var isChecking by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().background(BgDark).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("PURPLE", color = AccentPurple, fontSize = 48.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(8.dp))
        Text("Configuration du serveur", color = TextGray, fontSize = 14.sp)
        Spacer(Modifier.height(48.dp))
        OutlinedTextField(value = serverUrl, onValueChange = { serverUrl = it; errorMsg = "" }, placeholder = { Text("https://exemple.com/music/", color = TextGray) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri), colors = outlinedFieldColors(), modifier = Modifier.fillMaxWidth())
        if (errorMsg.isNotEmpty()) { Spacer(Modifier.height(8.dp)); Text(errorMsg, color = Color.Red, fontSize = 13.sp) }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                val url = serverUrl.trim()
                if (url.isEmpty()) { errorMsg = "Veuillez entrer une adresse."; return@Button }
                scope.launch {
                    isChecking = true; errorMsg = ""
                    try { ApiClient.init(url); ApiClient.service.getTracks(); session.saveServerUrl(url); onConfirm() }
                    catch (e: Exception) { errorMsg = "Impossible de joindre le serveur." }
                    isChecking = false
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple), shape = RoundedCornerShape(12.dp), enabled = !isChecking
        ) { if (isChecking) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp)) else Text("CONFIRMER", fontWeight = FontWeight.Bold) }
    }
}

@Composable
fun outlinedFieldColors() = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.Gray, focusedBorderColor = AccentPurple, unfocusedBorderColor = TextGray, cursorColor = AccentPurple, focusedLabelColor = AccentPurple, unfocusedLabelColor = TextGray)
