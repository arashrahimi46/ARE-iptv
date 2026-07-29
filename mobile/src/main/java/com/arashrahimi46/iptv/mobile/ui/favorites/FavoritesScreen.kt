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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.arashrahimi46.iptv.ui.components.AreChannelTile
import com.arashrahimi46.iptv.ui.components.ArePosterTile
import com.arashrahimi46.iptv.ui.components.AreSegmentedControl
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme

/** Dedicated Favorites destination -- not on the bottom bar (5 items there already: Home/Live/
 * Movies/Series/Settings; a 6th would crowd it), reached from a Settings row instead. Channels /
 * Movies / Series segments, mirroring :tv's FavoritesScreen shape with the same [AreSegmentedControl]
 * (D-pad Left/Right there, a tap here -- the control itself is touch-usable as-is). */
@Composable
fun FavoritesScreen(
    onOpenChannel: (Channel) -> Unit,
    onOpenTitle: (VodTitle) -> Unit,
    viewModel: FavoritesViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val colors = AreIptvTheme.colors
    var tab by remember { mutableStateOf(0) }
    val titles = listOf(
        stringResource(R.string.favorites_tab_channels),
        stringResource(R.string.favorites_tab_movies),
        stringResource(R.string.favorites_tab_series),
    )

    if (!state.hasSource) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.favorites_no_source), style = AreIptvTheme.typography.body, color = colors.textSecondary)
        }
        return
    }

    Column(Modifier.fillMaxSize()) {
        AreSegmentedControl(
            options = titles.indices.toList(),
            selected = tab,
            label = { titles[it] },
            onSelect = { tab = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        )
        when (tab) {
            0 -> ChannelGrid(state.channels, stringResource(R.string.favorites_empty_channels), onOpenChannel, viewModel::toggleChannelFavorite)
            1 -> TitleGrid(state.movies, stringResource(R.string.favorites_empty_movies), onOpenTitle, viewModel::toggleVodFavorite)
            2 -> TitleGrid(state.series, stringResource(R.string.favorites_empty_series), onOpenTitle, viewModel::toggleVodFavorite)
        }
    }
}

@Composable
private fun ChannelGrid(channels: List<Channel>, emptyLabel: String, onClick: (Channel) -> Unit, onToggle: (Long) -> Unit) {
    val colors = AreIptvTheme.colors
    if (channels.isEmpty()) {
        EmptyState(emptyLabel, colors.textSecondary)
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        modifier = Modifier.fillMaxWidth().fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        gridItems(channels, key = { it.id }) { channel ->
            AreChannelTile(
                channel = channel.name,
                onClick = { onClick(channel) },
                logoUrl = channel.logoUrl,
                number = channel.number,
                category = channel.categoryName,
                isRadio = channel.isRadio,
                fillWidth = true,
                isFavorite = true,
                onToggleFavorite = { onToggle(channel.id) },
            )
        }
    }
}

@Composable
private fun TitleGrid(items: List<VodTitle>, emptyLabel: String, onClick: (VodTitle) -> Unit, onToggle: (VodTitle) -> Unit) {
    val colors = AreIptvTheme.colors
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
            ArePosterTile(
                title = title.name,
                onClick = { onClick(title) },
                posterUrl = title.posterUrl,
                meta = listOfNotNull(title.year, title.categoryName).joinToString(" · ").ifBlank { null },
                rating = title.rating,
                fillWidth = true,
                isFavorite = true,
                onToggleFavorite = { onToggle(title) },
            )
        }
    }
}

@Composable
private fun EmptyState(text: String, color: androidx.compose.ui.graphics.Color) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(text, style = AreIptvTheme.typography.body, color = color)
    }
}
