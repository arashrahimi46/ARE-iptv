package com.arashrahimi46.iptv.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/*
 * Font families. The design system specifies Space Grotesk (display),
 * Manrope (body/UI) and JetBrains Mono (technical) loaded from Google Fonts.
 * Phase 0 ships with system fallbacks only (FontFamily.SansSerif /
 * FontFamily.Monospace) rather than the downloadable Google Fonts provider —
 * see report for rationale (offline-first TV boxes, no network dependency
 * for a design-system checkpoint). Swapping in real GoogleFont() family
 * definitions later is a drop-in change to these three vals.
 */
val DisplayFontFamily: FontFamily = FontFamily.SansSerif
val BodyFontFamily: FontFamily = FontFamily.SansSerif
val MonoFontFamily: FontFamily = FontFamily.Monospace

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
    label = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = (15 * LineHeightSnug).sp,
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
