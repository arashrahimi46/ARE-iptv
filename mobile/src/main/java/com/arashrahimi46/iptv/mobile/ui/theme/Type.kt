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
 * [AreIptvTypographyVazir]) mirroring :tv's [com.arashrahimi46.iptv.mobile.design.AreIptvTheme]
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

internal const val LineHeightTight = 1.05f
internal const val LineHeightSnug = 1.2f
internal const val LineHeightNormal = 1.45f

/**
 * Step 6: phone-appropriate type scale -- same role names/weights/families as :tv's (now :core's)
 * [com.arashrahimi46.iptv.mobile.design.AreIptvTypography], but :tv's sizes (hero 64sp/display 44sp/
 * h1 34sp/...) assume a ~10ft arm's-length TV viewport and are oversized on a handset held at
 * reading distance. Roughly a 60% scale-down, rounded to sizes that still read cleanly at phone
 * DPI; body/label/caption land close to Android's own Material defaults (16/14/12sp) since those
 * roles are read at normal phone distance either way.
 */
internal const val PhoneHeroSp = 40
internal const val PhoneDisplaySp = 32
internal const val PhoneH1Sp = 26
internal const val PhoneH2Sp = 21
internal const val PhoneH3Sp = 18
internal const val PhoneTileSp = 16
internal const val PhoneBodySp = 15
internal const val PhoneLabelSp = 14
internal const val PhoneCaptionSp = 12
internal const val PhoneMonoSp = 12

/** Same composite text roles as :tv's [com.arashrahimi46.iptv.mobile.design.AreIptvTypography]. */
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
    hero = TextStyle(fontFamily = DisplayFontFamily, fontWeight = FontWeight.Bold, fontSize = PhoneHeroSp.sp, lineHeight = (PhoneHeroSp * LineHeightTight).sp, letterSpacing = (-0.02).em),
    display = TextStyle(fontFamily = DisplayFontFamily, fontWeight = FontWeight.Bold, fontSize = PhoneDisplaySp.sp, lineHeight = (PhoneDisplaySp * LineHeightTight).sp, letterSpacing = (-0.02).em),
    h1 = TextStyle(fontFamily = DisplayFontFamily, fontWeight = FontWeight.Bold, fontSize = PhoneH1Sp.sp, lineHeight = (PhoneH1Sp * LineHeightSnug).sp, letterSpacing = (-0.02).em),
    h2 = TextStyle(fontFamily = DisplayFontFamily, fontWeight = FontWeight.SemiBold, fontSize = PhoneH2Sp.sp, lineHeight = (PhoneH2Sp * LineHeightSnug).sp, letterSpacing = (-0.02).em),
    h3 = TextStyle(fontFamily = DisplayFontFamily, fontWeight = FontWeight.SemiBold, fontSize = PhoneH3Sp.sp, lineHeight = (PhoneH3Sp * LineHeightSnug).sp, letterSpacing = (-0.02).em),
    tile = TextStyle(fontFamily = BodyFontFamily, fontWeight = FontWeight.SemiBold, fontSize = PhoneTileSp.sp, lineHeight = (PhoneTileSp * LineHeightSnug).sp),
    body = TextStyle(fontFamily = BodyFontFamily, fontWeight = FontWeight.Normal, fontSize = PhoneBodySp.sp, lineHeight = (PhoneBodySp * LineHeightNormal).sp),
    label = TextStyle(fontFamily = BodyFontFamily, fontWeight = FontWeight.SemiBold, fontSize = PhoneLabelSp.sp, lineHeight = (PhoneLabelSp * LineHeightSnug).sp),
    caption = TextStyle(fontFamily = BodyFontFamily, fontWeight = FontWeight.Medium, fontSize = PhoneCaptionSp.sp, lineHeight = (PhoneCaptionSp * LineHeightNormal).sp),
    mono = TextStyle(fontFamily = MonoFontFamily, fontWeight = FontWeight.Medium, fontSize = PhoneMonoSp.sp, lineHeight = (PhoneMonoSp * LineHeightNormal).sp),
)

/** Same type scale as [AreIptvTypographyDefault], entirely on [VazirFontFamily] -- swapped in for
 * fa/ar locales (see [com.arashrahimi46.iptv.mobile.ui.theme.AreIptvMobileTheme]'s
 * `LocalLayoutDirection` check), matching :tv's `AreIptvTypographyVazir` role-for-role but at the
 * same phone-scale sizes as [AreIptvTypographyDefault] above. */
val AreIptvTypographyVazir = AreIptvTypography(
    hero = TextStyle(fontFamily = VazirFontFamily, fontWeight = FontWeight.Bold, fontSize = PhoneHeroSp.sp, lineHeight = (PhoneHeroSp * LineHeightTight).sp, letterSpacing = (-0.02).em),
    display = TextStyle(fontFamily = VazirFontFamily, fontWeight = FontWeight.Bold, fontSize = PhoneDisplaySp.sp, lineHeight = (PhoneDisplaySp * LineHeightTight).sp, letterSpacing = (-0.02).em),
    h1 = TextStyle(fontFamily = VazirFontFamily, fontWeight = FontWeight.Bold, fontSize = PhoneH1Sp.sp, lineHeight = (PhoneH1Sp * LineHeightSnug).sp, letterSpacing = (-0.02).em),
    h2 = TextStyle(fontFamily = VazirFontFamily, fontWeight = FontWeight.SemiBold, fontSize = PhoneH2Sp.sp, lineHeight = (PhoneH2Sp * LineHeightSnug).sp, letterSpacing = (-0.02).em),
    h3 = TextStyle(fontFamily = VazirFontFamily, fontWeight = FontWeight.SemiBold, fontSize = PhoneH3Sp.sp, lineHeight = (PhoneH3Sp * LineHeightSnug).sp, letterSpacing = (-0.02).em),
    tile = TextStyle(fontFamily = VazirFontFamily, fontWeight = FontWeight.SemiBold, fontSize = PhoneTileSp.sp, lineHeight = (PhoneTileSp * LineHeightSnug).sp),
    body = TextStyle(fontFamily = VazirFontFamily, fontWeight = FontWeight.Normal, fontSize = PhoneBodySp.sp, lineHeight = (PhoneBodySp * LineHeightNormal).sp),
    label = TextStyle(fontFamily = VazirFontFamily, fontWeight = FontWeight.SemiBold, fontSize = PhoneLabelSp.sp, lineHeight = (PhoneLabelSp * LineHeightSnug).sp),
    caption = TextStyle(fontFamily = VazirFontFamily, fontWeight = FontWeight.Medium, fontSize = PhoneCaptionSp.sp, lineHeight = (PhoneCaptionSp * LineHeightNormal).sp),
    mono = TextStyle(fontFamily = VazirFontFamily, fontWeight = FontWeight.Medium, fontSize = PhoneMonoSp.sp, lineHeight = (PhoneMonoSp * LineHeightNormal).sp),
)
