package com.arashrahimi46.iptv.ui.live

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Text
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
    val favoriteChannelIds by viewModel.favoriteChannelIds.collectAsState()
    val settings = remember { UserSettings(context) }
    val isListMode by settings.isBrowseListMode.collectAsState(initial = false)
    val colors = AreIptvTheme.colors
    // Issue #5: which channel tile started playback -- see HomeScreen's identical use of
    // rememberPlaybackFocusRequester for the full explanation.
    var lastPlayedChannelId by rememberSaveable { mutableStateOf<Long?>(null) }

    if (!state.hasSource) {
        Text(
            text = "Add a playlist from the sidebar to see live channels here.",
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
            kind = if (index == 0) AreCategoryKind.Live else AreCategoryKind.Default,
        )
    }

    BrowseLayout(
        title = "Live TV",
        titleAccessory = { OnAirNowBadge() },
        categories = categoryOptions,
        selectedIndex = state.selectedCategoryIndex,
        onCategorySelected = viewModel::selectCategory,
        items = state.visibleChannels,
        itemKey = { it.id },
        categoryColumnHeader = "Channel groups",
        sectionTitle = categoryOptions.getOrNull(state.selectedCategoryIndex)?.name,
        sectionCountLabel = { count -> "$count channels" },
        emptyLabel = "No channels in this group yet.",
        listMode = isListMode,
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
        Text(text = "ON AIR NOW", style = AreIptvTheme.typography.caption, color = AreIptvTheme.colors.onAirText)
    }
}
