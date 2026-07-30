package com.arashrahimi46.iptv.mobile.ui.recordings

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arashrahimi46.iptv.data.model.Recording
import com.arashrahimi46.iptv.mobile.R
import com.arashrahimi46.iptv.ui.components.AreDialog
import com.arashrahimi46.iptv.ui.components.AreButton
import com.arashrahimi46.iptv.ui.components.AreButtonVariant
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.theme.glassWell

/**
 * Touch-first Recordings list: same [RecordingsViewModel] grouping (Recording now / Completed /
 * Interrupted / Unavailable) as :tv's Recordings tab, laid out as a flat sectioned scrollable list
 * instead of a D-pad grid.
 */
@Composable
fun RecordingsScreen(onPlay: (Long) -> Unit, viewModel: RecordingsViewModel = viewModel()) {
    val rows by viewModel.rows.collectAsState()
    val colors = AreIptvTheme.colors
    var pendingDelete by remember { mutableStateOf<Recording?>(null) }

    if (rows.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.recordings_empty), style = AreIptvTheme.typography.body, color = colors.textSecondary)
        }
        return
    }

    val grouped = rows.groupBy { it.group }
    val sections = listOf(
        RecordingGroup.RECORDING_NOW to R.string.recordings_group_recording_now,
        RecordingGroup.COMPLETED to R.string.recordings_group_completed,
        RecordingGroup.INTERRUPTED to R.string.recordings_group_interrupted,
        RecordingGroup.UNAVAILABLE to R.string.recordings_group_unavailable,
    )

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)) {
        sections.forEach { (group, labelRes) ->
            val groupRows = grouped[group].orEmpty()
            if (groupRows.isEmpty()) return@forEach
            item {
                Text(
                    text = stringResource(labelRes),
                    style = AreIptvTheme.typography.label,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 6.dp),
                )
            }
            items(groupRows, key = { it.recording.id }) { row ->
                RecordingRowItem(
                    row = row,
                    onPlay = { onPlay(row.recording.id) },
                    onDelete = { pendingDelete = row.recording },
                )
            }
        }
    }

    val toDelete = pendingDelete
    if (toDelete != null) {
        Dialog(onDismissRequest = { pendingDelete = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            AreDialog(
                onDismiss = { pendingDelete = null },
                title = stringResource(R.string.recordings_delete_title),
                actions = {
                    AreButton(text = stringResource(android.R.string.cancel), onClick = { pendingDelete = null }, variant = AreButtonVariant.Secondary)
                    AreButton(
                        text = stringResource(R.string.recordings_delete),
                        onClick = {
                            viewModel.delete(toDelete)
                            pendingDelete = null
                        },
                        variant = AreButtonVariant.Primary,
                    )
                },
            ) {
                Text(
                    text = stringResource(R.string.recordings_delete_body, toDelete.programTitle ?: toDelete.channelName),
                    style = AreIptvTheme.typography.body,
                    color = colors.textSecondary,
                )
            }
        }
    }
}

@Composable
private fun RecordingRowItem(row: RecordingRow, onPlay: () -> Unit, onDelete: () -> Unit) {
    val colors = AreIptvTheme.colors
    val shape = RoundedCornerShape(AreIptvTheme.radius.md)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .glassWell(shape)
            .then(if (row.playable) Modifier.clickable(onClick = onPlay) else Modifier)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(text = row.subtitle ?: row.title, style = AreIptvTheme.typography.label, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (row.subtitle != null) {
                Text(text = row.title, style = AreIptvTheme.typography.caption, color = colors.textTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(text = row.metaLabel, style = AreIptvTheme.typography.caption, color = colors.textTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            row.statusLabel?.let { Text(text = it, style = AreIptvTheme.typography.caption, color = colors.danger) }
            row.reconnectMessage?.let { Text(text = it, style = AreIptvTheme.typography.caption, color = colors.textTertiary) }
        }
        if (row.playable) {
            Box(Modifier.size(40.dp).clickable(onClick = onPlay), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.PlayArrow, contentDescription = stringResource(R.string.recordings_play), tint = colors.textPrimary)
            }
        }
        Box(Modifier.size(40.dp).clickable(onClick = onDelete), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.recordings_delete), tint = colors.textSecondary)
        }
    }
}
