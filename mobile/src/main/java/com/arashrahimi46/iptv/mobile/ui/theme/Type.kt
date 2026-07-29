package com.arashrahimi46.iptv.mobile.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.arashrahimi46.iptv.mobile.R

/**
 * Duplicated from :tv's ui/theme/Type.kt (same font families, same type scale) rather than
 * shared via :core -- :core is a resource-bearing library already (for the moved data/model
 * files), but keeping brand font *files* out of it avoids widening :core's manifest/AAR surface
 * for a design-token concern the two apps could plausibly diverge on later. The font .ttf files
 * are copied 1:1 into mobile/res/font, including the Vazirmatn RTL swap (see
 * [AreIptvTypographyVazir]) mirroring :tv's [com.arashrahimi46.iptv.ui.theme.AreIptvTheme]
 * `LocalLayoutDirection` check.
 */
private val DisplayFontFamily: FontFamily = FontFamily(
    Font(R.font.space_grotesk_regular, FontWeight.Normal),
    Font(R.font.space_grotesk_medium, FontWeight.Medium),
    Font(R.font.space_grotesk_semibold, FontWeight.SemiBold),
    Font(R.font.space_grotesk_bold, FontWeight.Bold),
)
private val BodyFontFamily: FontFamily = FontFamily(
    Font(R.font.manrope_regular, FontWeight.Normal),
    Font(R.font.manrope_medium, FontWeight.Medium),
    Font(R.font.manrope_semibold, FontWeight.SemiBold),
    Font(R.font.manrope_bold, FontWeight.Bold),
    Font(R.font.manrope_extrabold, FontWeight.ExtraBold),
)
private val MonoFontFamily: FontFamily = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
    Font(R.font.jetbrains_mono_bold, FontWeight.Bold),
)

/** Vazirmatn (OFL) -- the Persian/Arabic-capable family used for every text role when the active
 * locale is RTL (see [AreIptvTypographyVazir]); mirrors :tv's ui/theme/Type.kt. */
private val VazirFontFamily: FontFamily = FontFamily(
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_semibold, FontWeight.SemiBold),
    Font(R.font.vazirmatn_bold, FontWeight.Bold),
    Font(R.font.vazirmatn_extrabold, FontWeight.ExtraBold),
)

private const val LineHeightTight = 1.05f
private const val LineHeightSnug = 1.2f
private const val LineHeightNormal = 1.45f

/** Same composite text roles as :tv's [com.arashrahimi46.iptv.ui.theme.AreIptvTypography]. */
data class AreIptvTypography(
    val hero: TextStyle,
    val display: TextStyle,
    val h1: TextStyle,
    val h2: TextStyle,
    val h3: TextStyle,
    val tile: TextStyle,
    val body: TextStyle,
    val label: TextStyle,
    val caption: TextStyle,
    val mono: TextStyle,
)

val AreIptvTypographyDefault = AreIptvTypography(
    hero = TextStyle(fontFamily = DisplayFontFamily, fontWeight = FontWeight.Bold, fontSize = 64.sp, lineHeight = (64 * LineHeightTight).sp, letterSpacing = (-0.02).em),
    display = TextStyle(fontFamily = DisplayFontFamily, fontWeight = FontWeight.Bold, fontSize = 44.sp, lineHeight = (44 * LineHeightTight).sp, letterSpacing = (-0.02).em),
    h1 = TextStyle(fontFamily = DisplayFontFamily, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = (34 * LineHeightSnug).sp, letterSpacing = (-0.02).em),
    h2 = TextStyle(fontFamily = DisplayFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, lineHeight = (26 * LineHeightSnug).sp, letterSpacing = (-0.02).em),
    h3 = TextStyle(fontFamily = DisplayFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 21.sp, lineHeight = (21 * LineHeightSnug).sp, letterSpacing = (-0.02).em),
    tile = TextStyle(fontFamily = BodyFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = (18 * LineHeightSnug).sp),
    body = TextStyle(fontFamily = BodyFontFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = (16 * LineHeightNormal).sp),
    label = TextStyle(fontFamily = BodyFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = (16 * LineHeightSnug).sp),
    caption = TextStyle(fontFamily = BodyFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = (14 * LineHeightNormal).sp),
    mono = TextStyle(fontFamily = MonoFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = (14 * LineHeightNormal).sp),
)

/** Same type scale as [AreIptvTypographyDefault], entirely on [VazirFontFamily] -- swapped in for
 * fa/ar locales (see [com.arashrahimi46.iptv.mobile.ui.theme.AreIptvMobileTheme]'s
 * `LocalLayoutDirection` check), matching :tv's `AreIptvTypographyVazir`. */
val AreIptvTypographyVazir = AreIptvTypography(
    hero = TextStyle(fontFamily = VazirFontFamily, fontWeight = FontWeight.Bold, fontSize = 64.sp, lineHeight = (64 * LineHeightTight).sp, letterSpacing = (-0.02).em),
    display = TextStyle(fontFamily = VazirFontFamily, fontWeight = FontWeight.Bold, fontSize = 44.sp, lineHeight = (44 * LineHeightTight).sp, letterSpacing = (-0.02).em),
    h1 = TextStyle(fontFamily = VazirFontFamily, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = (34 * LineHeightSnug).sp, letterSpacing = (-0.02).em),
    h2 = TextStyle(fontFamily = VazirFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, lineHeight = (26 * LineHeightSnug).sp, letterSpacing = (-0.02).em),
    h3 = TextStyle(fontFamily = VazirFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 21.sp, lineHeight = (21 * LineHeightSnug).sp, letterSpacing = (-0.02).em),
    tile = TextStyle(fontFamily = VazirFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = (18 * LineHeightSnug).sp),
    body = TextStyle(fontFamily = VazirFontFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = (16 * LineHeightNormal).sp),
    label = TextStyle(fontFamily = VazirFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = (16 * LineHeightSnug).sp),
    caption = TextStyle(fontFamily = VazirFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = (14 * LineHeightNormal).sp),
    mono = TextStyle(fontFamily = VazirFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = (14 * LineHeightNormal).sp),
)
