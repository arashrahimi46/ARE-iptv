package com.arashrahimi46.iptv.ui.player

import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Text
import com.arashrahimi46.iptv.R
import com.arashrahimi46.iptv.data.recording.RecordingStorage
import com.arashrahimi46.iptv.ui.components.AreDialog
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.theme.TvFocusable

/**
 * "Record to …" drive chooser for Live TV Recording. Replaces SAF's `ACTION_OPEN_DOCUMENT_TREE`
 * picker, which has no handler on Android TV -- here the user picks between the app's own volumes
 * (internal + any mounted USB/SD) with the D-pad. Only shown when there's more than one volume;
 * with just internal storage the caller starts recording directly without asking. First row is
 * focused on open (TV convention: focus the default action).
 */
@Composable
fun RecordVolumeDialog(
    volumes: List<RecordingStorage.Volume>,
    onPick: (RecordingStorage.Volume) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val firstRowFocus = remember { FocusRequester() }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        AreDialog(onDismiss = onDismiss, title = stringResource(R.string.recording_pick_volume_title), width = 420.dp) {
            Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()).padding(vertical = 8.dp)) {
                volumes.forEachIndexed { index, volume ->
                    val name = stringResource(
                        if (volume.isInternal) R.string.recording_volume_internal else R.string.recording_volume_usb,
                    )
                    val label = volume.freeBytes?.let {
                        "$name — ${stringResource(R.string.recording_volume_free, Formatter.formatShortFileSize(context, it))}"
                    } ?: name
                    VolumeRow(
                        label = label,
                        modifier = if (index == 0) Modifier.focusRequester(firstRowFocus) else Modifier,
                        onClick = { onPick(volume) },
                    )
                }
            }
        }
    }
    LaunchedEffect(Unit) { runCatching { firstRowFocus.requestFocus() } }
}

@Composable
private fun VolumeRow(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = AreIptvTheme.colors
    TvFocusable(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AreIptvTheme.radius.md),
    ) { focused, _ ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (focused) colors.surfaceOverlay else Color.Transparent,
                    RoundedCornerShape(AreIptvTheme.radius.md),
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(4.dp))
            Text(
                text = label,
                style = AreIptvTheme.typography.body,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
