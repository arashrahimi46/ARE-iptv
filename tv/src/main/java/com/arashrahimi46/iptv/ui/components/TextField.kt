package com.arashrahimi46.iptv.ui.components

import androidx.compose.foundation.background
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
            when {
                editing -> runCatching { fieldFocus.requestFocus() } // focus -> IME opens
                wasEditing -> {
                    keyboard?.hide()
                    runCatching { rowFocus.requestFocus() } // land back on the row, not the top of the list
                }
            }
            wasEditing = editing
        }
    }

    // Resting/error border is always visible (even unfocused); the focus ring/glow/scale on
    // top of it comes from the shared tvFocusable() primitive below, driven by the same
    // interactionSource that BasicTextField owns. Scale is disabled -- growing a text input
    // 1.06x on focus reflows sibling layout and moves the caret; the ring/glow alone is the
    // correct treatment for inputs per the design system's focus-visible rule.
    val restingBorderColor = if (error != null) colors.danger else colors.borderDefault

    Column(modifier = modifier) {
        if (label != null) {
            Text(text = label, style = AreIptvTheme.typography.label, color = colors.textSecondary)
            Box(Modifier.height(8.dp))
        }
        // When [activateOnClick] and not editing, the ROW owns the focusable and OK enters edit
        // mode; otherwise the BasicTextField owns focus (original always-editable behavior).
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
                    ownsFocusable = rowOwnsFocus,
                )
                .then(
                    if (rowOwnsFocus) Modifier.onKeyEvent { ev ->
                        val isSelect = ev.key == Key.DirectionCenter || ev.key == Key.Enter || ev.key == Key.NumPadEnter
                        when {
                            isSelect && ev.type == KeyEventType.KeyUp -> { editing = true; true }
                            // Swallow the SELECT KeyDown so the platform doesn't also synthesize a click.
                            isSelect && ev.type == KeyEventType.KeyDown -> true
                            else -> false
                        }
                    } else Modifier,
                )
                .background(colors.surface1, shape)
                .border(1.dp, restingBorderColor, shape)
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
