@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.arashrahimi46.iptv.ui.streams

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.arashrahimi46.iptv.R
import com.arashrahimi46.iptv.data.model.DirectStream
import com.arashrahimi46.iptv.data.model.directStreamLabel
import com.arashrahimi46.iptv.ui.components.AreButton
import com.arashrahimi46.iptv.ui.components.AreButtonVariant
import com.arashrahimi46.iptv.ui.components.AreDialog
import com.arashrahimi46.iptv.ui.components.AreTextField
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.theme.TvFocusable
import com.arashrahimi46.iptv.ui.theme.rememberPlaybackFocusRequester

/**
 * Streams tab — VLC-style "Open network stream". A single button pops a one-field dialog; paste a
 * direct video URL and it plays through the existing player. Every played URL is auto-saved as a
 * renamable "box" (its play history, newest first); long-press a box to rename or delete it.
 * Playback is delegated to the shared player via
 * [com.arashrahimi46.iptv.ui.player.PlaybackSource.DirectStream]; [onStreamSelected] carries the
 * saved row id up to the nav host, which opens `player/direct/{id}`.
 */
@Composable
fun StreamsScreen(onStreamSelected: (Long) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val viewModel: StreamsViewModel = viewModel(
        factory = StreamsViewModel.factory(context.applicationContext as android.app.Application),
    )
    val streams by viewModel.streams.collectAsState()
    val colors = AreIptvTheme.colors
    val spacing = AreIptvTheme.spacing

    var showOpenDialog by remember { mutableStateOf(false) }
    var contextStream by remember { mutableStateOf<DirectStream?>(null) }
    var renameStream by remember { mutableStateOf<DirectStream?>(null) }
    // Which box launched playback — restores D-pad focus to it on return from the player.
    var lastPlayedId by rememberSaveable { mutableStateOf<Long?>(null) }

    fun play(id: Long) {
        lastPlayedId = id
        onStreamSelected(id)
    }

    val openButtonFocus = remember { FocusRequester() }

    Column(modifier = modifier.fillMaxSize().padding(start = spacing.safeX, end = spacing.safeX, top = spacing.sp2, bottom = spacing.sp6)) {

        // The VLC "Open network stream" entry point.
        AreButton(
            text = stringResource(R.string.streams_open),
            onClick = { showOpenDialog = true },
            icon = Icons.Filled.AddLink,
            modifier = Modifier.focusRequester(openButtonFocus),
        )
        Box(Modifier.height(spacing.sp6))

        if (streams.isEmpty()) {
            // Nothing saved yet — focus the open button so the D-pad has a target.
            androidx.compose.runtime.LaunchedEffect(Unit) {
                androidx.compose.runtime.withFrameNanos { }
                runCatching { openButtonFocus.requestFocus() }
            }
            Column(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Filled.Link, contentDescription = null, tint = colors.textTertiary, modifier = Modifier.size(48.dp))
                Box(Modifier.height(spacing.sp4))
                Text(
                    text = stringResource(R.string.streams_empty),
                    style = AreIptvTheme.typography.body,
                    color = colors.textSecondary,
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 260.dp),
                modifier = Modifier.fillMaxWidth().weight(1f),
                // Inner padding so a focused box's ring/glow + focus-scale growth isn't clipped
                // at the grid's own viewport edges (the top row otherwise loses the top of its ring).
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(streams, key = { it.id }) { stream ->
                    val focusRequester = rememberPlaybackFocusRequester(
                        savedId = lastPlayedId,
                        itemId = stream.id,
                    ) { lastPlayedId = null }
                    StreamBox(
                        stream = stream,
                        onClick = { play(stream.id) },
                        onLongClick = { contextStream = stream },
                        modifier = Modifier.focusRequester(focusRequester),
                    )
                }
            }
        }
    }

    if (showOpenDialog) {
        OpenStreamDialog(
            onDismiss = { showOpenDialog = false },
            onPlay = { url ->
                showOpenDialog = false
                viewModel.openStream(url) { id -> play(id) }
            },
        )
    }

    contextStream?.let { stream ->
        StreamContextDialog(
            stream = stream,
            onDismiss = { contextStream = null },
            onRename = { contextStream = null; renameStream = stream },
            onDelete = { contextStream = null; viewModel.delete(stream) },
        )
    }

    renameStream?.let { stream ->
        RenameStreamDialog(
            initial = stream.name.orEmpty(),
            onDismiss = { renameStream = null },
            onSave = { name -> renameStream = null; viewModel.rename(stream.id, name) },
        )
    }
}

/** A single history box: the label (custom name or URL host) + the raw URL, focusable/playable. */
@Composable
private fun StreamBox(
    stream: DirectStream,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AreIptvTheme.colors
    val shape = RoundedCornerShape(AreIptvTheme.radius.md)
    TvFocusable(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
        shape = shape,
        backgroundColor = colors.surface2,
        borderColor = colors.borderDefault,
    ) { _, _ ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier.size(40.dp).background(colors.surface3, RoundedCornerShape(AreIptvTheme.radius.xs)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = directStreamLabel(stream.url, stream.name),
                    style = AreIptvTheme.typography.label,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stream.url,
                    style = AreIptvTheme.typography.mono,
                    color = colors.textTertiary,
                    // Show up to 3 lines of the URL before ellipsizing (the box grows to fit).
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** VLC-style single-field "Open network stream" dialog. */
@Composable
private fun OpenStreamDialog(onDismiss: () -> Unit, onPlay: (String) -> Unit) {
    var url by remember { mutableStateOf("") }
    val fieldFocus = remember { FocusRequester() }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
    ) {
        androidx.compose.runtime.LaunchedEffect(Unit) {
            androidx.compose.runtime.withFrameNanos { }
            runCatching { fieldFocus.requestFocus() }
        }
        AreDialog(
            onDismiss = onDismiss,
            title = stringResource(R.string.streams_open),
            actions = {
                AreButton(stringResource(R.string.action_cancel), onClick = onDismiss, variant = AreButtonVariant.Ghost)
                AreButton(
                    stringResource(R.string.player_play),
                    onClick = { onPlay(url) },
                    disabled = url.isBlank(),
                )
            },
        ) {
            AreTextField(
                value = url,
                onValueChange = { url = it },
                placeholder = stringResource(R.string.streams_url_placeholder),
                helper = stringResource(R.string.streams_url_helper),
                mono = true,
                icon = Icons.Filled.Link,
                modifier = Modifier.fillMaxWidth().focusRequester(fieldFocus),
            )
        }
    }
}

/** Long-press menu for a history box: rename or delete. */
@Composable
private fun StreamContextDialog(
    stream: DirectStream,
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    val renameFocus = remember { FocusRequester() }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
    ) {
        androidx.compose.runtime.LaunchedEffect(Unit) { runCatching { renameFocus.requestFocus() } }
        AreDialog(
            onDismiss = onDismiss,
            title = directStreamLabel(stream.url, stream.name),
            actions = {
                AreButton(stringResource(R.string.action_cancel), onClick = onDismiss, variant = AreButtonVariant.Ghost)
                AreButton(stringResource(R.string.action_delete), onClick = onDelete, variant = AreButtonVariant.Danger)
                AreButton(stringResource(R.string.streams_rename), onClick = onRename, modifier = Modifier.focusRequester(renameFocus))
            },
        ) {
            Text(text = stream.url, style = AreIptvTheme.typography.mono, color = AreIptvTheme.colors.textSecondary)
        }
    }
}

/** Rename a history box to a friendly label. */
@Composable
private fun RenameStreamDialog(initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf(initial) }
    val fieldFocus = remember { FocusRequester() }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
    ) {
        androidx.compose.runtime.LaunchedEffect(Unit) {
            androidx.compose.runtime.withFrameNanos { }
            runCatching { fieldFocus.requestFocus() }
        }
        AreDialog(
            onDismiss = onDismiss,
            title = stringResource(R.string.streams_rename),
            actions = {
                AreButton(stringResource(R.string.action_cancel), onClick = onDismiss, variant = AreButtonVariant.Ghost)
                AreButton(stringResource(R.string.streams_save), onClick = { onSave(name) })
            },
        ) {
            AreTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = stringResource(R.string.streams_rename_placeholder),
                modifier = Modifier.fillMaxWidth().focusRequester(fieldFocus),
            )
        }
    }
}
