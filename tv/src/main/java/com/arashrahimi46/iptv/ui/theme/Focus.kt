package com.arashrahimi46.iptv.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.delay
import androidx.compose.ui.focus.FocusRequester
import com.arashrahimi46.iptv.ui.interaction.AreInteractiveBinding
import com.arashrahimi46.iptv.ui.interaction.AreInteractiveSurface
import androidx.compose.ui.Modifier
import android.graphics.BlurMaskFilter
import android.graphics.Paint as NativePaint
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The mandatory TV focus treatment: accent ring + glow + scale-up, per the
 * design system's "no element should ever be focusable without an
 * indicator" rule (tokens/base.css `:focus-visible`). Every focusable
 * component in the library composes with this modifier or [TvFocusable].
 *
 * Respects [LocalReducedMotion]: durations/scale collapse to the reduced
 * set when the CompositionLocal resolves true.
 *
 * @param interactionSource drives the focused/pressed state this modifier animates from.
 * @param shape corner shape for the ring + glow shadow (should match the surface it wraps).
 * @param glowColor tint for the ring/shadow — defaults to the theme's focus-ring color.
 * @param ringWidth stroke width of the focus ring (design system uses 3dp/2dp variants).
 * @param disableScale set true for controls (e.g. Switch) whose own thumb/track already animate.
 * @param ownsFocusable set false when a descendant composable (e.g. [androidx.compose.foundation.text.BasicTextField])
 *   already registers the focus target for this [interactionSource] -- applying `.focusable()` again here would
 *   create a second, nested focus node and break single-press D-pad focus travel. The visuals (ring/glow/scale)
 *   still track the shared [interactionSource] either way.
 */
@Composable
fun Modifier.tvFocusable(
    interactionSource: MutableInteractionSource,
    shape: Shape = RoundedCornerShape(AreIptvTheme.radius.md),
    glowColor: Color = AreIptvTheme.colors.focusRing,
    ringWidth: Dp = 3.dp,
    disableScale: Boolean = false,
    ownsFocusable: Boolean = true,
    /**
     * Set FALSE when the caller draws the ring itself as a sibling overlay ([TvFocusable] does).
     *
     * PERF, and why that overlay exists at all: [focusRingCached] wraps the element's own draw
     * (`onDrawWithContent { drawContent(); ring }`), so animating its alpha invalidates and
     * **re-records the whole element's display list on every tween frame** -- poster bitmap and all,
     * on both the tile losing focus and the one gaining it, for every D-pad step. Drawn instead as a
     * sibling with its own graphics layer, the fade is a RenderNode alpha property and nothing is
     * re-recorded. Measured on the XL95: `AndroidOwner:draw` 112 -> 61 ms (-45%), with the fade
     * intact (snapping the alpha instead only reached 71 ms). Screenshot-verified: differences are
     * confined to anti-aliasing along the 3dp stroke.
     *
     * Kept true for [com.arashrahimi46.iptv.ui.components.AreTextField], which is a single control
     * rather than one of forty tiles and has no BoxScope to hang an overlay in.
     */
    drawRing: Boolean = true,
): Modifier {
    val motion = AreIptvTheme.motion
    val focused by interactionSource.collectIsFocusedAsState()
    val pressed by interactionSource.collectIsPressedAsState()

    val targetScale = when {
        disableScale -> 1f
        pressed -> motion.pressScale
        focused -> motion.focusScale
        else -> 1f
    }
    // Both focus tweens are held as STATE and read inside the graphicsLayer / focus-ring lambdas
    // below, never here.
    //
    // PERF: this used to be `val scale by animateFloatAsState(...)`. The `by` delegate reads `.value`
    // during COMPOSITION, so every one of the ~8-15 tween frames recomposed this function and rebuilt
    // the whole modifier chain -- on BOTH the element losing focus and the one gaining it, for every
    // single D-pad step. (A previous pass believed it had fixed this by passing the ring alpha as a
    // lambda, `{ ringAlpha }` -- but `ringAlpha` was already an unwrapped Float by then, so the lambda
    // captured a value, not a state read, and changed nothing.) Deferring both reads to draw/layer
    // time is worth 18% of Compose:recompose and 6% of main-thread Running on the XL95, with no
    // visual change whatsoever.
    val scaleState = animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(durationMillis = motion.durFastMs, easing = motion.easeEmph),
        label = "tvFocusScale",
    )
    val ringAlphaState = animateFloatAsState(
        targetValue = if (focused) 1f else 0f,
        animationSpec = tween(durationMillis = motion.durFastMs, easing = motion.easeOut),
        label = "tvFocusRingAlpha",
    )

    return this
        .then(if (ownsFocusable) Modifier.focusable(interactionSource = interactionSource) else Modifier)
        .graphicsLayer {
            val sc = scaleState.value
            scaleX = sc
            scaleY = sc
        }
        // Design system `--focus-glow-tight` was `0 0 0 3px ring, 0 0 22px glow`. The GLOW half is
        // gone: product call -- the blue halo around a focused switch/button read as a smudge, and it
        // was the last per-frame CPU Gaussian blur in the focus path. The crisp ring alone carries
        // focus (so the "never focusable without an indicator" rule still holds).
        //
        // PERF: the ring is ALWAYS in the modifier chain (alpha 0 when unfocused), not conditionally
        // added/removed. Toggling it in/out on every focus change forced a relayout each time -- the
        // stutter felt moving the sidebar selection and travelling focus on Home.
        //
        // It takes `alpha` as a LAMBDA that reads the State directly, so the animated value is read
        // at DRAW time (see the note on the tweens above for why the lambda alone was not enough).
        // This used to be `.border(color = glowColor.copy(alpha = ringAlpha))`, which additionally
        // minted a fresh Color and border node per tween frame.
        .then(
            if (drawRing) Modifier.focusRingCached(glowColor, shape, ringWidth) { ringAlphaState.value }
            else Modifier,
        )
}


/**
 * The crisp focus ring, drawn at DRAW time from an [alpha] lambda -- a drop-in replacement for
 * `Modifier.border(width, color.copy(alpha = animated), shape)` that doesn't invalidate composition
 * as the value animates.
 *
 * Geometry deliberately mirrors [androidx.compose.foundation.border]: the stroke is inset by half its
 * width so the ring sits entirely INSIDE the element's bounds (centring it on the outline instead
 * would fatten every focused element by half a stroke and shift the layout's optical edge).
 */
fun Modifier.focusRingCached(
    color: Color,
    shape: Shape,
    width: Dp,
    alpha: () -> Float,
): Modifier = this.drawWithCache {
    val strokePx = width.toPx()
    val half = strokePx / 2f
    val stroke = Stroke(width = strokePx)
    val outline = shape.createOutline(size, layoutDirection, this)
    val rounded = (outline as? Outline.Rounded)?.roundRect
    val rect = (outline as? Outline.Rectangle)?.rect
    val generic = (outline as? Outline.Generic)?.path
    onDrawWithContent {
        drawContent()
        val a = alpha()
        if (a <= 0f) return@onDrawWithContent
        when {
            rounded != null -> drawRoundRect(
                color = color,
                alpha = a,
                topLeft = Offset(rounded.left + half, rounded.top + half),
                size = Size(
                    (rounded.width - strokePx).coerceAtLeast(0f),
                    (rounded.height - strokePx).coerceAtLeast(0f),
                ),
                cornerRadius = CornerRadius(
                    (rounded.topLeftCornerRadius.x - half).coerceAtLeast(0f),
                    (rounded.topLeftCornerRadius.y - half).coerceAtLeast(0f),
                ),
                style = stroke,
            )
            rect != null -> drawRect(
                color = color,
                alpha = a,
                topLeft = Offset(rect.left + half, rect.top + half),
                size = Size(
                    (rect.width - strokePx).coerceAtLeast(0f),
                    (rect.height - strokePx).coerceAtLeast(0f),
                ),
                style = stroke,
            )
            // A generic outline can't be inset analytically; stroke it in place, as border does.
            generic != null -> drawPath(generic, color, alpha = a, style = stroke)
        }
    }
}

// Modifier.tvGlow moved to :core (ui/theme/Glass.kt) in the Are* component migration -- it is a
// pure rendering primitive (no D-pad/focus dependency) shared by components now living in :core
// (Badge, StepIndicator) as well as ones still here (ContinueCard, StreamHealth).

/**
 * Restores D-pad focus to the exact tile that started playback when a screen
 * re-enters composition -- most notably pressing Back out of the player, which
 * otherwise leaves focus on the sidebar (the shell's default initial focus)
 * instead of the channel tile the user just came from. Callers hold the "last
 * played" id themselves via `rememberSaveable` (which survives the origin
 * screen being paused while the player destination is pushed on top, since it
 * stays on the back stack rather than being popped) and pass it in as
 * [savedId]; once this tile's [itemId] matches, focus is requested once and
 * [onConsumed] clears the saved id so an unrelated later recomposition (e.g.
 * scrolling a lazy rail) doesn't keep re-stealing focus.
 */
@Composable
fun rememberPlaybackFocusRequester(savedId: Long?, itemId: Long, onConsumed: () -> Unit): FocusRequester {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        if (savedId == itemId) {
            // Re-assert focus across ~0.5s of frames. On screen re-entry (Back from the player)
            // the Android focus system assigns default focus to the FIRST focusable -- the
            // sidebar -- and can keep re-winning for several frames as the player surface tears
            // down and paging reloads. Re-requesting until then outlasts that default pass so
            // focus ends on -- and stays on -- the tile the user launched from.
            repeat(30) {
                runCatching { focusRequester.requestFocus() }
                withFrameNanos { }
            }
            onConsumed()
        }
    }
    return focusRequester
}

/**
 * Convenience wrapper composing [AreInteractive] with D-pad focus + a click target, for the
 * common "focusable tile/card/button" pattern used across the media, category and guide
 * components.
 *
 * A thin D-pad-aware layer, not a second copy of the glass rendering: the fill/border/shadow/
 * scale-on-interaction logic all live in [AreInteractive] (`:core`), shared with `:mobile`. What
 * stays here, TV-only, is registering the D-pad focus target ([Modifier.focusable]), the
 * `DPAD_CENTER` key handling `combinedClickable` alone doesn't cover, and the accent focus ring --
 * none of which apply to a touch surface.
 */
@Composable
fun TvFocusable(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = RoundedCornerShape(AreIptvTheme.radius.md),
    glowColor: Color = AreIptvTheme.colors.focusRing,
    backgroundColor: Color = Color.Transparent,
    /** Optional gradient FILL (e.g. [accentGradientBrush] for the selected/current state). Takes
     *  precedence over [backgroundColor] when set -- the one funnel for the accent-gradient chip. */
    backgroundBrush: Brush? = null,
    /** Subtle resting drop shadow (dp of elevation). >0 lifts the surface off the page; used for the
     *  accent-gradient chip and filled buttons so they float (design ask: "subtle hint" depth). */
    shadowElevation: Dp = 0.dp,
    /** Optional 1dp outline drawn over the fill (under the focus ring) -- lets low-contrast
     *  fills (e.g. an unselected chip on a white card in light mode) read as a distinct shape. */
    borderColor: Color? = null,
    /** Optional gradient border (the glass "lit edge"). Takes precedence over [borderColor] when
     *  set, so a glassified component can carry the highlight->faint hairline without forking. */
    borderBrush: Brush? = null,
    enabled: Boolean = true,
    /** Long-press (hold OK ~[LONG_PRESS_MS]) handler; when null the control has short-press only. */
    onLongClick: (() -> Unit)? = null,
    /** Suppress the focus scale-up (ring + glow still track focus). For large tiles -- e.g. the
     *  multi-view video panes -- whose 6% growth would overlap tightly-packed neighbours. */
    disableScale: Boolean = false,
    content: @Composable BoxScope.(focused: Boolean, pressed: Boolean) -> Unit,
) {
    val focused by interactionSource.collectIsFocusedAsState()
    val motionT = AreIptvTheme.motion
    val ringAlphaOverlay = animateFloatAsState(
        targetValue = if (focused) 1f else 0f,
        animationSpec = tween(durationMillis = motionT.durFastMs, easing = motionT.easeOut),
        label = "tvFocusRingOverlay",
    )
    Box(modifier = modifier) {
        AreInteractiveSurface(
            onClick = onClick,
            modifier = Modifier
                .matchParentSize()
                .focusable(interactionSource = interactionSource)
                // combinedClickable() inside AreInteractive handles Enter/NumPadEnter (incl.
                // long-press) but NOT Key.DirectionCenter -- the key every real Android TV /
                // Fire TV remote's physical OK/Select button actually sends. Confirmed missing
                // via a real device-emulator D-pad test (not an emulator artifact): DPAD_CENTER
                // alone did nothing on a fully-focused, freshly-launched card across multiple
                // isolated repro attempts, while KEYCODE_ENTER worked every time. Without this,
                // nothing in the app would be selectable with a real remote's OK button -- fixed
                // once, here, so every TvFocusable-based component in the library inherits it.
                // We resolve short vs. long press from the native event's own down->up span so
                // DPAD_CENTER gets long-press too.
                .onKeyEvent { keyEvent ->
                    if (enabled && keyEvent.type == KeyEventType.KeyUp && keyEvent.key == Key.DirectionCenter) {
                        val heldMs = keyEvent.nativeKeyEvent.eventTime - keyEvent.nativeKeyEvent.downTime
                        if (onLongClick != null && heldMs >= LONG_PRESS_MS) onLongClick() else onClick()
                        true
                    } else {
                        // Swallow DirectionCenter KeyDown so the platform doesn't also fire a
                        // synthetic click on release -- we own the up->down timing above.
                        enabled && keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.DirectionCenter
                    }
                },
            interactionSource = interactionSource,
            shape = shape,
            backgroundColor = backgroundColor,
            backgroundBrush = backgroundBrush,
            shadowElevation = shadowElevation,
            borderColor = borderColor,
            borderBrush = borderBrush,
            enabled = enabled,
            onLongClick = onLongClick,
            disableScale = disableScale,
        ) { f, p -> content(f, p) }
        // The focus ring, as a SIBLING of the content with its own graphics layer -- see the
        // `drawRing` note on [tvFocusable] for why this is not a modifier on the Box above.
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer { alpha = ringAlphaOverlay.value }
                .focusRingCached(glowColor, shape, FocusRingWidth) { 1f },
        )
    }
}

/** Stroke width of the focus ring. */
private val FocusRingWidth = 3.dp

/** Hold threshold (ms) separating a tap from a long-press on the OK/select button. */
private const val LONG_PRESS_MS = 400L

/**
 * [LocalAreInteractiveBinding] implementation for `:tv`, provided by [TvAppTheme]. Every `:core`
 * component that calls `AreInteractive` directly (Button, Chip, Tabs, SegmentedControl,
 * CategoryCard, CategoryRow, ChannelTile, ContinueCard, GuideCell, PosterTile, Rail,
 * PlayerControls, ...) resolves here, restoring the D-pad focus target and accent focus ring that
 * `AreInteractiveSurface` alone deliberately omits.
 *
 * This is the same wrapping [TvFocusable] applies (focusable registration, `DPAD_CENTER` handling,
 * a sibling focus-ring box), minus [TvFocusable]'s customizable `glowColor` -- callers that need a
 * non-default ring color (e.g. multi-view's selected-pane accent ring) keep using [TvFocusable]
 * directly, which calls [AreInteractiveSurface] itself rather than going through this binding, so
 * there is no double focus-registration or double ring.
 */
private val tvAreInteractiveBinding: AreInteractiveBinding = { onClick,
    modifier,
    interactionSource,
    shape,
    backgroundColor,
    backgroundBrush,
    shadowElevation,
    borderColor,
    borderBrush,
    enabled,
    onLongClick,
    disableScale,
    content,
    ->
    val focused by interactionSource.collectIsFocusedAsState()
    val motion = AreIptvTheme.motion
    val ringAlpha = animateFloatAsState(
        targetValue = if (focused) 1f else 0f,
        animationSpec = tween(durationMillis = motion.durFastMs, easing = motion.easeOut),
        label = "areInteractiveFocusRing",
    )
    Box(modifier = modifier) {
        AreInteractiveSurface(
            onClick = onClick,
            modifier = Modifier
                .matchParentSize()
                .focusable(interactionSource = interactionSource)
                // See the identical block in TvFocusable for why DPAD_CENTER needs its own
                // handling on top of combinedClickable's Enter/NumPadEnter.
                .onKeyEvent { keyEvent ->
                    if (enabled && keyEvent.type == KeyEventType.KeyUp && keyEvent.key == Key.DirectionCenter) {
                        val heldMs = keyEvent.nativeKeyEvent.eventTime - keyEvent.nativeKeyEvent.downTime
                        if (onLongClick != null && heldMs >= LONG_PRESS_MS) onLongClick() else onClick()
                        true
                    } else {
                        enabled && keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.DirectionCenter
                    }
                },
            interactionSource = interactionSource,
            shape = shape,
            backgroundColor = backgroundColor,
            backgroundBrush = backgroundBrush,
            shadowElevation = shadowElevation,
            borderColor = borderColor,
            borderBrush = borderBrush,
            enabled = enabled,
            onLongClick = onLongClick,
            disableScale = disableScale,
            content = content,
        )
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer { alpha = ringAlpha.value }
                .focusRingCached(AreIptvTheme.colors.focusRing, shape, FocusRingWidth) { 1f },
        )
    }
}

/**
 * `:tv`'s composition root theme wrap: [AreIptvTheme] (`:core`, platform-agnostic tokens) plus
 * binding [LocalAreInteractiveBinding] to [tvAreInteractiveBinding] -- the one piece of TV-only
 * (D-pad focus/key-event) wiring `:core` cannot provide itself. Every `:tv` composition root
 * (MainActivity's splash and main NavHost) uses this instead of calling [AreIptvTheme] directly.
 */
@Composable
fun TvAppTheme(
    isDark: Boolean = true,
    accent: AccentPreset = AccentPreset.BLUE,
    reducedMotion: Boolean = false,
    content: @Composable () -> Unit,
) {
    AreIptvTheme(isDark = isDark, accent = accent, reducedMotion = reducedMotion) {
        CompositionLocalProvider(LocalAreInteractiveBinding provides tvAreInteractiveBinding) {
            content()
        }
    }
}

/**
 * Requests focus once the target actually exists, retrying for a few frames.
 *
 * A single `withFrameNanos { }` is enough on a screen that composes its target immediately (the
 * Streams empty state), but not when arriving via a tab switch: the shell recomposes, the browse
 * grid measures, and paging supplies its first page over several frames. Until the requester is
 * attached, [FocusRequester.requestFocus] throws and the request is silently lost — leaving focus
 * parked on the sidebar, which is exactly the "screen looks dead until you press RIGHT" symptom.
 *
 * Gives up after [attempts] rather than looping forever, so a screen whose target never appears
 * (an empty catalogue, say) simply leaves focus where it was instead of spinning. Returns whether
 * the request was ever granted, so a caller that put the UI into a state which REQUIRES this focus
 * (e.g. [com.arashrahimi46.iptv.ui.components.AreTextField]'s edit mode) can undo it rather than
 * leave the user stranded with nothing focused.
 *
 * @param reassertAfterMs re-claim focus once more this long after the first grant; 0 disables it.
 */
suspend fun FocusRequester.requestFocusWhenReady(
    attempts: Int = 20,
    gapMs: Long = 40L,
    reassertAfterMs: Long = SETTLE_MS,
): Boolean {
    var granted = false
    // NOTE: a `for`/`break`, not `repeat { return@repeat }` -- that label returns from the lambda,
    // i.e. it CONTINUES the loop, so a granted request kept re-requesting for the full
    // attempts*gapMs (800ms by default). On a nav that meant focus was yanked back to the
    // destination's entry point for most of a second, undoing every D-pad step the user made in
    // the meantime -- the opposite of the "screen looks dead" symptom this exists to fix.
    for (attempt in 0 until attempts) {
        withFrameNanos { }
        if (runCatching { requestFocus() }.isSuccess) {
            granted = true
            break
        }
        delay(gapMs)
    }
    // Re-assert once the navigation has settled. The first grant succeeds within a frame, but the
    // sidebar row the user just clicked takes focus back afterwards as the tab transition completes
    // -- so a single successful request leaves focus parked on the sidebar anyway. Claiming it a
    // second time, late, is what actually makes the content focused.
    if (granted && reassertAfterMs > 0) {
        delay(reassertAfterMs)
        runCatching { requestFocus() }
    }
    return granted
}

/** Long enough for the tab transition and the sidebar's collapse animation to finish. */
private const val SETTLE_MS = 250L
