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
import com.arashrahimi46.iptv.core.R
import com.arashrahimi46.iptv.data.model.Channel
import com.arashrahimi46.iptv.data.settings.UserSettings
import com.arashrahimi46.iptv.ui.browse.BrowseCategoryOption
import com.arashrahimi46.iptv.ui.browse.BrowseLayout
import com.arashrahimi46.iptv.ui.browse.BrowseTileActions
import com.arashrahimi46.iptv.ui.components.AreCategoryKind
import com.arashrahimi46.iptv.ui.components.AreChannelTile
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.theme.requestFocusWhenReady
import com.arashrahimi46.iptv.ui.theme.glassChild
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

    // Initial D-pad focus: the persistent shell leaves focus on the sidebar, so without this the
    // content reads as dead until the user blindly presses RIGHT. Runs on EVERY entry to the tab
    // (not once per session) so every screen behaves the way Streams already did.
    // The one exception is returning from the player, where rememberPlaybackFocusRequester restores
    // focus to the tile you launched -- landing back on your place beats landing at the top.
    val contentFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        if (lastPlayedChannelId == null) contentFocusRequester.requestFocusWhenReady()
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

    // remember-ed: this is a param to [BrowseLayout], and rebuilding the List (with fresh
    // BrowseCategoryOption instances) on every recomposition meant the param never compared equal, so
    // BrowseLayout could never skip -- taking the whole category column and every visible grid item's
    // content lambda with it. It re-ran on any favourites emission and any paging state change.
    val categoryOptions = remember(state.categories) {
        state.categories.mapIndexed { index, category ->
            BrowseCategoryOption(
                name = category.name,
                count = category.count,
                kind = if (index == 1) AreCategoryKind.Live else AreCategoryKind.Default,
                pinned = category.pinned,
                // index 0 = "Favorites", 1 = "All channels" -- neither is pinnable.
                pinnable = index >= 2,
            )
        }
    }
    // Hoisted out of the item lambda below. `favoriteChannelIds` is a Set -- an unstable type -- so
    // capturing it directly made the trailing lambda a new instance on every recomposition, which
    // defeats strong skipping's lambda memoization for the same reason as above.
    val isFavorite = remember(favoriteChannelIds) { { id: Long -> id in favoriteChannelIds } }

    // Hold-OK menu for a channel tile. A Live tile's click plays directly, so Play is wired to the
    // same path. No Resume/Play-from-start: live channels have no continue-watching progress.
    val tileActions = remember(isFavorite) {
        BrowseTileActions<Channel>(
            title = { it.name },
            isFavorite = { isFavorite(it.id) },
            onToggleFavorite = { viewModel.toggleFavorite(it.id) },
            onPlay = { lastPlayedChannelId = it.id; onChannelSelected(it.id) },
        )
    }

    BrowseLayout(
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
        tileActions = tileActions,
        modifier = modifier,
    ) { channel, onLongClick ->
        val focusRequester = rememberPlaybackFocusRequester(lastPlayedChannelId, channel.id) { lastPlayedChannelId = null }
        AreChannelTile(
            channel = channel.name,
            onClick = { lastPlayedChannelId = channel.id; onChannelSelected(channel.id) },
            number = channel.number,
            category = channel.categoryName,
            isRadio = channel.isRadio,
            logoUrl = channel.logoUrl,
            isFavorite = isFavorite(channel.id),
            onToggleFavorite = { viewModel.toggleFavorite(channel.id) },
            onLongClick = onLongClick,
            fillWidth = true,
            modifier = Modifier.focusRequester(focusRequester),
        )
    }
}

@Composable
private fun OnAirNowBadge() {
    val colors = AreIptvTheme.colors
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(AreIptvTheme.radius.pill)
    androidx.compose.foundation.layout.Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(7.dp),
        modifier = Modifier
            // Glass, not a flat red slab: the neutral glass material carries the shape (tint +
            // hairline, per glassChild), and the live colour goes on as a WASH over it rather than
            // as the fill. A solid `live * 0.14` read as a muddy maroon rectangle -- opaque-looking
            // on a translucent page, and the one badge in the app that ignored the glass language.
            .glassChild(shape)
            .background(colors.live.copy(alpha = 0.10f), shape)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        androidx.compose.foundation.layout.Box(Modifier.size(8.dp).background(colors.live, CircleShape))
        Text(text = stringResource(R.string.live_on_air_now), style = AreIptvTheme.typography.caption, color = AreIptvTheme.colors.onAirText, maxLines = 1, softWrap = false)
    }
}
