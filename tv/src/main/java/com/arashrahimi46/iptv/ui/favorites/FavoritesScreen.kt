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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Text
import com.arashrahimi46.iptv.data.model.Channel
import com.arashrahimi46.iptv.data.model.VodTitle
import com.arashrahimi46.iptv.ui.components.AreBadge
import com.arashrahimi46.iptv.ui.components.AreBadgeTone
import com.arashrahimi46.iptv.ui.components.AreChannelTile
import com.arashrahimi46.iptv.ui.components.ArePosterTile
import com.arashrahimi46.iptv.ui.components.AreTabs
import com.arashrahimi46.iptv.ui.components.TabItem
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme

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
    var tab by remember { mutableStateOf("channels") }

    if (!state.hasSource) {
        Text(
            text = "Add a playlist from the sidebar, then favorite channels or titles to see them here.",
            style = AreIptvTheme.typography.body,
            color = colors.textSecondary,
            modifier = modifier.padding(horizontal = spacing.safeX, vertical = spacing.sp10),
        )
        return
    }

    Column(modifier = modifier.padding(top = spacing.sp6, bottom = spacing.sp10)) {
        Box(Modifier.padding(horizontal = spacing.safeX)) {
            Text(text = "Favorites", style = AreIptvTheme.typography.display, color = colors.textPrimary)
            Box(Modifier.padding(top = spacing.sp6))
            AreTabs(
                items = listOf(
                    TabItem("channels", "Channels"),
                    TabItem("movies", "Movies"),
                    TabItem("sports", "Sports"),
                    TabItem("kids", "Kids"),
                ),
                active = tab,
                onSelect = { tab = it },
            )
        }

        Box(Modifier.padding(top = spacing.sp8))

        Box(Modifier.padding(horizontal = spacing.safeX, vertical = 0.dp)) {
            RecentlyFavoritedNote()
        }
        Box(Modifier.padding(top = spacing.sp6))

        Box(Modifier.padding(horizontal = spacing.safeX)) {
            when (tab) {
                "channels" -> ChannelGrid(
                    channels = state.channels,
                    emptyLabel = "No favorited channels yet -- tap the heart on a channel tile to add one.",
                    onChannelSelected = onChannelSelected,
                    onToggleFavorite = viewModel::toggleChannelFavorite,
                )
                "movies" -> MovieGrid(
                    movies = state.movies,
                    emptyLabel = "No favorited movies yet -- tap the heart on a movie tile to add one.",
                    onTitleSelected = onTitleSelected,
                    onToggleFavorite = viewModel::toggleVodFavorite,
                )
                "sports" -> MixedGrid(
                    items = state.sports,
                    emptyLabel = "No favorited sports content yet -- favorite a channel or title in a Sports category.",
                    onChannelSelected = onChannelSelected,
                    onTitleSelected = onTitleSelected,
                    onToggleChannelFavorite = viewModel::toggleChannelFavorite,
                    onToggleVodFavorite = viewModel::toggleVodFavorite,
                )
                "kids" -> MixedGrid(
                    items = state.kids,
                    emptyLabel = "No favorited kids content yet -- favorite a channel or title in a Kids category.",
                    onChannelSelected = onChannelSelected,
                    onTitleSelected = onTitleSelected,
                    onToggleChannelFavorite = viewModel::toggleChannelFavorite,
                    onToggleVodFavorite = viewModel::toggleVodFavorite,
                )
            }
        }
    }
}

/**
 * Honest replacement for Favorites.jsx's "Smart favorites / frequently
 * watched" copy -- there's no watch-history data to back a "frequently
 * watched" claim yet (see [FavoritesViewModel] doc), so this states the real
 * sort order instead of fabricating one.
 */
@Composable
private fun RecentlyFavoritedNote() {
    val colors = AreIptvTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        AreBadge("Recently favorited", tone = AreBadgeTone.Smart)
        Text(text = "Sorted by when you favorited it, most recent first.", style = AreIptvTheme.typography.caption, color = colors.textTertiary)
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
    FlowRow(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        channels.forEach { channel ->
            AreChannelTile(
                channel = channel.name,
                onClick = { onChannelSelected(channel.id) },
                logoUrl = channel.logoUrl,
                number = channel.number,
                now = channel.categoryName,
                isFavorite = true,
                onToggleFavorite = { onToggleFavorite(channel.id) },
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
    FlowRow(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        movies.forEach { movie ->
            ArePosterTile(
                title = movie.name,
                onClick = { onTitleSelected(movie) },
                posterUrl = movie.posterUrl,
                meta = listOfNotNull(movie.year, movie.categoryName).joinToString(" · ").ifEmpty { null },
                rating = movie.rating,
                isFavorite = true,
                onToggleFavorite = { onToggleFavorite(movie) },
            )
        }
    }
}

@Composable
private fun MixedGrid(
    items: List<FavoriteContent>,
    emptyLabel: String,
    onChannelSelected: (Long) -> Unit,
    onTitleSelected: (VodTitle) -> Unit,
    onToggleChannelFavorite: (Long) -> Unit,
    onToggleVodFavorite: (VodTitle) -> Unit,
) {
    val colors = AreIptvTheme.colors
    if (items.isEmpty()) {
        Text(text = emptyLabel, style = AreIptvTheme.typography.body, color = colors.textSecondary)
        return
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        items.forEach { item ->
            when (item) {
                is FavoriteContent.ChannelItem -> AreChannelTile(
                    channel = item.channel.name,
                    onClick = { onChannelSelected(item.channel.id) },
                    logoUrl = item.channel.logoUrl,
                    number = item.channel.number,
                    now = item.channel.categoryName,
                    isFavorite = true,
                    onToggleFavorite = { onToggleChannelFavorite(item.channel.id) },
                )
                is FavoriteContent.TitleItem -> ArePosterTile(
                    title = item.title.name,
                    onClick = { onTitleSelected(item.title) },
                    posterUrl = item.title.posterUrl,
                    meta = listOfNotNull(item.title.year, item.title.categoryName).joinToString(" · ").ifEmpty { null },
                    rating = item.title.rating,
                    isFavorite = true,
                    onToggleFavorite = { onToggleVodFavorite(item.title) },
                )
            }
        }
    }
}
