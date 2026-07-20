package com.arashrahimi46.iptv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.theme.TvFocusable

/**
 * CategoryRow — compact list row for the category filter column (CategoryRow.jsx)
 * used down the left of Live TV, Movies, Series and the EPG guide.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AreCategoryRow(
    name: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    count: Int? = null,
    kind: AreCategoryKind = AreCategoryKind.Default,
    smart: Boolean = false,
    active: Boolean = false,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val colors = AreIptvTheme.colors
    val shape = RoundedCornerShape(AreIptvTheme.radius.md)
    val background = if (active) colors.accentWash else colors.bgBase

    TvFocusable(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(56.dp),
        interactionSource = interactionSource,
        shape = shape,
        backgroundColor = background,
    ) { _, _ ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(if (active) colors.accent else colors.surface3, RoundedCornerShape(AreIptvTheme.radius.sm)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    kind.icon,
                    contentDescription = null,
                    tint = if (active) colors.accentFg else colors.textTertiary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = name,
                    style = AreIptvTheme.typography.label,
                    color = if (active) colors.textPrimary else colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (smart) {
                    AreBadge("Smart", tone = AreBadgeTone.Smart)
                }
            }
            if (count != null) {
                Text(text = "$count", style = AreIptvTheme.typography.mono, color = colors.textTertiary)
            }
            Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.textTertiary, modifier = Modifier.size(18.dp))
        }
    }
}

@Preview(widthDp = 420, heightDp = 260, showBackground = true)
@Composable
private fun AreCategoryRowPreview() {
    AreIptvTheme {
        androidx.compose.foundation.layout.Column {
            AreCategoryRow(name = "Sports", onClick = {}, count = 42, kind = AreCategoryKind.Live, active = true)
            AreCategoryRow(name = "Movies", onClick = {}, count = 512, kind = AreCategoryKind.Movies)
        }
    }
}
