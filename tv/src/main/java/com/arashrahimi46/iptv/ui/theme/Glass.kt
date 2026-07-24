package com.arashrahimi46.iptv.ui.theme

import android.graphics.BlurMaskFilter
import android.graphics.Paint as NativePaint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Sentinel "this surface should lift" value; the actual look comes from [softShadow], not from a
 *  hard platform elevation. Kept as a Dp so call sites read naturally (`shadowElevation = GlassElevation`). */
val GlassElevation: Dp = 6.dp

/**
 * A soft, diffuse drop shadow for the glass language -- a wide, low-alpha Gaussian blur of the shape,
 * nudged down a few dp. Deliberately NOT [androidx.compose.ui.draw.shadow]: the platform elevation
 * shadow reads too hard/dark for glass ("still strong shadows"). This is a whisper of depth that
 * mostly shows in light mode; in dark the lit edge carries the separation. Draw BEFORE the fill.
 */
fun Modifier.softShadow(
    shape: Shape,
    color: Color = Color.Black,
    alpha: Float = 0.10f,
    blur: Dp = 18.dp,
    offsetY: Dp = 4.dp,
): Modifier = drawBehind {
    if (alpha <= 0f) return@drawBehind
    val offPx = offsetY.toPx()
    val paint = NativePaint().apply {
        isAntiAlias = true
        this.color = color.copy(alpha = alpha).toArgb()
        maskFilter = BlurMaskFilter(blur.toPx(), BlurMaskFilter.Blur.NORMAL)
    }
    val path = Path().apply {
        when (val o = shape.createOutline(size, layoutDirection, this@drawBehind)) {
            is Outline.Rounded -> {
                val rr = o.roundRect
                addRoundRect(
                    RoundRect(
                        left = rr.left,
                        top = rr.top + offPx,
                        right = rr.right,
                        bottom = rr.bottom + offPx,
                        cornerRadius = CornerRadius(rr.topLeftCornerRadius.x, rr.topLeftCornerRadius.y),
                    ),
                )
            }
            is Outline.Rectangle -> addRect(o.rect.translate(Offset(0f, offPx)))
            is Outline.Generic -> addPath(o.path, Offset(0f, offPx))
        }
    }.asAndroidPath()
    drawIntoCanvas { it.nativeCanvas.drawPath(path, paint) }
}

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
        .then(if (shadow) Modifier.softShadow(shape) else Modifier)
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
