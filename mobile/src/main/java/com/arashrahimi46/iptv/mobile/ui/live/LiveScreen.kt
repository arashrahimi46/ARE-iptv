package com.arashrahimi46.iptv.mobile.ui.live

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.arashrahimi46.iptv.core.R as CoreR
import com.arashrahimi46.iptv.data.model.Channel
import com.arashrahimi46.iptv.mobile.ui.components.AreChannelTile
import com.arashrahimi46.iptv.mobile.ui.components.AreEmptyState
import com.arashrahimi46.iptv.mobile.ui.components.AreErrorState
import com.arashrahimi46.iptv.mobile.ui.components.AreLoadingState
import com.arashrahimi46.iptv.mobile.ui.components.AreRefreshBox
import com.arashrahimi46.iptv.mobile.ui.components.AreScreenScaffold
import com.arashrahimi46.iptv.mobile.ui.components.AreSkeleton
import com.arashrahimi46.iptv.mobile.ui.components.AreTabs
import com.arashrahimi46.iptv.mobile.ui.components.AreTextField
import com.arashrahimi46.iptv.mobile.ui.components.AreTileActionSheet

/**
 * Minimum column width for the channel grid. `AreChannelTile` fills its column (`width = null`), so
 * this is a density decision rather than a tile size: 168dp yields two columns on every phone and
 * three on a large/landscape one, which keeps the 16:9 logo zone and the info panel legible.
 */
private val LiveChannelMinWidth = 168.dp

private val GridPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveScreen(onOpenChannel: (Channel) -> Unit, viewModel: LiveViewModel = viewModel()) {
    val categories by viewModel.categories.collectAsState()
    val selected by viewModel.selectedCategory.collectAsState()
    val query by viewModel.query.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val favoriteChannelIds by viewModel.favoriteChannelIds.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val hasSource by viewModel.hasSource.collectAsState()
    val pagingItems = viewModel.channels.collectAsLazyPagingItems()

    val keyboard = LocalSoftwareKeyboardController.current
    val gridState = rememberLazyGridState()
    val searchGridState = rememberLazyGridState()
    // The long-pressed channel, or null when the action sheet is closed.
    var sheetChannel by remember { mutableStateOf<Channel?>(null) }

    val searching = query.isNotEmpty()
    val allLabel = stringResource(CoreR.string.search_scope_all)

    // Scrolling a grid is an unambiguous "done typing" -- drop the IME so results are not squeezed
    // into the top third of the screen.
    val activeScrollState = if (searching) searchGridState else gridState
    LaunchedEffect(activeScrollState.isScrollInProgress) {
        if (activeScrollState.isScrollInProgress) keyboard?.hide()
    }

    AreScreenScaffold(title = stringResource(CoreR.string.nav_live_tv)) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(),
        ) {
            AreTextField(
                value = query,
                onValueChange = viewModel::setQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                placeholder = stringResource(CoreR.string.search_placeholder),
                leadingIcon = Icons.Filled.Search,
                trailingIcon = if (searching) Icons.Filled.Close else null,
                onTrailingIconClick = { viewModel.setQuery("") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
            )

            if (searching) {
                SearchResults(
                    results = searchResults,
                    query = query,
                    state = searchGridState,
                    favoriteChannelIds = favoriteChannelIds,
                    onOpenChannel = onOpenChannel,
                    onToggleFavorite = viewModel::toggleFavorite,
                    onLongPress = { sheetChannel = it },
                )
            } else {
                if (categories.isNotEmpty()) {
                    AreTabs(
                        options = remember(categories) { listOf<String?>(null) + categories },
                        selected = selected,
                        label = { it ?: allLabel },
                        onSelect = viewModel::selectCategory,
                        // 16dp is the horizontal inset every other tab strip in the app uses
                        // (Guide, Favorites, Search, Movies/Series, the season strip). Live was the
                        // only one passing a bare fillMaxWidth, so its track ran hard into both
                        // screen edges while every other screen's floated clear of them.
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        scrollable = true,
                    )
                }
                AreRefreshBox(
                    refreshing = isRefreshing,
                    onRefresh = viewModel::refresh,
                    modifier = Modifier.fillMaxSize(),
                    enabled = hasSource,
                ) {
                    ChannelPagingGrid(
                        pagingItems = pagingItems,
                        state = gridState,
                        hasSource = hasSource,
                        favoriteChannelIds = favoriteChannelIds,
                        onOpenChannel = onOpenChannel,
                        onToggleFavorite = viewModel::toggleFavorite,
                        onLongPress = { sheetChannel = it },
                    )
                }
            }
        }
    }

    sheetChannel?.let { channel ->
        AreTileActionSheet(
            title = channel.name,
            onDismiss = { sheetChannel = null },
            isFavorite = channel.id in favoriteChannelIds,
            onToggleFavorite = { viewModel.toggleFavorite(channel) },
            onPlay = {
                sheetChannel = null
                onOpenChannel(channel)
            },
        )
    }
}

@Composable
private fun ChannelPagingGrid(
    pagingItems: LazyPagingItems<Channel>,
    state: LazyGridState,
    hasSource: Boolean,
    favoriteChannelIds: Set<Long>,
    onOpenChannel: (Channel) -> Unit,
    onToggleFavorite: (Channel) -> Unit,
    onLongPress: (Channel) -> Unit,
) {
    val refresh = pagingItems.loadState.refresh
    when {
        !hasSource -> {
            AreEmptyState(
                title = stringResource(CoreR.string.nav_live_tv),
                modifier = Modifier.fillMaxSize(),
                message = stringResource(CoreR.string.live_no_source),
                icon = Icons.Filled.LiveTv,
            )
            return
        }
        refresh is LoadState.Error -> {
            AreErrorState(
                message = stringResource(CoreR.string.settings_refresh_failed),
                modifier = Modifier.fillMaxSize(),
                onRetry = { pagingItems.retry() },
            )
            return
        }
        refresh is LoadState.Loading -> {
            SkeletonGrid()
            return
        }
        pagingItems.itemCount == 0 -> {
            AreEmptyState(
                title = stringResource(CoreR.string.live_empty_group),
                modifier = Modifier.fillMaxSize(),
                icon = Icons.Filled.LiveTv,
            )
            return
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = LiveChannelMinWidth),
        modifier = Modifier.fillMaxSize(),
        state = state,
        contentPadding = GridPadding,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        items(
            count = pagingItems.itemCount,
            key = pagingItems.itemKey { it.id },
            contentType = { "channel" },
        ) { index ->
            val channel = pagingItems[index] ?: return@items
            ChannelCell(
                channel = channel,
                isFavorite = channel.id in favoriteChannelIds,
                onOpenChannel = onOpenChannel,
                onToggleFavorite = onToggleFavorite,
                onLongPress = onLongPress,
            )
        }
        // Append progress and retry get a full-width footer so they can never masquerade as a cell.
        when (val append = pagingItems.loadState.append) {
            is LoadState.Loading -> item(span = { GridItemSpan(maxLineSpan) }, contentType = "footer") {
                AreLoadingState(Modifier.fillMaxWidth().padding(vertical = 16.dp))
            }
            is LoadState.Error -> item(span = { GridItemSpan(maxLineSpan) }, contentType = "footer") {
                AreErrorState(
                    message = stringResource(CoreR.string.settings_refresh_failed),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    onRetry = { pagingItems.retry() },
                )
            }
            else -> Unit
        }
    }
}

@Composable
private fun SearchResults(
    results: List<Channel>,
    query: String,
    state: LazyGridState,
    favoriteChannelIds: Set<Long>,
    onOpenChannel: (Channel) -> Unit,
    onToggleFavorite: (Channel) -> Unit,
    onLongPress: (Channel) -> Unit,
) {
    if (results.isEmpty()) {
        AreEmptyState(
            title = stringResource(CoreR.string.search_no_results, query),
            modifier = Modifier.fillMaxSize(),
            icon = Icons.Filled.Search,
        )
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = LiveChannelMinWidth),
        modifier = Modifier.fillMaxSize(),
        state = state,
        contentPadding = GridPadding,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        items(results, key = { it.id }, contentType = { "channel" }) { channel ->
            ChannelCell(
                channel = channel,
                isFavorite = channel.id in favoriteChannelIds,
                onOpenChannel = onOpenChannel,
                onToggleFavorite = onToggleFavorite,
                onLongPress = onLongPress,
            )
        }
    }
}

@Composable
private fun ChannelCell(
    channel: Channel,
    isFavorite: Boolean,
    onOpenChannel: (Channel) -> Unit,
    onToggleFavorite: (Channel) -> Unit,
    onLongPress: (Channel) -> Unit,
) {
    AreChannelTile(
        channel = channel.name,
        onClick = { onOpenChannel(channel) },
        logoUrl = channel.logoUrl,
        number = channel.number,
        category = channel.categoryName,
        lockCategory = channel.categoryName,
        isRadio = channel.isRadio,
        catchup = channel.catchupDays > 0,
        width = null,
        isFavorite = isFavorite,
        onToggleFavorite = { onToggleFavorite(channel) },
        onLongClick = { onLongPress(channel) },
    )
}

/** Cold-start placeholder: the real grid's column geometry, so nothing jumps when data lands. */
@Composable
private fun SkeletonGrid() {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = LiveChannelMinWidth),
        modifier = Modifier.fillMaxSize(),
        contentPadding = GridPadding,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        userScrollEnabled = false,
    ) {
        items(SkeletonCells, contentType = { "skeleton" }) {
            AreSkeleton(Modifier.fillMaxWidth().aspectRatio(4f / 3f))
        }
    }
}

private val SkeletonCells = List(8) { it }
