package com.randomfilm.purplemusic20.ui.theme

import android.content.Context
import android.os.Build
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Token set for the app's custom (non-Material) chrome: screen backgrounds, panels,
 * primary/accent purple-ish tints, secondary text and the nav bar. Kept separate from
 * MaterialTheme.colorScheme because most of the UI was built against these flat tokens
 * directly rather than Material's semantic roles.
 */
data class AppColors(
    val background: Color,
    val panel: Color,
    val primary: Color,
    val accent: Color,
    val textSecondary: Color,
    val navBg: Color,
)

/** Default matches the Violet preset so any composable read outside a provider still looks right. */
val LocalAppColors = staticCompositionLocalOf {
    AppColors(
        background = BgDark,
        panel = BgPanel,
        primary = PrimaryPurple,
        accent = AccentPurple,
        textSecondary = TextGray,
        navBg = NavBg,
    )
}

/** Named, selectable color presets for the app's custom theme tokens. */
enum class ThemePreset(val key: String, val label: String, val colors: AppColors) {
    VIOLET(
        key = "violet",
        label = "Violet",
        colors = AppColors(
            background = BgDark,
            panel = BgPanel,
            primary = PrimaryPurple,
            accent = AccentPurple,
            textSecondary = TextGray,
            navBg = NavBg,
        )
    ),
    AMOLED(
        key = "amoled",
        label = "AMOLED",
        colors = AppColors(
            background = Color(0xFF000000),
            panel = Color(0xFF0A0A0A),
            primary = Color(0xFF7B2CBF),
            accent = Color(0xFFB388FF),
            textSecondary = Color(0xFF9E9E9E),
            navBg = Color(0xFF000000),
        )
    ),
    MIDNIGHT(
        key = "midnight",
        label = "Minuit",
        colors = AppColors(
            background = Color(0xFF0B1120),
            panel = Color(0xFF161F35),
            primary = Color(0xFF3B5BDB),
            accent = Color(0xFF7C9BFF),
            textSecondary = Color(0xFF8D97B8),
            navBg = Color(0xFF0E1526),
        )
    ),
    FOREST(
        key = "forest",
        label = "Forêt",
        colors = AppColors(
            background = Color(0xFF0D1811),
            panel = Color(0xFF16261C),
            primary = Color(0xFF2E7D4F),
            accent = Color(0xFF6FCF97),
            textSecondary = Color(0xFF8FA396),
            navBg = Color(0xFF0F1D14),
        )
    ),
    CRIMSON(
        key = "crimson",
        label = "Cramoisi",
        colors = AppColors(
            background = Color(0xFF1A0E0E),
            panel = Color(0xFF2A1616),
            primary = Color(0xFFB33A3A),
            accent = Color(0xFFFF6B6B),
            textSecondary = Color(0xFFB09090),
            navBg = Color(0xFF1E1010),
        )
    );

    companion object {
        fun fromKey(key: String): ThemePreset = entries.find { it.key == key } ?: VIOLET
    }
}

/**
 * Maps the Android 12+ Material You dynamic color scheme (derived from the user's wallpaper)
 * onto our [AppColors] tokens. Only meaningful on API 31+; callers must gate on
 * Build.VERSION.SDK_INT >= Build.VERSION_CODES.S before invoking this, otherwise the Violet
 * preset is returned as a safe fallback.
 */
fun materialYouAppColors(context: Context, dark: Boolean = true): AppColors {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return ThemePreset.VIOLET.colors
    val scheme = if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    return AppColors(
        background = scheme.background,
        panel = scheme.surfaceVariant,
        primary = scheme.primary,
        accent = scheme.tertiary,
        textSecondary = scheme.onSurfaceVariant,
        navBg = scheme.surfaceContainer,
    )
}
