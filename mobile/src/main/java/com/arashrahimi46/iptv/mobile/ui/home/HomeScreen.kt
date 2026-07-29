package com.arashrahimi46.iptv.mobile.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import com.arashrahimi46.iptv.mobile.ui.components.ChannelRow
import com.arashrahimi46.iptv.mobile.ui.components.HomeRow
import com.arashrahimi46.iptv.mobile.ui.components.PosterRow
import com.arashrahimi46.iptv.mobile.ui.theme.AreIptvMobileTheme

@Composable
fun HomeScreen(
    onOpenChannel: (Channel) -> Unit,
    onOpenTitle: (VodTitle) -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val favoriteChannelIds by viewModel.favoriteChannelIds.collectAsState()
    val favoriteVodIds by viewModel.favoriteVodIds.collectAsState()
    val colors = AreIptvMobileTheme.colors

    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (state.continueWatching.isNotEmpty()) {
            item {
                HomeRow(stringResource(R.string.home_section_continue_watching)) {
                    PosterRow(state.continueWatching, onOpenTitle, favoriteVodIds, viewModel::toggleTitleFavorite)
                }
            }
        }
        if (state.liveNow.isNotEmpty()) {
            item {
                HomeRow(stringResource(R.string.home_section_live_now)) {
                    ChannelRow(state.liveNow, onOpenChannel, favoriteChannelIds, viewModel::toggleChannelFavorite)
                }
            }
        }
        if (state.recommended.isNotEmpty()) {
            item {
                HomeRow(stringResource(R.string.home_section_for_you)) {
                    PosterRow(state.recommended, onOpenTitle, favoriteVodIds, viewModel::toggleTitleFavorite)
                }
            }
        }
        if (state.favoriteChannels.isNotEmpty()) {
            item {
                HomeRow(stringResource(R.string.nav_favorites)) {
                    ChannelRow(state.favoriteChannels, onOpenChannel, favoriteChannelIds, viewModel::toggleChannelFavorite)
                }
            }
        }
        if (state.favoriteTitles.isNotEmpty()) {
            item {
                HomeRow(stringResource(R.string.nav_favorites)) {
                    PosterRow(state.favoriteTitles, onOpenTitle, favoriteVodIds, viewModel::toggleTitleFavorite)
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
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}
