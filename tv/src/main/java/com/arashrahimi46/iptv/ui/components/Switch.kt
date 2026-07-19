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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.theme.TvFocusable

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
    val trackColor = if (checked) colors.accent else colors.surface3
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 28.dp else 4.dp,
        animationSpec = tween(motion.durFastMs, easing = motion.easeEmph),
        label = "switchThumb",
    )

    TvFocusable(
        onClick = { if (!disabled) onCheckedChange(!checked) },
        modifier = modifier.size(width = 58.dp, height = 34.dp),
        interactionSource = interactionSource,
        shape = CircleShape,
        backgroundColor = if (disabled) trackColor.copy(alpha = 0.5f) else trackColor,
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
