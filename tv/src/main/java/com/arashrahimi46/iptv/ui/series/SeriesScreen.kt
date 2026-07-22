package com.arashrahimi46.iptv.ui.series

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
    val movieCountTitlesTemplate = stringResource(R.string.movies_count_titles)
    val seriesEpisodeCountTemplate = stringResource(R.string.series_episode_count)
    // Which series tile opened Detail (-> player) -- restore D-pad focus onto it when Back
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
            text = stringResource(R.string.series_no_source),
            style = AreIptvTheme.typography.body,
            color = colors.textSecondary,
            modifier = modifier.padding(horizontal = AreIptvTheme.spacing.safeX, vertical = AreIptvTheme.spacing.sp10),
        )
        return
    }

    // index 0 = "Favorites", 1 = "All series" -- neither is pinnable.
    val categoryOptions = state.categories.mapIndexed { index, it ->
        BrowseCategoryOption(name = it.name, count = it.count, pinned = it.pinned, pinnable = index >= 2)
    }

    BrowseLayout(
        title = stringResource(R.string.series_title),
        categories = categoryOptions,
        selectedIndex = state.selectedCategoryIndex,
        onCategorySelected = viewModel::selectCategory,
        onCategoryPinToggle = viewModel::togglePin,
        items = series,
        itemKey = { it.id },
        categoryColumnHeader = stringResource(R.string.movies_genres),
        sectionTitle = categoryOptions.getOrNull(state.selectedCategoryIndex)?.name,
        sectionCount = state.selectedCount,
        sectionCountLabel = { count -> String.format(movieCountTitlesTemplate, count) },
        emptyLabel = stringResource(R.string.series_empty_genre),
        listMode = isListMode,
        // Dense responsive poster grid: ~130dp columns that the tiles fill. Adaptive reflows the
        // column count to the available width, so covers stay small enough to show fully (with
        // their title) even when the sidebar expands and squeezes the content.
        minItemWidth = 130.dp,
        contentFocusRequester = contentFocusRequester,
        modifier = modifier,
    ) { show ->
        val episodeMeta = if (show.episodeCount > 0) String.format(seriesEpisodeCountTemplate, show.episodeCount) else null
        val focusRequester = rememberPlaybackFocusRequester(lastSelectedId, show.id) { lastSelectedId = null }
        ArePosterTile(
            title = show.name,
            onClick = { lastSelectedId = show.id; onSeriesSelected(show) },
            meta = listOfNotNull(episodeMeta, show.year, show.categoryName).joinToString(" · ").ifEmpty { null },
            rating = show.rating,
            posterUrl = show.posterUrl,
            fillWidth = true,
            isFavorite = show.id in favoriteVodIds,
            onToggleFavorite = { viewModel.toggleFavorite(show.id) },
            focusRequester = focusRequester,
        )
    }
}
