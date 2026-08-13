package com.arashrahimi46.iptv.mobile.ui.components

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import com.arashrahimi46.iptv.core.R as CoreR
import com.arashrahimi46.iptv.mobile.design.AreIptvTheme
import com.arashrahimi46.iptv.mobile.design.tvGlow

enum class AreStreamHealthSize { Small, Medium, Large }

private fun dotSize(size: AreStreamHealthSize): Dp = when (size) {
    AreStreamHealthSize.Small -> 8.dp
    AreStreamHealthSize.Medium -> 11.dp
    AreStreamHealthSize.Large -> 14.dp
}

/**
 * StreamHealth — traffic-light indicator for stream reliability:
 * green stable / amber moderate / red poor.
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
        AreStreamHealthLevel.Stable -> colors.healthStable to stringResource(CoreR.string.health_stable)
        AreStreamHealthLevel.Moderate -> colors.healthModerate to stringResource(CoreR.string.health_moderate)
        AreStreamHealthLevel.Poor -> colors.healthPoor to stringResource(CoreR.string.health_poor)
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
