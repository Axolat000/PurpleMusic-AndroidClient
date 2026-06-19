package com.randomfilm.purplemusic20.ui.components

import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.imageLoader
import com.randomfilm.purplemusic20.SessionManager
import com.randomfilm.purplemusic20.ui.theme.AccentPurple
import com.randomfilm.purplemusic20.ui.theme.BgDark
import com.randomfilm.purplemusic20.ui.theme.BgPanel
import com.randomfilm.purplemusic20.ui.theme.PrimaryPurple
import com.randomfilm.purplemusic20.ui.theme.TextGray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(session: SessionManager, currentHidden: Set<String>, currentVolume: Float, currentSortMode: String, onSave: (Set<String>, String) -> Unit, onVolumeChange: (Float) -> Unit, onSetSleepTimer: (Int) -> Unit, onRedoTutorial: () -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var localHidden by remember { mutableStateOf(currentHidden) }
    var localVolume by remember { mutableFloatStateOf(currentVolume) }
    var localSortMode by remember { mutableStateOf(currentSortMode) }
    var localSleepTimer by remember { mutableIntStateOf(0) }
    var coverCacheEnabled by remember { mutableStateOf(session.isCoverCacheEnabled()) }
    var customGenreInput by remember { mutableStateOf("") }

    val prefs = context.getSharedPreferences("purple_music_state", Context.MODE_PRIVATE)
    var visualizerEnabled by remember { mutableStateOf(prefs.getBoolean("visualizer_enabled", false)) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        visualizerEnabled = granted
        prefs.edit().putBoolean("visualizer_enabled", granted).apply()
        if (!granted) {
            Toast.makeText(context, "Permission refusée pour le visualiseur", Toast.LENGTH_SHORT).show()
        }
    }

    // --- RESTAURATION: Noms lisibles pour les options de tri ---
    val sortOptions = mapOf("popular" to "Les plus écoutés", "date_desc" to "Plus récent", "date_asc" to "Plus ancien", "alpha_asc" to "Nom (A-Z)", "alpha_desc" to "Nom (Z-A)", "artist" to "Par Artiste")

    val predefinedGenres = listOf("Phonk/Funk", "Rap", "Pop", "Rock", "Electro", "Hyperpop", "Nightcore", "Qualité inférieure", "Autre")
    val displayGenres = (predefinedGenres + localHidden).distinct()
    var sortExpanded by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Général", "Filtres & Tri", "Égaliseur", "Avancé")

    var eqEnabled by remember { mutableStateOf(prefs.getBoolean("eq_enabled", false)) }
    var eqBands by remember { mutableStateOf<List<Short>>(emptyList()) }
    var eqLevels by remember { mutableStateOf<Map<Short, Short>>(emptyMap()) }
    var eqMinLevel by remember { mutableStateOf<Short>(0) }
    var eqMaxLevel by remember { mutableStateOf<Short>(0) }
    var eqCenterFreqs by remember { mutableStateOf<Map<Short, Int>>(emptyMap()) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val audioSessionId = prefs.getInt("audio_session_id", 0)
                val tempEq = android.media.audiofx.Equalizer(0, audioSessionId)
                val bandsList = (0 until tempEq.numberOfBands).map { it.toShort() }
                val minL = tempEq.bandLevelRange[0]
                val maxL = tempEq.bandLevelRange[1]
                val freqs = mutableMapOf<Short, Int>()
                val initialLevels = mutableMapOf<Short, Short>()
                for (i in bandsList) {
                    freqs[i] = tempEq.getCenterFreq(i)
                    initialLevels[i] = prefs.getInt("eq_band_$i", tempEq.getBandLevel(i).toInt()).toShort()
                }
                tempEq.release()
                withContext(Dispatchers.Main) {
                    eqBands = bandsList
                    eqMinLevel = minL
                    eqMaxLevel = maxL
                    eqCenterFreqs = freqs
                    eqLevels = initialLevels
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Dialog(
        onDismissRequest = {
            onSave(localHidden, localSortMode)
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = BgPanel)
        ) {
            Column(Modifier.fillMaxSize().padding(16.dp)) {
                Text("Paramètres", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))

                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = BgPanel,
                    contentColor = AccentPurple,
                    edgePadding = 0.dp,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = AccentPurple
                        )
                    },
                    divider = { HorizontalDivider(color = Color.White.copy(alpha = 0.1f)) }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, color = if (selectedTab == index) AccentPurple else TextGray) }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                    when (selectedTab) {
                        0 -> { // Général
                            item {
                                Text("Volume interne :", color = TextGray, fontSize = 14.sp)
                                Slider(value = localVolume, onValueChange = { localVolume = it; onVolumeChange(it) }, colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = AccentPurple, inactiveTrackColor = Color.White.copy(0.2f)))
                                Spacer(Modifier.height(15.dp))
                                
                                Text("Minuteur de sommeil :", color = TextGray, fontSize = 14.sp)
                                var sleepTimerExpanded by remember { mutableStateOf(false) }
                                val timerOptions = listOf(0, 15, 30, 45, 60, 90, 120)
                                ExposedDropdownMenuBox(expanded = sleepTimerExpanded, onExpandedChange = { sleepTimerExpanded = it }) {
                                    OutlinedTextField(
                                        value = if (localSleepTimer == 0) "Désactivé" else "$localSleepTimer minutes",
                                        onValueChange = {}, readOnly = true,
                                        modifier = Modifier.fillMaxWidth().menuAnchor(), trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sleepTimerExpanded) },
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                    )
                                    ExposedDropdownMenu(expanded = sleepTimerExpanded, onDismissRequest = { sleepTimerExpanded = false }, modifier = Modifier.background(BgPanel)) {
                                        timerOptions.forEach { mins -> DropdownMenuItem(text = { Text(if (mins == 0) "Désactivé" else "$mins minutes", color = Color.White) }, onClick = { localSleepTimer = mins; sleepTimerExpanded = false; onSetSleepTimer(mins) }) }
                                    }
                                }
                                Spacer(Modifier.height(15.dp))

                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                    Text("Thème dynamique (Lecteur)", color = Color.White, fontSize = 14.sp)
                                    var dynamicTheme by remember { mutableStateOf(session.isDynamicThemeEnabled()) }
                                    Switch(checked = dynamicTheme, onCheckedChange = { dynamicTheme = it; session.setDynamicThemeEnabled(it) }, colors = SwitchDefaults.colors(checkedThumbColor = AccentPurple, checkedTrackColor = PrimaryPurple))
                                }
                                Spacer(Modifier.height(15.dp))

                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                    Text("Visualiseur Audio (Spectre)", color = Color.White, fontSize = 14.sp)
                                    Switch(checked = visualizerEnabled, onCheckedChange = { 
                                        if (it) {
                                            if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                                permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                            } else {
                                                visualizerEnabled = true
                                                prefs.edit().putBoolean("visualizer_enabled", true).apply()
                                            }
                                        } else {
                                            visualizerEnabled = false
                                            prefs.edit().putBoolean("visualizer_enabled", false).apply()
                                        }
                                    }, colors = SwitchDefaults.colors(checkedThumbColor = AccentPurple, checkedTrackColor = PrimaryPurple))
                                }
                            }
                        }
                        1 -> { // Filtres & Tri
                            item {
                                ExposedDropdownMenuBox(expanded = sortExpanded, onExpandedChange = { sortExpanded = it }) {
                                    OutlinedTextField(
                                        value = sortOptions[localSortMode] ?: "Plus récent",
                                        onValueChange = {}, readOnly = true, label = { Text("Trier par défaut") },
                                        modifier = Modifier.fillMaxWidth().menuAnchor(), trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sortExpanded) },
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                    )
                                    ExposedDropdownMenu(expanded = sortExpanded, onDismissRequest = { sortExpanded = false }, modifier = Modifier.background(BgPanel)) {
                                        sortOptions.forEach { (key, label) -> DropdownMenuItem(text = { Text(label, color = Color.White) }, onClick = { localSortMode = key; sortExpanded = false }) }
                                    }
                                }
                                Spacer(Modifier.height(20.dp))
                                Text("Masquer les genres :", color = TextGray, fontSize = 14.sp)
                            }
                            items(displayGenres) { genre ->
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { localHidden = if (localHidden.contains(genre)) localHidden - genre else localHidden + genre }) {
                                    Checkbox(checked = localHidden.contains(genre), onCheckedChange = { c -> localHidden = if (c) localHidden + genre else localHidden - genre }, colors = CheckboxDefaults.colors(checkedColor = Color.Red, uncheckedColor = TextGray))
                                    Text(genre, color = Color.White)
                                }
                            }
                            item {
                                Spacer(Modifier.height(10.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(
                                        value = customGenreInput, onValueChange = { customGenreInput = it },
                                        label = { Text("Autre genre à masquer", fontSize = 12.sp) },
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White), modifier = Modifier.weight(1f).height(60.dp)
                                    )
                                    IconButton(onClick = { if (customGenreInput.isNotBlank()) { localHidden = localHidden + customGenreInput; customGenreInput = "" } }) { Icon(Icons.Rounded.Add, "Ajouter", tint = AccentPurple) }
                                }
                            }
                        }
                        2 -> { // Égaliseur
                            item {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                    Text("Activer l'égaliseur", color = Color.White, fontSize = 14.sp)
                                    Switch(checked = eqEnabled, onCheckedChange = { 
                                        eqEnabled = it
                                        prefs.edit().putBoolean("eq_enabled", it).apply()
                                    }, colors = SwitchDefaults.colors(checkedThumbColor = AccentPurple, checkedTrackColor = PrimaryPurple))
                                }
                                Spacer(Modifier.height(15.dp))
                            }
                            if (eqBands.isNotEmpty()) {
                                items(eqBands) { band ->
                                    val freqHz = eqCenterFreqs[band] ?: 0
                                    val freqText = if (freqHz >= 1000000) "${freqHz / 1000000} kHz" else "${freqHz / 1000} Hz"
                                    val level = eqLevels[band] ?: 0
                                    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(freqText, color = Color.White, fontSize = 12.sp)
                                            Text("${level / 100} dB", color = TextGray, fontSize = 12.sp)
                                        }
                                        Slider(
                                            value = level.toFloat(),
                                            onValueChange = { newVal ->
                                                val newLevel = newVal.toInt().toShort()
                                                eqLevels = eqLevels.toMutableMap().apply { put(band, newLevel) }
                                                prefs.edit().putInt("eq_band_$band", newLevel.toInt()).apply()
                                            },
                                            valueRange = eqMinLevel.toFloat()..eqMaxLevel.toFloat(),
                                            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = AccentPurple, inactiveTrackColor = Color.White.copy(0.2f)),
                                            enabled = eqEnabled
                                        )
                                    }
                                }
                            } else {
                                item {
                                    Text("Égaliseur non disponible", color = TextGray)
                                }
                            }
                        }
                        3 -> { // Avancé / Opti
                            item {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                    Text("Mettre en cache les covers", color = Color.White, fontSize = 14.sp)
                                    Switch(checked = coverCacheEnabled, onCheckedChange = { coverCacheEnabled = it; session.setCoverCacheEnabled(it) }, colors = SwitchDefaults.colors(checkedThumbColor = AccentPurple, checkedTrackColor = PrimaryPurple))
                                }
                                Spacer(Modifier.height(10.dp))
                                Button(onClick = { context.imageLoader.diskCache?.clear(); context.imageLoader.memoryCache?.clear(); Toast.makeText(context, "Cache vidé", Toast.LENGTH_SHORT).show() }, colors = ButtonDefaults.buttonColors(containerColor = BgDark), modifier = Modifier.fillMaxWidth()) { Text("Vider le cache des covers", color = AccentPurple) }
                                Spacer(Modifier.height(10.dp))
                                Button(onClick = onRedoTutorial, colors = ButtonDefaults.buttonColors(containerColor = BgDark), modifier = Modifier.fillMaxWidth()) { Text("Refaire le tutoriel", color = AccentPurple) }
                            }
                        }
                    }
                }
                
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { onSave(localHidden, localSortMode); onDismiss() }) { Text("Fermer", color = AccentPurple) }
                }
            }
        }
    }
}
