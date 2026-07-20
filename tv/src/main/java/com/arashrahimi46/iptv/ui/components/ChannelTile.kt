package com.arashrahimi46.iptv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.theme.TvFocusable

/**
 * ChannelTile — logo-first live-TV tile (ChannelTile.jsx). IPTV providers
 * can't reliably serve stream previews, so the tile leads with a logo/initials
 * chip over an in-card info panel: now-playing program + progress, next up,
 * and quality/health info.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AreChannelTile(
    channel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    logoUrl: String? = null,
    number: String? = null,
    now: String? = null,
    next: String? = null,
    progress: Float = 0.45f,
    health: AreStreamHealthLevel = AreStreamHealthLevel.Stable,
    quality: String? = null,
    codec: String? = null,
    catchup: Boolean = false,
    width: Dp = AreIptvTheme.spacing.tileLandWidth,
    /** Null hides the favorite affordance entirely -- only screens wired to real favorites persistence pass this. */
    isFavorite: Boolean? = null,
    onToggleFavorite: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val colors = AreIptvTheme.colors
    val shape = RoundedCornerShape(AreIptvTheme.radius.md)
    val initials = channel.replace(Regex(" ?HD$", RegexOption.IGNORE_CASE), "")
        .split(" ").take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")
    val healthColor = when (health) {
        AreStreamHealthLevel.Stable -> colors.healthStable
        AreStreamHealthLevel.Moderate -> colors.healthModerate
        AreStreamHealthLevel.Poor -> colors.healthPoor
    }

    TvFocusable(
        onClick = onClick,
        modifier = modifier.width(width),
        interactionSource = interactionSource,
        shape = shape,
        backgroundColor = colors.surface2,
    ) { focused, _ ->
        Column(Modifier.fillMaxWidth()) {
            // logo zone
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 8f),
            ) {
                Row(
                    modifier = Modifier.align(Alignment.TopStart).padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    AreBadge("Live", tone = AreBadgeTone.Live)
                    if (catchup) AreBadge("Catch-up", tone = AreBadgeTone.Catchup)
                }
                Row(
                    modifier = Modifier.align(Alignment.TopEnd).padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (quality != null) {
                        Text(
                            text = quality,
                            style = AreIptvTheme.typography.mono,
                            color = colors.accentHover,
                            modifier = Modifier
                                .background(colors.accentWash, RoundedCornerShape(AreIptvTheme.radius.xs))
                                .padding(horizontal = 7.dp, vertical = 3.dp),
                        )
                    }
                    // P0.3 (WCAG 1.4.1): stream health was color-only (dot color alone) --
                    // contentDescription exposes the same "Stable/Moderate/Poor" status
                    // AreStreamHealth's own label uses, so it's not color-only for TalkBack.
                    Box(
                        Modifier
                            .size(9.dp)
                            .background(healthColor, CircleShape)
                            .semantics { contentDescription = "Stream health: ${health.name}" },
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(62.dp)
                        .background(colors.surfaceOverlay, RoundedCornerShape(AreIptvTheme.radius.sm)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (logoUrl != null) {
                        SubcomposeAsyncImage(
                            model = logoUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize().padding(8.dp),
                        ) {
                            when (painter.state) {
                                is AsyncImagePainter.State.Success -> SubcomposeAsyncImageContent()
                                else -> Text(text = initials, style = AreIptvTheme.typography.h2, color = colors.textPrimary)
                            }
                        }
                    } else {
                        Text(text = initials, style = AreIptvTheme.typography.h2, color = colors.textPrimary)
                    }
                }
                if (onToggleFavorite != null) {
                    AreIconButton(
                        icon = if (isFavorite == true) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = if (isFavorite == true) "Remove from favorites" else "Add to favorites",
                        onClick = onToggleFavorite,
                        variant = AreIconButtonVariant.Glass,
                        size = AreIconButtonSize.Small,
                        modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
                    )
                }
            }
            // info panel
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surface1)
                    .padding(top = 12.dp, start = 12.dp, end = 12.dp, bottom = 11.dp),
            ) {
                Box(Modifier.fillMaxWidth().height(3.dp).background(Color.Black.copy(alpha = 0.5f))) {
                    Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).fillMaxSize().background(colors.accent))
                }
                Box(Modifier.height(9.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (number != null) {
                        Text(text = number, style = AreIptvTheme.typography.mono, color = colors.textTertiary)
                    }
                    Text(
                        text = channel,
                        style = AreIptvTheme.typography.tile,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).then(if (focused) Modifier.basicMarquee() else Modifier),
                    )
                }
                if (now != null) {
                    Text(
                        text = "Now · $now",
                        style = AreIptvTheme.typography.caption,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = if (focused) Modifier.basicMarquee() else Modifier,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (next != null) "Next · $next" else "",
                        style = AreIptvTheme.typography.caption,
                        color = colors.textTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).then(if (focused) Modifier.basicMarquee() else Modifier),
                    )
                    if (codec != null) {
                        Text(text = codec, style = AreIptvTheme.typography.mono, color = colors.textTertiary)
                    }
                }
            }
        }
    }
}

@Preview(widthDp = 700, heightDp = 320, showBackground = true)
@Composable
private fun AreChannelTilePreview() {
    AreIptvTheme {
        Row(modifier = Modifier.padding(24.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            AreChannelTile(
                channel = "Sky Sports HD",
                onClick = {},
                number = "101",
                now = "Premier League Live",
                next = "Match of the Day",
                quality = "1080p",
                codec = "H.264",
                catchup = true,
            )
        }
    }
}
