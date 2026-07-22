package com.arashrahimi46.iptv.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.arashrahimi46.iptv.data.model.Channel
import com.arashrahimi46.iptv.data.model.VodTitle
import com.arashrahimi46.iptv.ui.components.AreCategoryCard
import com.arashrahimi46.iptv.ui.components.AreCategoryKind
import com.arashrahimi46.iptv.ui.components.AreChannelTile
import com.arashrahimi46.iptv.ui.components.AreContinueCard
import com.arashrahimi46.iptv.ui.components.ArePosterTile
import com.arashrahimi46.iptv.ui.components.AreRail
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.theme.rememberPlaybackFocusRequester

/**
 * Real Home dashboard (Home.jsx): rails (no top Hero banner -- removed per
 * design review so more of the catalog fits without scrolling), sourced from the active
 * playlist's parsed catalog via [HomeViewModel]. Rails degrade gracefully to
 * an empty/onboarding-prompt state rather than crashing on a fresh install
 * with no (or an empty) catalog. Channel/poster tiles open the real
 * [com.arashrahimi46.iptv.ui.player.LivePlayerScreen]/[com.arashrahimi46.iptv.ui.detail.DetailScreen]
 * for that exact row's id -- Home is one of the entry points the spec
 * requires to prove Detail is genuinely content-id-driven, not a single
 * hardcoded record.
 */
@Composable
fun HomeScreen(
    onChannelSelected: (Channel) -> Unit,
    onTitleSelected: (VodTitle) -> Unit,
    modifier: Modifier = Modifier,
    onCategorySelected: (String) -> Unit = {},
    // P1.2: resumes playback directly (bypassing Detail) for a Continue Watching tile --
    // it's the exact bookmarked movie/episode, not "go pick from this title's episode list".
    onResumeVod: (Long) -> Unit = {},
    onResumeEpisode: (Long) -> Unit = {},
) {
    val context = LocalContext.current
    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.factory(context.applicationContext as android.app.Application),
    )
    val state by viewModel.uiState.collectAsState()
    val nowPlayingTitles by viewModel.nowPlayingTitles.collectAsState()
    val spacing = AreIptvTheme.spacing
    // Issue #5: which channel tile started playback -- survives the screen being paused
    // while the player is pushed on top (see [rememberPlaybackFocusRequester]) so Back
    // can restore D-pad focus to it instead of leaving focus on the sidebar.
    var lastPlayedChannelId by rememberSaveable { mutableStateOf<Long?>(null) }

    // Design review: the big top-of-screen Hero banner is gone -- rail tiles/cards
    // below are sized down instead (see the explicit `width =` overrides on each rail
    // item further down) so more of the catalog fits on screen without scrolling as much.
    Column(modifier = modifier.padding(bottom = spacing.sp16)) {
        if (!state.hasSource || (state.channels.isEmpty() && state.movies.isEmpty())) {
            Box(modifier = Modifier.padding(horizontal = spacing.safeX)) {
                // QA LOW defect: a real source existed but Room hadn't emitted its first
                // catalog read yet (cold-start DB open on a large catalog can take several
                // seconds) -- showing EmptyHero() here read as "your playlist vanished"
                // rather than "still loading".
                if (state.isInitializing) LoadingHero() else EmptyHero()
            }
            Box(Modifier.padding(top = spacing.sp10))
        }

        // P1.2: hidden when empty, same as every other rail below.
        if (state.continueWatching.isNotEmpty()) {
            AreRail(title = "Continue Watching", seeAll = false) {
                items(state.continueWatching, key = { it.vodTitleId?.let { id -> "v$id" } ?: "e${it.seriesEpisodeId}" }) { item ->
                    AreContinueCard(
                        title = item.title,
                        onClick = {
                            if (item.vodTitleId != null) onResumeVod(item.vodTitleId) else item.seriesEpisodeId?.let(onResumeEpisode)
                        },
                        meta = item.meta,
                        progress = item.progress,
                        width = 260.dp,
                    )
                }
            }
        }

        if (state.channels.isNotEmpty()) {
            AreRail(title = "Live now") {
                items(state.channels.take(20), key = { it.id }) { channel ->
                    val focusRequester = rememberPlaybackFocusRequester(lastPlayedChannelId, channel.id) { lastPlayedChannelId = null }
                    AreChannelTile(
                        channel = channel.name,
                        onClick = { lastPlayedChannelId = channel.id; onChannelSelected(channel) },
                        number = channel.number,
                        now = nowPlayingTitles[channel.id],
                        logoUrl = channel.logoUrl,
                        width = 260.dp,
                        modifier = Modifier.focusRequester(focusRequester),
                    )
                }
            }
        }

        if (state.categories.isNotEmpty()) {
            AreRail(title = "Browse by category") {
                items(state.categories.take(20), key = { it.name }) { category ->
                    AreCategoryCard(name = category.name, onClick = { onCategorySelected(category.name) }, count = category.count, kind = AreCategoryKind.Default, width = 260.dp)
                }
            }
        }

        // "Recommended" has no real personalization engine yet -- shown as a
        // catalog sample (first titles across movies+series) rather than
        // hardcoded mock data, clearly a placeholder pending a real Phase 4+ engine.
        // Copy and the "Smart"/AI badge are both neutral until there's a real
        // recommender behind this rail (product-lead ruling on qa's Phase 1 finding).
        val recommended = (state.movies + state.series).take(12)
        if (recommended.isNotEmpty()) {
            AreRail(title = "Browse movies & series") {
                items(recommended, key = { it.id }) { title ->
                    ArePosterTile(title = title.name, onClick = { onTitleSelected(title) }, meta = listOfNotNull(title.year, title.categoryName).joinToString(" · "), rating = title.rating, posterUrl = title.posterUrl, width = 168.dp)
                }
            }
        }

        if (state.movies.isNotEmpty()) {
            AreRail(title = "Movies") {
                items(state.movies.take(20), key = { it.id }) { movie ->
                    ArePosterTile(title = movie.name, onClick = { onTitleSelected(movie) }, meta = listOfNotNull(movie.year, movie.categoryName).joinToString(" · "), rating = movie.rating, posterUrl = movie.posterUrl, width = 168.dp)
                }
            }
        }

        if (state.series.isNotEmpty()) {
            AreRail(title = "Series") {
                items(state.series.take(20), key = { it.id }) { show ->
                    ArePosterTile(title = show.name, onClick = { onTitleSelected(show) }, meta = show.categoryName, rating = show.rating, posterUrl = show.posterUrl, width = 168.dp)
                }
            }
        }
    }
}

@Composable
private fun LoadingHero() {
    val colors = AreIptvTheme.colors
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 64.dp)) {
        Text(text = "Loading your catalog…", style = AreIptvTheme.typography.h1, color = colors.textPrimary)
    }
}

@Composable
private fun EmptyHero() {
    val colors = AreIptvTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 64.dp),
    ) {
        Text(text = "No playlist yet", style = AreIptvTheme.typography.h1, color = colors.textPrimary)
        Box(Modifier.padding(top = 8.dp))
        Text(
            text = "Add a playlist from the sidebar to see your channels, movies and series here.",
            style = AreIptvTheme.typography.body,
            color = colors.textSecondary,
        )
    }
}
