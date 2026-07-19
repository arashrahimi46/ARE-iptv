package com.arashrahimi46.iptv.ui.guide

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Text
import com.arashrahimi46.iptv.ui.components.AreChip
import com.arashrahimi46.iptv.ui.components.AreGuideCell
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** px-per-minute scale for proportional GuideCell widths (mirrors Guide.jsx `PX`). */
private val DpPerMinute = 3.dp
private val TimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/**
 * TV Guide (Guide.jsx): 6-hour rolling window, day chips, channel-group
 * filter, timeline header, and the sticky "focused-program info bar" (no
 * tooltips on TV -- driven by [AreGuideCell]'s `onFocusChange`). Selecting a
 * currently-live cell opens the real [com.arashrahimi46.iptv.ui.player.LivePlayerScreen].
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

    Column(modifier = modifier.padding(top = spacing.sp6, bottom = spacing.sp10)) {
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

        // Channel-group filter.
        Row(
            modifier = Modifier.padding(horizontal = spacing.safeX).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            state.groups.forEach { group ->
                AreChip(text = if (group == "All") "All channels" else group, onClick = { viewModel.selectGroup(group) }, selected = group == state.selectedGroup)
            }
        }
        Box(Modifier.height(spacing.sp5))

        // Sticky focused-program info bar (no hover/tooltips on TV -- last focused cell stays shown).
        Box(Modifier.padding(horizontal = spacing.safeX)) {
            FocusedInfoBar(focused)
        }
        Box(Modifier.height(spacing.sp5))

        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .padding(horizontal = spacing.safeX)
                .horizontalScroll(scrollState),
        ) {
            TimelineHeader(windowStartMs = state.windowStartMs, windowEndMs = state.windowEndMs, zone = zone)
            Box(Modifier.height(8.dp))
            state.rows.forEach { row ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChannelHeaderCell(name = row.channel.name, number = row.channel.number)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.slots.forEach { slot ->
                            val durationMinutes = ((slot.endMs - slot.startMs) / 60000L).coerceAtLeast(1L)
                            AreGuideCell(
                                title = slot.title,
                                time = Instant.ofEpochMilli(slot.startMs).atZone(zone).format(TimeFormatter),
                                onClick = { if (slot.isNow) onChannelSelected(row.channel.id) },
                                live = slot.isNow,
                                now = slot.isNow,
                                width = (DpPerMinute * durationMinutes.toInt()) - 6.dp,
                                onFocusChange = { isFocused -> if (isFocused) viewModel.setFocused(GuideFocusedInfo(row.channel, slot)) },
                            )
                        }
                    }
                }
                Box(Modifier.height(8.dp))
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
