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
    // Non-null while the hold-OK delete confirmation is open for that playlist.
    var pendingDelete by remember { mutableStateOf<PlaylistSource?>(null) }

    // verticalScroll + contentAlignment.Center on the Box: a Box centers a child shorter than the
    // viewport (so the block sits mid-screen, never top-clipped) and, once the playlist list makes
    // the content taller than the viewport, the scroll kicks in and the top stays reachable.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgBase)
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 40.dp, vertical = 56.dp),
        ) {
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
            Box(Modifier.height(36.dp))

            Column(
                modifier = Modifier.widthIn(max = 560.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                sources.forEachIndexed { index, source ->
                    val label = "${source.name}  ·  ${if (source.type == SourceType.XTREAM) xtreamLabel else m3uLabel}"
                    AreButton(
                        text = label,
                        onClick = { viewModel.select(source.id, onSelected) },
                        variant = AreButtonVariant.Secondary,
                        size = AreButtonSize.Large,
                        full = true,
                        onLongClick = { pendingDelete = source },
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
    }

    // Land D-pad focus on the first playlist as soon as the list is populated.
    androidx.compose.runtime.LaunchedEffect(sources.isNotEmpty()) {
        runCatching { firstItemFocus.requestFocus() }
    }

    // Hold-OK delete confirmation. AreDialog is only the visual scrim+card, so it's wrapped in a real
    // Dialog to pop above the screen (same pattern as HomeAddSectionDialog). Cancel is listed first so
    // the platform's default focus lands on it, not the destructive Delete.
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
