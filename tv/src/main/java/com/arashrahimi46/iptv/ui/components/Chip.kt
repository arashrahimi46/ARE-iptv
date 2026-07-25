package com.arashrahimi46.iptv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.tv.material3.Text
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.theme.ControlTone
import com.arashrahimi46.iptv.ui.theme.TvFocusable
import com.arashrahimi46.iptv.ui.theme.controlSkin

enum class AreChipSize { Small, Medium }

/**
 * Chip — filter / category pill (Chip.jsx). Toggles a `selected` state.
 * Optional leading dot (genre color) or icon.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AreChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    icon: ImageVector? = null,
    dotColor: Color? = null,
    size: AreChipSize = AreChipSize.Medium,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val height = if (size == AreChipSize.Small) 34.dp else 42.dp
    val paddingH = if (size == AreChipSize.Small) 14.dp else 18.dp
    val shape = RoundedCornerShape(AreIptvTheme.radius.pill)

    // ONE funnel for every control's appearance (see ControlSkin.kt): an unselected chip is neutral
    // glass, a selected one is the accent lens (§6.2). Nothing about the fill, border, content colour
    // or lift is decided here, so a chip matches every other control of the same tone.
    val skin = controlSkin(ControlTone.Neutral, selected = selected)
    val contentColor = skin.content

    TvFocusable(
        onClick = onClick,
        modifier = modifier.height(height),
        interactionSource = interactionSource,
        shape = shape,
        backgroundColor = skin.fillColor,
        backgroundBrush = skin.fillBrush,
        shadowElevation = skin.elevation,
        borderColor = skin.borderColor,
        borderBrush = skin.borderBrush,
    ) { _, _ ->
        Row(
            modifier = Modifier.fillMaxHeight().padding(horizontal = paddingH),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
            if (dotColor != null) {
                Box(Modifier.size(8.dp).background(dotColor, CircleShape))
            }
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(18.dp))
            }
            Text(text = text, style = AreIptvTheme.typography.label, color = contentColor)
        }
    }
}

@Preview(widthDp = 700, heightDp = 140, showBackground = true)
@Composable
private fun AreChipPreview() {
    AreIptvTheme {
        Box(Modifier.padding(24.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AreChip("All", onClick = {}, selected = true)
                AreChip("Sports", onClick = {}, dotColor = AreIptvTheme.colors.danger)
                AreChip("Movies", onClick = {})
            }
        }
    }
}
