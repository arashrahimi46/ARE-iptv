package com.arashrahimi46.iptv.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * The one shared "glass" surface language: a translucent fill + a hairline gradient border whose
 * bright top stop reads as a lit edge (bright top -> faint bottom). The fill alpha IS the glass --
 * content behind shows through; there is deliberately NO backdrop blur here (see the design spec:
 * real blur is reserved for dialogs via the window API, and is impossible over the SurfaceView video).
 *
 * Compose [TvFocusable] / [Modifier.tvFocusable] outside this so the accent ring + glow still draw
 * on top -- focus is intentionally not baked into the glass.
 *
 * @param elevated denser fill for modals/HUD over media so text stays legible.
 */
fun Modifier.glassSurface(
    shape: Shape,
    elevated: Boolean = false,
): Modifier = composed {
    val c = AreIptvTheme.colors
    this
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
