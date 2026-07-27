package com.arashrahimi46.iptv.ui.guide

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Text
import com.arashrahimi46.iptv.R
import com.arashrahimi46.iptv.ui.components.AreChip
import com.arashrahimi46.iptv.ui.components.AreSegmentedControl
import com.arashrahimi46.iptv.ui.components.AreDialog
import com.arashrahimi46.iptv.ui.components.AreGuideCell
import com.arashrahimi46.iptv.ui.components.rememberClockFormatter
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.theme.glassSurface
import com.arashrahimi46.iptv.ui.theme.rememberPlaybackFocusRequester
import com.arashrahimi46.iptv.ui.theme.requestFocusWhenReady
import java.time.Instant
import java.time.ZoneId

/** px-per-minute scale for proportional GuideCell widths (mirrors Guide.jsx `PX`). */
private val DpPerMinute = 3.dp

/**
 * TV Guide (Guide.jsx): 6-hour rolling window, day chips, channel-group
 * filter, timeline header, and the sticky "focused-program info bar" (no
 * tooltips on TV -- driven by [AreGuideCell]'s `onFocusChange`). Selecting any
 * cell in a channel's row (any programme, not just the live one) immediately
 * plays that channel via the real [com.arashrahimi46.iptv.ui.player.LivePlayerScreen].
 */
@Composable
fun GuideScreen(
    onChannelSelected: (channelId: Long) -> Unit,
    modifier: Modifier = Modifier,
    onCatchup: (channelId: Long, startMs: Long, endMs: Long) -> Unit = { _, _, _ -> },
) {
    val context = LocalContext.current
    val viewModel: GuideViewModel = viewModel(
        factory = GuideViewModel.factory(context.applicationContext as android.app.Application),
    )
    val state by viewModel.uiState.collectAsState()
    // PERF: `focused` is deliberately NOT read here. Every D-pad move writes it, and reading it at
    // this level made the ENTIRE Guide recompose on every keypress -- the grid, the timeline header
    // and every visible cell -- to update one info bar. FocusedInfoBar collects it itself, so the
    // recomposition scope is that one Row. See its doc.
    val colors = AreIptvTheme.colors
    val spacing = AreIptvTheme.spacing
    val timeFormatter = rememberClockFormatter()
    // Issue #5: which cell started playback -- channel id alone doesn't disambiguate
    // which of a row's several programme cells was clicked (any cell can start playback,
    // not just the live one -- see issue #4 above), so the clicked slot's start time is
    // tracked alongside it. See HomeScreen's rememberPlaybackFocusRequester for the rest
    // of the explanation (survives the screen pausing while the player is on top).
    var lastPlayedChannelId by rememberSaveable { mutableStateOf<Long?>(null) }
    var lastPlayedSlotStartMs by rememberSaveable { mutableStateOf<Long?>(null) }
    // Catch-up action menu: the (channel, slot) whose past cell was pressed, or null when closed.
    var catchupMenu by remember { mutableStateOf<Pair<com.arashrahimi46.iptv.data.model.Channel, GuideProgramSlot>?>(null) }

    if (!state.hasSource) {
        Text(
            text = stringResource(R.string.guide_no_source),
            style = AreIptvTheme.typography.body,
            color = colors.textSecondary,
            modifier = modifier.padding(horizontal = spacing.safeX, vertical = spacing.sp10),
        )
        return
    }

    // remember-ed so it's a stable `remember` key for the per-cell time labels below.
    val zone = remember { ZoneId.systemDefault() }

    // Initial D-pad focus, matching every other tab: without it the shell leaves focus on the
    // sidebar and the Guide reads as dead. Lands on the SELECTED category chip rather than a
    // programme cell -- from there Down enters the grid and Left/Right switches category, whereas
    // starting inside the grid would mean travelling back up past the info bar to change category.
    // Stands down when returning from the player (the played cell is restored instead).
    val groupFocusRequester = remember { FocusRequester() }
    // Keyed on whether the selected chip EXISTS yet, not on which one is selected: categories arrive
    // asynchronously (so LaunchedEffect(Unit) would fire before there is anything to focus), but
    // re-firing on every category change would yank focus back out of the grid mid-browse.
    val selectedChipExists = state.selectedGroup in state.groups
    LaunchedEffect(selectedChipExists) {
        if (selectedChipExists && lastPlayedChannelId == null) groupFocusRequester.requestFocusWhenReady()
    }

    // P0.2: fillMaxSize (not just padding) so this root Column has a real bounded height to
    // hand down -- GuideScreen's caller (MainActivity) no longer wraps this tab in a
    // `verticalScroll` (see FullSizeTab there); this Composable owns its own layout end to
    // end instead. That bounded height is what makes the LazyColumn below (and the .weight(1f)
    // it and its sibling Row get further down) a valid virtualized layout rather than throwing
    // "measured with an unbounded amount of height" -- a lazy layout can't live inside another
    // unbounded-height vertical scroll container (same-axis nesting), which is exactly what
    // wrapping this tab in a scrolling container would still do.
    Column(modifier = modifier.fillMaxSize().padding(top = spacing.sp2, bottom = spacing.sp10)) {
        // Header: day chips (the page title lives in the shell top bar).
        Row(
            modifier = Modifier.padding(horizontal = spacing.safeX).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(Modifier.weight(1f))
            AreSegmentedControl(
                options = GuideDay.entries,
                selected = state.day,
                label = { stringResource(it.labelRes) },
                onSelect = { viewModel.selectDay(it) },
            )
        }
        Box(Modifier.height(spacing.sp5))

        // Category tabs -- horizontally scrollable so every category is reachable on a real
        // playlist with dozens of groups (the Guide is strictly per-category; no "All" tab).
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = spacing.safeX),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            state.groups.forEach { group ->
                val isSelected = group == state.selectedGroup
                AreChip(
                    text = group,
                    onClick = { viewModel.selectGroup(group) },
                    selected = isSelected,
                    modifier = if (isSelected) Modifier.focusRequester(groupFocusRequester) else Modifier,
                )
            }
        }
        Box(Modifier.height(spacing.sp5))

        // P0.4: EPG source unreachable -- a small banner, not a full-screen replacement. Channel
        // rows below still render (each with its own "No programme data" placeholder slot).
        if (state.epgUnavailable) {
            Box(Modifier.padding(horizontal = spacing.safeX)) {
                Text(
                    text = stringResource(R.string.guide_epg_unavailable),
                    style = AreIptvTheme.typography.caption,
                    color = colors.danger,
                )
            }
            Box(Modifier.height(spacing.sp4))
        }

        // Sticky focused-program info bar (no hover/tooltips on TV -- last focused cell stays shown).
        Box(Modifier.padding(horizontal = spacing.safeX)) {
            FocusedInfoBar(viewModel)
        }
        Box(Modifier.height(spacing.sp5))

        // Shared horizontal ScrollState -- the SAME instance applied to the timeline header
        // and every row below keeps them scrolling in lockstep (Compose's standard
        // synced-header pattern), now that rows are virtualized (below) and can no longer
        // all live under one shared-scroll Column together.
        val scrollState = rememberScrollState()
        // weight(1f) -- this is the last child of the fillMaxSize root Column above, so it
        // claims exactly the height left over after the header rows/chips/info-bar -- the real
        // bounded height the LazyColumn below needs to be a valid lazy layout.
        Column(modifier = Modifier.weight(1f).padding(horizontal = spacing.safeX)) {
            // Channel column is PINNED (drawn outside the shared horizontalScroll) on both the
            // timeline header and every row, so the channel you're browsing stays visible no
            // matter how far right you scroll through its programmes. Only the programme lane
            // (weight(1f) + horizontalScroll) moves, keeping header + rows in lockstep.
            TimelineHeader(windowStartMs = state.windowStartMs, windowEndMs = state.windowEndMs, zone = zone, scrollState = scrollState)
            Box(Modifier.height(8.dp))
            // P0.2: was a plain Column.forEach that eagerly composed every channel row for
            // the whole ~6h window regardless of what's on screen -- with large catalogs
            // that's hundreds of rows composed up front. LazyColumn only composes the rows
            // actually visible (plus a small buffer). weight(1f) fills the remaining height
            // left in this Column after the timeline header row above.
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.rows, key = { it.channel.id }) { row ->
                    // A focused cell scales up (1.06) + draws an outward glow that overflows the
                    // row height. LazyColumn draws items in order, so the NEXT row would paint over
                    // this row's downward glow -- clipping it flat. Raise the focused row's zIndex
                    // so it draws above its siblings and the glow stays whole.
                    var rowFocused by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier
                            .zIndex(if (rowFocused) 1f else 0f)
                            .onFocusChanged { rowFocused = it.hasFocus },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ChannelHeaderCell(name = row.channel.name, number = row.channel.number)
                        Row(
                            modifier = Modifier.weight(1f).horizontalScroll(scrollState),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            row.slots.forEach { slot ->
                                // PERF: both are pure functions of the slot's fixed timestamps, but ran
                                // on EVERY recomposition of this row -- and a ZonedDateTime + format()
                                // per cell, times every cell of every visible row, is real allocation
                                // and CPU on the D-pad path. Remembered against the slot instead.
                                val cellWidth = remember(slot.startMs, slot.endMs) {
                                    val durationMinutes = ((slot.endMs - slot.startMs) / 60000L).coerceAtLeast(1L)
                                    // Clamp so window-clipped boundary programmes (1-2 min) don't
                                    // compute a <=0.dp, invisible/unfocusable cell.
                                    ((DpPerMinute * durationMinutes.toInt()) - 6.dp).coerceAtLeast(24.dp)
                                }
                                val timeLabel = remember(slot.startMs, zone, timeFormatter) {
                                    Instant.ofEpochMilli(slot.startMs).atZone(zone).format(timeFormatter)
                                }
                                val focusRequester = rememberPlaybackFocusRequester(
                                    savedId = lastPlayedChannelId?.takeIf { lastPlayedSlotStartMs == slot.startMs },
                                    itemId = row.channel.id,
                                ) {
                                    lastPlayedChannelId = null
                                    lastPlayedSlotStartMs = null
                                }
                                AreGuideCell(
                                    title = slot.title,
                                    time = timeLabel,
                                    onClick = {
                                        // A catch-up-eligible past cell opens the action menu (watch from
                                        // start / go live); any other cell plays the live channel as before.
                                        if (slot.catchupEligible) {
                                            catchupMenu = row.channel to slot
                                        } else {
                                            lastPlayedChannelId = row.channel.id
                                            lastPlayedSlotStartMs = slot.startMs
                                            onChannelSelected(row.channel.id)
                                        }
                                    },
                                    live = slot.isNow,
                                    now = slot.isNow,
                                    catchup = slot.catchupEligible,
                                    width = cellWidth,
                                    onFocusChange = { isFocused -> if (isFocused) viewModel.setFocused(GuideFocusedInfo(row.channel, slot)) },
                                    modifier = Modifier.focusRequester(focusRequester),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    catchupMenu?.let { (channel, slot) ->
        CatchupActionDialog(
            channelName = channel.name,
            programTitle = slot.title,
            programStartMs = slot.programStartMs,
            programEndMs = slot.programEndMs,
            onWatchFromStart = {
                catchupMenu = null
                lastPlayedChannelId = channel.id
                lastPlayedSlotStartMs = slot.startMs
                onCatchup(channel.id, slot.programStartMs, slot.programEndMs)
            },
            onGoLive = {
                catchupMenu = null
                lastPlayedChannelId = channel.id
                lastPlayedSlotStartMs = slot.startMs
                onChannelSelected(channel.id)
            },
            onDismiss = { catchupMenu = null },
        )
    }
}

/**
 * Catch-up action menu (docs/catchup-v1-design.md, D4). A focus-trapped [AreDialog] wrapped in a real
 * [Dialog] window (same convention as [com.arashrahimi46.iptv.ui.home.HomeAddSectionDialog]); default
 * focus lands on "Watch from start". "Record" is shown disabled as the seam for the future DVR work.
 */
@Composable
private fun CatchupActionDialog(
    channelName: String,
    programTitle: String,
    programStartMs: Long,
    programEndMs: Long,
    onWatchFromStart: () -> Unit,
    onGoLive: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = AreIptvTheme.colors
    val zone = ZoneId.systemDefault()
    val timeFormatter = rememberClockFormatter()
    val start = Instant.ofEpochMilli(programStartMs).atZone(zone).format(timeFormatter)
    val end = Instant.ofEpochMilli(programEndMs).atZone(zone).format(timeFormatter)
    val watchFocus = remember { androidx.compose.ui.focus.FocusRequester() }
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
    ) {
        AreDialog(
            onDismiss = onDismiss,
            title = programTitle,
            width = 440.dp,
            actions = {
                com.arashrahimi46.iptv.ui.components.AreButton(
                    text = stringResource(R.string.action_close),
                    onClick = onDismiss,
                    variant = com.arashrahimi46.iptv.ui.components.AreButtonVariant.Ghost,
                )
            },
        ) {
            Text(
                text = "⟲ ${stringResource(R.string.guide_catchup_label)} · $channelName",
                style = AreIptvTheme.typography.caption,
                color = colors.success,
            )
            Box(Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.guide_catchup_aired, start, end),
                style = AreIptvTheme.typography.caption,
                color = colors.textTertiary,
            )
            Box(Modifier.height(AreIptvTheme.spacing.sp5))
            com.arashrahimi46.iptv.ui.components.AreButton(
                text = stringResource(R.string.guide_catchup_watch_from_start),
                onClick = onWatchFromStart,
                variant = com.arashrahimi46.iptv.ui.components.AreButtonVariant.Primary,
                full = true,
                modifier = Modifier.fillMaxWidth().focusRequester(watchFocus),
            )
            Box(Modifier.height(8.dp))
            com.arashrahimi46.iptv.ui.components.AreButton(
                text = stringResource(R.string.guide_catchup_go_live),
                onClick = onGoLive,
                variant = com.arashrahimi46.iptv.ui.components.AreButtonVariant.Secondary,
                full = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Box(Modifier.height(8.dp))
            com.arashrahimi46.iptv.ui.components.AreButton(
                text = stringResource(R.string.guide_catchup_record_soon),
                onClick = {},
                variant = com.arashrahimi46.iptv.ui.components.AreButtonVariant.Secondary,
                full = true,
                disabled = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
    LaunchedEffect(Unit) { runCatching { watchFocus.requestFocus() } }
}

@Composable
private fun TimelineHeader(windowStartMs: Long, windowEndMs: Long, zone: ZoneId, scrollState: ScrollState) {
    val timeFormatter = rememberClockFormatter()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Fixed spacer over the pinned channel column -- keeps the first time mark aligned
        // to where the programme lane begins (the rail's own styling names it as channels).
        Box(Modifier.width(AreIptvTheme.spacing.guideChannelWidth))
        // Only the time marks scroll -- shares scrollState with the rows below.
        Row(modifier = Modifier.weight(1f).horizontalScroll(scrollState)) {
            var mark = windowStartMs
            while (mark < windowEndMs) {
                Box(Modifier.width(DpPerMinute * 30)) {
                    Text(
                        text = Instant.ofEpochMilli(mark).atZone(zone).format(timeFormatter),
                        style = AreIptvTheme.typography.mono,
                        color = AreIptvTheme.colors.textTertiary,
                    )
                }
                mark += 30 * 60_000L
            }
        }
    }
}

@Composable
private fun ChannelHeaderCell(name: String, number: String?) {
    val colors = AreIptvTheme.colors
    val shape = RoundedCornerShape(AreIptvTheme.radius.sm)
    Row(
        modifier = Modifier
            .width(AreIptvTheme.spacing.guideChannelWidth)
            .height(AreIptvTheme.spacing.guideRowHeight)
            // surface2 + a defined border deliberately set the pinned rail apart from the
            // programme cells (surface1) beside it -- and give the rail a visible edge on the
            // near-white light-theme background, where a borderless white-on-white cell vanished.
            .glassSurface(shape)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Channel initials tile -- the same identity cue the focused-info bar uses, so the
        // left rail reads unmistakably as "a channel" rather than another programme block.
        Box(
            modifier = Modifier.size(36.dp).background(colors.surface3, RoundedCornerShape(AreIptvTheme.radius.xs)),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = name.take(3).uppercase(), style = AreIptvTheme.typography.caption, color = colors.textSecondary)
        }
        Column(modifier = Modifier.weight(1f)) {
            if (number != null) {
                Text(text = number, style = AreIptvTheme.typography.mono, color = colors.textTertiary)
            }
            Text(text = name, style = AreIptvTheme.typography.label, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/**
 * Takes the [GuideViewModel], not the value: collecting `focused` HERE keeps the recomposition that
 * every D-pad move triggers confined to this one Row. Read at the screen root it invalidated the
 * whole Guide -- rows, cells, timeline -- once per keypress, which is what made fast cell-to-cell
 * travel stutter on a TV SoC.
 */
@Composable
private fun FocusedInfoBar(viewModel: GuideViewModel) {
    // `.value` rather than `by`: the null checks below smart-cast off a plain val, not a delegate.
    val info = viewModel.focused.collectAsState().value
    val colors = AreIptvTheme.colors
    val zone = ZoneId.systemDefault()
    val timeFormatter = rememberClockFormatter()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glassSurface(RoundedCornerShape(AreIptvTheme.radius.md))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier.size(44.dp).background(colors.surface3, RoundedCornerShape(AreIptvTheme.radius.xs)),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = (info?.channel?.name ?: stringResource(R.string.guide_no_channel_placeholder)).take(3).uppercase(), style = AreIptvTheme.typography.caption, color = colors.textPrimary)
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = info?.slot?.title ?: stringResource(R.string.guide_focus_hint),
                    style = AreIptvTheme.typography.h3,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (info?.slot?.isNow == true) {
                    Box(Modifier.size(6.dp).background(colors.live, CircleShape))
                    Text(text = stringResource(R.string.guide_live_badge), style = AreIptvTheme.typography.caption, color = colors.live)
                }
            }
            if (info != null) {
                val start = Instant.ofEpochMilli(info.slot.startMs).atZone(zone).format(timeFormatter)
                val end = Instant.ofEpochMilli(info.slot.endMs).atZone(zone).format(timeFormatter)
                Text(
                    text = "${info.channel.name} · $start – $end",
                    style = AreIptvTheme.typography.caption,
                    color = colors.textSecondary,
                )
            }
        }
    }
}
