package com.arashrahimi46.iptv.mobile.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.arashrahimi46.iptv.core.R as CoreR
import com.arashrahimi46.iptv.mobile.data.model.Channel
import com.arashrahimi46.iptv.mobile.data.model.VodTitle
import com.arashrahimi46.iptv.mobile.ui.components.AreChannelTile
import com.arashrahimi46.iptv.mobile.ui.components.AreEmptyState
import com.arashrahimi46.iptv.mobile.ui.components.AreIconButton
import com.arashrahimi46.iptv.mobile.ui.components.AreIconButtonSize
import com.arashrahimi46.iptv.mobile.ui.components.AreIconButtonVariant
import com.arashrahimi46.iptv.mobile.ui.components.AreListRow
import com.arashrahimi46.iptv.mobile.ui.components.ArePosterTile
import com.arashrahimi46.iptv.mobile.ui.components.AreScreenScaffold
import com.arashrahimi46.iptv.mobile.ui.components.AreSectionHeader
import com.arashrahimi46.iptv.mobile.ui.components.AreTabs
import com.arashrahimi46.iptv.mobile.ui.components.AreTextField
import com.arashrahimi46.iptv.mobile.ui.components.AreTileActionSheet

/** Channel cells are wide (logo + now/next), posters are narrow -- both fill their grid cell. */
private val ChannelCellMin = 168.dp
private val PosterCellMin = 120.dp

/** Columns used by the "All" page, which lays its bounded sections out non-lazily inside a LazyColumn. */
private const val AllPageChannelColumns = 2
private const val AllPagePosterColumns = 3

private val GridPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp)

/** What a long-pressed tile opens the action sheet for. */
private sealed interface SheetTarget {
    data class OfChannel(val channel: Channel) : SheetTarget
    data class OfTitle(val title: VodTitle) : SheetTarget
}

/**
 * Search: a query field that takes focus (and the keyboard) on entry, four swipeable scope pages
 * (All / Live TV / Movies / Series) bound to [AreTabs], and vertical result grids -- not the TV
 * build's horizontal rails.
 *
 * Below [SearchViewModel.MIN_QUERY_LENGTH] characters the body shows recent searches instead of
 * results; committing a query with the IME's Search action records it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onOpenChannel: (Channel) -> Unit,
    onOpenTitle: (VodTitle) -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: SearchViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val favoriteChannelIds by viewModel.favoriteChannelIds.collectAsState()
    val favoriteVodIds by viewModel.favoriteVodIds.collectAsState()
    val recentQueries by viewModel.recentQueries.collectAsState()
    val keyboard = LocalSoftwareKeyboardController.current

    val scopes = remember { listOf(SearchScope.All, SearchScope.LiveTv, SearchScope.Movies, SearchScope.Series) }
    val pagerState = rememberPagerState(initialPage = scopes.indexOf(state.scope).coerceAtLeast(0)) { scopes.size }
    var sheetTarget by remember { mutableStateOf<SheetTarget?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // The one sanctioned FocusRequester in the rewritten UI: Search opens with the keyboard up.
    val fieldFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        runCatching { fieldFocus.requestFocus() }
        keyboard?.show()
    }

    // Tabs <-> pager, bound both ways.
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { viewModel.setScope(scopes[it]) }
    }

    // Any drag in the results area dismisses the keyboard (§3 scroll rule).
    val hideOnScroll = remember(keyboard) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y != 0f) keyboard?.hide()
                return Offset.Zero
            }
        }
    }

    AreScreenScaffold(title = stringResource(CoreR.string.search_title), onBack = onBack) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .imePadding(),
        ) {
            AreTextField(
                value = state.query,
                onValueChange = viewModel::setQuery,
                placeholder = stringResource(CoreR.string.search_placeholder),
                leadingIcon = Icons.Filled.Search,
                trailingIcon = if (state.query.isNotEmpty()) Icons.Filled.Close else null,
                onTrailingIconClick = { viewModel.setQuery("") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        viewModel.commitQuery(state.query)
                        keyboard?.hide()
                    },
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .focusRequester(fieldFocus),
            )
            AreTabs(
                options = scopes,
                selected = scopes[pagerState.currentPage],
                label = { scopeLabel(it) },
                onSelect = { scope ->
                    viewModel.setScope(scope)
                    coroutineScope.launch { pagerState.animateScrollToPage(scopes.indexOf(scope)) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            )

            val tooShort = state.query.trim().length < SearchViewModel.MIN_QUERY_LENGTH
            when {
                !state.hasSource -> AreEmptyState(
                    title = stringResource(CoreR.string.search_title),
                    message = stringResource(CoreR.string.search_no_source),
                    icon = Icons.Filled.Search,
                )

                tooShort -> RecentSearches(
                    recent = recentQueries,
                    onPick = { q ->
                        viewModel.setQuery(q)
                        keyboard?.hide()
                    },
                    onRemove = viewModel::clearRecent,
                    modifier = Modifier.fillMaxSize().nestedScroll(hideOnScroll),
                )

                else -> HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize().nestedScroll(hideOnScroll),
                    key = { scopes[it] },
                ) { page ->
                    val scope = scopes[page]
                    val channels = if (scope == SearchScope.All || scope == SearchScope.LiveTv) state.channelResults else emptyList()
                    val movies = if (scope == SearchScope.All || scope == SearchScope.Movies) state.movieResults else emptyList()
                    val series = if (scope == SearchScope.All || scope == SearchScope.Series) state.seriesResults else emptyList()
                    val pageEmpty = channels.isEmpty() && movies.isEmpty() && series.isEmpty()

                    when {
                        pageEmpty -> AreEmptyState(
                            title = stringResource(CoreR.string.search_no_results, state.query.trim()),
                            icon = Icons.Filled.Search,
                        )

                        scope == SearchScope.All -> AllScopePage(
                            channels = channels,
                            movies = movies,
                            series = series,
                            favoriteChannelIds = favoriteChannelIds,
                            favoriteVodIds = favoriteVodIds,
                            onOpenChannel = onOpenChannel,
                            onOpenTitle = onOpenTitle,
                            onLongPressChannel = { sheetTarget = SheetTarget.OfChannel(it) },
                            onLongPressTitle = { sheetTarget = SheetTarget.OfTitle(it) },
                            onToggleChannelFavorite = viewModel::toggleChannelFavorite,
                            onToggleVodFavorite = viewModel::toggleVodFavorite,
                            onSeeAll = { scope ->
                                viewModel.setScope(scope)
                                coroutineScope.launch { pagerState.animateScrollToPage(scopes.indexOf(scope)) }
                            },
                        )

                        scope == SearchScope.LiveTv -> LazyVerticalGrid(
                            columns = GridCells.Adaptive(ChannelCellMin),
                            contentPadding = GridPadding,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            items(channels, key = { it.id }, contentType = { "channel" }) { channel ->
                                ChannelResult(
                                    channel = channel,
                                    isFavorite = channel.id in favoriteChannelIds,
                                    onOpen = onOpenChannel,
                                    onLongPress = { sheetTarget = SheetTarget.OfChannel(channel) },
                                    onToggleFavorite = { viewModel.toggleChannelFavorite(channel.id) },
                                )
                            }
                        }

                        else -> {
                            val titles = if (scope == SearchScope.Movies) movies else series
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(PosterCellMin),
                                contentPadding = GridPadding,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                items(titles, key = { it.id }, contentType = { "title" }) { title ->
                                    TitleResult(
                                        title = title,
                                        isFavorite = title.id in favoriteVodIds,
                                        onOpen = onOpenTitle,
                                        onLongPress = { sheetTarget = SheetTarget.OfTitle(title) },
                                        onToggleFavorite = { viewModel.toggleVodFavorite(title) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    when (val target = sheetTarget) {
        is SheetTarget.OfChannel -> AreTileActionSheet(
            title = target.channel.name,
            onDismiss = { sheetTarget = null },
            isFavorite = target.channel.id in favoriteChannelIds,
            onToggleFavorite = { viewModel.toggleChannelFavorite(target.channel.id) },
            onPlay = {
                sheetTarget = null
                onOpenChannel(target.channel)
            },
        )

        is SheetTarget.OfTitle -> AreTileActionSheet(
            title = target.title.name,
            onDismiss = { sheetTarget = null },
            isFavorite = target.title.id in favoriteVodIds,
            onToggleFavorite = { viewModel.toggleVodFavorite(target.title) },
            onOpenDetails = {
                sheetTarget = null
                onOpenTitle(target.title)
            },
        )

        null -> Unit
    }
}

@Composable
private fun scopeLabel(scope: SearchScope): String = stringResource(
    when (scope) {
        SearchScope.All -> CoreR.string.search_scope_all
        SearchScope.LiveTv -> CoreR.string.search_scope_live
        SearchScope.Movies -> CoreR.string.search_scope_movies
        SearchScope.Series -> CoreR.string.search_scope_series
    },
)

/**
 * The "All" page: a bounded section per kind (max [SearchViewModel.ALL_PAGE_SECTION_LIMIT] each)
 * with a see-all row that switches the pager to that scope. The sections are laid out as plain
 * rows inside a [LazyColumn] -- a nested lazy grid has no bounded height here.
 */
@Composable
private fun AllScopePage(
    channels: List<Channel>,
    movies: List<VodTitle>,
    series: List<VodTitle>,
    favoriteChannelIds: Set<Long>,
    favoriteVodIds: Set<Long>,
    onOpenChannel: (Channel) -> Unit,
    onOpenTitle: (VodTitle) -> Unit,
    onLongPressChannel: (Channel) -> Unit,
    onLongPressTitle: (VodTitle) -> Unit,
    onToggleChannelFavorite: (Long) -> Unit,
    onToggleVodFavorite: (VodTitle) -> Unit,
    onSeeAll: (SearchScope) -> Unit,
) {
    val limit = SearchViewModel.ALL_PAGE_SECTION_LIMIT
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp),
    ) {
        if (channels.isNotEmpty()) {
            item(key = "live-header", contentType = "header") {
                AreSectionHeader(stringResource(CoreR.string.search_section_live))
            }
            val rows = channels.take(limit).chunked(AllPageChannelColumns)
            gridRows(rows, keyPrefix = "live-row", columns = AllPageChannelColumns) { channel ->
                ChannelResult(
                    channel = channel,
                    isFavorite = channel.id in favoriteChannelIds,
                    onOpen = onOpenChannel,
                    onLongPress = { onLongPressChannel(channel) },
                    onToggleFavorite = { onToggleChannelFavorite(channel.id) },
                )
            }
            if (channels.size > limit) {
                item(key = "live-see-all", contentType = "see-all") {
                    AreListRow(
                        title = stringResource(CoreR.string.browse_see_all),
                        supporting = stringResource(CoreR.string.search_scope_live),
                        onClick = { onSeeAll(SearchScope.LiveTv) },
                    )
                }
            }
        }
        vodSection(
            titles = movies,
            headerRes = CoreR.string.search_scope_movies,
            scope = SearchScope.Movies,
            favoriteVodIds = favoriteVodIds,
            onOpenTitle = onOpenTitle,
            onLongPressTitle = onLongPressTitle,
            onToggleVodFavorite = onToggleVodFavorite,
            onSeeAll = onSeeAll,
        )
        vodSection(
            titles = series,
            headerRes = CoreR.string.search_scope_series,
            scope = SearchScope.Series,
            favoriteVodIds = favoriteVodIds,
            onOpenTitle = onOpenTitle,
            onLongPressTitle = onLongPressTitle,
            onToggleVodFavorite = onToggleVodFavorite,
            onSeeAll = onSeeAll,
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.vodSection(
    titles: List<VodTitle>,
    headerRes: Int,
    scope: SearchScope,
    favoriteVodIds: Set<Long>,
    onOpenTitle: (VodTitle) -> Unit,
    onLongPressTitle: (VodTitle) -> Unit,
    onToggleVodFavorite: (VodTitle) -> Unit,
    onSeeAll: (SearchScope) -> Unit,
) {
    if (titles.isEmpty()) return
    val limit = SearchViewModel.ALL_PAGE_SECTION_LIMIT
    item(key = "${scope.name}-header", contentType = "header") {
        AreSectionHeader(stringResource(headerRes))
    }
    val rows = titles.take(limit).chunked(AllPagePosterColumns)
    gridRows(rows, keyPrefix = "${scope.name}-row", columns = AllPagePosterColumns) { title ->
        TitleResult(
            title = title,
            isFavorite = title.id in favoriteVodIds,
            onOpen = onOpenTitle,
            onLongPress = { onLongPressTitle(title) },
            onToggleFavorite = { onToggleVodFavorite(title) },
        )
    }
    if (titles.size > limit) {
        item(key = "${scope.name}-see-all", contentType = "see-all") {
            AreListRow(
                title = stringResource(CoreR.string.browse_see_all),
                supporting = stringResource(headerRes),
                onClick = { onSeeAll(scope) },
            )
        }
    }
}

/** Emits one lazy item per row of a fixed-column grid, padding the short last row so cells align. */
private fun <T> androidx.compose.foundation.lazy.LazyListScope.gridRows(
    rows: List<List<T>>,
    keyPrefix: String,
    columns: Int,
    cell: @Composable (T) -> Unit,
) {
    rows.forEachIndexed { index, row ->
        item(key = "$keyPrefix-$index", contentType = "$keyPrefix-cells") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                row.forEach { value ->
                    Box(Modifier.weight(1f)) { cell(value) }
                }
                repeat(columns - row.size) { Box(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun ChannelResult(
    channel: Channel,
    isFavorite: Boolean,
    onOpen: (Channel) -> Unit,
    onLongPress: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    AreChannelTile(
        channel = channel.name,
        onClick = { onOpen(channel) },
        logoUrl = channel.logoUrl,
        number = channel.number,
        category = channel.categoryName,
        isRadio = channel.isRadio,
        width = null,
        isFavorite = isFavorite,
        onToggleFavorite = onToggleFavorite,
        onLongClick = onLongPress,
        lockCategory = channel.categoryName,
    )
}

@Composable
private fun TitleResult(
    title: VodTitle,
    isFavorite: Boolean,
    onOpen: (VodTitle) -> Unit,
    onLongPress: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    ArePosterTile(
        title = title.name,
        onClick = { onOpen(title) },
        posterUrl = title.posterUrl,
        meta = listOfNotNull(title.year, title.categoryName).joinToString(" · ").ifBlank { null },
        rating = title.rating,
        width = null,
        isFavorite = isFavorite,
        onToggleFavorite = onToggleFavorite,
        onLongClick = onLongPress,
        lockCategory = title.categoryName,
    )
}

/** Shown instead of results while the query is too short: past queries, each removable. */
@Composable
private fun RecentSearches(
    recent: List<String>,
    onPick: (String) -> Unit,
    onRemove: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (recent.isEmpty()) {
        AreEmptyState(
            title = stringResource(CoreR.string.search_type_to_search),
            message = stringResource(CoreR.string.search_min_chars, SearchViewModel.MIN_QUERY_LENGTH),
            icon = Icons.Filled.Search,
            modifier = modifier,
        )
        return
    }
    LazyColumn(modifier = modifier, contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp)) {
        item(key = "recent-header", contentType = "header") {
            Row(
                modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.weight(1f)) { AreSectionHeader(stringResource(CoreR.string.search_recent)) }
                AreIconButton(
                    icon = Icons.Filled.Close,
                    contentDescription = stringResource(CoreR.string.search_clear_recent),
                    onClick = { onRemove(null) },
                    variant = AreIconButtonVariant.Ghost,
                    size = AreIconButtonSize.Small,
                )
            }
        }
        items(recent, key = { it }, contentType = { "recent" }) { query ->
            AreListRow(
                title = query,
                leadingIcon = Icons.Filled.History,
                onClick = { onPick(query) },
                trailing = {
                    AreIconButton(
                        icon = Icons.Filled.Close,
                        contentDescription = stringResource(CoreR.string.search_clear_recent),
                        onClick = { onRemove(query) },
                        variant = AreIconButtonVariant.Ghost,
                        size = AreIconButtonSize.Small,
                    )
                },
            )
        }
    }
}
