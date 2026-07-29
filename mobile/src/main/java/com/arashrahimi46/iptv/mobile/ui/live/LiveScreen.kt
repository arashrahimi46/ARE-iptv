package com.arashrahimi46.iptv.mobile.ui.live

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import com.arashrahimi46.iptv.mobile.R
import com.arashrahimi46.iptv.data.model.Channel
import com.arashrahimi46.iptv.mobile.ui.components.FavoriteToggleButton
import com.arashrahimi46.iptv.mobile.ui.theme.AreIptvMobileTheme

@Composable
fun LiveScreen(onOpenChannel: (Channel) -> Unit, viewModel: LiveViewModel = viewModel()) {
    val categories by viewModel.categories.collectAsState()
    val selected by viewModel.selectedCategory.collectAsState()
    val query by viewModel.query.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val pagingItems = viewModel.channels.collectAsLazyPagingItems()
    val favoriteChannelIds by viewModel.favoriteChannelIds.collectAsState()
    val colors = AreIptvMobileTheme.colors

    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = viewModel::setQuery,
            placeholder = { Text(stringResource(R.string.search_placeholder)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )

        if (query.length >= 2) {
            LazyColumn(Modifier.fillMaxSize()) {
                items(searchResults, key = { it.id }) { channel ->
                    ChannelRowItem(
                        channel,
                        onClick = { onOpenChannel(channel) },
                        isFavorite = channel.id in favoriteChannelIds,
                        onToggleFavorite = { viewModel.toggleFavorite(channel) },
                    )
                }
            }
            return
        }

        if (categories.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    FilterChip(
                        selected = selected == null,
                        onClick = { viewModel.selectCategory(null) },
                        label = { Text(stringResource(R.string.search_scope_all)) },
                    )
                }
                items(categories) { name ->
                    FilterChip(
                        selected = selected == name,
                        onClick = { viewModel.selectCategory(name) },
                        label = { Text(name) },
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 52.dp),
        ) {
            items(
                count = pagingItems.itemCount,
                key = { index -> pagingItems.peek(index)?.id ?: index },
            ) { index ->
                val channel = pagingItems[index] ?: return@items
                ChannelRowItem(
                    channel,
                    onClick = { onOpenChannel(channel) },
                    isFavorite = channel.id in favoriteChannelIds,
                    onToggleFavorite = { viewModel.toggleFavorite(channel) },
                )
            }
        }
    }
}

@Composable
private fun ChannelRowItem(channel: Channel, onClick: () -> Unit, isFavorite: Boolean, onToggleFavorite: () -> Unit) {
    val colors = AreIptvMobileTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.logoWell),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = channel.logoUrl,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .aspectRatio(1f)
                    .padding(6.dp),
            )
        }
        Column(Modifier.weight(1f)) {
            Text(channel.name, style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            channel.categoryName?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = colors.textTertiary, maxLines = 1)
            }
        }
        IconButton(onClick = onToggleFavorite, modifier = Modifier.size(48.dp)) {
            Icon(
                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = stringResource(
                    if (isFavorite) R.string.detail_remove_from_favorites else R.string.detail_add_to_favorites,
                ),
                tint = if (isFavorite) colors.accent else colors.textTertiary,
            )
        }
    }
}
