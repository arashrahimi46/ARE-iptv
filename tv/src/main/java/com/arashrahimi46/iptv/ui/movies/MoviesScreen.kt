package com.arashrahimi46.iptv.ui.movies

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.tv.material3.Text
import com.arashrahimi46.iptv.R
import com.arashrahimi46.iptv.data.model.VodTitle
import com.arashrahimi46.iptv.data.settings.UserSettings
import com.arashrahimi46.iptv.ui.browse.BrowseCategoryOption
import com.arashrahimi46.iptv.ui.browse.BrowseLayout
import com.arashrahimi46.iptv.ui.components.ArePosterTile
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.theme.rememberPlaybackFocusRequester

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
    val movieCountTitlesTemplate = stringResource(R.string.movies_count_titles)
    // Which movie tile opened Detail (-> player) -- restore D-pad focus onto it when Back
    // re-enters this grid, instead of letting the sidebar take focus. Same mechanism as
    // LiveScreen; survives this screen being paused under the overlay via rememberSaveable.
    var lastSelectedId by rememberSaveable { mutableStateOf<Long?>(null) }

    // Initial D-pad focus into the category column on genuine first entry -- but NOT when
    // returning from Detail/player (lastSelectedId != null), where rememberPlaybackFocusRequester
    // restores focus to the launched tile. initialFocusDone keeps this to the first entry only.
    val contentFocusRequester = remember { FocusRequester() }
    var initialFocusDone by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!initialFocusDone && lastSelectedId == null) {
            withFrameNanos { }
            runCatching { contentFocusRequester.requestFocus() }
            initialFocusDone = true
        }
    }

    if (!state.hasSource) {
        Text(
            text = stringResource(R.string.movies_no_source),
            style = AreIptvTheme.typography.body,
            color = colors.textSecondary,
            modifier = modifier.padding(horizontal = AreIptvTheme.spacing.safeX, vertical = AreIptvTheme.spacing.sp10),
        )
        return
    }

    // index 0 = "Favorites", 1 = "All movies" -- neither is pinnable.
    val categoryOptions = state.categories.mapIndexed { index, it ->
        BrowseCategoryOption(name = it.name, count = it.count, pinned = it.pinned, pinnable = index >= 2)
    }

    BrowseLayout(
        title = stringResource(R.string.movies_title),
        categories = categoryOptions,
        selectedIndex = state.selectedCategoryIndex,
        onCategorySelected = viewModel::selectCategory,
        onCategoryPinToggle = viewModel::togglePin,
        items = movies,
        itemKey = { it.id },
        categoryColumnHeader = stringResource(R.string.movies_genres),
        sectionTitle = categoryOptions.getOrNull(state.selectedCategoryIndex)?.name,
        sectionCount = state.selectedCount,
        sectionCountLabel = { count -> String.format(movieCountTitlesTemplate, count) },
        emptyLabel = stringResource(R.string.movies_empty_genre),
        listMode = isListMode,
        // Dense responsive poster grid: small columns the tiles fill. Adaptive reflows the column
        // count to the available width. Kept small (≈115dp) so the content pane fits 3+ columns:
        // a 2:3 poster at a wider column makes each tile (poster + title + meta) taller than the
        // grid viewport, so the focused tile can't be fully scrolled into view and its title/meta
        // fall off the bottom edge. Smaller covers keep a whole tile shorter than the viewport.
        minItemWidth = 115.dp,
        contentFocusRequester = contentFocusRequester,
        modifier = modifier,
    ) { movie ->
        val focusRequester = rememberPlaybackFocusRequester(lastSelectedId, movie.id) { lastSelectedId = null }
        ArePosterTile(
            title = movie.name,
            onClick = { lastSelectedId = movie.id; onMovieSelected(movie) },
            meta = listOfNotNull(movie.year, movie.categoryName).joinToString(" · ").ifEmpty { null },
            rating = movie.rating,
            posterUrl = movie.posterUrl,
            fillWidth = true,
            isFavorite = movie.id in favoriteVodIds,
            onToggleFavorite = { viewModel.toggleFavorite(movie.id) },
            focusRequester = focusRequester,
        )
    }
}
