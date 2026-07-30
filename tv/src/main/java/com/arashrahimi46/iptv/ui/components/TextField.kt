package com.arashrahimi46.iptv.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.theme.glassWell
import com.arashrahimi46.iptv.ui.theme.requestFocusWhenReady
import com.arashrahimi46.iptv.ui.theme.tvFocusable

/**
 * TextField — labeled input (TextField.jsx). `mono` renders the value in the
 * monospace family for URLs / Xtream params so users can verify character-by-character.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AreTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    mono: Boolean = false,
    masked: Boolean = false,
    icon: ImageVector? = null,
    prefix: String? = null,
    helper: String? = null,
    error: String? = null,
    /** TV UX: when true the field is a focusable row that only OPENS the keyboard on OK/Select
     *  (and closes it on Back / IME "Done"). D-pad scrolling past it just highlights it -- it does
     *  NOT pop the IME, which is the annoyance when several fields sit in a scrolling settings list.
     *  Leave false for a field whose whole purpose is typing (e.g. Search), where auto-IME is wanted. */
    activateOnClick: Boolean = false,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val colors = AreIptvTheme.colors
    val shape = RoundedCornerShape(AreIptvTheme.radius.md)
    // Tap-to-edit state (only meaningful when [activateOnClick]). While NOT editing the row owns
    // the focusable and shows the value read-only; OK flips [editing] on, which moves focus into
    // the real BasicTextField below (that focus is what raises the IME).
    var editing by remember { mutableStateOf(false) }
    val rowFocus = remember { FocusRequester() }
    val fieldFocus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    if (activateOnClick) {
        var wasEditing by remember { mutableStateOf(false) }
        LaunchedEffect(editing) {
            val cameFromEditing = wasEditing
            // Recorded BEFORE the suspending focus handoff below, so a fast OK/Back pair (which
            // cancels this effect mid-flight) can't leave the flag describing the wrong state.
            wasEditing = editing
            when {
                // The BasicTextField is composed in the SAME recomposition that flips `editing`, so
                // its focus node is not attached yet on this frame -- a single un-retried
                // requestFocus() throws and the request is lost for good, which is how focus ended
                // up nowhere (and then on the tab row above). requestFocusWhenReady retries across
                // the swap; no re-assert, because by then the user may legitimately have moved on.
                editing -> {
                    // focus -> IME opens
                    if (!fieldFocus.requestFocusWhenReady(attempts = 12, gapMs = 16L, reassertAfterMs = 0L)) {
                        // Never leave the field claiming to be in edit mode with nothing focused:
                        // fall back to the row, which re-enters this effect down the `wasEditing`
                        // branch and hides any keyboard that did manage to open.
                        editing = false
                    }
                }
                cameFromEditing -> {
                    keyboard?.hide()
                    // land back on the row, not the top of the list
                    rowFocus.requestFocusWhenReady(attempts = 12, gapMs = 16L, reassertAfterMs = 0L)
                }
            }
        }
        // A hardware/software Back while editing is often consumed by the IME first (closing the
        // keyboard) without ever reaching Compose's key-input pipeline, so the onKeyEvent Back
        // handler on the BasicTextField below can miss the first press entirely -- the field is
        // still "editing" when the SECOND Back arrives, and that one lands on the host Dialog's
        // own OnBackPressedCallback (onDismissRequest), silently discarding whatever was typed.
        // BackHandler registers directly on the activity's OnBackPressedDispatcher, which is what
        // the IME's own back-consumption competes with -- registering here (nested inside the
        // Dialog's content) makes this callback the most-recently-added enabled one, so it wins
        // over the Dialog's callback on the very first Back that reaches the dispatcher at all,
        // regardless of whether the IME ate the immediately preceding one.
        androidx.activity.compose.BackHandler(enabled = editing) { editing = false }
    }

    // The resting glass edge is always visible (even unfocused); the focus ring/glow/scale on
    // top of it comes from the shared tvFocusable() primitive below, driven by the same
    // interactionSource that BasicTextField owns. Scale is disabled -- growing a text input
    // 1.06x on focus reflows sibling layout and moves the caret; the ring/glow alone is the
    // correct treatment for inputs per the design system's focus-visible rule.

    Column(modifier = modifier) {
        if (label != null) {
            Text(text = label, style = AreIptvTheme.typography.label, color = colors.textSecondary)
            Box(Modifier.height(8.dp))
        }
        // When [activateOnClick] and not editing, the ROW shows the value read-only and OK enters
        // edit mode; otherwise the BasicTextField renders (original always-editable behavior).
        val rowOwnsFocus = activateOnClick && !editing
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .then(if (activateOnClick) Modifier.focusRequester(rowFocus) else Modifier)
                .tvFocusable(
                    interactionSource = interactionSource,
                    shape = shape,
                    glowColor = if (error != null) colors.danger else colors.focusRing,
                    disableScale = true,
                    // In tap-to-edit mode the row is focusable ALWAYS, editing or not. It used to be
                    // `activateOnClick && !editing`, which removed the focus node that was holding
                    // focus in the very recomposition that composed the BasicTextField: for a frame
                    // nothing was focused, Compose cleared focus to the root, and the next resolution
                    // grabbed the first focusable in the shell's content group -- on Search, the
                    // segmented-control pills above the field. Keeping the row focusable means focus
                    // never leaves this subtree; it just moves down into the field and back out.
                    // (The always-editable variant still lets the BasicTextField own the only focus
                    // node -- see the `ownsFocusable` docs on tvFocusable.)
                    ownsFocusable = activateOnClick,
                )
                .then(
                    // Structurally unconditional for the lifetime of the field (`activateOnClick` is
                    // fixed per call site) -- gating this on `editing` would change the modifier
                    // chain across the very transition we are trying to keep stable.
                    if (activateOnClick) Modifier.onKeyEvent { ev ->
                        val isSelect = ev.key == Key.DirectionCenter || ev.key == Key.Enter || ev.key == Key.NumPadEnter
                        when {
                            editing -> false // the field itself owns keys while editing
                            isSelect && ev.type == KeyEventType.KeyUp -> { editing = true; true }
                            // Swallow the SELECT KeyDown so the platform doesn't also synthesize a click.
                            isSelect && ev.type == KeyEventType.KeyDown -> true
                            else -> false
                        }
                    } else Modifier,
                )
                // V2 §6.1: a field is RECESSED glass (glassWell -- darker fill, inner top shadow,
                // hairline lit along the bottom), not a raised control. glassTrack made it
                // translucent but it still read as a flat slab: on a dark page a lit TOP edge says
                // "sits on top of the glass", which is the opposite of what an input is. The
                // danger edge still overrides the lit edge when there's a validation error.
                .glassWell(shape)
                .then(if (error != null) Modifier.border(1.dp, colors.danger, shape) else Modifier)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = colors.textTertiary, modifier = Modifier.size(20.dp))
            }
            if (prefix != null) {
                Text(text = prefix, style = AreIptvTheme.typography.mono, color = colors.textTertiary)
            }
            Box(Modifier.weight(1f)) {
                val textStyle = if (mono) AreIptvTheme.typography.mono else AreIptvTheme.typography.body
                if (rowOwnsFocus) {
                    // Read-only display while highlighted-but-not-editing: no BasicTextField means
                    // no IME on focus. OK enters edit mode (see the row's onKeyEvent above).
                    val empty = value.isEmpty()
                    val shown = when {
                        empty -> placeholder ?: ""
                        masked -> "•".repeat(value.length.coerceAtMost(12))
                        else -> value
                    }
                    Text(
                        text = shown,
                        style = textStyle.copy(color = if (empty) colors.textTertiary else colors.textPrimary),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                } else {
                    if (value.isEmpty() && placeholder != null) {
                        Text(text = placeholder, style = AreIptvTheme.typography.body, color = colors.textTertiary)
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        // P0.3: BasicTextField has no built-in label param (unlike Material's
                        // TextField) -- the visible label above is purely visual, TalkBack never
                        // announced it when the field itself gained focus. contentDescription
                        // links them for TalkBack; falls back to the placeholder if there's no
                        // label (e.g. this field is used unlabeled with a placeholder only).
                        modifier = Modifier.fillMaxWidth()
                            .then(if (activateOnClick) Modifier.focusRequester(fieldFocus) else Modifier)
                            // In tap-to-edit mode Back leaves edit mode (closes IME, returns to the row)
                            // instead of navigating away mid-typing.
                            .then(
                                if (activateOnClick) Modifier.onKeyEvent { ev ->
                                    if (ev.type == KeyEventType.KeyUp && ev.key == Key.Back) { editing = false; true } else false
                                } else Modifier,
                            )
                            .semantics {
                                val description = label ?: placeholder
                                if (description != null) contentDescription = description
                            },
                        interactionSource = interactionSource,
                        textStyle = textStyle.copy(color = colors.textPrimary),
                        singleLine = true,
                        keyboardOptions = if (activateOnClick) KeyboardOptions(imeAction = ImeAction.Done) else KeyboardOptions.Default,
                        keyboardActions = if (activateOnClick) KeyboardActions(onDone = { editing = false }) else KeyboardActions.Default,
                        visualTransformation = if (masked) PasswordVisualTransformation() else VisualTransformation.None,
                        cursorBrush = SolidColor(colors.accent),
                    )
                }
            }
        }
        if (helper != null || error != null) {
            Box(Modifier.height(8.dp))
            Text(
                text = error ?: helper.orEmpty(),
                style = AreIptvTheme.typography.caption,
                color = if (error != null) colors.danger else colors.textTertiary,
            )
        }
    }
}

@Preview(widthDp = 700, heightDp = 260, showBackground = true)
@Composable
private fun AreTextFieldPreview() {
    AreIptvTheme {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AreTextField(
                value = "http://provider.example.com/get.php",
                onValueChange = {},
                label = "Playlist URL",
                mono = true,
                icon = Icons.Filled.Link,
            )
            AreTextField(
                value = "",
                onValueChange = {},
                label = "Username",
                placeholder = "Enter username",
                helper = "Provided by your IPTV service",
            )
        }
    }
}
