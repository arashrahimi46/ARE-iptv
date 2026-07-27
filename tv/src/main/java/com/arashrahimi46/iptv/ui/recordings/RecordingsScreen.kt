package com.arashrahimi46.iptv.ui.recordings

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
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.arashrahimi46.iptv.R
import coil.request.videoFrameMillis
import com.arashrahimi46.iptv.data.model.Recording
import com.arashrahimi46.iptv.ui.components.AreButton
import com.arashrahimi46.iptv.ui.components.AreButtonSize
import com.arashrahimi46.iptv.ui.components.AreButtonVariant
import com.arashrahimi46.iptv.ui.components.AreDialog
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.theme.glassChild
import com.arashrahimi46.iptv.ui.theme.glassSurface
import com.arashrahimi46.iptv.ui.theme.requestFocusWhenReady

/** Order the grouped sections render in (design §7): live capture first, then the archive. */
private val GROUP_ORDER = listOf(
    RecordingGroup.RECORDING_NOW,
    RecordingGroup.COMPLETED,
    RecordingGroup.INTERRUPTED,
    RecordingGroup.UNAVAILABLE,
)

/**
 * Recordings tab (Live TV Recording V1, design §7): DB-driven, grouped by state. Completed/interrupted
 * recordings play (partials as `≈`) via the existing player path; unavailable/missing are greyed with
 * a reconnect/why message; everything deletes with confirmation.
 */
@Composable
fun RecordingsScreen(
    onPlay: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val viewModel: RecordingsViewModel = viewModel(
        factory = RecordingsViewModel.factory(context.applicationContext as android.app.Application),
    )
    val rows by viewModel.rows.collectAsState()
    val colors = AreIptvTheme.colors
    val spacing = AreIptvTheme.spacing
    var toDelete by remember { mutableStateOf<Recording?>(null) }

    // Initial D-pad focus, matching every other tab: the shell leaves focus on the sidebar, so the
    // list reads as dead until the user blindly presses RIGHT. Lands on the first card's primary
    // action. Ordered by GROUP_ORDER, not by `rows`, so this is the card that actually renders first.
    val firstCardFocus = remember { FocusRequester() }
    val firstRowId = GROUP_ORDER.firstNotNullOfOrNull { group ->
        rows.firstOrNull { it.group == group }?.recording?.id
    }
    LaunchedEffect(firstRowId) {
        if (firstRowId != null) firstCardFocus.requestFocusWhenReady()
    }

    Column(modifier = modifier.fillMaxSize().padding(top = spacing.sp2)) {

        if (rows.isEmpty()) {
            Text(
                text = stringResource(R.string.recordings_empty),
                style = AreIptvTheme.typography.body,
                color = colors.textSecondary,
                modifier = Modifier.padding(horizontal = spacing.safeX),
            )
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = spacing.safeX, vertical = spacing.sp4),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GROUP_ORDER.forEach { group ->
                val groupRows = rows.filter { it.group == group }
                if (groupRows.isNotEmpty()) {
                    item(key = "header-$group") {
                        Text(
                            text = groupTitle(group),
                            style = AreIptvTheme.typography.h3,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
                        )
                    }
                    items(groupRows, key = { it.recording.id }) { row ->
                        RecordingCard(
                            row = row,
                            onPlay = { onPlay(row.recording.id) },
                            onDelete = { toDelete = row.recording },
                            actionFocusRequester = firstCardFocus.takeIf { row.recording.id == firstRowId },
                        )
                    }
                }
            }
        }
    }

    val deleting = toDelete
    if (deleting != null) {
        val confirmFocus = remember { FocusRequester() }
        Dialog(
            onDismissRequest = { toDelete = null },
            properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
        ) {
            androidx.compose.runtime.LaunchedEffect(Unit) { runCatching { confirmFocus.requestFocus() } }
            AreDialog(
                onDismiss = { toDelete = null },
                title = stringResource(R.string.recordings_delete_title),
                actions = {
                    AreButton(stringResource(R.string.action_cancel), onClick = { toDelete = null }, variant = AreButtonVariant.Ghost)
                    AreButton(
                        stringResource(R.string.recordings_delete),
                        onClick = { viewModel.delete(deleting); toDelete = null },
                        variant = AreButtonVariant.Danger,
                        modifier = Modifier.focusRequester(confirmFocus),
                    )
                },
            ) {
                Text(
                    text = stringResource(R.string.recordings_delete_body, deleting.channelName),
                    style = AreIptvTheme.typography.body,
                    color = colors.textSecondary,
                )
            }
        }
    }
}

@Composable
private fun groupTitle(group: RecordingGroup): String = when (group) {
    RecordingGroup.RECORDING_NOW -> stringResource(R.string.recordings_group_recording_now)
    RecordingGroup.COMPLETED -> stringResource(R.string.recordings_group_completed)
    RecordingGroup.INTERRUPTED -> stringResource(R.string.recordings_group_interrupted)
    RecordingGroup.UNAVAILABLE -> stringResource(R.string.recordings_group_unavailable)
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun RecordingCard(
    row: RecordingRow,
    onPlay: () -> Unit,
    onDelete: () -> Unit,
    // Non-null only for the first card in the list, so opening the tab lands on a real control.
    // Attached to whichever action this card actually shows first (Play, else Delete).
    actionFocusRequester: FocusRequester? = null,
) {
    val colors = AreIptvTheme.colors
    val unavailable = row.group == RecordingGroup.UNAVAILABLE
    val firstAction = if (actionFocusRequester != null) Modifier.focusRequester(actionFocusRequester) else Modifier
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glassSurface(RoundedCornerShape(AreIptvTheme.radius.lg))
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (row.videoUri != null) {
            RecordingThumb(uri = row.videoUri, modifier = Modifier.padding(end = 16.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (row.group == RecordingGroup.RECORDING_NOW) {
                Icon(Icons.Filled.FiberManualRecord, contentDescription = null, tint = colors.danger, modifier = Modifier.padding(end = 2.dp))
            }
            Text(
                text = row.title,
                style = AreIptvTheme.typography.h3,
                color = if (unavailable) colors.textTertiary else colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (row.subtitle != null) {
            Text(text = row.subtitle, style = AreIptvTheme.typography.caption, color = colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(Modifier.padding(top = 4.dp))
        Text(text = row.metaLabel, style = AreIptvTheme.typography.mono, color = colors.textTertiary)
        if (row.statusLabel != null) {
            Text(text = row.statusLabel, style = AreIptvTheme.typography.caption, color = colors.warning, modifier = Modifier.padding(top = 4.dp))
        }
        if (row.reconnectMessage != null) {
            Text(text = row.reconnectMessage, style = AreIptvTheme.typography.caption, color = colors.textTertiary, modifier = Modifier.padding(top = 4.dp))
        }
        if (row.playable || !unavailable) {
            Box(Modifier.padding(top = 12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (row.playable) {
                    AreButton(
                        text = stringResource(R.string.recordings_play),
                        onClick = onPlay,
                        variant = AreButtonVariant.Primary,
                        size = AreButtonSize.Small,
                        icon = Icons.Filled.PlayArrow,
                        modifier = firstAction,
                    )
                }
                if (row.group != RecordingGroup.RECORDING_NOW) {
                    AreButton(
                        text = stringResource(R.string.recordings_delete),
                        onClick = onDelete,
                        variant = AreButtonVariant.Ghost,
                        size = AreButtonSize.Small,
                        icon = Icons.Filled.Delete,
                        // Only when Play isn't there to take it -- a card must not hold the
                        // requester twice, and Play is the first control when it exists.
                        modifier = if (row.playable) Modifier else firstAction,
                    )
                }
            }
        } else if (unavailable) {
            // Unavailable/missing: no play, but still deletable (drops the row; file delete queues).
            Box(Modifier.padding(top = 12.dp))
            AreButton(
                text = stringResource(R.string.recordings_delete),
                onClick = onDelete,
                variant = AreButtonVariant.Ghost,
                size = AreButtonSize.Small,
                icon = Icons.Filled.Delete,
                modifier = firstAction,
            )
        }
        }
    }
}

/**
 * 16:9 thumbnail: a still frame ~3s into the recording (skips black intro frames), decoded by Coil's
 * [coil.decode.VideoFrameDecoder] and cached. A play glyph on [surface2] shows through as the empty
 * state while it loads or when a partial/interrupted file can't yield a frame.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun RecordingThumb(uri: android.net.Uri, modifier: Modifier = Modifier) {
    val colors = AreIptvTheme.colors
    val shape = RoundedCornerShape(AreIptvTheme.radius.md)
    Box(
        modifier = modifier
            .size(width = 150.dp, height = 84.dp)
            .glassChild(shape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = colors.textTertiary, modifier = Modifier.size(26.dp))
        coil.compose.AsyncImage(
            model = coil.request.ImageRequest.Builder(LocalContext.current)
                .data(uri)
                .videoFrameMillis(3000)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
