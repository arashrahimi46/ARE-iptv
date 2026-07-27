package com.arashrahimi46.iptv.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
    Column(modifier = modifier.fillMaxWidth().padding(start = spacing.safeX, end = spacing.safeX, top = spacing.sp2, bottom = spacing.sp6)) {

        // Same class as the QA MEDIUM text-wrap defect elsewhere in this screen: neither
        // column can safely shrink (the field column's fixed width and the categories
        // column's fixed width were themselves earlier QA fixes for label wrap), so
        // BoxWithConstraints measures the real available width and stacks the two columns
        // vertically instead of forcing them side by side when there genuinely isn't room for
        // both -- deterministic on measured dp, unlike relying on FlowRow's line-wrap
        // heuristics interacting with weight().
        val fieldColumnWidth = 420.dp
        val columnGap = 32.dp
        val minResultsWidth = 320.dp
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            if (maxWidth < fieldColumnWidth + columnGap + minResultsWidth) {
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    SearchFieldColumn(
                        modifier = Modifier.width(fieldColumnWidth),
                        query = state.query,
                        onQueryChange = viewModel::setQuery,
                    )
                    SearchResultsColumn(
                        modifier = Modifier.fillMaxWidth(),
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
                Row(horizontalArrangement = Arrangement.spacedBy(columnGap)) {
                    SearchFieldColumn(
                        modifier = Modifier.width(fieldColumnWidth),
                        query = state.query,
                        onQueryChange = viewModel::setQuery,
                    )
                    SearchResultsColumn(
                        modifier = Modifier.weight(1f),
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
) {
    // Issue #10: no custom on-screen keyboard -- AreTextField's BasicTextField
    // focuses Android TV's native IME, which the D-pad remote can drive directly.
    Column(modifier = modifier) {
        AreTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = stringResource(R.string.search_placeholder),
            icon = Icons.Filled.Search,
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
        // physical screen edge and clip almost entirely out of view -- FlowRow (already
        // used for the results below) wraps them to a second line instead.
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
            if (state.channelResults.isNotEmpty()) {
                Text(text = stringResource(R.string.search_section_live), style = AreIptvTheme.typography.h3, color = colors.textSecondary)
                Box(Modifier.padding(top = 12.dp, bottom = 22.dp)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        state.channelResults.forEach { channel ->
                            val focusRequester = rememberPlaybackFocusRequester(lastChannelId, channel.id) { lastChannelId = null }
                            AreChannelTile(
                                channel = channel.name,
                                onClick = { lastChannelId = channel.id; onChannelSelected(channel) },
                                number = channel.number,
                                category = channel.categoryName,
                                isRadio = channel.isRadio,
                                logoUrl = channel.logoUrl,
                                isFavorite = channel.id in favoriteChannelIds,
                                onToggleFavorite = { viewModel.toggleChannelFavorite(channel.id) },
                                modifier = Modifier.focusRequester(focusRequester),
                                lockCategory = channel.categoryName,
                            )
                        }
                    }
                }
            }
            if (state.titleResults.isNotEmpty()) {
                Text(text = stringResource(R.string.search_section_titles), style = AreIptvTheme.typography.h3, color = colors.textSecondary)
                Box(Modifier.padding(top = 12.dp)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        state.titleResults.forEach { title ->
                            val focusRequester = rememberPlaybackFocusRequester(lastTitleId, title.id) { lastTitleId = null }
                            ArePosterTile(
                                title = title.name,
                                onClick = { lastTitleId = title.id; onTitleSelected(title) },
                                meta = listOfNotNull(title.year, title.categoryName).joinToString(" · ").ifEmpty { null },
                                rating = title.rating,
                                posterUrl = title.posterUrl,
                                isFavorite = title.id in favoriteVodIds,
                                onToggleFavorite = { viewModel.toggleVodFavorite(title) },
                                focusRequester = focusRequester,
                                lockCategory = title.categoryName,
                            )
                        }
                    }
                }
            }
        }
    }
}
