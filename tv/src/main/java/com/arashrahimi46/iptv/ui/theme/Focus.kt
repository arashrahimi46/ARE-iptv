package com.arashrahimi46.iptv.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import android.graphics.BlurMaskFilter
import android.graphics.Paint as NativePaint
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The mandatory TV focus treatment: accent ring + glow + scale-up, per the
 * design system's "no element should ever be focusable without an
 * indicator" rule (tokens/base.css `:focus-visible`). Every focusable
 * component in the library composes with this modifier or [TvFocusable].
 *
 * Respects [LocalReducedMotion]: durations/scale collapse to the reduced
 * set when the CompositionLocal resolves true.
 *
 * @param interactionSource drives the focused/pressed state this modifier animates from.
 * @param shape corner shape for the ring + glow shadow (should match the surface it wraps).
 * @param glowColor tint for the ring/shadow — defaults to the theme's focus-ring color.
 * @param ringWidth stroke width of the focus ring (design system uses 3dp/2dp variants).
 * @param disableScale set true for controls (e.g. Switch) whose own thumb/track already animate.
 * @param ownsFocusable set false when a descendant composable (e.g. [androidx.compose.foundation.text.BasicTextField])
 *   already registers the focus target for this [interactionSource] -- applying `.focusable()` again here would
 *   create a second, nested focus node and break single-press D-pad focus travel. The visuals (ring/glow/scale)
 *   still track the shared [interactionSource] either way.
 */
@Composable
fun Modifier.tvFocusable(
    interactionSource: MutableInteractionSource,
    shape: Shape = RoundedCornerShape(AreIptvTheme.radius.md),
    glowColor: Color = AreIptvTheme.colors.focusRing,
    ringWidth: Dp = 3.dp,
    disableScale: Boolean = false,
    ownsFocusable: Boolean = true,
): Modifier = composed {
    val motion = AreIptvTheme.motion
    val focused by interactionSource.collectIsFocusedAsState()
    val pressed by interactionSource.collectIsPressedAsState()

    val targetScale = when {
        disableScale -> 1f
        pressed -> motion.pressScale
        focused -> motion.focusScale
        else -> 1f
    }
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(durationMillis = motion.durFastMs, easing = motion.easeEmph),
        label = "tvFocusScale",
    )
    val ringAlpha by animateFloatAsState(
        targetValue = if (focused) 1f else 0f,
        animationSpec = tween(durationMillis = motion.durFastMs, easing = motion.easeOut),
        label = "tvFocusRingAlpha",
    )

    this
        .then(if (ownsFocusable) Modifier.focusable(interactionSource = interactionSource) else Modifier)
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        // Design system `--focus-glow-tight` = `0 0 0 3px ring, 0 0 22px glow` -- a
        // SYMMETRIC (zero-offset) glow + crisp ring. Compose's Modifier.shadow is a
        // directional elevation shadow (light from the top), which on a dark
        // background rendered a colored, offset, boxy silhouette *behind* the element
        // -- the reported "extra box behind the focus ring". [tvGlow] draws the glow
        // as concentric edge-hugging outlines with no offset, matching the token.
        .then(if (ringAlpha > 0f) Modifier.tvGlow(glowColor, shape, alpha = ringAlpha) else Modifier)
        .then(
            if (ringAlpha > 0f) {
                Modifier.border(width = ringWidth, color = glowColor.copy(alpha = ringAlpha), shape = shape)
            } else {
                Modifier
            },
        )
}

/**
 * Symmetric accent glow matching the design system's box-shadow glow tokens
 * (`--focus-glow-tight`, `--glow-accent`, `--glow-live`, `--glow-smart` — all
 * `0 0 <blur>`, i.e. zero offset). Draws the [shape]'s outline several times
 * with growing stroke width and fading alpha so the halo hugs the edge and
 * fades outward — no directional offset, no boxy backplate. Place this BEFORE
 * any opaque `.background()` in the chain so the element's fill covers the
 * glow's inner half and only the outer halo shows.
 */
fun Modifier.tvGlow(
    color: Color,
    shape: Shape,
    spread: Dp = 7.dp,
    alpha: Float = 1f,
): Modifier = this.drawBehind {
    if (alpha <= 0f) return@drawBehind
    val spreadPx = spread.toPx()
    // A thin core stroke that the Gaussian blur softens -- NOT a fat stroke. A wide
    // stroke (previously == spread) made the halo huge and bleed onto neighbouring
    // text. Keep the core ~2dp; `spread` controls the blur radius (the softness).
    val strokePx = 2.dp.toPx()
    // Push the glow path OUTWARD so the blurred stroke sits just outside the shape
    // edge instead of straddling it -- otherwise the inner half bleeds INTO the
    // element ("shadow came into the button"). Any faint inner tail is covered by
    // the element's own opaque background (drawn on top).
    val out = spreadPx * 0.5f
    val path = Path().apply {
        when (val o = shape.createOutline(size, layoutDirection, this@drawBehind)) {
            is Outline.Rounded -> {
                val rr = o.roundRect
                addRoundRect(
                    RoundRect(
                        left = rr.left - out,
                        top = rr.top - out,
                        right = rr.right + out,
                        bottom = rr.bottom + out,
                        cornerRadius = CornerRadius(rr.topLeftCornerRadius.x + out, rr.topLeftCornerRadius.y + out),
                    ),
                )
            }
            is Outline.Rectangle -> addRect(o.rect.inflate(out))
            is Outline.Generic -> addPath(o.path)
        }
    }
    // One real Gaussian blur (BlurMaskFilter) = one smooth halo (no banded layered
    // strokes). minSdk 36 -> fully hardware-accelerated.
    val paint = NativePaint().apply {
        isAntiAlias = true
        style = NativePaint.Style.STROKE
        strokeWidth = strokePx
        this.color = color.copy(alpha = (0.55f * alpha).coerceIn(0f, 1f)).toArgb()
        maskFilter = BlurMaskFilter(spreadPx, BlurMaskFilter.Blur.NORMAL)
    }
    drawIntoCanvas { canvas ->
        canvas.nativeCanvas.drawPath(path.asAndroidPath(), paint)
    }
}

/**
 * Convenience wrapper composing [tvFocusable] with a click target, for the
 * common "focusable tile/card/button" pattern used across the media,
 * category and guide components.
 */
@Composable
fun TvFocusable(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = RoundedCornerShape(AreIptvTheme.radius.md),
    glowColor: Color = AreIptvTheme.colors.focusRing,
    backgroundColor: Color = Color.Transparent,
    enabled: Boolean = true,
    content: @Composable BoxScope.(focused: Boolean, pressed: Boolean) -> Unit,
) {
    val focused by interactionSource.collectIsFocusedAsState()
    val pressed by interactionSource.collectIsPressedAsState()
    Box(
        modifier = modifier
            .tvFocusable(interactionSource, shape, glowColor)
            .background(backgroundColor, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            // clickable() above handles Enter/NumPadEnter by default but NOT
            // Key.DirectionCenter -- the key every real Android TV / Fire TV remote's
            // physical OK/Select button actually sends. Confirmed missing via a real
            // device-emulator D-pad test (not an emulator artifact): DPAD_CENTER alone
            // did nothing on a fully-focused, freshly-launched card across multiple
            // isolated repro attempts, while KEYCODE_ENTER worked every time. Without
            // this, nothing in the app would be selectable with a real remote's OK
            // button -- fixed once, here, so every TvFocusable-based component in the
            // library inherits it.
            .onKeyEvent { keyEvent ->
                if (enabled && keyEvent.type == KeyEventType.KeyUp && keyEvent.key == Key.DirectionCenter) {
                    onClick()
                    true
                } else {
                    false
                }
            },
    ) {
        content(focused, pressed)
    }
}
