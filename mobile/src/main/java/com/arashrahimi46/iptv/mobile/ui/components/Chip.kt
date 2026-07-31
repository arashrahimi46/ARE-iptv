package com.arashrahimi46.iptv.mobile.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.theme.ControlTone
import com.arashrahimi46.iptv.ui.theme.controlSkin

/** Visual heights 34 / 42 dp; both carry a 48dp touch target. */
enum class AreChipSize { Small, Medium }

/**
 * A filter/selection pill. Selection is the glass LENS (via [controlSkin]), never a gradient -- ground
 * rule 8. The 48dp target is intrinsic now; the old `LocalMinTouchTarget` CompositionLocal hack is
 * gone.
 */
@Composable
fun AreChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    icon: ImageVector? = null,
    dotColor: Color? = null,
    size: AreChipSize = AreChipSize.Medium,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
) {
    val height = if (size == AreChipSize.Small) 34.dp else 42.dp
    val paddingH = if (size == AreChipSize.Small) 14.dp else 18.dp
    val shape = RoundedCornerShape(AreIptvTheme.radius.pill)
    val skin = controlSkin(
        tone = ControlTone.Neutral,
        selected = selected,
        disabled = !enabled,
        selectable = true,
    )
    // The label cross-fades with the fill so a selection change doesn't snap.
    val contentColor by animateColorAsState(
        targetValue = skin.content,
        animationSpec = tween(AreIptvTheme.motion.durFastMs),
        label = "are-chip-content",
    )

    Row(
        modifier = modifier
            .areTouch(
                onClick = onClick,
                skin = skin,
                shape = shape,
                enabled = enabled,
                role = Role.RadioButton,
                interactionSource = interactionSource,
                minTouchTarget = 48.dp,
            )
            .semantics { this.selected = selected }
            .height(height)
            .padding(horizontal = paddingH),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        if (dotColor != null) {
            Box(Modifier.size(8.dp).background(dotColor, CircleShape))
        }
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(16.dp))
        }
        Text(
            text = text,
            style = AreIptvTheme.typography.label,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
