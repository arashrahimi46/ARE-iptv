package com.arashrahimi46.iptv.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.theme.GlassElevation
import com.arashrahimi46.iptv.ui.theme.TvFocusable
import com.arashrahimi46.iptv.ui.theme.accentGradientBrush
import com.arashrahimi46.iptv.ui.theme.glassSurface
import kotlin.math.roundToInt

/**
 * Segmented control — one glass track holding a small fixed set of options, with the selected one
 * marked by an accent-gradient indicator pill that **slides** between segments when the selection
 * changes (design §6b/§6c). D-pad Left/Right moves between segments like any focusable row; OK
 * selects. For long or dynamic option lists (e.g. Guide's channel groups) keep [AreChip] instead.
 *
 * The indicator is a single pill drawn behind transparent segment labels; each segment reports its
 * measured x/width via [onGloballyPositioned] and the indicator animates toward the selected one, so
 * it works with variable-width labels and honours reduced-motion (the theme's motion durations
 * collapse when reduced-motion is on).
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
    val motion = AreIptvTheme.motion
    val pill = RoundedCornerShape(AreIptvTheme.radius.pill)
    // Measured geometry of each segment (index -> x, width in px), captured post-layout.
    val bounds = remember { mutableStateMapOf<Int, Pair<Float, Float>>() }
    val selectedIndex = options.indexOf(selected)
    val target = bounds[selectedIndex]

    // Animate the indicator toward the selected segment. First composition starts AT the target
    // (initialValue == target) so it appears in place instead of sliding in from x=0.
    val animX by animateFloatAsState(
        targetValue = target?.first ?: 0f,
        animationSpec = tween(motion.durBaseMs, easing = motion.easeEmph),
        label = "segmentIndicatorX",
    )
    val animW by animateFloatAsState(
        targetValue = target?.second ?: 0f,
        animationSpec = tween(motion.durBaseMs, easing = motion.easeEmph),
        label = "segmentIndicatorW",
    )
    val density = LocalDensity.current

    Box(
        modifier = modifier
            .height(46.dp)
            // Track has no shadow of its own (shadow = false) -- the floating indicator carries the lift.
            .glassSurface(pill, shadow = false)
            .padding(4.dp),
    ) {
        // The sliding accent-gradient indicator, behind the labels.
        if (animW > 0f) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(animX.roundToInt(), 0) }
                    .width(with(density) { animW.toDp() })
                    .fillMaxHeight()
                    .shadow(GlassElevation, pill)
                    .background(accentGradientBrush(), pill),
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.selectableGroup(),
        ) {
            options.forEachIndexed { index, option ->
                val isSelected = option == selected
                TvFocusable(
                    onClick = { onSelect(option) },
                    shape = pill,
                    backgroundColor = Color.Transparent,
                    modifier = Modifier.onGloballyPositioned {
                        bounds[index] = it.positionInParent().x to it.size.width.toFloat()
                    },
                ) { _, _ ->
                    Box(
                        modifier = Modifier.fillMaxHeight().padding(horizontal = 20.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        androidx.tv.material3.Text(
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
