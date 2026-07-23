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
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import com.arashrahimi46.iptv.R
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
    /** Grid mode: fill the cell width (set by [GridCells.Adaptive]) instead of a fixed [width], so
     * browse grids pack a responsive number of columns rather than one fixed-width tile per cell. */
    fillWidth: Boolean = false,
    badges: @Composable (() -> Unit)? = null,
    /** Null hides the favorite affordance entirely -- only screens wired to real favorites persistence pass this. */
    isFavorite: Boolean? = null,
    onToggleFavorite: (() -> Unit)? = null,
    /** Hold-OK handler -- e.g. Continue Watching uses it to open a "Remove from Continue Watching" dialog. */
    onLongClick: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    /** Attached to the focusable poster (not the outer Column) so a caller can restore D-pad
     * focus onto the exact tile -- e.g. Back out of Detail/player (see rememberPlaybackFocusRequester). */
    focusRequester: androidx.compose.ui.focus.FocusRequester? = null,
) {
    val colors = AreIptvTheme.colors
    val shape = RoundedCornerShape(AreIptvTheme.radius.md)
    val initials = title.split(" ").take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")
    val focused by interactionSource.collectIsFocusedAsState()

    // Bring the WHOLE tile (poster + title + meta) into view on focus -- the focusable is only the
    // poster, so without this the grid scrolls to show the poster but leaves the title/meta below it
    // clipped at the screen edge (and the 1.06x focus scale pushes them further down).
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    LaunchedEffect(focused) {
        if (focused) bringIntoViewRequester.bringIntoView()
    }

    Column(
        modifier = modifier
            .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier.width(width))
            .bringIntoViewRequester(bringIntoViewRequester),
    ) {
        TvFocusable(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
            interactionSource = interactionSource,
            shape = shape,
            backgroundColor = colors.surface3,
            onLongClick = onLongClick,
        ) { _, _ ->
            Box(Modifier.fillMaxSize()) {
                // Initials show only until the poster resolves (posters can be letterboxed or
                // have transparent edges, so a permanent initials layer would bleed through).
                // onState is a lightweight callback, NOT subcomposition (SubcomposeAsyncImage) --
                // so lazy-grid scroll/focus stays smooth.
                val posterLoaded = remember(posterUrl) { mutableStateOf(false) }
                if (posterUrl == null || !posterLoaded.value) {
                    Text(
                        text = initials,
                        style = AreIptvTheme.typography.display,
                        color = colors.textTertiary,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
                if (posterUrl != null) {
                    AsyncImage(
                        model = posterUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        onState = { posterLoaded.value = it is AsyncImagePainter.State.Success },
                        modifier = Modifier.fillMaxSize().clip(shape),
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
                    // Inset from the poster edges and rounded into a pill so the bar floats inside
                    // the rounded tile instead of butting square corners against the rounded image.
                    val barShape = RoundedCornerShape(AreIptvTheme.radius.pill)
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp)
                            .height(5.dp)
                            .clip(barShape)
                            .background(Color.Black.copy(alpha = 0.45f)),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress.coerceIn(0f, 1f))
                                .fillMaxSize()
                                .clip(barShape)
                                .background(colors.accent),
                        )
                    }
                }
                if (onToggleFavorite != null) {
                    AreIconButton(
                        icon = if (isFavorite == true) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = if (isFavorite == true) stringResource(R.string.detail_remove_from_favorites) else stringResource(R.string.detail_add_to_favorites),
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
            Text(
                text = meta,
                style = AreIptvTheme.typography.caption,
                color = colors.textTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
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
