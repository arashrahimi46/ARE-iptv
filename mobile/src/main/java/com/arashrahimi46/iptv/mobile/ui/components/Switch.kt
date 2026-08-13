package com.arashrahimi46.iptv.mobile.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.arashrahimi46.iptv.mobile.design.AreIptvTheme
import com.arashrahimi46.iptv.mobile.design.accentLensBrush
import com.arashrahimi46.iptv.mobile.design.glassBorderBrush
import com.arashrahimi46.iptv.mobile.design.lensBorderBrush

private val TrackWidth = 58.dp
private val TrackHeight = 34.dp
private val ThumbSize = 26.dp
private val ThumbOff = 4.dp
private val ThumbOn = 28.dp

/**
 * The glass switch. ON is the accent LENS (selection, not an action -- ground rule 8); OFF is the
 * plain glass track.
 *
 * Draggable as well as tappable: a phone user expects to be able to throw the thumb across. A drag
 * shorter than the platform touch slop never starts, so it falls through to the tap.
 *
 * The thumb is laid out direction-aware (`CenterStart` + `offset`, both of which mirror under RTL),
 * so the DRAG has to travel on the same start-relative axis. Pointer deltas are raw screen px
 * (positive == rightward in every locale), hence `reverseDirection = isRtl`: without it, dragging
 * toward the visual "on" end in fa/ar turned the switch off.
 */
@Composable
fun AreSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
) {
    val colors = AreIptvTheme.colors
    val motion = AreIptvTheme.motion
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val density = LocalDensity.current
    val slop = LocalViewConfiguration.current.touchSlop
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val travelPx = with(density) { (ThumbOn - ThumbOff).toPx() }

    // Accumulated drag, in px along the START->END axis (see the KDoc). Reset on every gesture.
    var dragPx by remember { mutableFloatStateOf(0f) }
    val draggedOn = remember(checked) { checked }

    val target = if (checked) ThumbOn else ThumbOff
    val thumbX by animateDpAsState(
        targetValue = target,
        animationSpec = tween(motion.durFastMs, easing = motion.easeEmph),
        label = "are-switch-thumb",
    )

    val trackBrush = if (checked) accentLensBrush() else null
    // ON is an accent lens and reads on both themes. OFF is plain glass, and the glass hairline is a
    // light-on-dark sheen -- on the off-white light page the OFF track lost its outline entirely.
    // Same swap PosterTile/ChannelTile make: light gets the solid `borderDefault` edge (null brush
    // below), dark keeps the lit gradient.
    val borderBrush: Brush? = when {
        checked -> lensBorderBrush()
        AreIptvTheme.isDark -> glassBorderBrush()
        else -> null
    }

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                interactionSource = source,
                indication = ripple(bounded = false, radius = 24.dp, color = colors.accent.copy(alpha = 0.24f)),
                onValueChange = onCheckedChange,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .width(TrackWidth)
                .height(TrackHeight)
                .then(
                    if (trackBrush != null) {
                        Modifier.background(trackBrush, CircleShape)
                    } else {
                        Modifier.background(colors.glassTrackTint, CircleShape)
                    },
                )
                .then(
                    if (borderBrush != null) {
                        Modifier.border(1.dp, borderBrush, CircleShape)
                    } else {
                        Modifier.border(1.dp, colors.borderDefault, CircleShape)
                    },
                )
                .clip(CircleShape)
                .draggable(
                    orientation = Orientation.Horizontal,
                    enabled = enabled,
                    // Deltas arrive already flipped, so `dragPx` is start-relative like the thumb.
                    reverseDirection = isRtl,
                    state = rememberDraggableState { delta -> dragPx += delta },
                    onDragStopped = {
                        val restPx = if (draggedOn) travelPx else 0f
                        val moved = kotlin.math.abs(dragPx) > slop
                        if (moved) {
                            val settled = (restPx + dragPx) > travelPx / 2f
                            if (settled != checked) onCheckedChange(settled)
                        }
                        dragPx = 0f
                    },
                ),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = thumbX)
                    .size(ThumbSize)
                    .background(
                        if (checked) colors.textPrimary else colors.textSecondary,
                        CircleShape,
                    ),
            )
        }
    }
}
