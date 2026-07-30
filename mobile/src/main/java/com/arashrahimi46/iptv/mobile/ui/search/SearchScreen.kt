package com.arashrahimi46.iptv.mobile.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arashrahimi46.iptv.data.model.Channel
import com.arashrahimi46.iptv.data.model.VodTitle
import com.arashrahimi46.iptv.mobile.R
import com.arashrahimi46.iptv.mobile.ui.components.AreTextField
import com.arashrahimi46.iptv.ui.components.AreChannelTile
import com.arashrahimi46.iptv.ui.components.ArePosterTile
import com.arashrahimi46.iptv.ui.components.AreSegmentedControl
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme

/** Search tile widths at phone scale -- narrower than Favorites' 160/110dp since Search stacks
 * query field + scope chips above the results, leaving less vertical budget per row. */
private val SearchChannelWidth = 150.dp
private val SearchPosterWidth = 110.dp

/**
 * Dedicated Search destination: a query field ([AreTextField], phone IME) plus scope chips (All/
 * Live TV/Movies/Series), reusing the same local substring-search pipeline as :tv's Search
 * ([SearchViewModel]). Results render as two horizontally-scrolling sections (Live TV, Movies &
 * series) instead of :tv's D-pad row-by-row layout -- same [AreChannelTile]/[ArePosterTile] tiles.
 */
@Composable
fun SearchScreen(
    onOpenChannel: (Channel) -> Unit,
    onOpenTitle: (VodTitle) -> Unit,
    viewModel: SearchViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val favoriteChannelIds by viewModel.favoriteChannelIds.collectAsState()
    val favoriteVodIds by viewModel.favoriteVodIds.collectAsState()
    val colors = AreIptvTheme.colors
    val scopes = listOf(SearchScope.All, SearchScope.LiveTv, SearchScope.Movies, SearchScope.Series)
    val scopeLabels = mapOf(
        SearchScope.All to stringResource(R.string.search_scope_all),
        SearchScope.LiveTv to stringResource(R.string.search_scope_live),
        SearchScope.Movies to stringResource(R.string.search_scope_movies),
        SearchScope.Series to stringResource(R.string.search_scope_series),
    )

    Column(Modifier.fillMaxSize()) {
        AreTextField(
            value = state.query,
            onValueChange = viewModel::setQuery,
            placeholder = stringResource(R.string.search_placeholder),
            icon = Icons.Filled.Search,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        )
        AreSegmentedControl(
            options = scopes,
            selected = state.scope,
            label = { scopeLabels.getValue(it) },
            onSelect = viewModel::setScope,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        )

        when {
            !state.hasSource -> CenteredHint(stringResource(R.string.search_no_source))
            state.query.trim().length < SearchViewModel.MIN_QUERY_LENGTH -> CenteredHint(stringResource(R.string.search_type_to_search))
            state.channelResults.isEmpty() && state.titleResults.isEmpty() ->
                CenteredHint(stringResource(R.string.search_no_results, state.query))
            else -> Column(Modifier.fillMaxSize().padding(top = 8.dp)) {
                if (state.channelResults.isNotEmpty()) {
                    SectionHeader(stringResource(R.string.search_section_live))
                    LazyRow(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(state.channelResults, key = { it.id }) { channel ->
                            AreChannelTile(
                                channel = channel.name,
                                onClick = { onOpenChannel(channel) },
                                logoUrl = channel.logoUrl,
                                number = channel.number,
                                category = channel.categoryName,
                                isRadio = channel.isRadio,
                                width = SearchChannelWidth,
                                isFavorite = channel.id in favoriteChannelIds,
                                onToggleFavorite = { viewModel.toggleChannelFavorite(channel.id) },
                            )
                        }
                    }
                }
                if (state.titleResults.isNotEmpty()) {
                    SectionHeader(stringResource(R.string.search_section_titles))
                    LazyRow(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(state.titleResults, key = { it.id }) { title ->
                            ArePosterTile(
                                title = title.name,
                                onClick = { onOpenTitle(title) },
                                posterUrl = title.posterUrl,
                                meta = listOfNotNull(title.year, title.categoryName).joinToString(" · ").ifBlank { null },
                                rating = title.rating,
                                width = SearchPosterWidth,
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

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = AreIptvTheme.typography.label,
        color = AreIptvTheme.colors.textSecondary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
    )
}

@Composable
private fun CenteredHint(text: String) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(text, style = AreIptvTheme.typography.body, color = AreIptvTheme.colors.textSecondary)
    }
}
