package com.arashrahimi46.iptv.ui.movies

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
 * Movies browse (Browse.jsx): reuses the exact [BrowseLayout] shared with
 * Live TV, sourced from the real [VodTitle] catalog (isSeries = false) via
 * [MoviesViewModel]. Selecting a tile opens the real [com.arashrahimi46.iptv.ui.detail.DetailScreen]
 * for that title's own id -- never a hardcoded record.
 */
@Composable
fun MoviesScreen(onMovieSelected: (VodTitle) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val viewModel: MoviesViewModel = viewModel(
        factory = MoviesViewModel.factory(context.applicationContext as android.app.Application),
    )
    val state by viewModel.uiState.collectAsState()
    val movies = viewModel.pagingData.collectAsLazyPagingItems()
    val favoriteVodIds by viewModel.favoriteVodIds.collectAsState()
    val settings = remember { UserSettings(context) }
    val isListMode by settings.isBrowseListMode.collectAsState(initial = false)
    val colors = AreIptvTheme.colors

    if (!state.hasSource) {
        Text(
            text = "Add a playlist from the sidebar to see movies here.",
            style = AreIptvTheme.typography.body,
            color = colors.textSecondary,
            modifier = modifier.padding(horizontal = AreIptvTheme.spacing.safeX, vertical = AreIptvTheme.spacing.sp10),
        )
        return
    }

    val categoryOptions = state.categories.map { BrowseCategoryOption(name = it.name, count = it.count) }

    BrowseLayout(
        title = "Movies",
        categories = categoryOptions,
        selectedIndex = state.selectedCategoryIndex,
        onCategorySelected = viewModel::selectCategory,
        items = movies,
        itemKey = { it.id },
        categoryColumnHeader = "Genres",
        sectionTitle = categoryOptions.getOrNull(state.selectedCategoryIndex)?.name,
        sectionCount = state.selectedCount,
        sectionCountLabel = { count -> "$count titles" },
        emptyLabel = "No movies in this genre yet.",
        listMode = isListMode,
        modifier = modifier,
    ) { movie ->
        ArePosterTile(
            title = movie.name,
            onClick = { onMovieSelected(movie) },
            meta = listOfNotNull(movie.year, movie.categoryName).joinToString(" · ").ifEmpty { null },
            rating = movie.rating,
            posterUrl = movie.posterUrl,
            isFavorite = movie.id in favoriteVodIds,
            onToggleFavorite = { viewModel.toggleFavorite(movie.id) },
        )
    }
}
