package com.arashrahimi46.iptv.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.arashrahimi46.iptv.core.R as CoreR
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.theme.ProvideOnGlass
import com.arashrahimi46.iptv.ui.theme.glassSurface

/** The drag affordance every sheet carries. */
@Composable
internal fun AreSheetHandle() {
    Box(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .width(36.dp)
                .heightIn(min = 4.dp, max = 4.dp)
                .background(AreIptvTheme.colors.textTertiary, RoundedCornerShape(AreIptvTheme.radius.pill)),
        )
    }
}

/**
 * The app's modal sheet. Scrim tap and drag-down both dismiss -- which the TV `AreDialog` never
 * allowed, and which is the whole reason sheets replace it for anything non-destructive.
 *
 * Insets are the sheet's own: `windowInsets = WindowInsets(0)` plus [navigationBarsPadding] on the
 * body, so the content clears the gesture bar without the sheet floating above it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AreBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
    actions: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(topStart = AreIptvTheme.radius.xl, topEnd = AreIptvTheme.radius.xl)
    val maxHeight = LocalConfiguration.current.screenHeightDp.dp * 0.9f

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        contentColor = AreIptvTheme.colors.textPrimary,
        dragHandle = { AreSheetHandle() },
        contentWindowInsets = { WindowInsets(0) },
    ) {
        ProvideOnGlass(true) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxHeight)
                    .glassSurface(shape, elevated = true)
                    .navigationBarsPadding(),
            ) {
                if (title != null) {
                    Text(
                        text = title,
                        style = AreIptvTheme.typography.h3,
                        color = AreIptvTheme.colors.textPrimary,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 12.dp),
                    )
                }
                content()
                if (actions != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically,
                        content = actions,
                    )
                }
            }
        }
    }
}

/**
 * Single-choice list sheet -- the workhorse behind every Settings choice row. Picking a row calls
 * [onSelect] and THEN [onDismiss]; a picker that stays open after a choice reads as broken on touch.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> AreChoiceSheet(
    title: String,
    options: List<T>,
    selected: T?,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    supporting: (@Composable (T) -> String?)? = null,
    leading: (@Composable (T) -> Unit)? = null,
) {
    AreBottomSheet(onDismiss = onDismiss, modifier = modifier, title = title) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 12.dp),
        ) {
            // Keyed by the option's own identity, not its index: a choice list can change under an
            // open sheet (track lists arrive asynchronously), and index keys would then re-associate
            // saved row state onto a different option. `toString()` because T is unconstrained and a
            // lazy-list key must be a Saveable type -- an arbitrary T is not.
            items(options, key = { it.toString() }) { option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .areTouch(
                            onClick = {
                                onSelect(option)
                                onDismiss()
                            },
                            shape = RoundedCornerShape(0.dp),
                            role = androidx.compose.ui.semantics.Role.RadioButton,
                            minTouchTarget = 0.dp,
                            disableScale = true,
                        )
                        .heightIn(min = 56.dp)
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    if (leading != null) leading(option)
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = label(option),
                            style = AreIptvTheme.typography.body,
                            color = AreIptvTheme.colors.textPrimary,
                        )
                        val sub = supporting?.invoke(option)
                        if (sub != null) {
                            Text(
                                text = sub,
                                style = AreIptvTheme.typography.caption,
                                color = AreIptvTheme.colors.textSecondary,
                            )
                        }
                    }
                    if (option == selected) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            tint = AreIptvTheme.colors.accent,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
        }
    }
}

/** Long-press context sheet for a tile. Replaces the TV `AreTileActionDialog`. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AreTileActionSheet(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    isFavorite: Boolean? = null,
    onToggleFavorite: (() -> Unit)? = null,
    onPlay: (() -> Unit)? = null,
    onPlayFromStart: (() -> Unit)? = null,
    onOpenDetails: (() -> Unit)? = null,
) {
    AreBottomSheet(onDismiss = onDismiss, modifier = modifier, title = title) {
        Column(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
            if (onPlay != null) {
                AreListRow(
                    title = stringResource(CoreR.string.detail_play),
                    leadingIcon = Icons.Filled.PlayArrow,
                    onClick = { onPlay(); onDismiss() },
                    trailing = {},
                )
            }
            if (onPlayFromStart != null) {
                AreListRow(
                    title = stringResource(CoreR.string.tile_action_play_from_start),
                    leadingIcon = Icons.Filled.Replay,
                    onClick = { onPlayFromStart(); onDismiss() },
                    trailing = {},
                )
            }
            if (onToggleFavorite != null) {
                AreListRow(
                    title = if (isFavorite == true) {
                        stringResource(CoreR.string.detail_remove_from_favorites)
                    } else {
                        stringResource(CoreR.string.detail_add_to_favorites)
                    },
                    leadingIcon = if (isFavorite == true) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    onClick = { onToggleFavorite(); onDismiss() },
                    trailing = {},
                )
            }
            if (onOpenDetails != null) {
                AreListRow(
                    title = stringResource(CoreR.string.common_details),
                    leadingIcon = Icons.Filled.Info,
                    onClick = { onOpenDetails(); onDismiss() },
                    trailing = {},
                )
            }
        }
    }
}
