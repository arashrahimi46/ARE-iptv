package com.arashrahimi46.iptv.mobile.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arashrahimi46.iptv.core.R as CoreR
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.theme.ControlTone
import com.arashrahimi46.iptv.ui.theme.controlSkin

enum class AreButtonVariant { Primary, Secondary, Ghost, Danger }

/** Visual heights 40 / 52 / 62 dp. `Small` keeps a 48dp TOUCH target regardless. */
enum class AreButtonSize { Small, Medium, Large }

private data class ButtonSizeSpec(val height: Dp, val paddingH: Dp, val gap: Dp, val iconSize: Dp)

private fun sizeSpec(size: AreButtonSize): ButtonSizeSpec = when (size) {
    AreButtonSize.Small -> ButtonSizeSpec(40.dp, 16.dp, 8.dp, 18.dp)
    AreButtonSize.Medium -> ButtonSizeSpec(52.dp, 22.dp, 10.dp, 20.dp)
    AreButtonSize.Large -> ButtonSizeSpec(62.dp, 30.dp, 12.dp, 24.dp)
}

/**
 * The app's text button. Fill/border/label colour all come from [controlSkin] -- never re-derived
 * here (ground rule 7).
 *
 * [loading] swaps the label for a spinner without changing the button's width: the label stays in
 * the layout at `alpha = 0f` behind the indicator, so a "Connect" button doesn't jump to spinner
 * width and back.
 */
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
    enabled: Boolean = true,
    loading: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
) {
    val spec = sizeSpec(size)
    val shape = RoundedCornerShape(AreIptvTheme.radius.md)
    val skin = controlSkin(
        tone = when (variant) {
            AreButtonVariant.Primary -> ControlTone.Primary
            AreButtonVariant.Secondary -> ControlTone.Neutral
            AreButtonVariant.Ghost -> ControlTone.Ghost
            AreButtonVariant.Danger -> ControlTone.Danger
        },
        disabled = !enabled,
    )
    val contentColor = skin.content
    val loadingLabel = stringResource(CoreR.string.detail_loading)

    Row(
        modifier = modifier
            .then(if (full) Modifier.fillMaxWidth() else Modifier)
            .areTouch(
                onClick = onClick,
                skin = skin,
                shape = shape,
                enabled = enabled && !loading,
                onLongClick = onLongClick,
                interactionSource = interactionSource,
                // Only `Small` is under 48dp; the other two already clear it on both axes.
                minTouchTarget = if (size == AreButtonSize.Small) 48.dp else 0.dp,
            )
            .then(if (loading) Modifier.semantics { stateDescription = loadingLabel } else Modifier)
            .height(spec.height)
            .padding(horizontal = spec.paddingH),
        horizontalArrangement = Arrangement.spacedBy(spec.gap, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null && !loading) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(spec.iconSize))
        }
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = AreIptvTheme.typography.label,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = if (loading) Modifier.alpha(0f) else Modifier,
            )
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = contentColor,
                )
            }
        }
        if (trailingIcon != null && !loading) {
            Icon(trailingIcon, contentDescription = null, tint = contentColor, modifier = Modifier.size(spec.iconSize))
        }
    }
}
