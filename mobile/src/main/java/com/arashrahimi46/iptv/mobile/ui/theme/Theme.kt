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
import androidx.compose.ui.unit.dp
import com.arashrahimi46.iptv.ui.theme.LocalMinTouchTarget as CoreLocalMinTouchTarget
import com.arashrahimi46.iptv.ui.theme.LocalThemeIsDark as CoreLocalThemeIsDark
import com.arashrahimi46.iptv.ui.theme.AreIptvTypography as CoreAreIptvTypography
import com.arashrahimi46.iptv.ui.theme.DisplayFontFamily as CoreDisplayFontFamily
import com.arashrahimi46.iptv.ui.theme.BodyFontFamily as CoreBodyFontFamily
import com.arashrahimi46.iptv.ui.theme.MonoFontFamily as CoreMonoFontFamily
import com.arashrahimi46.iptv.ui.theme.VazirFontFamily as CoreVazirFontFamily
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

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
        // Step 6: :core's own AreIptvTypographyDefault/Vazir are :tv-scale (hero 64sp/display 44sp/
        // h1 34sp/...) -- feeding those to :core components (ArePosterTile, AreButton, AreChip, the
        // whole Milestone A/B surface) would render TV-sized headings on a handset. Build a
        // phone-scale instance of :core's OWN AreIptvTypography type instead (same role names/
        // weights, :core's font families since these ARE :core components), using the same sp
        // values as :mobile's own Type.kt phone scale so the two stay in lockstep.
        CoreLocalAreIptvTypography provides (
            if (LocalLayoutDirection.current == LayoutDirection.Rtl) corePhoneTypographyVazir
            else corePhoneTypographyDefault
        ),
        CoreLocalAreIptvSpacing provides AreIptvSpacingDefault,
        CoreLocalAreIptvRadius provides AreIptvRadiusDefault,
        CoreLocalAreIptvMotion provides AreIptvMotionDefault,
        CoreLocalThemeIsDark provides isDark,
        // Touch accessibility minimum. :tv leaves this 0 -- see LocalMinTouchTarget: on TV the focus
        // ring is drawn at the focusable's bounds, so inflating them would ring a larger box than
        // the control. A finger needs the 48dp; a D-pad does not.
        CoreLocalMinTouchTarget provides 48.dp,
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

/**
 * Phone-scale instances of :core's OWN [CoreAreIptvTypography] type -- same sp values as
 * [AreIptvTypographyDefault]/[AreIptvTypographyVazir] above (this package's Type.kt), just built
 * against :core's font families since these feed :core components directly via
 * [CoreLocalAreIptvTypography]. Kept in lockstep by construction: both read the same
 * Phone*Sp/LineHeight* constants from Type.kt.
 */
private val corePhoneTypographyDefault = CoreAreIptvTypography(
    hero = TextStyle(fontFamily = CoreDisplayFontFamily, fontWeight = FontWeight.Bold, fontSize = PhoneHeroSp.sp, lineHeight = (PhoneHeroSp * LineHeightTight).sp, letterSpacing = (-0.02).em),
    display = TextStyle(fontFamily = CoreDisplayFontFamily, fontWeight = FontWeight.Bold, fontSize = PhoneDisplaySp.sp, lineHeight = (PhoneDisplaySp * LineHeightTight).sp, letterSpacing = (-0.02).em),
    h1 = TextStyle(fontFamily = CoreDisplayFontFamily, fontWeight = FontWeight.Bold, fontSize = PhoneH1Sp.sp, lineHeight = (PhoneH1Sp * LineHeightSnug).sp, letterSpacing = (-0.02).em),
    h2 = TextStyle(fontFamily = CoreDisplayFontFamily, fontWeight = FontWeight.SemiBold, fontSize = PhoneH2Sp.sp, lineHeight = (PhoneH2Sp * LineHeightSnug).sp, letterSpacing = (-0.02).em),
    h3 = TextStyle(fontFamily = CoreDisplayFontFamily, fontWeight = FontWeight.SemiBold, fontSize = PhoneH3Sp.sp, lineHeight = (PhoneH3Sp * LineHeightSnug).sp, letterSpacing = (-0.02).em),
    tile = TextStyle(fontFamily = CoreBodyFontFamily, fontWeight = FontWeight.SemiBold, fontSize = PhoneTileSp.sp, lineHeight = (PhoneTileSp * LineHeightSnug).sp),
    body = TextStyle(fontFamily = CoreBodyFontFamily, fontWeight = FontWeight.Normal, fontSize = PhoneBodySp.sp, lineHeight = (PhoneBodySp * LineHeightNormal).sp),
    label = TextStyle(fontFamily = CoreBodyFontFamily, fontWeight = FontWeight.SemiBold, fontSize = PhoneLabelSp.sp, lineHeight = (PhoneLabelSp * LineHeightSnug).sp),
    caption = TextStyle(fontFamily = CoreBodyFontFamily, fontWeight = FontWeight.Medium, fontSize = PhoneCaptionSp.sp, lineHeight = (PhoneCaptionSp * LineHeightNormal).sp),
    mono = TextStyle(fontFamily = CoreMonoFontFamily, fontWeight = FontWeight.Medium, fontSize = PhoneMonoSp.sp, lineHeight = (PhoneMonoSp * LineHeightNormal).sp),
)

private val corePhoneTypographyVazir = CoreAreIptvTypography(
    hero = TextStyle(fontFamily = CoreVazirFontFamily, fontWeight = FontWeight.Bold, fontSize = PhoneHeroSp.sp, lineHeight = (PhoneHeroSp * LineHeightTight).sp, letterSpacing = (-0.02).em),
    display = TextStyle(fontFamily = CoreVazirFontFamily, fontWeight = FontWeight.Bold, fontSize = PhoneDisplaySp.sp, lineHeight = (PhoneDisplaySp * LineHeightTight).sp, letterSpacing = (-0.02).em),
    h1 = TextStyle(fontFamily = CoreVazirFontFamily, fontWeight = FontWeight.Bold, fontSize = PhoneH1Sp.sp, lineHeight = (PhoneH1Sp * LineHeightSnug).sp, letterSpacing = (-0.02).em),
    h2 = TextStyle(fontFamily = CoreVazirFontFamily, fontWeight = FontWeight.SemiBold, fontSize = PhoneH2Sp.sp, lineHeight = (PhoneH2Sp * LineHeightSnug).sp, letterSpacing = (-0.02).em),
    h3 = TextStyle(fontFamily = CoreVazirFontFamily, fontWeight = FontWeight.SemiBold, fontSize = PhoneH3Sp.sp, lineHeight = (PhoneH3Sp * LineHeightSnug).sp, letterSpacing = (-0.02).em),
    tile = TextStyle(fontFamily = CoreVazirFontFamily, fontWeight = FontWeight.SemiBold, fontSize = PhoneTileSp.sp, lineHeight = (PhoneTileSp * LineHeightSnug).sp),
    body = TextStyle(fontFamily = CoreVazirFontFamily, fontWeight = FontWeight.Normal, fontSize = PhoneBodySp.sp, lineHeight = (PhoneBodySp * LineHeightNormal).sp),
    label = TextStyle(fontFamily = CoreVazirFontFamily, fontWeight = FontWeight.SemiBold, fontSize = PhoneLabelSp.sp, lineHeight = (PhoneLabelSp * LineHeightSnug).sp),
    caption = TextStyle(fontFamily = CoreVazirFontFamily, fontWeight = FontWeight.Medium, fontSize = PhoneCaptionSp.sp, lineHeight = (PhoneCaptionSp * LineHeightNormal).sp),
    mono = TextStyle(fontFamily = CoreVazirFontFamily, fontWeight = FontWeight.Medium, fontSize = PhoneMonoSp.sp, lineHeight = (PhoneMonoSp * LineHeightNormal).sp),
)
