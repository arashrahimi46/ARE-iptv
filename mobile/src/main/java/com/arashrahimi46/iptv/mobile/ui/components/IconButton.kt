package com.arashrahimi46.iptv.mobile.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arashrahimi46.iptv.mobile.design.AreIptvTheme
import com.arashrahimi46.iptv.mobile.design.ControlTone
import com.arashrahimi46.iptv.mobile.design.ProvideOnGlass
import com.arashrahimi46.iptv.mobile.design.controlSkin

enum class AreIconButtonVariant { Solid, Glass, Ghost }

/** Visual 40 / 48 / 56 dp, glyph 20 / 24 / 28 dp. `ExtraSmall` no longer exists. */
enum class AreIconButtonSize { Small, Medium, Large }

private fun boxFor(size: AreIconButtonSize): Dp = when (size) {
    AreIconButtonSize.Small -> 40.dp
    AreIconButtonSize.Medium -> 48.dp
    AreIconButtonSize.Large -> 56.dp
}

private fun glyphFor(size: AreIconButtonSize): Dp = when (size) {
    AreIconButtonSize.Small -> 20.dp
    AreIconButtonSize.Medium -> 24.dp
    AreIconButtonSize.Large -> 28.dp
}

/**
 * Glyph-only button. [contentDescription] is mandatory and non-null -- it IS the accessibility
 * label, and a glyph with no label is invisible to TalkBack.
 *
 * `Small` is 40dp visually with a 48dp touch target, which is what makes it the only size legal
 * inside a tile.
 */
@Composable
fun AreIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: AreIconButtonVariant = AreIconButtonVariant.Ghost,
    size: AreIconButtonSize = AreIconButtonSize.Medium,
    active: Boolean = false,
    enabled: Boolean = true,
    contentTint: Color? = null,
    onLongClick: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
) {
    // `Glass` is the same NEUTRAL tone as a page control -- what differs is the CONTEXT it resolves
    // in (design §6: nesting is contextual, not a variant), which is why it composes under
    // ProvideOnGlass rather than picking a different tone. Ghost/Solid never read the flag, so they
    // are left in whatever context the caller already established.
    OnGlassIf(variant == AreIconButtonVariant.Glass) {
        val skin = controlSkin(
            tone = when (variant) {
                AreIconButtonVariant.Solid -> ControlTone.Primary
                AreIconButtonVariant.Glass -> ControlTone.Neutral
                AreIconButtonVariant.Ghost -> ControlTone.Ghost
            },
            selected = active,
            disabled = !enabled,
        )
        val shape = if (variant == AreIconButtonVariant.Solid) {
            RoundedCornerShape(AreIptvTheme.radius.md)
        } else {
            CircleShape
        }
        val box = boxFor(size)
        Box(
            modifier = modifier
                .areTouch(
                    onClick = onClick,
                    skin = skin,
                    shape = shape,
                    enabled = enabled,
                    onLongClick = onLongClick,
                    interactionSource = interactionSource,
                    minTouchTarget = if (size == AreIconButtonSize.Small) 48.dp else 0.dp,
                )
                .size(box),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = contentTint ?: skin.content,
                modifier = Modifier.size(glyphFor(size)),
            )
        }
    }
}

/** [ProvideOnGlass] only when [on]; otherwise the caller's context is left untouched. */
@Composable
private fun OnGlassIf(on: Boolean, content: @Composable () -> Unit) {
    if (on) ProvideOnGlass(true, content) else content()
}
