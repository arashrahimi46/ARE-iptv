package com.arashrahimi46.iptv.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme

/**
 * Rail — a titled horizontal row of tiles (Rail.jsx), the core Home building
 * block. Backed by [LazyRow] so D-pad focus travel drives scrolling and the
 * "peek" (rail-peek) of the next tile falls out of the content padding.
 * Header shows title, optional SMART tag, and a see-all affordance.
 */
@Composable
fun AreRail(
    title: String,
    modifier: Modifier = Modifier,
    smart: Boolean = false,
    seeAll: Boolean = true,
    onSeeAll: () -> Unit = {},
    content: LazyListScope.() -> Unit,
) {
    val colors = AreIptvTheme.colors
    val spacing = AreIptvTheme.spacing

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.safeX)
                .padding(bottom = spacing.sp4),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = title, style = AreIptvTheme.typography.h2, color = colors.textPrimary)
            if (smart) {
                AreBadge("Smart", tone = AreBadgeTone.Smart, glow = true)
            }
            if (seeAll) {
                Box(Modifier.weight(1f))
                Text(text = "See all  ›", style = AreIptvTheme.typography.label, color = colors.textTertiary)
            }
        }
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(
                start = spacing.safeX,
                end = spacing.safeX + spacing.railPeek,
                top = 4.dp,
                bottom = 12.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(spacing.railGap),
            content = content,
        )
    }
}

@Preview(widthDp = 1600, heightDp = 460, showBackground = true)
@Composable
private fun AreRailPreview() {
    val movies = listOf("Oppenheimer", "The Bear", "Dune Part Two", "Fallout")
    AreIptvTheme {
        AreRail(title = "Continue Watching", smart = true) {
            items(movies) { name ->
                AreContinueCard(title = name, onClick = {}, meta = "Movie")
            }
        }
    }
}
