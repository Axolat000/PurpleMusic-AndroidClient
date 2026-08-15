package com.randomfilm.purplemusic20.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.randomfilm.purplemusic20.R
import com.randomfilm.purplemusic20.data.*
import com.randomfilm.purplemusic20.ui.theme.*
import com.randomfilm.purplemusic20.util.TutorialAudioPlayer

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
    var audioMuted by remember { mutableStateOf(prefs.getBoolean("tutorial_audio_muted", false)) }

    val context = LocalContext.current
    val audioPlayer = remember { TutorialAudioPlayer(context) }
    DisposableEffect(Unit) {
        audioPlayer.init()
        audioPlayer.startBgm(if (audioMuted) 0f else session.getVolume())
        onDispose { audioPlayer.release() }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        visualizer = granted
        prefs.edit().putBoolean("visualizer_enabled", granted).apply()
        if (!granted) {
            Toast.makeText(context, context.getString(R.string.settings_visualizer_permission_denied), Toast.LENGTH_SHORT).show()
        }
    }

    Box(Modifier.fillMaxSize()) {
    Column(
        Modifier.fillMaxSize().background(LocalAppColors.current.background).padding(24.dp),
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
                            ) { audioPlayer.playStepSfx(); step++ },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(stringResource(R.string.tutorial_welcome_greeting), color = LocalAppColors.current.textSecondary, fontSize = 24.sp)
                            Text(stringResource(R.string.app_brand_name_full), color = LocalAppColors.current.accent, fontSize = 40.sp, fontWeight = FontWeight.Black)
                            Spacer(Modifier.height(64.dp))
                            Text(stringResource(R.string.tutorial_click_to_start), color = Color.White.copy(alpha = alpha), fontSize = 18.sp, modifier = Modifier.scale(scale))
                        }
                    }
                    1 -> {
                        Text(stringResource(R.string.tutorial_step_theme_title), color = LocalAppColors.current.accent, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        Text(stringResource(R.string.tutorial_step_theme_desc), color = Color.White, textAlign = TextAlign.Center, fontSize = 16.sp)
                        Spacer(Modifier.height(32.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.tutorial_step_theme_switch_label), color = Color.White, modifier = Modifier.weight(1f))
                            Switch(
                                checked = dynamicTheme,
                                onCheckedChange = { dynamicTheme = it; session.setDynamicThemeEnabled(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = LocalAppColors.current.accent, checkedTrackColor = LocalAppColors.current.primary)
                            )
                        }
                    }
                    2 -> {
                        Text(stringResource(R.string.tutorial_step_visualizer_title), color = LocalAppColors.current.accent, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        Text(stringResource(R.string.tutorial_step_visualizer_desc), color = Color.White, textAlign = TextAlign.Center, fontSize = 16.sp)
                        Spacer(Modifier.height(32.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.tutorial_step_visualizer_switch_label), color = Color.White, modifier = Modifier.weight(1f))
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
                                colors = SwitchDefaults.colors(checkedThumbColor = LocalAppColors.current.accent, checkedTrackColor = LocalAppColors.current.primary)
                            )
                        }
                    }
                    3 -> {
                        Text(stringResource(R.string.tutorial_step_sort_title), color = LocalAppColors.current.accent, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        Text(stringResource(R.string.tutorial_step_sort_desc), color = Color.White, textAlign = TextAlign.Center, fontSize = 16.sp)
                        Spacer(Modifier.height(32.dp))

                        val sortOptions = mapOf(
                            "popular" to stringResource(R.string.sort_popular),
                            "date_desc" to stringResource(R.string.sort_recent),
                            "date_asc" to stringResource(R.string.sort_oldest),
                            "alpha_asc" to stringResource(R.string.sort_name_asc),
                            "alpha_desc" to stringResource(R.string.sort_name_desc),
                            "artist" to stringResource(R.string.sort_by_artist)
                        )
                        val sortRecentText = stringResource(R.string.sort_recent)

                        @OptIn(ExperimentalMaterial3Api::class)
                        ExposedDropdownMenuBox(expanded = sortExpanded, onExpandedChange = { sortExpanded = it }) {
                            OutlinedTextField(
                                value = sortOptions[sortMode] ?: sortRecentText,
                                onValueChange = {}, readOnly = true, label = { Text(stringResource(R.string.settings_sort_default_label)) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(), trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sortExpanded) },
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )
                            ExposedDropdownMenu(expanded = sortExpanded, onDismissRequest = { sortExpanded = false }, modifier = Modifier.background(LocalAppColors.current.panel)) {
                                sortOptions.forEach { (key, label) -> DropdownMenuItem(text = { Text(label, color = Color.White) }, onClick = { sortMode = key; session.saveSortMode(key); sortExpanded = false }) }
                            }
                        }
                    }
                    4 -> {
                        Text(stringResource(R.string.tutorial_step_eq_title), color = LocalAppColors.current.accent, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        Text(stringResource(R.string.tutorial_step_eq_desc), color = Color.White, textAlign = TextAlign.Center, fontSize = 16.sp)
                        Spacer(Modifier.height(32.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.settings_eq_enable_label), color = Color.White, modifier = Modifier.weight(1f))
                            Switch(
                                checked = eqEnabled,
                                onCheckedChange = { eqEnabled = it; prefs.edit().putBoolean("eq_enabled", it).apply() },
                                colors = SwitchDefaults.colors(checkedThumbColor = LocalAppColors.current.accent, checkedTrackColor = LocalAppColors.current.primary)
                            )
                        }
                    }
                    5 -> {
                        Text(stringResource(R.string.tutorial_step_cache_title), color = LocalAppColors.current.accent, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        Text(stringResource(R.string.tutorial_step_cache_desc), color = Color.White, textAlign = TextAlign.Center, fontSize = 16.sp)
                        Spacer(Modifier.height(32.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.tutorial_step_cache_switch_label), color = Color.White, modifier = Modifier.weight(1f))
                            Switch(
                                checked = coverCache,
                                onCheckedChange = { coverCache = it; session.setCoverCacheEnabled(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = LocalAppColors.current.accent, checkedTrackColor = LocalAppColors.current.primary)
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
                TextButton(onClick = { audioPlayer.playStepSfx(); step-- }) {
                    Text(stringResource(R.string.tutorial_nav_back), color = LocalAppColors.current.textSecondary)
                }

                Row(horizontalArrangement = Arrangement.Center) {
                    for (i in 1 until totalSteps) {
                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (i == step) LocalAppColors.current.accent else LocalAppColors.current.textSecondary.copy(alpha = 0.5f))
                        )
                    }
                }

                if (step < totalSteps - 1) {
                    TextButton(onClick = { audioPlayer.playStepSfx(); step++ }) {
                        Text(stringResource(R.string.tutorial_nav_next), color = LocalAppColors.current.accent, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = {
                            prefs.edit().putBoolean("tutorial_v2_completed", true).apply()
                            onFinish()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LocalAppColors.current.primary)
                    ) {
                        Text(stringResource(R.string.tutorial_nav_finish), color = Color.White)
                    }
                }
            }
        }
    }

    IconButton(
        onClick = {
            audioMuted = !audioMuted
            prefs.edit().putBoolean("tutorial_audio_muted", audioMuted).apply()
            audioPlayer.setVolume(if (audioMuted) 0f else session.getVolume())
        },
        modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
    ) {
        Icon(
            if (audioMuted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
            contentDescription = stringResource(if (audioMuted) R.string.tutorial_audio_unmute else R.string.tutorial_audio_mute),
            tint = LocalAppColors.current.textSecondary
        )
    }
    }
}
