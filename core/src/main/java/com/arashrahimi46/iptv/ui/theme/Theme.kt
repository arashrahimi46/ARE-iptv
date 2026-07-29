package com.arashrahimi46.iptv.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf

val LocalAreIptvColors = staticCompositionLocalOf { AreIptvDarkColors }
val LocalAreIptvTypography = staticCompositionLocalOf { AreIptvTypographyDefault }
val LocalAreIptvSpacing = staticCompositionLocalOf { AreIptvSpacingDefault }
val LocalAreIptvRadius = staticCompositionLocalOf { AreIptvRadiusDefault }
/** Effective dark/light state after resolving ThemeMode.SYSTEM against the device. Read this
 * (via [AreIptvTheme.isDark]) instead of the stored theme pref when a component needs to know which
 * mode is actually on screen (e.g. the per-mode accent picker). */
val LocalThemeIsDark = staticCompositionLocalOf { true }
