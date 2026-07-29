package com.arashrahimi46.iptv.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.arashrahimi46.iptv.data.model.Channel
import com.arashrahimi46.iptv.data.model.VodTitle
import com.arashrahimi46.iptv.mobile.R
import com.arashrahimi46.iptv.mobile.ui.theme.AreIptvMobileTheme

/** A titled horizontally-scrolling rail -- the phone equivalent of :tv's Home rails, sized for
 * touch/thumb scrolling rather than D-pad focus traversal. */
@Composable
fun HomeRow(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = AreIptvMobileTheme.colors.textPrimary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        content()
    }
}

/** Poster tile width, derived from the phone's own vertical budget (a Home rail row is short --
 * no chrome stack above it competing for height) rather than reused from :tv's 208dp Home-rail
 * token, which assumes a 540dp TV viewport. See CLAUDE.md's tile-sizing lesson. */
val PosterTileWidth = 110.dp
val ChannelTileWidth = 96.dp

/** Heart toggle overlaid on a tile's poster/logo. The clickable bounds are 48dp per the ≥48dp
 * tap-target rule; the visible circular badge stays smaller (32dp) so it doesn't dominate the
 * tile artwork -- only the touch area, not the drawn glyph, needs to be 48dp. */
@Composable
fun FavoriteToggleButton(isFavorite: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.45f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = stringResource(
                    if (isFavorite) R.string.detail_remove_from_favorites else R.string.detail_add_to_favorites,
                ),
                tint = androidx.compose.ui.graphics.Color.White,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
fun PosterTile(
    title: VodTitle,
    modifier: Modifier = Modifier,
    isFavorite: Boolean? = null,
    onToggleFavorite: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    val colors = AreIptvMobileTheme.colors
    Column(
        modifier = modifier
            .width(PosterTileWidth)
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.surface2)
                // surface2 is near-white in light theme, barely distinguishable from the page
                // background without this edge -- same "near-white surface needs a border" rule
                // CLAUDE.md documents for :tv, applied here too (QA-flagged gap).
                .border(1.dp, colors.borderDefault, RoundedCornerShape(10.dp)),
        ) {
            AsyncImage(
                model = title.posterUrl,
                contentDescription = title.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            if (isFavorite != null && onToggleFavorite != null) {
                FavoriteToggleButton(
                    isFavorite = isFavorite,
                    onToggle = onToggleFavorite,
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                )
            }
        }
        Text(
            text = title.name,
            style = MaterialTheme.typography.labelMedium,
            color = colors.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
fun ChannelTile(
    channel: Channel,
    modifier: Modifier = Modifier,
    isFavorite: Boolean? = null,
    onToggleFavorite: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    val colors = AreIptvMobileTheme.colors
    Column(
        modifier = modifier
            .width(ChannelTileWidth)
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.logoWell),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = channel.logoUrl,
                contentDescription = channel.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
            )
            if (isFavorite != null && onToggleFavorite != null) {
                FavoriteToggleButton(
                    isFavorite = isFavorite,
                    onToggle = onToggleFavorite,
                    modifier = Modifier.align(Alignment.TopEnd).padding(2.dp),
                )
            }
        }
        Text(
            text = channel.name,
            style = MaterialTheme.typography.labelMedium,
            color = colors.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
fun PosterRow(
    titles: List<VodTitle>,
    onClick: (VodTitle) -> Unit,
    favoriteIds: Set<Long> = emptySet(),
    onToggleFavorite: ((VodTitle) -> Unit)? = null,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(titles, key = { it.id }) { title ->
            PosterTile(
                title,
                onClick = { onClick(title) },
                isFavorite = onToggleFavorite?.let { title.id in favoriteIds },
                onToggleFavorite = onToggleFavorite?.let { { it(title) } },
            )
        }
    }
}

@Composable
fun ChannelRow(
    channels: List<Channel>,
    onClick: (Channel) -> Unit,
    favoriteIds: Set<Long> = emptySet(),
    onToggleFavorite: ((Channel) -> Unit)? = null,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(channels, key = { it.id }) { channel ->
            ChannelTile(
                channel,
                onClick = { onClick(channel) },
                isFavorite = onToggleFavorite?.let { channel.id in favoriteIds },
                onToggleFavorite = onToggleFavorite?.let { { it(channel) } },
            )
        }
    }
}
