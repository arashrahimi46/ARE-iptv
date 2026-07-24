package com.arashrahimi46.iptv.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.theme.TvFocusable
import com.arashrahimi46.iptv.ui.theme.glassBorderBrush

/** IconButton variants, mirrors IconButton.jsx: solid | glass | ghost. */
enum class AreIconButtonVariant { Solid, Glass, Ghost }

enum class AreIconButtonSize { Small, Medium, Large }

private fun dimsFor(size: AreIconButtonSize) = when (size) {
    AreIconButtonSize.Small -> 40.dp
    AreIconButtonSize.Medium -> 52.dp
    AreIconButtonSize.Large -> 64.dp
}

/**
 * IconButton — square control for a single glyph (player HUD, nav, toolbars).
 * `active` renders the accent-filled state (e.g. currently playing).
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AreIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: AreIconButtonVariant = AreIconButtonVariant.Ghost,
    size: AreIconButtonSize = AreIconButtonSize.Medium,
    active: Boolean = false,
    disabled: Boolean = false,
    /** Overrides the glyph tint (e.g. a red REC dot) without changing the variant's background. */
    contentTint: Color? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val colors = AreIptvTheme.colors
    val dims = dimsFor(size)
    val shape = RoundedCornerShape(AreIptvTheme.radius.md)

    val (background, contentColor) = when (variant) {
        AreIconButtonVariant.Solid -> colors.surface2 to colors.textPrimary
        // textPrimary (not a hardcoded white) so glass icons read on the frosted panel in BOTH
        // themes -- white-on-60%-white was invisible in light mode (e.g. the player HUD).
        AreIconButtonVariant.Glass -> colors.surfaceGlass to colors.textPrimary
        AreIconButtonVariant.Ghost -> Color.Transparent to colors.textSecondary
    }
    val resolvedBackground = if (active) colors.accent else background
    val resolvedContentColor = contentTint ?: if (active) colors.accentFg else contentColor
    // Glass variant carries the "lit edge" gradient hairline (not when accent-active).
    val glassBorder = if (variant == AreIconButtonVariant.Glass && !active) glassBorderBrush() else null

    TvFocusable(
        onClick = onClick,
        modifier = modifier.size(dims),
        interactionSource = interactionSource,
        shape = shape,
        backgroundColor = if (disabled) resolvedBackground.copy(alpha = 0.4f) else resolvedBackground,
        borderBrush = glassBorder,
        enabled = !disabled,
    ) { _, _ ->
        Box(modifier = Modifier.size(dims), contentAlignment = Alignment.Center) {
            Icon(
                icon,
                contentDescription = contentDescription,
                tint = if (disabled) resolvedContentColor.copy(alpha = 0.4f) else resolvedContentColor,
                modifier = Modifier.size(if (size == AreIconButtonSize.Large) 28.dp else 24.dp),
            )
        }
    }
}

@Preview(widthDp = 500, heightDp = 160, showBackground = true)
@Composable
private fun AreIconButtonPreview() {
    AreIptvTheme {
        Box(Modifier.padding(24.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                AreIconButton(Icons.Filled.Search, "Search", onClick = {}, variant = AreIconButtonVariant.Solid)
                AreIconButton(Icons.Filled.Settings, "Settings", onClick = {}, variant = AreIconButtonVariant.Glass, active = true)
                AreIconButton(Icons.Filled.Search, "Search", onClick = {}, variant = AreIconButtonVariant.Ghost)
            }
        }
    }
}
