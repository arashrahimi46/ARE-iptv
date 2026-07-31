package com.arashrahimi46.iptv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.arashrahimi46.iptv.ui.interaction.AreInteractive
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.theme.ControlTone
import com.arashrahimi46.iptv.ui.theme.controlSkin

data class TabItem(val id: String, val label: String)

/**
 * Tabs — horizontal category switcher (Tabs.jsx). Underline indicator marks
 * the active tab; a thin divider runs beneath the full row.
 */
@Composable
fun AreTabs(
    items: List<TabItem>,
    active: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AreIptvTheme.colors
    Column(modifier = modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items.forEach { item ->
                AreTab(item = item, active = item.id == active, onClick = { onSelect(item.id) })
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.borderSubtle),
        )
    }
}

@Composable
private fun AreTab(item: TabItem, active: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    // ONE funnel for every control's appearance (see ControlSkin.kt): an unselected tab is neutral
    // glass so the strip reads as a row of glass controls, and the current tab is the accent lens
    // (§6.2) -- the same "selected" material as a current chip or segment. Neither is hand-rolled here.
    val skin = controlSkin(ControlTone.Neutral, selected = active)
    AreInteractive(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(AreIptvTheme.radius.xs),
        backgroundColor = skin.fillColor,
        backgroundBrush = skin.fillBrush,
        shadowElevation = skin.elevation,
        borderColor = skin.borderColor,
        borderBrush = skin.borderBrush,
    ) { _, _ ->
        Text(
            text = item.label,
            style = AreIptvTheme.typography.h3,
            color = skin.content,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

@Preview(widthDp = 700, heightDp = 160, showBackground = true)
@Composable
private fun AreTabsPreview() {
    AreIptvTheme {
        Box(Modifier.padding(24.dp)) {
            AreTabs(
                items = listOf(TabItem("all", "All"), TabItem("hd", "HD"), TabItem("sports", "Sports")),
                active = "hd",
                onSelect = {},
            )
        }
    }
}
