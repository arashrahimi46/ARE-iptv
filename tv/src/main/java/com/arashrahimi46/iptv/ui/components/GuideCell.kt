package com.arashrahimi46.iptv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.theme.TvFocusable

/**
 * GuideCell — one program block in the EPG grid (GuideCell.jsx). Width is set
 * by the caller (proportional to duration). Marks the live/now program with
 * an accent edge bar; long titles simply ellipsize in Phase 0 (the design
 * source's on-focus marquee scroll is a Phase 1+ polish item — see report).
 *
 * [onFocusChange] mirrors GuideCell.jsx's `onFocusChange` prop -- the Guide
 * screen wires this into the sticky "focused-program info bar" (no tooltips
 * on TV) via a shared focus state.
 */
@Composable
fun AreGuideCell(
    title: String,
    time: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    live: Boolean = false,
    now: Boolean = false,
    catchup: Boolean = false,
    progress: Float = 0f,
    width: Dp = AreIptvTheme.spacing.guideChannelWidth,
    onFocusChange: (Boolean) -> Unit = {},
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val colors = AreIptvTheme.colors
    val shape = RoundedCornerShape(AreIptvTheme.radius.sm)
    val background = if (now) colors.accentWash else colors.surface1
    val focused by interactionSource.collectIsFocusedAsState()
    LaunchedEffect(focused) { onFocusChange(focused) }

    TvFocusable(
        onClick = onClick,
        modifier = modifier.width(width).height(AreIptvTheme.spacing.guideRowHeight),
        interactionSource = interactionSource,
        shape = shape,
        backgroundColor = background,
    ) { _, _ ->
        Box(Modifier.fillMaxWidth().fillMaxHeight()) {
            if (now) {
                Box(
                    Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxHeight()
                        .width(3.dp)
                        .background(colors.accent),
                )
            }
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = time,
                        style = AreIptvTheme.typography.mono,
                        color = if (now) colors.accentHover else colors.textTertiary,
                    )
                    // P0.3 (WCAG 1.4.1): these are color-only status cues to a sighted user
                    // (a colored dot / a glyph-only chip) -- contentDescription exposes the
                    // same status to TalkBack instead of relying on color/shape alone.
                    if (live) {
                        Box(
                            Modifier
                                .padding(start = 8.dp)
                                .size(6.dp)
                                .background(colors.live, CircleShape)
                                .semantics { contentDescription = "Live" },
                        )
                    }
                    if (catchup) {
                        Text(
                            text = " ⟲",
                            style = AreIptvTheme.typography.caption,
                            color = colors.success,
                            modifier = Modifier.semantics { contentDescription = "Catch-up available" },
                        )
                    }
                }
                Box(Modifier.height(4.dp))
                Text(
                    text = title,
                    style = AreIptvTheme.typography.label,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (now && progress > 0f) {
                Box(
                    Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .height(2.dp)
                        .background(colors.accent),
                )
            }
        }
    }
}

@Preview(widthDp = 900, heightDp = 140, showBackground = true)
@Composable
private fun AreGuideCellPreview() {
    AreIptvTheme {
        Row(modifier = Modifier.padding(24.dp)) {
            AreGuideCell(title = "Premier League Live", time = "20:00", onClick = {}, live = true, now = true, progress = 0.4f)
            AreGuideCell(title = "News at Nine", time = "21:00", onClick = {}, catchup = true)
        }
    }
}
