package com.arashrahimi46.iptv.mobile.ui.series

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arashrahimi46.iptv.data.model.SeriesEpisode
import com.arashrahimi46.iptv.mobile.R
import com.arashrahimi46.iptv.mobile.ui.theme.AreIptvMobileTheme

/**
 * Touch-first episode picker for a series -- the piece :mobile was missing entirely (Series tiles
 * used to route straight into the generic movie player and fail with "no playable stream" since a
 * grouped series [com.arashrahimi46.iptv.data.model.VodTitle] has no stream URL of its own, only
 * its episodes do). Mirrors :tv's `DetailScreen` episode-list scope, without the TV-only metadata/
 * cast/plot panel or D-pad season rail -- a flat, season-grouped scrollable list instead.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeriesDetailScreen(
    vodTitleId: Long,
    onOpenEpisode: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: SeriesDetailViewModel = viewModel(
        factory = SeriesDetailViewModel.factory(
            androidx.compose.ui.platform.LocalContext.current.applicationContext as android.app.Application,
            vodTitleId,
        ),
    ),
) {
    val state by viewModel.uiState.collectAsState()
    val colors = AreIptvMobileTheme.colors

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.title?.name ?: "", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.title == null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.detail_not_found), color = colors.textSecondary)
            }
            else -> SeriesEpisodeList(state, onOpenEpisode, Modifier.padding(padding))
        }
    }
}

@Composable
private fun SeriesEpisodeList(state: SeriesDetailUiState, onOpenEpisode: (Long) -> Unit, modifier: Modifier = Modifier) {
    val colors = AreIptvMobileTheme.colors
    val seasons = state.episodesBySeason.keys.sorted()
    val totalEpisodes = state.episodesBySeason.values.sumOf { it.size }

    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
        item {
            Text(
                text = if (seasons.size > 1) {
                    stringResource(R.string.detail_episodes_header_seasons, seasons.size, totalEpisodes)
                } else if (totalEpisodes > 0) {
                    stringResource(R.string.detail_episodes_header_total, totalEpisodes)
                } else {
                    stringResource(R.string.detail_episodes_header)
                },
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
        when {
            state.episodesLoadError != null -> item {
                Text(
                    stringResource(R.string.detail_episodes_load_error, state.episodesLoadError),
                    color = colors.danger,
                    modifier = Modifier.padding(16.dp),
                )
            }
            state.isM3uSeriesWithoutEpisodes -> item {
                Text(
                    stringResource(R.string.detail_m3u_no_episodes),
                    color = colors.textSecondary,
                    modifier = Modifier.padding(16.dp),
                )
            }
            seasons.isEmpty() -> item {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center) {
                    CircularProgressIndicator()
                }
                Text(
                    stringResource(R.string.detail_episodes_loading),
                    color = colors.textSecondary,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            else -> seasons.forEach { season ->
                val episodes = state.episodesBySeason[season].orEmpty()
                item {
                    Text(
                        text = stringResource(R.string.detail_season_label, season),
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
                    )
                }
                items(episodes) { episode -> EpisodeRow(episode, onClick = { onOpenEpisode(episode.id) }) }
            }
        }
    }
}

@Composable
private fun EpisodeRow(episode: SeriesEpisode, onClick: () -> Unit) {
    val colors = AreIptvMobileTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = colors.textSecondary)
        }
        Column(Modifier.padding(start = 4.dp)) {
            Text(
                text = stringResource(R.string.detail_play_episode, episode.season, episode.episode),
                style = MaterialTheme.typography.labelMedium,
                color = colors.textSecondary,
            )
            Text(
                text = episode.name,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
