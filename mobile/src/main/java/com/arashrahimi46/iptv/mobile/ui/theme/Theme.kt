package com.arashrahimi46.iptv.mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.arashrahimi46.iptv.ui.interaction.AreInteractiveBinding
import com.arashrahimi46.iptv.ui.interaction.AreInteractiveSurface
import com.arashrahimi46.iptv.ui.theme.AreIptvColors
import com.arashrahimi46.iptv.ui.theme.AreIptvDarkColors
import com.arashrahimi46.iptv.ui.theme.AreIptvLightColors
import com.arashrahimi46.iptv.ui.theme.AreIptvMotionDefault
import com.arashrahimi46.iptv.ui.theme.AreIptvRadiusDefault
import com.arashrahimi46.iptv.ui.theme.AreIptvSpacingDefault
import com.arashrahimi46.iptv.ui.theme.LocalAreInteractiveBinding
import com.arashrahimi46.iptv.ui.theme.LocalAreIptvColors as CoreLocalAreIptvColors
import com.arashrahimi46.iptv.ui.theme.LocalAreIptvMotion as CoreLocalAreIptvMotion
import com.arashrahimi46.iptv.ui.theme.LocalAreIptvRadius as CoreLocalAreIptvRadius
import com.arashrahimi46.iptv.ui.theme.LocalAreIptvSpacing as CoreLocalAreIptvSpacing
import com.arashrahimi46.iptv.ui.theme.LocalAreIptvTypography as CoreLocalAreIptvTypography
import com.arashrahimi46.iptv.ui.theme.LocalThemeIsDark as CoreLocalThemeIsDark
import com.arashrahimi46.iptv.ui.theme.AreIptvTypographyDefault as CoreAreIptvTypographyDefault
import com.arashrahimi46.iptv.ui.theme.AreIptvTypographyVazir as CoreAreIptvTypographyVazir

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

    // RTL locales (fa/ar) have no glyphs in the Latin brand fonts -- swap the whole type scale to
    // Vazirmatn, mirroring :tv's AreIptvTheme. LocalLayoutDirection is already RTL here, derived
    // from the per-app locale.
    val typography = if (LocalLayoutDirection.current == LayoutDirection.Rtl) {
        AreIptvTypographyVazir
    } else {
        AreIptvTypographyDefault
    }

    CompositionLocalProvider(
        LocalAreIptvColors provides colors,
        LocalAreIptvTypography provides typography,
        LocalAreInteractiveBinding provides mobileAreInteractiveBinding,
        // Bridges :core's OWN CompositionLocals (a distinct set from the two above, despite the
        // matching names) so components imported straight from :core -- AreButton, AreTabs,
        // controlSkin, glassSurface -- resolve the real per-session theme instead of silently
        // falling back to core Theme.kt's defaults (AreIptvDarkColors, always, regardless of
        // [isDark]). Typography can't reuse the `typography` val above -- :mobile's AreIptvTypography
        // (Type.kt in this package) is a deliberately separate data class from :core's identically
        // named one (own duplicated font families), so :core components need :core's own instance,
        // picked by the same RTL rule. :mobile has no per-platform spacing/radius/motion of its own,
        // so those three just get :core's shared default token sets -- the same objects :tv uses at rest.
        CoreLocalAreIptvColors provides colors,
        CoreLocalAreIptvTypography provides (
            if (LocalLayoutDirection.current == LayoutDirection.Rtl) CoreAreIptvTypographyVazir
            else CoreAreIptvTypographyDefault
        ),
        CoreLocalAreIptvSpacing provides AreIptvSpacingDefault,
        CoreLocalAreIptvRadius provides AreIptvRadiusDefault,
        CoreLocalAreIptvMotion provides AreIptvMotionDefault,
        CoreLocalThemeIsDark provides isDark,
    ) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}

/**
 * [LocalAreInteractiveBinding] implementation for `:mobile`: a plain passthrough to
 * [AreInteractiveSurface] -- a touch surface never registers `.focusable()`, so `focused` reads
 * false naturally off [interactionSource] with no extra wiring needed, matching what
 * `AreTouchable` already assumed before this binding existed.
 */
private val mobileAreInteractiveBinding: AreInteractiveBinding = { onClick,
    modifier,
    interactionSource,
    shape,
    backgroundColor,
    backgroundBrush,
    shadowElevation,
    borderColor,
    borderBrush,
    enabled,
    onLongClick,
    disableScale,
    content,
    ->
    AreInteractiveSurface(
        onClick = onClick,
        modifier = modifier,
        interactionSource = interactionSource,
        shape = shape,
        backgroundColor = backgroundColor,
        backgroundBrush = backgroundBrush,
        shadowElevation = shadowElevation,
        borderColor = borderColor,
        borderBrush = borderBrush,
        enabled = enabled,
        onLongClick = onLongClick,
        disableScale = disableScale,
        content = content,
    )
}
