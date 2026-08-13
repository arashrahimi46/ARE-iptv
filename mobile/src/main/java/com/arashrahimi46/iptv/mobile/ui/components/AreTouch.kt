package com.arashrahimi46.iptv.mobile.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arashrahimi46.iptv.mobile.design.AreIptvTheme
import com.arashrahimi46.iptv.mobile.design.ControlSkin
import com.arashrahimi46.iptv.mobile.design.softShadow

/**
 * Phone press surface. Ripple-indicated, 48dp-guaranteed, no focus machinery.
 *
 * Draw order: touch target -> scale -> softShadow -> fill -> border -> clip -> combinedClickable.
 * `pressScale` (0.97) sits UNDER the ripple; it is the brand's press feel and is switched off by
 * [disableScale] on large surfaces (tiles use the ripple alone).
 *
 * The 48dp guarantee comes from Material 3's [minimumInteractiveComponentSize], which expands the
 * POINTER bounds and the layout footprint while leaving the drawn size alone -- so a 34dp chip
 * keeps its 34dp look and its ripple hugs the pill instead of a 48dp square. Pass
 * `minTouchTarget = 0.dp` for a control already >= 48dp on both axes.
 *
 * Deliberately NOT `Modifier.composed {}` (see the PERF note in `ui/theme/Glass.kt`), and
 * deliberately free of `focusable()` / `FocusRequester` / `bringIntoViewRequester` /
 * `collectIsFocusedAsState` -- a touch surface has none of those.
 */
@Composable
fun Modifier.areTouch(
    onClick: () -> Unit,
    shape: Shape = RoundedCornerShape(AreIptvTheme.radius.md),
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
    role: Role? = Role.Button,
    contentDescription: String? = null,
    interactionSource: MutableInteractionSource? = null,
    backgroundColor: Color = Color.Transparent,
    backgroundBrush: Brush? = null,
    borderColor: Color? = null,
    borderBrush: Brush? = null,
    shadowElevation: Dp = 0.dp,
    minTouchTarget: Dp = 48.dp,
    disableScale: Boolean = false,
    rippleBounded: Boolean = true,
): Modifier = areTouchSurface(
    onClick = onClick,
    shape = shape,
    enabled = enabled,
    onLongClick = onLongClick,
    onLongClickLabel = onLongClickLabel,
    role = role,
    contentDescription = contentDescription,
    interactionSource = interactionSource,
    backgroundColor = backgroundColor,
    backgroundBrush = backgroundBrush,
    borderColor = borderColor,
    borderBrush = borderBrush,
    shadowElevation = shadowElevation,
    minTouchTarget = minTouchTarget,
    disableScale = disableScale,
    rippleBounded = rippleBounded,
    onAccentFill = false,
)

/** Applies a [ControlSkin] wholesale. Every skinned control uses this, never the raw params. */
@Composable
fun Modifier.areTouch(
    onClick: () -> Unit,
    skin: ControlSkin,
    shape: Shape = RoundedCornerShape(AreIptvTheme.radius.md),
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    role: Role? = Role.Button,
    interactionSource: MutableInteractionSource? = null,
    minTouchTarget: Dp = 48.dp,
    disableScale: Boolean = false,
): Modifier = areTouchSurface(
    onClick = onClick,
    shape = shape,
    enabled = enabled,
    onLongClick = onLongClick,
    onLongClickLabel = null,
    role = role,
    contentDescription = null,
    interactionSource = interactionSource,
    backgroundColor = skin.fillColor,
    backgroundBrush = skin.fillBrush,
    borderColor = skin.borderColor,
    borderBrush = skin.borderBrush,
    shadowElevation = skin.elevation,
    minTouchTarget = minTouchTarget,
    disableScale = disableScale,
    rippleBounded = true,
    // An accent-filled control (`skin.content == accentFg`) swallows an accent ripple; it needs the
    // on-accent tint instead. Resolved here rather than at the call site so the rule lives once.
    onAccentFill = skin.content == AreIptvTheme.colors.accentFg,
)

@Composable
private fun Modifier.areTouchSurface(
    onClick: () -> Unit,
    shape: Shape,
    enabled: Boolean,
    onLongClick: (() -> Unit)?,
    onLongClickLabel: String?,
    role: Role?,
    contentDescription: String?,
    interactionSource: MutableInteractionSource?,
    backgroundColor: Color,
    backgroundBrush: Brush?,
    borderColor: Color?,
    borderBrush: Brush?,
    shadowElevation: Dp,
    minTouchTarget: Dp,
    disableScale: Boolean,
    rippleBounded: Boolean,
    onAccentFill: Boolean,
): Modifier {
    val colors = AreIptvTheme.colors
    val motion = AreIptvTheme.motion
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val haptics = LocalHapticFeedback.current

    val rippleColor = remember(onAccentFill, colors.accent, colors.textOnAccent) {
        if (onAccentFill) colors.textOnAccent.copy(alpha = 0.30f) else colors.accent.copy(alpha = 0.24f)
    }
    val indication = remember(rippleBounded, rippleColor) { ripple(bounded = rippleBounded, color = rippleColor) }

    val scale = animateFloatAsState(
        targetValue = if (!disableScale && pressed) motion.pressScale else 1f,
        animationSpec = tween(motion.durFastMs, easing = motion.easeEmph),
        label = "are-touch-press-scale",
    )

    val longPress: (() -> Unit)? = if (onLongClick == null) {
        null
    } else {
        {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            onLongClick()
        }
    }

    return this
        .then(if (minTouchTarget > 0.dp) Modifier.minimumInteractiveComponentSize() else Modifier)
        // `scale.value` is read INSIDE graphicsLayer, never unwrapped in composition -- a press must
        // animate a layer property, not recompose the control 60 times a second.
        .then(if (disableScale) Modifier else Modifier.graphicsLayer { scaleX = scale.value; scaleY = scale.value })
        .then(if (shadowElevation > 0.dp) Modifier.softShadow(shape) else Modifier)
        .then(
            when {
                backgroundBrush != null -> Modifier.background(backgroundBrush, shape)
                backgroundColor != Color.Transparent -> Modifier.background(backgroundColor, shape)
                else -> Modifier
            },
        )
        .then(
            when {
                borderBrush != null -> Modifier.border(1.dp, borderBrush, shape)
                borderColor != null -> Modifier.border(1.dp, borderColor, shape)
                else -> Modifier
            },
        )
        .clip(shape)
        .combinedClickable(
            interactionSource = source,
            indication = indication,
            enabled = enabled,
            role = role,
            onLongClick = longPress,
            onLongClickLabel = onLongClickLabel,
            onClick = onClick,
        )
        .then(
            if (contentDescription != null) {
                Modifier.semantics { this.contentDescription = contentDescription }
            } else {
                Modifier
            },
        )
}
