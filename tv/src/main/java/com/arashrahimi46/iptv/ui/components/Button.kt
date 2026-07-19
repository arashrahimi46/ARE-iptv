package com.arashrahimi46.iptv.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.theme.TvFocusable

/** Visual style, mirrors the design system's `variant` prop (Button.jsx). */
enum class AreButtonVariant { Primary, Secondary, Ghost, Danger }

/** Sizing, mirrors the design system's `size` prop. */
enum class AreButtonSize { Small, Medium, Large }

private data class ButtonSizeSpec(val height: Dp, val paddingH: Dp, val gap: Dp, val iconSize: Dp)

private fun sizeSpec(size: AreButtonSize): ButtonSizeSpec = when (size) {
    AreButtonSize.Small -> ButtonSizeSpec(40.dp, 16.dp, 8.dp, 18.dp)
    AreButtonSize.Medium -> ButtonSizeSpec(52.dp, 22.dp, 10.dp, 20.dp)
    AreButtonSize.Large -> ButtonSizeSpec(62.dp, 30.dp, 12.dp, 24.dp)
}

/**
 * Button — primary action control, built for D-pad focus (Button.jsx).
 * Variants: primary | secondary | ghost | danger. Sizes: sm | md | lg.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AreButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: AreButtonVariant = AreButtonVariant.Primary,
    size: AreButtonSize = AreButtonSize.Medium,
    icon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    full: Boolean = false,
    disabled: Boolean = false,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val colors = AreIptvTheme.colors
    val spec = sizeSpec(size)
    val shape = RoundedCornerShape(AreIptvTheme.radius.md)

    val (background, contentColor) = when (variant) {
        AreButtonVariant.Primary -> colors.accent to colors.accentFg
        AreButtonVariant.Secondary -> colors.surface2 to colors.textPrimary
        AreButtonVariant.Ghost -> Color.Transparent to colors.textSecondary
        AreButtonVariant.Danger -> colors.danger to Color.White
    }

    TvFocusable(
        onClick = onClick,
        modifier = (if (full) modifier.fillMaxWidth() else modifier.wrapContentWidth())
            .height(spec.height),
        interactionSource = interactionSource,
        shape = shape,
        backgroundColor = if (disabled) background.copy(alpha = 0.4f) else background,
        enabled = !disabled,
    ) { _, _ ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spec.paddingH),
            horizontalArrangement = Arrangement.spacedBy(spec.gap, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(spec.iconSize))
            }
            Text(
                text = text,
                style = AreIptvTheme.typography.label,
                color = if (disabled) contentColor.copy(alpha = 0.4f) else contentColor,
            )
            if (trailingIcon != null) {
                Icon(trailingIcon, contentDescription = null, tint = contentColor, modifier = Modifier.size(spec.iconSize))
            }
        }
    }
}

@Preview(widthDp = 900, heightDp = 260, showBackground = true)
@Composable
private fun AreButtonPreview() {
    AreIptvTheme {
        Box(Modifier.padding(24.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                AreButton("Play now", onClick = {}, variant = AreButtonVariant.Primary, icon = Icons.Filled.PlayArrow)
                AreButton("More info", onClick = {}, variant = AreButtonVariant.Secondary)
                AreButton("Skip", onClick = {}, variant = AreButtonVariant.Ghost)
                AreButton("Remove", onClick = {}, variant = AreButtonVariant.Danger)
            }
        }
    }
}
