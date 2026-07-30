package com.arashrahimi46.iptv.mobile.ui.streams

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
import androidx.compose.material.icons.filled.Edit
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
import com.arashrahimi46.iptv.data.model.DirectStream
import com.arashrahimi46.iptv.data.model.directStreamLabel
import com.arashrahimi46.iptv.mobile.R
import com.arashrahimi46.iptv.mobile.ui.components.AreTextField
import com.arashrahimi46.iptv.ui.components.AreButton
import com.arashrahimi46.iptv.ui.components.AreButtonVariant
import com.arashrahimi46.iptv.ui.components.AreDialog
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.theme.glassWell

/**
 * Touch-first "Open network stream" screen: paste a direct video URL and play it, plus the play
 * history (rename/delete a box) -- same [StreamsViewModel]/[com.arashrahimi46.iptv.data.model.DirectStream]
 * dedup-by-URL data pipeline as :tv's Streams tab.
 */
@Composable
fun StreamsScreen(onPlay: (Long) -> Unit, viewModel: StreamsViewModel = viewModel()) {
    val streams by viewModel.streams.collectAsState()
    val colors = AreIptvTheme.colors
    var url by remember { mutableStateOf("") }
    var renameTarget by remember { mutableStateOf<DirectStream?>(null) }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            AreTextField(
                value = url,
                onValueChange = { url = it },
                label = stringResource(R.string.streams_open),
                placeholder = stringResource(R.string.streams_url_placeholder),
                helper = stringResource(R.string.streams_url_helper),
                mono = true,
            )
            AreButton(
                text = stringResource(R.string.streams_open),
                onClick = { viewModel.openStream(url) { id -> url = ""; onPlay(id) } },
                variant = AreButtonVariant.Primary,
                full = true,
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        if (streams.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.streams_empty), style = AreIptvTheme.typography.body, color = colors.textSecondary)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
                items(streams, key = { it.id }) { stream ->
                    StreamRow(
                        stream = stream,
                        onPlay = { onPlay(stream.id) },
                        onRename = { renameTarget = stream },
                        onDelete = { viewModel.delete(stream) },
                    )
                }
            }
        }
    }

    val target = renameTarget
    if (target != null) {
        RenameDialog(
            initial = target.name.orEmpty(),
            onDismiss = { renameTarget = null },
            onConfirm = { name ->
                viewModel.rename(target.id, name)
                renameTarget = null
            },
        )
    }
}

@Composable
private fun StreamRow(stream: DirectStream, onPlay: () -> Unit, onRename: () -> Unit, onDelete: () -> Unit) {
    val colors = AreIptvTheme.colors
    val shape = RoundedCornerShape(AreIptvTheme.radius.md)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .glassWell(shape)
            .clickable(onClick = onPlay)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = directStreamLabel(stream.url, stream.name),
                style = AreIptvTheme.typography.label,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(text = stream.url, style = AreIptvTheme.typography.mono.copy(color = colors.textTertiary), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(Modifier.size(40.dp).clickable(onClick = onRename), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.streams_rename), tint = colors.textSecondary)
        }
        Box(Modifier.size(40.dp).clickable(onClick = onDelete), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.recordings_delete), tint = colors.textSecondary)
        }
        Box(Modifier.size(40.dp).clickable(onClick = onPlay), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.PlayArrow, contentDescription = stringResource(R.string.recordings_play), tint = colors.textPrimary)
        }
    }
}

@Composable
private fun RenameDialog(initial: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf(initial) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        AreDialog(
            onDismiss = onDismiss,
            title = stringResource(R.string.streams_rename),
            actions = {
                AreButton(text = stringResource(android.R.string.cancel), onClick = onDismiss, variant = AreButtonVariant.Secondary)
                AreButton(text = stringResource(R.string.streams_save), onClick = { onConfirm(name) }, variant = AreButtonVariant.Primary)
            },
        ) {
            AreTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = stringResource(R.string.streams_rename_placeholder),
            )
        }
    }
}
