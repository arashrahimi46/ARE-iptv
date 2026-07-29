package com.arashrahimi46.iptv.mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.arashrahimi46.iptv.ui.theme.AreIptvColors
import com.arashrahimi46.iptv.ui.theme.AreIptvDarkColors
import com.arashrahimi46.iptv.ui.theme.AreIptvLightColors

/**
 * Phone theming, wired to the SAME design-system tokens as :tv
 * ([com.arashrahimi46.iptv.ui.theme.AreIptvColors]/[AreIptvTypography]) so the two apps read as one
 * product, on top of standard `androidx.compose.material3` (NOT `androidx.tv.material3` -- :tv's
 * [com.arashrahimi46.iptv.ui.theme.AreIptvTheme] is D-pad/TV-material specific and wrong for touch).
 */
val LocalAreIptvColors = staticCompositionLocalOf { AreIptvDarkColors }
val LocalAreIptvTypography = staticCompositionLocalOf { AreIptvTypographyDefault }

object AreIptvMobileTheme {
    val colors: AreIptvColors
        @Composable get() = LocalAreIptvColors.current

    val typography: AreIptvTypography
        @Composable get() = LocalAreIptvTypography.current
}

@Composable
fun AreIptvMobileTheme(
    isDark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (isDark) AreIptvDarkColors else AreIptvLightColors
    val scheme = if (isDark) {
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
            outline = colors.borderDefault,
            outlineVariant = colors.borderSubtle,
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
            outline = colors.borderDefault,
            outlineVariant = colors.borderSubtle,
            error = colors.danger,
        )
    }

    CompositionLocalProvider(
        LocalAreIptvColors provides colors,
        LocalAreIptvTypography provides AreIptvTypographyDefault,
    ) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
