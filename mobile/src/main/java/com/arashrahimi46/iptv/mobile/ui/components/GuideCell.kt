package com.arashrahimi46.iptv.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arashrahimi46.iptv.core.R as CoreR
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.theme.glassBorderBrush

/**
 * One programme in the Guide. The phone Guide is a vertical AGENDA list, not a time-axis grid, so
 * the cell fills whatever width the caller hands it -- the TV `width` (proportional to duration)
 * and `onFocusChange` (which drove the sticky focused-programme info bar) are both gone.
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
    onLongClick: (() -> Unit)? = null,
) {
    val colors = AreIptvTheme.colors
    val shape = RoundedCornerShape(AreIptvTheme.radius.sm)
    val liveDesc = stringResource(CoreR.string.badge_live)
    val catchupDesc = stringResource(CoreR.string.guide_catchup_available_desc)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .areTouch(
                onClick = onClick,
                shape = shape,
                onLongClick = onLongClick,
                backgroundColor = if (now) colors.accentWash else colors.surfaceGlass,
                // Every cell keeps an edge so the blocks stay distinct on the near-white light
                // page; the on-now cell takes an accent edge and becomes the focal point.
                borderColor = if (now) colors.accent else null,
                borderBrush = if (now) null else glassBorderBrush(),
                minTouchTarget = 0.dp,
                disableScale = true,
            )
            .heightIn(min = 64.dp),
    ) {
        if (now) {
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .width(3.dp)
                    .background(colors.accent),
            )
        }
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = time,
                    style = AreIptvTheme.typography.mono,
                    color = if (now) colors.accentHover else colors.textTertiary,
                )
                // WCAG 1.4.1: these are colour/glyph-only status cues, so each carries the same
                // status as a contentDescription.
                if (live) {
                    Box(
                        Modifier
                            .padding(start = 8.dp)
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
            Box(Modifier.height(4.dp))
            Text(
                text = title,
                style = AreIptvTheme.typography.label,
                color = colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (now && progress > 0f) {
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(2.dp)
                    .background(colors.accent),
            )
        }
    }
}
