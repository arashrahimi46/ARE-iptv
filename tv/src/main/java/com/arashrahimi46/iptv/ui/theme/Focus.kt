package com.arashrahimi46.iptv.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.delay
import androidx.compose.ui.focus.FocusRequester
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
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(durationMillis = motion.durFastMs, easing = motion.easeEmph),
        label = "tvFocusScale",
    )
    val ringAlpha by animateFloatAsState(
        targetValue = if (focused) 1f else 0f,
        animationSpec = tween(durationMillis = motion.durFastMs, easing = motion.easeOut),
        label = "tvFocusRingAlpha",
    )

    return this
        .then(if (ownsFocusable) Modifier.focusable(interactionSource = interactionSource) else Modifier)
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
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
        // It takes `alpha` as a LAMBDA so the animated value is read at DRAW time. This used to be
        // `.border(color = glowColor.copy(alpha = ringAlpha))`, which read `ringAlpha` in
        // COMPOSITION -- so every one of the ~8-15 tween frames rebuilt this whole modifier chain
        // and minted a fresh Color and border node, on BOTH the leaving and the entering element,
        // for every single D-pad step. It was the one hole in an otherwise draw-time-only chain.
        .focusRingCached(glowColor, shape, ringWidth) { ringAlpha }
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

/**
 * Symmetric accent glow matching the design system's box-shadow glow tokens
 * (`--focus-glow-tight`, `--glow-accent`, `--glow-live`, `--glow-smart` — all
 * `0 0 <blur>`, i.e. zero offset). Draws the [shape]'s outline several times
 * with growing stroke width and fading alpha so the halo hugs the edge and
 * fades outward — no directional offset, no boxy backplate. Place this BEFORE
 * any opaque `.background()` in the chain so the element's fill covers the
 * glow's inner half and only the outer halo shows.
 */
fun Modifier.tvGlow(
    color: Color,
    shape: Shape,
    spread: Dp = 7.dp,
    alpha: Float = 1f,
): Modifier = if (alpha <= 0f) this else this.drawWithCache {
    val spreadPx = spread.toPx()
    // A thin core stroke that the Gaussian blur softens -- NOT a fat stroke. A wide
    // stroke (previously == spread) made the halo huge and bleed onto neighbouring
    // text. Keep the core ~2dp; `spread` controls the blur radius (the softness).
    val strokePx = 2.dp.toPx()
    // Push the glow path OUTWARD so the blurred stroke sits just outside the shape
    // edge instead of straddling it -- otherwise the inner half bleeds INTO the
    // element ("shadow came into the button"). Any faint inner tail is covered by
    // the element's own opaque background (drawn on top).
    val out = spreadPx * 0.5f
    val path = Path().apply {
        when (val o = shape.createOutline(size, layoutDirection, this@drawWithCache)) {
            is Outline.Rounded -> {
                val rr = o.roundRect
                addRoundRect(
                    RoundRect(
                        left = rr.left - out,
                        top = rr.top - out,
                        right = rr.right + out,
                        bottom = rr.bottom + out,
                        cornerRadius = CornerRadius(rr.topLeftCornerRadius.x + out, rr.topLeftCornerRadius.y + out),
                    ),
                )
            }
            is Outline.Rectangle -> addRect(o.rect.inflate(out))
            is Outline.Generic -> addPath(o.path)
        }
    }.asAndroidPath()
    // One real Gaussian blur (BlurMaskFilter) = one smooth halo (no banded layered
    // strokes). minSdk 36 -> fully hardware-accelerated.
    val paint = NativePaint().apply {
        isAntiAlias = true
        style = NativePaint.Style.STROKE
        strokeWidth = strokePx
        this.color = color.copy(alpha = (0.38f * alpha).coerceIn(0f, 1f)).toArgb()
        maskFilter = BlurMaskFilter(spreadPx, BlurMaskFilter.Blur.NORMAL)
    }
    // PERF: drawWithCache, not drawBehind -- same pixels, built once per size/shape instead of per
    // draw pass. As drawBehind this allocated a Path, a NativePaint AND a BlurMaskFilter on every
    // single draw, so each of these badges/dots/switches churned three objects per frame and missed
    // HWUI's paint cache. [alpha] is a plain parameter, so a change to it recreates the modifier and
    // invalidates this cache -- correctness is preserved without reading it at draw time.
    onDrawBehind {
        drawIntoCanvas { canvas -> canvas.nativeCanvas.drawPath(path, paint) }
    }
}

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
 * Convenience wrapper composing [tvFocusable] with a click target, for the
 * common "focusable tile/card/button" pattern used across the media,
 * category and guide components.
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
    val pressed by interactionSource.collectIsPressedAsState()
    Box(
        modifier = modifier
            .tvFocusable(interactionSource, shape, glowColor, disableScale = disableScale)
            .then(if (shadowElevation > 0.dp) Modifier.softShadow(shape) else Modifier)
            .then(
                if (backgroundBrush != null) Modifier.background(backgroundBrush, shape)
                else Modifier.background(backgroundColor, shape),
            )
            .then(
                when {
                    borderBrush != null -> Modifier.border(1.dp, borderBrush, shape)
                    borderColor != null -> Modifier.border(1.dp, borderColor, shape)
                    else -> Modifier
                },
            )
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
                onLongClick = onLongClick,
            )
            // combinedClickable() above handles Enter/NumPadEnter (incl. long-press) but NOT
            // Key.DirectionCenter -- the key every real Android TV / Fire TV remote's
            // physical OK/Select button actually sends. Confirmed missing via a real
            // device-emulator D-pad test (not an emulator artifact): DPAD_CENTER alone
            // did nothing on a fully-focused, freshly-launched card across multiple
            // isolated repro attempts, while KEYCODE_ENTER worked every time. Without
            // this, nothing in the app would be selectable with a real remote's OK
            // button -- fixed once, here, so every TvFocusable-based component in the
            // library inherits it. We resolve short vs. long press from the native
            // event's own down->up span so DPAD_CENTER gets long-press too.
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
    ) {
        content(focused, pressed)
    }
}

/** Hold threshold (ms) separating a tap from a long-press on the OK/select button. */
private const val LONG_PRESS_MS = 400L

/**
 * Requests focus once the target actually exists, retrying for a few frames.
 *
 * A single `withFrameNanos { }` is enough on a screen that composes its target immediately (the
 * Streams empty state), but not when arriving via a tab switch: the shell recomposes, the browse
 * grid measures, and paging supplies its first page over several frames. Until the requester is
 * attached, [FocusRequester.requestFocus] throws and the request is silently lost — leaving focus
 * parked on the sidebar, which is exactly the "screen looks dead until you press RIGHT" symptom.
 *
 * Gives up after [maxFrames] rather than looping forever, so a screen whose target never appears
 * (an empty catalogue, say) simply leaves focus where it was instead of spinning.
 */
suspend fun FocusRequester.requestFocusWhenReady(attempts: Int = 20, gapMs: Long = 40L) {
    var granted = false
    repeat(attempts) {
        withFrameNanos { }
        if (runCatching { requestFocus() }.isSuccess) {
            granted = true
            return@repeat
        }
        delay(gapMs)
    }
    // Re-assert once the navigation has settled. The first grant succeeds within a frame, but the
    // sidebar row the user just clicked takes focus back afterwards as the tab transition completes
    // -- so a single successful request leaves focus parked on the sidebar anyway. Claiming it a
    // second time, late, is what actually makes the content focused.
    if (granted) {
        delay(SETTLE_MS)
        runCatching { requestFocus() }
    }
}

/** Long enough for the tab transition and the sidebar's collapse animation to finish. */
private const val SETTLE_MS = 250L
