package com.arashrahimi46.iptv.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.arashrahimi46.iptv.data.model.Channel
import com.arashrahimi46.iptv.data.model.VodTitle
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

@Composable
fun PosterTile(title: VodTitle, modifier: Modifier = Modifier, onClick: () -> Unit) {
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
                .background(colors.surface2),
        ) {
            AsyncImage(
                model = title.posterUrl,
                contentDescription = title.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
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
fun ChannelTile(channel: Channel, modifier: Modifier = Modifier, onClick: () -> Unit) {
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
fun PosterRow(titles: List<VodTitle>, onClick: (VodTitle) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(titles, key = { it.id }) { PosterTile(it, onClick = { onClick(it) }) }
    }
}

@Composable
fun ChannelRow(channels: List<Channel>, onClick: (Channel) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(channels, key = { it.id }) { ChannelTile(it, onClick = { onClick(it) }) }
    }
}
