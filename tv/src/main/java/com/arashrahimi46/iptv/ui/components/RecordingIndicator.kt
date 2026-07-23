package com.arashrahimi46.iptv.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.arashrahimi46.iptv.R
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.theme.Ink950

/**
 * "Recording in progress" badge — the camcorder metaphor: a pulsing red dot + REC + a live elapsed
 * timer, on a dark glass pill so it reads over any video. Amber "Reconnecting…" while the capture
 * stalls ([reconnecting]) so a silent drop isn't mistaken for a healthy recording. The dot's
 * bump-and-shrink pulse is dropped to a steady dot when [reducedMotion] is on (accessibility).
 *
 * Deliberately theme-independent (Ink950 pill + white text like the rest of the player HUD scrims),
 * since it always overlays video, not an app surface.
 */
@Composable
fun RecordingIndicator(
    reconnecting: Boolean,
    elapsedMs: Long,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = AreIptvTheme.colors
    val dotColor = if (reconnecting) colors.warning else colors.danger

    // Pulse the dot: scale 1.0 -> 1.4 on a ~750ms ease-in-out loop. Steady when reduced-motion.
    val scale = if (reducedMotion) {
        1f
    } else {
        val transition = rememberInfiniteTransition(label = "rec-pulse")
        val s by transition.animateFloat(
            initialValue = 1f,
            targetValue = 1.4f,
            animationSpec = infiniteRepeatable(tween(750, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "rec-dot-scale",
        )
        s
    }

    Row(
        modifier = modifier
            .background(Ink950.copy(alpha = 0.55f), RoundedCornerShape(AreIptvTheme.radius.pill))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.size(10.dp).scale(scale).background(dotColor, CircleShape))
        Text(
            text = if (reconnecting) stringResource(R.string.player_recording_reconnecting)
                else stringResource(R.string.player_recording_label),
            style = AreIptvTheme.typography.label,
            color = Color.White,
        )
        Text(
            text = formatRecElapsed(elapsedMs),
            style = AreIptvTheme.typography.label,
            color = Color.White.copy(alpha = 0.85f),
        )
    }
}

/** m:ss, or h:mm:ss once past an hour. */
private fun formatRecElapsed(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
