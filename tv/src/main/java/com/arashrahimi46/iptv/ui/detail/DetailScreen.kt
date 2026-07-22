package com.arashrahimi46.iptv.ui.detail

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.arashrahimi46.iptv.ui.theme.rememberPlaybackFocusRequester
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import com.arashrahimi46.iptv.data.model.SeriesEpisode
import com.arashrahimi46.iptv.data.model.VodTitle
import com.arashrahimi46.iptv.ui.components.AreButton
import com.arashrahimi46.iptv.ui.components.AreButtonVariant
import com.arashrahimi46.iptv.ui.components.AreChip
import com.arashrahimi46.iptv.ui.components.AreIconButton
import com.arashrahimi46.iptv.ui.components.AreIconButtonVariant
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.theme.TvFocusable

/**
 * Movie/series detail overlay (Detail.jsx), content-id-driven: [contentId] is
 * the real [VodTitle.id] from the NavHost arg the tapped tile carried, so
 * different posters resolve genuinely different rows via [DetailViewModel] --
 * this is exactly the bug the spec calls out in the prototype ("always shows
 * one hardcoded record"), avoided here by construction (see report for how
 * this was verified). Full-bleed, outside
 * [com.arashrahimi46.iptv.ui.shell.AreIptvAppShell] (own NavHost destination),
 * own [BackHandler] popping the real back stack rather than falling through
 * to app-exit.
 *
 * Hero uses [VodTitle.posterUrl] as the only art source -- the schema has no
 * `backdropUrl` column, so this is poster-as-hero, an acceptable v1 shape
 * rather than a true Detail.jsx backdrop. Synopsis/cast are also not in the schema (no field in [VodTitle])
 * -- the meta row (year/rating/category) is shown in their place; a real
 * synopsis needs a schema addition + a metadata source, cut for this phase.
 *
 * Detail.jsx's Cast & crew tab, More like this (related titles) tab and
 * Trailer action are explicit product-lead scope cuts for v1 (backlogged
 * alongside Continue Watching/PiP/catch-up) -- same root cause as the
 * backdrop/synopsis above (no cast/trailer data in the schema, no metadata
 * source to back them), not an oversight.
 */
@Composable
fun DetailScreen(
    contentId: Long,
    onBack: () -> Unit,
    onPlay: (PlayTarget) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val viewModel: DetailViewModel = viewModel(
        factory = DetailViewModel.factory(context.applicationContext as android.app.Application, contentId),
    )
    val state by viewModel.uiState.collectAsState()
    val isFavorite by viewModel.isFavorite.collectAsState()
    val colors = AreIptvTheme.colors
    // Which episode row opened the player -- restore D-pad focus onto it when Back re-enters this
    // screen, instead of letting focus fall to the top Back button. Survives this screen being
    // paused under the player overlay via rememberSaveable. Same mechanism as the browse grids.
    var lastPlayedEpisodeId by rememberSaveable { mutableStateOf<Long?>(null) }

    // Land D-pad focus on Play when the page opens, instead of the top Back button (the first
    // focusable). Skipped when returning from an episode -- there the episode row restores focus
    // via rememberPlaybackFocusRequester, so we must not steal it back to Play.
    val playFocusRequester = remember { FocusRequester() }

    BackHandler(onBack = onBack)

    Box(modifier = modifier.fillMaxSize().background(colors.bgBase)) {
        val title = state.title
        if (state.loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Loading…", style = AreIptvTheme.typography.body, color = colors.textSecondary)
            }
        } else if (title == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Title not found", style = AreIptvTheme.typography.h2, color = colors.textPrimary)
                    Box(Modifier.padding(top = 16.dp))
                    AreIconButton(Icons.Filled.ArrowBack, "Back", onClick = onBack, variant = AreIconButtonVariant.Solid)
                }
            }
        } else {
            LaunchedEffect(title.id) {
                if (lastPlayedEpisodeId == null) {
                    withFrameNanos { }
                    runCatching { playFocusRequester.requestFocus() }
                }
            }
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                Box(Modifier.padding(24.dp, 24.dp, 24.dp, 0.dp)) {
                    AreIconButton(Icons.Filled.ArrowBack, "Back", onClick = onBack, variant = AreIconButtonVariant.Glass)
                }
                Row(
                    modifier = Modifier.padding(horizontal = AreIptvTheme.spacing.safeX, vertical = 40.dp),
                    horizontalArrangement = Arrangement.spacedBy(44.dp),
                ) {
                    HeroArt(title = title)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = title.name, style = AreIptvTheme.typography.display, color = colors.textPrimary)
                        Box(Modifier.padding(top = 16.dp))
                        MetaRow(title)
                        Box(Modifier.padding(top = 26.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            AreButton(
                                text = playLabel(title, state),
                                onClick = { resolvePlayTarget(title, state)?.let(onPlay) },
                                variant = AreButtonVariant.Primary,
                                icon = Icons.Filled.PlayArrow,
                                modifier = Modifier.focusRequester(playFocusRequester),
                            )
                            AreIconButton(
                                icon = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                                onClick = viewModel::toggleFavorite,
                                variant = AreIconButtonVariant.Solid,
                            )
                        }
                    }
                }

                if (title.isSeries) {
                    Box(Modifier.padding(horizontal = AreIptvTheme.spacing.safeX)) {
                        SeriesEpisodesSection(
                            state = state,
                            savedEpisodeId = lastPlayedEpisodeId,
                            onEpisodeConsumed = { lastPlayedEpisodeId = null },
                            onPlayEpisode = { episode ->
                                lastPlayedEpisodeId = episode.id
                                onPlay(PlayTarget.Episode(episode.id))
                            },
                        )
                    }
                    Box(Modifier.padding(bottom = 40.dp))
                }
            }
        }
    }
}

/** Where Play should navigate -- resolved from real Room rows, never a hardcoded stream. */
sealed class PlayTarget {
    data class Movie(val vodTitleId: Long) : PlayTarget()
    data class Episode(val episodeId: Long) : PlayTarget()
}

private fun playLabel(title: VodTitle, state: DetailUiState): String = when {
    !title.isSeries -> "Play"
    state.episodesBySeason.isNotEmpty() -> {
        val firstSeason = state.episodesBySeason.keys.min()
        val firstEpisode = state.episodesBySeason.getValue(firstSeason).minByOrNull { it.episode }
        "Play S$firstSeason · E${firstEpisode?.episode ?: 1}"
    }
    else -> "Play"
}

private fun resolvePlayTarget(title: VodTitle, state: DetailUiState): PlayTarget? = when {
    !title.isSeries -> title.streamUrl?.let { PlayTarget.Movie(title.id) }
    state.episodesBySeason.isNotEmpty() -> {
        val firstSeason = state.episodesBySeason.keys.min()
        state.episodesBySeason.getValue(firstSeason).minByOrNull { it.episode }?.let { PlayTarget.Episode(it.id) }
    }
    // M3U series (or an Xtream series with no episodes returned): the title row itself is playable.
    title.streamUrl != null -> PlayTarget.Movie(title.id)
    else -> null
}

@Composable
private fun HeroArt(title: VodTitle) {
    val colors = AreIptvTheme.colors
    val shape = RoundedCornerShape(AreIptvTheme.radius.lg)
    val initials = title.name.split(" ").take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")
    Box(
        modifier = Modifier
            .width(240.dp)
            .aspectRatio(2f / 3f)
            .background(colors.surface3, shape),
        contentAlignment = Alignment.Center,
    ) {
        // Initials show only until the poster resolves (avoids bleed-through on transparent/
        // letterboxed art). onState is a lightweight callback, not SubcomposeAsyncImage.
        val posterLoaded = remember(title.posterUrl) { mutableStateOf(false) }
        if (title.posterUrl == null || !posterLoaded.value) {
            Text(text = initials, style = AreIptvTheme.typography.display, color = colors.textTertiary)
        }
        if (title.posterUrl != null) {
            AsyncImage(
                model = title.posterUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                onState = { posterLoaded.value = it is AsyncImagePainter.State.Success },
                modifier = Modifier.fillMaxSize().clip(shape),
            )
        }
    }
}

@Composable
private fun MetaRow(title: VodTitle) {
    val colors = AreIptvTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        if (title.rating != null) {
            Text(text = "★ ${title.rating}", style = AreIptvTheme.typography.label, color = colors.textPrimary)
        }
        if (title.year != null) {
            Text(text = title.year, style = AreIptvTheme.typography.body, color = colors.textSecondary)
        }
        if (title.categoryName != null) {
            Text(text = title.categoryName, style = AreIptvTheme.typography.body, color = colors.textSecondary)
        }
    }
}

@Composable
private fun SeriesEpisodesSection(
    state: DetailUiState,
    savedEpisodeId: Long?,
    onEpisodeConsumed: () -> Unit,
    onPlayEpisode: (SeriesEpisode) -> Unit,
) {
    val colors = AreIptvTheme.colors
    val totalEpisodes = state.episodesBySeason.values.sumOf { it.size }
    val seasons = remember(state.episodesBySeason) { state.episodesBySeason.keys.sorted() }
    // Restore to the season that CONTAINS the episode we're returning from (Back out of the
    // player), so the focus restore below can find it -- otherwise a multi-season show resets to
    // season 1 and the played episode isn't even on screen. Falls back to the first season.
    val restoredSeason = remember(seasons, savedEpisodeId) {
        savedEpisodeId
            ?.let { id -> state.episodesBySeason.entries.firstOrNull { (_, eps) -> eps.any { it.id == id } }?.key }
            ?: seasons.firstOrNull()
    }
    // Selected season tab -- defaults to the restored season, resets if the season set changes.
    var selectedSeason by remember(seasons) { mutableStateOf(restoredSeason) }
    Column {
        val header = if (totalEpisodes > 0) {
            val seasonLabel = if (seasons.size > 1) "${seasons.size} seasons · " else ""
            "Episodes · $seasonLabel$totalEpisodes total"
        } else {
            "Episodes"
        }
        Text(text = header, style = AreIptvTheme.typography.h2, color = colors.textPrimary)
        Box(Modifier.padding(top = 18.dp))
        when {
            state.episodesBySeason.isNotEmpty() -> {
                // Season tabs: pick a season, only its episodes render below -- so a many-season
                // show is a couple of clicks to any episode instead of a long vertical scroll.
                if (seasons.size > 1) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        seasons.forEach { season ->
                            AreChip(
                                text = "Season $season",
                                onClick = { selectedSeason = season },
                                selected = season == selectedSeason,
                            )
                        }
                    }
                    Box(Modifier.padding(top = 18.dp))
                }
                val episodes = state.episodesBySeason[selectedSeason].orEmpty().sortedBy { it.episode }
                Text(
                    text = "Season ${selectedSeason ?: ""} · ${episodes.size} episodes",
                    style = AreIptvTheme.typography.h3,
                    color = colors.textSecondary,
                )
                Box(Modifier.padding(top = 10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    episodes.forEach { episode ->
                        val focusRequester = rememberPlaybackFocusRequester(savedEpisodeId, episode.id, onEpisodeConsumed)
                        EpisodeRow(
                            episode = episode,
                            onClick = { onPlayEpisode(episode) },
                            modifier = Modifier.focusRequester(focusRequester),
                        )
                    }
                }
            }
            state.isM3uSeriesWithoutEpisodes -> {
                // Documented M3U limitation (not a bug): playlists provide no authoritative
                // season/episode structure, so a series-typed M3U entry plays as one item.
                Text(
                    text = "This playlist entry has no season/episode data -- use Play above to watch it as a single item.",
                    style = AreIptvTheme.typography.body,
                    color = colors.textSecondary,
                )
            }
            state.episodesLoadError != null -> {
                Text(text = "Couldn't load episodes: ${state.episodesLoadError}", style = AreIptvTheme.typography.body, color = colors.danger)
            }
            else -> {
                Text(text = "Loading episodes…", style = AreIptvTheme.typography.body, color = colors.textSecondary)
            }
        }
    }
}

@Composable
private fun EpisodeRow(episode: SeriesEpisode, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = AreIptvTheme.colors
    TvFocusable(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AreIptvTheme.radius.md),
        backgroundColor = colors.surface1,
    ) { _, _ ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "${episode.episode}. ${episode.name}",
                style = AreIptvTheme.typography.label,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
