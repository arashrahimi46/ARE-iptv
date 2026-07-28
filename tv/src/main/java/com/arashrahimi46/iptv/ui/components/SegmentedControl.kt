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
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.theme.ControlTone
import com.arashrahimi46.iptv.ui.theme.TvFocusable
import com.arashrahimi46.iptv.ui.theme.controlSkin
import com.arashrahimi46.iptv.ui.theme.glassBorderBrush
import kotlin.math.roundToInt

/**
 * Segmented control — one glass track holding a small fixed set of options, with the selected one
 * marked by a glass-lens indicator pill that **slides** between segments (a spring, so it
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
    // ONE funnel for the sliding indicator + labels (see ControlSkin.kt): the indicator is the
    // "selected" accent lens (§6.2) and unselected labels take the neutral content colour, so a
    // change to the lens recipe reaches this control too. The slide/spring geometry below is
    // untouched -- only the *appearance* is sourced from the funnel.
    val lensSkin = controlSkin(ControlTone.Neutral, selected = true)
    val restSkin = controlSkin(ControlTone.Neutral, selected = false)
    // Measured geometry of each segment (index -> x, width in px), captured post-layout.
    val bounds = remember { mutableStateMapOf<Int, Pair<Float, Float>>() }
    val selectedIndex = options.indexOf(selected)
    val target = bounds[selectedIndex]

    // Spring toward the selected segment for a smooth, premium settle. First composition starts AT
    // the target (initialValue == target) so it appears in place instead of sliding in from x=0.
    val slide = spring<Float>(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
    // Neither is `by`: both are read inside layout-time lambdas below. `animX` always was; `animW`
    // was resolved into composition scope (an `if` guard plus `Modifier.width(Dp)`), so every frame
    // of the spring's long settle recomposed the track, the lens and the whole `forEachIndexed` over
    // the segments -- N x TvFocusable + Text, on four call sites the user hits constantly.
    val animX = animateFloatAsState(target?.first ?: 0f, animationSpec = slide, label = "segmentX")
    val animW = animateFloatAsState(target?.second ?: 0f, animationSpec = slide, label = "segmentW")

    Box(
        modifier = modifier
            .height(54.dp)
            // Manual fill + border (NOT glassSurface) so the track does NOT clip its children --
            // a focused segment's ring/glow can extend past the edge. Generous padding gives the
            // lens pill room to sit inside the glass track (the "glassy vibe").
            .background(colors.surfaceGlass, pill)
            .border(1.dp, glassBorderBrush(), pill)
            .padding(8.dp),
    ) {
        // The sliding glass-lens indicator, behind the labels. No shadow: it lives INSIDE the glass
        // track, so a drop shadow reads as a hard smudge -- the lens (more glass + a brighter rim,
        // not opaque paint) is the marker.
        // Guarded on the MEASURED bounds, not on the animated width: `target != null` is the same
        // "geometry is known yet?" question and it only changes on layout, where `animW > 0f` changed
        // on every spring frame.
        if (target != null) {
            Box(
                modifier = Modifier
                    // Absolute alignment + absoluteOffset: the measured x from positionInParent is
                    // always from the left edge, so the indicator must NOT be mirrored in RTL.
                    .align(AbsoluteAlignment.TopLeft)
                    .absoluteOffset { IntOffset(animX.value.roundToInt(), 0) }
                    // Width fixed at MEASURE time from the animated State, so a spring frame
                    // invalidates one node's measurement instead of recomposing the control.
                    .layout { measurable, constraints ->
                        val w = animW.value.roundToInt().coerceAtLeast(0)
                        val placeable = measurable.measure(constraints.copy(minWidth = w, maxWidth = w))
                        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                    }
                    .fillMaxHeight()
                    .then(
                        if (lensSkin.fillBrush != null) Modifier.background(lensSkin.fillBrush, pill)
                        else Modifier.background(lensSkin.fillColor, pill),
                    )
                    .then(
                        when {
                            lensSkin.borderBrush != null -> Modifier.border(1.dp, lensSkin.borderBrush, pill)
                            lensSkin.borderColor != null -> Modifier.border(1.dp, lensSkin.borderColor, pill)
                            else -> Modifier
                        },
                    ),
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
                            color = if (isSelected) lensSkin.content else restSkin.content,
                        )
                    }
                }
            }
        }
    }
}
