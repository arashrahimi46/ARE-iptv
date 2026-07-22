package com.arashrahimi46.iptv.ui.theme

import androidx.compose.ui.graphics.Color

// ---- Brand: electric blue ----
val Blue300 = Color(0xFF93C5FD)
val Blue400 = Color(0xFF60A5FA)
val Blue500 = Color(0xFF3B82F6)
val Blue600 = Color(0xFF2563EB)
val Blue700 = Color(0xFF1D4ED8)
val BlueGlow = Color(0x8C3B82F6) // rgba(59,130,246,0.55)
val BlueWash = Color(0x243B82F6) // rgba(59,130,246,0.14)

// ---- Neutral ramp ----
val Ink950 = Color(0xFF06070A)
val Ink900 = Color(0xFF0A0B0F)
val Ink850 = Color(0xFF0E1015)
val Ink800 = Color(0xFF14161C)
val Ink750 = Color(0xFF191C23)
val Ink700 = Color(0xFF1F232C)
val Ink600 = Color(0xFF262A34)
val Ink500 = Color(0xFF333844)
val Ink400 = Color(0xFF4B515F)
val Ink300 = Color(0xFF6B7280)
// P0.3 (WCAG 1.4.1/contrast): Ink300 at caption sizes on the dark surfaces textTertiary
// renders over (surface1/surface2) measured 2.97-4.13:1 -- below the 4.5:1 AA minimum.
// Ink250 is a dedicated, slightly lighter step for TEXT use specifically (Ink300 itself is
// left as-is in case something else in the ramp depends on it) that measures ~5:1 against
// those same surfaces.
val Ink250 = Color(0xFF8C92A3)
val Ink200 = Color(0xFF9AA2B1)
val Ink100 = Color(0xFFC4CAD4)
val Ink050 = Color(0xFFE7EAF0)
val White = Color(0xFFF6F8FC)

// ---- Status ramps ----
val Green500 = Color(0xFF22C55E)
val Green400 = Color(0xFF4ADE80)
val Amber500 = Color(0xFFEAB308)
val Amber400 = Color(0xFFFACC15)
val Red500 = Color(0xFFEF4444)
val Red400 = Color(0xFFF87171)
val Violet500 = Color(0xFF8B5CF6)
val Violet400 = Color(0xFFA78BFA)

// ---- Light theme raw values ----
val LightBgSunken = Color(0xFFE9EDF3)
val LightBgBase = Color(0xFFF3F5F9)
val LightSurface1 = Color(0xFFFFFFFF)
val LightSurface2 = Color(0xFFF7F9FC)
val LightSurface3 = Color(0xFFEEF1F6)
val LightTextPrimary = Color(0xFF0D1017)
val LightTextSecondary = Color(0xFF4A5163)
// P0.3: was #757D8E (4.13:1 on LightSurface1/white -- below the 4.5:1 AA minimum at
// caption sizes). Darkened to measure ~4.8:1.
val LightTextTertiary = Color(0xFF6B7280)
val LightTextDisabled = Color(0xFFA5ABB8)
val LightBorderSubtle = Color(0x120F141E) // rgba(15,20,30,0.07)
val LightBorderDefault = Color(0x1F0F141E) // rgba(15,20,30,0.12)
val LightBorderStrong = Color(0x330F141E) // rgba(15,20,30,0.20)
val LightAccentWash = Color(0x1A2563EB) // rgba(37,99,235,0.10)

/**
 * Full semantic color palette for one theme mode (dark or light), mirroring
 * the CSS semantic aliases in tokens/colors.css. Reference these, never the
 * raw ramps above, from component code.
 */
data class AreIptvColors(
    val isDark: Boolean,
    val bgSunken: Color,
    val bgBase: Color,
    val surface1: Color,
    val surface2: Color,
    val surface3: Color,
    val surfaceOverlay: Color,
    val surfaceGlass: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textDisabled: Color,
    val textOnAccent: Color,
    val borderSubtle: Color,
    val borderDefault: Color,
    val borderStrong: Color,
    val accent: Color,
    val accentHover: Color,
    val accentPress: Color,
    val accentWash: Color,
    val accentFg: Color,
    val focusRing: Color,
    val success: Color = Green500,
    val warning: Color = Amber500,
    val danger: Color = Red500,
    val live: Color = Red500,
    val smart: Color = Violet500,
    val healthStable: Color = Green500,
    val healthModerate: Color = Amber500,
    val healthPoor: Color = Red500,
    val violetText: Color,
    // The -400 (lighter) tint of each status hue, for text/icon use over the -500 solid
    // indicator dots above -- these were being reached via raw hex literals at call sites
    // (PosterTile's rating star, LiveScreen's "ON AIR NOW" label, Badge's catch-up text)
    // instead of through the theme, same class of violation as everything else in this pass.
    val ratingStar: Color = Amber400,
    val onAirText: Color = Red400,
    val catchupText: Color = Green400,
    // Channel-logo "well": kept a dark neutral in BOTH themes because provider logos are
    // overwhelmingly white/light PNGs designed for dark backgrounds -- a light-theme white
    // well (surfaceOverlay = 86% white) made them vanish. logoWellText is the initials
    // fallback colour, which must read on that dark well.
    val logoWell: Color = Ink800,
    val logoWellText: Color = White,
)

val AreIptvDarkColors = AreIptvColors(
    isDark = true,
    bgSunken = Ink950,
    bgBase = Ink900,
    surface1 = Ink800,
    surface2 = Ink700,
    surface3 = Ink600,
    surfaceOverlay = Color(0xD10E1015), // rgba(14,16,21,0.82)
    surfaceGlass = Color(0x8C1E222C), // rgba(30,34,44,0.55)
    textPrimary = White,
    textSecondary = Ink200,
    textTertiary = Ink250,
    textDisabled = Ink400,
    textOnAccent = Color(0xFFFFFFFF),
    borderSubtle = Color(0x0FFFFFFF),
    borderDefault = Color(0x1AFFFFFF),
    borderStrong = Color(0x2EFFFFFF),
    accent = Blue500,
    accentHover = Blue400,
    accentPress = Blue600,
    accentWash = BlueWash,
    accentFg = Color(0xFFFFFFFF),
    focusRing = Blue400,
    violetText = Violet400,
)

val AreIptvLightColors = AreIptvColors(
    isDark = false,
    bgSunken = LightBgSunken,
    bgBase = LightBgBase,
    surface1 = LightSurface1,
    surface2 = LightSurface2,
    surface3 = LightSurface3,
    surfaceOverlay = Color(0xDBFFFFFF), // rgba(255,255,255,0.86)
    surfaceGlass = Color(0x99FFFFFF), // rgba(255,255,255,0.6)
    textPrimary = LightTextPrimary,
    textSecondary = LightTextSecondary,
    textTertiary = LightTextTertiary,
    textDisabled = LightTextDisabled,
    textOnAccent = Color(0xFFFFFFFF),
    borderSubtle = LightBorderSubtle,
    borderDefault = LightBorderDefault,
    borderStrong = LightBorderStrong,
    accent = Blue600,
    accentHover = Blue500,
    accentPress = Blue700,
    accentWash = LightAccentWash,
    accentFg = Color(0xFFFFFFFF),
    focusRing = Blue600,
    violetText = Violet400,
)
