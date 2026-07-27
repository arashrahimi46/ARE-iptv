package com.arashrahimi46.iptv.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.arashrahimi46.iptv.R
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.theme.glassChild
import com.arashrahimi46.iptv.ui.theme.TvFocusable

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
    // "See all" is horizontally offset from the tiles, so a plain 2D focus search moving UP from a
    // tile lands on the vertically-aligned tile in the rail above instead of ever reaching it. We
    // redirect the rail's UP exit to this requester so the button is actually reachable by D-pad.
    val seeAllFocus = remember { FocusRequester() }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.safeX)
                // Top inset so the See-all focus ring + glow have somewhere to draw. The ring is
                // painted OUTSIDE the control's bounds, and the first rail on Home sits flush
                // against the top of the scroll area -- so without this the ring is sliced off,
                // which is what made a focused See all look broken rather than focused.
                .padding(top = 6.dp, bottom = spacing.sp4),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = title, style = AreIptvTheme.typography.h3, color = colors.textPrimary)
            if (smart) {
                AreBadge(stringResource(R.string.badge_smart), tone = AreBadgeTone.Smart, glow = true)
            }
            if (seeAll) {
                Box(Modifier.weight(1f))
                // Real focusable/clickable control -- previously a bare Text, so on TV there was
                // no D-pad target and onSeeAll never fired.
                // A glass chip, not bare text. As plain text it only announced itself as a control
                // once focused -- at two metres an unfocused "See all ›" reads as a caption, so the
                // one D-pad target in the header was invisible until you happened to land on it.
                val seeAllShape = RoundedCornerShape(AreIptvTheme.radius.pill)
                TvFocusable(
                    onClick = onSeeAll,
                    modifier = Modifier.focusRequester(seeAllFocus),
                    shape = seeAllShape,
                ) { focused, _ ->
                    Text(
                        text = stringResource(R.string.browse_see_all),
                        style = AreIptvTheme.typography.label,
                        color = if (focused) colors.textPrimary else colors.textSecondary,
                        modifier = Modifier
                            .glassChild(seeAllShape)
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                    )
                }
            }
        }
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                // focusProperties only governs a group's exit when the container is an explicit
                // focusGroup; without it the up-redirect is ignored and focus jumps to the rail above.
                .then(if (seeAll) Modifier.focusProperties { up = seeAllFocus }.focusGroup() else Modifier),
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
