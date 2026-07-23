package com.arashrahimi46.iptv.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import androidx.tv.material3.lightColorScheme

val LocalAreIptvColors = staticCompositionLocalOf { AreIptvDarkColors }
val LocalAreIptvTypography = staticCompositionLocalOf { AreIptvTypographyDefault }
val LocalAreIptvSpacing = staticCompositionLocalOf { AreIptvSpacingDefault }
val LocalAreIptvRadius = staticCompositionLocalOf { AreIptvRadiusDefault }

/**
 * Design-system token accessors, analogous to `MaterialTheme.colorScheme` /
 * `MaterialTheme.typography`. Always read tokens through this object (or the
 * underlying CompositionLocals) rather than hardcoding values.
 */
object AreIptvTheme {
    val colors: AreIptvColors
        @Composable get() = LocalAreIptvColors.current

    val typography: AreIptvTypography
        @Composable get() = LocalAreIptvTypography.current

    val spacing: AreIptvSpacing
        @Composable get() = LocalAreIptvSpacing.current

    val radius: AreIptvRadius
        @Composable get() = LocalAreIptvRadius.current

    val motion: AreIptvMotion
        @Composable get() = LocalAreIptvMotion.current
}

/**
 * Root theme composable for the ARE iptv TV app. Wraps `androidx.tv.material3.MaterialTheme`
 * for base TV component compatibility (Card, Button, etc. focus/scale defaults) while
 * exposing the custom design-token layer via [AreIptvTheme].
 *
 * @param isDark false switches every color token to the light ramp (single theme
 *   object, not a build variant — see tokens/colors.css `[data-theme="light"]`).
 * @param accent the user-selected brand accent, overlaid onto the neutral base palette.
 *   Defaults to [AccentPreset.BLUE], which reproduces the original hardwired accent exactly.
 *   Backed by the real Settings accent picker (per-mode: dark and light are chosen
 *   independently, resolved at the composition root in MainActivity).
 * @param reducedMotion drives [LocalReducedMotion] and collapses [AreIptvTheme.motion]
 *   to the reduced-motion token set. Backed by the real Settings "Reduce motion" toggle
 *   since Phase 4 (via [com.arashrahimi46.iptv.data.settings.UserSettings.isReducedMotion],
 *   read at the composition root in MainActivity) -- this doc-comment was stale.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AreIptvTheme(
    isDark: Boolean = true,
    accent: AccentPreset = AccentPreset.BLUE,
    reducedMotion: Boolean = false,
    content: @Composable () -> Unit,
) {
    val base = if (isDark) AreIptvDarkColors else AreIptvLightColors
    val colors = base.withAccent(accent.tokens(isDark))
    val motion = if (reducedMotion) AreIptvMotionReduced else AreIptvMotionDefault

    val tvColorScheme = if (isDark) {
        darkColorScheme(
            primary = colors.accent,
            onPrimary = colors.accentFg,
            secondary = colors.surface2,
            onSecondary = colors.textPrimary,
            background = colors.bgBase,
            onBackground = colors.textPrimary,
            surface = colors.surface1,
            onSurface = colors.textPrimary,
            surfaceVariant = colors.surface2,
            onSurfaceVariant = colors.textSecondary,
            border = colors.borderDefault,
            borderVariant = colors.borderSubtle,
            error = colors.danger,
        )
    } else {
        lightColorScheme(
            primary = colors.accent,
            onPrimary = colors.accentFg,
            secondary = colors.surface2,
            onSecondary = colors.textPrimary,
            background = colors.bgBase,
            onBackground = colors.textPrimary,
            surface = colors.surface1,
            onSurface = colors.textPrimary,
            surfaceVariant = colors.surface2,
            onSurfaceVariant = colors.textSecondary,
            border = colors.borderDefault,
            borderVariant = colors.borderSubtle,
            error = colors.danger,
        )
    }

    CompositionLocalProvider(
        LocalAreIptvColors provides colors,
        LocalAreIptvTypography provides AreIptvTypographyDefault,
        LocalAreIptvSpacing provides AreIptvSpacingDefault,
        LocalAreIptvRadius provides AreIptvRadiusDefault,
        LocalAreIptvMotion provides motion,
        LocalReducedMotion provides reducedMotion,
    ) {
        MaterialTheme(
            colorScheme = tvColorScheme,
            content = content,
        )
    }
}
