package com.arashrahimi46.iptv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.theme.tvGlow

/** Stream reliability level, mirrors StreamHealth.jsx `level` prop. */
enum class AreStreamHealthLevel { Stable, Moderate, Poor }

enum class AreStreamHealthSize { Small, Medium, Large }

private fun dotSize(size: AreStreamHealthSize): Dp = when (size) {
    AreStreamHealthSize.Small -> 8.dp
    AreStreamHealthSize.Medium -> 11.dp
    AreStreamHealthSize.Large -> 14.dp
}

/**
 * StreamHealth — traffic-light indicator for stream reliability
 * (StreamHealth.jsx): green stable / amber moderate / red poor.
 */
@Composable
fun AreStreamHealth(
    level: AreStreamHealthLevel,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true,
    bitrate: String? = null,
    size: AreStreamHealthSize = AreStreamHealthSize.Medium,
) {
    val colors = AreIptvTheme.colors
    val (color, label) = when (level) {
        AreStreamHealthLevel.Stable -> colors.healthStable to "Stable"
        AreStreamHealthLevel.Moderate -> colors.healthModerate to "Moderate"
        AreStreamHealthLevel.Poor -> colors.healthPoor to "Poor"
    }
    val d = dotSize(size)

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(d)
                .tvGlow(color, CircleShape, spread = 5.dp)
                .background(color, CircleShape),
        )
        if (showLabel) {
            Box(Modifier.padding(start = 8.dp)) {
                Text(text = label, style = AreIptvTheme.typography.caption, color = colors.textSecondary)
            }
        }
        if (bitrate != null) {
            Box(Modifier.padding(start = 8.dp)) {
                Text(text = bitrate, style = AreIptvTheme.typography.mono, color = colors.textTertiary)
            }
        }
    }
}

@Preview(widthDp = 500, heightDp = 140, showBackground = true)
@Composable
private fun AreStreamHealthPreview() {
    AreIptvTheme {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp),
        ) {
            AreStreamHealth(level = AreStreamHealthLevel.Stable, bitrate = "8.2 Mbps")
            AreStreamHealth(level = AreStreamHealthLevel.Moderate)
            AreStreamHealth(level = AreStreamHealthLevel.Poor)
        }
    }
}
