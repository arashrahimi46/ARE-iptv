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
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arashrahimi46.iptv.core.R as CoreR
import com.arashrahimi46.iptv.mobile.design.AreIptvTheme
import com.arashrahimi46.iptv.mobile.design.glassWell

/**
 * The app's text input: a recessed [glassWell] with an accent border while focused.
 *
 * The TV field's `activateOnClick` mode (row owns focus, OK opens the IME, D-pad-scrolling past a
 * field must NOT pop the keyboard) does not exist here -- a phone field focuses and opens the IME
 * on tap, always.
 *
 * [trailingIcon] + [onTrailingIconClick] is the universal clear button; every search field in the
 * app uses it, which is why [trailingIconContentDescription] defaults to "Clear". A caller that
 * puts some OTHER action there (paste, reveal) MUST pass its own description.
 * [keyboardOptions]/[keyboardActions] exist so a screen can give the IME a real action
 * instead of a newline.
 */
@Composable
fun AreTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    label: String? = null,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    onTrailingIconClick: (() -> Unit)? = null,
    trailingIconContentDescription: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    isError: Boolean = false,
    supportingText: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    monospace: Boolean = false,
    interactionSource: MutableInteractionSource? = null,
) {
    val colors = AreIptvTheme.colors
    val shape = RoundedCornerShape(AreIptvTheme.radius.md)
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val borderColor = when {
        isError -> colors.danger
        focused -> colors.accent
        else -> null
    }
    val textStyle = if (monospace) AreIptvTheme.typography.mono else AreIptvTheme.typography.body

    Column(modifier = modifier) {
        if (label != null) {
            Text(text = label, style = AreIptvTheme.typography.label, color = colors.textSecondary)
            Box(Modifier.height(8.dp))
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .glassWell(shape)
                .then(if (borderColor != null) Modifier.border(1.dp, borderColor, shape) else Modifier)
                .padding(start = 16.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (leadingIcon != null) {
                Icon(
                    leadingIcon,
                    contentDescription = null,
                    tint = colors.textTertiary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Box(Modifier.weight(1f).padding(vertical = 8.dp)) {
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
                    enabled = enabled,
                    interactionSource = source,
                    textStyle = textStyle.copy(color = colors.textPrimary),
                    singleLine = singleLine,
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    visualTransformation = visualTransformation,
                    cursorBrush = SolidColor(colors.accent),
                )
            }
            if (trailingIcon != null) {
                if (onTrailingIconClick != null) {
                    AreIconButton(
                        icon = trailingIcon,
                        // The button's own action, never the field's name: announcing "Search" on
                        // the clear button both mis-states what tapping does and repeats the
                        // contentDescription the field itself already carries.
                        contentDescription = trailingIconContentDescription
                            ?: stringResource(CoreR.string.action_clear),
                        onClick = onTrailingIconClick,
                        variant = AreIconButtonVariant.Ghost,
                        size = AreIconButtonSize.Small,
                    )
                } else {
                    Icon(
                        trailingIcon,
                        contentDescription = null,
                        tint = colors.textTertiary,
                        modifier = Modifier.padding(end = 10.dp).size(20.dp),
                    )
                }
            }
        }
        if (supportingText != null) {
            Box(Modifier.height(6.dp))
            Text(
                text = supportingText,
                style = AreIptvTheme.typography.caption,
                color = if (isError) colors.danger else colors.textTertiary,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}
