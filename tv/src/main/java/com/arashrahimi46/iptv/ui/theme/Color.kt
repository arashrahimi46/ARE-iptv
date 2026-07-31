package com.arashrahimi46.iptv.ui.theme

import androidx.compose.ui.graphics.Color

// ---- Brand: electric blue ----
val Blue300 = Color(0xFF93C5FD)
val Blue400 = Color(0xFF60A5FA)
val Blue500 = Color(0xFF3B82F6)
val Blue600 = Color(0xFF2563EB)
val Blue700 = Color(0xFF1D4ED8)
val BlueGlow = Color(0x8C3B82F6) // rgba(59,130,246,0.55)
val BlueWash = Color(0x243B82F6) // rgba(59,130,246,0.14)

// ---- Neutral ramp ----
val Ink950 = Color(0xFF06070A)
val Ink900 = Color(0xFF0A0B0F)
val Ink850 = Color(0xFF0E1015)
val Ink800 = Color(0xFF14161C)
val Ink750 = Color(0xFF191C23)
val Ink700 = Color(0xFF1F232C)
val Ink600 = Color(0xFF262A34)
val Ink500 = Color(0xFF333844)
val Ink400 = Color(0xFF4B515F)
val Ink300 = Color(0xFF6B7280)
// P0.3 (WCAG 1.4.1/contrast): Ink300 at caption sizes on the dark surfaces textTertiary
// renders over (surface1/surface2) measured 2.97-4.13:1 -- below the 4.5:1 AA minimum.
// Ink250 is a dedicated, slightly lighter step for TEXT use specifically (Ink300 itself is
// left as-is in case something else in the ramp depends on it) that measures ~5:1 against
// those same surfaces.
val Ink250 = Color(0xFF8C92A3)
val Ink200 = Color(0xFF9AA2B1)
val Ink100 = Color(0xFFC4CAD4)
val Ink050 = Color(0xFFE7EAF0)
val White = Color(0xFFF6F8FC)

// ---- Status ramps ----
val Green500 = Color(0xFF22C55E)
val Green400 = Color(0xFF4ADE80)
val Amber500 = Color(0xFFEAB308)
val Amber400 = Color(0xFFFACC15)
val Red500 = Color(0xFFEF4444)
val Red400 = Color(0xFFF87171)
val Violet500 = Color(0xFF8B5CF6)
val Violet400 = Color(0xFFA78BFA)

// ---- Light theme raw values ----
val LightBgSunken = Color(0xFFE9EDF3)
val LightBgBase = Color(0xFFF3F5F9)
val LightSurface1 = Color(0xFFFFFFFF)
val LightSurface2 = Color(0xFFF7F9FC)
val LightSurface3 = Color(0xFFEEF1F6)
val LightTextPrimary = Color(0xFF0D1017)
val LightTextSecondary = Color(0xFF4A5163)
// P0.3: was #757D8E (4.13:1 on LightSurface1/white -- below the 4.5:1 AA minimum at
// caption sizes). Darkened to measure ~4.8:1.
val LightTextTertiary = Color(0xFF6B7280)
val LightTextDisabled = Color(0xFFA5ABB8)
val LightBorderSubtle = Color(0x120F141E) // rgba(15,20,30,0.07)
val LightBorderDefault = Color(0x1F0F141E) // rgba(15,20,30,0.12)
val LightBorderStrong = Color(0x330F141E) // rgba(15,20,30,0.20)
val LightAccentWash = Color(0x1A2563EB) // rgba(37,99,235,0.10)

/**
 * Full semantic color palette for one theme mode (dark or light), mirroring
 * the CSS semantic aliases in tokens/colors.css. Reference these, never the
 * raw ramps above, from component code.
 */
data class AreIptvColors(
    val isDark: Boolean,
    val bgSunken: Color,
    val bgBase: Color,
    val surface1: Color,
    val surface2: Color,
    val surface3: Color,
    val surfaceOverlay: Color,
    val surfaceGlass: Color,
    // Glass surface language: an elevated (denser) fill for modals/HUD over media so text stays
    // legible, plus the two stops of the "lit edge" gradient border (bright top -> faint bottom).
    val surfaceGlassElevated: Color,
    /**
     * A far sheerer glass fill than [surfaceGlassElevated], for a large hero surface (the floating
     * sidebar) that sits over real backdrop blur and should read as *true see-through* glass. Used
     * ONLY on blur-capable tiers -- the blur itself carries legibility there, so the fill can drop to
     * ~30% and let the blurred content behind actually show. Tier C never uses it (no blur to reveal).
     * Default is the dark value; light overrides it.
     */
    val surfaceGlassSheer: Color = Color(0x4D1E222C), // rgba(30,34,44,0.30)
    val glassHighlight: Color,
    val borderGlass: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textDisabled: Color,
    val textOnAccent: Color,
    val borderSubtle: Color,
    val borderDefault: Color,
    val borderStrong: Color,
    val accent: Color,
    val accentHover: Color,
    val accentPress: Color,
    val accentWash: Color,
    val accentFg: Color,
    val focusRing: Color,
    val success: Color = Green500,
    val warning: Color = Amber500,
    val danger: Color = Red500,
    val live: Color = Red500,
    val smart: Color = Violet500,
    val healthStable: Color = Green500,
    val healthModerate: Color = Amber500,
    val healthPoor: Color = Red500,
    val violetText: Color,
    // The -400 (lighter) tint of each status hue, for text/icon use over the -500 solid
    // indicator dots above -- these were being reached via raw hex literals at call sites
    // (PosterTile's rating star, LiveScreen's "ON AIR NOW" label, Badge's catch-up text)
    // instead of through the theme, same class of violation as everything else in this pass.
    val ratingStar: Color = Amber400,
    val onAirText: Color = Red400,
    val catchupText: Color = Green400,
    // Channel-logo "well": kept a dark neutral in BOTH themes because provider logos are
    // overwhelmingly white/light PNGs designed for dark backgrounds -- a light-theme white
    // well (surfaceOverlay = 86% white) made them vanish. logoWellText is the initials
    // fallback colour, which must read on that dark well.
    val logoWell: Color = Ink800,
    val logoWellText: Color = White,
    // ---- Glass V2 (see docs/glass-v2-design.md §7) ----
    /** Nested-control tint. A control INSIDE glass gets tint + hairline and never a second fill. */
    val glassChildTint: Color = Color(0x0FFFFFFF), // white .06
    /** Same rule as [glassChildTint] but for a *track* or *chip* whose shape has to stay legible. */
    val glassTrackTint: Color = Color(0x1CFFFFFF), // white .11
    /** Contrast floor drawn under the ambient backdrop's artwork/mesh layers. */
    val backdropVeil: Color = Color(0x8C06070A), // ink .55
    /**
     * Luminance floor under the now-translucent channel-tile logo well. Deliberately the SAME dark
     * value in both themes, for exactly the reason [logoWell] is: provider logos are overwhelmingly
     * white-on-transparent PNGs and vanish on a light plate.
     */
    val logoWellScrim: Color = Color(0x8C0A0C11), // ink .55, both themes
    /** Strength of the per-tile artwork wash (§6.3.1). */
    val tileWashAlpha: Float = 0.22f,
    /**
     * Fill for an input well. Goes DARKER than its parent surface, unlike every other glass token --
     * a field is recessed into the glass, not sitting on it (see [Modifier.glassWell]).
     */
    val glassWellTint: Color = Color(0x3D000000), // black .24
)

val AreIptvDarkColors = AreIptvColors(
    isDark = true,
    bgSunken = Ink950,
    bgBase = Ink900,
    surface1 = Ink800,
    surface2 = Ink700,
    surface3 = Ink600,
    surfaceOverlay = Color(0xD10E1015), // rgba(14,16,21,0.82)
    surfaceGlass = Color(0x8C1E222C), // rgba(30,34,44,0.55)
    surfaceGlassElevated = Color(0xB814161C), // rgba(20,22,28,0.72)
    glassHighlight = Color(0x38FFFFFF), // white 0.22
    borderGlass = Color(0x1AFFFFFF), // white 0.10
    textPrimary = White,
    textSecondary = Ink200,
    textTertiary = Ink250,
    textDisabled = Ink400,
    textOnAccent = Color(0xFFFFFFFF),
    borderSubtle = Color(0x0FFFFFFF),
    borderDefault = Color(0x1AFFFFFF),
    borderStrong = Color(0x2EFFFFFF),
    accent = Blue500,
    accentHover = Blue400,
    accentPress = Blue600,
    accentWash = BlueWash,
    accentFg = Color(0xFFFFFFFF),
    focusRing = Blue400,
    violetText = Violet400,
)

val AreIptvLightColors = AreIptvColors(
    isDark = false,
    bgSunken = LightBgSunken,
    bgBase = LightBgBase,
    surface1 = LightSurface1,
    surface2 = LightSurface2,
    surface3 = LightSurface3,
    surfaceOverlay = Color(0xDBFFFFFF), // rgba(255,255,255,0.86)
    surfaceGlass = Color(0x99FFFFFF), // rgba(255,255,255,0.6)
    surfaceGlassElevated = Color(0xD1FFFFFF), // rgba(255,255,255,0.82)
    surfaceGlassSheer = Color(0x80FFFFFF), // rgba(255,255,255,0.50) -- light glass reads on the bright page at ~50%
    glassHighlight = Color(0xB3FFFFFF), // white 0.70
    borderGlass = Color(0x240F141E), // ink 0.14 -- light needs a dark edge to read
    textPrimary = LightTextPrimary,
    textSecondary = LightTextSecondary,
    textTertiary = LightTextTertiary,
    textDisabled = LightTextDisabled,
    textOnAccent = Color(0xFFFFFFFF),
    borderSubtle = LightBorderSubtle,
    borderDefault = LightBorderDefault,
    borderStrong = LightBorderStrong,
    accent = Blue600,
    accentHover = Blue500,
    accentPress = Blue700,
    accentWash = LightAccentWash,
    accentFg = Color(0xFFFFFFFF),
    focusRing = Blue600,
    violetText = Violet400,
    glassChildTint = Color(0x0D0F141E), // ink .05
    glassTrackTint = Color(0x170F141E), // ink .09
    backdropVeil = Color(0x00000000), // light needs no darkening veil; the page is already bright
    tileWashAlpha = 0.16f,
    glassWellTint = Color(0x0F0F141E), // ink .06 -- a light-theme well darkens only slightly
)

/**
 * Glass V2 §7: V1's fill alphas were tuned to compensate for having *nothing* behind the glass. Once
 * a real blurred backdrop is there (Tier A/B, §8) the same alphas read as opaque slabs, so the fills
 * get lighter and the specular top stop gets stronger. Tier C keeps the V1 numbers exactly -- it has
 * no blur, so it still needs the density to stay legible.
 */
fun AreIptvColors.withBlurredBackdrop(): AreIptvColors = if (isDark) {
    copy(
        surfaceGlass = Color(0x611E222C), // .55 -> .38
        surfaceGlassElevated = Color(0x9414161C), // .72 -> .58
        glassHighlight = Color(0x47FFFFFF), // .22 -> .28
    )
} else {
    copy(
        surfaceGlass = Color(0x6BFFFFFF), // .60 -> .42
        surfaceGlassElevated = Color(0xA8FFFFFF), // .82 -> .66
        glassHighlight = Color(0xC7FFFFFF), // .70 -> .78
    )
}

// ---- Selectable accent presets ----
// Each preset is a 5-shade ramp (300..700, Tailwind-style, matching the Blue ramp above).
// Accent tokens for a theme mode are DERIVED from the ramp by [tokens] -- exactly how the
// original blue accent picked shade 500 in dark / 600 in light -- so a preset needs no
// per-mode hand-tuning. `onAccent` is the text/icon color that reads on a solid fill of it:
// pure white for saturated hues, dark ink for the bright Amber/Cyan where white fails contrast.
private val AccentWhite = Color(0xFFFFFFFF)

private val Teal300 = Color(0xFF5EEAD4)
private val Teal400 = Color(0xFF2DD4BF)
private val Teal500 = Color(0xFF14B8A6)
private val Teal600 = Color(0xFF0D9488)
private val Teal700 = Color(0xFF0F766E)

private val Violet300 = Color(0xFFC4B5FD)
private val Violet600 = Color(0xFF7C3AED)
private val Violet700 = Color(0xFF6D28D9)

private val Emerald300 = Color(0xFF6EE7B7)
private val Emerald400 = Color(0xFF34D399)
private val Emerald500 = Color(0xFF10B981)
private val Emerald600 = Color(0xFF059669)
private val Emerald700 = Color(0xFF047857)

private val AmberA300 = Color(0xFFFCD34D)
private val AmberA400 = Color(0xFFFBBF24)
private val AmberA500 = Color(0xFFF59E0B)
private val AmberA600 = Color(0xFFD97706)
private val AmberA700 = Color(0xFFB45309)

private val Rose300 = Color(0xFFFDA4AF)
private val Rose400 = Color(0xFFFB7185)
private val Rose500 = Color(0xFFF43F5E)
private val Rose600 = Color(0xFFE11D48)
private val Rose700 = Color(0xFFBE123C)

private val Cyan300 = Color(0xFF67E8F9)
private val Cyan400 = Color(0xFF22D3EE)
private val Cyan500 = Color(0xFF06B6D4)
private val Cyan600 = Color(0xFF0891B2)
private val Cyan700 = Color(0xFF0E7490)

private val Slate300 = Color(0xFF94A3B8)
private val Slate400 = Color(0xFF7C8AA0)
private val Slate500 = Color(0xFF5A6779)
private val Slate600 = Color(0xFF475569)
private val Slate700 = Color(0xFF334155)

/** The accent-related subset of [AreIptvColors], derived from an [AccentPreset] for one mode. */
data class AccentTokens(
    val accent: Color,
    val accentHover: Color,
    val accentPress: Color,
    val accentWash: Color,
    val accentFg: Color,
    val focusRing: Color,
)

/**
 * A user-selectable brand accent. [id] is the stable value persisted in DataStore.
 * BLUE reproduces the app's original hardwired accent exactly (backward compatible).
 */
enum class AccentPreset(
    val id: String,
    private val s300: Color,
    private val s400: Color,
    private val s500: Color,
    private val s600: Color,
    private val s700: Color,
    private val onAccent: Color,
) {
    BLUE("blue", Blue300, Blue400, Blue500, Blue600, Blue700, AccentWhite),
    TEAL("teal", Teal300, Teal400, Teal500, Teal600, Teal700, AccentWhite),
    VIOLET("violet", Violet300, Violet400, Violet500, Violet600, Violet700, AccentWhite),
    EMERALD("emerald", Emerald300, Emerald400, Emerald500, Emerald600, Emerald700, AccentWhite),
    AMBER("amber", AmberA300, AmberA400, AmberA500, AmberA600, AmberA700, Ink950),
    ROSE("rose", Rose300, Rose400, Rose500, Rose600, Rose700, AccentWhite),
    CYAN("cyan", Cyan300, Cyan400, Cyan500, Cyan600, Cyan700, Ink950),
    SLATE("slate", Slate300, Slate400, Slate500, Slate600, Slate700, AccentWhite);

    /** The swatch color shown in the settings picker -- the mode's resolved solid accent. */
    fun swatch(isDark: Boolean): Color = if (isDark) s500 else s600

    /** Derive the per-mode accent tokens, mirroring the original blue's shade choices. */
    fun tokens(isDark: Boolean): AccentTokens = if (isDark) {
        AccentTokens(
            accent = s500,
            accentHover = s400,
            accentPress = s600,
            accentWash = s500.copy(alpha = 0.14f),
            accentFg = onAccent,
            focusRing = s400,
        )
    } else {
        AccentTokens(
            accent = s600,
            accentHover = s500,
            accentPress = s700,
            accentWash = s600.copy(alpha = 0.10f),
            accentFg = onAccent,
            focusRing = s600,
        )
    }

    companion object {
        /** Resolve a persisted [id] back to a preset, falling back to [BLUE] for unknown/null. */
        fun fromId(id: String?): AccentPreset = entries.firstOrNull { it.id == id } ?: BLUE
    }
}

/** Overlay an accent preset's tokens onto a base (neutral) palette for a mode. */
fun AreIptvColors.withAccent(tokens: AccentTokens): AreIptvColors = copy(
    accent = tokens.accent,
    accentHover = tokens.accentHover,
    accentPress = tokens.accentPress,
    accentWash = tokens.accentWash,
    accentFg = tokens.accentFg,
    focusRing = tokens.focusRing,
)
