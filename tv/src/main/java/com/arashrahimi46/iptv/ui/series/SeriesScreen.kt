package com.arashrahimi46.iptv.ui.series

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.tv.material3.Text
import com.arashrahimi46.iptv.data.model.VodTitle
import com.arashrahimi46.iptv.data.settings.UserSettings
import com.arashrahimi46.iptv.ui.browse.BrowseCategoryOption
import com.arashrahimi46.iptv.ui.browse.BrowseLayout
import com.arashrahimi46.iptv.ui.components.ArePosterTile
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme

/**
 * Series browse (Browse.jsx): reuses the exact [BrowseLayout] shared with
 * Live TV / Movies, sourced from the real [VodTitle] catalog (isSeries = true)
 * via [SeriesViewModel]. Selecting a tile opens the real
 * [com.arashrahimi46.iptv.ui.detail.DetailScreen] for that title's own id.
 */
@Composable
fun SeriesScreen(onSeriesSelected: (VodTitle) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val viewModel: SeriesViewModel = viewModel(
        factory = SeriesViewModel.factory(context.applicationContext as android.app.Application),
    )
    val state by viewModel.uiState.collectAsState()
    val series = viewModel.pagingData.collectAsLazyPagingItems()
    val favoriteVodIds by viewModel.favoriteVodIds.collectAsState()
    val settings = remember { UserSettings(context) }
    val isListMode by settings.isBrowseListMode.collectAsState(initial = false)
    val colors = AreIptvTheme.colors

    if (!state.hasSource) {
        Text(
            text = "Add a playlist from the sidebar to see series here.",
            style = AreIptvTheme.typography.body,
            color = colors.textSecondary,
            modifier = modifier.padding(horizontal = AreIptvTheme.spacing.safeX, vertical = AreIptvTheme.spacing.sp10),
        )
        return
    }

    val categoryOptions = state.categories.map { BrowseCategoryOption(name = it.name, count = it.count) }

    BrowseLayout(
        title = "Series",
        categories = categoryOptions,
        selectedIndex = state.selectedCategoryIndex,
        onCategorySelected = viewModel::selectCategory,
        items = series,
        itemKey = { it.id },
        categoryColumnHeader = "Genres",
        sectionTitle = categoryOptions.getOrNull(state.selectedCategoryIndex)?.name,
        sectionCount = state.selectedCount,
        sectionCountLabel = { count -> "$count titles" },
        emptyLabel = "No series in this genre yet.",
        listMode = isListMode,
        modifier = modifier,
    ) { show ->
        ArePosterTile(
            title = show.name,
            onClick = { onSeriesSelected(show) },
            meta = listOfNotNull(show.year, show.categoryName).joinToString(" · ").ifEmpty { null },
            rating = show.rating,
            posterUrl = show.posterUrl,
            isFavorite = show.id in favoriteVodIds,
            onToggleFavorite = { viewModel.toggleFavorite(show.id) },
        )
    }
}
