package com.arashrahimi46.iptv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme

/**
 * PlayerControls — glass transport HUD overlaid on live video / VOD
 * (PlayerControls.jsx). Shows program title + now/next, a TimeShift-aware
 * seek bar (buffered region vs live edge), transport buttons and quick actions.
 */
@Composable
fun ArePlayerControls(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    live: Boolean = true,
    playing: Boolean = true,
    position: Float = 0.62f,
    buffered: Float = 0.8f,
    elapsed: String = "20:28",
    total: String = "20:45",
    channelLogoInitials: String = "SKY",
    onPlayPause: () -> Unit = {},
    onRewind: () -> Unit = {},
    onFastForward: () -> Unit = {},
    onJumpToLive: () -> Unit = {},
    onOpenGuide: () -> Unit = {},
    onMultiView: () -> Unit = {},
) {
    val colors = AreIptvTheme.colors

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surfaceGlass, RoundedCornerShape(AreIptvTheme.radius.xl))
            .border(1.dp, colors.borderDefault, RoundedCornerShape(AreIptvTheme.radius.xl))
            .padding(AreIptvTheme.spacing.sp6),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(colors.surfaceOverlay, RoundedCornerShape(AreIptvTheme.radius.sm))
                    .border(1.dp, colors.borderDefault, RoundedCornerShape(AreIptvTheme.radius.sm)),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = channelLogoInitials, style = AreIptvTheme.typography.label, color = colors.textPrimary)
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (live) {
                        AreBadge("Live", tone = AreBadgeTone.Live)
                    }
                    Text(
                        text = title,
                        style = AreIptvTheme.typography.h3,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (subtitle != null) {
                    Text(text = subtitle, style = AreIptvTheme.typography.caption, color = colors.textSecondary)
                }
            }
            Text(
                text = "$elapsed / $total",
                style = AreIptvTheme.typography.mono,
                color = colors.textSecondary,
            )
        }

        Box(Modifier.height(AreIptvTheme.spacing.sp4))

        // TimeShift seek bar
        Box(Modifier.fillMaxWidth().height(6.dp).background(colors.surface3, RoundedCornerShape(3.dp))) {
            Box(
                Modifier
                    .fillMaxWidth(buffered.coerceIn(0f, 1f))
                    .height(6.dp)
                    .background(colors.borderStrong, RoundedCornerShape(3.dp)),
            )
            Box(
                Modifier
                    .fillMaxWidth(position.coerceIn(0f, 1f))
                    .height(6.dp)
                    .background(colors.accent, RoundedCornerShape(3.dp)),
            )
        }

        Box(Modifier.height(AreIptvTheme.spacing.sp4))

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AreIconButton(Icons.Filled.FastRewind, "Rewind", onClick = onRewind, variant = AreIconButtonVariant.Glass)
            AreIconButton(
                if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                if (playing) "Pause" else "Play",
                onClick = onPlayPause,
                variant = AreIconButtonVariant.Glass,
                size = AreIconButtonSize.Large,
                active = true,
            )
            AreIconButton(Icons.Filled.FastForward, "Fast forward", onClick = onFastForward, variant = AreIconButtonVariant.Glass)
            Box(Modifier.width(1.dp).height(32.dp).background(colors.borderDefault))
            AreIconButton(Icons.Filled.SkipNext, "Jump to live", onClick = onJumpToLive, variant = AreIconButtonVariant.Glass)
            Box(Modifier.weight(1f))
            AreIconButton(Icons.Filled.VolumeUp, "Audio track", onClick = {}, variant = AreIconButtonVariant.Glass)
            AreIconButton(Icons.Filled.ClosedCaption, "Subtitles", onClick = {}, variant = AreIconButtonVariant.Glass)
            AreIconButton(Icons.Filled.ViewColumn, "Multi-view", onClick = onMultiView, variant = AreIconButtonVariant.Glass)
            AreIconButton(Icons.Filled.PictureInPicture, "Picture in picture", onClick = {}, variant = AreIconButtonVariant.Glass)
            AreIconButton(Icons.Filled.GridView, "Open guide", onClick = onOpenGuide, variant = AreIconButtonVariant.Glass)
        }
    }
}

@Preview(widthDp = 1400, heightDp = 260, showBackground = true, backgroundColor = 0xFF06070A)
@Composable
private fun ArePlayerControlsPreview() {
    AreIptvTheme {
        Box(Modifier.padding(24.dp)) {
            ArePlayerControls(title = "Sky Sports Main Event", subtitle = "Premier League: Arsenal vs Chelsea")
        }
    }
}
