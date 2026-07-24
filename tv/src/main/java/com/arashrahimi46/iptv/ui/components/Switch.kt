package com.arashrahimi46.iptv.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.theme.TvFocusable
import com.arashrahimi46.iptv.ui.theme.accentGradientBrush
import com.arashrahimi46.iptv.ui.theme.tvGlow

/** Switch — on/off toggle (theme, parental lock, PiP, autoplay) (Switch.jsx). */
@Composable
fun AreSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    disabled: Boolean = false,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val colors = AreIptvTheme.colors
    val motion = AreIptvTheme.motion
    // ON = accent-gradient track + soft accent glow (design §5 Toggle); OFF = neutral surface.
    val onBrush = if (checked && !disabled) accentGradientBrush() else null
    val offColor = if (disabled) colors.surface3.copy(alpha = 0.5f) else colors.surface3
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 28.dp else 4.dp,
        animationSpec = tween(motion.durFastMs, easing = motion.easeEmph),
        label = "switchThumb",
    )

    TvFocusable(
        onClick = { if (!disabled) onCheckedChange(!checked) },
        // P0.3: Role.Switch + toggled state so TalkBack announces this as a switch and
        // reads its on/off state -- previously just a focusable/clickable Box with no
        // semantics of its own beyond that.
        modifier = modifier
            .then(if (onBrush != null) Modifier.tvGlow(colors.accent, CircleShape) else Modifier)
            .size(width = 58.dp, height = 34.dp)
            .semantics {
                role = Role.Switch
                toggleableState = if (checked) ToggleableState.On else ToggleableState.Off
            },
        interactionSource = interactionSource,
        shape = CircleShape,
        backgroundColor = if (checked && !disabled) Color.Transparent else if (disabled && checked) colors.accent.copy(alpha = 0.5f) else offColor,
        backgroundBrush = onBrush,
        enabled = !disabled,
    ) { _, _ ->
        Box(Modifier.padding(4.dp)) {
            Box(
                modifier = Modifier
                    .offset(x = thumbOffset - 4.dp)
                    .size(26.dp)
                    .shadow(4.dp, CircleShape)
                    .background(Color.White, CircleShape),
                contentAlignment = Alignment.Center,
            ) {}
        }
    }
}

@Preview(widthDp = 300, heightDp = 140, showBackground = true)
@Composable
private fun AreSwitchPreview() {
    AreIptvTheme {
        Box(Modifier.padding(24.dp)) {
            AreSwitch(checked = true, onCheckedChange = {})
        }
    }
}
