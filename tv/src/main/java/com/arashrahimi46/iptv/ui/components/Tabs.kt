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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.theme.TvFocusable
import com.arashrahimi46.iptv.ui.theme.tvGlow

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
    val colors = AreIptvTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    TvFocusable(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(AreIptvTheme.radius.xs),
    ) { _, _ ->
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = item.label,
                style = if (active) AreIptvTheme.typography.h3 else AreIptvTheme.typography.h3,
                color = if (active) colors.textPrimary else colors.textTertiary,
            )
            Box(Modifier.height(8.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .then(
                        if (active) {
                            Modifier.tvGlow(colors.accent, RoundedCornerShape(AreIptvTheme.radius.pill), spread = 6.dp)
                        } else {
                            Modifier
                        },
                    )
                    .background(if (active) colors.accent else Color.Transparent, RoundedCornerShape(AreIptvTheme.radius.pill)),
            )
        }
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
