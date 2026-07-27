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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.theme.glassChild
import com.arashrahimi46.iptv.ui.theme.tvGlow

/** Status tone, mirrors Badge.jsx `tone` prop. */
enum class AreBadgeTone { Live, New, Quality, Catchup, Smart, Neutral }

/**
 * Badge — tiny status overline (Badge.jsx). LIVE and SMART glow when
 * [glow] is true.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AreBadge(
    text: String,
    modifier: Modifier = Modifier,
    tone: AreBadgeTone = AreBadgeTone.Neutral,
    glow: Boolean = false,
    /** Optional glyph before the label, tinted with the tone's foreground. */
    icon: ImageVector? = null,
) {
    val colors = AreIptvTheme.colors
    val shape = RoundedCornerShape(AreIptvTheme.radius.xs)

    // [glass] tones defer their fill + hairline to glassChild() (design §6.1) instead of a
    // surfaceGlass fill that compounds when the badge sits on a glass surface; bg/border are ignored.
    data class ToneStyle(val bg: Color, val fg: Color, val border: Color?, val glowColor: Color?, val glass: Boolean = false)

    val style = when (tone) {
        AreBadgeTone.Live -> ToneStyle(colors.live, Color.White, null, colors.live)
        AreBadgeTone.New -> ToneStyle(colors.accent, Color.White, null, colors.accent)
        AreBadgeTone.Quality -> ToneStyle(Color.Transparent, colors.textPrimary, null, null, glass = true)
        AreBadgeTone.Catchup -> ToneStyle(colors.success.copy(alpha = 0.16f), colors.catchupText, colors.success.copy(alpha = 0.4f), null)
        AreBadgeTone.Smart -> ToneStyle(colors.smart.copy(alpha = 0.16f), colors.violetText, colors.smart.copy(alpha = 0.45f), colors.smart)
        // textTertiary, not textSecondary: the glass fill is deliberately faint, so a bright label
        // on top of it reads as a solid grey slab rather than as glass. Dimming the text is what
        // actually makes the chip recede.
        AreBadgeTone.Neutral -> ToneStyle(Color.Transparent, colors.textTertiary, null, null, glass = true)
    }

    // LIVE reads as a compact status flag, not a chip — smaller than the other tones.
    // NEUTRAL joins it: it carries a provider's category ("OUTDOOR;SPORTS"), which is supporting
    // metadata under the channel name, not a status to announce. At the full 14sp it competed with
    // the name it sits beneath and dominated the tile.
    val compact = tone == AreBadgeTone.Live || tone == AreBadgeTone.Neutral

    Row(
        modifier = modifier
            .height(if (compact) 18.dp else 22.dp)
            // Glow BEFORE the background so the symmetric halo sits behind the pill,
            // not painted over its fill (design system `--glow-live`/`--glow-smart`).
            .then(
                if (glow && style.glowColor != null) {
                    Modifier.tvGlow(style.glowColor, shape, spread = 8.dp)
                } else {
                    Modifier
                },
            )
            .then(
                if (style.glass) {
                    Modifier.glassChild(shape)
                } else {
                    Modifier
                        .background(style.bg, shape)
                        .then(if (style.border != null) Modifier.border(1.dp, style.border, shape) else Modifier)
                },
            )
            .padding(horizontal = if (compact) 6.dp else 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 5.dp),
    ) {
        if (tone == AreBadgeTone.Live) {
            Box(Modifier.size(5.dp).background(Color.White, CircleShape))
        }
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, tint = style.fg, modifier = Modifier.size(12.dp))
        }
        Text(
            text = text.uppercase(),
            style = if (compact) AreIptvTheme.typography.caption.copy(fontSize = 11.sp) else AreIptvTheme.typography.caption,
            color = style.fg,
            // Provider category names can be long ("Sports · Premium UK") -- a badge is a chip, so it
            // clips rather than wraps out of its fixed height.
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
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
