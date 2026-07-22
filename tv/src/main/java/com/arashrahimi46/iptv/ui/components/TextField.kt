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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val colors = AreIptvTheme.colors
    val shape = RoundedCornerShape(AreIptvTheme.radius.md)

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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .tvFocusable(
                    interactionSource = interactionSource,
                    shape = shape,
                    glowColor = if (error != null) colors.danger else colors.focusRing,
                    disableScale = true,
                    ownsFocusable = false,
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
                    modifier = Modifier.fillMaxWidth().semantics {
                        val description = label ?: placeholder
                        if (description != null) contentDescription = description
                    },
                    interactionSource = interactionSource,
                    textStyle = (if (mono) AreIptvTheme.typography.mono else AreIptvTheme.typography.body)
                        .copy(color = colors.textPrimary),
                    singleLine = true,
                    visualTransformation = if (masked) PasswordVisualTransformation() else VisualTransformation.None,
                    cursorBrush = SolidColor(colors.accent),
                )
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
