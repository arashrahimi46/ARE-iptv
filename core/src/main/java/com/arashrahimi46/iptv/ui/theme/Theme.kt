package com.arashrahimi46.iptv.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf

// NOTE: [AreIptvTypography] (Type.kt) stays in :tv -- it reads font resources (R.font.*) that
// live in :tv's res/font, which :core has no equivalent of. LocalAreIptvTypography and the
// AreIptvTheme accessor object therefore stay in tv/.../ui/theme/Theme.kt alongside it; this file
// carries every other shared token CompositionLocal.

val LocalAreIptvColors = staticCompositionLocalOf { AreIptvDarkColors }
val LocalAreIptvSpacing = staticCompositionLocalOf { AreIptvSpacingDefault }
val LocalAreIptvRadius = staticCompositionLocalOf { AreIptvRadiusDefault }
/** Effective dark/light state after resolving ThemeMode.SYSTEM against the device. Read this
 * (via [AreIptvTheme.isDark]) instead of the stored theme pref when a component needs to know which
 * mode is actually on screen (e.g. the per-mode accent picker). */
val LocalThemeIsDark = staticCompositionLocalOf { true }
