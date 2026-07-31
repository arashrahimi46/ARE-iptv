package com.arashrahimi46.iptv.mobile.ui.streams

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arashrahimi46.iptv.core.R as CoreR
import com.arashrahimi46.iptv.data.model.DirectStream
import com.arashrahimi46.iptv.data.model.directStreamLabel
import com.arashrahimi46.iptv.mobile.ui.components.AreBottomSheet
import com.arashrahimi46.iptv.mobile.ui.components.AreButton
import com.arashrahimi46.iptv.mobile.ui.components.AreButtonVariant
import com.arashrahimi46.iptv.mobile.ui.components.AreEmptyState
import com.arashrahimi46.iptv.mobile.ui.components.AreIconButton
import com.arashrahimi46.iptv.mobile.ui.components.AreIconButtonSize
import com.arashrahimi46.iptv.mobile.ui.components.AreIconButtonVariant
import com.arashrahimi46.iptv.mobile.ui.components.AreListRow
import com.arashrahimi46.iptv.mobile.ui.components.AreScreenScaffold
import com.arashrahimi46.iptv.mobile.ui.components.AreSwipeToDismissRow
import com.arashrahimi46.iptv.mobile.ui.components.AreTextField
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.theme.glassWell
import kotlinx.coroutines.launch

/**
 * Touch-first "Open network stream" screen: paste a direct video URL and play it, plus the play
 * history -- same [StreamsViewModel]/[DirectStream] dedup-by-URL pipeline as before.
 *
 * Phone mechanics: the URL field has a paste affordance and an IME "Go" that opens the stream,
 * history rows swipe away with a deferred delete + Undo snackbar, and rename lives in a bottom
 * sheet behind an overflow button rather than a modal dialog.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamsScreen(
    onPlay: (Long) -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: StreamsViewModel = viewModel(),
) {
    val streams by viewModel.streams.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val layoutDirection = LocalLayoutDirection.current

    var url by rememberSaveable { mutableStateOf("") }
    var menuTarget by remember { mutableStateOf<DirectStream?>(null) }
    var renameTarget by remember { mutableStateOf<DirectStream?>(null) }
    val pendingDelete = remember { mutableStateListOf<Long>() }

    val undoLabel = stringResource(CoreR.string.common_undo)
    val deletedLabel = stringResource(CoreR.string.common_deleted)
    val deleteLabel = stringResource(CoreR.string.action_delete)
    val playLabel = stringResource(CoreR.string.detail_play)
    val moreLabel = stringResource(CoreR.string.common_more)

    val visible = streams.filter { it.id !in pendingDelete }

    val open: () -> Unit = {
        keyboard?.hide()
        viewModel.openStream(url) { id ->
            url = ""
            onPlay(id)
        }
    }

    val requestDelete: (DirectStream) -> Unit = { stream ->
        pendingDelete.add(stream.id)
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = deletedLabel,
                actionLabel = undoLabel,
                duration = SnackbarDuration.Short,
            )
            pendingDelete.remove(stream.id)
            if (result != SnackbarResult.ActionPerformed) viewModel.delete(stream)
        }
    }

    AreScreenScaffold(
        title = stringResource(CoreR.string.streams_open),
        onBack = onBack,
        snackbarHostState = snackbarHostState,
    ) { insets ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = insets.calculateTopPadding())
                .imePadding(),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AreTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = stringResource(CoreR.string.streams_open),
                    placeholder = stringResource(CoreR.string.streams_url_placeholder),
                    supportingText = stringResource(CoreR.string.streams_url_helper),
                    leadingIcon = Icons.Filled.Link,
                    trailingIcon = Icons.Filled.ContentPaste,
                    // The slot's default description is "Clear"; this one pastes the clipboard.
                    trailingIconContentDescription = stringResource(CoreR.string.common_paste),
                    onTrailingIconClick = {
                        clipboard.getText()?.text?.trim()?.takeIf { it.isNotEmpty() }?.let { url = it }
                    },
                    monospace = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Go,
                    ),
                    keyboardActions = KeyboardActions(onGo = { open() }),
                )
                AreButton(
                    text = stringResource(CoreR.string.streams_open),
                    onClick = open,
                    variant = AreButtonVariant.Primary,
                    full = true,
                    enabled = url.isNotBlank(),
                )
            }

            if (visible.isEmpty()) {
                AreEmptyState(
                    title = stringResource(CoreR.string.streams_open),
                    message = stringResource(CoreR.string.streams_empty),
                    icon = Icons.Filled.Link,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = 4.dp,
                        bottom = insets.calculateBottomPadding() + 40.dp,
                        start = insets.calculateStartPadding(layoutDirection),
                        end = insets.calculateEndPadding(layoutDirection),
                    ),
                ) {
                    items(items = visible, key = { it.id }, contentType = { "stream" }) { stream ->
                        AreSwipeToDismissRow(
                            onDismissed = { requestDelete(stream) },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            icon = Icons.Filled.Delete,
                            label = deleteLabel,
                        ) {
                            StreamCard(
                                stream = stream,
                                playLabel = playLabel,
                                moreLabel = moreLabel,
                                onPlay = { onPlay(stream.id) },
                                onMore = { menuTarget = stream },
                            )
                        }
                    }
                }
            }
        }
    }

    // Overflow: the only non-destructive per-row action left is Rename (delete is the swipe).
    val menu = menuTarget
    if (menu != null) {
        AreBottomSheet(
            onDismiss = { menuTarget = null },
            title = directStreamLabel(menu.url, menu.name),
        ) {
            AreListRow(
                title = stringResource(CoreR.string.action_rename),
                leadingIcon = Icons.Filled.DriveFileRenameOutline,
                onClick = {
                    menuTarget = null
                    renameTarget = menu
                },
                trailing = {},
            )
        }
    }

    val rename = renameTarget
    if (rename != null) {
        RenameSheet(
            initial = rename.name.orEmpty(),
            onDismiss = { renameTarget = null },
            onConfirm = { name ->
                viewModel.rename(rename.id, name)
                renameTarget = null
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StreamCard(
    stream: DirectStream,
    playLabel: String,
    moreLabel: String,
    onPlay: () -> Unit,
    onMore: () -> Unit,
) {
    val shape = RoundedCornerShape(AreIptvTheme.radius.md)
    Column(Modifier.fillMaxWidth().glassWell(shape)) {
        AreListRow(
            title = directStreamLabel(stream.url, stream.name),
            supporting = stream.url,
            onClick = onPlay,
            trailing = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AreIconButton(
                        icon = Icons.Filled.PlayArrow,
                        contentDescription = playLabel,
                        onClick = onPlay,
                        variant = AreIconButtonVariant.Glass,
                        size = AreIconButtonSize.Small,
                    )
                    AreIconButton(
                        icon = Icons.Filled.MoreVert,
                        contentDescription = moreLabel,
                        onClick = onMore,
                        variant = AreIconButtonVariant.Ghost,
                        size = AreIconButtonSize.Small,
                    )
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RenameSheet(initial: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf(initial) }
    AreBottomSheet(
        onDismiss = onDismiss,
        title = stringResource(CoreR.string.action_rename),
        actions = {
            AreButton(
                text = stringResource(CoreR.string.action_cancel),
                onClick = onDismiss,
                variant = AreButtonVariant.Secondary,
            )
            AreButton(
                text = stringResource(CoreR.string.action_save),
                onClick = { onConfirm(name) },
                variant = AreButtonVariant.Primary,
            )
        },
    ) {
        AreTextField(
            value = name,
            onValueChange = { name = it },
            placeholder = stringResource(CoreR.string.streams_rename_placeholder),
            modifier = Modifier.padding(horizontal = 20.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onConfirm(name) }),
        )
    }
}
