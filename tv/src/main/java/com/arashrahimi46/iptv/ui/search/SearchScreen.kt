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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Text
import com.arashrahimi46.iptv.data.model.Channel
import com.arashrahimi46.iptv.data.model.VodTitle
import com.arashrahimi46.iptv.ui.components.AreButton
import com.arashrahimi46.iptv.ui.components.AreButtonSize
import com.arashrahimi46.iptv.ui.components.AreButtonVariant
import com.arashrahimi46.iptv.ui.components.AreChannelTile
import com.arashrahimi46.iptv.ui.components.AreChip
import com.arashrahimi46.iptv.ui.components.AreOnScreenKeyboard
import com.arashrahimi46.iptv.ui.components.ArePosterTile
import com.arashrahimi46.iptv.ui.components.AreTextField
import com.arashrahimi46.iptv.ui.theme.AreIptvColors
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme

/**
 * Local search (Search.jsx): a query field driven entirely by
 * [AreOnScreenKeyboard] key presses (D-pad + select, no IME) plus grouped
 * result rows -- live channels and movies/series -- reusing
 * [AreChannelTile]/[ArePosterTile]. No ranking backend: plain substring
 * match over the already-loaded catalog, per spec.
 */
@Composable
private fun ScopeChip(label: String, value: SearchScope, current: SearchScope, onSelect: (SearchScope) -> Unit) {
    AreChip(text = label, selected = value == current, onClick = { onSelect(value) })
}

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
            text = "Add a playlist from the sidebar to search your catalog.",
            style = AreIptvTheme.typography.body,
            color = colors.textSecondary,
            modifier = modifier.padding(horizontal = spacing.safeX, vertical = spacing.sp10),
        )
        return
    }

    // Follow-up on the QA MEDIUM text-wrap defect: fillMaxWidth on the inner keyboard+
    // results Row alone (round 1) and on this outer root Column (round 2) were real fixes
    // but didn't touch the actual root cause -- see the BoxWithConstraints comment below.
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = spacing.safeX, vertical = spacing.sp6)) {
        Text(text = "Search", style = AreIptvTheme.typography.display, color = colors.textPrimary)
        Box(Modifier.padding(top = spacing.sp6))

        // Round 3 of the QA MEDIUM text-wrap defect: QA's uiautomator measurements showed
        // that with the sidebar expanded (280dp), the fixed 560dp keyboard column plus the
        // 32dp gap alone exceed the width physically left over, so the results column was
        // being handed a near-zero (or negative, clamped to a sliver) remainder -- no
        // fillMaxWidth-style modifier fix can manufacture width that isn't there. Neither
        // column can safely shrink (560dp keyboard width and the categories column's fixed
        // width were themselves earlier QA fixes for keyboard-key clipping / label wrap), so
        // BoxWithConstraints measures the real available width and stacks the two columns
        // vertically instead of forcing them side by side when there genuinely isn't room for
        // both -- deterministic on measured dp, unlike relying on FlowRow's line-wrap
        // heuristics interacting with weight().
        val keyboardColumnWidth = 560.dp
        val columnGap = 32.dp
        val minResultsWidth = 320.dp
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            if (maxWidth < keyboardColumnWidth + columnGap + minResultsWidth) {
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    SearchKeyboardColumn(
                        modifier = Modifier.width(keyboardColumnWidth),
                        query = state.query,
                        onQueryChange = viewModel::setQuery,
                        onCharacter = viewModel::appendCharacter,
                        onSpace = viewModel::appendSpace,
                        onBackspace = viewModel::backspace,
                        onClear = viewModel::clear,
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
                    SearchKeyboardColumn(
                        modifier = Modifier.width(keyboardColumnWidth),
                        query = state.query,
                        onQueryChange = viewModel::setQuery,
                        onCharacter = viewModel::appendCharacter,
                        onSpace = viewModel::appendSpace,
                        onBackspace = viewModel::backspace,
                        onClear = viewModel::clear,
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
private fun SearchKeyboardColumn(
    modifier: Modifier = Modifier,
    query: String,
    onQueryChange: (String) -> Unit,
    onCharacter: (Char) -> Unit,
    onSpace: () -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
) {
    // QA MEDIUM defect: L, O, P appeared "missing" from the keyboard -- they're
    // exactly the trailing keys of the two widest rows (QWERTYUIOP is 10 keys x
    // 48dp + 9 gaps x 8dp = 552dp; ASDFGHJKL is 496dp), both wider than this
    // column's old fixed 420dp -- the results Column to the right, drawn after,
    // painted over the overflowing keys rather than an actual data/layout bug in
    // AreOnScreenKeyboard itself (all 26 letters are really in DefaultKeyboardRows).
    // 560dp (the caller's fixed width) gives the widest row (552dp) real room.
    Column(modifier = modifier) {
        AreTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = "Search channels, movies, series…",
            icon = Icons.Filled.Search,
        )
        Box(Modifier.padding(top = 20.dp))
        AreOnScreenKeyboard(
            onCharacter = onCharacter,
            onSpace = onSpace,
            onBackspace = onBackspace,
            onClear = onClear,
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
    Column(modifier = modifier) {
        // Search.jsx's scope chips (All / Live TV / Movies / Series) -- Catch-up
        // omitted, an accepted v1 scope cut (see product-lead ruling). QA MEDIUM defect:
        // a plain Row here doesn't reflow, so when the sidebar auto-expands (104->280dp)
        // and steals width from this column, the trailing chips get pushed past the
        // physical screen edge and clip almost entirely out of view -- FlowRow (already
        // used for the results below) wraps them to a second line instead.
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ScopeChip("All", SearchScope.All, state.scope, viewModel::setScope)
            ScopeChip("Live TV", SearchScope.LiveTv, state.scope, viewModel::setScope)
            ScopeChip("Movies", SearchScope.Movies, state.scope, viewModel::setScope)
            ScopeChip("Series", SearchScope.Series, state.scope, viewModel::setScope)
        }
        Box(Modifier.padding(top = 18.dp))
        if (state.categoryFilter != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "Category: ${state.categoryFilter}", style = AreIptvTheme.typography.h3, color = colors.textPrimary)
                AreButton(text = "Clear", onClick = { viewModel.setCategoryFilter(null) }, variant = AreButtonVariant.Ghost, size = AreButtonSize.Small)
            }
            Box(Modifier.padding(top = 16.dp))
        }
        if (state.categoryFilter == null && state.query.isBlank()) {
            Text(
                text = "Type on the keyboard to search your catalog.",
                style = AreIptvTheme.typography.body,
                color = colors.textSecondary,
            )
        } else if (state.channelResults.isEmpty() && state.titleResults.isEmpty()) {
            val label = state.categoryFilter ?: state.query
            Text(text = "No results for \"$label\".", style = AreIptvTheme.typography.body, color = colors.textSecondary)
        } else {
            if (state.channelResults.isNotEmpty()) {
                Text(text = "Live TV", style = AreIptvTheme.typography.h3, color = colors.textSecondary)
                Box(Modifier.padding(top = 12.dp, bottom = 22.dp)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        state.channelResults.forEach { channel ->
                            AreChannelTile(
                                channel = channel.name,
                                onClick = { onChannelSelected(channel) },
                                number = channel.number,
                                now = channel.categoryName,
                                isFavorite = channel.id in favoriteChannelIds,
                                onToggleFavorite = { viewModel.toggleChannelFavorite(channel.id) },
                            )
                        }
                    }
                }
            }
            if (state.titleResults.isNotEmpty()) {
                Text(text = "Movies & series", style = AreIptvTheme.typography.h3, color = colors.textSecondary)
                Box(Modifier.padding(top = 12.dp)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        state.titleResults.forEach { title ->
                            ArePosterTile(
                                title = title.name,
                                onClick = { onTitleSelected(title) },
                                meta = listOfNotNull(title.year, title.categoryName).joinToString(" · ").ifEmpty { null },
                                rating = title.rating,
                                isFavorite = title.id in favoriteVodIds,
                                onToggleFavorite = { viewModel.toggleVodFavorite(title) },
                            )
                        }
                    }
                }
            }
        }
    }
}
