package com.arashrahimi46.iptv.mobile.ui.guide

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.arashrahimi46.iptv.data.model.Channel
import com.arashrahimi46.iptv.mobile.R
import com.arashrahimi46.iptv.ui.components.AreChip
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.theme.glassWell
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/**
 * TV Guide for touch: same real-EPG pipeline as :tv's Guide ([GuideViewModel] mirrors
 * [com.arashrahimi46.iptv.ui.guide.GuideViewModel]'s query/window logic) laid out as a scrollable
 * channel list instead of a D-pad timeline grid -- each row shows the channel plus a horizontally
 * scrolling strip of that channel's programmes for the selected day, the "now" slot highlighted.
 * Day chips (Yesterday/Today/Tomorrow) and category chips mirror :tv's Guide.jsx funnel via
 * [AreChip].
 */
@Composable
fun GuideScreen(onOpenChannel: (Channel) -> Unit, viewModel: GuideViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val colors = AreIptvTheme.colors
    val days = listOf(GuideDay.Yesterday, GuideDay.Today, GuideDay.Tomorrow)

    if (!state.hasSource) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.guide_no_source), style = AreIptvTheme.typography.body, color = colors.textSecondary)
        }
        return
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            days.forEach { day ->
                AreChip(
                    text = stringResource(day.labelRes),
                    selected = day == state.day,
                    onClick = { viewModel.selectDay(day) },
                )
            }
        }
        if (state.groups.size > 1) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            ) {
                items(state.groups, key = { it }) { group ->
                    AreChip(text = group, selected = group == state.selectedGroup, onClick = { viewModel.selectGroup(group) })
                }
            }
        }
        if (state.epgUnavailable) {
            Text(
                text = stringResource(R.string.guide_epg_unavailable),
                style = AreIptvTheme.typography.caption,
                color = colors.textTertiary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp),
        ) {
            items(state.rows, key = { it.channel.id }) { row ->
                GuideChannelRowItem(row, onClick = { onOpenChannel(row.channel) })
            }
        }
    }
}

@Composable
private fun GuideChannelRowItem(row: GuideChannelRow, onClick: () -> Unit) {
    val colors = AreIptvTheme.colors
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AsyncImage(
                model = row.channel.logoUrl,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(32.dp),
            )
            Text(
                text = row.channel.name,
                style = AreIptvTheme.typography.label,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(row.slots) { slot ->
                GuideProgramCell(slot, onClick)
            }
        }
    }
}

@Composable
private fun GuideProgramCell(slot: GuideProgramSlot, onClick: () -> Unit) {
    val colors = AreIptvTheme.colors
    val shape = RoundedCornerShape(AreIptvTheme.radius.md)
    val start = timeFormatter.format(Instant.ofEpochMilli(slot.startMs).atZone(ZoneId.systemDefault()))
    Column(
        modifier = Modifier
            .width(150.dp)
            .glassWell(shape)
            .then(if (slot.isNow) Modifier.border(1.dp, colors.accent, shape) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Text(text = start, style = AreIptvTheme.typography.caption, color = colors.textTertiary)
        Text(
            text = slot.title,
            style = AreIptvTheme.typography.body,
            color = colors.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
