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
import com.arashrahimi46.iptv.ui.theme.accentLensBrush
import com.arashrahimi46.iptv.ui.theme.glassBorderBrush
import com.arashrahimi46.iptv.ui.theme.lensBorderBrush
import com.arashrahimi46.iptv.ui.theme.tvGlow
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

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
    // ON = accent lens track; OFF = glass track (§6.1: was an opaque surface3 slug punched through
    // the glass Settings panel -- the canary defect of the spec). The ON state used to also carry a
    // `tvGlow` halo -- dropped: on a list of switches it read as a smudge around every enabled row,
    // and it is a per-draw CPU Gaussian blur. The lens fill + bright rim already say "on".
    val onBrush = if (checked && !disabled) accentLensBrush() else null
    val offColor = if (disabled) colors.glassTrackTint.copy(alpha = 0.5f) else colors.glassTrackTint
    // Held as State and read inside the offset LAMBDA, not unwrapped: `by` made this a composition
    // read, so every frame of the toggle tween recomposed AreSwitch -- TvFocusable, its whole content
    // lambda and this modifier chain -- and the non-lambda `Modifier.offset(x = Dp)` then re-ran
    // layout from composition too. The lambda form defers the read to the LAYOUT phase, and (unlike
    // `absoluteOffset`) it still mirrors under RTL, so the thumb travels the correct way in Persian.
    val thumbOffset = animateDpAsState(
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
            .size(width = 58.dp, height = 34.dp)
            .semantics {
                role = Role.Switch
                toggleableState = if (checked) ToggleableState.On else ToggleableState.Off
            },
        interactionSource = interactionSource,
        shape = CircleShape,
        backgroundColor = if (checked && !disabled) Color.Transparent else if (disabled && checked) colors.accent.copy(alpha = 0.5f) else offColor,
        backgroundBrush = onBrush,
        // ON carries the lens's bright specular rim; OFF the neutral lit edge -- the track fill is
        // translucent now, so it needs the hairline to read (esp. the near-white light theme).
        borderBrush = if (checked && !disabled) lensBorderBrush() else glassBorderBrush(),
        enabled = !disabled,
    ) { _, _ ->
        Box(Modifier.padding(4.dp)) {
            Box(
                modifier = Modifier
                    .offset { IntOffset((thumbOffset.value - 4.dp).toPx().roundToInt(), 0) }
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
