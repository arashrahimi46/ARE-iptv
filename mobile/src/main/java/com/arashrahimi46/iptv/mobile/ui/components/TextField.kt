package com.arashrahimi46.iptv.mobile.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.theme.glassWell

/**
 * Touch counterpart to `:tv`'s [com.arashrahimi46.iptv.ui.components.AreTextField]. That component
 * is genuinely D-pad-specific -- its `activateOnClick` mode (row owns focus, OK opens the IME,
 * D-pad-scrolling past the field must NOT pop the keyboard) is built on `tvFocusable`'s key-event
 * handling and focus-ring rendering, neither of which exist on touch (a phone field just focuses
 * and opens the IME on tap, always). Rather than force that machinery through the shared
 * `AreInteractive` seam -- which several :tv call sites depend on for exact D-pad behavior -- this
 * is a separate, mobile-appropriate composable: same glass chrome (label, resting [glassWell] edge,
 * icon/masking/helper/error slots), no `activateOnClick`, no focus ring (matches
 * [com.arashrahimi46.iptv.mobile.ui.theme.AreTouchable]'s "a touch surface has neither" rule) --
 * just a subtle accent border while focused.
 */
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
    helper: String? = null,
    error: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val colors = AreIptvTheme.colors
    val shape = RoundedCornerShape(AreIptvTheme.radius.md)
    val focused by interactionSource.collectIsFocusedAsState()
    val borderColor = when {
        error != null -> colors.danger
        focused -> colors.accent
        else -> null
    }

    Column(modifier = modifier) {
        if (label != null) {
            Text(text = label, style = AreIptvTheme.typography.label, color = colors.textSecondary)
            Box(Modifier.height(8.dp))
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .glassWell(shape)
                .then(if (borderColor != null) Modifier.border(1.dp, borderColor, shape) else Modifier)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = colors.textTertiary, modifier = Modifier.size(20.dp))
            }
            Box(Modifier.weight(1f)) {
                val textStyle = if (mono) AreIptvTheme.typography.mono else AreIptvTheme.typography.body
                if (value.isEmpty() && placeholder != null) {
                    Text(
                        text = placeholder,
                        style = textStyle.copy(color = colors.textTertiary),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            val description = label ?: placeholder
                            if (description != null) contentDescription = description
                        },
                    interactionSource = interactionSource,
                    textStyle = textStyle.copy(color = colors.textPrimary),
                    singleLine = true,
                    keyboardOptions = keyboardOptions,
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
