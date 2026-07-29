package com.arashrahimi46.iptv.mobile.ui.theme

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arashrahimi46.iptv.ui.interaction.AreInteractive

/**
 * Phone counterpart to `:tv`'s `TvFocusable`: the same shared glass rendering
 * ([AreInteractive], `:core`) with no D-pad focus target and no focus ring -- a touch surface has
 * neither. `focused` is always false (nothing here ever emits a `FocusInteraction`); `pressed`
 * comes straight off the tap. Not wired to any screen yet -- that lands with the touch UI pass.
 */
@Composable
fun AreTouchable(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = RoundedCornerShape(10.dp),
    backgroundColor: Color = Color.Transparent,
    backgroundBrush: Brush? = null,
    shadowElevation: Dp = 0.dp,
    borderColor: Color? = null,
    borderBrush: Brush? = null,
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    disableScale: Boolean = false,
    content: @Composable (pressed: Boolean) -> Unit,
) {
    AreInteractive(
        onClick = onClick,
        modifier = modifier,
        interactionSource = interactionSource,
        shape = shape,
        backgroundColor = backgroundColor,
        backgroundBrush = backgroundBrush,
        shadowElevation = shadowElevation,
        borderColor = borderColor,
        borderBrush = borderBrush,
        enabled = enabled,
        onLongClick = onLongClick,
        disableScale = disableScale,
    ) { _, pressed -> content(pressed) }
}
