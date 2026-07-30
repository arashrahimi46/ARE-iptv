package com.arashrahimi46.iptv.mobile.ui.live

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.arashrahimi46.iptv.mobile.R
import com.arashrahimi46.iptv.data.model.Channel
import com.arashrahimi46.iptv.mobile.ui.components.AreTextField
import com.arashrahimi46.iptv.ui.components.AreChannelTile
import com.arashrahimi46.iptv.ui.components.AreChip

/** Phone-appropriate minimum column width for [AreChannelTile] in grid (fillWidth) mode -- wide
 * enough for the tile's 2:1 logo zone + info panel to stay legible; :tv's Live grid uses 180dp
 * against a much larger viewport, so this keeps the same tile proportions at phone scale. */
private val LiveChannelMinWidth = 160.dp

@Composable
fun LiveScreen(onOpenChannel: (Channel) -> Unit, viewModel: LiveViewModel = viewModel()) {
    val categories by viewModel.categories.collectAsState()
    val selected by viewModel.selectedCategory.collectAsState()
    val query by viewModel.query.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val pagingItems = viewModel.channels.collectAsLazyPagingItems()
    val favoriteChannelIds by viewModel.favoriteChannelIds.collectAsState()

    Column(Modifier.fillMaxSize()) {
        AreTextField(
            value = query,
            onValueChange = viewModel::setQuery,
            placeholder = stringResource(R.string.search_placeholder),
            icon = Icons.Filled.Search,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )

        if (query.length >= 2) {
            ChannelGrid(
                channels = searchResults,
                favoriteChannelIds = favoriteChannelIds,
                onOpenChannel = onOpenChannel,
                onToggleFavorite = viewModel::toggleFavorite,
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            )
            return
        }

        if (categories.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    AreChip(
                        text = stringResource(R.string.search_scope_all),
                        selected = selected == null,
                        onClick = { viewModel.selectCategory(null) },
                    )
                }
                items(categories) { name ->
                    AreChip(text = name, selected = selected == name, onClick = { viewModel.selectCategory(name) })
                }
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = LiveChannelMinWidth),
            modifier = Modifier.fillMaxWidth().fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 52.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            gridItems(
                items = (0 until pagingItems.itemCount).toList(),
                key = { index -> pagingItems.peek(index)?.id ?: index },
            ) { index ->
                val channel = pagingItems[index] ?: return@gridItems
                AreChannelTile(
                    channel = channel.name,
                    onClick = { onOpenChannel(channel) },
                    logoUrl = channel.logoUrl,
                    number = channel.number,
                    category = channel.categoryName,
                    isRadio = channel.isRadio,
                    fillWidth = true,
                    isFavorite = channel.id in favoriteChannelIds,
                    onToggleFavorite = { viewModel.toggleFavorite(channel) },
                )
            }
        }
    }
}

@Composable
private fun ChannelGrid(
    channels: List<Channel>,
    favoriteChannelIds: Set<Long>,
    onOpenChannel: (Channel) -> Unit,
    onToggleFavorite: (Channel) -> Unit,
    contentPadding: PaddingValues,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = LiveChannelMinWidth),
        modifier = Modifier.fillMaxWidth().fillMaxSize(),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        gridItems(channels, key = { it.id }) { channel ->
            AreChannelTile(
                channel = channel.name,
                onClick = { onOpenChannel(channel) },
                logoUrl = channel.logoUrl,
                number = channel.number,
                category = channel.categoryName,
                isRadio = channel.isRadio,
                fillWidth = true,
                isFavorite = channel.id in favoriteChannelIds,
                onToggleFavorite = { onToggleFavorite(channel) },
            )
        }
    }
}
