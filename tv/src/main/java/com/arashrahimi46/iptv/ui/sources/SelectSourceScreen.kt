package com.arashrahimi46.iptv.ui.sources

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.arashrahimi46.iptv.R
import com.arashrahimi46.iptv.data.model.PlaylistSource
import com.arashrahimi46.iptv.data.model.SourceType
import com.arashrahimi46.iptv.ui.components.AreButton
import com.arashrahimi46.iptv.ui.components.AreButtonSize
import com.arashrahimi46.iptv.ui.components.AreButtonVariant
import com.arashrahimi46.iptv.ui.components.AreDialog
import com.arashrahimi46.iptv.ui.components.AreTextField
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme

/**
 * Startup playlist picker. Shown whenever at least one [com.arashrahimi46.iptv.data.model.PlaylistSource]
 * exists (see MainActivity's start-destination logic): the user always chooses
 * which added playlist to enter before the shell loads. Previously the app
 * auto-activated the source on add and jumped straight to the shell, so the
 * added playlists were never listed anywhere.
 *
 * @param onSelected invoked after the picked source has been marked active.
 * @param onAddNew   opens the onboarding wizard to add another playlist.
 */
@Composable
fun SelectSourceScreen(onSelected: () -> Unit, onAddNew: () -> Unit) {
    val context = LocalContext.current
    val viewModel: SelectSourceViewModel = viewModel(
        factory = SelectSourceViewModel.factory(context.applicationContext as android.app.Application),
    )
    val sources by viewModel.sources.collectAsState()
    val colors = AreIptvTheme.colors
    val firstItemFocus = remember { FocusRequester() }
    val xtreamLabel = stringResource(R.string.sources_type_xtream)
    val m3uLabel = stringResource(R.string.sources_type_m3u)
    val stalkerLabel = stringResource(R.string.sources_type_stalker)
    val exploreLabel = stringResource(R.string.sources_from_explore)
    // At most one of these is non-null at a time -- they are the three steps of the hold-OK flow:
    // options menu -> rename dialog OR delete confirmation.
    var pendingOptions by remember { mutableStateOf<PlaylistSource?>(null) }
    var pendingRename by remember { mutableStateOf<PlaylistSource?>(null) }
    var pendingDelete by remember { mutableStateOf<PlaylistSource?>(null) }

    // Two-pane landscape layout: a fixed header on the left, the playlist list scrolling on the
    // right. Keeping the header out of the scroll path means a long list can never push the brand
    // and title off the top of the screen (the old single-column scroll clipped them on focus).
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgBase)
            .padding(horizontal = 56.dp, vertical = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(48.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(colors.accent, RoundedCornerShape(AreIptvTheme.radius.sm)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "A", style = AreIptvTheme.typography.h2, color = colors.accentFg)
                }
                Text(text = stringResource(R.string.brand_name), style = AreIptvTheme.typography.h2, color = colors.textPrimary)
            }
            Box(Modifier.height(18.dp))
            Text(text = stringResource(R.string.sources_choose_title), style = AreIptvTheme.typography.display, color = colors.textPrimary)
            Box(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.sources_choose_subtitle),
                style = AreIptvTheme.typography.body,
                color = colors.textSecondary,
            )
            if (sources.isNotEmpty()) {
                Box(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.sources_delete_hint),
                    style = AreIptvTheme.typography.caption,
                    color = colors.textTertiary,
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .widthIn(max = 560.dp)
                .verticalScroll(rememberScrollState())
                // Inset the content within the scroll's clip bounds so the first/last item's focus
                // ring and glow have room to draw instead of being clipped at the top/side edges.
                .padding(vertical = 16.dp, horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            sources.forEachIndexed { index, source ->
                val typeLabel = when (source.type) {
                    SourceType.XTREAM -> xtreamLabel
                    SourceType.STALKER -> stalkerLabel
                    SourceType.M3U -> m3uLabel
                }
                // Provenance, not decoration: it tells the user this list came from our curated
                // catalogue rather than something they typed, which is why it can break on its own.
                val label = if (source.origin != null) {
                    "${source.name}  ·  $typeLabel  ·  $exploreLabel"
                } else {
                    "${source.name}  ·  $typeLabel"
                }
                AreButton(
                    text = label,
                    onClick = { viewModel.select(source.id, onSelected) },
                    variant = AreButtonVariant.Secondary,
                    size = AreButtonSize.Large,
                    full = true,
                    onLongClick = { pendingOptions = source },
                    modifier = if (index == 0) Modifier.focusRequester(firstItemFocus) else Modifier,
                )
            }

            Box(Modifier.height(8.dp))
            AreButton(
                text = stringResource(R.string.sources_add_new_playlist),
                onClick = onAddNew,
                variant = AreButtonVariant.Ghost,
                size = AreButtonSize.Large,
                full = true,
                modifier = if (sources.isEmpty()) Modifier.focusRequester(firstItemFocus) else Modifier,
            )
        }
    }

    // Land D-pad focus on the first playlist as soon as the list is populated.
    androidx.compose.runtime.LaunchedEffect(sources.isNotEmpty()) {
        runCatching { firstItemFocus.requestFocus() }
    }

    // Hold-OK options menu. Hold used to delete outright; it now offers the choice, which also means
    // a stray long-press can no longer start a destructive flow. Rename is listed first so the
    // platform's default focus lands on the safe action.
    pendingOptions?.let { target ->
        Dialog(
            onDismissRequest = { pendingOptions = null },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            AreDialog(
                onDismiss = { pendingOptions = null },
                title = target.name,
                actions = {
                    AreButton(
                        stringResource(R.string.action_rename),
                        onClick = { pendingRename = target; pendingOptions = null },
                        variant = AreButtonVariant.Secondary,
                    )
                    AreButton(
                        stringResource(R.string.action_delete),
                        onClick = { pendingDelete = target; pendingOptions = null },
                        variant = AreButtonVariant.Danger,
                    )
                },
            ) {
                Text(
                    text = stringResource(R.string.sources_options_body),
                    style = AreIptvTheme.typography.body,
                    color = colors.textSecondary,
                )
            }
        }
    }

    // Rename. Name-only by design: the URL and credentials define the catalog, so changing them
    // would mean re-auth + a full re-import -- a separate flow, not an edit of this row.
    pendingRename?.let { target ->
        var draft by remember(target.id) { mutableStateOf(target.name) }
        val saveFocus = remember { FocusRequester() }
        Dialog(
            onDismissRequest = { pendingRename = null },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            AreDialog(
                onDismiss = { pendingRename = null },
                title = stringResource(R.string.sources_rename_title),
                actions = {
                    AreButton(stringResource(R.string.action_cancel), onClick = { pendingRename = null }, variant = AreButtonVariant.Ghost)
                    AreButton(
                        stringResource(R.string.action_save),
                        onClick = { viewModel.rename(target.id, draft); pendingRename = null },
                        disabled = draft.isBlank(),
                        modifier = Modifier.focusRequester(saveFocus),
                    )
                },
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = stringResource(R.string.sources_rename_body),
                        style = AreIptvTheme.typography.body,
                        color = colors.textSecondary,
                    )
                    // activateOnClick keeps the IME from auto-popping (OK enters edit, Done/Back
                    // leaves). The explicit `down` target pins the one path that must never break:
                    // typing a name and being unable to reach Save would strand the user mid-edit.
                    AreTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        label = stringResource(R.string.sources_rename_label),
                        activateOnClick = true,
                        modifier = Modifier.focusProperties { down = saveFocus },
                    )
                }
            }
        }
    }

    // Delete confirmation, now reached from the options menu rather than straight from the hold.
    // AreDialog is only the visual scrim+card, so it's wrapped in a real Dialog to pop above the
    // screen (same pattern as HomeAddSectionDialog). Cancel is listed first so the platform's
    // default focus lands on it, not the destructive Delete.
    pendingDelete?.let { target ->
        Dialog(
            onDismissRequest = { pendingDelete = null },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            AreDialog(
                onDismiss = { pendingDelete = null },
                title = stringResource(R.string.sources_delete_title),
                actions = {
                    AreButton(stringResource(R.string.action_cancel), onClick = { pendingDelete = null }, variant = AreButtonVariant.Ghost)
                    AreButton(
                        stringResource(R.string.action_delete),
                        onClick = { viewModel.delete(target.id); pendingDelete = null },
                        variant = AreButtonVariant.Danger,
                    )
                },
            ) {
                Text(
                    text = stringResource(R.string.sources_delete_body, target.name),
                    style = AreIptvTheme.typography.body,
                    color = colors.textSecondary,
                )
            }
        }
    }
}
