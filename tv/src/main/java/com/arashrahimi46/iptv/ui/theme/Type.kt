package com.arashrahimi46.iptv.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.arashrahimi46.iptv.R

/*
 * Font families. The design system specifies Space Grotesk (display),
 * Manrope (body/UI) and JetBrains Mono (technical). Self-hosted as local
 * .ttf resources in res/font (OFL-licensed static weight instances of the
 * Google Fonts releases) rather than the Google Fonts downloadable-provider
 * API, so there's no network/Play-Services dependency at runtime for an
 * offline-first TV app.
 */
val DisplayFontFamily: FontFamily = FontFamily(
    Font(R.font.space_grotesk_regular, FontWeight.Normal),
    Font(R.font.space_grotesk_medium, FontWeight.Medium),
    Font(R.font.space_grotesk_semibold, FontWeight.SemiBold),
    Font(R.font.space_grotesk_bold, FontWeight.Bold),
)
val BodyFontFamily: FontFamily = FontFamily(
    Font(R.font.manrope_regular, FontWeight.Normal),
    Font(R.font.manrope_medium, FontWeight.Medium),
    Font(R.font.manrope_semibold, FontWeight.SemiBold),
    Font(R.font.manrope_bold, FontWeight.Bold),
    Font(R.font.manrope_extrabold, FontWeight.ExtraBold),
)
val MonoFontFamily: FontFamily = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
    Font(R.font.jetbrains_mono_bold, FontWeight.Bold),
)

// Line-height multipliers from tokens/typography.css
const val LineHeightTight = 1.05f
const val LineHeightSnug = 1.2f
const val LineHeightNormal = 1.45f
const val LineHeightRelaxed = 1.6f

/** Composite text roles, mirroring tokens/typography.css `--text-*` composites. */
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
    hero = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 64.sp,
        lineHeight = (64 * LineHeightTight).sp,
        letterSpacing = (-0.02).em,
    ),
    display = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 44.sp,
        lineHeight = (44 * LineHeightTight).sp,
        letterSpacing = (-0.02).em,
    ),
    h1 = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = (34 * LineHeightSnug).sp,
        letterSpacing = (-0.02).em,
    ),
    h2 = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = (26 * LineHeightSnug).sp,
        letterSpacing = (-0.02).em,
    ),
    h3 = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 21.sp,
        lineHeight = (21 * LineHeightSnug).sp,
        letterSpacing = (-0.02).em,
    ),
    tile = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = (18 * LineHeightSnug).sp,
    ),
    body = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = (16 * LineHeightNormal).sp,
    ),
    // Design token tokens/typography.css defines --fs-label: 15px, which contradicts that same
    // file's own "nothing under 16px for UI text" floor. Product decision: legibility floor wins
    // over faithfully reproducing the self-contradicting source value -- corrected to 16sp.
    label = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = (16 * LineHeightSnug).sp,
    ),
    caption = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = (14 * LineHeightNormal).sp,
    ),
    mono = TextStyle(
        fontFamily = MonoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = (14 * LineHeightNormal).sp,
    ),
)
