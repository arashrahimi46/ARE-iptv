package com.arashrahimi46.iptv.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.arashrahimi46.iptv.core.R
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme

/**
 * TileActionDialog — the hold-OK action menu for a movie/series/channel tile.
 *
 * Rendered in a real [Dialog] window (not an inline overlay) so D-pad focus is trapped in the
 * modal instead of leaking to the rail behind it, and focus is placed on the primary action on
 * open. Only the actions whose lambdas are non-null are rendered; favourite is always present.
 */
@Composable
fun AreTileActionDialog(
    title: String,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    /** Labelled "Resume" when [hasProgress], else "Play". Null hides the row entirely. */
    onPlay: (() -> Unit)? = null,
    /** Only offered alongside a resumable [onPlay] -- ignored when [hasProgress] is false. */
    onPlayFromStart: (() -> Unit)? = null,
    hasProgress: Boolean = false,
    onDismiss: () -> Unit,
) {
    val primaryFocus = remember { FocusRequester() }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
    ) {
        LaunchedEffect(Unit) { runCatching { primaryFocus.requestFocus() } }
        AreDialog(onDismiss = onDismiss, title = title, width = 460.dp) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (onPlay != null) {
                    AreButton(
                        text = stringResource(if (hasProgress) R.string.tile_action_resume else R.string.detail_play),
                        onClick = { onPlay(); onDismiss() },
                        icon = Icons.Filled.PlayArrow,
                        full = true,
                        modifier = Modifier.focusRequester(primaryFocus),
                    )
                    if (hasProgress && onPlayFromStart != null) {
                        AreButton(
                            text = stringResource(R.string.tile_action_play_from_start),
                            onClick = { onPlayFromStart(); onDismiss() },
                            icon = Icons.Filled.Replay,
                            variant = AreButtonVariant.Secondary,
                            full = true,
                        )
                    }
                }
                AreButton(
                    text = stringResource(
                        if (isFavorite) R.string.detail_remove_from_favorites else R.string.detail_add_to_favorites,
                    ),
                    onClick = { onToggleFavorite(); onDismiss() },
                    icon = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    variant = AreButtonVariant.Secondary,
                    full = true,
                    // Only the primary action takes focus on open; when there is no Play row the
                    // favourite row IS the primary action.
                    modifier = if (onPlay == null) Modifier.focusRequester(primaryFocus) else Modifier,
                )
                AreButton(
                    text = stringResource(R.string.action_cancel),
                    onClick = onDismiss,
                    variant = AreButtonVariant.Ghost,
                    full = true,
                )
            }
        }
    }
}

@Preview(widthDp = 900, heightDp = 600, showBackground = true)
@Composable
private fun AreTileActionDialogPreview() {
    AreIptvTheme {
        AreTileActionDialog(
            title = "Dune Part Two",
            isFavorite = false,
            onToggleFavorite = {},
            onPlay = {},
            onPlayFromStart = {},
            hasProgress = true,
            onDismiss = {},
        )
    }
}
