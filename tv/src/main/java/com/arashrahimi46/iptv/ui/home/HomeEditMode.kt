package com.arashrahimi46.iptv.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.arashrahimi46.iptv.ui.components.AreIconButton
import com.arashrahimi46.iptv.ui.components.AreIconButtonSize
import com.arashrahimi46.iptv.ui.components.AreIconButtonVariant
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme

/** Human-readable label for a Home section, used by [HomeSectionEditRow] and (for a built-in
 * section) matching the rail title it renders with the Customize button off. */
fun homeSectionLabel(section: HomeSection): String = when (section) {
    is HomeSection.Builtin -> when (section.key) {
        BuiltinSection.CONTINUE_WATCHING -> "Continue Watching"
        BuiltinSection.LIVE_NOW -> "Live now"
        BuiltinSection.CATEGORIES -> "Browse by category"
        BuiltinSection.RECOMMENDED -> "Browse movies & series"
        BuiltinSection.MOVIES -> "Movies"
        BuiltinSection.SERIES -> "Series"
    }
    is HomeSection.Category -> section.name
}

/**
 * Edit-mode chrome wrapped around one Home rail (Home layout customization, step 4): a header with
 * grab handle, `position/total` indicator, section label, and a hide/show toggle, over the section's
 * normal content (dimmed when [hidden]).
 *
 * The grab handle (icon + position) is its OWN small focus target, deliberately separate from the
 * hide/show [AreIconButton] beside it -- an earlier version made the entire row one giant
 * `.focusable()`, which swallowed DPAD_RIGHT (there was nothing "to the right" of a full-width
 * focused node for the focus system to land on) so the eye button was never reachable and SELECT
 * always grabbed instead of toggling. Two normal sibling focus targets in the header [Row] fixes
 * that -- Left/Right moves between them like any other row of controls.
 *
 * Grab -> move -> drop: focusing the grab handle and pressing SELECT/ENTER grabs it
 * ([onGrabToggle]); while [grabbed], DPAD_UP/DOWN reorder the section ([onMove]) instead of moving
 * D-pad focus, and SELECT/ENTER or BACK drops it ([onDrop]), persisting the new order. The handle
 * intercepts these keys via [Modifier.onPreviewKeyEvent] -- same idiom as
 * [com.arashrahimi46.iptv.ui.player.LivePlayerScreen]'s channel-switch key handling -- so they're
 * consumed before the focus system can treat DPAD_UP/DOWN as ordinary focus travel. Only the KeyUp
 * phase is handled (mirroring that same reference), so a still-held key doesn't repeat the move.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HomeSectionEditRow(
    label: String,
    position: Int,
    total: Int,
    hidden: Boolean,
    grabbed: Boolean,
    focusRequester: FocusRequester,
    onGrabToggle: () -> Unit,
    onMove: (delta: Int) -> Unit,
    onDrop: () -> Unit,
    onToggleHidden: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = AreIptvTheme.colors
    val handleShape = RoundedCornerShape(AreIptvTheme.radius.sm)
    var handleFocused by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AreIptvTheme.spacing.safeX)
                .padding(bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .onFocusChanged { handleFocused = it.isFocused }
                    .focusable()
                    .clip(handleShape)
                    .then(
                        when {
                            grabbed -> Modifier
                                .background(colors.accentWash)
                                .border(2.dp, colors.accent, handleShape)
                            handleFocused -> Modifier
                                .background(colors.surface3)
                                .border(2.dp, colors.borderStrong, handleShape)
                            else -> Modifier
                        },
                    )
                    .onPreviewKeyEvent { keyEvent ->
                        if (keyEvent.type != KeyEventType.KeyUp) {
                            false
                        } else {
                            when (keyEvent.key) {
                                Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                                    if (grabbed) onDrop() else onGrabToggle()
                                    true
                                }
                                Key.Back -> {
                                    if (grabbed) {
                                        onDrop()
                                        true
                                    } else {
                                        false
                                    }
                                }
                                Key.DirectionUp -> {
                                    if (grabbed) {
                                        onMove(-1)
                                        true
                                    } else {
                                        false
                                    }
                                }
                                Key.DirectionDown -> {
                                    if (grabbed) {
                                        onMove(1)
                                        true
                                    } else {
                                        false
                                    }
                                }
                                else -> false
                            }
                        }
                    }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.Filled.DragHandle,
                    contentDescription = if (grabbed) "Grabbed -- Up/Down to move, OK to drop" else "Grab to reorder",
                    tint = if (grabbed) colors.accent else colors.textTertiary,
                )
                Text(text = "${position + 1}/$total", style = AreIptvTheme.typography.caption, color = colors.textTertiary)
            }
            Text(text = label, style = AreIptvTheme.typography.label, color = colors.textPrimary, modifier = Modifier.weight(1f))
            AreIconButton(
                icon = if (hidden) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                contentDescription = if (hidden) "Show section" else "Hide section",
                onClick = onToggleHidden,
                variant = AreIconButtonVariant.Ghost,
                size = AreIconButtonSize.Small,
            )
        }
        Box(modifier = if (hidden) Modifier.alpha(0.35f) else Modifier) {
            content()
        }
    }
}
