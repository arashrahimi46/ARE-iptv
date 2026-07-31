package com.arashrahimi46.iptv.mobile.ui.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arashrahimi46.iptv.core.R as CoreR
import com.arashrahimi46.iptv.data.model.Channel
import com.arashrahimi46.iptv.data.model.VodTitle
import com.arashrahimi46.iptv.mobile.ui.components.AreChannelTile
import com.arashrahimi46.iptv.mobile.ui.components.AreEmptyState
import com.arashrahimi46.iptv.mobile.ui.components.ArePosterTile
import com.arashrahimi46.iptv.mobile.ui.components.AreRefreshBox
import com.arashrahimi46.iptv.mobile.ui.components.AreScreenScaffold
import com.arashrahimi46.iptv.mobile.ui.components.AreTabs
import com.arashrahimi46.iptv.mobile.ui.components.AreTileActionSheet
import kotlinx.coroutines.launch

private const val PAGE_CHANNELS = 0
private const val PAGE_MOVIES = 1
private const val PAGE_SERIES = 2

/**
 * Favourites. A child destination (not on the bottom bar -- five tabs already live there), so it
 * carries a real Back affordance via [AreScreenScaffold].
 *
 * Touch model: the three sections are an [AreTabs] strip bound bidirectionally to a
 * [HorizontalPager], so a swipe changes section and the strip follows. `rememberPagerState` is
 * saveable, which is what preserves the section across rotation. Removing a favourite -- by the
 * tile heart or by the long-press [AreTileActionSheet] -- posts an undo snackbar; undo re-toggles,
 * and because the repository keys on the stable stream key the row returns to its old position.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    onOpenChannel: (Channel) -> Unit,
    onOpenTitle: (VodTitle) -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: FavoritesViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val refreshing by viewModel.isRefreshing.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val pagerState = rememberPagerState(pageCount = { 3 })
    val tabs = listOf(PAGE_CHANNELS, PAGE_MOVIES, PAGE_SERIES)
    val tabLabels = listOf(
        stringResource(CoreR.string.favorites_tab_channels),
        stringResource(CoreR.string.favorites_tab_movies),
        stringResource(CoreR.string.favorites_tab_series),
    )

    val removedMessage = stringResource(CoreR.string.favorites_removed)
    val undoLabel = stringResource(CoreR.string.common_undo)
    fun announceRemoval(restore: () -> Unit) {
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = removedMessage,
                actionLabel = undoLabel,
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) restore()
        }
    }

    var sheetChannel by remember { mutableStateOf<Channel?>(null) }
    var sheetTitle by remember { mutableStateOf<VodTitle?>(null) }

    AreScreenScaffold(
        title = stringResource(CoreR.string.favorites_title),
        onBack = onBack,
        snackbarHostState = snackbarHostState,
    ) { padding ->
        if (!state.hasSource) {
            AreEmptyState(
                title = stringResource(CoreR.string.favorites_title),
                message = stringResource(CoreR.string.favorites_no_source),
                icon = Icons.Filled.FavoriteBorder,
                modifier = Modifier.fillMaxSize().padding(padding),
            )
            return@AreScreenScaffold
        }

        Column(Modifier.fillMaxSize().padding(padding)) {
            AreTabs(
                options = tabs,
                selected = pagerState.currentPage,
                label = { tabLabels[it] },
                onSelect = { page -> scope.launch { pagerState.animateScrollToPage(page) } },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                AreRefreshBox(
                    refreshing = refreshing,
                    onRefresh = viewModel::refresh,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    when (page) {
                        PAGE_CHANNELS -> ChannelGrid(
                            channels = state.channels,
                            emptyMessage = stringResource(CoreR.string.favorites_empty_channels),
                            onClick = onOpenChannel,
                            onLongClick = { sheetChannel = it },
                            onRemove = { channel ->
                                viewModel.toggleChannelFavorite(channel.id)
                                announceRemoval { viewModel.restoreChannelFavorite(channel.id) }
                            },
                        )

                        else -> TitleGrid(
                            items = if (page == PAGE_MOVIES) state.movies else state.series,
                            emptyMessage = stringResource(
                                if (page == PAGE_MOVIES) {
                                    CoreR.string.favorites_empty_movies
                                } else {
                                    CoreR.string.favorites_empty_series
                                },
                            ),
                            onClick = onOpenTitle,
                            onLongClick = { sheetTitle = it },
                            onRemove = { title ->
                                viewModel.toggleVodFavorite(title)
                                announceRemoval { viewModel.restoreVodFavorite(title) }
                            },
                        )
                    }
                }
            }
        }
    }

    sheetChannel?.let { channel ->
        AreTileActionSheet(
            title = channel.name,
            onDismiss = { sheetChannel = null },
            isFavorite = true,
            onToggleFavorite = {
                viewModel.toggleChannelFavorite(channel.id)
                announceRemoval { viewModel.restoreChannelFavorite(channel.id) }
            },
            onPlay = { onOpenChannel(channel) },
        )
    }

    sheetTitle?.let { title ->
        AreTileActionSheet(
            title = title.name,
            onDismiss = { sheetTitle = null },
            isFavorite = true,
            onToggleFavorite = {
                viewModel.toggleVodFavorite(title)
                announceRemoval { viewModel.restoreVodFavorite(title) }
            },
            onOpenDetails = { onOpenTitle(title) },
        )
    }
}

/** Channel favourites. Density matches Live's grid so the two read as the same catalogue. */
@Composable
private fun ChannelGrid(
    channels: List<Channel>,
    emptyMessage: String,
    onClick: (Channel) -> Unit,
    onLongClick: (Channel) -> Unit,
    onRemove: (Channel) -> Unit,
) {
    if (channels.isEmpty()) {
        FavoritesEmpty(emptyMessage)
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 168.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 52.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        gridItems(channels, key = { it.id }, contentType = { "channel" }) { channel ->
            AreChannelTile(
                channel = channel.name,
                onClick = { onClick(channel) },
                logoUrl = channel.logoUrl,
                number = channel.number,
                category = channel.categoryName,
                lockCategory = channel.categoryName,
                isRadio = channel.isRadio,
                isFavorite = true,
                onToggleFavorite = { onRemove(channel) },
                onLongClick = { onLongClick(channel) },
            )
        }
    }
}

/** Movie/series favourites. 120dp minimum matches the Movies/Series grids. */
@Composable
private fun TitleGrid(
    items: List<VodTitle>,
    emptyMessage: String,
    onClick: (VodTitle) -> Unit,
    onLongClick: (VodTitle) -> Unit,
    onRemove: (VodTitle) -> Unit,
) {
    if (items.isEmpty()) {
        FavoritesEmpty(emptyMessage)
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 120.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 52.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        gridItems(items, key = { it.id }, contentType = { "title" }) { title ->
            ArePosterTile(
                title = title.name,
                onClick = { onClick(title) },
                posterUrl = title.posterUrl,
                meta = listOfNotNull(title.year, title.categoryName).joinToString(" · ").ifBlank { null },
                lockCategory = title.categoryName,
                rating = title.rating,
                isFavorite = true,
                onToggleFavorite = { onRemove(title) },
                onLongClick = { onLongClick(title) },
            )
        }
    }
}

/** An empty page must still accept a pull, so it is a scrollable container -- `PullToRefreshBox`
 * only sees the gesture through a child that participates in nested scroll. */
@Composable
private fun FavoritesEmpty(message: String) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
    ) {
        AreEmptyState(
            title = stringResource(CoreR.string.favorites_title),
            message = message,
            icon = Icons.Filled.FavoriteBorder,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
