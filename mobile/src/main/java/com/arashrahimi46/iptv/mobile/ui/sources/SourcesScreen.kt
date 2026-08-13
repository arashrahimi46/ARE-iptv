package com.arashrahimi46.iptv.mobile.ui.sources

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arashrahimi46.iptv.core.R as CoreR
import com.arashrahimi46.iptv.mobile.data.model.PlaylistSource
import com.arashrahimi46.iptv.mobile.data.model.SourceType
import com.arashrahimi46.iptv.mobile.data.parser.MAX_PLAYLISTS
import com.arashrahimi46.iptv.mobile.design.AreIptvTheme
import com.arashrahimi46.iptv.mobile.ui.components.AreAlertDialog
import com.arashrahimi46.iptv.mobile.ui.components.AreBottomSheet
import com.arashrahimi46.iptv.mobile.ui.components.AreIconButton
import com.arashrahimi46.iptv.mobile.ui.components.AreListRow
import com.arashrahimi46.iptv.mobile.ui.components.AreScreenScaffold
import com.arashrahimi46.iptv.mobile.ui.components.AreSectionHeader
import com.arashrahimi46.iptv.mobile.ui.components.AreTextField
import com.arashrahimi46.iptv.mobile.ui.explore.EXPLORE_ENABLED

/**
 * Multi-playlist management: list every added playlist, switch which one the app is showing, add
 * another, rename or delete one. Mirrors the logic of :tv's `SelectSourceScreen` (same repository
 * and [com.arashrahimi46.iptv.mobile.data.settings.UserSettings] calls) with touch mechanics
 * instead of D-pad ones.
 *
 * Two deliberate divergences from :tv:
 * - :tv hides rename/delete behind hold-OK, because a remote has no third gesture. Here each row
 *   carries an explicit overflow button opening a sheet -- a long-press is undiscoverable on a
 *   phone, and tapping the row itself has to stay the switch action.
 * - This is a Settings sub-screen, not a startup gate, so it never has to leave with a source
 *   selected; switching is immediate and the user stays put.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourcesScreen(
    onAddNew: () -> Unit,
    onOpenExplore: () -> Unit,
    onBack: () -> Unit,
    viewModel: SourcesViewModel = viewModel(),
) {
    val sources by viewModel.sources.collectAsState()
    val activeSourceId by viewModel.activeSourceId.collectAsState()
    val isAtCap by viewModel.isAtCap.collectAsState()
    val colors = AreIptvTheme.colors

    // At most one of these is non-null at a time -- the three steps of the overflow flow:
    // options sheet -> rename dialog OR delete confirmation.
    var pendingOptions by remember { mutableStateOf<PlaylistSource?>(null) }
    var pendingRename by remember { mutableStateOf<PlaylistSource?>(null) }
    var pendingDelete by remember { mutableStateOf<PlaylistSource?>(null) }
    var showCap by remember { mutableStateOf(false) }

    AreScreenScaffold(title = stringResource(CoreR.string.sources_choose_title), onBack = onBack) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 32.dp,
            ),
        ) {
            item(contentType = "intro") {
                Text(
                    text = stringResource(CoreR.string.sources_choose_subtitle),
                    style = AreIptvTheme.typography.body,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
                )
                Text(
                    text = stringResource(CoreR.string.explore_used_count, sources.size, MAX_PLAYLISTS),
                    style = AreIptvTheme.typography.caption,
                    color = if (isAtCap) colors.danger else colors.textTertiary,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp),
                )
            }

            items(sources, key = { it.id }, contentType = { "source" }) { source ->
                val isActive = source.id == activeSourceId
                AreListRow(
                    title = source.name,
                    // Selection is announced by the platform off `selected` semantics, so the check
                    // glyph can stay decorative and no new string is needed to label it.
                    modifier = Modifier.semantics { selected = isActive },
                    supporting = sourceSubtitle(source),
                    onClick = { viewModel.select(source.id) },
                    trailing = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isActive) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = colors.accent,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                            AreIconButton(
                                icon = Icons.Filled.MoreVert,
                                contentDescription = stringResource(CoreR.string.common_more),
                                onClick = { pendingOptions = source },
                            )
                        }
                    },
                )
            }

            item(contentType = "header") {
                AreSectionHeader(stringResource(CoreR.string.settings_section_playlists))
            }
            item(contentType = "row") {
                AreListRow(
                    title = stringResource(CoreR.string.sources_add_new_playlist),
                    leadingIcon = Icons.Filled.Add,
                    // At the cap the row stays live and explains why, rather than going dead: a row
                    // that silently does nothing reads as a broken app (same call :tv's Explore makes).
                    onClick = { if (isAtCap) showCap = true else onAddNew() },
                )
            }
            // Hidden, not disabled: a dead row with no explanation reads worse than no row at all.
            if (EXPLORE_ENABLED) {
                item(contentType = "row") {
                    AreListRow(
                        title = stringResource(CoreR.string.explore_link),
                        // No subtitle: the "we don't host these streams" disclaimer is a paragraph, and
                        // it belongs on the Explore screen itself rather than squeezed into a row.
                        leadingIcon = Icons.Filled.Explore,
                        onClick = { if (isAtCap) showCap = true else onOpenExplore() },
                    )
                }
            }
        }
    }

    pendingOptions?.let { target ->
        AreBottomSheet(onDismiss = { pendingOptions = null }, title = target.name) {
            Text(
                text = stringResource(CoreR.string.sources_options_body),
                style = AreIptvTheme.typography.body,
                color = colors.textSecondary,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            )
            AreListRow(
                title = stringResource(CoreR.string.action_rename),
                leadingIcon = Icons.Filled.DriveFileRenameOutline,
                onClick = { pendingRename = target; pendingOptions = null },
            )
            AreListRow(
                title = stringResource(CoreR.string.action_delete),
                leadingIcon = Icons.Filled.Delete,
                onClick = { pendingDelete = target; pendingOptions = null },
            )
        }
    }

    // Rename. Name-only by design: the URL and credentials define the catalog, so changing them
    // would mean re-auth + a full re-import -- a separate flow, not an edit of this row.
    pendingRename?.let { target ->
        var draft by remember(target.id) { mutableStateOf(target.name) }
        val blank = draft.isBlank()
        AreAlertDialog(
            onDismiss = { pendingRename = null },
            title = stringResource(CoreR.string.sources_rename_title),
            text = stringResource(CoreR.string.sources_rename_body),
            confirmLabel = stringResource(CoreR.string.action_save),
            // The repository ignores a blank name, so Save can't corrupt the row; the field carries
            // the reason nothing happened instead of the button going dead under the user's thumb.
            onConfirm = { if (!blank) { viewModel.rename(target.id, draft); pendingRename = null } },
            dismissLabel = stringResource(CoreR.string.action_cancel),
            content = {
                AreTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = stringResource(CoreR.string.sources_rename_label),
                    isError = blank,
                    supportingText = if (blank) stringResource(CoreR.string.onboarding_invalid_name) else null,
                )
            },
        )
    }

    pendingDelete?.let { target ->
        AreAlertDialog(
            onDismiss = { pendingDelete = null },
            title = stringResource(CoreR.string.sources_delete_title),
            text = stringResource(CoreR.string.sources_delete_body, target.name),
            confirmLabel = stringResource(CoreR.string.action_delete),
            onConfirm = { viewModel.delete(target.id); pendingDelete = null },
            dismissLabel = stringResource(CoreR.string.action_cancel),
            destructive = true,
        )
    }

    if (showCap) {
        AreAlertDialog(
            onDismiss = { showCap = false },
            title = stringResource(CoreR.string.explore_cap_title, MAX_PLAYLISTS),
            text = stringResource(CoreR.string.explore_cap_body),
            confirmLabel = stringResource(CoreR.string.action_close),
            onConfirm = { showCap = false },
        )
    }
}

/** "Xtream" or "M3U · From Explore" -- provenance, not decoration: an Explore playlist can break on
 *  its own, and the badge survives a rename because it keys off [PlaylistSource.origin]. */
@Composable
private fun sourceSubtitle(source: PlaylistSource): String {
    val type = stringResource(
        when (source.type) {
            SourceType.XTREAM -> CoreR.string.sources_type_xtream
            SourceType.M3U -> CoreR.string.sources_type_m3u
            SourceType.STALKER -> CoreR.string.sources_type_stalker
        },
    )
    if (source.origin == null) return type
    return "$type  ·  ${stringResource(CoreR.string.sources_from_explore)}"
}
