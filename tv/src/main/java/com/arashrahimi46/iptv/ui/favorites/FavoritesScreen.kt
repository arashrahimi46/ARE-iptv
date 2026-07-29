package com.arashrahimi46.iptv.ui.favorites

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Text
import com.arashrahimi46.iptv.core.R
import com.arashrahimi46.iptv.data.model.Channel
import com.arashrahimi46.iptv.data.model.VodTitle
import com.arashrahimi46.iptv.ui.components.AreChannelTile
import com.arashrahimi46.iptv.ui.components.AreSegmentedControl
import com.arashrahimi46.iptv.ui.components.ArePosterTile
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.theme.requestFocusWhenReady
import com.arashrahimi46.iptv.ui.theme.rememberPlaybackFocusRequester

/**
 * Real Favorites screen (Favorites.jsx), player-independent: every tab reads
 * straight from Room via [FavoritesViewModel] -- no fake/optimistic-only
 * state. Tabs per the design source: Channels / Movies / Sports / Kids.
 * "Sports"/"Kids" are genre-filtered views across whatever content type is
 * favorited (channel or movie/series), not separate content types -- see
 * [FavoritesViewModel] for the exact category-name heuristic. The design
 * source's "New group" chip (custom favorite groups) is intentionally not
 * built here -- explicitly out of scope for this phase (backlog/v2).
 */
@Composable
fun FavoritesScreen(
    onChannelSelected: (Long) -> Unit,
    onTitleSelected: (VodTitle) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val viewModel: FavoritesViewModel = viewModel(
        factory = FavoritesViewModel.factory(context.applicationContext as android.app.Application),
    )
    val state by viewModel.uiState.collectAsState()
    val colors = AreIptvTheme.colors
    val spacing = AreIptvTheme.spacing
    // Land on the first tab that actually has content (so favoriting only a movie doesn't
    // drop the user on an empty Channels tab). `null` = "not yet chosen": once the user picks
    // a tab explicitly it sticks, even if that tab is empty.
    // rememberSaveable (not remember) so the chosen tab survives this screen being paused under
    // the player/detail overlay -- otherwise Back returns to a different tab than the one the
    // restored tile lives on, and the focus restore below can't find it.
    var selectedTab by rememberSaveable { mutableStateOf<String?>(null) }
    val tab = selectedTab ?: when {
        state.channels.isNotEmpty() -> "channels"
        state.movies.isNotEmpty() -> "movies"
        state.series.isNotEmpty() -> "series"
        else -> "channels"
    }

    if (!state.hasSource) {
        Text(
            text = stringResource(R.string.favorites_no_source),
            style = AreIptvTheme.typography.body,
            color = colors.textSecondary,
            modifier = modifier.padding(horizontal = spacing.safeX, vertical = spacing.sp10),
        )
        return
    }

    // Initial D-pad focus: the shell leaves focus on the sidebar, so the tab reads as dead until
    // the user presses RIGHT. Lands on the CURRENT tab chip rather than the first tile -- from
    // there Down reaches the grid, whereas from a tile you'd have to travel back up to switch tabs.
    // Runs on every entry, matching Streams and the browse screens.
    val tabFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { tabFocus.requestFocusWhenReady() }

    // Scroll position of the grid for the CURRENT tab. Hoisted here (rather than remembered inside
    // TileRows) so the collapsing gap below can read it -- keyed on `tab` so switching tabs hands the
    // new grid a fresh state at offset 0, which also resets the header back to expanded.
    val listState = remember(tab) { LazyListState() }

    // fillMaxSize so the tab-content Box below has a real bounded height to hand to its
    // LazyColumns (this screen scrolls its own content now -- see MainActivity's FullSizeTab).
    // Vertical insets match BrowseLayout's (sp1/sp3, not sp2/sp10): on a 1080p TV the tab strip plus
    // the old 8/40dp insets left under one poster row of viewport, so the row's title/meta -- which
    // sit BELOW the focusable poster -- were clipped at the bottom edge. The grid's own bottom
    // contentPadding (see TileRows) is what keeps the last row clear of the overscan edge now.
    Column(modifier = modifier.fillMaxSize().padding(top = spacing.sp1, bottom = spacing.sp3)) {
        Column(Modifier.padding(horizontal = spacing.safeX)) {
            AreSegmentedControl(
                options = listOf("channels", "movies", "series"),
                selected = tab,
                label = {
                    when (it) {
                        "channels" -> stringResource(R.string.favorites_tab_channels)
                        "movies" -> stringResource(R.string.favorites_tab_movies)
                        else -> stringResource(R.string.favorites_tab_series)
                    }
                },
                onSelect = { selectedTab = it },
                selectedFocusRequester = tabFocus,
            )
        }

        HeaderGap(listState)

        Box(Modifier.weight(1f).padding(horizontal = spacing.safeX)) {
            when (tab) {
                "channels" -> ChannelGrid(
                    channels = state.channels,
                    emptyLabel = stringResource(R.string.favorites_empty_channels),
                    listState = listState,
                    onChannelSelected = onChannelSelected,
                    onToggleFavorite = viewModel::toggleChannelFavorite,
                )
                "movies" -> MovieGrid(
                    movies = state.movies,
                    emptyLabel = stringResource(R.string.favorites_empty_movies),
                    listState = listState,
                    onTitleSelected = onTitleSelected,
                    onToggleFavorite = viewModel::toggleVodFavorite,
                )
                "series" -> MovieGrid(
                    movies = state.series,
                    emptyLabel = stringResource(R.string.favorites_empty_series),
                    listState = listState,
                    onTitleSelected = onTitleSelected,
                    onToggleFavorite = viewModel::toggleVodFavorite,
                )
            }
        }
    }
}

/**
 * The collapsing part of the header: the breathing room between the pinned tab strip and the grid.
 * Expanded at rest, tightened once the grid has scrolled off its first row, restored on the way back
 * up. The [AreSegmentedControl] itself never moves or fades -- it must stay reachable by D-pad at
 * every scroll position, so it is the gap, not the tabs, that gives ground.
 *
 * PERF (CLAUDE.md "Rendering performance": Favorites scroll is recomposition-bound): the scroll-state
 * read lives HERE, behind [derivedStateOf], so a scroll frame invalidates this Spacer and nothing
 * else. Reading [listState] up in [FavoritesScreen] would recompose the whole grid subtree per frame.
 */
@Composable
private fun HeaderGap(listState: LazyListState) {
    val spacing = AreIptvTheme.spacing
    val motion = AreIptvTheme.motion
    val collapsed by remember(listState) {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 24
        }
    }
    val gap by animateDpAsState(
        targetValue = if (collapsed) spacing.sp1 else spacing.sp6,
        animationSpec = tween(motion.durBaseMs, easing = motion.easeOut),
        label = "favoritesHeaderGap",
    )
    Spacer(Modifier.height(gap))
}

@Composable
private fun ChannelGrid(
    channels: List<Channel>,
    emptyLabel: String,
    listState: LazyListState,
    onChannelSelected: (Long) -> Unit,
    onToggleFavorite: (Long) -> Unit,
) {
    val colors = AreIptvTheme.colors
    if (channels.isEmpty()) {
        Text(text = emptyLabel, style = AreIptvTheme.typography.body, color = colors.textSecondary)
        return
    }
    // Restore D-pad focus onto the channel that opened the player when Back re-enters this tab.
    var lastPlayedId by rememberSaveable { mutableStateOf<Long?>(null) }
    TileRows(channels, AreIptvTheme.spacing.tileLandWidth, listState) { channel ->
        val focusRequester = rememberPlaybackFocusRequester(lastPlayedId, channel.id) { lastPlayedId = null }
        AreChannelTile(
            channel = channel.name,
            onClick = { lastPlayedId = channel.id; onChannelSelected(channel.id) },
            logoUrl = channel.logoUrl,
            number = channel.number,
            category = channel.categoryName,
            isRadio = channel.isRadio,
            isFavorite = true,
            onToggleFavorite = { onToggleFavorite(channel.id) },
            modifier = Modifier.focusRequester(focusRequester),
        )
    }
}

/**
 * PERF: what [FlowRow] used to do here, but virtualized. Favorites is unbounded -- a user with a few
 * hundred favorites had every single glass tile (and its [coil.compose.AsyncImage]) composed, measured
 * and left in the display list, all of it re-recorded on every scroll frame. This is the same fix Home
 * got, shaped to keep the layout byte-identical rather than swapping in a LazyVerticalGrid: tiles here
 * are fixed-width, so chunking them into rows of `floor((width + gap) / (tileWidth + gap))` reproduces
 * FlowRow's exact packing -- left-aligned, 18dp gaps, ragged last row -- while a LazyVerticalGrid would
 * have distributed the leftover width across the columns and widened the gaps.
 */
/**
 * Poster width for the Movies/Series tabs, deliberately NOT [AreIptvTheme.spacing.tilePosterWidth]
 * (208dp). A 2:3 poster at 208dp is 312dp tall, and with its title+meta the item stood ~356dp high
 * against a ~370dp viewport -- one favorite filled the screen and its meta line was still cut off at
 * the bottom edge. Browse renders the SAME posters through `GridCells.Adaptive(115.dp)`, so 208dp
 * here was the outlier, not the baseline. 150dp lands between the two: comfortably inside the
 * viewport with its label, and several per row instead of one.
 */
private val FavoritePosterWidth = 150.dp

@Composable
private fun <T> TileRows(items: List<T>, tileWidth: Dp, listState: LazyListState, tile: @Composable (T) -> Unit) {
    val gap = 18.dp
    BoxWithConstraints {
        val perRow = ((maxWidth + gap) / (tileWidth + gap)).toInt().coerceAtLeast(1)
        val rows = remember(items, perRow) { items.chunked(perRow) }
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(gap),
            // Same reasoning as BrowseLayout's grid. Top: headroom for the 1.06x focus scale so a
            // focused first row isn't clipped against the tab strip. Bottom: room for a focused row's
            // title/meta -- which [ArePosterTile] draws BELOW the focusable poster -- to scroll fully
            // into view. With no bottom padding at all they were cut mid-glyph at the viewport edge.
            contentPadding = PaddingValues(top = 10.dp, bottom = 52.dp),
        ) {
            items(rows.size) { index ->
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    rows[index].forEach { tile(it) }
                }
            }
        }
    }
}

@Composable
private fun MovieGrid(
    movies: List<VodTitle>,
    emptyLabel: String,
    listState: LazyListState,
    onTitleSelected: (VodTitle) -> Unit,
    onToggleFavorite: (VodTitle) -> Unit,
) {
    val colors = AreIptvTheme.colors
    if (movies.isEmpty()) {
        Text(text = emptyLabel, style = AreIptvTheme.typography.body, color = colors.textSecondary)
        return
    }
    // Restore D-pad focus onto the title that opened Detail when Back re-enters this tab.
    var lastPlayedId by rememberSaveable { mutableStateOf<Long?>(null) }
    TileRows(movies, FavoritePosterWidth, listState) { movie ->
        val focusRequester = rememberPlaybackFocusRequester(lastPlayedId, movie.id) { lastPlayedId = null }
        ArePosterTile(
            title = movie.name,
            onClick = { lastPlayedId = movie.id; onTitleSelected(movie) },
            width = FavoritePosterWidth,
            posterUrl = movie.posterUrl,
            meta = listOfNotNull(movie.year, movie.categoryName).joinToString(" · ").ifEmpty { null },
            rating = movie.rating,
            isFavorite = true,
            onToggleFavorite = { onToggleFavorite(movie) },
            focusRequester = focusRequester,
        )
    }
}
