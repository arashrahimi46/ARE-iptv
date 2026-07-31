package com.arashrahimi46.iptv.mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.arashrahimi46.iptv.ui.theme.AreIptvColors
import com.arashrahimi46.iptv.ui.theme.AreIptvDarkColors
import com.arashrahimi46.iptv.ui.theme.AreIptvLightColors
import com.arashrahimi46.iptv.ui.theme.AreIptvMotionDefault
import com.arashrahimi46.iptv.ui.theme.AreIptvMotionReduced
import com.arashrahimi46.iptv.ui.theme.AreIptvRadiusDefault
import com.arashrahimi46.iptv.ui.theme.AreIptvSpacingDefault
import com.arashrahimi46.iptv.ui.theme.LocalAmbientArtwork as CoreLocalAmbientArtwork
import com.arashrahimi46.iptv.ui.theme.LocalGlassTier
import com.arashrahimi46.iptv.ui.theme.LocalReducedMotion
import com.arashrahimi46.iptv.ui.theme.rememberGlassTier
import com.arashrahimi46.iptv.ui.theme.withBlurredBackdrop
import com.arashrahimi46.iptv.ui.theme.LocalAreIptvColors as CoreLocalAreIptvColors
import com.arashrahimi46.iptv.ui.theme.LocalAreIptvMotion as CoreLocalAreIptvMotion
import com.arashrahimi46.iptv.ui.theme.LocalAreIptvRadius as CoreLocalAreIptvRadius
import com.arashrahimi46.iptv.ui.theme.LocalAreIptvSpacing as CoreLocalAreIptvSpacing
import com.arashrahimi46.iptv.ui.theme.LocalAreIptvTypography as CoreLocalAreIptvTypography
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
 * Phone theming, wired to this module's own copy of the design-system tokens
 * ([com.arashrahimi46.iptv.ui.theme.AreIptvColors]/[AreIptvTypography]) -- the same token *values*
 * :tv uses, so the two apps read as one product, but a separate source tree (:core is
 * resources-only; only strings are shared). Built on standard `androidx.compose.material3`, NEVER
 * `androidx.tv.material3`, which is D-pad specific and wrong for touch.
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
    reducedMotion: Boolean = false,
    content: @Composable () -> Unit,
) {
    val base = if (isDark) AreIptvDarkColors else AreIptvLightColors
    // Glass V2 §7/§8: the fill alphas only get lighter where a real blurred backdrop exists to carry
    // legibility. Tier C keeps the known-good V1 numbers, so it is the safe fallback.
    val tier = rememberGlassTier()
    val colors = if (tier.hasBackdropBlur) base.withBlurredBackdrop() else base
    val motion = if (reducedMotion) AreIptvMotionReduced else AreIptvMotionDefault
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
        // Bridges the CompositionLocals declared in `com.arashrahimi46.iptv.ui.theme` -- the glass
        // layer this module owns a copy of -- which are a distinct set from the two above despite
        // the matching names. (The `Core*` import aliases are legacy naming from when that package
        // lived in :core; :core is resources-only now.) Without this, everything built on that
        // layer -- glassSurface, controlSkin, the Are* tiles -- would silently fall back to its
        // Theme.kt defaults (AreIptvDarkColors, always, regardless of [isDark]). Typography can't
        // reuse the `typography` val above: that package declares its own AreIptvTypography data
        // class, separate from this one, so it needs an instance of its own type picked by the same
        // RTL rule. There is no phone-specific spacing/radius/motion, so those three take the
        // shared default token sets.
        CoreLocalAreIptvColors provides colors,
        // Step 6: the glass layer's own AreIptvTypographyDefault/Vazir are TV-scale (hero 64sp/
        // display 44sp/h1 34sp/...) -- feeding those to the components built on it would render
        // TV-sized headings on a handset. Build a phone-scale instance of ITS AreIptvTypography
        // type instead (same role names/weights, its font families since these are its components),
        // using the same sp values as this package's Type.kt phone scale so the two stay in step.
        CoreLocalAreIptvTypography provides (
            if (LocalLayoutDirection.current == LayoutDirection.Rtl) corePhoneTypographyVazir
            else corePhoneTypographyDefault
        ),
        CoreLocalAreIptvSpacing provides AreIptvSpacingDefault,
        CoreLocalAreIptvRadius provides AreIptvRadiusDefault,
        CoreLocalAreIptvMotion provides motion,
        LocalReducedMotion provides reducedMotion,
        LocalGlassTier provides tier,
        CoreLocalThemeIsDark provides isDark,
        // :mobile has no ambient-backdrop concept (that's :tv's own AreIptvAppShell) -- ArePosterTile/
        // AreChannelTile still read LocalAmbientArtwork.current unconditionally (no guard at the
        // call site), and it has no default, so any screen rendering those tiles without this
        // crashed with "LocalAmbientArtwork not provided". A static, never-updated null state is
        // the correct fix here: it satisfies the read, and since nothing on :mobile ever writes to
        // it, the tiles' ambient-wash branch simply never activates -- the same visual result as if
        // the CompositionLocal didn't exist, at the cost of one throwaway MutableState.
        CoreLocalAmbientArtwork provides remember { mutableStateOf<String?>(null) },
    ) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}

/**
 * Phone-scale instances of the glass layer's OWN [CoreAreIptvTypography] type -- same sp values as
 * [AreIptvTypographyDefault]/[AreIptvTypographyVazir] above (this package's Type.kt), just built
 * against that package's font families since they feed its components directly via
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
