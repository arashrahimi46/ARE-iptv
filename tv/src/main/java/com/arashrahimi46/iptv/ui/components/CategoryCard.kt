package com.arashrahimi46.iptv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Theaters
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.theme.TvFocusable

/** Content kind, drives the folder icon/watermark (CategoryCard.jsx `kind`). */
enum class AreCategoryKind(val icon: ImageVector, val defaultUnit: String) {
    Live(Icons.Filled.LiveTv, "channels"),
    Movies(Icons.Filled.Movie, "movies"),
    Series(Icons.Filled.Theaters, "series"),
    Guide(Icons.Filled.TableChart, "channels"),
    Default(Icons.Filled.Folder, "items"),
}

/**
 * CategoryCard — folder-style browse tile for an IPTV category (CategoryCard.jsx).
 * Artwork is optional and often absent in real playlists, so the no-image state
 * is a first-class clean folder tile (kind-icon watermark + label).
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AreCategoryCard(
    name: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    count: Int? = null,
    kind: AreCategoryKind = AreCategoryKind.Default,
    smart: Boolean = false,
    compact: Boolean = false,
    width: Dp = AreIptvTheme.spacing.tileLandWidth,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val colors = AreIptvTheme.colors
    val shape = RoundedCornerShape(AreIptvTheme.radius.md)

    TvFocusable(
        onClick = onClick,
        modifier = modifier.width(width).aspectRatio(if (compact) 16f / 7f else 16f / 10f),
        interactionSource = interactionSource,
        shape = shape,
        backgroundColor = colors.surface2,
    ) { _, _ ->
        Box(Modifier.fillMaxSize()) {
            Icon(
                kind.icon,
                contentDescription = null,
                tint = colors.textPrimary.copy(alpha = 0.05f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(if (compact) 96.dp else 132.dp),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
                    .size(if (compact) 30.dp else 36.dp)
                    .background(colors.accentWash, RoundedCornerShape(AreIptvTheme.radius.sm)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(kind.icon, contentDescription = null, tint = colors.accent, modifier = Modifier.size(if (compact) 16.dp else 20.dp))
            }
            if (smart) {
                Box(Modifier.align(Alignment.TopEnd).padding(10.dp)) {
                    AreBadge("Smart", tone = AreBadgeTone.Smart, glow = true)
                }
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 10.dp)
                    .padding(bottom = if (compact) 10.dp else 12.dp),
            ) {
                Text(
                    text = name,
                    style = if (compact) AreIptvTheme.typography.label else AreIptvTheme.typography.h3,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (count != null) {
                    Text(
                        text = "$count ${kind.defaultUnit}",
                        style = AreIptvTheme.typography.caption,
                        color = colors.textSecondary,
                    )
                }
            }
        }
    }
}

@Preview(widthDp = 700, heightDp = 260, showBackground = true)
@Composable
private fun AreCategoryCardPreview() {
    AreIptvTheme {
        Row(modifier = Modifier.padding(24.dp)) {
            AreCategoryCard(name = "Sports", onClick = {}, count = 42, kind = AreCategoryKind.Live, smart = true)
        }
    }
}
