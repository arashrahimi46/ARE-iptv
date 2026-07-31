package com.arashrahimi46.iptv.mobile.ui.movies

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.arashrahimi46.iptv.core.R as CoreR
import com.arashrahimi46.iptv.data.model.VodTitle
import com.arashrahimi46.iptv.mobile.ui.components.AreEmptyState
import com.arashrahimi46.iptv.mobile.ui.components.AreErrorState
import com.arashrahimi46.iptv.mobile.ui.components.AreIconButton
import com.arashrahimi46.iptv.mobile.ui.components.AreIconButtonVariant
import com.arashrahimi46.iptv.mobile.ui.components.ArePosterTile
import com.arashrahimi46.iptv.mobile.ui.components.AreRefreshBox
import com.arashrahimi46.iptv.mobile.ui.components.AreScreenScaffold
import com.arashrahimi46.iptv.mobile.ui.components.AreSkeleton
import com.arashrahimi46.iptv.mobile.ui.components.AreTabs
import com.arashrahimi46.iptv.mobile.ui.components.AreTileActionSheet

/** Poster cell width the grid adapts to. Phone-width decision, never a TV tile token (D10). */
private val VodCellMin = 120.dp

/** How many skeleton cells the first load draws — roughly two screens on a phone. */
private const val SkeletonCells = 12

/**
 * Shared by the Movies and Series tabs: a scrollable category tab strip over an adaptive poster
 * grid. Touch-only — tap opens the title, long press opens [AreTileActionSheet], the tile heart
 * toggles the favourite, pull-to-refresh re-imports the active source.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun VodGridScreen(
    viewModel: VodGridViewModel,
    onOpenTitle: (VodTitle) -> Unit,
    onOpenSearch: (() -> Unit)? = null,
) {
    val categories by viewModel.categories.collectAsState()
    val selected by viewModel.selectedCategory.collectAsState()
    val favoriteVodIds by viewModel.favoriteVodIds.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val pagingItems = viewModel.items.collectAsLazyPagingItems()
    val gridState = rememberLazyGridState()

    var sheetTitle by remember { mutableStateOf<VodTitle?>(null) }

    val allLabel = stringResource(CoreR.string.search_scope_all)
    val screenTitle = stringResource(
        if (viewModel.isSeries) CoreR.string.nav_series else CoreR.string.nav_movies,
    )
    val emptyText = stringResource(
        if (viewModel.isSeries) CoreR.string.series_empty_genre else CoreR.string.movies_empty_genre,
    )
    val errorText = stringResource(CoreR.string.browse_load_error)
    val searchLabel = stringResource(CoreR.string.action_search)

    // `null` is the "All" tab; the rest are the source's own category names.
    val tabOptions = remember(categories) { listOf<String?>(null) + categories }

    AreScreenScaffold(
        title = screenTitle,
        actions = if (onOpenSearch == null) {
            null
        } else {
            {
                AreIconButton(
                    icon = Icons.Filled.Search,
                    contentDescription = searchLabel,
                    onClick = onOpenSearch,
                    variant = AreIconButtonVariant.Ghost,
                )
            }
        },
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(top = inner.calculateTopPadding())) {
            if (categories.isNotEmpty()) {
                AreTabs(
                    options = tabOptions,
                    selected = selected,
                    label = { it ?: allLabel },
                    onSelect = viewModel::selectCategory,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    scrollable = true,
                )
            }

            AreRefreshBox(
                refreshing = isRefreshing,
                // Only the VM refresh. It re-fetches the source into Room, and the Room-backed
                // PagingSource invalidates itself on write -- so calling pagingItems.refresh() too
                // fired a second, immediate reload that reset the grid to the top before the
                // network fetch had even returned, and the list then flashed again when it did.
                onRefresh = viewModel::refresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                val refreshState = pagingItems.loadState.refresh
                val gridPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 12.dp,
                    bottom = 24.dp + inner.calculateBottomPadding(),
                )

                when {
                    refreshState is LoadState.Error -> AreErrorState(
                        message = errorText,
                        modifier = Modifier.fillMaxWidth(),
                        onRetry = { pagingItems.retry() },
                    )

                    refreshState is LoadState.Loading && pagingItems.itemCount == 0 ->
                        SkeletonGrid(gridPadding)

                    pagingItems.itemCount == 0 -> AreEmptyState(
                        title = emptyText,
                        modifier = Modifier.fillMaxWidth(),
                        icon = Icons.Filled.MovieFilter,
                    )

                    else -> LazyVerticalGrid(
                        columns = GridCells.Adaptive(VodCellMin),
                        state = gridState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = gridPadding,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        items(
                            count = pagingItems.itemCount,
                            key = pagingItems.itemKey { it.id },
                            contentType = { "vod" },
                        ) { index ->
                            val title = pagingItems[index] ?: return@items
                            ArePosterTile(
                                title = title.name,
                                onClick = { onOpenTitle(title) },
                                posterUrl = title.posterUrl,
                                meta = listOfNotNull(title.year, title.categoryName)
                                    .joinToString(" · ")
                                    .ifBlank { null },
                                rating = title.rating,
                                width = null,
                                isFavorite = title.id in favoriteVodIds,
                                onToggleFavorite = { viewModel.toggleFavorite(title) },
                                onLongClick = { sheetTitle = title },
                                lockCategory = title.categoryName,
                            )
                        }

                        // Append states get a full-span footer so the grid never reflows around them.
                        val append = pagingItems.loadState.append
                        if (append is LoadState.Loading) {
                            item(span = { GridItemSpan(maxLineSpan) }, contentType = "footer") {
                                Box(Modifier.fillMaxWidth().height(72.dp).padding(top = 12.dp)) {
                                    AreSkeleton(Modifier.fillMaxWidth().height(48.dp))
                                }
                            }
                        } else if (append is LoadState.Error) {
                            item(span = { GridItemSpan(maxLineSpan) }, contentType = "footer") {
                                AreErrorState(message = errorText, onRetry = { pagingItems.retry() })
                            }
                        }
                    }
                }
            }
        }
    }

    sheetTitle?.let { title ->
        AreTileActionSheet(
            title = title.name,
            onDismiss = { sheetTitle = null },
            isFavorite = title.id in favoriteVodIds,
            onToggleFavorite = { viewModel.toggleFavorite(title) },
            onOpenDetails = { onOpenTitle(title) },
        )
    }
}

/** First-load placeholder: the same adaptive geometry as the real grid, so nothing shifts. */
@Composable
private fun SkeletonGrid(contentPadding: PaddingValues) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(VodCellMin),
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        userScrollEnabled = false,
    ) {
        items(count = SkeletonCells, contentType = { "skeleton" }) {
            AreSkeleton(Modifier.fillMaxWidth().aspectRatio(2f / 3f))
        }
    }
}
