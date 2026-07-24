package com.arashrahimi46.iptv.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Subtle resting lift shared by every glass surface and accent chip so the UI floats above the
 *  page (design ask: "subtle hint" depth). Reads mainly in light mode; in dark the lit edge carries
 *  the separation. Kept low (6dp) so a grid of tiles doesn't turn heavy. */
val GlassElevation: Dp = 6.dp

/**
 * The one shared "glass" surface language: a translucent fill + a hairline gradient border whose
 * bright top stop reads as a lit edge (bright top -> faint bottom) + a subtle drop shadow. The fill
 * alpha IS the glass -- content behind shows through; there is deliberately NO backdrop blur here
 * (see the design spec: real blur is reserved for dialogs via the window API, and is impossible over
 * the SurfaceView video).
 *
 * Compose [TvFocusable] / [Modifier.tvFocusable] outside this so the accent ring + glow still draw
 * on top -- focus is intentionally not baked into the glass.
 *
 * @param elevated denser fill for modals/HUD over media so text stays legible.
 * @param shadow set false for surfaces that shouldn't lift (e.g. an inline track already inside a
 *   glass panel, or a divider) to avoid a shadow-on-shadow smudge.
 */
fun Modifier.glassSurface(
    shape: Shape,
    elevated: Boolean = false,
    shadow: Boolean = true,
): Modifier = composed {
    val c = AreIptvTheme.colors
    this
        .then(if (shadow) Modifier.shadow(GlassElevation, shape) else Modifier)
        .background(if (elevated) c.surfaceGlassElevated else c.surfaceGlass, shape)
        .border(1.dp, glassBorderBrush(), shape)
        .clip(shape)
}

/** The vertical "lit edge" gradient used by every glass surface and its border brush. */
@Composable
fun glassBorderBrush(): Brush {
    val c = AreIptvTheme.colors
    return Brush.verticalGradient(listOf(c.glassHighlight, c.borderGlass))
}

/**
 * The accent-gradient fill for the SELECTED / current state (design §6b: "accent-gradient glass
 * chip, denser than plain focus"). A glossy lighter-top -> accent-bottom sheen that separates a
 * flat *selection* from the *focus* ring -- the piece that was missing everywhere and made active
 * items read as the old flat wash. Pass to [TvFocusable]'s `backgroundBrush`.
 */
@Composable
fun accentGradientBrush(): Brush {
    val c = AreIptvTheme.colors
    return Brush.verticalGradient(listOf(c.accentHover, c.accent))
}
