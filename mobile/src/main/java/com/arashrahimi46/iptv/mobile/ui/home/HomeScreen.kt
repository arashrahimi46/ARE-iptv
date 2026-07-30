package com.arashrahimi46.iptv.mobile.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arashrahimi46.iptv.mobile.R
import com.arashrahimi46.iptv.data.model.Channel
import com.arashrahimi46.iptv.data.model.VodTitle
import com.arashrahimi46.iptv.ui.components.AreButton
import com.arashrahimi46.iptv.ui.components.AreButtonSize
import com.arashrahimi46.iptv.ui.components.AreButtonVariant
import com.arashrahimi46.iptv.ui.components.AreChannelTile
import com.arashrahimi46.iptv.ui.components.ArePosterTile
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme

/** Phone-scale poster/channel widths for a Home rail. :tv's Home rails (208dp/320dp tokens, or the
 * 168dp/260dp explicit widths tv's own Home passes) assume a ~1920px-wide, arm's-length TV
 * viewport; a phone screen is ~360-411dp wide in portrait, so those would only fit ~1.5 tiles.
 * Sized instead so a rail shows ~3 poster tiles / ~2 channel tiles plus a peek of the next one. */
private val HomePosterWidth = 120.dp
private val HomeChannelWidth = 160.dp

@Composable
fun HomeScreen(
    onOpenChannel: (Channel) -> Unit,
    onOpenTitle: (VodTitle) -> Unit,
    onOpenEpisode: (Long) -> Unit,
    onOpenSearch: () -> Unit = {},
    onOpenGuide: () -> Unit = {},
    viewModel: HomeViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val favoriteChannelIds by viewModel.favoriteChannelIds.collectAsState()
    val favoriteVodIds by viewModel.favoriteVodIds.collectAsState()

    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AreButton(
                text = stringResource(R.string.nav_tv_guide),
                onClick = onOpenGuide,
                icon = Icons.Filled.Schedule,
                variant = AreButtonVariant.Ghost,
                size = AreButtonSize.Small,
            )
            AreButton(
                text = stringResource(R.string.nav_search),
                onClick = onOpenSearch,
                icon = Icons.Filled.Search,
                variant = AreButtonVariant.Ghost,
                size = AreButtonSize.Small,
            )
        }
        HomeContent(state, favoriteChannelIds, favoriteVodIds, viewModel, onOpenChannel, onOpenTitle, onOpenEpisode)
    }
}

@Composable
private fun HomeContent(
    state: HomeUiState,
    favoriteChannelIds: Set<Long>,
    favoriteVodIds: Set<Long>,
    viewModel: HomeViewModel,
    onOpenChannel: (Channel) -> Unit,
    onOpenTitle: (VodTitle) -> Unit,
    onOpenEpisode: (Long) -> Unit,
) {
    val colors = AreIptvTheme.colors
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (state.continueWatching.isNotEmpty()) {
            item {
                HomeRow(stringResource(R.string.home_section_continue_watching)) {
                    items(state.continueWatching, key = { it.id }) { title ->
                        ArePosterTile(
                            title = title.name,
                            onClick = {
                                // A series entry resumes straight into its saved episode (the
                                // user already picked one) instead of reopening the episode
                                // picker; a movie behaves like every other rail.
                                val episodeId = state.continueWatchingEpisodeIds[title.id]
                                if (title.isSeries && episodeId != null) onOpenEpisode(episodeId) else onOpenTitle(title)
                            },
                            posterUrl = title.posterUrl,
                            meta = listOfNotNull(title.year, title.categoryName).joinToString(" · ").ifBlank { null },
                            rating = title.rating,
                            width = HomePosterWidth,
                            isFavorite = title.id in favoriteVodIds,
                            onToggleFavorite = { viewModel.toggleTitleFavorite(title) },
                        )
                    }
                }
            }
        }
        if (state.liveNow.isNotEmpty()) {
            item {
                HomeRow(stringResource(R.string.home_section_live_now)) {
                    items(state.liveNow, key = { it.id }) { channel ->
                        AreChannelTile(
                            channel = channel.name,
                            onClick = { onOpenChannel(channel) },
                            logoUrl = channel.logoUrl,
                            number = channel.number,
                            category = channel.categoryName,
                            isRadio = channel.isRadio,
                            width = HomeChannelWidth,
                            isFavorite = channel.id in favoriteChannelIds,
                            onToggleFavorite = { viewModel.toggleChannelFavorite(channel) },
                        )
                    }
                }
            }
        }
        if (state.recommended.isNotEmpty()) {
            item {
                HomeRow(stringResource(R.string.home_section_for_you)) {
                    items(state.recommended, key = { it.id }) { title ->
                        ArePosterTile(
                            title = title.name,
                            onClick = { onOpenTitle(title) },
                            posterUrl = title.posterUrl,
                            meta = listOfNotNull(title.year, title.categoryName).joinToString(" · ").ifBlank { null },
                            rating = title.rating,
                            width = HomePosterWidth,
                            isFavorite = title.id in favoriteVodIds,
                            onToggleFavorite = { viewModel.toggleTitleFavorite(title) },
                        )
                    }
                }
            }
        }
        if (state.favoriteChannels.isNotEmpty()) {
            item {
                HomeRow(stringResource(R.string.nav_favorites)) {
                    items(state.favoriteChannels, key = { it.id }) { channel ->
                        AreChannelTile(
                            channel = channel.name,
                            onClick = { onOpenChannel(channel) },
                            logoUrl = channel.logoUrl,
                            number = channel.number,
                            category = channel.categoryName,
                            isRadio = channel.isRadio,
                            width = HomeChannelWidth,
                            isFavorite = channel.id in favoriteChannelIds,
                            onToggleFavorite = { viewModel.toggleChannelFavorite(channel) },
                        )
                    }
                }
            }
        }
        if (state.favoriteTitles.isNotEmpty()) {
            item {
                HomeRow(stringResource(R.string.nav_favorites)) {
                    items(state.favoriteTitles, key = { it.id }) { title ->
                        ArePosterTile(
                            title = title.name,
                            onClick = { onOpenTitle(title) },
                            posterUrl = title.posterUrl,
                            meta = listOfNotNull(title.year, title.categoryName).joinToString(" · ").ifBlank { null },
                            rating = title.rating,
                            width = HomePosterWidth,
                            isFavorite = title.id in favoriteVodIds,
                            onToggleFavorite = { viewModel.toggleTitleFavorite(title) },
                        )
                    }
                }
            }
        }
        if (
            state.continueWatching.isEmpty() && state.liveNow.isEmpty() &&
            state.recommended.isEmpty() && state.favoriteChannels.isEmpty() && state.favoriteTitles.isEmpty()
        ) {
            item {
                Text(
                    stringResource(R.string.home_loading_catalog),
                    style = AreIptvTheme.typography.body,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}

/** A titled horizontally-scrolling rail -- the phone equivalent of :tv's [AreRail], but without its
 * "See all" affordance or its `safeX`/`railGap`/`railPeek` spacing tokens: those are sized for a
 * ~1920px TV viewport (64dp side margins, 20dp inter-tile gaps, 80dp peek) and would eat a third of
 * a phone's width. Reusing [AreRail] as-is was tried first and rejected for exactly that reason. */
@Composable
private fun HomeRow(title: String, content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = AreIptvTheme.typography.h3,
            color = AreIptvTheme.colors.textPrimary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            content = content,
        )
    }
}
