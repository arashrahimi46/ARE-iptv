package com.arashrahimi46.iptv.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
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

    val (enabledBg, enabledFg) = when (variant) {
        AreButtonVariant.Primary -> colors.accent to colors.accentFg
        AreButtonVariant.Secondary -> colors.surface2 to colors.textPrimary
        AreButtonVariant.Ghost -> Color.Transparent to colors.textSecondary
        AreButtonVariant.Danger -> colors.danger to Color.White
    }
    // Disabled: dimming both fill and label by the same alpha collapses contrast in light mode
    // (a 40% accent fill under 40% white text is unreadable -- the "Refresh now" defect). Use a
    // neutral muted surface + tertiary text so the label stays legible and clearly disabled in
    // both themes. Ghost stays transparent.
    val background = if (!disabled) enabledBg
        else if (variant == AreButtonVariant.Ghost) Color.Transparent else colors.surface3
    val contentColor = if (disabled) colors.textTertiary else enabledFg

    TvFocusable(
        onClick = onClick,
        modifier = (if (full) modifier.fillMaxWidth() else modifier.wrapContentWidth())
            .height(spec.height),
        interactionSource = interactionSource,
        shape = shape,
        backgroundColor = background,
        enabled = !disabled,
    ) { _, _ ->
        // QA regression root cause (onboarding Continue engulfed by Skip's hit-region, Settings
        // PIN row squeezed to near-zero): this Row unconditionally called .fillMaxWidth(),
        // which measures with the OUTER Box's loosened min-width-0-but-max-width-unchanged
        // constraint from wrapContentWidth() above and greedily fills it -- defeating
        // wrapContentWidth() entirely for every full=false button (every AreButton call site
        // that doesn't pass full=true). The outer Box then reports THAT large size to its own
        // parent Row, so a "Skip for now"/"Change PIN" button's real clickable bounds became
        // however much slack was available in the parent Row, not its own label's intrinsic
        // size -- exactly the reported "one button's tap region covers a sibling" defect class.
        // fillMaxWidth only makes sense here when the outer Box is ALSO deliberately full-width
        // (full=true), so centering the icon/text within that intentional full width does
        // something; otherwise this Row must be free to report its own intrinsic size.
        Row(
            // fillMaxHeight so the label/icon Row spans the full button height and
            // CenterVertically actually centers them. Without it the Row wraps to the
            // text height and TvFocusable's Box pins it to the top edge -- the reported
            // "Skip for now / Continue text not centered" defect. Mirrors the design
            // system's `align-items:center; justify-content:center` on the button.
            modifier = (if (full) Modifier.fillMaxWidth() else Modifier)
                .fillMaxHeight()
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
                color = contentColor,
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
