package com.arashrahimi46.iptv.mobile.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arashrahimi46.iptv.core.R as CoreR
import com.arashrahimi46.iptv.mobile.data.model.Channel
import com.arashrahimi46.iptv.mobile.data.model.VodTitle
import com.arashrahimi46.iptv.mobile.ui.components.AreChannelTile
import com.arashrahimi46.iptv.mobile.ui.components.AreEmptyState
import com.arashrahimi46.iptv.mobile.ui.components.AreIconButton
import com.arashrahimi46.iptv.mobile.ui.components.ArePosterTile
import com.arashrahimi46.iptv.mobile.ui.components.AreRail
import com.arashrahimi46.iptv.mobile.ui.components.AreRefreshBox
import com.arashrahimi46.iptv.mobile.ui.components.AreScreenScaffold
import com.arashrahimi46.iptv.mobile.ui.components.AreSkeleton
import com.arashrahimi46.iptv.mobile.ui.components.AreTileActionSheet
import com.arashrahimi46.iptv.mobile.ui.components.areTouch
import com.arashrahimi46.iptv.mobile.design.AreIptvTheme
import com.arashrahimi46.iptv.mobile.design.ProvideOnGlass
import com.arashrahimi46.iptv.mobile.design.glassSurface

/** Phone-scale rail tile widths. The TV `tilePosterWidth`/`tileLandWidth` tokens (208dp/320dp) are
 * sized for a ~1920px arm's-length viewport; on a ~360-411dp portrait phone they would fit ~1.5
 * tiles. These show ~2.5 posters / ~2 channel tiles plus a peek of the next one. */
private val HomePosterWidth = 132.dp
private val HomeChannelWidth = 168.dp

/** What a long press opened the action sheet for. */
private sealed interface HomeTileAction {
    data class OnChannel(val channel: Channel) : HomeTileAction
    data class OnTitle(val title: VodTitle, val resumeEpisodeId: Long?) : HomeTileAction
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenChannel: (Channel) -> Unit,
    onOpenTitle: (VodTitle) -> Unit,
    onOpenEpisode: (Long) -> Unit,
    onOpenSearch: () -> Unit = {},
    onOpenGuide: () -> Unit = {},
    // Favorites / Recordings / Streams are reachable from Home, not buried in Settings. They are
    // library destinations you visit while browsing, not preferences you configure once, and
    // Settings is the wrong shelf for them (design §4.2 said otherwise; that was wrong in use).
    onOpenFavorites: () -> Unit = {},
    onOpenRecordings: () -> Unit = {},
    onOpenStreams: () -> Unit = {},
    viewModel: HomeViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val favoriteChannelIds by viewModel.favoriteChannelIds.collectAsState()
    val favoriteVodIds by viewModel.favoriteVodIds.collectAsState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    var sheet by remember { mutableStateOf<HomeTileAction?>(null) }

    AreScreenScaffold(
        title = stringResource(CoreR.string.app_name),
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        scrollBehavior = scrollBehavior,
        actions = {
            AreIconButton(
                icon = Icons.Filled.Search,
                contentDescription = stringResource(CoreR.string.nav_search),
                onClick = onOpenSearch,
            )
            AreIconButton(
                icon = Icons.Filled.CalendarMonth,
                contentDescription = stringResource(CoreR.string.nav_tv_guide),
                onClick = onOpenGuide,
            )
        },
    ) { inset ->
        val contentPadding = PaddingValues(
            top = inset.calculateTopPadding() + 8.dp,
            bottom = inset.calculateBottomPadding() + 24.dp,
        )
        AreRefreshBox(
            refreshing = isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            when {
                state.isLoading -> HomeSkeletons(contentPadding)
                !state.hasSource || state.isEmpty -> AreEmptyState(
                    title = stringResource(CoreR.string.home_no_playlist_title),
                    message = stringResource(CoreR.string.home_no_playlist_body),
                    icon = Icons.Filled.Tv,
                    modifier = Modifier.padding(contentPadding),
                )
                else -> HomeRails(
                    state = state,
                    contentPadding = contentPadding,
                    onOpenRecordings = onOpenRecordings,
                    onOpenStreams = onOpenStreams,
                    favoriteChannelIds = favoriteChannelIds,
                    favoriteVodIds = favoriteVodIds,
                    onToggleChannelFavorite = viewModel::toggleChannelFavorite,
                    onToggleTitleFavorite = viewModel::toggleTitleFavorite,
                    onOpenChannel = onOpenChannel,
                    onOpenTitle = onOpenTitle,
                    onOpenEpisode = onOpenEpisode,
                    onOpenFavorites = onOpenFavorites,
                    onLongPress = { sheet = it },
                )
            }
        }
    }

    when (val action = sheet) {
        null -> Unit
        is HomeTileAction.OnChannel -> AreTileActionSheet(
            title = action.channel.name,
            onDismiss = { sheet = null },
            isFavorite = action.channel.id in favoriteChannelIds,
            onToggleFavorite = { viewModel.toggleChannelFavorite(action.channel) },
            onPlay = { onOpenChannel(action.channel) },
        )
        is HomeTileAction.OnTitle -> AreTileActionSheet(
            title = action.title.name,
            onDismiss = { sheet = null },
            isFavorite = action.title.id in favoriteVodIds,
            onToggleFavorite = { viewModel.toggleTitleFavorite(action.title) },
            // Only a Continue-watching series row has a specific episode to resume; everything else
            // opens its detail screen, which is where Play lives.
            onPlay = action.resumeEpisodeId?.let { id -> { onOpenEpisode(id) } },
            onOpenDetails = { onOpenTitle(action.title) },
        )
    }
}

@Composable
private fun HomeRails(
    state: HomeUiState,
    contentPadding: PaddingValues,
    favoriteChannelIds: Set<Long>,
    favoriteVodIds: Set<Long>,
    onToggleChannelFavorite: (Channel) -> Unit,
    onToggleTitleFavorite: (VodTitle) -> Unit,
    onOpenChannel: (Channel) -> Unit,
    onOpenTitle: (VodTitle) -> Unit,
    onOpenEpisode: (Long) -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenRecordings: () -> Unit,
    onOpenStreams: () -> Unit,
    onLongPress: (HomeTileAction) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item(key = "quick-actions", contentType = "quick-actions") {
            HomeQuickActions(
                onOpenFavorites = onOpenFavorites,
                onOpenRecordings = onOpenRecordings,
                onOpenStreams = onOpenStreams,
            )
        }
        // Every rail is conditional, so without a key the LazyColumn re-associates saved item state
        // (each rail's LazyRow scroll offset) BY INDEX: the moment a rail appears or disappears,
        // every rail below it inherits its neighbour's scroll position. The key fixes that; the
        // contentType is the separate, additive fix that lets Compose's slot-reuse pool tell a
        // poster rail from a channel rail (see the perf notes on :tv's Home).
        if (state.continueWatching.isNotEmpty()) {
            item(key = "rail-continue-watching", contentType = "rail-poster") {
                AreRail(stringResource(CoreR.string.home_section_continue_watching)) {
                    items(state.continueWatching, key = { it.id }) { title ->
                        val episodeId = state.continueWatchingEpisodeIds[title.id]
                        ArePosterTile(
                            title = title.name,
                            // A series entry resumes straight into its saved episode (the user
                            // already picked one) instead of reopening the episode picker.
                            onClick = {
                                if (title.isSeries && episodeId != null) onOpenEpisode(episodeId) else onOpenTitle(title)
                            },
                            posterUrl = title.posterUrl,
                            meta = title.metaLine(),
                            rating = title.rating,
                            progress = state.continueWatchingProgress[title.id],
                            width = HomePosterWidth,
                            isFavorite = title.id in favoriteVodIds,
                            onToggleFavorite = { onToggleTitleFavorite(title) },
                            onLongClick = { onLongPress(HomeTileAction.OnTitle(title, episodeId)) },
                            lockCategory = title.categoryName,
                        )
                    }
                }
            }
        }
        if (state.liveNow.isNotEmpty()) {
            item(key = "rail-live-now", contentType = "rail-channel") {
                ChannelRail(
                    title = stringResource(CoreR.string.home_section_live_now),
                    channels = state.liveNow,
                    favoriteChannelIds = favoriteChannelIds,
                    onToggleChannelFavorite = onToggleChannelFavorite,
                    onOpenChannel = onOpenChannel,
                    onLongPress = onLongPress,
                )
            }
        }
        if (state.recommended.isNotEmpty()) {
            item(key = "rail-recommended", contentType = "rail-poster") {
                TitleRail(
                    title = stringResource(CoreR.string.home_section_for_you),
                    titles = state.recommended,
                    favoriteVodIds = favoriteVodIds,
                    onToggleTitleFavorite = onToggleTitleFavorite,
                    onOpenTitle = onOpenTitle,
                    onLongPress = onLongPress,
                )
            }
        }
        if (state.favoriteChannels.isNotEmpty()) {
            item(key = "rail-favorite-channels", contentType = "rail-channel") {
                ChannelRail(
                    title = stringResource(CoreR.string.home_favorite_channels),
                    channels = state.favoriteChannels,
                    favoriteChannelIds = favoriteChannelIds,
                    onToggleChannelFavorite = onToggleChannelFavorite,
                    onOpenChannel = onOpenChannel,
                    onLongPress = onLongPress,
                    onSeeAll = onOpenFavorites,
                )
            }
        }
        if (state.favoriteTitles.isNotEmpty()) {
            item(key = "rail-favorite-titles", contentType = "rail-poster") {
                TitleRail(
                    title = stringResource(CoreR.string.home_favorite_titles),
                    titles = state.favoriteTitles,
                    favoriteVodIds = favoriteVodIds,
                    onToggleTitleFavorite = onToggleTitleFavorite,
                    onOpenTitle = onOpenTitle,
                    onLongPress = onLongPress,
                    onSeeAll = onOpenFavorites,
                )
            }
        }
    }
}

@Composable
private fun TitleRail(
    title: String,
    titles: List<VodTitle>,
    favoriteVodIds: Set<Long>,
    onToggleTitleFavorite: (VodTitle) -> Unit,
    onOpenTitle: (VodTitle) -> Unit,
    onLongPress: (HomeTileAction) -> Unit,
    onSeeAll: (() -> Unit)? = null,
) {
    AreRail(title = title, onSeeAll = onSeeAll) {
        items(titles, key = { it.id }) { item ->
            ArePosterTile(
                title = item.name,
                onClick = { onOpenTitle(item) },
                posterUrl = item.posterUrl,
                meta = item.metaLine(),
                rating = item.rating,
                width = HomePosterWidth,
                isFavorite = item.id in favoriteVodIds,
                onToggleFavorite = { onToggleTitleFavorite(item) },
                onLongClick = { onLongPress(HomeTileAction.OnTitle(item, null)) },
                lockCategory = item.categoryName,
            )
        }
    }
}

/**
 * The three library destinations that are not bottom-nav tabs. They sit at the top of Home as
 * glass cards rather than as rows inside Settings: they are places you go while browsing, not
 * preferences you set once, and reaching Recordings meant three taps through a settings list.
 *
 * Real [glassSurface], not a nested control skin, because Home's rails render straight onto the
 * page background -- this IS the page's first glass layer, so it is not the "glass never stacks"
 * case that makes the bottom bar's tabs use [controlSkin] instead.
 */
@Composable
private fun HomeQuickActions(
    onOpenFavorites: () -> Unit,
    onOpenRecordings: () -> Unit,
    onOpenStreams: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // 16dp matches the rails' own contentPadding, so the cards line up with the first
            // tile of every rail below instead of sitting on their own margin.
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        HomeQuickAction(
            icon = Icons.Filled.Favorite,
            label = stringResource(CoreR.string.nav_favorites),
            onClick = onOpenFavorites,
            modifier = Modifier.weight(1f),
        )
        HomeQuickAction(
            icon = Icons.Filled.Videocam,
            label = stringResource(CoreR.string.nav_recordings),
            onClick = onOpenRecordings,
            modifier = Modifier.weight(1f),
        )
        HomeQuickAction(
            icon = Icons.Filled.Wifi,
            label = stringResource(CoreR.string.nav_streams),
            onClick = onOpenStreams,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun HomeQuickAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(AreIptvTheme.radius.lg)
    Column(
        modifier = modifier
            .glassSurface(shape, elevated = true)
            // Clip before areTouch so the ripple stays inside the rounded card instead of
            // squaring off its corners on press.
            .clip(shape)
            .areTouch(onClick = onClick, shape = shape, contentDescription = label)
            .padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ProvideOnGlass {
            Icon(
                imageVector = icon,
                // The label directly beneath says the same thing; announcing it twice makes
                // TalkBack read every card as "Favorites, Favorites".
                contentDescription = null,
                tint = AreIptvTheme.colors.accent,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = label,
                style = AreIptvTheme.typography.label,
                color = AreIptvTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ChannelRail(
    title: String,
    channels: List<Channel>,
    favoriteChannelIds: Set<Long>,
    onToggleChannelFavorite: (Channel) -> Unit,
    onOpenChannel: (Channel) -> Unit,
    onLongPress: (HomeTileAction) -> Unit,
    onSeeAll: (() -> Unit)? = null,
) {
    AreRail(title = title, onSeeAll = onSeeAll) {
        items(channels, key = { it.id }) { channel ->
            AreChannelTile(
                channel = channel.name,
                onClick = { onOpenChannel(channel) },
                logoUrl = channel.logoUrl,
                number = channel.number,
                category = channel.categoryName,
                isRadio = channel.isRadio,
                width = HomeChannelWidth,
                isFavorite = channel.id in favoriteChannelIds,
                onToggleFavorite = { onToggleChannelFavorite(channel) },
                onLongClick = { onLongPress(HomeTileAction.OnChannel(channel)) },
                lockCategory = channel.categoryName,
            )
        }
    }
}

private fun VodTitle.metaLine(): String? =
    listOfNotNull(year, categoryName).joinToString(" · ").ifBlank { null }

/** Cold start shows the shape of what is coming, not a centred spinner: a spinner on a rail screen
 * says "something is happening somewhere", a skeleton rail says "three shelves are loading". */
@Composable
private fun HomeSkeletons(contentPadding: PaddingValues) {
    val shape = RoundedCornerShape(AreIptvTheme.radius.lg)
    Column(
        modifier = Modifier.fillMaxSize().padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        repeat(3) { index ->
            val poster = index != 1
            Column(Modifier.fillMaxWidth()) {
                AreSkeleton(
                    modifier = Modifier.padding(start = 16.dp, bottom = 10.dp).size(width = 140.dp, height = 20.dp),
                    shape = RoundedCornerShape(AreIptvTheme.radius.sm),
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    userScrollEnabled = false,
                ) {
                    items(4) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            AreSkeleton(
                                modifier = if (poster) {
                                    Modifier.width(HomePosterWidth).height(HomePosterWidth * 3 / 2)
                                } else {
                                    Modifier.width(HomeChannelWidth).height(HomeChannelWidth * 9 / 16)
                                },
                                shape = shape,
                            )
                            AreSkeleton(
                                modifier = Modifier
                                    .width(if (poster) HomePosterWidth else HomeChannelWidth)
                                    .height(12.dp),
                                shape = RoundedCornerShape(AreIptvTheme.radius.sm),
                            )
                        }
                    }
                }
            }
        }
    }
}
