package com.arashrahimi46.iptv.mobile.ui.explore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arashrahimi46.iptv.core.R as CoreR
import com.arashrahimi46.iptv.mobile.data.parser.ExploreEntry
import com.arashrahimi46.iptv.mobile.data.parser.MAX_PLAYLISTS
import com.arashrahimi46.iptv.mobile.design.AreIptvTheme
import com.arashrahimi46.iptv.mobile.ui.components.AreAlertDialog
import com.arashrahimi46.iptv.mobile.ui.components.AreEmptyState
import com.arashrahimi46.iptv.mobile.ui.components.AreLoadingState
import com.arashrahimi46.iptv.mobile.ui.components.AreScreenScaffold
import com.arashrahimi46.iptv.mobile.ui.components.AreTextField
import com.arashrahimi46.iptv.mobile.ui.components.areTouch

/**
 * The user asked for the Explore catalogue's entry points to be hidden in the phone app. This screen
 * and its nav route stay wired, so flipping this back to `true` restores the feature -- it is the
 * only switch, and both entry points (onboarding and Sources) read it, so there is no second copy to
 * keep in step.
 */
internal const val EXPLORE_ENABLED = false

/**
 * The curated Explore catalogue: publicly listed free playlists, so that adding a playlist doesn't
 * require already having a URL from a provider. Mirrors the logic of :tv's `ExploreScreen` -- picking
 * an entry imports it as an ordinary M3U source (see [ExploreViewModel.add]) and there is no
 * separate browse or playback path, which is the whole reason this feature is small.
 *
 * Reachable from onboarding (a fresh user with no subscription can get content) and from
 * [com.arashrahimi46.iptv.mobile.ui.sources.SourcesScreen].
 *
 * @param onAdded invoked with the new source id once an import finishes and it's been made active.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    onAdded: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: ExploreViewModel = viewModel(),
) {
    val entries by viewModel.entries.collectAsState()
    val status by viewModel.status.collectAsState()
    val playlistCount by viewModel.playlistCount.collectAsState()
    val isAtCap by viewModel.isAtCap.collectAsState()
    val colors = AreIptvTheme.colors

    // The entry awaiting a name, or null. Held here rather than in the ViewModel because it's pure
    // screen state -- nothing outside this composable cares which card is mid-dialog.
    var naming by remember { mutableStateOf<ExploreEntry?>(null) }
    var showCap by remember { mutableStateOf(false) }

    AreScreenScaffold(title = stringResource(CoreR.string.explore_title), onBack = onBack) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(160.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 12.dp,
                bottom = padding.calculateBottomPadding() + 32.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }, contentType = "intro") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    // States plainly that these are third-party streams we neither host nor control
                    // -- the same position the Terms take, said where the user actually is.
                    Text(
                        text = stringResource(CoreR.string.explore_subtitle),
                        style = AreIptvTheme.typography.body,
                        color = colors.textSecondary,
                    )
                    Text(
                        text = stringResource(CoreR.string.explore_used_count, playlistCount, MAX_PLAYLISTS),
                        style = AreIptvTheme.typography.caption,
                        color = if (isAtCap) colors.danger else colors.textTertiary,
                    )
                }
            }

            items(entries, key = { it.id }, contentType = { "entry" }) { entry ->
                ExploreCard(
                    entry = entry,
                    // At the cap the cards stay live and explain why, rather than going dead: a tile
                    // that silently does nothing reads as a broken app.
                    onClick = { if (isAtCap) showCap = true else naming = entry },
                )
            }

            if (entries.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }, contentType = "empty") {
                    AreEmptyState(
                        title = stringResource(CoreR.string.explore_title),
                        message = stringResource(CoreR.string.explore_hint),
                    )
                }
            } else {
                item(span = { GridItemSpan(maxLineSpan) }, contentType = "hint") {
                    Text(
                        text = stringResource(CoreR.string.explore_hint),
                        style = AreIptvTheme.typography.caption,
                        color = colors.textTertiary,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
    }

    naming?.let { entry ->
        ExploreNameDialog(
            entry = entry,
            onDismiss = { naming = null },
            onConfirm = { name -> naming = null; viewModel.add(entry, name, onAdded) },
        )
    }

    // Visibility is driven by `status`, not by local state, so the empty onDismiss makes back and
    // scrim taps no-ops for the duration of the import instead of hiding a running job.
    (status as? ExploreStatus.Adding)?.let { adding ->
        AreAlertDialog(
            onDismiss = {},
            title = stringResource(CoreR.string.explore_adding_title),
            text = stringResource(CoreR.string.explore_adding_body, adding.entry.name),
            content = { AreLoadingState() },
        )
    }

    (status as? ExploreStatus.Failed)?.let { failed ->
        AreAlertDialog(
            onDismiss = viewModel::dismissError,
            title = stringResource(CoreR.string.explore_failed_title),
            // Names the playlist, so a dead curated URL reads as "this list is broken" rather than
            // "your connection is" -- the accepted cost of a bundled catalogue.
            text = stringResource(CoreR.string.explore_failed_body, failed.entry.name),
            confirmLabel = stringResource(CoreR.string.action_close),
            onConfirm = viewModel::dismissError,
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

@Composable
private fun ExploreCard(entry: ExploreEntry, onClick: () -> Unit) {
    val colors = AreIptvTheme.colors
    val shape = RoundedCornerShape(AreIptvTheme.radius.lg)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .areTouch(
                onClick = onClick,
                shape = shape,
                // Near-white card on the off-white page needs the border edge to read at all in
                // light theme; the fill alone disappears.
                backgroundColor = colors.surface1,
                borderColor = colors.borderDefault,
                // The card is well over 48dp on both axes already.
                minTouchTarget = 0.dp,
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(text = entry.glyph, style = AreIptvTheme.typography.h2, color = colors.textPrimary)
        Text(
            text = entry.name,
            style = AreIptvTheme.typography.h3,
            color = colors.textPrimary,
            maxLines = 2,
        )
        Text(
            text = stringResource(CoreR.string.explore_channels_approx, entry.channels),
            style = AreIptvTheme.typography.caption,
            color = colors.textTertiary,
        )
    }
}

/** Name-before-add, pre-filled with the entry name so add-add is the fast path, but it's the user's. */
@Composable
private fun ExploreNameDialog(entry: ExploreEntry, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var draft by remember(entry.id) { mutableStateOf(entry.name) }
    val blank = draft.isBlank()
    AreAlertDialog(
        onDismiss = onDismiss,
        title = stringResource(CoreR.string.explore_add_title, entry.name),
        text = stringResource(CoreR.string.explore_add_body),
        confirmLabel = stringResource(CoreR.string.explore_add_action),
        onConfirm = { if (!blank) onConfirm(draft) },
        dismissLabel = stringResource(CoreR.string.action_cancel),
        content = {
            AreTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier.fillMaxWidth(),
                label = stringResource(CoreR.string.explore_name_label),
                isError = blank,
                supportingText = if (blank) stringResource(CoreR.string.onboarding_invalid_name) else null,
            )
        },
    )
}
