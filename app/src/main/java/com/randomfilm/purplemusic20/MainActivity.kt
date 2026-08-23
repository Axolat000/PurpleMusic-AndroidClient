package com.randomfilm.purplemusic20

import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.randomfilm.purplemusic20.data.SessionManager
import com.randomfilm.purplemusic20.ui.MainApp
import com.randomfilm.purplemusic20.ui.theme.LocalAppColors
import com.randomfilm.purplemusic20.ui.theme.PurpleMusic20Theme
import com.randomfilm.purplemusic20.ui.theme.ThemePreset
import com.randomfilm.purplemusic20.ui.theme.ThemeUtils
import com.randomfilm.purplemusic20.ui.theme.materialYouAppColors

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            val session = remember { SessionManager(context) }
            var themePreset by remember { mutableStateOf(session.getThemePreset()) }
            var materialYouEnabled by remember { mutableStateOf(session.isMaterialYouEnabled()) }
            var customThemeBaseColor by remember { mutableStateOf(session.getCustomThemeBaseColor()) }

            val resolvedColors = remember(themePreset, materialYouEnabled, customThemeBaseColor) {
                if (materialYouEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    materialYouAppColors(context)
                } else if (themePreset == "custom") {
                    ThemeUtils.generateAppColors(Color(customThemeBaseColor))
                } else {
                    ThemePreset.fromKey(themePreset).colors
                }
            }

            PurpleMusic20Theme {
                CompositionLocalProvider(LocalAppColors provides resolvedColors) {
                    Surface(color = resolvedColors.background, modifier = Modifier.fillMaxSize()) {
                        MainApp(
                            currentThemePreset = themePreset,
                            currentMaterialYouEnabled = materialYouEnabled,
                            currentCustomThemeBaseColor = customThemeBaseColor,
                            onThemeChange = { preset ->
                                themePreset = preset
                                session.saveThemePreset(preset)
                            },
                            onMaterialYouChange = { enabled ->
                                materialYouEnabled = enabled
                                session.setMaterialYouEnabled(enabled)
                            },
                            onCustomThemeBaseColorChange = { argb ->
                                customThemeBaseColor = argb
                                session.saveCustomThemeBaseColor(argb)
                            }
                        )
                    }
                }
            }
        }
    }
}
