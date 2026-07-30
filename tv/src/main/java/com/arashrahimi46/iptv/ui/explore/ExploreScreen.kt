package com.arashrahimi46.iptv.ui.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Text
import com.arashrahimi46.iptv.core.R
import com.arashrahimi46.iptv.data.parser.ExploreEntry
import com.arashrahimi46.iptv.data.parser.MAX_PLAYLISTS
import com.arashrahimi46.iptv.ui.components.AreButton
import com.arashrahimi46.iptv.ui.components.AreButtonSize
import com.arashrahimi46.iptv.ui.components.AreButtonVariant
import com.arashrahimi46.iptv.ui.components.AreDialog
import com.arashrahimi46.iptv.ui.components.AreTextField
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.theme.TvFocusable
import com.arashrahimi46.iptv.ui.theme.glassSurface

/**
 * The curated Explore catalogue: around twenty vetted, publicly listed free playlists, so that adding
 * a playlist doesn't require already having a URL.
 *
 * Selecting an entry imports it as an ordinary M3U source (see [ExploreViewModel.add]) -- there is no
 * separate browse or playback path here, which is the whole reason this feature is small.
 *
 * @param onAdded invoked with the new source id once an import finishes and it's been made active.
 */
@Composable
fun ExploreScreen(onAdded: (Long) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val viewModel: ExploreViewModel = viewModel(
        factory = ExploreViewModel.factory(context.applicationContext as android.app.Application),
    )
    val entries by viewModel.entries.collectAsState()
    val status by viewModel.status.collectAsState()
    val playlistCount by viewModel.playlistCount.collectAsState()
    val isAtCap by viewModel.isAtCap.collectAsState()
    val colors = AreIptvTheme.colors

    // The entry awaiting a name, or null. Held here rather than in the ViewModel because it's pure
    // screen state -- nothing outside this composable cares which card is mid-dialog.
    var naming by remember { mutableStateOf<ExploreEntry?>(null) }
    var showCap by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgBase)
            .padding(horizontal = 40.dp, vertical = 24.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.widthIn(max = 760.dp)) {
                Text(text = stringResource(R.string.explore_title), style = AreIptvTheme.typography.display, color = colors.textPrimary)
                Box(Modifier.height(6.dp))
                // States plainly that these are third-party streams we neither host nor control --
                // the same position §2 and §10 of the Terms take, said where the user actually is.
                Text(
                    text = stringResource(R.string.explore_subtitle),
                    style = AreIptvTheme.typography.body,
                    color = colors.textSecondary,
                )
            }
            Text(
                text = stringResource(R.string.explore_used_count, playlistCount, MAX_PLAYLISTS),
                style = AreIptvTheme.typography.caption,
                color = if (isAtCap) colors.danger else colors.textTertiary,
            )
        }

        Box(Modifier.height(18.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 10.dp, horizontal = 6.dp),
        ) {
            items(entries, key = { it.id }) { entry ->
                ExploreCard(
                    entry = entry,
                    // At the cap the cards stay focusable and explain why, rather than going dead:
                    // a tile that silently does nothing reads as a broken app.
                    onClick = { if (isAtCap) showCap = true else naming = entry },
                )
            }
        }

        Text(
            text = stringResource(R.string.explore_hint),
            style = AreIptvTheme.typography.caption,
            color = colors.textTertiary,
        )
    }

    naming?.let { entry ->
        ExploreNameDialog(
            entry = entry,
            onDismiss = { naming = null },
            onConfirm = { name -> naming = null; viewModel.add(entry, name, onAdded) },
        )
    }

    (status as? ExploreStatus.Adding)?.let { adding ->
        Dialog(onDismissRequest = {}, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            AreDialog(onDismiss = {}, title = stringResource(R.string.explore_adding_title)) {
                Text(
                    text = stringResource(R.string.explore_adding_body, adding.entry.name),
                    style = AreIptvTheme.typography.body,
                    color = colors.textSecondary,
                )
            }
        }
    }

    (status as? ExploreStatus.Failed)?.let { failed ->
        Dialog(onDismissRequest = viewModel::dismissError, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            AreDialog(
                onDismiss = viewModel::dismissError,
                title = stringResource(R.string.explore_failed_title),
                actions = { AreButton(stringResource(R.string.action_close), onClick = viewModel::dismissError) },
            ) {
                // Names the playlist, so a dead curated URL reads as "this list is broken" rather
                // than "your connection is" -- the accepted cost of a bundled catalogue.
                Text(
                    text = stringResource(R.string.explore_failed_body, failed.entry.name),
                    style = AreIptvTheme.typography.body,
                    color = colors.textSecondary,
                )
            }
        }
    }

    if (showCap) {
        Dialog(onDismissRequest = { showCap = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            AreDialog(
                onDismiss = { showCap = false },
                title = stringResource(R.string.explore_cap_title, MAX_PLAYLISTS),
                actions = { AreButton(stringResource(R.string.action_close), onClick = { showCap = false }) },
            ) {
                Text(
                    text = stringResource(R.string.explore_cap_body),
                    style = AreIptvTheme.typography.body,
                    color = colors.textSecondary,
                )
            }
        }
    }
}

@Composable
private fun ExploreCard(entry: ExploreEntry, onClick: () -> Unit) {
    val colors = AreIptvTheme.colors
    val shape = RoundedCornerShape(AreIptvTheme.radius.lg)
    // Fill comes from glassSurface() below and ONLY from there -- passing backgroundColor as well
    // stacked a second surfaceGlass (plus glassSurface's own hairline + shadow) on the same card,
    // which is the "glass must not stack" rule this codebase states at MultiViewScreen/VolumePicker.
    TvFocusable(onClick = onClick, shape = shape) { _, _ ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .glassSurface(shape)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = entry.glyph, style = AreIptvTheme.typography.h2, color = colors.textPrimary)
            Text(text = entry.name, style = AreIptvTheme.typography.h3, color = colors.textPrimary, maxLines = 1)
            Text(
                text = stringResource(R.string.explore_channels_approx, entry.channels),
                style = AreIptvTheme.typography.caption,
                color = colors.textTertiary,
            )
        }
    }
}

/** Name-before-add. Pre-filled with the entry name so OK-OK is the fast path, but it's the user's. */
@Composable
private fun ExploreNameDialog(entry: ExploreEntry, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    val colors = AreIptvTheme.colors
    var draft by remember(entry.id) { mutableStateOf(entry.name) }
    val addFocus = remember { FocusRequester() }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        AreDialog(
            onDismiss = onDismiss,
            title = stringResource(R.string.explore_add_title, entry.name),
            actions = {
                AreButton(stringResource(R.string.action_cancel), onClick = onDismiss, variant = AreButtonVariant.Ghost)
                AreButton(
                    stringResource(R.string.explore_add_action),
                    onClick = { onConfirm(draft) },
                    disabled = draft.isBlank(),
                    modifier = Modifier.focusRequester(addFocus),
                )
            },
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = stringResource(R.string.explore_add_body),
                    style = AreIptvTheme.typography.body,
                    color = colors.textSecondary,
                )
                // Same explicit down-target as the rename dialog: the field owns the D-pad while
                // editing, so without this you could type a name and never reach Add.
                AreTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    label = stringResource(R.string.explore_name_label),
                    activateOnClick = true,
                    modifier = Modifier.focusProperties { down = addFocus },
                )
            }
        }
    }
}
