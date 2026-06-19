package com.randomfilm.purplemusic20.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DownloadForOffline
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.randomfilm.purplemusic20.ApiClient
import com.randomfilm.purplemusic20.SessionManager
import com.randomfilm.purplemusic20.ui.theme.AccentPurple
import com.randomfilm.purplemusic20.ui.theme.BgDark
import com.randomfilm.purplemusic20.ui.theme.BgPanel
import com.randomfilm.purplemusic20.ui.theme.PrimaryPurple
import com.randomfilm.purplemusic20.ui.theme.TextGray
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(session: SessionManager, onOfflineMode: () -> Unit, onSuccess: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }
    var loginUser by remember { mutableStateOf("") }; var loginPass by remember { mutableStateOf("") }; var loginLoading by remember { mutableStateOf(false) }
    var regUser by remember { mutableStateOf("") }; var regPass by remember { mutableStateOf("") }; var regPass2 by remember { mutableStateOf("") }; var regLoading by remember { mutableStateOf(false) }

    // --- RESTAURATION: Changement de serveur ---
    var showServerDialog by remember { mutableStateOf(false) }
    var serverUrlInput by remember { mutableStateOf(session.getServerUrl()) }
    var serverChecking by remember { mutableStateOf(false) }
    var serverError by remember { mutableStateOf("") }

    if (showServerDialog) {
        AlertDialog(
            onDismissRequest = { showServerDialog = false }, containerColor = BgPanel,
            title = { Text("Changer de serveur", color = Color.White) },
            text = {
                Column {
                    Text("Adresse du serveur :", color = TextGray, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = serverUrlInput, onValueChange = { serverUrlInput = it; serverError = "" },
                        placeholder = { Text("https://exemple.com/music/", color = TextGray) }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri), colors = outlinedFieldColors(), modifier = Modifier.fillMaxWidth()
                    )
                    if (serverError.isNotEmpty()) { Spacer(Modifier.height(6.dp)); Text(serverError, color = Color.Red, fontSize = 12.sp) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val url = serverUrlInput.trim()
                    if (url.isEmpty()) { serverError = "Adresse vide."; return@TextButton }
                    scope.launch {
                        serverChecking = true; serverError = ""
                        try {
                            ApiClient.init(url)
                            ApiClient.service.getTracks()
                            session.saveServerUrl(url)
                            showServerDialog = false
                            Toast.makeText(context, "Serveur mis à jour ✓", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) { serverError = "Serveur inaccessible." }
                        serverChecking = false
                    }
                }) { if (serverChecking) CircularProgressIndicator(color = AccentPurple, modifier = Modifier.size(18.dp)) else Text("Confirmer", color = AccentPurple) }
            },
            dismissButton = { TextButton(onClick = { showServerDialog = false }) { Text("Annuler", color = TextGray) } }
        )
    }

    Column(Modifier.fillMaxSize().background(BgDark).padding(horizontal = 32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("PURPLE", color = AccentPurple, fontSize = 48.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(6.6.dp))

        // --- RESTAURATION: Affichage du serveur actuel ---
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = session.getServerUrl().removePrefix("https://").removePrefix("http://").trimEnd('/'),
                color = TextGray, fontSize = 12.sp, maxLines = 1, modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(Modifier.width(6.dp))
            TextButton(
                onClick = { serverUrlInput = session.getServerUrl(); showServerDialog = true },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) { Text("Changer", color = AccentPurple, fontSize = 12.sp) }
        }

        Spacer(Modifier.height(32.dp))

        TabRow(
            selectedTabIndex = selectedTab, containerColor = BgPanel, contentColor = AccentPurple, modifier = Modifier.fillMaxWidth(),
            indicator = { t -> TabRowDefaults.Indicator(modifier = Modifier.tabIndicatorOffset(t[selectedTab]), color = AccentPurple) }
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Connexion", color = if (selectedTab == 0) AccentPurple else TextGray) })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Inscription", color = if (selectedTab == 1) AccentPurple else TextGray) })
        }
        Spacer(Modifier.height(28.dp))

        if (selectedTab == 0) {
            OutlinedTextField(value = loginUser, onValueChange = { loginUser = it }, label = { Text("Utilisateur") }, singleLine = true, colors = outlinedFieldColors(), modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(value = loginPass, onValueChange = { loginPass = it }, label = { Text("Mot de passe") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), colors = outlinedFieldColors(), modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(28.dp))
            Button(
                onClick = {
                    scope.launch {
                        loginLoading = true
                        try {
                            val res = ApiClient.service.login(loginUser, loginPass)
                            if (res.status == "success") { session.saveUser(res.user_id ?: 0, res.username ?: "", loginPass, res.is_admin == true); onSuccess() }
                            else Toast.makeText(context, res.message ?: "Erreur", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {}
                        loginLoading = false
                    }
                }, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple), shape = RoundedCornerShape(12.dp), enabled = !loginLoading
            ) { if (loginLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp)) else Text("CONNEXION", fontWeight = FontWeight.Bold) }
        } else {
            OutlinedTextField(value = regUser, onValueChange = { regUser = it }, label = { Text("Utilisateur") }, singleLine = true, colors = outlinedFieldColors(), modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(value = regPass, onValueChange = { regPass = it }, label = { Text("Mot de passe") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), colors = outlinedFieldColors(), modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(value = regPass2, onValueChange = { regPass2 = it }, label = { Text("Confirmer") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), colors = outlinedFieldColors(), modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(28.dp))
            Button(
                onClick = {
                    if (regPass == regPass2 && regPass.length >= 6) {
                        scope.launch {
                            regLoading = true
                            try {
                                val res = ApiClient.service.register(regUser, regPass)
                                if (res.status == "success") { session.saveUser(ApiClient.service.login(regUser, regPass).user_id?:0, regUser, regPass, false); onSuccess() }
                            } catch (e: Exception) {}
                            regLoading = false
                        }
                    }
                }, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple), shape = RoundedCornerShape(12.dp), enabled = !regLoading
            ) { if (regLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp)) else Text("S'INSCRIRE", fontWeight = FontWeight.Bold) }
        }

        Spacer(Modifier.height(30.dp))
        TextButton(onClick = onOfflineMode) {
            Icon(Icons.Rounded.DownloadForOffline, null, tint = AccentPurple, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Musiques téléchargées (Hors-ligne)", color = AccentPurple, fontWeight = FontWeight.Medium)
        }
    }
}
