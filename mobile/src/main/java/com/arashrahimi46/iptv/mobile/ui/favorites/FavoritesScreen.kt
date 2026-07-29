package com.arashrahimi46.iptv.mobile.ui.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arashrahimi46.iptv.data.model.Channel
import com.arashrahimi46.iptv.data.model.VodTitle
import com.arashrahimi46.iptv.mobile.R
import com.arashrahimi46.iptv.mobile.ui.components.ChannelTile
import com.arashrahimi46.iptv.mobile.ui.components.PosterTile
import com.arashrahimi46.iptv.mobile.ui.theme.AreIptvMobileTheme

/** Dedicated Favorites destination -- not on the bottom bar (5 items there already: Home/Live/
 * Movies/Series/Settings; a 6th would crowd it), reached from a Settings row instead. Channels /
 * Movies / Series tabs, mirroring :tv's FavoritesScreen shape with touch tabs instead of a D-pad
 * segmented control. */
@Composable
fun FavoritesScreen(
    onOpenChannel: (Channel) -> Unit,
    onOpenTitle: (VodTitle) -> Unit,
    viewModel: FavoritesViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val colors = AreIptvMobileTheme.colors
    var tab by remember { mutableIntStateOf(0) }
    val titles = listOf(
        stringResource(R.string.favorites_tab_channels),
        stringResource(R.string.favorites_tab_movies),
        stringResource(R.string.favorites_tab_series),
    )

    if (!state.hasSource) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.favorites_no_source), style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
        }
        return
    }

    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab) {
            titles.forEachIndexed { index, title ->
                Tab(selected = tab == index, onClick = { tab = index }, text = { Text(title) })
            }
        }
        when (tab) {
            0 -> ChannelGrid(state.channels, stringResource(R.string.favorites_empty_channels), onOpenChannel, viewModel::toggleChannelFavorite)
            1 -> TitleGrid(state.movies, stringResource(R.string.favorites_empty_movies), onOpenTitle, viewModel::toggleVodFavorite)
            2 -> TitleGrid(state.series, stringResource(R.string.favorites_empty_series), onOpenTitle, viewModel::toggleVodFavorite)
        }
    }
}

@Composable
private fun ChannelGrid(channels: List<Channel>, emptyLabel: String, onClick: (Channel) -> Unit, onToggle: (Long) -> Unit) {
    val colors = AreIptvMobileTheme.colors
    if (channels.isEmpty()) {
        EmptyState(emptyLabel, colors.textSecondary)
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 96.dp),
        modifier = Modifier.fillMaxWidth().fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        gridItems(channels, key = { it.id }) { channel ->
            ChannelTile(
                channel,
                onClick = { onClick(channel) },
                isFavorite = true,
                onToggleFavorite = { onToggle(channel.id) },
            )
        }
    }
}

@Composable
private fun TitleGrid(items: List<VodTitle>, emptyLabel: String, onClick: (VodTitle) -> Unit, onToggle: (VodTitle) -> Unit) {
    val colors = AreIptvMobileTheme.colors
    if (items.isEmpty()) {
        EmptyState(emptyLabel, colors.textSecondary)
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 110.dp),
        modifier = Modifier.fillMaxWidth().fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        gridItems(items, key = { it.id }) { title ->
            PosterTile(
                title,
                onClick = { onClick(title) },
                isFavorite = true,
                onToggleFavorite = { onToggle(title) },
            )
        }
    }
}

@Composable
private fun EmptyState(text: String, color: androidx.compose.ui.graphics.Color) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = color)
    }
}
