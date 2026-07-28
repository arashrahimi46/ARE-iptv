package com.arashrahimi46.iptv.ui.player

import android.view.ViewGroup
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Text
import com.arashrahimi46.iptv.R
import com.arashrahimi46.iptv.data.settings.MiniPlayerBehavior
import com.arashrahimi46.iptv.ui.components.AreBadge
import com.arashrahimi46.iptv.ui.components.AreBadgeTone
import com.arashrahimi46.iptv.ui.components.AreButton
import com.arashrahimi46.iptv.ui.components.AreButtonVariant
import com.arashrahimi46.iptv.ui.components.AreDialog
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import kotlin.math.roundToInt

/** How long OK must be held on the mini-player to open the Stop & close dialog (vs a short press
 * that expands to fullscreen). */
private const val MINI_LONG_PRESS_MS = 500L

/** Inset of the docked mini from the screen edges; also the horizontal inset at both dodge corners. */
private val MINI_PADDING = 28.dp

/** How long an obstruction must hold before the mini commits to dodging (or returning). Filters out
 * the transient overlap a scroll container creates as it drags the focused item past the corner. */
private const val MINI_DODGE_DEBOUNCE_MS = 200L

/**
 * The in-app corner mini-player: a small, always-selectable live video docked bottom-right that
 * keeps playing while the user browses any tab (rendered by the shell over all tab content). It
 * binds a [PlayerView] to the shared [LivePlaybackController.player] so playback is the exact same
 * instance the fullscreen player handed off -- no re-buffer.
 *
 * Remote model (the user's choice): D-pad focuses it like a tile; a short OK press expands it back
 * to fullscreen (via [onExpand]); a long OK press opens a focus-trapping Stop & close dialog.
 * Renders nothing unless a live channel is currently docked.
 *
 * Anti-occlusion (the user's choice, per-user setting): when browsing focus lands on content behind
 * its home (bottom-right) slot, [MiniPlayerBehavior.DODGE] slides it to the bottom-left and back
 * home once focus leaves; [MiniPlayerBehavior.FADE] instead fades+shrinks it while it isn't focused.
 * [focusedContentBounds] is the shell-reported rect (root coords) of whatever tab content currently
 * holds focus -- null when nothing in the tabs is focused (e.g. the mini itself is).
 */
@Composable
fun LiveMiniPlayerOverlay(
    controller: LivePlaybackController,
    onExpand: (channelId: Long) -> Unit,
    modifier: Modifier = Modifier,
    behavior: MiniPlayerBehavior = MiniPlayerBehavior.DODGE,
    focusedContentBounds: Rect? = null,
    reducedMotion: Boolean = false,
    // Hoisted by the shell so its ancestor key handler can jump focus back to the docked mini
    // (the remote's Window/PiP key) from anywhere -- the tab grids otherwise trap D-pad focus.
    focusRequester: FocusRequester = remember { FocusRequester() },
) {
    val session = controller.session
    if (controller.mode != LiveMiniMode.Mini || controller.player == null || session == null) return

    val context = LocalContext.current
    var showStopDialog by remember { mutableStateOf(false) }
    var downAt by remember { mutableStateOf(0L) }
    // True once the current OK press was already resolved as a long-press (via the framework's
    // long-press flag), so the following KeyUp doesn't also fire the short-press expand.
    var longFired by remember { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val colors = AreIptvTheme.colors

    // Grab focus the moment the mini docks (and whenever the docked channel changes) so it's
    // immediately actionable -- pressing any arrow then simply moves into the tab content to browse.
    LaunchedEffect(session.channelId) {
        kotlinx.coroutines.delay(150)
        runCatching { focusRequester.requestFocus() }
    }

    // Clamp the docked aspect ratio to something sane (odd streams -> 16:9), same spirit as PiP.
    val ratio = (controller.videoWidth.toFloat() / controller.videoHeight).let {
        if (it < 0.5f || it > 2.4f) 16f / 9f else it
    }

    // Anti-occlusion geometry. We test the focused content rect against the mini's HOME (bottom-right)
    // slot -- computed from the measured container rect + mini size, so the test stays stable even
    // after the mini has slid away (it never re-evaluates against its dodged position).
    val padPx = with(LocalDensity.current) { MINI_PADDING.toPx() }
    // The mini's home is Alignment.BottomEnd, which mirrors to the bottom-LEFT in RTL. The
    // obstruction test must check that same corner, so the home slot flips with layout direction --
    // otherwise (RTL) it tested the far bottom-right corner and dodged the mini INTO focused content.
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    var containerRect by remember { mutableStateOf<Rect?>(null) }
    var miniSize by remember { mutableStateOf(IntSize.Zero) }
    val homeRect = containerRect?.takeIf { miniSize != IntSize.Zero }?.let { c ->
        val bottom = c.bottom - padPx
        if (isRtl) {
            val left = c.left + padPx
            Rect(left, bottom - miniSize.height, left + miniSize.width, bottom)
        } else {
            val right = c.right - padPx
            Rect(right - miniSize.width, bottom - miniSize.height, right, bottom)
        }
    }
    val obstructed = focusedContentBounds != null && homeRect != null && homeRect.overlaps(focusedContentBounds)

    // Debounce the obstruction: a scroll container briefly drags the focused item THROUGH the mini's
    // corner before settling it elsewhere, which would otherwise start a dodge and snap it back (a
    // visible twitch). Only commit once the state has held for [MINI_DODGE_DEBOUNCE_MS] -- a passing
    // overlap flips back before the delay elapses (the effect restarts), so it never commits.
    var stableObstructed by remember { mutableStateOf(false) }
    LaunchedEffect(obstructed) {
        kotlinx.coroutines.delay(MINI_DODGE_DEBOUNCE_MS)
        stableObstructed = obstructed
    }

    // DODGE: slide left to the bottom-left corner while obstructed; return home otherwise.
    val dodged = behavior == MiniPlayerBehavior.DODGE && stableObstructed
    val targetOffsetX = if (dodged) containerRect?.let { -(it.width - miniSize.width - 2 * padPx) } ?: 0f else 0f
    // FADE: dim + shrink whenever the mini itself isn't focused (i.e. the user is browsing elsewhere).
    val faded = behavior == MiniPlayerBehavior.FADE && !focused
    val targetAlpha = if (faded) 0.5f else 1f
    val targetScale = if (faded) 0.85f else 1f

    // NOT `by`: these States are read inside the `offset`/`graphicsLayer` lambdas below, i.e. at
    // LAYOUT and DRAW time. Resolving them into plain Floats here would defeat that -- the lambdas
    // would capture already-settled values and every frame of all three tweens would recompose this
    // composable, which owns an AndroidView/PlayerView factory block, a badge and a title. And the
    // FADE tween fires on losing focus, i.e. exactly while the user is D-pad browsing behind it.
    val animOffsetX = animateFloatAsState(targetOffsetX, label = "miniDodgeX")
    val animAlpha = animateFloatAsState(targetAlpha, label = "miniAlpha")
    val animScale = animateFloatAsState(targetScale, label = "miniScale")

    Box(modifier = modifier.fillMaxSize().onGloballyPositioned { containerRect = it.boundsInRoot() }) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(MINI_PADDING)
                .fillMaxWidth(0.25f)
                .aspectRatio(ratio)
                .onGloballyPositioned { miniSize = it.size }
                .offset {
                    IntOffset((if (reducedMotion) targetOffsetX else animOffsetX.value).roundToInt(), 0)
                }
                .graphicsLayer {
                    this.alpha = if (reducedMotion) targetAlpha else animAlpha.value
                    val s = if (reducedMotion) targetScale else animScale.value
                    scaleX = s
                    scaleY = s
                }
                .clip(RoundedCornerShape(AreIptvTheme.radius.lg))
                .background(Color.Black)
                .border(
                    width = if (focused) 3.dp else 1.dp,
                    color = if (focused) colors.accent else colors.borderStrong,
                    shape = RoundedCornerShape(AreIptvTheme.radius.lg),
                )
                .focusRequester(focusRequester)
                .focusable(interactionSource = interaction)
                .onPreviewKeyEvent { event ->
                    when (event.key) {
                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                            when (event.type) {
                                KeyEventType.KeyDown -> {
                                    if (downAt == 0L) downAt = System.currentTimeMillis()
                                    // The framework flags a held OK (and adb --longpress) as a long
                                    // press on the DOWN event -- open the Stop dialog immediately.
                                    if (event.nativeKeyEvent.isLongPress) {
                                        longFired = true
                                        showStopDialog = true
                                    }
                                }
                                KeyEventType.KeyUp -> {
                                    val held = System.currentTimeMillis() - downAt
                                    downAt = 0L
                                    when {
                                        longFired -> longFired = false // handled on DOWN; don't expand
                                        held >= MINI_LONG_PRESS_MS -> showStopDialog = true
                                        else -> onExpand(session.channelId)
                                    }
                                }
                                else -> {}
                            }
                            true
                        }
                        else -> false
                    }
                },
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = {
                    PlayerView(context).apply {
                        useController = false
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                        player = controller.player
                    }
                },
                // Rebind if the docked instance changes (e.g. a new channel is minimized while this
                // overlay stays composed); detach on teardown so the view never holds a released player.
                update = { view -> view.player = controller.player },
                onRelease = { view -> view.player = null },
            )

            // Small label strip: LIVE badge + channel title, bottom-anchored over the video.
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AreBadge(stringResource(R.string.player_live_badge), tone = AreBadgeTone.Live)
                Text(
                    text = session.title,
                    style = AreIptvTheme.typography.caption,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }

    if (showStopDialog) {
        val cancelFocus = remember { FocusRequester() }
        Dialog(
            onDismissRequest = { showStopDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
        ) {
            LaunchedEffect(Unit) { runCatching { cancelFocus.requestFocus() } }
            AreDialog(
                onDismiss = { showStopDialog = false },
                title = stringResource(R.string.mini_player_stop_title),
                actions = {
                    AreButton(
                        stringResource(R.string.action_cancel),
                        onClick = { showStopDialog = false },
                        variant = AreButtonVariant.Ghost,
                        modifier = Modifier.focusRequester(cancelFocus),
                    )
                    AreButton(
                        stringResource(R.string.mini_player_stop),
                        onClick = {
                            showStopDialog = false
                            controller.closeMini()
                        },
                        variant = AreButtonVariant.Primary,
                    )
                },
            ) {
                Text(
                    text = stringResource(R.string.mini_player_stop_body, session.title),
                    style = AreIptvTheme.typography.body,
                    color = colors.textSecondary,
                )
            }
        }
    }
}
