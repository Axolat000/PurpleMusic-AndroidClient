package com.randomfilm.purplemusic20.ui.screens

import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import coil.imageLoader
import com.randomfilm.purplemusic20.R
import com.randomfilm.purplemusic20.data.*
import com.randomfilm.purplemusic20.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ─── Settings Dialog ──────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(session: SessionManager, currentHidden: Set<String>, currentVolume: Float, currentSortMode: String, currentThemePreset: String, currentMaterialYouEnabled: Boolean, onSave: (Set<String>, String) -> Unit, onVolumeChange: (Float) -> Unit, onSetSleepTimer: (Int) -> Unit, onRedoTutorial: () -> Unit, onThemeChange: (String) -> Unit, onMaterialYouChange: (Boolean) -> Unit, onDismiss: () -> Unit) {
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
            Toast.makeText(context, context.getString(R.string.settings_visualizer_permission_denied), Toast.LENGTH_SHORT).show()
        }
    }

    // --- RESTAURATION: Noms lisibles pour les options de tri ---
    val sortOptions = mapOf(
        "popular" to stringResource(R.string.sort_popular),
        "date_desc" to stringResource(R.string.sort_recent),
        "date_asc" to stringResource(R.string.sort_oldest),
        "alpha_asc" to stringResource(R.string.sort_name_asc),
        "alpha_desc" to stringResource(R.string.sort_name_desc),
        "artist" to stringResource(R.string.sort_by_artist)
    )

    val predefinedGenres = listOf("Phonk/Funk", "Rap", "Pop", "Rock", "Electro", "Hyperpop", "Nightcore", "Qualité inférieure", "Autre")
    val displayGenres = (predefinedGenres + localHidden).distinct()
    var sortExpanded by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        stringResource(R.string.settings_tab_general),
        stringResource(R.string.settings_tab_filters_sort),
        stringResource(R.string.settings_tab_equalizer),
        stringResource(R.string.settings_tab_appearance),
        stringResource(R.string.settings_tab_advanced)
    )

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
            colors = CardDefaults.cardColors(containerColor = LocalAppColors.current.panel)
        ) {
            Column(Modifier.fillMaxSize().padding(16.dp)) {
                Text(stringResource(R.string.settings_dialog_title), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))

                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = LocalAppColors.current.panel,
                    contentColor = LocalAppColors.current.accent,
                    edgePadding = 0.dp,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = LocalAppColors.current.accent
                        )
                    },
                    divider = { HorizontalDivider(color = Color.White.copy(alpha = 0.1f)) }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, color = if (selectedTab == index) LocalAppColors.current.accent else LocalAppColors.current.textSecondary) }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                    when (selectedTab) {
                        0 -> { // Général
                            item {
                                Text(stringResource(R.string.settings_volume_label), color = LocalAppColors.current.textSecondary, fontSize = 14.sp)
                                Slider(value = localVolume, onValueChange = { localVolume = it; onVolumeChange(it) }, colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = LocalAppColors.current.accent, inactiveTrackColor = Color.White.copy(0.2f)))
                                Spacer(Modifier.height(15.dp))

                                Text(stringResource(R.string.settings_sleep_timer_label), color = LocalAppColors.current.textSecondary, fontSize = 14.sp)
                                var sleepTimerExpanded by remember { mutableStateOf(false) }
                                val timerOptions = listOf(0, 15, 30, 45, 60, 90, 120)
                                val sleepTimerDisabledText = stringResource(R.string.settings_sleep_timer_disabled)
                                ExposedDropdownMenuBox(expanded = sleepTimerExpanded, onExpandedChange = { sleepTimerExpanded = it }) {
                                    OutlinedTextField(
                                        value = if (localSleepTimer == 0) sleepTimerDisabledText else stringResource(R.string.settings_minutes_format, localSleepTimer),
                                        onValueChange = {}, readOnly = true,
                                        modifier = Modifier.fillMaxWidth().menuAnchor(), trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sleepTimerExpanded) },
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                    )
                                    ExposedDropdownMenu(expanded = sleepTimerExpanded, onDismissRequest = { sleepTimerExpanded = false }, modifier = Modifier.background(LocalAppColors.current.panel)) {
                                        timerOptions.forEach { mins -> DropdownMenuItem(text = { Text(if (mins == 0) sleepTimerDisabledText else stringResource(R.string.settings_minutes_format, mins), color = Color.White) }, onClick = { localSleepTimer = mins; sleepTimerExpanded = false; onSetSleepTimer(mins) }) }
                                    }
                                }
                                Spacer(Modifier.height(15.dp))

                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                    Text(stringResource(R.string.settings_dynamic_theme_label), color = Color.White, fontSize = 14.sp)
                                    var dynamicTheme by remember { mutableStateOf(session.isDynamicThemeEnabled()) }
                                    Switch(checked = dynamicTheme, onCheckedChange = { dynamicTheme = it; session.setDynamicThemeEnabled(it) }, colors = SwitchDefaults.colors(checkedThumbColor = LocalAppColors.current.accent, checkedTrackColor = LocalAppColors.current.primary))
                                }
                                Spacer(Modifier.height(15.dp))

                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                    Text(stringResource(R.string.settings_visualizer_label), color = Color.White, fontSize = 14.sp)
                                    Switch(checked = visualizerEnabled, onCheckedChange = {
                                        if (it) {
                                            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                                                permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                            } else {
                                                visualizerEnabled = true
                                                prefs.edit().putBoolean("visualizer_enabled", true).apply()
                                            }
                                        } else {
                                            visualizerEnabled = false
                                            prefs.edit().putBoolean("visualizer_enabled", false).apply()
                                        }
                                    }, colors = SwitchDefaults.colors(checkedThumbColor = LocalAppColors.current.accent, checkedTrackColor = LocalAppColors.current.primary))
                                }
                            }
                        }
                        1 -> { // Filtres & Tri
                            item {
                                val sortRecentText = stringResource(R.string.sort_recent)
                                ExposedDropdownMenuBox(expanded = sortExpanded, onExpandedChange = { sortExpanded = it }) {
                                    OutlinedTextField(
                                        value = sortOptions[localSortMode] ?: sortRecentText,
                                        onValueChange = {}, readOnly = true, label = { Text(stringResource(R.string.settings_sort_default_label)) },
                                        modifier = Modifier.fillMaxWidth().menuAnchor(), trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sortExpanded) },
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                    )
                                    ExposedDropdownMenu(expanded = sortExpanded, onDismissRequest = { sortExpanded = false }, modifier = Modifier.background(LocalAppColors.current.panel)) {
                                        sortOptions.forEach { (key, label) -> DropdownMenuItem(text = { Text(label, color = Color.White) }, onClick = { localSortMode = key; sortExpanded = false }) }
                                    }
                                }
                                Spacer(Modifier.height(20.dp))
                                Text(stringResource(R.string.settings_hide_genres_label), color = LocalAppColors.current.textSecondary, fontSize = 14.sp)
                            }
                            items(displayGenres) { genre ->
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { localHidden = if (localHidden.contains(genre)) localHidden - genre else localHidden + genre }) {
                                    Checkbox(checked = localHidden.contains(genre), onCheckedChange = { c -> localHidden = if (c) localHidden + genre else localHidden - genre }, colors = CheckboxDefaults.colors(checkedColor = Color.Red, uncheckedColor = LocalAppColors.current.textSecondary))
                                    Text(genre, color = Color.White)
                                }
                            }
                            item {
                                Spacer(Modifier.height(10.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(
                                        value = customGenreInput, onValueChange = { customGenreInput = it },
                                        label = { Text(stringResource(R.string.settings_other_genre_placeholder), fontSize = 12.sp) },
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White), modifier = Modifier.weight(1f).height(60.dp)
                                    )
                                    IconButton(onClick = { if (customGenreInput.isNotBlank()) { localHidden = localHidden + customGenreInput; customGenreInput = "" } }) { Icon(Icons.Rounded.Add, stringResource(R.string.settings_add_genre_content_description), tint = LocalAppColors.current.accent) }
                                }
                            }
                        }
                        2 -> { // Égaliseur
                            item {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                    Text(stringResource(R.string.settings_eq_enable_label), color = Color.White, fontSize = 14.sp)
                                    Switch(checked = eqEnabled, onCheckedChange = {
                                        eqEnabled = it
                                        prefs.edit().putBoolean("eq_enabled", it).apply()
                                    }, colors = SwitchDefaults.colors(checkedThumbColor = LocalAppColors.current.accent, checkedTrackColor = LocalAppColors.current.primary))
                                }
                                Spacer(Modifier.height(15.dp))
                            }
                            if (eqBands.isNotEmpty()) {
                                items(eqBands) { band ->
                                    val freqHz = eqCenterFreqs[band] ?: 0
                                    val freqText = if (freqHz >= 1000000) stringResource(R.string.settings_eq_freq_khz, freqHz / 1000000) else stringResource(R.string.settings_eq_freq_hz, freqHz / 1000)
                                    val level = eqLevels[band] ?: 0
                                    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(freqText, color = Color.White, fontSize = 12.sp)
                                            Text(stringResource(R.string.settings_eq_level_db, level / 100), color = LocalAppColors.current.textSecondary, fontSize = 12.sp)
                                        }
                                        Slider(
                                            value = level.toFloat(),
                                            onValueChange = { newVal ->
                                                val newLevel = newVal.toInt().toShort()
                                                eqLevels = eqLevels.toMutableMap().apply { put(band, newLevel) }
                                                prefs.edit().putInt("eq_band_$band", newLevel.toInt()).apply()
                                            },
                                            valueRange = eqMinLevel.toFloat()..eqMaxLevel.toFloat(),
                                            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = LocalAppColors.current.accent, inactiveTrackColor = Color.White.copy(0.2f)),
                                            enabled = eqEnabled
                                        )
                                    }
                                }
            } else {
                                item {
                                    Text(stringResource(R.string.settings_eq_unavailable), color = LocalAppColors.current.textSecondary)
                                }
                            }
                        }
                        3 -> { // Apparence
                            item {
                                var localThemePreset by remember { mutableStateOf(currentThemePreset) }
                                var localMaterialYou by remember { mutableStateOf(currentMaterialYouEnabled) }

                                Text(stringResource(R.string.settings_theme_colors_label), color = LocalAppColors.current.textSecondary, fontSize = 14.sp)
                                Spacer(Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    ThemePreset.entries.forEach { preset ->
                                        val selected = !localMaterialYou && localThemePreset == preset.key
                                        val presetLabel = when (preset) {
                                            ThemePreset.VIOLET -> stringResource(R.string.theme_preset_violet)
                                            ThemePreset.AMOLED -> stringResource(R.string.theme_preset_amoled)
                                            ThemePreset.MIDNIGHT -> stringResource(R.string.theme_preset_midnight)
                                            ThemePreset.FOREST -> stringResource(R.string.theme_preset_forest)
                                            ThemePreset.CRIMSON -> stringResource(R.string.theme_preset_crimson)
                                        }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(CircleShape)
                                                    .background(preset.colors.primary)
                                                    .border(
                                                        width = if (selected) 3.dp else 1.dp,
                                                        color = if (selected) preset.colors.accent else Color.White.copy(alpha = 0.2f),
                                                        shape = CircleShape
                                                    )
                                                    .clickable {
                                                        localThemePreset = preset.key
                                                        localMaterialYou = false
                                                        onThemeChange(preset.key)
                                                        onMaterialYouChange(false)
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(18.dp)
                                                        .clip(CircleShape)
                                                        .background(preset.colors.accent)
                                                )
                                            }
                                            Spacer(Modifier.height(6.dp))
                                            Text(presetLabel, color = if (selected) LocalAppColors.current.accent else LocalAppColors.current.textSecondary, fontSize = 11.sp)
                                        }
                                    }
                                }

                                Spacer(Modifier.height(20.dp))
                                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                                Spacer(Modifier.height(20.dp))

                                val materialYouAvailable = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                    Column(Modifier.weight(1f)) {
                                        Text(stringResource(R.string.settings_material_you_label), color = if (materialYouAvailable) Color.White else LocalAppColors.current.textSecondary, fontSize = 14.sp)
                                        if (!materialYouAvailable) {
                                            Text(stringResource(R.string.settings_material_you_requirement), color = LocalAppColors.current.textSecondary, fontSize = 11.sp)
                                        }
                                    }
                                    Switch(
                                        checked = localMaterialYou,
                                        enabled = materialYouAvailable,
                                        onCheckedChange = { enabled ->
                                            localMaterialYou = enabled
                                            onMaterialYouChange(enabled)
                                        },
                                        colors = SwitchDefaults.colors(checkedThumbColor = LocalAppColors.current.accent, checkedTrackColor = LocalAppColors.current.primary)
                                    )
                                }

                                Spacer(Modifier.height(20.dp))
                                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                                Spacer(Modifier.height(20.dp))

                                Text(stringResource(R.string.settings_language_label), color = LocalAppColors.current.textSecondary, fontSize = 14.sp)
                                Spacer(Modifier.height(12.dp))
                                var languageExpanded by remember { mutableStateOf(false) }
                                val languageOptions = listOf(
                                    "" to stringResource(R.string.language_system),
                                    "fr" to stringResource(R.string.language_french),
                                    "en" to stringResource(R.string.language_english),
                                    "es" to stringResource(R.string.language_spanish),
                                    "de" to stringResource(R.string.language_german)
                                )
                                val currentLocales = AppCompatDelegate.getApplicationLocales()
                                val currentLanguageTag = if (currentLocales.isEmpty) "" else (currentLocales[0]?.language ?: "")
                                val currentLanguageLabel = languageOptions.find { it.first == currentLanguageTag }?.second ?: languageOptions[0].second
                                ExposedDropdownMenuBox(expanded = languageExpanded, onExpandedChange = { languageExpanded = it }) {
                                    OutlinedTextField(
                                        value = currentLanguageLabel,
                                        onValueChange = {}, readOnly = true,
                                        modifier = Modifier.fillMaxWidth().menuAnchor(), trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = languageExpanded) },
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                    )
                                    ExposedDropdownMenu(expanded = languageExpanded, onDismissRequest = { languageExpanded = false }, modifier = Modifier.background(LocalAppColors.current.panel)) {
                                        languageOptions.forEach { (tag, label) ->
                                            DropdownMenuItem(text = { Text(label, color = Color.White) }, onClick = {
                                                languageExpanded = false
                                                val localeList = if (tag.isEmpty()) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag)
                                                AppCompatDelegate.setApplicationLocales(localeList)
                                            })
                                        }
                                    }
                                }
                            }
                        }
                        4 -> { // Avancé / Opti
                            item {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                    Text(stringResource(R.string.settings_cover_cache_label), color = Color.White, fontSize = 14.sp)
                                    Switch(checked = coverCacheEnabled, onCheckedChange = { coverCacheEnabled = it; session.setCoverCacheEnabled(it) }, colors = SwitchDefaults.colors(checkedThumbColor = LocalAppColors.current.accent, checkedTrackColor = LocalAppColors.current.primary))
                                }
                                Spacer(Modifier.height(10.dp))
                                Button(onClick = { context.imageLoader.diskCache?.clear(); context.imageLoader.memoryCache?.clear(); Toast.makeText(context, context.getString(R.string.settings_cache_cleared_toast), Toast.LENGTH_SHORT).show() }, colors = ButtonDefaults.buttonColors(containerColor = LocalAppColors.current.background), modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.settings_clear_cache_button), color = LocalAppColors.current.accent) }
                                Spacer(Modifier.height(10.dp))
                                Button(onClick = onRedoTutorial, colors = ButtonDefaults.buttonColors(containerColor = LocalAppColors.current.background), modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.settings_redo_tutorial_button), color = LocalAppColors.current.accent) }
                            }
                        }
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { onSave(localHidden, localSortMode); onDismiss() }) { Text(stringResource(R.string.settings_close_button), color = LocalAppColors.current.accent) }
                }
            }
        }
    }
}
