package com.arashrahimi46.iptv.ui.live

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.tv.material3.Text
import com.arashrahimi46.iptv.R
import com.arashrahimi46.iptv.data.settings.UserSettings
import com.arashrahimi46.iptv.ui.browse.BrowseCategoryOption
import com.arashrahimi46.iptv.ui.browse.BrowseLayout
import com.arashrahimi46.iptv.ui.components.AreCategoryKind
import com.arashrahimi46.iptv.ui.components.AreChannelTile
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.theme.rememberPlaybackFocusRequester

/**
 * Live TV browse (Live.jsx): sticky category column + channel grid, backed by
 * the real [com.arashrahimi46.iptv.data.model.Channel] catalog via [LiveViewModel].
 * Selecting a tile opens the real [com.arashrahimi46.iptv.ui.player.LivePlayerScreen]
 * (own NavHost destination) with that channel's id.
 */
@Composable
fun LiveScreen(onChannelSelected: (channelId: Long) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val viewModel: LiveViewModel = viewModel(
        factory = LiveViewModel.factory(context.applicationContext as android.app.Application),
    )
    val state by viewModel.uiState.collectAsState()
    val channels = viewModel.pagingData.collectAsLazyPagingItems()
    val favoriteChannelIds by viewModel.favoriteChannelIds.collectAsState()
    val settings = remember { UserSettings(context) }
    val isListMode by settings.isBrowseListMode.collectAsState(initial = false)
    val colors = AreIptvTheme.colors
    val liveCountChannelsTemplate = stringResource(R.string.live_count_channels)
    // Issue #5: which channel tile started playback -- see HomeScreen's identical use of
    // rememberPlaybackFocusRequester for the full explanation.
    var lastPlayedChannelId by rememberSaveable { mutableStateOf<Long?>(null) }

    // Initial D-pad focus: on genuine first entry the persistent shell leaves focus on the
    // sidebar and the category column reads as dead until the user blindly presses RIGHT.
    // Drive focus into the category column once -- but NOT when returning from the player
    // (lastPlayedChannelId != null), where rememberPlaybackFocusRequester restores focus to
    // the launched tile instead. initialFocusDone (rememberSaveable) keeps it to first entry.
    val contentFocusRequester = remember { FocusRequester() }
    var initialFocusDone by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!initialFocusDone && lastPlayedChannelId == null) {
            withFrameNanos { }
            runCatching { contentFocusRequester.requestFocus() }
            initialFocusDone = true
        }
    }

    if (!state.hasSource) {
        Text(
            text = stringResource(R.string.live_no_source),
            style = AreIptvTheme.typography.body,
            color = colors.textSecondary,
            modifier = modifier.padding(horizontal = AreIptvTheme.spacing.safeX, vertical = AreIptvTheme.spacing.sp10),
        )
        return
    }

    val categoryOptions = state.categories.mapIndexed { index, category ->
        BrowseCategoryOption(
            name = category.name,
            count = category.count,
            kind = if (index == 1) AreCategoryKind.Live else AreCategoryKind.Default,
            pinned = category.pinned,
            // index 0 = "Favorites", 1 = "All channels" -- neither is pinnable.
            pinnable = index >= 2,
        )
    }

    BrowseLayout(
        title = stringResource(R.string.live_title),
        titleAccessory = { OnAirNowBadge() },
        categories = categoryOptions,
        selectedIndex = state.selectedCategoryIndex,
        onCategorySelected = viewModel::selectCategory,
        onCategoryPinToggle = viewModel::togglePin,
        items = channels,
        itemKey = { it.id },
        categoryColumnHeader = stringResource(R.string.live_channel_groups),
        sectionTitle = categoryOptions.getOrNull(state.selectedCategoryIndex)?.name,
        sectionCount = state.selectedCount,
        sectionCountLabel = { count -> String.format(liveCountChannelsTemplate, count) },
        emptyLabel = stringResource(R.string.live_empty_group),
        listMode = isListMode,
        // Responsive channel grid (was one fixed 320dp tile per row): ~180dp columns that the tiles
        // fill, so the content pane packs 2-3 readable channel cards per row instead of one big tile.
        minItemWidth = 180.dp,
        contentFocusRequester = contentFocusRequester,
        modifier = modifier,
    ) { channel ->
        val focusRequester = rememberPlaybackFocusRequester(lastPlayedChannelId, channel.id) { lastPlayedChannelId = null }
        AreChannelTile(
            channel = channel.name,
            onClick = { lastPlayedChannelId = channel.id; onChannelSelected(channel.id) },
            number = channel.number,
            now = channel.categoryName,
            logoUrl = channel.logoUrl,
            isFavorite = channel.id in favoriteChannelIds,
            onToggleFavorite = { viewModel.toggleFavorite(channel.id) },
            fillWidth = true,
            modifier = Modifier.focusRequester(focusRequester),
        )
    }
}

@Composable
private fun OnAirNowBadge() {
    val colors = AreIptvTheme.colors
    androidx.compose.foundation.layout.Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(7.dp),
        modifier = Modifier
            .background(colors.live.copy(alpha = 0.14f), androidx.compose.foundation.shape.RoundedCornerShape(AreIptvTheme.radius.pill))
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        androidx.compose.foundation.layout.Box(Modifier.size(8.dp).background(colors.live, CircleShape))
        Text(text = stringResource(R.string.live_on_air_now), style = AreIptvTheme.typography.caption, color = AreIptvTheme.colors.onAirText, maxLines = 1, softWrap = false)
    }
}
