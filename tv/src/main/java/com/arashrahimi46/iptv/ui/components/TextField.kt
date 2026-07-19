package com.arashrahimi46.iptv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme

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
    icon: ImageVector? = null,
    prefix: String? = null,
    helper: String? = null,
    error: String? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val colors = AreIptvTheme.colors
    val focused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(AreIptvTheme.radius.md)

    val borderColor = when {
        error != null -> colors.danger
        focused -> colors.focusRing
        else -> colors.borderDefault
    }
    val borderWidth = if (error != null || focused) 2.dp else 1.dp

    Column(modifier = modifier) {
        if (label != null) {
            Text(text = label, style = AreIptvTheme.typography.label, color = colors.textSecondary)
            Box(Modifier.height(8.dp))
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(colors.surface1, shape)
                .border(borderWidth, borderColor, shape)
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
                    modifier = Modifier.fillMaxWidth(),
                    interactionSource = interactionSource,
                    textStyle = (if (mono) AreIptvTheme.typography.mono else AreIptvTheme.typography.body)
                        .copy(color = colors.textPrimary),
                    singleLine = true,
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
