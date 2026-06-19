package com.randomfilm.purplemusic20.ui.screens

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.randomfilm.purplemusic20.SessionManager
import com.randomfilm.purplemusic20.ui.theme.AccentPurple
import com.randomfilm.purplemusic20.ui.theme.BgDark
import com.randomfilm.purplemusic20.ui.theme.BgPanel
import com.randomfilm.purplemusic20.ui.theme.PrimaryPurple
import com.randomfilm.purplemusic20.ui.theme.TextGray

@Composable
fun TutorialScreen(
    session: SessionManager,
    prefs: SharedPreferences,
    onFinish: () -> Unit
) {
    var step by remember { mutableIntStateOf(0) }
    val totalSteps = 6

    var dynamicTheme by remember { mutableStateOf(session.isDynamicThemeEnabled()) }
    var visualizer by remember { mutableStateOf(prefs.getBoolean("visualizer_enabled", false)) }
    var eqEnabled by remember { mutableStateOf(prefs.getBoolean("eq_enabled", false)) }
    var coverCache by remember { mutableStateOf(session.isCoverCacheEnabled()) }
    var sortMode by remember { mutableStateOf(session.getSortMode()) }
    var sortExpanded by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        visualizer = granted
        prefs.edit().putBoolean("visualizer_enabled", granted).apply()
        if (!granted) {
            Toast.makeText(context, "Permission refusée pour le visualiseur", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        Modifier.fillMaxSize().background(BgDark).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInHorizontally(initialOffsetX = { it }) + fadeIn()).togetherWith(slideOutHorizontally(targetOffsetX = { -it }) + fadeOut())
                } else {
                    (slideInHorizontally(initialOffsetX = { -it }) + fadeIn()).togetherWith(slideOutHorizontally(targetOffsetX = { it }) + fadeOut())
                }
            }, label = "tutorial_animation",
            modifier = Modifier.weight(1f)
        ) { targetStep ->
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                when (targetStep) {
                    0 -> {
                        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                        val alpha by infiniteTransition.animateFloat(
                            initialValue = 0.3f, targetValue = 1f,
                            animationSpec = infiniteRepeatable(animation = tween(1000, easing = LinearEasing), repeatMode = RepeatMode.Reverse), label = "alpha"
                        )
                        val scale by infiniteTransition.animateFloat(
                            initialValue = 0.95f, targetValue = 1.05f,
                            animationSpec = infiniteRepeatable(animation = tween(1000, easing = LinearEasing), repeatMode = RepeatMode.Reverse), label = "scale"
                        )

                        Column(
                            modifier = Modifier.fillMaxSize().clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { step++ },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("Bienvenue sur", color = TextGray, fontSize = 24.sp)
                            Text("PURPLE MUSIC", color = AccentPurple, fontSize = 40.sp, fontWeight = FontWeight.Black)
                            Spacer(Modifier.height(64.dp))
                            Text("Cliquez pour commencer", color = Color.White.copy(alpha = alpha), fontSize = 18.sp, modifier = Modifier.scale(scale))
                        }
                    }
                    1 -> {
                        Text("Thème Dynamique", color = AccentPurple, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        Text("Adapte les couleurs du lecteur en fonction de la pochette de la musique écoutée. (Par défaut: Non)", color = Color.White, textAlign = TextAlign.Center, fontSize = 16.sp)
                        Spacer(Modifier.height(32.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text("Activer le thème dynamique", color = Color.White, modifier = Modifier.weight(1f))
                            Switch(
                                checked = dynamicTheme,
                                onCheckedChange = { dynamicTheme = it; session.setDynamicThemeEnabled(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = AccentPurple, checkedTrackColor = PrimaryPurple)
                            )
                        }
                    }
                    2 -> {
                        Text("Visualiseur Audio", color = AccentPurple, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        Text("Affiche des barres réagissant à la musique sur l'écran du lecteur. Nécessite l'autorisation d'enregistrer l'audio (le micro n'est pas utilisé pour vous écouter).", color = Color.White, textAlign = TextAlign.Center, fontSize = 16.sp)
                        Spacer(Modifier.height(32.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text("Activer le visualiseur", color = Color.White, modifier = Modifier.weight(1f))
                            Switch(
                                checked = visualizer,
                                onCheckedChange = {
                                    if (it) {
                                        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                                            permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                        } else {
                                            visualizer = true
                                            prefs.edit().putBoolean("visualizer_enabled", true).apply()
                                        }
                                    } else {
                                        visualizer = false
                                        prefs.edit().putBoolean("visualizer_enabled", false).apply()
                                    }
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = AccentPurple, checkedTrackColor = PrimaryPurple)
                            )
                        }
                    }
                    3 -> {
                        Text("Filtres & Tri", color = AccentPurple, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        Text("Choisissez l'ordre d'affichage par défaut de vos musiques dans la bibliothèque.", color = Color.White, textAlign = TextAlign.Center, fontSize = 16.sp)
                        Spacer(Modifier.height(32.dp))
                        
                        val sortOptions = mapOf("popular" to "Les plus écoutés", "date_desc" to "Plus récent", "date_asc" to "Plus ancien", "alpha_asc" to "Nom (A-Z)", "alpha_desc" to "Nom (Z-A)", "artist" to "Par Artiste")
                        
                        @OptIn(ExperimentalMaterial3Api::class)
                        ExposedDropdownMenuBox(expanded = sortExpanded, onExpandedChange = { sortExpanded = it }) {
                            OutlinedTextField(
                                value = sortOptions[sortMode] ?: "Plus récent",
                                onValueChange = {}, readOnly = true, label = { Text("Trier par défaut") },
                                modifier = Modifier.fillMaxWidth().menuAnchor(), trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sortExpanded) },
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )
                            ExposedDropdownMenu(expanded = sortExpanded, onDismissRequest = { sortExpanded = false }, modifier = Modifier.background(BgPanel)) {
                                sortOptions.forEach { (key, label) -> DropdownMenuItem(text = { Text(label, color = Color.White) }, onClick = { sortMode = key; session.saveSortMode(key); sortExpanded = false }) }
                            }
                        }
                    }
                    4 -> {
                        Text("Égaliseur", color = AccentPurple, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        Text("Activez l'égaliseur audio intégré pour modifier les fréquences et ajuster le son à votre écoute.", color = Color.White, textAlign = TextAlign.Center, fontSize = 16.sp)
                        Spacer(Modifier.height(32.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text("Activer l'égaliseur", color = Color.White, modifier = Modifier.weight(1f))
                            Switch(
                                checked = eqEnabled,
                                onCheckedChange = { eqEnabled = it; prefs.edit().putBoolean("eq_enabled", it).apply() },
                                colors = SwitchDefaults.colors(checkedThumbColor = AccentPurple, checkedTrackColor = PrimaryPurple)
                            )
                        }
                    }
                    5 -> {
                        Text("Mise en Cache", color = AccentPurple, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        Text("Sauvegardez les pochettes des musiques sur votre appareil pour les charger instantanément la prochaine fois et économiser vos données.", color = Color.White, textAlign = TextAlign.Center, fontSize = 16.sp)
                        Spacer(Modifier.height(32.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text("Activer le cache des covers", color = Color.White, modifier = Modifier.weight(1f))
                            Switch(
                                checked = coverCache,
                                onCheckedChange = { coverCache = it; session.setCoverCacheEnabled(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = AccentPurple, checkedTrackColor = PrimaryPurple)
                            )
                        }
                    }
                }
            }
        }

        if (step > 0) {
            Spacer(Modifier.height(16.dp))

            // Navigation Buttons
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { step-- }) {
                    Text("Retour", color = TextGray)
                }
                
                Row(horizontalArrangement = Arrangement.Center) {
                    for (i in 1 until totalSteps) {
                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (i == step) AccentPurple else TextGray.copy(alpha = 0.5f))
                        )
                    }
                }
                
                if (step < totalSteps - 1) {
                    TextButton(onClick = { step++ }) {
                        Text("Suivant", color = AccentPurple, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = {
                            prefs.edit().putBoolean("tutorial_v2_completed", true).apply()
                            onFinish()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                    ) {
                        Text("Terminer", color = Color.White)
                    }
                }
            }
        }
    }
}
