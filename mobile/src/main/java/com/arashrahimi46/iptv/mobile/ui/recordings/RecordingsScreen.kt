package com.arashrahimi46.iptv.mobile.ui.recordings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arashrahimi46.iptv.core.R as CoreR
import com.arashrahimi46.iptv.data.model.Recording
import com.arashrahimi46.iptv.mobile.ui.components.AreEmptyState
import com.arashrahimi46.iptv.mobile.ui.components.AreIconButton
import com.arashrahimi46.iptv.mobile.ui.components.AreIconButtonSize
import com.arashrahimi46.iptv.mobile.ui.components.AreIconButtonVariant
import com.arashrahimi46.iptv.mobile.ui.components.AreListRow
import com.arashrahimi46.iptv.mobile.ui.components.AreScreenScaffold
import com.arashrahimi46.iptv.mobile.ui.components.AreSectionHeader
import com.arashrahimi46.iptv.mobile.ui.components.AreSwipeToDismissRow
import com.arashrahimi46.iptv.mobile.ui.components.RecordingIndicator
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.theme.LocalReducedMotion
import com.arashrahimi46.iptv.ui.theme.glassWell
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val Sections = listOf(
    RecordingGroup.RECORDING_NOW to CoreR.string.recordings_group_recording_now,
    RecordingGroup.COMPLETED to CoreR.string.recordings_group_completed,
    RecordingGroup.INTERRUPTED to CoreR.string.recordings_group_interrupted,
    RecordingGroup.UNAVAILABLE to CoreR.string.recordings_group_unavailable,
)

/**
 * Touch-first Recordings list: same [RecordingsViewModel] grouping (Recording now / Completed /
 * Interrupted / Unavailable) as before, laid out as a sectioned scrollable list with a real top
 * bar, ripple rows, a >=48dp play button and swipe-to-delete.
 *
 * Delete is *deferred*, not undone: the swiped row is hidden locally, a snackbar offers Undo for
 * its ~4s life, and only when that expires does the repository actually delete. That needs no new
 * DAO method and no way to resurrect a file that is already gone from the drive.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingsScreen(
    onPlay: (Long) -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: RecordingsViewModel = viewModel(),
) {
    val rows by viewModel.rows.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    // Ids swiped away but not yet committed -- hidden from the list while the snackbar is up.
    val pendingDelete = remember { mutableStateListOf<Long>() }
    val undoLabel = stringResource(CoreR.string.common_undo)
    val deletedLabel = stringResource(CoreR.string.common_deleted)
    val deleteLabel = stringResource(CoreR.string.recordings_delete)
    val playLabel = stringResource(CoreR.string.recordings_play)
    val reducedMotion = LocalReducedMotion.current
    val layoutDirection = LocalLayoutDirection.current

    val visible = rows.filter { it.recording.id !in pendingDelete }
    val grouped = visible.groupBy { it.group }
    val hasActive = grouped[RecordingGroup.RECORDING_NOW].orEmpty().isNotEmpty()

    // One shared 1Hz tick drives every in-progress row's elapsed timer; it stops when none is live.
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(hasActive) {
        while (hasActive) {
            nowMs = System.currentTimeMillis()
            delay(1_000)
        }
    }

    val requestDelete: (Recording) -> Unit = { recording ->
        pendingDelete.add(recording.id)
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = deletedLabel,
                actionLabel = undoLabel,
                duration = SnackbarDuration.Short,
            )
            pendingDelete.remove(recording.id)
            if (result != SnackbarResult.ActionPerformed) viewModel.delete(recording)
        }
    }

    AreScreenScaffold(
        title = stringResource(CoreR.string.recordings_title),
        onBack = onBack,
        snackbarHostState = snackbarHostState,
    ) { insets ->
        if (visible.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = insets.calculateTopPadding(),
                        bottom = insets.calculateBottomPadding(),
                    ),
                verticalArrangement = Arrangement.Center,
            ) {
                AreEmptyState(
                    title = stringResource(CoreR.string.recordings_title),
                    message = stringResource(CoreR.string.recordings_empty),
                    icon = Icons.Filled.FiberManualRecord,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = insets.calculateTopPadding() + 8.dp,
                    bottom = insets.calculateBottomPadding() + 40.dp,
                    start = insets.calculateStartPadding(layoutDirection),
                    end = insets.calculateEndPadding(layoutDirection),
                ),
            ) {
                Sections.forEach { (group, labelRes) ->
                    val groupRows = grouped[group].orEmpty()
                    if (groupRows.isEmpty()) return@forEach
                    item(key = "header-$group", contentType = "header") {
                        AreSectionHeader(stringResource(labelRes))
                    }
                    items(
                        items = groupRows,
                        key = { it.recording.id },
                        contentType = { "recording" },
                    ) { row ->
                        AreSwipeToDismissRow(
                            onDismissed = { requestDelete(row.recording) },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            icon = Icons.Filled.Delete,
                            label = deleteLabel,
                        ) {
                            RecordingCard(
                                row = row,
                                elapsedMs = nowMs - row.recording.startedAtMs,
                                reducedMotion = reducedMotion,
                                playLabel = playLabel,
                                onPlay = { onPlay(row.recording.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordingCard(
    row: RecordingRow,
    elapsedMs: Long,
    reducedMotion: Boolean,
    playLabel: String,
    onPlay: () -> Unit,
) {
    val colors = AreIptvTheme.colors
    val shape = RoundedCornerShape(AreIptvTheme.radius.md)

    val trailing: (@Composable () -> Unit)? = when {
        row.group == RecordingGroup.RECORDING_NOW -> {
            {
                RecordingIndicator(
                    reconnecting = false,
                    elapsedMs = elapsedMs,
                    reducedMotion = reducedMotion,
                )
            }
        }
        row.playable -> {
            {
                AreIconButton(
                    icon = Icons.Filled.PlayArrow,
                    contentDescription = playLabel,
                    onClick = onPlay,
                    variant = AreIconButtonVariant.Glass,
                    size = AreIconButtonSize.Small,
                )
            }
        }
        else -> null
    }

    Column(Modifier.fillMaxWidth().glassWell(shape)) {
        AreListRow(
            title = row.subtitle ?: row.title,
            supporting = buildString {
                if (row.subtitle != null) append(row.title).append('\n')
                append(row.metaLabel)
            },
            onClick = if (row.playable) onPlay else null,
            trailing = trailing,
        )
        if (row.statusLabel != null || row.reconnectMessage != null) {
            Column(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                row.statusLabel?.let {
                    Text(text = it, style = AreIptvTheme.typography.caption, color = colors.danger)
                }
                row.reconnectMessage?.let {
                    Text(text = it, style = AreIptvTheme.typography.caption, color = colors.textTertiary)
                }
            }
        }
    }
}
