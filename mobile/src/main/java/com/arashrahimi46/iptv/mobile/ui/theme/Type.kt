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
 * are copied 1:1 into mobile/res/font. monolean: v1 skips the Vazirmatn RTL font swap :tv does
 * for fa/ar locales -- fa/ar users get the Latin brand fonts for now; upgrade path is copying
 * vazirmatn_*.ttf here and mirroring AreIptvTheme's LocalLayoutDirection check.
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
