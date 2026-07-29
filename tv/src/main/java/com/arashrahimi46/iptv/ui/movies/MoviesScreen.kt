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
import com.arashrahimi46.iptv.ui.browse.BrowseTileActions
import com.arashrahimi46.iptv.ui.components.ArePosterTile
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.theme.requestFocusWhenReady
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

    // Initial D-pad focus: the persistent shell leaves focus on the sidebar, so without this the
    // content reads as dead until the user blindly presses RIGHT. Runs on EVERY entry to the tab
    // (not once per session) so every screen behaves the way Streams already did.
    // The one exception is returning from the player, where rememberPlaybackFocusRequester restores
    // focus to the tile you launched -- landing back on your place beats landing at the top.
    val contentFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        if (lastSelectedId == null) contentFocusRequester.requestFocusWhenReady()
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
    // remember-ed so the List (and its fresh BrowseCategoryOption instances) doesn't compare unequal
    // on every recomposition, which would stop BrowseLayout from ever skipping.
    val categoryOptions = remember(state.categories) {
        state.categories.mapIndexed { index, it ->
            BrowseCategoryOption(name = it.name, count = it.count, pinned = it.pinned, pinnable = index >= 2)
        }
    }

    // Hoisted out of the item lambda below. `favoriteVodIds` is a Set -- an unstable type -- so
    // capturing it directly made the trailing lambda a new instance on every recomposition, which
    // defeats strong skipping's lambda memoization and takes every visible grid tile with it.
    // (LiveScreen already does this; Movies/Series were still capturing the Set directly.)
    val isFavorite = remember(favoriteVodIds) { { id: Long -> id in favoriteVodIds } }

    // Hold-OK menu for a movie tile. No Play row: a movie tile's click opens Detail (which owns the
    // stream/resume plumbing), so there is no direct-play entry point to reuse from here.
    val tileActions = remember(isFavorite) {
        BrowseTileActions<VodTitle>(
            title = { it.name },
            isFavorite = { isFavorite(it.id) },
            onToggleFavorite = { viewModel.toggleFavorite(it.id) },
        )
    }

    BrowseLayout(
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
        tileActions = tileActions,
        modifier = modifier,
    ) { movie, onLongClick ->
        val focusRequester = rememberPlaybackFocusRequester(lastSelectedId, movie.id) { lastSelectedId = null }
        ArePosterTile(
            title = movie.name,
            onClick = { lastSelectedId = movie.id; onMovieSelected(movie) },
            meta = listOfNotNull(movie.year, movie.categoryName).joinToString(" · ").ifEmpty { null },
            rating = movie.rating,
            posterUrl = movie.posterUrl,
            fillWidth = true,
            isFavorite = isFavorite(movie.id),
            onToggleFavorite = { viewModel.toggleFavorite(movie.id) },
            onLongClick = onLongClick,
            focusRequester = focusRequester,
        )
    }
}
