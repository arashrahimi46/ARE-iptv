package com.arashrahimi46.iptv.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.theme.TvFocusable
import com.arashrahimi46.iptv.ui.theme.glassSurface

/**
 * Segmented control — one glass track holding a small fixed set of options, the selected one filled
 * with the accent (design §6c, recommended for the Settings tab strip and the Search/Favorites/Guide
 * filter rows). D-pad Left/Right moves between segments like any focusable row; OK selects. For long
 * or dynamic option lists (e.g. Guide's channel groups) keep [AreChip] instead.
 */
@Composable
fun <T> AreSegmentedControl(
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AreIptvTheme.colors
    val pill = RoundedCornerShape(AreIptvTheme.radius.pill)
    Row(
        modifier = modifier
            .height(46.dp)
            .glassSurface(pill)
            .padding(4.dp)
            .selectableGroup(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            TvFocusable(
                onClick = { onSelect(option) },
                shape = pill,
                backgroundColor = if (isSelected) colors.accent else Color.Transparent,
            ) { _, _ ->
                Box(
                    modifier = Modifier.fillMaxHeight().padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label(option),
                        style = AreIptvTheme.typography.label,
                        color = if (isSelected) colors.accentFg else colors.textSecondary,
                    )
                }
            }
        }
    }
}
