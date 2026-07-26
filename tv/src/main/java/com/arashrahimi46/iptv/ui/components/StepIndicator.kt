package com.arashrahimi46.iptv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.theme.ControlTone
import com.arashrahimi46.iptv.ui.theme.controlSkin
import com.arashrahimi46.iptv.ui.theme.tvGlow

/**
 * StepIndicator — the onboarding wizard progress header (forms/StepIndicator.jsx).
 * Marks completed steps with a check, the current step in the accent color, and
 * upcoming steps muted, connected by a track.
 */
@Composable
fun AreStepIndicator(
    steps: List<String>,
    current: Int,
    modifier: Modifier = Modifier,
) {
    val colors = AreIptvTheme.colors
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        steps.forEachIndexed { index, label ->
            val done = index < current
            val active = index == current
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // ONE funnel for the marker's fill/border/content (see ControlSkin.kt): the current
                // step is the accent lens (§6.2, the same "selected" material as a current tab/chip),
                // an upcoming step is neutral glass at rest. A completed (done) step keeps its bespoke
                // accent-wash + check -- the funnel has no "completed" tone, so it is not routed here.
                val skin = controlSkin(ControlTone.Neutral, selected = active)
                val badgeFillBrush = if (done) null else skin.fillBrush
                val badgeFillColor = if (done) colors.accentWash else skin.fillColor
                val badgeBorderBrush = if (done) null else skin.borderBrush
                val badgeBorderColor = if (done) null else skin.borderColor
                val badgeContent = if (done) colors.accentHover else skin.content
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .then(if (active) Modifier.tvGlow(colors.accent, CircleShape) else Modifier)
                        .then(
                            if (badgeFillBrush != null) Modifier.background(badgeFillBrush, CircleShape)
                            else Modifier.background(badgeFillColor, CircleShape),
                        )
                        .then(
                            when {
                                badgeBorderBrush != null -> Modifier.border(1.dp, badgeBorderBrush, CircleShape)
                                badgeBorderColor != null -> Modifier.border(1.dp, badgeBorderColor, CircleShape)
                                else -> Modifier
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = if (done) "✓" else "${index + 1}", style = AreIptvTheme.typography.label, color = badgeContent)
                }
                Text(
                    text = label,
                    style = AreIptvTheme.typography.label,
                    color = if (active) colors.textPrimary else colors.textTertiary,
                )
            }
            if (index < steps.lastIndex) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .width(32.dp)
                        .height(2.dp)
                        .background(if (index < current) colors.accent else colors.borderDefault),
                )
            }
        }
    }
}

@Preview(widthDp = 900, heightDp = 120, showBackground = true)
@Composable
private fun AreStepIndicatorPreview() {
    AreIptvTheme {
        Box(Modifier.padding(24.dp)) {
            AreStepIndicator(steps = listOf("Source", "Credentials", "EPG", "Confirm"), current = 1)
        }
    }
}
