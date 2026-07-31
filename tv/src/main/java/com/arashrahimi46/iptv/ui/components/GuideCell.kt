package com.arashrahimi46.iptv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.arashrahimi46.iptv.core.R
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.theme.TvFocusable
import com.arashrahimi46.iptv.ui.theme.glassBorderBrush

/**
 * GuideCell — one program block in the EPG grid (GuideCell.jsx). Width is set
 * by the caller (proportional to duration). Marks the live/now program with
 * an accent edge bar; long titles simply ellipsize in Phase 0 (the design
 * source's on-focus marquee scroll is a Phase 1+ polish item — see report).
 *
 * [onFocusChange] mirrors GuideCell.jsx's `onFocusChange` prop -- the Guide
 * screen wires this into the sticky "focused-program info bar" (no tooltips
 * on TV) via a shared focus state.
 */
@Composable
fun AreGuideCell(
    title: String,
    time: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    live: Boolean = false,
    now: Boolean = false,
    catchup: Boolean = false,
    progress: Float = 0f,
    width: Dp = AreIptvTheme.spacing.guideChannelWidth,
    onFocusChange: (Boolean) -> Unit = {},
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val colors = AreIptvTheme.colors
    val shape = RoundedCornerShape(AreIptvTheme.radius.sm)
    val background = if (now) colors.accentWash else colors.surfaceGlass
    val focused by interactionSource.collectIsFocusedAsState()
    LaunchedEffect(focused) { onFocusChange(focused) }
    val liveDesc = stringResource(R.string.badge_live)
    val catchupDesc = stringResource(R.string.guide_catchup_available_desc)

    TvFocusable(
        onClick = onClick,
        modifier = modifier.width(width).height(AreIptvTheme.spacing.guideRowHeight),
        interactionSource = interactionSource,
        shape = shape,
        backgroundColor = background,
        // Edge on every programme cell so they read as distinct blocks on the near-white
        // light-theme surface (borderless white-on-white merged into one flat sheet); the
        // now-playing cell keeps its accent-tinted fill + accent edge bar and takes a matching
        // accent border so the "on now" block stays the clear focal point.
        borderColor = if (now) colors.accent else null,
        borderBrush = if (now) null else glassBorderBrush(),
        // No scale-up here, unlike every other focusable. The programme lane is a
        // `horizontalScroll` container, which CLIPS: a 1.06x cell at the lane's left edge grew
        // straight into that clip and had its ring/glow shaved off against the pinned channel
        // column. The ring + glow alone still mark focus, and this also drops a graphicsLayer
        // animation from the D-pad path, which is the hottest path on this screen.
        disableScale = true,
    ) { _, _ ->
        Box(Modifier.fillMaxWidth().fillMaxHeight()) {
            // Elapsed portion of the programme, as a wash filling left-to-right UNDER the label.
            // This is the "how far in are we" read; before it, the only now-marker was the static
            // edge bar below, which never moved because the screen never passed a progress value
            // at all -- the bar it used to drive was permanently 0f-wide.
            if (now && progress > 0f) {
                Box(
                    Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxHeight()
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .background(colors.accent.copy(alpha = 0.20f)),
                )
            }
            if (now) {
                Box(
                    Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxHeight()
                        .width(3.dp)
                        .background(colors.accent),
                )
            }
            // A programme that began before the window opens is clamped to the window edge and lands
            // on the layout's 24dp minimum width -- far too narrow for a clock, let alone a title, so
            // it rendered as a cropped glyph or two ("1 8", ": ."). Below the threshold the cell is
            // still a focusable block (it must be: it plays the channel, and D-pad focus has to be
            // able to reach it) but it draws no text at all.
            // Three tiers, by how much horizontal room the programme's duration bought it:
            //  - under [MinLabelledWidth] a window-clipped sliver, which renders no text at all
            //    (it still has to be focusable -- it plays the channel, and D-pad travel has to be
            //    able to cross it) because a cropped half-glyph reads as corruption;
            //  - under [MinTimedWidth] the title ONLY, across two lines. The clock is the first
            //    thing to go: the same time is already in the ruler above and in the info bar, so
            //    spending a whole line on it here is what turned every 30-minute block into "Hom…";
            //  - above it, the full clock + status row and a one-line title, as before.
            if (width >= MinLabelledWidth) {
                val roomy = width >= MinTimedWidth
                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                    if (roomy) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = time,
                                style = AreIptvTheme.typography.mono,
                                color = if (now) colors.accentHover else colors.textTertiary,
                                // A programme that started before the window opens is clamped to the
                                // window edge and lands on the 24dp minimum width -- without this the
                                // clock wrapped to two lines ("18" / "00") and blew the fixed height.
                                maxLines = 1,
                            )
                            StatusGlyphs(live, catchup, liveDesc, catchupDesc)
                        }
                        Box(Modifier.height(2.dp))
                    }
                    Text(
                        text = title,
                        // caption (14sp), not label (16sp): the design system's stated 16sp floor is
                        // for *UI* text, and this is dense tabular schedule data -- the same 14sp the
                        // mono time label uses. Buys ~10 more channel rows on screen.
                        style = AreIptvTheme.typography.caption,
                        color = colors.textPrimary,
                        maxLines = if (roomy) 1 else 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    // The status cues still have to appear on a narrow cell -- they just move under
                    // the title, since there is no clock row to sit beside.
                    if (!roomy && (live || catchup)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StatusGlyphs(live, catchup, liveDesc, catchupDesc, leadingGap = 0.dp)
                        }
                    }
                }
            }
        }
    }
}

/**
 * P0.3 (WCAG 1.4.1): live/catch-up are colour-only cues to a sighted user (a coloured dot, a glyph),
 * so each carries a contentDescription that states the status for TalkBack.
 */
@Composable
private fun StatusGlyphs(
    live: Boolean,
    catchup: Boolean,
    liveDesc: String,
    catchupDesc: String,
    leadingGap: Dp = 8.dp,
) {
    val colors = AreIptvTheme.colors
    if (live) {
        Box(
            Modifier
                .padding(start = leadingGap)
                .size(6.dp)
                .background(colors.live, CircleShape)
                .semantics { contentDescription = liveDesc },
        )
    }
    if (catchup) {
        Text(
            text = " ⟲",
            style = AreIptvTheme.typography.caption,
            color = colors.success,
            modifier = Modifier.semantics { contentDescription = catchupDesc },
        )
    }
}

/** Narrowest cell that still gets any text. A word of a title at 14sp plus the cell's 8dp side
 *  padding needs roughly this much; below it the label is cropped mid-glyph rather than ellipsized. */
private val MinLabelledWidth = 48.dp

/** Narrowest cell that also gets the clock row. Below this the title takes both lines instead --
 *  at the guide's dp-per-minute scale this is roughly a 25-minute programme. */
private val MinTimedWidth = 108.dp

@Preview(widthDp = 900, heightDp = 140, showBackground = true)
@Composable
private fun AreGuideCellPreview() {
    AreIptvTheme {
        Row(modifier = Modifier.padding(24.dp)) {
            AreGuideCell(title = "Premier League Live", time = "20:00", onClick = {}, live = true, now = true, progress = 0.4f)
            AreGuideCell(title = "News at Nine", time = "21:00", onClick = {}, catchup = true)
        }
    }
}
