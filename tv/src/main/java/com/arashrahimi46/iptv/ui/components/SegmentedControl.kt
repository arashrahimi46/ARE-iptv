package com.arashrahimi46.iptv.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.theme.TvFocusable
import com.arashrahimi46.iptv.ui.theme.accentGradientBrush
import com.arashrahimi46.iptv.ui.theme.glassBorderBrush
import kotlin.math.roundToInt

/**
 * Segmented control — one glass track holding a small fixed set of options, with the selected one
 * marked by an accent-gradient indicator pill that **slides** between segments (a spring, so it
 * settles smoothly) when the selection changes (design §6b/§6c). D-pad Left/Right moves between
 * segments; OK selects. For long or dynamic option lists (e.g. Guide's channel groups) keep [AreChip].
 *
 * The track is deliberately NOT clipped, so a focused segment's ring/glow and the indicator's soft
 * shadow have room to breathe past the track edge instead of being sliced off. Each segment reports
 * its measured x/width via [onGloballyPositioned] and the indicator animates toward the selected one,
 * so it works with variable-width labels.
 */
@Composable
fun <T> AreSegmentedControl(
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    /**
     * Attached to the **selected** segment. Callers whose screen redirects D-pad entry into this
     * control point their `focusProperties { enter }` here, so arriving from outside lands on the
     * current tab rather than on whichever end the directional search reaches first (in RTL that
     * was the far segment -- Settings opened focused on its last tab).
     */
    selectedFocusRequester: FocusRequester? = null,
) {
    val colors = AreIptvTheme.colors
    val pill = RoundedCornerShape(AreIptvTheme.radius.pill)
    // Measured geometry of each segment (index -> x, width in px), captured post-layout.
    val bounds = remember { mutableStateMapOf<Int, Pair<Float, Float>>() }
    val selectedIndex = options.indexOf(selected)
    val target = bounds[selectedIndex]

    // Spring toward the selected segment for a smooth, premium settle. First composition starts AT
    // the target (initialValue == target) so it appears in place instead of sliding in from x=0.
    val slide = spring<Float>(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
    val animX by animateFloatAsState(target?.first ?: 0f, animationSpec = slide, label = "segmentX")
    val animW by animateFloatAsState(target?.second ?: 0f, animationSpec = slide, label = "segmentW")
    val density = LocalDensity.current

    Box(
        modifier = modifier
            .height(54.dp)
            // Manual fill + border (NOT glassSurface) so the track does NOT clip its children --
            // a focused segment's ring/glow can extend past the edge. Generous padding gives the
            // accent pill room to sit inside the glass track (the "glassy vibe").
            .background(colors.surfaceGlass, pill)
            .border(1.dp, glassBorderBrush(), pill)
            .padding(8.dp),
    ) {
        // The sliding accent-gradient indicator, behind the labels. No shadow: it lives INSIDE the
        // glass track, so a drop shadow reads as a hard smudge -- the gradient alone is the marker.
        if (animW > 0f) {
            Box(
                modifier = Modifier
                    // Absolute alignment + absoluteOffset: the measured x from positionInParent is
                    // always from the left edge, so the indicator must NOT be mirrored in RTL.
                    .align(AbsoluteAlignment.TopLeft)
                    .absoluteOffset { IntOffset(animX.roundToInt(), 0) }
                    .width(with(density) { animW.toDp() })
                    .fillMaxHeight()
                    .background(accentGradientBrush(), pill),
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxHeight().selectableGroup(),
        ) {
            options.forEachIndexed { index, option ->
                val isSelected = option == selected
                TvFocusable(
                    onClick = { onSelect(option) },
                    shape = pill,
                    backgroundColor = Color.Transparent,
                    modifier = Modifier
                        .fillMaxHeight()
                        .then(if (isSelected && selectedFocusRequester != null) Modifier.focusRequester(selectedFocusRequester) else Modifier)
                        .onGloballyPositioned {
                            bounds[index] = it.positionInParent().x to it.size.width.toFloat()
                        },
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
}
