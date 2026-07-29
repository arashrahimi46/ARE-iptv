package com.arashrahimi46.iptv.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Text
import com.arashrahimi46.iptv.R
import com.arashrahimi46.iptv.data.model.Channel
import com.arashrahimi46.iptv.data.model.VodTitle
import com.arashrahimi46.iptv.ui.components.AreButton
import com.arashrahimi46.iptv.ui.components.AreSegmentedControl
import com.arashrahimi46.iptv.ui.components.AreButtonSize
import com.arashrahimi46.iptv.ui.components.AreButtonVariant
import com.arashrahimi46.iptv.ui.components.AreChannelTile
import com.arashrahimi46.iptv.ui.components.ArePosterTile
import com.arashrahimi46.iptv.ui.components.AreTextField
import com.arashrahimi46.iptv.ui.theme.AreIptvColors
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.theme.requestFocusWhenReady
import com.arashrahimi46.iptv.ui.theme.rememberPlaybackFocusRequester

/**
 * Local search (Search.jsx): a query field driven by [AreTextField], which
 * focuses Android TV's native IME/D-pad remote text input (no custom
 * on-screen keyboard), plus grouped result rows -- live channels and
 * movies/series -- reusing [AreChannelTile]/[ArePosterTile]. No ranking
 * backend: plain substring match over the already-loaded catalog, per spec.
 */
@Composable
fun SearchScreen(
    onChannelSelected: (Channel) -> Unit,
    onTitleSelected: (VodTitle) -> Unit,
    modifier: Modifier = Modifier,
    /** Home's "Browse by category" cards have no dedicated category-browse screen of
     * their own -- they route here with an exact category to filter by (see report),
     * distinct from a typed text query. */
    initialCategory: String? = null,
) {
    val context = LocalContext.current
    val viewModel: SearchViewModel = viewModel(
        factory = SearchViewModel.factory(context.applicationContext as android.app.Application),
    )
    LaunchedEffect(initialCategory) {
        if (!initialCategory.isNullOrBlank()) viewModel.setCategoryFilter(initialCategory)
    }
    val state by viewModel.uiState.collectAsState()
    val favoriteChannelIds by viewModel.favoriteChannelIds.collectAsState()
    val favoriteVodIds by viewModel.favoriteVodIds.collectAsState()
    val colors = AreIptvTheme.colors
    val spacing = AreIptvTheme.spacing

    if (!state.hasSource) {
        Text(
            text = stringResource(R.string.search_no_source),
            style = AreIptvTheme.typography.body,
            color = colors.textSecondary,
            modifier = modifier.padding(horizontal = spacing.safeX, vertical = spacing.sp10),
        )
        return
    }

    // Follow-up on the QA MEDIUM text-wrap defect: fillMaxWidth on the inner field+
    // results Row alone (round 1) and on this outer root Column (round 2) were real fixes
    // but didn't touch the actual root cause -- see the BoxWithConstraints comment below.
    // fillMaxSize (not just fillMaxWidth) so the results list below inherits a real bounded
    // height for its LazyColumn -- this screen scrolls its own results now, see MainActivity's
    // FullSizeTab.
    // Insets match BrowseLayout/Favorites (sp1/sp3), not the old sp2/sp6: the results LazyColumn
    // now carries its own 52dp bottom contentPadding, which already exceeds safeY, so the larger
    // root inset was pure lost viewport on a screen that only fits one tile row to begin with.
    Column(modifier = modifier.fillMaxSize().padding(start = spacing.safeX, end = spacing.safeX, top = spacing.sp1, bottom = spacing.sp3)) {

        // Same class as the QA MEDIUM text-wrap defect elsewhere in this screen: neither
        // column can safely shrink (the field column's fixed width and the categories
        // column's fixed width were themselves earlier QA fixes for label wrap), so
        // BoxWithConstraints measures the real available width and stacks the two columns
        // vertically instead of forcing them side by side when there genuinely isn't room for
        // both -- deterministic on measured dp, unlike relying on FlowRow's line-wrap
        // heuristics interacting with weight().
        // Initial D-pad focus: same rule as every other tab -- land on the thing you came for.
        // Here that is the query field, highlighted but NOT yet in edit mode (see activateOnClick
        // below), so arriving at Search never throws a full-screen keyboard over the results.
        val fieldFocus = remember { FocusRequester() }
        LaunchedEffect(Unit) { fieldFocus.requestFocusWhenReady() }
        val fieldColumnWidth = 420.dp
        val columnGap = 32.dp
        val minResultsWidth = 320.dp
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            if (maxWidth < fieldColumnWidth + columnGap + minResultsWidth) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    SearchFieldColumn(
                        modifier = Modifier.width(fieldColumnWidth),
                        query = state.query,
                        onQueryChange = viewModel::setQuery,
                        fieldFocus = fieldFocus,
                    )
                    SearchResultsColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        state = state,
                        colors = colors,
                        favoriteChannelIds = favoriteChannelIds,
                        favoriteVodIds = favoriteVodIds,
                        onChannelSelected = onChannelSelected,
                        onTitleSelected = onTitleSelected,
                        viewModel = viewModel,
                    )
                }
            } else {
                Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(columnGap)) {
                    SearchFieldColumn(
                        modifier = Modifier.width(fieldColumnWidth),
                        query = state.query,
                        onQueryChange = viewModel::setQuery,
                        fieldFocus = fieldFocus,
                    )
                    SearchResultsColumn(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        state = state,
                        colors = colors,
                        favoriteChannelIds = favoriteChannelIds,
                        favoriteVodIds = favoriteVodIds,
                        onChannelSelected = onChannelSelected,
                        onTitleSelected = onTitleSelected,
                        viewModel = viewModel,
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchFieldColumn(
    modifier: Modifier = Modifier,
    query: String,
    onQueryChange: (String) -> Unit,
    fieldFocus: FocusRequester? = null,
) {
    // Issue #10: no custom on-screen keyboard -- AreTextField's BasicTextField
    // focuses Android TV's native IME, which the D-pad remote can drive directly.
    Column(modifier = modifier) {
        AreTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = stringResource(R.string.search_placeholder),
            icon = Icons.Filled.Search,
            // activateOnClick: the screen now focuses this field on every entry, and with the
            // always-editable form that would raise the IME over the results the instant Search
            // opens. As a focusable row it highlights instead, and OK starts typing.
            activateOnClick = true,
            modifier = if (fieldFocus != null) Modifier.focusRequester(fieldFocus) else Modifier,
        )
    }
}

@Composable
private fun SearchResultsColumn(
    modifier: Modifier = Modifier,
    state: SearchUiState,
    colors: AreIptvColors,
    favoriteChannelIds: Set<Long>,
    favoriteVodIds: Set<Long>,
    onChannelSelected: (Channel) -> Unit,
    onTitleSelected: (VodTitle) -> Unit,
    viewModel: SearchViewModel,
) {
    // Which result opened the player/detail -- restore D-pad focus onto it when Back re-enters
    // Search, instead of letting focus fall to the sidebar. Channels and titles are separate id
    // spaces (different tables), so they're tracked apart to avoid a cross-collision. Survives
    // this tab being paused under the overlay via rememberSaveable.
    var lastChannelId by rememberSaveable { mutableStateOf<Long?>(null) }
    var lastTitleId by rememberSaveable { mutableStateOf<Long?>(null) }
    Column(modifier = modifier) {
        // Search.jsx's scope chips (All / Live TV / Movies / Series) -- Catch-up
        // omitted, an accepted v1 scope cut (see product-lead ruling). QA MEDIUM defect:
        // a plain Row here doesn't reflow, so when the sidebar auto-expands (104->280dp)
        // and steals width from this column, the trailing chips get pushed past the
        // physical screen edge and clip almost entirely out of view -- FlowRow wraps them
        // to a second line instead.
        AreSegmentedControl(
            options = listOf(SearchScope.All, SearchScope.LiveTv, SearchScope.Movies, SearchScope.Series),
            selected = state.scope,
            label = {
                when (it) {
                    SearchScope.All -> stringResource(R.string.search_scope_all)
                    SearchScope.LiveTv -> stringResource(R.string.search_scope_live)
                    SearchScope.Movies -> stringResource(R.string.search_scope_movies)
                    SearchScope.Series -> stringResource(R.string.search_scope_series)
                }
            },
            onSelect = viewModel::setScope,
        )
        Box(Modifier.padding(top = 18.dp))
        if (state.categoryFilter != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = stringResource(R.string.search_category_label, state.categoryFilter), style = AreIptvTheme.typography.h3, color = colors.textPrimary)
                AreButton(text = stringResource(R.string.action_clear), onClick = { viewModel.setCategoryFilter(null) }, variant = AreButtonVariant.Ghost, size = AreButtonSize.Small)
            }
            Box(Modifier.padding(top = 16.dp))
        }
        if (state.categoryFilter == null && state.query.isBlank()) {
            Text(
                text = stringResource(R.string.search_type_to_search),
                style = AreIptvTheme.typography.body,
                color = colors.textSecondary,
            )
        } else if (state.categoryFilter == null && state.query.trim().length < SearchViewModel.MIN_QUERY_LENGTH) {
            // Non-blank but too short to search yet -- a "keep typing" hint, not a "no results" dead end.
            Text(
                text = stringResource(R.string.search_min_chars, SearchViewModel.MIN_QUERY_LENGTH),
                style = AreIptvTheme.typography.body,
                color = colors.textSecondary,
            )
        } else if (state.channelResults.isEmpty() && state.titleResults.isEmpty()) {
            val label = state.categoryFilter ?: state.query
            Text(text = stringResource(R.string.search_no_results, label), style = AreIptvTheme.typography.body, color = colors.textSecondary)
        } else {
            SearchResults(
                modifier = Modifier.weight(1f),
                channels = state.channelResults,
                titles = state.titleResults,
                colors = colors,
                channelTile = { channel ->
                    val focusRequester = rememberPlaybackFocusRequester(lastChannelId, channel.id) { lastChannelId = null }
                    AreChannelTile(
                        channel = channel.name,
                        onClick = { lastChannelId = channel.id; onChannelSelected(channel) },
                        width = SearchChannelWidth,
                        number = channel.number,
                        category = channel.categoryName,
                        isRadio = channel.isRadio,
                        logoUrl = channel.logoUrl,
                        isFavorite = channel.id in favoriteChannelIds,
                        onToggleFavorite = { viewModel.toggleChannelFavorite(channel.id) },
                        modifier = Modifier.focusRequester(focusRequester),
                        lockCategory = channel.categoryName,
                    )
                },
                titleTile = { title ->
                    val focusRequester = rememberPlaybackFocusRequester(lastTitleId, title.id) { lastTitleId = null }
                    ArePosterTile(
                        title = title.name,
                        onClick = { lastTitleId = title.id; onTitleSelected(title) },
                        width = SearchPosterWidth,
                        meta = listOfNotNull(title.year, title.categoryName).joinToString(" · ").ifEmpty { null },
                        rating = title.rating,
                        posterUrl = title.posterUrl,
                        isFavorite = title.id in favoriteVodIds,
                        onToggleFavorite = { viewModel.toggleVodFavorite(title) },
                        focusRequester = focusRequester,
                        lockCategory = title.categoryName,
                    )
                },
            )
        }
    }
}

/**
 * PERF: what the two result [androidx.compose.foundation.layout.FlowRow]s used to do here, but
 * virtualized -- the same fix Favorites got (see `TileRows` there) and Browse got before it. Both
 * FlowRows were eager: with [SearchViewModel.RESULT_LIMIT] at 30 per catalog and titles being
 * movies+series concatenated, a broad query composed and measured up to ~90 glass tiles in a single
 * frame -- each one an [coil.compose.AsyncImage], a glass border, a softShadow bake and three-odd
 * text measures (~1-1.5ms each on the target Cortex-A55) -- and then kept all of them in the
 * display list to be re-recorded every scroll frame. Now only the on-screen rows exist.
 *
 * Chunked rows rather than a [androidx.compose.foundation.lazy.grid.LazyVerticalGrid], for the same
 * reason as Favorites: the tiles are fixed-width, so rows of `floor((width + gap) / (tileWidth +
 * gap))` reproduce FlowRow's exact packing -- left-aligned, 18dp gaps, ragged last row -- while a
 * grid would have distributed the leftover width across the columns and widened the gaps. Channels
 * and titles share ONE LazyColumn so the two sections still scroll as one surface, but they are
 * different shapes with different per-row counts, so every item carries a `contentType`: key alone
 * is not enough (see CLAUDE.md) -- without it the slot-reuse pool hands a poster row's slot to a
 * channel row and the whole row re-measures from scratch.
 */
/**
 * Result tile widths, deliberately smaller than the `tileLandWidth` (320dp) / `tilePosterWidth`
 * (208dp) tokens the tiles default to. Search stacks a query field AND a scope strip above the
 * results, so its viewport is the shortest in the app -- at the token sizes a 2:3 poster is 312dp
 * tall and the first row alone overflowed the screen, cutting the titles off. Browse renders the
 * same content through `GridCells.Adaptive(115.dp)` (movies/series) and `(180.dp)` (live), so the
 * tokens were the outlier here, not the baseline. These match Browse's density, which is what makes
 * a full row -- poster AND its title/meta -- fit under this screen's taller header stack.
 */
private val SearchChannelWidth = 180.dp
private val SearchPosterWidth = 120.dp

@Composable
private fun SearchResults(
    modifier: Modifier = Modifier,
    channels: List<Channel>,
    titles: List<VodTitle>,
    colors: AreIptvColors,
    channelTile: @Composable (Channel) -> Unit,
    titleTile: @Composable (VodTitle) -> Unit,
) {
    val gap = 18.dp
    BoxWithConstraints(modifier = modifier) {
        val channelsPerRow = ((maxWidth + gap) / (SearchChannelWidth + gap)).toInt().coerceAtLeast(1)
        val titlesPerRow = ((maxWidth + gap) / (SearchPosterWidth + gap)).toInt().coerceAtLeast(1)
        val channelRows = remember(channels, channelsPerRow) { channels.chunked(channelsPerRow) }
        val titleRows = remember(titles, titlesPerRow) { titles.chunked(titlesPerRow) }
        // No verticalArrangement: FlowRow put its 18dp gap BETWEEN tile rows only, and the section
        // headers carry their own (12dp under, 22dp above the second section) -- a list-wide
        // arrangement would have applied the same gap around the headers too and shifted everything.
        // contentPadding, not a Modifier.padding: ArePosterTile draws its title/meta BELOW the
        // focusable poster and the focusable scales 1.06x, so with a flush viewport edge the last
        // visible row lost its label mid-glyph and its focus ring. Same values BrowseLayout uses.
        LazyColumn(contentPadding = PaddingValues(top = 10.dp, bottom = 52.dp)) {
            if (channelRows.isNotEmpty()) {
                item(key = "header-live", contentType = "header") {
                    Text(
                        text = stringResource(R.string.search_section_live),
                        style = AreIptvTheme.typography.h3,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                }
                itemsIndexed(
                    channelRows,
                    key = { _, row -> "channel-${row.first().id}" },
                    contentType = { _, _ -> "channelRow" },
                ) { index, row ->
                    Row(
                        modifier = if (index == 0) Modifier else Modifier.padding(top = gap),
                        horizontalArrangement = Arrangement.spacedBy(gap),
                    ) {
                        row.forEach { channel -> channelTile(channel) }
                    }
                }
            }
            if (titleRows.isNotEmpty()) {
                item(key = "header-titles", contentType = "header") {
                    Text(
                        text = stringResource(R.string.search_section_titles),
                        style = AreIptvTheme.typography.h3,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(top = if (channelRows.isEmpty()) 0.dp else 22.dp, bottom = 12.dp),
                    )
                }
                itemsIndexed(
                    titleRows,
                    // Movies and series are separate tables concatenated into one list, so their
                    // ids can collide -- key on both to keep the row keys unique.
                    key = { _, row -> "title-${row.first().isSeries}-${row.first().id}" },
                    contentType = { _, _ -> "titleRow" },
                ) { index, row ->
                    Row(
                        modifier = if (index == 0) Modifier else Modifier.padding(top = gap),
                        horizontalArrangement = Arrangement.spacedBy(gap),
                    ) {
                        row.forEach { title -> titleTile(title) }
                    }
                }
            }
        }
    }
}
