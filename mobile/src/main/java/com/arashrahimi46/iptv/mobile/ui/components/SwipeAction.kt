package com.arashrahimi46.iptv.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.arashrahimi46.iptv.mobile.design.AreIptvTheme

/**
 * Swipe a row away, end-to-start only (dragging the other way on an RTL locale still means "away",
 * because `EndToStart` is direction-aware).
 *
 * This fires [onDismissed] immediately -- the CALLER owns the undo snackbar and the re-insert. That
 * split is deliberate: only the screen knows what "undo" means for its data, and a component that
 * held the row back waiting for a decision would have to own a timer.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AreSwipeToDismissRow(
    onDismissed: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Filled.Delete,
    label: String? = null,
    background: Color? = null,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colors = AreIptvTheme.colors
    val plate = background ?: colors.danger
    val haptics = LocalHapticFeedback.current
    val state = rememberSwipeToDismissBoxState(
        // 45% of the row's width, not Material's default 50% -- a one-handed thumb runs out of
        // travel before the middle of a phone screen.
        positionalThreshold = { total -> total * 0.45f },
    )

    LaunchedEffect(state) {
        snapshotFlow { state.targetValue }.collect { target ->
            if (target == SwipeToDismissBoxValue.EndToStart) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        }
    }
    LaunchedEffect(state.currentValue) {
        if (state.currentValue == SwipeToDismissBoxValue.EndToStart) {
            onDismissed()
            state.snapTo(SwipeToDismissBoxValue.Settled)
        }
    }

    SwipeToDismissBox(
        state = state,
        modifier = modifier,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = enabled,
        backgroundContent = {
            Box(
                modifier = Modifier.fillMaxSize().background(plate).padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                androidx.compose.foundation.layout.Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxHeight(),
                ) {
                    Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                    if (label != null) {
                        Text(text = label, style = AreIptvTheme.typography.label, color = Color.White)
                    }
                }
            }
        },
        content = { content() },
    )
}
