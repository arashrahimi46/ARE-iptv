package com.arashrahimi46.iptv.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.arashrahimi46.iptv.data.model.Channel
import com.arashrahimi46.iptv.data.model.VodTitle
import com.arashrahimi46.iptv.ui.components.AreBadge
import com.arashrahimi46.iptv.ui.components.AreBadgeTone
import com.arashrahimi46.iptv.ui.components.AreButton
import com.arashrahimi46.iptv.ui.components.AreButtonVariant
import com.arashrahimi46.iptv.ui.components.AreCategoryCard
import com.arashrahimi46.iptv.ui.components.AreCategoryKind
import com.arashrahimi46.iptv.ui.components.AreChannelTile
import com.arashrahimi46.iptv.ui.components.AreContinueCard
import com.arashrahimi46.iptv.ui.components.AreHero
import com.arashrahimi46.iptv.ui.components.ArePosterTile
import com.arashrahimi46.iptv.ui.components.AreRail
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme

/**
 * Real Home dashboard (Home.jsx): Hero + rails, sourced from the active
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
) {
    val context = LocalContext.current
    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.factory(context.applicationContext as android.app.Application),
    )
    val state by viewModel.uiState.collectAsState()
    val spacing = AreIptvTheme.spacing

    Column(modifier = modifier.padding(bottom = spacing.sp16)) {
        Box(modifier = Modifier.padding(horizontal = spacing.safeX)) {
            if (state.hasSource && (state.channels.isNotEmpty() || state.movies.isNotEmpty())) {
                val featuredChannel = state.channels.firstOrNull()
                val featuredMovie = state.movies.firstOrNull() ?: state.series.firstOrNull()
                AreHero(
                    title = featuredChannel?.name ?: featuredMovie?.name ?: "Welcome to ARE iptv",
                    kicker = if (featuredChannel != null) "Live now" else "Featured",
                    meta = featuredChannel?.categoryName ?: featuredMovie?.let { listOfNotNull(it.year, it.categoryName).joinToString(" · ") },
                    badges = {
                        if (featuredChannel != null) AreBadge("Live", tone = AreBadgeTone.Live, glow = true)
                    },
                    actions = {
                        AreButton(
                            "Play now",
                            onClick = {
                                if (featuredChannel != null) onChannelSelected(featuredChannel) else featuredMovie?.let(onTitleSelected)
                            },
                            variant = AreButtonVariant.Primary,
                        )
                        AreButton(
                            "More info",
                            onClick = { featuredMovie?.let(onTitleSelected) },
                            variant = AreButtonVariant.Secondary,
                        )
                    },
                )
            } else {
                EmptyHero()
            }
        }
        Box(Modifier.padding(top = spacing.sp10))

        // Continue watching: no bookmarks recorded yet in this phase (schema-only), so the rail is hidden.

        if (state.channels.isNotEmpty()) {
            AreRail(title = "Live now") {
                items(state.channels.take(20)) { channel ->
                    AreChannelTile(channel = channel.name, onClick = { onChannelSelected(channel) }, number = channel.number, now = channel.categoryName)
                }
            }
        }

        if (state.categories.isNotEmpty()) {
            AreRail(title = "Browse by category") {
                items(state.categories.take(20)) { category ->
                    AreCategoryCard(name = category.name, onClick = { onCategorySelected(category.name) }, count = category.count, kind = AreCategoryKind.Default)
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
                items(recommended) { title ->
                    ArePosterTile(title = title.name, onClick = { onTitleSelected(title) }, meta = listOfNotNull(title.year, title.categoryName).joinToString(" · "), rating = title.rating)
                }
            }
        }

        if (state.movies.isNotEmpty()) {
            AreRail(title = "Movies") {
                items(state.movies.take(20)) { movie ->
                    ArePosterTile(title = movie.name, onClick = { onTitleSelected(movie) }, meta = listOfNotNull(movie.year, movie.categoryName).joinToString(" · "), rating = movie.rating)
                }
            }
        }

        if (state.series.isNotEmpty()) {
            AreRail(title = "Series") {
                items(state.series.take(20)) { show ->
                    ArePosterTile(title = show.name, onClick = { onTitleSelected(show) }, meta = show.categoryName, rating = show.rating)
                }
            }
        }
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
