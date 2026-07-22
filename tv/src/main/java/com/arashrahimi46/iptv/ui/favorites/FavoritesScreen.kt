package com.arashrahimi46.iptv.ui.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.arashrahimi46.iptv.ui.components.AreChannelTile
import com.arashrahimi46.iptv.ui.components.AreChip
import com.arashrahimi46.iptv.ui.components.ArePosterTile
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
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

    Column(modifier = modifier.padding(top = spacing.sp6, bottom = spacing.sp10)) {
        Column(Modifier.padding(horizontal = spacing.safeX)) {
            Text(text = stringResource(R.string.favorites_title), style = AreIptvTheme.typography.display, color = colors.textPrimary)
            Box(Modifier.padding(top = spacing.sp6))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(
                    "channels" to stringResource(R.string.favorites_tab_channels),
                    "movies" to stringResource(R.string.favorites_tab_movies),
                    "series" to stringResource(R.string.favorites_tab_series),
                ).forEach { (id, label) ->
                    AreChip(text = label, selected = id == tab, onClick = { selectedTab = id })
                }
            }
        }

        Box(Modifier.padding(top = spacing.sp8))

        Box(Modifier.padding(horizontal = spacing.safeX)) {
            when (tab) {
                "channels" -> ChannelGrid(
                    channels = state.channels,
                    emptyLabel = stringResource(R.string.favorites_empty_channels),
                    onChannelSelected = onChannelSelected,
                    onToggleFavorite = viewModel::toggleChannelFavorite,
                )
                "movies" -> MovieGrid(
                    movies = state.movies,
                    emptyLabel = stringResource(R.string.favorites_empty_movies),
                    onTitleSelected = onTitleSelected,
                    onToggleFavorite = viewModel::toggleVodFavorite,
                )
                "series" -> MovieGrid(
                    movies = state.series,
                    emptyLabel = stringResource(R.string.favorites_empty_series),
                    onTitleSelected = onTitleSelected,
                    onToggleFavorite = viewModel::toggleVodFavorite,
                )
            }
        }
    }
}

@Composable
private fun ChannelGrid(
    channels: List<Channel>,
    emptyLabel: String,
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
    FlowRow(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        channels.forEach { channel ->
            val focusRequester = rememberPlaybackFocusRequester(lastPlayedId, channel.id) { lastPlayedId = null }
            AreChannelTile(
                channel = channel.name,
                onClick = { lastPlayedId = channel.id; onChannelSelected(channel.id) },
                logoUrl = channel.logoUrl,
                number = channel.number,
                now = channel.categoryName,
                isFavorite = true,
                onToggleFavorite = { onToggleFavorite(channel.id) },
                modifier = Modifier.focusRequester(focusRequester),
            )
        }
    }
}

@Composable
private fun MovieGrid(
    movies: List<VodTitle>,
    emptyLabel: String,
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
    FlowRow(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        movies.forEach { movie ->
            val focusRequester = rememberPlaybackFocusRequester(lastPlayedId, movie.id) { lastPlayedId = null }
            ArePosterTile(
                title = movie.name,
                onClick = { lastPlayedId = movie.id; onTitleSelected(movie) },
                posterUrl = movie.posterUrl,
                meta = listOfNotNull(movie.year, movie.categoryName).joinToString(" · ").ifEmpty { null },
                rating = movie.rating,
                isFavorite = true,
                onToggleFavorite = { onToggleFavorite(movie) },
                focusRequester = focusRequester,
            )
        }
    }
}
