package com.arashrahimi46.iptv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme

/** Status tone, mirrors Badge.jsx `tone` prop. */
enum class AreBadgeTone { Live, New, Quality, Catchup, Smart, Neutral }

/**
 * Badge — tiny status overline (Badge.jsx). LIVE and SMART glow when
 * [glow] is true.
 */
@Composable
fun AreBadge(
    text: String,
    modifier: Modifier = Modifier,
    tone: AreBadgeTone = AreBadgeTone.Neutral,
    glow: Boolean = false,
) {
    val colors = AreIptvTheme.colors
    val shape = RoundedCornerShape(AreIptvTheme.radius.xs)

    data class ToneStyle(val bg: Color, val fg: Color, val border: Color?, val glowColor: Color?)

    val style = when (tone) {
        AreBadgeTone.Live -> ToneStyle(colors.live, Color.White, null, colors.live)
        AreBadgeTone.New -> ToneStyle(colors.accent, Color.White, null, colors.accent)
        AreBadgeTone.Quality -> ToneStyle(colors.surfaceGlass, colors.textPrimary, colors.borderStrong, null)
        AreBadgeTone.Catchup -> ToneStyle(colors.success.copy(alpha = 0.16f), colors.catchupText, colors.success.copy(alpha = 0.4f), null)
        AreBadgeTone.Smart -> ToneStyle(colors.smart.copy(alpha = 0.16f), colors.violetText, colors.smart.copy(alpha = 0.45f), colors.smart)
        AreBadgeTone.Neutral -> ToneStyle(colors.surface2, colors.textSecondary, colors.borderDefault, null)
    }

    Row(
        modifier = modifier
            .height(22.dp)
            .background(style.bg, shape)
            .then(if (style.border != null) Modifier.border(1.dp, style.border, shape) else Modifier)
            .then(
                if (glow && style.glowColor != null) {
                    Modifier.shadow(10.dp, shape, ambientColor = style.glowColor, spotColor = style.glowColor)
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (tone == AreBadgeTone.Live) {
            Box(Modifier.size(6.dp).background(Color.White, CircleShape))
        }
        Text(text = text.uppercase(), style = AreIptvTheme.typography.caption, color = style.fg)
    }
}

@Preview(widthDp = 600, heightDp = 100, showBackground = true)
@Composable
private fun AreBadgePreview() {
    AreIptvTheme {
        Row(
            modifier = Modifier.padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AreBadge("Live", tone = AreBadgeTone.Live, glow = true)
            AreBadge("New", tone = AreBadgeTone.New)
            AreBadge("4K", tone = AreBadgeTone.Quality)
            AreBadge("Catch-up", tone = AreBadgeTone.Catchup)
            AreBadge("Smart", tone = AreBadgeTone.Smart, glow = true)
        }
    }
}
