package com.randomfilm.purplemusic20.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils

/**
 * Derives a full [AppColors] token set from a single base color -- the base becomes [AppColors.background],
 * everything else (panel/primary/accent/text/navBg) is computed via HSL offsets around its hue. Used by the
 * app-wide dynamic theme (Settings > Appearance, see MainApp.kt), which feeds it a color extracted from the
 * current track's cover art. Mirrors the equivalent generateThemeFromBaseColor() in the web app's
 * js/theme.js, adapted to this app's smaller 6-token AppColors (no separate border/muted-text distinction
 * there).
 */
object ThemeUtils {
    fun isLight(color: Color): Boolean = color.luminance() > 0.5f

    /** True when [color]'s RGB channels are close enough together to read as grayscale. HSL saturation is
     * unreliable for this near the lightness extremes -- its denominator shrinks toward 0 or 1, so a few
     * units of compression noise in an otherwise black/white color can read back as high saturation
     * despite looking neutral. Raw channel spread doesn't have that distortion. */
    private fun isAchromatic(color: Color, threshold: Float = 0.08f): Boolean {
        val spread = maxOf(color.red, color.green, color.blue) - minOf(color.red, color.green, color.blue)
        return spread < threshold
    }

    fun deriveAccent(base: Color): Color {
        if (base.toArgb() == 0xFF000000.toInt()) return Color.White
        if (base.toArgb() == 0xFFFFFFFF.toInt()) return PrimaryPurple

        val isLight = isLight(base)
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(base.toArgb(), hsl)
        val wasAchromatic = isAchromatic(base)

        if (isLight) {
            hsl[1] = (hsl[1] + 0.5f).coerceIn(0.6f, 1.0f)
            hsl[2] = (hsl[2] - 0.4f).coerceIn(0.3f, 0.5f)
        } else {
            hsl[1] = (hsl[1] + 0.4f).coerceIn(0.5f, 0.9f)
            hsl[2] = (hsl[2] + 0.5f).coerceIn(0.6f, 0.85f)
        }

        if (wasAchromatic) {
            hsl[0] = 270f
            hsl[1] = 0.6f
            if (isLight) hsl[2] = 0.4f
        }

        return Color(ColorUtils.HSLToColor(hsl))
    }

    /** Less vivid than [deriveAccent] -- used as button/highlight backgrounds rather than text/icon tints. */
    fun derivePrimary(base: Color): Color {
        val isLight = isLight(base)
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(base.toArgb(), hsl)
        if (isAchromatic(base)) hsl[0] = 270f
        hsl[1] = (hsl[1] + 0.2f).coerceIn(0.35f, 0.7f)
        hsl[2] = if (isLight) 0.4f else 0.48f
        return Color(ColorUtils.HSLToColor(hsl))
    }

    fun derivePanel(base: Color): Color {
        if (base.toArgb() == 0xFFFFFFFF.toInt()) return Color(0xFFF2F2F2)
        val isLight = isLight(base)
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(base.toArgb(), hsl)
        hsl[2] = if (isLight) (hsl[2] - 0.05f).coerceIn(0f, 1f) else (hsl[2] + 0.05f).coerceIn(0f, 1f)
        return Color(ColorUtils.HSLToColor(hsl))
    }

    /** Slightly more pronounced offset than [derivePanel] -- distinguishes the nav bar from regular panels. */
    fun deriveNavBg(base: Color): Color {
        val isLight = isLight(base)
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(base.toArgb(), hsl)
        hsl[2] = if (isLight) (hsl[2] - 0.08f).coerceIn(0f, 1f) else (hsl[2] + 0.03f).coerceIn(0f, 1f)
        return Color(ColorUtils.HSLToColor(hsl))
    }

    fun deriveTextSecondary(base: Color): Color {
        val isLight = isLight(base)
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(base.toArgb(), hsl)
        hsl[1] = (hsl[1] * 0.5f).coerceIn(0f, 1f)
        hsl[2] = if (isLight) 0.4f else 0.7f
        return Color(ColorUtils.HSLToColor(hsl))
    }

    /** Full [AppColors] token set derived from [base] -- see class doc. */
    fun generateAppColors(base: Color): AppColors = AppColors(
        background = base,
        panel = derivePanel(base),
        primary = derivePrimary(base),
        accent = deriveAccent(base),
        textSecondary = deriveTextSecondary(base),
        navBg = deriveNavBg(base)
    )
}
