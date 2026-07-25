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
import com.arashrahimi46.iptv.ui.theme.ControlTone
import com.arashrahimi46.iptv.ui.theme.controlSkin

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

    // Same single funnel as AreButton (ControlSkin.kt). The Glass variant no longer decides its own
    // density: whether it is a full glass surface or a nested tint is answered by LocalOnGlass, i.e.
    // by what it is actually sitting on. The top bar's buttons and the player HUD's buttons are the
    // same variant on different backgrounds, and only context can tell them apart.
    val skin = controlSkin(
        tone = when {
            active -> ControlTone.Primary
            variant == AreIconButtonVariant.Ghost -> ControlTone.Ghost
            else -> ControlTone.Neutral
        },
        disabled = disabled,
    )
    val resolvedContentColor = contentTint ?: skin.content

    TvFocusable(
        onClick = onClick,
        modifier = modifier.size(dims),
        interactionSource = interactionSource,
        shape = shape,
        backgroundColor = skin.fillColor,
        backgroundBrush = skin.fillBrush,
        shadowElevation = skin.elevation,
        borderColor = skin.borderColor,
        borderBrush = skin.borderBrush,
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
