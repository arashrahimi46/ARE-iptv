package com.arashrahimi46.iptv.ui.guide

import androidx.compose.foundation.background
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Text
import com.arashrahimi46.iptv.ui.components.AreChip
import com.arashrahimi46.iptv.ui.components.AreGuideCell
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.theme.rememberPlaybackFocusRequester
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** px-per-minute scale for proportional GuideCell widths (mirrors Guide.jsx `PX`). */
private val DpPerMinute = 3.dp
private val TimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/**
 * TV Guide (Guide.jsx): 6-hour rolling window, day chips, channel-group
 * filter, timeline header, and the sticky "focused-program info bar" (no
 * tooltips on TV -- driven by [AreGuideCell]'s `onFocusChange`). Selecting any
 * cell in a channel's row (any programme, not just the live one) immediately
 * plays that channel via the real [com.arashrahimi46.iptv.ui.player.LivePlayerScreen].
 */
@Composable
fun GuideScreen(onChannelSelected: (channelId: Long) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val viewModel: GuideViewModel = viewModel(
        factory = GuideViewModel.factory(context.applicationContext as android.app.Application),
    )
    val state by viewModel.uiState.collectAsState()
    val focused by viewModel.focused.collectAsState()
    val colors = AreIptvTheme.colors
    val spacing = AreIptvTheme.spacing
    // Issue #5: which cell started playback -- channel id alone doesn't disambiguate
    // which of a row's several programme cells was clicked (any cell can start playback,
    // not just the live one -- see issue #4 above), so the clicked slot's start time is
    // tracked alongside it. See HomeScreen's rememberPlaybackFocusRequester for the rest
    // of the explanation (survives the screen pausing while the player is on top).
    var lastPlayedChannelId by rememberSaveable { mutableStateOf<Long?>(null) }
    var lastPlayedSlotStartMs by rememberSaveable { mutableStateOf<Long?>(null) }

    if (!state.hasSource) {
        Text(
            text = "Add a playlist from the sidebar to see the TV guide here.",
            style = AreIptvTheme.typography.body,
            color = colors.textSecondary,
            modifier = modifier.padding(horizontal = spacing.safeX, vertical = spacing.sp10),
        )
        return
    }

    val zone = ZoneId.systemDefault()

    // P0.2: fillMaxSize (not just padding) so this root Column has a real bounded height to
    // hand down -- GuideScreen's caller (MainActivity) no longer wraps this tab in a
    // `verticalScroll` (see FullSizeTab there); this Composable owns its own layout end to
    // end instead. That bounded height is what makes the LazyColumn below (and the .weight(1f)
    // it and its sibling Row get further down) a valid virtualized layout rather than throwing
    // "measured with an unbounded amount of height" -- a lazy layout can't live inside another
    // unbounded-height vertical scroll container (same-axis nesting), which is exactly what
    // wrapping this tab in a scrolling container would still do.
    Column(modifier = modifier.fillMaxSize().padding(top = spacing.sp6, bottom = spacing.sp10)) {
        // Header: title + day chips.
        Row(
            modifier = Modifier.padding(horizontal = spacing.safeX).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(text = "TV Guide", style = AreIptvTheme.typography.display, color = colors.textPrimary)
            Box(Modifier.weight(1f))
            GuideDay.entries.forEach { day ->
                AreChip(text = day.label, onClick = { viewModel.selectDay(day) }, selected = day == state.day)
            }
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
                AreChip(text = group, onClick = { viewModel.selectGroup(group) }, selected = group == state.selectedGroup)
            }
        }
        Box(Modifier.height(spacing.sp5))

        // P0.4: EPG source unreachable -- a small banner, not a full-screen replacement. Channel
        // rows below still render (each with its own "No programme data" placeholder slot).
        if (state.epgUnavailable) {
            Box(Modifier.padding(horizontal = spacing.safeX)) {
                Text(
                    text = "EPG source unavailable -- showing channels without programme data.",
                    style = AreIptvTheme.typography.caption,
                    color = colors.danger,
                )
            }
            Box(Modifier.height(spacing.sp4))
        }

        // Sticky focused-program info bar (no hover/tooltips on TV -- last focused cell stays shown).
        Box(Modifier.padding(horizontal = spacing.safeX)) {
            FocusedInfoBar(focused)
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
            Row(modifier = Modifier.horizontalScroll(scrollState)) {
                TimelineHeader(windowStartMs = state.windowStartMs, windowEndMs = state.windowEndMs, zone = zone)
            }
            Box(Modifier.height(8.dp))
            // P0.2: was a plain Column.forEach that eagerly composed every channel row for
            // the whole ~6h window regardless of what's on screen -- with large catalogs
            // that's hundreds of rows composed up front. LazyColumn only composes the rows
            // actually visible (plus a small buffer). weight(1f) fills the remaining height
            // left in this Column after the timeline header row above.
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.rows, key = { it.channel.id }) { row ->
                    Row(
                        modifier = Modifier.horizontalScroll(scrollState),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ChannelHeaderCell(name = row.channel.name, number = row.channel.number)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.slots.forEach { slot ->
                                val durationMinutes = ((slot.endMs - slot.startMs) / 60000L).coerceAtLeast(1L)
                                val focusRequester = rememberPlaybackFocusRequester(
                                    savedId = lastPlayedChannelId?.takeIf { lastPlayedSlotStartMs == slot.startMs },
                                    itemId = row.channel.id,
                                ) {
                                    lastPlayedChannelId = null
                                    lastPlayedSlotStartMs = null
                                }
                                AreGuideCell(
                                    title = slot.title,
                                    time = Instant.ofEpochMilli(slot.startMs).atZone(zone).format(TimeFormatter),
                                    onClick = {
                                        lastPlayedChannelId = row.channel.id
                                        lastPlayedSlotStartMs = slot.startMs
                                        onChannelSelected(row.channel.id)
                                    },
                                    live = slot.isNow,
                                    now = slot.isNow,
                                    // Clamp so window-clipped boundary programmes (1-2 min) don't
                                    // compute a <=0.dp, invisible/unfocusable cell.
                                    width = ((DpPerMinute * durationMinutes.toInt()) - 6.dp).coerceAtLeast(24.dp),
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
}

@Composable
private fun TimelineHeader(windowStartMs: Long, windowEndMs: Long, zone: ZoneId) {
    Row {
        Box(Modifier.width(AreIptvTheme.spacing.guideChannelWidth))
        var mark = windowStartMs
        while (mark < windowEndMs) {
            Box(Modifier.width(DpPerMinute * 30)) {
                Text(
                    text = Instant.ofEpochMilli(mark).atZone(zone).format(TimeFormatter),
                    style = AreIptvTheme.typography.mono,
                    color = AreIptvTheme.colors.textTertiary,
                )
            }
            mark += 30 * 60_000L
        }
    }
}

@Composable
private fun ChannelHeaderCell(name: String, number: String?) {
    val colors = AreIptvTheme.colors
    Row(
        modifier = Modifier
            .width(AreIptvTheme.spacing.guideChannelWidth)
            .height(AreIptvTheme.spacing.guideRowHeight)
            .background(colors.surface1, RoundedCornerShape(AreIptvTheme.radius.sm))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column {
            if (number != null) {
                Text(text = number, style = AreIptvTheme.typography.mono, color = colors.textTertiary)
            }
            Text(text = name, style = AreIptvTheme.typography.label, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun FocusedInfoBar(info: GuideFocusedInfo?) {
    val colors = AreIptvTheme.colors
    val zone = ZoneId.systemDefault()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface1, RoundedCornerShape(AreIptvTheme.radius.md))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier.size(44.dp).background(colors.surface3, RoundedCornerShape(AreIptvTheme.radius.xs)),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = (info?.channel?.name ?: "--").take(3).uppercase(), style = AreIptvTheme.typography.caption, color = colors.textPrimary)
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = info?.slot?.title ?: "Focus a programme to see details",
                    style = AreIptvTheme.typography.h3,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (info?.slot?.isNow == true) {
                    Box(Modifier.size(6.dp).background(colors.live, CircleShape))
                    Text(text = "LIVE", style = AreIptvTheme.typography.caption, color = colors.live)
                }
            }
            if (info != null) {
                val start = Instant.ofEpochMilli(info.slot.startMs).atZone(zone).format(TimeFormatter)
                val end = Instant.ofEpochMilli(info.slot.endMs).atZone(zone).format(TimeFormatter)
                Text(
                    text = "${info.channel.name} · $start – $end",
                    style = AreIptvTheme.typography.caption,
                    color = colors.textSecondary,
                )
            }
        }
    }
}
