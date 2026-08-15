package com.randomfilm.purplemusic20.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.randomfilm.purplemusic20.R
import com.randomfilm.purplemusic20.data.*
import com.randomfilm.purplemusic20.ui.theme.*
import kotlinx.coroutines.launch

// ─── Ecrans Setup & Auth ───────────────────────────────────────────────────────
@Composable
fun ServerSetupScreen(session: SessionManager, onConfirm: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var serverUrl by remember { mutableStateOf("") }
    var isChecking by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().background(LocalAppColors.current.background).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(stringResource(R.string.app_brand_name), color = LocalAppColors.current.accent, fontSize = 48.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.auth_server_setup_title), color = LocalAppColors.current.textSecondary, fontSize = 14.sp)
        Spacer(Modifier.height(48.dp))
        OutlinedTextField(value = serverUrl, onValueChange = { serverUrl = it; errorMsg = "" }, placeholder = { Text(stringResource(R.string.auth_server_url_placeholder), color = LocalAppColors.current.textSecondary) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri), colors = outlinedFieldColors(), modifier = Modifier.fillMaxWidth())
        if (errorMsg.isNotEmpty()) { Spacer(Modifier.height(8.dp)); Text(errorMsg, color = Color.Red, fontSize = 13.sp) }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                val url = serverUrl.trim()
                if (url.isEmpty()) { errorMsg = context.getString(R.string.auth_enter_address_error); return@Button }
                scope.launch {
                    isChecking = true; errorMsg = ""
                    try { ApiClient.init(url); ApiClient.service.getTracks(); session.saveServerUrl(url); onConfirm() }
                    catch (e: Exception) { errorMsg = context.getString(R.string.auth_server_unreachable_error) }
                    isChecking = false
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = LocalAppColors.current.primary), shape = RoundedCornerShape(12.dp), enabled = !isChecking
        ) { if (isChecking) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp)) else Text(stringResource(R.string.auth_confirm_button), fontWeight = FontWeight.Bold) }
    }
}

@Composable
fun LoginScreenImpl(session: SessionManager, onOfflineMode: () -> Unit, onSuccess: () -> Unit) {
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
            onDismissRequest = { showServerDialog = false }, containerColor = LocalAppColors.current.panel,
            title = { Text(stringResource(R.string.auth_change_server_title), color = Color.White) },
            text = {
                Column {
                    Text(stringResource(R.string.auth_server_address_label), color = LocalAppColors.current.textSecondary, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = serverUrlInput, onValueChange = { serverUrlInput = it; serverError = "" },
                        placeholder = { Text(stringResource(R.string.auth_server_url_placeholder), color = LocalAppColors.current.textSecondary) }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri), colors = outlinedFieldColors(), modifier = Modifier.fillMaxWidth()
                    )
                    if (serverError.isNotEmpty()) { Spacer(Modifier.height(6.dp)); Text(serverError, color = Color.Red, fontSize = 12.sp) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val url = serverUrlInput.trim()
                    if (url.isEmpty()) { serverError = context.getString(R.string.auth_empty_address_error); return@TextButton }
                    scope.launch {
                        serverChecking = true; serverError = ""
                        try {
                            ApiClient.init(url)
                            ApiClient.service.getTracks()
                            session.saveServerUrl(url)
                            showServerDialog = false
                            Toast.makeText(context, context.getString(R.string.auth_server_updated_toast), Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) { serverError = context.getString(R.string.auth_server_unreachable_short) }
                        serverChecking = false
                    }
                }) { if (serverChecking) CircularProgressIndicator(color = LocalAppColors.current.accent, modifier = Modifier.size(18.dp)) else Text(stringResource(R.string.auth_dialog_confirm), color = LocalAppColors.current.accent) }
            },
            dismissButton = { TextButton(onClick = { showServerDialog = false }) { Text(stringResource(R.string.auth_dialog_cancel), color = LocalAppColors.current.textSecondary) } }
        )
    }

    Column(Modifier.fillMaxSize().background(LocalAppColors.current.background).padding(horizontal = 32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(stringResource(R.string.app_brand_name), color = LocalAppColors.current.accent, fontSize = 48.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(6.dp))

        // --- RESTAURATION: Affichage du serveur actuel ---
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = session.getServerUrl().removePrefix("https://").removePrefix("http://").trimEnd('/'),
                color = LocalAppColors.current.textSecondary, fontSize = 12.sp, maxLines = 1, modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(Modifier.width(6.dp))
            TextButton(
                onClick = { serverUrlInput = session.getServerUrl(); showServerDialog = true },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) { Text(stringResource(R.string.auth_change_button), color = LocalAppColors.current.accent, fontSize = 12.sp) }
        }

        Spacer(Modifier.height(32.dp))

        TabRow(
            selectedTabIndex = selectedTab, containerColor = LocalAppColors.current.panel, contentColor = LocalAppColors.current.accent, modifier = Modifier.fillMaxWidth(),
            indicator = { t -> TabRowDefaults.Indicator(modifier = Modifier.tabIndicatorOffset(t[selectedTab]), color = LocalAppColors.current.accent) }
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text(stringResource(R.string.auth_tab_login), color = if (selectedTab == 0) LocalAppColors.current.accent else LocalAppColors.current.textSecondary) })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text(stringResource(R.string.auth_tab_register), color = if (selectedTab == 1) LocalAppColors.current.accent else LocalAppColors.current.textSecondary) })
        }
        Spacer(Modifier.height(28.dp))

        if (selectedTab == 0) {
            OutlinedTextField(value = loginUser, onValueChange = { loginUser = it }, label = { Text(stringResource(R.string.auth_username_label)) }, singleLine = true, colors = outlinedFieldColors(), modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(value = loginPass, onValueChange = { loginPass = it }, label = { Text(stringResource(R.string.auth_password_label)) }, singleLine = true, visualTransformation = PasswordVisualTransformation(), colors = outlinedFieldColors(), modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(28.dp))
            Button(
                onClick = {
                    scope.launch {
                        loginLoading = true
                        try {
                            val res = ApiClient.service.login(loginUser, loginPass)
                            if (res.status == "success") { session.saveUser(res.user_id ?: 0, res.username ?: "", loginPass, res.is_admin == true); onSuccess() }
                            else Toast.makeText(context, res.message ?: context.getString(R.string.auth_generic_error), Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {}
                        loginLoading = false
                    }
                }, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = LocalAppColors.current.primary), shape = RoundedCornerShape(12.dp), enabled = !loginLoading
            ) { if (loginLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp)) else Text(stringResource(R.string.auth_login_button), fontWeight = FontWeight.Bold) }
        } else {
            OutlinedTextField(value = regUser, onValueChange = { regUser = it }, label = { Text(stringResource(R.string.auth_username_label)) }, singleLine = true, colors = outlinedFieldColors(), modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(value = regPass, onValueChange = { regPass = it }, label = { Text(stringResource(R.string.auth_password_label)) }, singleLine = true, visualTransformation = PasswordVisualTransformation(), colors = outlinedFieldColors(), modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(value = regPass2, onValueChange = { regPass2 = it }, label = { Text(stringResource(R.string.auth_confirm_password_label)) }, singleLine = true, visualTransformation = PasswordVisualTransformation(), colors = outlinedFieldColors(), modifier = Modifier.fillMaxWidth())
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
                }, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = LocalAppColors.current.primary), shape = RoundedCornerShape(12.dp), enabled = !regLoading
            ) { if (regLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp)) else Text(stringResource(R.string.auth_register_button), fontWeight = FontWeight.Bold) }
        }

        Spacer(Modifier.height(30.dp))
        TextButton(onClick = onOfflineMode) {
            Icon(Icons.Rounded.DownloadForOffline, null, tint = LocalAppColors.current.accent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.auth_offline_downloads), color = LocalAppColors.current.accent, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun outlinedFieldColors() = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.Gray, focusedBorderColor = LocalAppColors.current.accent, unfocusedBorderColor = LocalAppColors.current.textSecondary, cursorColor = LocalAppColors.current.accent, focusedLabelColor = LocalAppColors.current.accent, unfocusedLabelColor = LocalAppColors.current.textSecondary)
