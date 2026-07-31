package com.arashrahimi46.iptv.mobile.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.theme.glassLens
import com.arashrahimi46.iptv.ui.theme.glassTrack
import com.arashrahimi46.iptv.ui.theme.lensContentColor

private val TrackHeight = 48.dp
private val TrackInset = 8.dp

/**
 * The one segmented selector in the app -- it replaces the TV `AreSegmentedControl` at every call
 * site. The selected segment is marked by a spring-animated glass LENS sliding inside a
 * [glassTrack], positioned from the segments' own measured bounds.
 *
 * [scrollable] is the touch answer to D-pad bring-into-view: with many options the strip scrolls
 * and the selected segment animates into view on change. Fixed mode gives every segment
 * `weight(1f)`. The lens lives INSIDE the scrolling content, so it travels with its segment.
 *
 * The track IS clipped now -- the TV rationale for leaving it unclipped (room for a focus ring) is
 * gone.
 *
 * **RTL:** every measured number here ([positionInParent], `size.width`) is LEFT-origin regardless of
 * layout direction, so anything derived from it must be applied unmirrored -- the lens uses
 * [AbsoluteAlignment.TopLeft] + `absoluteOffset` (the same rule the TV `AreSegmentedControl`
 * documents), and the scroll target is mirrored explicitly because `horizontalScroll`'s value is
 * measured from the *reading* start, which in RTL is the right edge.
 */
@Composable
fun <T> AreTabs(
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    scrollable: Boolean = false,
    badge: (@Composable (T) -> Unit)? = null,
) {
    val colors = AreIptvTheme.colors
    val shape = RoundedCornerShape(AreIptvTheme.radius.pill)
    val density = LocalDensity.current
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    // Measured segment bounds in dp, keyed by index. A SnapshotStateMap is fine here -- a tab strip
    // holds a handful of segments, not a 40-tile grid.
    val bounds = remember(options.size) { mutableStateMapOf<Int, Pair<Dp, Dp>>() }
    // Width of the segment Row itself -- the coordinate space `bounds` is expressed in. Needed to
    // mirror a left-origin x into an RTL scroll offset.
    var stripWidth by remember(options.size) { mutableStateOf(0.dp) }
    val selectedIndex = options.indexOf(selected).coerceAtLeast(0)
    val lens = bounds[selectedIndex]
    val scrollState = rememberScrollState()

    val lensX by animateDpAsState(
        targetValue = lens?.first ?: 0.dp,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
        label = "are-tabs-lens-x",
    )
    val lensW by animateDpAsState(
        targetValue = lens?.second ?: 0.dp,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
        label = "are-tabs-lens-w",
    )

    if (scrollable) {
        LaunchedEffect(selectedIndex, lens, isRtl, stripWidth) {
            val b = bounds[selectedIndex] ?: return@LaunchedEffect
            // `ScrollState.value` counts from the reading START (right edge in RTL) while `b.first`
            // counts from the left, so in RTL the target is the segment's distance from the strip's
            // RIGHT edge. Feeding the raw left-origin x scrolled to the mirror-image segment.
            val fromStart = if (isRtl) stripWidth - b.first - b.second else b.first
            scrollState.animateScrollTo(with(density) { fromStart.toPx() }.toInt().coerceAtLeast(0))
        }
    }

    Box(
        modifier = modifier
            .height(TrackHeight)
            .then(
                if (AreIptvTheme.isDark) {
                    Modifier.glassTrack(shape)
                } else {
                    // The glass hairline is a light-on-dark sheen (white .70 along the top edge), so
                    // on the off-white light page the track had no readable outline -- the same
                    // defect PosterTile/ChannelTile fixed by swapping in a solid `borderDefault`.
                    Modifier.background(colors.glassTrackTint, shape).border(1.dp, colors.borderDefault, shape)
                },
            )
            .clip(shape)
            .selectableGroup(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .then(if (scrollable) Modifier.horizontalScroll(scrollState) else Modifier.fillMaxWidth())
                .padding(TrackInset),
        ) {
            if (lens != null && lensW > 0.dp) {
                Box(
                    modifier = Modifier
                        // Absolute alignment + absoluteOffset: `lensX` came from positionInParent
                        // and is always measured from the LEFT edge, so the lens must NOT be
                        // mirrored in RTL. `offset` resolves through placeRelative and was landing
                        // the lens on the mirror-image segment in fa/ar.
                        .align(AbsoluteAlignment.TopLeft)
                        .absoluteOffset(x = lensX)
                        .width(lensW)
                        .fillMaxHeight()
                        .glassLens(shape),
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .onGloballyPositioned { coords ->
                        val w = with(density) { coords.size.width.toDp() }
                        if (stripWidth != w) stripWidth = w
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                options.forEachIndexed { index, option ->
                    val isSelected = index == selectedIndex
                    Row(
                        modifier = Modifier
                            .then(if (scrollable) Modifier else Modifier.weight(1f))
                            .fillMaxHeight()
                            .onGloballyPositioned { coords ->
                                val x = with(density) { coords.positionInParent().x.toDp() }
                                val w = with(density) { coords.size.width.toDp() }
                                val prev = bounds[index]
                                if (prev == null || prev.first != x || prev.second != w) bounds[index] = x to w
                            }
                            .areTouch(
                                onClick = { onSelect(option) },
                                shape = shape,
                                role = Role.Tab,
                                minTouchTarget = 48.dp,
                                disableScale = true,
                            )
                            // Role.Tab alone tells TalkBack *what* this is, never *which one is
                            // current*. `selected` is what makes it announce "selected".
                            .semantics { this.selected = isSelected }
                            .padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                    ) {
                        Text(
                            text = label(option),
                            style = AreIptvTheme.typography.label,
                            color = if (isSelected) lensContentColor() else AreIptvTheme.colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (badge != null) badge(option)
                    }
                }
            }
        }
    }
}
