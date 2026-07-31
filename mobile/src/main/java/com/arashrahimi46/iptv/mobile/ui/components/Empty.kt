package com.arashrahimi46.iptv.mobile.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arashrahimi46.iptv.core.R as CoreR
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.theme.LocalReducedMotion

/** "Nothing here" -- with an action when there is one the user can actually take. */
@Composable
fun AreEmptyState(
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    icon: ImageVector? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val colors = AreIptvTheme.colors
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = colors.textTertiary, modifier = Modifier.size(40.dp))
        }
        Text(
            text = title,
            style = AreIptvTheme.typography.h3,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        if (message != null) {
            Text(
                text = message,
                style = AreIptvTheme.typography.body,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )
        }
        if (actionLabel != null && onAction != null) {
            AreButton(text = actionLabel, onClick = onAction, variant = AreButtonVariant.Primary)
        }
    }
}

/** A failed load, with the retry the user needs. */
@Composable
fun AreErrorState(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    AreEmptyState(
        title = message,
        modifier = modifier,
        icon = Icons.Filled.ErrorOutline,
        actionLabel = if (onRetry != null) stringResource(CoreR.string.action_retry) else null,
        onAction = onRetry,
    )
}

/** A determinate-free wait. Prefer [AreSkeleton] for list/grid loads -- a spinner says less. */
@Composable
fun AreLoadingState(modifier: Modifier = Modifier, message: String? = null) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        CircularProgressIndicator(color = AreIptvTheme.colors.accent, strokeWidth = 3.dp)
        if (message != null) {
            Text(
                text = message,
                style = AreIptvTheme.typography.body,
                color = AreIptvTheme.colors.textSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * Shimmering placeholder block for grid/rail skeletons. Static under reduced motion -- an infinite
 * animation is exactly what that setting exists to stop.
 */
@Composable
fun AreSkeleton(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(AreIptvTheme.radius.lg),
) {
    val tint = AreIptvTheme.colors.glassTrackTint
    val reduced = LocalReducedMotion.current
    if (reduced) {
        Box(modifier.background(tint, shape))
        return
    }
    val transition = rememberInfiniteTransition(label = "are-skeleton")
    val alpha = transition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "are-skeleton-alpha",
    )
    // `alpha.value` is read at DRAW time inside graphicsLayer so the shimmer costs a layer property
    // and not a recomposition of every skeleton in the grid, 60 times a second.
    Box(modifier.graphicsLayer { this.alpha = alpha.value }.background(tint, shape))
}
