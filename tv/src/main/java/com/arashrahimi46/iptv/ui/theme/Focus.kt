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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
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
 */
@Composable
fun Modifier.tvFocusable(
    interactionSource: MutableInteractionSource,
    shape: Shape = RoundedCornerShape(AreIptvTheme.radius.md),
    glowColor: Color = AreIptvTheme.colors.focusRing,
    ringWidth: Dp = 3.dp,
    disableScale: Boolean = false,
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
        .focusable(interactionSource = interactionSource)
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .shadow(
            elevation = if (focused) 22.dp else 0.dp,
            shape = shape,
            ambientColor = glowColor,
            spotColor = glowColor,
        )
        .then(
            if (ringAlpha > 0f) {
                Modifier.border(width = ringWidth, color = glowColor.copy(alpha = ringAlpha), shape = shape)
            } else {
                Modifier
            },
        )
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
            ),
    ) {
        content(focused, pressed)
    }
}
