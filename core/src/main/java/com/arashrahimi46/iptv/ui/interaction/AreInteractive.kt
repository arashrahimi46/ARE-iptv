package com.arashrahimi46.iptv.ui.interaction

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arashrahimi46.iptv.ui.theme.LocalAreIptvMotion
import com.arashrahimi46.iptv.ui.theme.LocalAreIptvRadius
import com.arashrahimi46.iptv.ui.theme.softShadow

/**
 * The platform-agnostic half of the app's "glass" focusable/pressable surface: fill (solid or
 * gradient) + optional hairline border (solid or gradient) + optional soft shadow + a scale-on-
 * interaction animation, driven purely off an [androidx.compose.foundation.interaction]
 * [MutableInteractionSource] via [collectIsFocusedAsState]/[collectIsPressedAsState].
 *
 * This is the shared rendering core extracted from `:tv`'s `TvFocusable` -- see that composable's
 * doc comment for the design rationale (glass fill, gradient "lit edge" border, [softShadow] not a
 * platform elevation shadow). What is DELIBERATELY NOT here: any D-pad specifics (registering a
 * focus target, `DPAD_CENTER` key handling) or the accent focus RING -- those stay a TV-only
 * concern layered on top by `TvFocusable`, since a touch surface has no equivalent. Any caller that
 * wants keyboard/D-pad focusability must attach its own `.focusable(interactionSource)` (or rely on
 * [combinedClickable]'s built-in focus registration) to [modifier] before/around this composable --
 * [AreInteractive] only ever READS focus state off [interactionSource], it never claims it.
 *
 * @param backgroundBrush Optional gradient FILL. Takes precedence over [backgroundColor] when set.
 * @param shadowElevation Sentinel "this surface should lift" value (dp); >0 draws [softShadow].
 * @param borderColor Optional 1dp solid outline drawn over the fill.
 * @param borderBrush Optional gradient border (the glass "lit edge"). Takes precedence over
 *   [borderColor] when set.
 * @param disableScale Suppress the focus/press scale animation -- for controls whose own thumb/
 *   track already animate, or large surfaces where the growth would overlap neighbours.
 * @param content Rendered inside the surface, given the live (focused, pressed) state read off
 *   [interactionSource].
 */
@Composable
fun AreInteractive(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = RoundedCornerShape(LocalAreIptvRadius.current.md),
    backgroundColor: Color = Color.Transparent,
    backgroundBrush: Brush? = null,
    shadowElevation: Dp = 0.dp,
    borderColor: Color? = null,
    borderBrush: Brush? = null,
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    disableScale: Boolean = false,
    content: @Composable (focused: Boolean, pressed: Boolean) -> Unit,
) {
    val motion = LocalAreIptvMotion.current
    val focused by interactionSource.collectIsFocusedAsState()
    val pressed by interactionSource.collectIsPressedAsState()

    val targetScale = when {
        disableScale -> 1f
        pressed -> motion.pressScale
        focused -> motion.focusScale
        else -> 1f
    }
    // Deferred to draw time via the animated State, not a `by` read here -- see the PERF note on
    // `tvFocusable`'s identical tween for why: reading `.value` during composition recomposes this
    // function on every one of the ~8-15 tween frames instead of leaving it to graphicsLayer.
    val scaleState = animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(durationMillis = motion.durFastMs, easing = motion.easeEmph),
        label = "areInteractiveScale",
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                val sc = scaleState.value
                scaleX = sc
                scaleY = sc
            }
            .then(if (shadowElevation > 0.dp) Modifier.softShadow(shape) else Modifier)
            .then(
                if (backgroundBrush != null) Modifier.background(backgroundBrush, shape)
                else Modifier.background(backgroundColor, shape),
            )
            .then(
                when {
                    borderBrush != null -> Modifier.border(1.dp, borderBrush, shape)
                    borderColor != null -> Modifier.border(1.dp, borderColor, shape)
                    else -> Modifier
                },
            )
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
                onLongClick = onLongClick,
            ),
    ) {
        content(focused, pressed)
    }
}
