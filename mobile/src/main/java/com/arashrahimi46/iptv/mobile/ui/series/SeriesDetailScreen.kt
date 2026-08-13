package com.arashrahimi46.iptv.mobile.ui.series

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.arashrahimi46.iptv.core.R as CoreR
import com.arashrahimi46.iptv.mobile.data.model.SeriesEpisode
import com.arashrahimi46.iptv.mobile.data.model.VodTitle
import com.arashrahimi46.iptv.mobile.ui.components.AreBadge
import com.arashrahimi46.iptv.mobile.ui.components.AreBadgeTone
import com.arashrahimi46.iptv.mobile.ui.components.AreButton
import com.arashrahimi46.iptv.mobile.ui.components.AreButtonSize
import com.arashrahimi46.iptv.mobile.ui.components.AreButtonVariant
import com.arashrahimi46.iptv.mobile.ui.components.AreEmptyState
import com.arashrahimi46.iptv.mobile.ui.components.AreErrorState
import com.arashrahimi46.iptv.mobile.ui.components.AreIconButton
import com.arashrahimi46.iptv.mobile.ui.components.AreIconButtonSize
import com.arashrahimi46.iptv.mobile.ui.components.AreIconButtonVariant
import com.arashrahimi46.iptv.mobile.ui.components.AreLoadingState
import com.arashrahimi46.iptv.mobile.ui.components.AreRefreshBox
import com.arashrahimi46.iptv.mobile.ui.components.AreSectionHeader
import com.arashrahimi46.iptv.mobile.ui.components.AreTabs
import com.arashrahimi46.iptv.mobile.ui.components.areTouch
import com.arashrahimi46.iptv.mobile.design.AreIptvTheme
import com.arashrahimi46.iptv.mobile.design.ProvideOnGlass
import com.arashrahimi46.iptv.mobile.design.glassSurface
import com.arashrahimi46.iptv.mobile.design.glassWell

/** Same 4:5 hero the movie screen uses, so the two detail pages read as one family. */
private const val HeroAspect = 4f / 5f

/** Room the sticky action bar needs at the bottom of the scroll. */
private val ActionBarReserve = 112.dp

/**
 * Touch-first series detail: hero + metadata, a season selector, the episode list for the selected
 * season, and a sticky Play/Resume bar placed for one-handed reach.
 *
 * This screen is where the season/episode gap on :mobile closes. A grouped series
 * [VodTitle] carries no stream URL of its own, so the primary action has to resolve to something
 * playable first -- see [resolveSeriesPlayTarget]. Its [SeriesPlayTarget.WholeTitle] branch is the
 * one that unblocks M3U entries typed as series but never split into `SxxEyy` rows: those used to
 * dead-end on "This item has no playable stream" with no way out, because the copy told the user to
 * "use Play above" and there was no Play button.
 *
 * Phone mechanics replacing the TV ones: ripple/press feedback instead of focus rings, a scrollable
 * [AreTabs] season strip instead of a D-pad season rail, pull-to-refresh instead of a Refresh
 * button, 48dp-minimum episode rows.
 */
@Composable
fun SeriesDetailScreen(
    vodTitleId: Long,
    onOpenEpisode: (Long) -> Unit,
    onBack: () -> Unit,
    /** Plays the parent row itself, via `player/movie/{id}` -- see [SeriesPlayTarget.WholeTitle].
     * Defaults to a no-op only so the current nav graph keeps compiling; AppNav MUST wire it. */
    onPlayTitle: (Long) -> Unit = {},
    viewModel: SeriesDetailViewModel = viewModel(
        factory = SeriesDetailViewModel.factory(
            LocalContext.current.applicationContext as android.app.Application,
            vodTitleId,
        ),
    ),
) {
    val state by viewModel.uiState.collectAsState()
    val isFavorite by viewModel.isFavorite.collectAsState()
    val title = state.title

    when {
        state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            AreLoadingState(message = stringResource(CoreR.string.detail_loading))
        }

        title == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            AreEmptyState(
                title = stringResource(CoreR.string.detail_not_found),
                actionLabel = stringResource(CoreR.string.action_back),
                onAction = onBack,
            )
        }

        else -> SeriesDetailContent(
            title = title,
            state = state,
            isFavorite = isFavorite,
            onOpenEpisode = onOpenEpisode,
            onPlayTitle = onPlayTitle,
            onSelectSeason = viewModel::selectSeason,
            onRefresh = viewModel::refreshEpisodes,
            onToggleFavorite = viewModel::toggleFavorite,
            onBack = onBack,
        )
    }
}

@Composable
private fun SeriesDetailContent(
    title: VodTitle,
    state: SeriesDetailUiState,
    isFavorite: Boolean,
    onOpenEpisode: (Long) -> Unit,
    onPlayTitle: (Long) -> Unit,
    onSelectSeason: (Int) -> Unit,
    onRefresh: () -> Unit,
    onToggleFavorite: () -> Unit,
    onBack: () -> Unit,
) {
    val colors = AreIptvTheme.colors
    val listState = rememberLazyListState()

    // Stable list identity, so the season strip isn't rebuilt on every unrelated state emission.
    val seasons = remember(state.episodesBySeason) { state.episodesBySeason.keys.sorted() }
    val selectedSeason = state.selectedSeason ?: seasons.firstOrNull()
    val episodes = selectedSeason?.let { state.episodesBySeason[it] }.orEmpty()
    val totalEpisodes = state.episodesBySeason.values.sumOf { it.size }

    val collapsed by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 240
        }
    }
    val barAlpha by animateFloatAsState(if (collapsed) 1f else 0f, label = "series-detail-bar")

    Box(Modifier.fillMaxSize()) {
        AreRefreshBox(refreshing = state.refreshing, onRefresh = onRefresh, modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = ActionBarReserve),
            ) {
                item(contentType = "hero") { Hero(title) }
                item(contentType = "body") { Body(title) }

                item(contentType = "episodes-header") {
                    AreSectionHeader(
                        text = when {
                            seasons.size > 1 -> stringResource(
                                CoreR.string.detail_episodes_header_seasons,
                                seasons.size,
                                totalEpisodes,
                            )
                            totalEpisodes > 0 -> stringResource(CoreR.string.detail_episodes_header_total, totalEpisodes)
                            else -> stringResource(CoreR.string.detail_episodes_header)
                        },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }

                if (seasons.size > 1 && selectedSeason != null) {
                    item(contentType = "seasons") {
                        AreTabs(
                            options = seasons,
                            selected = selectedSeason,
                            label = { season -> stringResource(CoreR.string.detail_season_label, season) },
                            onSelect = onSelectSeason,
                            modifier = Modifier.padding(horizontal = 16.dp),
                            scrollable = true,
                        )
                    }
                }

                when {
                    // An error is worth surfacing even when cached rows exist -- but with rows on
                    // screen the list itself is the better answer, so only show it when empty.
                    state.episodesLoadError != null && totalEpisodes == 0 -> item(contentType = "state") {
                        AreErrorState(
                            message = stringResource(
                                CoreR.string.detail_episodes_load_error,
                                state.episodesLoadError,
                            ),
                            onRetry = onRefresh,
                        )
                    }

                    !state.episodesLoaded -> item(contentType = "state") {
                        AreLoadingState(message = stringResource(CoreR.string.detail_episodes_loading))
                    }

                    // Loaded and genuinely empty. If the parent row is itself playable the copy
                    // pointing at "Play above" is now true -- the button exists.
                    totalEpisodes == 0 -> item(contentType = "state") {
                        AreEmptyState(
                            title = stringResource(
                                if (!title.streamUrl.isNullOrBlank()) {
                                    CoreR.string.detail_m3u_no_episodes
                                } else {
                                    CoreR.string.series_no_episodes
                                },
                            ),
                        )
                    }

                    else -> items(episodes, key = { it.id }, contentType = { "episode" }) { episode ->
                        EpisodeRow(
                            episode = episode,
                            progress = state.progressByEpisodeId[episode.id] ?: 0f,
                            onClick = { onOpenEpisode(episode.id) },
                        )
                    }
                }
            }
        }

        // Top bar: transparent over the hero, glass once collapsed.
        Box(Modifier.fillMaxWidth().align(Alignment.TopCenter)) {
            Box(
                Modifier
                    .matchParentSize()
                    .alpha(barAlpha)
                    .glassSurface(RectangleShape, elevated = false, shadow = false),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(56.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                AreIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(CoreR.string.action_back),
                    onClick = onBack,
                    variant = AreIconButtonVariant.Glass,
                )
                Text(
                    text = title.name,
                    style = AreIptvTheme.typography.h3,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.alpha(barAlpha),
                )
            }
        }

        // Sticky action bar -- thumb-reachable, above the gesture bar.
        val playTarget = resolveSeriesPlayTarget(state)
        ProvideOnGlass(true) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .glassSurface(RectangleShape, elevated = true, shadow = false)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (playTarget != null) {
                    val firstEpisode = firstEpisode(state)
                    AreButton(
                        text = when {
                            state.resumeEpisodeId != null -> stringResource(CoreR.string.tile_action_resume)
                            playTarget is SeriesPlayTarget.Episode && firstEpisode != null ->
                                stringResource(
                                    CoreR.string.detail_play_episode,
                                    firstEpisode.season,
                                    firstEpisode.episode,
                                )
                            else -> stringResource(CoreR.string.detail_play)
                        },
                        onClick = {
                            when (playTarget) {
                                is SeriesPlayTarget.Episode -> onOpenEpisode(playTarget.episodeId)
                                is SeriesPlayTarget.WholeTitle -> onPlayTitle(playTarget.vodTitleId)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        variant = AreButtonVariant.Primary,
                        size = AreButtonSize.Large,
                        icon = Icons.Filled.PlayArrow,
                        full = true,
                    )
                } else {
                    Box(Modifier.weight(1f))
                }
                AreIconButton(
                    icon = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = stringResource(
                        if (isFavorite) {
                            CoreR.string.detail_remove_from_favorites
                        } else {
                            CoreR.string.detail_add_to_favorites
                        },
                    ),
                    onClick = onToggleFavorite,
                    variant = AreIconButtonVariant.Glass,
                    size = AreIconButtonSize.Large,
                    active = isFavorite,
                )
            }
        }
    }
}

@Composable
private fun Hero(title: VodTitle) {
    val colors = AreIptvTheme.colors
    val scrim = remember(colors.bgBase) {
        Brush.verticalGradient(0.45f to Color.Transparent, 1f to colors.bgBase)
    }
    Box(Modifier.fillMaxWidth().aspectRatio(HeroAspect)) {
        AsyncImage(
            model = title.posterUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().background(colors.surface1),
        )
        Box(Modifier.matchParentSize().background(scrim))
    }
}

@Composable
private fun Body(title: VodTitle) {
    val colors = AreIptvTheme.colors
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text(text = title.name, style = AreIptvTheme.typography.h1, color = colors.textPrimary)

        val meta = listOfNotNull(title.year, title.categoryName, title.genre).joinToString(" · ")
        if (meta.isNotBlank()) {
            Text(
                text = meta,
                style = AreIptvTheme.typography.body,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 6.dp),
            )
        }

        val ratings = listOfNotNull(
            title.rating?.let { "★ $it" },
            title.imdbRating?.let { "IMDb $it" },
            title.rtRating?.let { "RT $it" },
        )
        if (ratings.isNotEmpty()) {
            Row(
                modifier = Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ratings.forEach { AreBadge(text = it, tone = AreBadgeTone.Neutral) }
            }
        }

        title.plot?.takeIf { it.isNotBlank() }?.let { plot ->
            AreSectionHeader(
                text = stringResource(CoreR.string.detail_storyline),
                modifier = Modifier.padding(top = 20.dp),
            )
            Text(text = plot, style = AreIptvTheme.typography.body, color = colors.textPrimary)
        }
    }
}

/**
 * One tappable episode. [progress] > 0 draws a hairline resume bar along the bottom edge, which is
 * also how a half-watched episode is told apart from an unstarted one at a glance -- finished
 * episodes have no bookmark at all (the player clears it past the completion threshold), so they
 * simply show no bar.
 */
@Composable
private fun EpisodeRow(episode: SeriesEpisode, progress: Float, onClick: () -> Unit) {
    val colors = AreIptvTheme.colors
    val shape = RoundedCornerShape(AreIptvTheme.radius.md)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .glassWell(shape)
            .areTouch(onClick = onClick, shape = shape, minTouchTarget = 0.dp)
            .heightIn(min = 56.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                androidx.compose.material3.Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = colors.textSecondary,
                )
            }
            Column(Modifier.padding(start = 4.dp)) {
                Text(
                    text = stringResource(CoreR.string.detail_play_episode, episode.season, episode.episode),
                    style = AreIptvTheme.typography.caption,
                    color = colors.textSecondary,
                )
                Text(
                    text = episode.name,
                    style = AreIptvTheme.typography.body,
                    color = colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (progress > 0f) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(AreIptvTheme.radius.pill))
                    .background(colors.borderDefault),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(progress)
                        .height(3.dp)
                        .background(colors.accent),
                )
            }
        }
    }
}
