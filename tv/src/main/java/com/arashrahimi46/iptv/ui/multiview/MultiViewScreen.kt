package com.arashrahimi46.iptv.ui.multiview

import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.arashrahimi46.iptv.R
import com.arashrahimi46.iptv.data.db.AppDatabase
import com.arashrahimi46.iptv.data.model.Channel
import com.arashrahimi46.iptv.ui.components.AreBadge
import com.arashrahimi46.iptv.ui.components.AreBadgeTone
import com.arashrahimi46.iptv.ui.components.AreButton
import com.arashrahimi46.iptv.ui.components.AreButtonVariant
import com.arashrahimi46.iptv.ui.components.AreChip
import com.arashrahimi46.iptv.ui.components.AreDialog
import com.arashrahimi46.iptv.ui.components.AreIconButton
import com.arashrahimi46.iptv.ui.components.AreIconButtonVariant
import com.arashrahimi46.iptv.ui.components.AreStreamHealth
import com.arashrahimi46.iptv.ui.components.AreStreamHealthLevel
import com.arashrahimi46.iptv.ui.components.AreTextField
import com.arashrahimi46.iptv.ui.player.StreamRetryPolicy
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.theme.Ink950
import com.arashrahimi46.iptv.ui.theme.TvFocusable
import com.arashrahimi46.iptv.ui.theme.glassBorderBrush
import kotlinx.coroutines.delay

/**
 * MultiView -- 2-up/4-up simultaneous live streams, one active (audio) pane.
 * The panes are a curated, persistent list of live channels the user explicitly
 * added (from the live player's "add to multi-view", or an empty slot's "+"
 * picker here); never an auto-filled catalog slice. Own NavHost destination
 * outside [com.arashrahimi46.iptv.ui.shell.AreIptvAppShell].
 *
 * Each filled pane is a real Media3/ExoPlayer instance: all panes play
 * simultaneously, only the active pane's audio is unmuted. OK on a pane makes it
 * active (audio); long-press removes it from multi-view.
 */
@Composable
fun MultiViewScreen(onBack: () -> Unit, onOpenChannel: (Long) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val viewModel: MultiViewViewModel = viewModel(factory = MultiViewViewModel.factory(context.applicationContext as android.app.Application))
    val state by viewModel.uiState.collectAsState()
    var pickerOpen by remember { mutableStateOf(false) }
    // Long-press used to remove the pane outright; a mis-held OK silently deleted a channel and
    // there was no way to promote a pane to full screen. Non-null = that pane's menu is open.
    var paneMenu by remember { mutableStateOf<Channel?>(null) }

    BackHandler(onBack = onBack)

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                AreIconButton(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back), onClick = onBack, variant = AreIconButtonVariant.Glass)
                Text(text = stringResource(R.string.multiview_title), style = AreIptvTheme.typography.h2, color = Color.White)
                Box(Modifier.weight(1f))
                AreChip(
                    text = stringResource(R.string.multiview_4up),
                    icon = Icons.Filled.GridView,
                    selected = state.paneCount == 4,
                    onClick = { viewModel.setPaneCount(4) },
                )
                AreChip(
                    text = stringResource(R.string.multiview_2up),
                    icon = Icons.Filled.ViewColumn,
                    selected = state.paneCount == 2,
                    onClick = { viewModel.setPaneCount(2) },
                )
            }

            if (!state.hasSource) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.multiview_no_source),
                        style = AreIptvTheme.typography.body,
                        color = AreIptvTheme.colors.textSecondary,
                    )
                }
            } else {
                // Always render exactly [paneCount] slots -- a slot with no curated channel yet
                // shows a "+" placeholder that opens the picker, so the grid is never blank and
                // there's always a way in. Slot index = audio/active index target.
                val slots: List<Channel?> = List(state.paneCount) { state.panes.getOrNull(it) }
                Column(
                    modifier = Modifier.fillMaxSize().padding(start = 28.dp, end = 28.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    slots.chunked(2).forEachIndexed { rowIdx, row ->
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            row.forEachIndexed { colIdx, channel ->
                                val index = rowIdx * 2 + colIdx
                                // key by channel id so a slot whose channel changes (e.g. after a
                                // delete shifts the list) gets a FRESH pane -- its own ExoPlayer +
                                // PlayerView -- instead of Compose reusing the previous channel's
                                // pane in place (which left the old video under the new label).
                                key(channel?.id ?: "empty-$index") {
                                    if (channel != null) {
                                        MultiViewPane(
                                            channel = channel,
                                            active = index == state.activeIndex,
                                            concurrentPanes = state.paneCount,
                                            onClick = { viewModel.setActive(index) },
                                            onLongClick = { paneMenu = channel },
                                            modifier = Modifier.weight(1f).fillMaxSize(),
                                        )
                                    } else {
                                        EmptyPaneSlot(
                                            onClick = { pickerOpen = true },
                                            modifier = Modifier.weight(1f).fillMaxSize(),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (pickerOpen) {
            ChannelPickerDialog(
                categories = state.pickerCategories,
                loadChannels = viewModel::channelsFor,
                onPick = { viewModel.addChannel(it); pickerOpen = false },
                onDismiss = { pickerOpen = false },
            )
        }

        paneMenu?.let { channel ->
            PaneMenuDialog(
                channel = channel,
                onWatch = { paneMenu = null; onOpenChannel(channel.id) },
                onRemove = { paneMenu = null; viewModel.removeChannel(channel.id) },
                onDismiss = { paneMenu = null },
            )
        }
    }
}

/** Long-press menu for a filled pane: promote it to full screen, or drop it from multi-view. */
@Composable
private fun PaneMenuDialog(
    channel: Channel,
    onWatch: () -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    // Watch is the safe default and takes focus, so a second stray OK can't delete the pane.
    val watchFocus = remember { FocusRequester() }
    LaunchedEffect(channel.id) { watchFocus.requestFocus() }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        AreDialog(
            onDismiss = onDismiss,
            title = channel.name,
            width = 460.dp,
            actions = {
                AreButton(
                    text = stringResource(R.string.multiview_pane_watch),
                    onClick = onWatch,
                    variant = AreButtonVariant.Primary,
                    modifier = Modifier.focusRequester(watchFocus),
                )
                AreButton(
                    text = stringResource(R.string.multiview_pane_remove),
                    onClick = onRemove,
                    variant = AreButtonVariant.Danger,
                )
            },
        ) {
            Text(
                text = stringResource(R.string.multiview_pane_menu_body),
                style = AreIptvTheme.typography.body,
                color = AreIptvTheme.colors.textSecondary,
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun EmptyPaneSlot(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = AreIptvTheme.colors
    val shape = RoundedCornerShape(AreIptvTheme.radius.md)
    TvFocusable(
        onClick = onClick,
        modifier = modifier,
        shape = shape,
        glowColor = colors.accent,
        backgroundColor = colors.surfaceGlass,
        borderBrush = glassBorderBrush(),
        disableScale = true,
    ) { _, _ ->
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(40.dp))
            Box(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.multiview_add_channel),
                style = AreIptvTheme.typography.body,
                color = colors.textSecondary,
            )
        }
    }
}

@Composable
private fun ChannelPickerDialog(
    categories: List<String>,
    loadChannels: suspend (PickerFilter) -> List<Channel>,
    onPick: (Channel) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = AreIptvTheme.colors
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf<PickerFilter>(PickerFilter.All) }
    var channels by remember { mutableStateOf<List<Channel>>(emptyList()) }
    // Category/Favorites hit the DB; "All" returns the pre-loaded candidates -- reloaded whenever
    // the selected chip changes. Search then refines this set client-side (search within category).
    LaunchedEffect(filter) { channels = loadChannels(filter) }
    val filtered = remember(query, channels) {
        if (query.isBlank()) channels else channels.filter { it.name.contains(query.trim(), ignoreCase = true) }
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        AreDialog(
            onDismiss = onDismiss,
            title = stringResource(R.string.multiview_add_to),
            width = 560.dp,
        ) {
            // Filter chips: All · Favorites · categories (pinned categories already floated to the
            // front by the view model). Default "All" is selected.
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    AreChip(
                        text = stringResource(R.string.search_scope_all),
                        selected = filter is PickerFilter.All,
                        onClick = { filter = PickerFilter.All },
                    )
                }
                item {
                    AreChip(
                        text = stringResource(R.string.favorites_title),
                        icon = Icons.Filled.Favorite,
                        selected = filter is PickerFilter.Favorites,
                        onClick = { filter = PickerFilter.Favorites },
                    )
                }
                items(categories, key = { it }) { cat ->
                    AreChip(
                        text = cat,
                        selected = (filter as? PickerFilter.Category)?.name == cat,
                        onClick = { filter = PickerFilter.Category(cat) },
                    )
                }
            }
            Box(Modifier.height(12.dp))
            // activateOnClick: on TV the channel list is the primary content -- an auto-popping IME
            // would bury it. The field is a focusable row; OK opens the keyboard, D-pad-down drops
            // straight into the list.
            AreTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = stringResource(R.string.action_search),
                activateOnClick = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Box(Modifier.height(12.dp))
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(filtered, key = { it.id }) { channel ->
                    TvFocusable(
                        onClick = { onPick(channel) },
                        modifier = Modifier.fillMaxWidth(),
                        glowColor = colors.accent,
                        // Nested inside the glass picker dialog -- child tint + glass hairline,
                        // never a second fill (glass must not stack).
                        backgroundColor = colors.glassChildTint,
                        borderColor = colors.borderGlass,
                    ) { _, _ ->
                        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                            Text(channel.name, style = AreIptvTheme.typography.body, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (channel.categoryName != null) {
                                Text(channel.categoryName, style = AreIptvTheme.typography.caption, color = colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MultiViewPane(
    channel: Channel,
    active: Boolean,
    concurrentPanes: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val colors = AreIptvTheme.colors
    // P0.1: the pane was previously a dead end on failure -- a static red dot, no recovery.
    // currentSource is local, separate from the `channel` prop, so a fallback (below) can
    // swap the playing source without needing the parent's channel list to change.
    var currentSource by remember(channel.id) { mutableStateOf(channel) }
    var health by remember(channel.id) { mutableStateOf(AreStreamHealthLevel.Moderate) }
    // Bumped by the auto-retry effect below to force exoPlayer's remember() key to change
    // (a fresh instance/reconnect attempt), same pattern as LivePlayerScreen's retryCount.
    var retryCount by remember(channel.id) { mutableStateOf(0) }
    var autoRetryAttempt by remember(currentSource.id) { mutableStateOf(0) }

    val exoPlayer = remember(currentSource.streamUrl, retryCount) {
        // Multi-view runs up to 4 decoders at once, but a TV box has only a handful of hardware
        // decoders. A pane that can't acquire one previously rendered BLACK (the reported bug --
        // player-added streams that play fine fullscreen were black here). `setEnableDecoderFallback`
        // is what fixes that: an overflow pane drops to a software decoder instead of going black.
        //
        // PERF: EXTENSION_RENDERER_MODE_ON is deliberately NOT set. Decoder fallback already delivers
        // the overflow recovery above; the extension mode additionally makes the ffmpeg/libvpx
        // software decoders *eligible from the start*, so on a box with two hardware decoders a 4-up
        // grid could land on 2 hardware + 2 software 1080p decodes -- on exactly the weak CPUs this
        // screen is hardest on. The fullscreen player wires this to the user's hardware-decoding
        // preference (LivePlayerScreen) rather than forcing it; multiview now simply leaves it at the
        // platform default and lets fallback handle the exception case.
        val renderersFactory = DefaultRenderersFactory(context).apply {
            setEnableDecoderFallback(true)
        }
        // PERF: DefaultLoadControl's defaults allow ~50s of buffer per player. That is a reasonable
        // bet for ONE player on a slow line, but four of them each claiming that much buffer (and
        // each churning its own DefaultAllocator) is a GC-then-OOM shape on a low-RAM TV. Multiview
        // trades buffer depth for headroom: enough to ride out a hiccup, not enough to hoard.
        val loadControl = DefaultLoadControl.Builder()
            // minBufferMs MUST be >= bufferForPlaybackAfterRebufferMs -- DefaultLoadControl asserts
            // it and throws IllegalArgumentException from the builder, which crashed Multi-View on
            // open every time (1_500 < 2_000). 5s still gives up ~90% of the ~50s default per player.
            .setBufferDurationsMs(5_000, 15_000, 1_000, 2_000)
            .setTargetBufferBytes(8 * 1024 * 1024)
            .setPrioritizeTimeOverSizeThresholds(false)
            .build()
        ExoPlayer.Builder(context, renderersFactory)
            .setLoadControl(loadControl)
            .build().apply {
                setMediaItem(MediaItem.fromUri(currentSource.streamUrl))
                playWhenReady = true
                prepare()
            }
    }

    // Backgrounding must stop the decoders. Without this, pressing HOME left all four panes decoding
    // AND pulling four streams off the network indefinitely -- the composable never leaves the tree,
    // so the DisposableEffect below never runs. The fullscreen player has always torn down on
    // ON_STOP; multiview simply never got the same treatment. Pause rather than release so returning
    // to the app resumes in place instead of rebuilding four players at once.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> exoPlayer.pause()
                Lifecycle.Event.ON_START -> exoPlayer.play()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                health = when (playbackState) {
                    Player.STATE_READY -> {
                        autoRetryAttempt = 0
                        AreStreamHealthLevel.Stable
                    }
                    Player.STATE_BUFFERING -> AreStreamHealthLevel.Moderate
                    else -> health
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                health = AreStreamHealthLevel.Poor
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // P0.1: same degraded-health -> auto-retry-with-backoff -> fallback-to-next-source
    // policy as LivePlayerScreen (see StreamRetryPolicy), applied per pane instead of
    // leaving a failed pane permanently dead.
    LaunchedEffect(health, autoRetryAttempt, currentSource.id) {
        if (health == AreStreamHealthLevel.Stable) return@LaunchedEffect
        if (autoRetryAttempt >= StreamRetryPolicy.MAX_RETRIES) {
            val alternate = AppDatabase.get(context).channelDao().findAlternateByName(currentSource.name, currentSource.id)
            if (alternate != null) {
                currentSource = alternate
                autoRetryAttempt = 0
            } else if (health != AreStreamHealthLevel.Poor) {
                // QA fix: a pure-buffering degradation (no onPlayerError ever fired) with no
                // alternate to fall back to previously left the pane stuck on the Moderate
                // (amber) dot forever, with none of this effect's keys able to change again to
                // re-evaluate. There's no per-pane manual-retry UI (out of scope per the P0.1
                // brief), so the recovery signal available within scope is flipping the
                // existing indicator to Poor -- at least surfaces "this pane is dead" instead
                // of a spinner that never resolves.
                health = AreStreamHealthLevel.Poor
            }
            return@LaunchedEffect
        }
        // A pure-buffering pane gets a grace period scaled by how many players are starting at once
        // -- a flat 8s is shorter than four concurrent cold starts on a weak box, so the retry logic
        // used to tear down and rebuild all four players exactly when the device was most loaded.
        delay(
            if (health == AreStreamHealthLevel.Poor) {
                StreamRetryPolicy.backoffMillis(autoRetryAttempt)
            } else {
                StreamRetryPolicy.bufferingGraceMillis(concurrentPanes)
            },
        )
        autoRetryAttempt++
        retryCount++
    }

    // Only the active pane's audio plays -- every pane keeps decoding video so switching
    // the active pane is instant, matching the design's "tap to make active" behavior.
    LaunchedEffect(active, exoPlayer) {
        exoPlayer.volume = if (active) 1f else 0f
    }

    val shape = RoundedCornerShape(AreIptvTheme.radius.md)
    // Fill the grid cell (already sized by the row/column weights). disableScale: these big panes
    // sit in a tight 2x2 grid, so a 6% focus-scale grew them into their neighbours (the reported
    // clipping) -- the ring + glow alone are the focus indicator here. Long-press opens the pane menu.
    TvFocusable(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
        shape = shape,
        glowColor = colors.accent,
        disableScale = true,
    ) { _, _ ->
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black, shape),
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = {
                    PlayerView(context).apply {
                        useController = false
                        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                        player = exoPlayer
                    }
                },
                // Reattach whenever a new ExoPlayer instance is built (retry/fallback swaps the
                // player while the PlayerView stays). Without this the view keeps rendering the
                // old, now-released player -- a stale/frozen frame under the current label.
                update = { it.player = exoPlayer },
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Ink950.copy(alpha = 0f), Ink950.copy(alpha = 0.85f)),
                            startY = 0.45f,
                        ),
                    ),
            )
            Row(
                modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AreBadge(stringResource(R.string.player_live_badge), tone = AreBadgeTone.Live, glow = true)
                if (active) AreBadge(stringResource(R.string.multiview_audio_badge), tone = AreBadgeTone.New)
            }
            Box(Modifier.align(Alignment.TopEnd).padding(12.dp)) {
                AreStreamHealth(level = health, showLabel = false)
            }
            Column(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                Text(
                    text = channel.name,
                    style = AreIptvTheme.typography.h3,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (channel.categoryName != null) {
                    Text(
                        text = channel.categoryName,
                        style = AreIptvTheme.typography.caption,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
