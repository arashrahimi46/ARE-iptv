package com.arashrahimi46.iptv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.theme.TvFocusable

/**
 * PosterTile — portrait VOD tile for movies/series (PosterTile.jsx). Loads
 * [posterUrl] via Coil when present, falling back to an initials chip while
 * loading, on load error, or when no URL is available.
 */
@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ArePosterTile(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    posterUrl: String? = null,
    meta: String? = null,
    rating: String? = null,
    progress: Float? = null,
    width: androidx.compose.ui.unit.Dp = AreIptvTheme.spacing.tilePosterWidth,
    badges: @Composable (() -> Unit)? = null,
    /** Null hides the favorite affordance entirely -- only screens wired to real favorites persistence pass this. */
    isFavorite: Boolean? = null,
    onToggleFavorite: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val colors = AreIptvTheme.colors
    val shape = RoundedCornerShape(AreIptvTheme.radius.md)
    val initials = title.split(" ").take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")
    val focused by interactionSource.collectIsFocusedAsState()

    Column(modifier = modifier.width(width)) {
        TvFocusable(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f),
            interactionSource = interactionSource,
            shape = shape,
            backgroundColor = colors.surface3,
        ) { _, _ ->
            Box(Modifier.fillMaxSize()) {
                if (posterUrl != null) {
                    SubcomposeAsyncImage(
                        model = posterUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(shape),
                    ) {
                        when (painter.state) {
                            is AsyncImagePainter.State.Success -> SubcomposeAsyncImageContent()
                            else -> Text(
                                text = initials,
                                style = AreIptvTheme.typography.display,
                                color = colors.textTertiary,
                                modifier = Modifier.align(Alignment.Center),
                            )
                        }
                    }
                } else {
                    Text(
                        text = initials,
                        style = AreIptvTheme.typography.display,
                        color = colors.textTertiary,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
                if (badges != null) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        badges()
                    }
                }
                if (rating != null) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                            .background(colors.surfaceOverlay, RoundedCornerShape(AreIptvTheme.radius.pill))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(Icons.Filled.Star, contentDescription = null, tint = colors.ratingStar, modifier = Modifier.height(12.dp))
                        Text(text = rating, style = AreIptvTheme.typography.caption, color = colors.textPrimary)
                    }
                }
                if (progress != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(Color.Black.copy(alpha = 0.5f)),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress.coerceIn(0f, 1f))
                                .fillMaxSize()
                                .background(colors.accent),
                        )
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
        }
        Box(Modifier.height(10.dp))
        Text(
            text = title,
            style = AreIptvTheme.typography.tile,
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = if (focused) Modifier.basicMarquee() else Modifier,
        )
        if (meta != null) {
            Text(text = meta, style = AreIptvTheme.typography.caption, color = colors.textTertiary)
        }
    }
}

@Preview(widthDp = 700, heightDp = 420, showBackground = true)
@Composable
private fun ArePosterTilePreview() {
    AreIptvTheme {
        Row(modifier = Modifier.padding(24.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            ArePosterTile(title = "Dune Part Two", onClick = {}, meta = "2024 · Sci-Fi", rating = "8.6")
            ArePosterTile(title = "The Bear", onClick = {}, meta = "S3", progress = 0.4f)
        }
    }
}
