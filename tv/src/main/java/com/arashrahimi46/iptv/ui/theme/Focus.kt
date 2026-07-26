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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.Modifier
import android.graphics.BlurMaskFilter
import android.graphics.Paint as NativePaint
import androidx.compose.ui.draw.drawBehind
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
        // Design system `--focus-glow-tight` = `0 0 0 3px ring, 0 0 22px glow` -- a
        // SYMMETRIC (zero-offset) glow + crisp ring.
        //
        // PERF: both the glow and the ring are ALWAYS in the modifier chain (alpha 0 when
        // unfocused), not conditionally added/removed. Toggling them in/out on every focus
        // change forced a relayout each time -- the stutter felt moving the sidebar selection
        // and travelling focus on Home. The glow ([tvGlowCached]) also builds its blur
        // path/paint ONCE (drawWithCache) and only modulates alpha per frame, instead of the
        // old per-frame Path/Paint/BlurMaskFilter allocation that churned the GC mid-animation.
        //
        // Both take `alpha` as a LAMBDA so the animated value is read at DRAW time. This used to be
        // `.border(color = glowColor.copy(alpha = ringAlpha))`, which read `ringAlpha` in
        // COMPOSITION -- so every one of the ~8-15 tween frames rebuilt this whole modifier chain
        // and minted a fresh Color and border node, on BOTH the leaving and the entering element,
        // for every single D-pad step. It was the one hole in an otherwise draw-time-only chain.
        .tvGlowCached(glowColor, shape) { ringAlpha }
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
 * Perf-tuned focus glow used by [tvFocusable]: identical look to [tvGlow] but built for a
 * value that animates every frame. [drawWithCache] builds the outward-inflated blur path and
 * the [BlurMaskFilter] paint ONCE (rebuilt only when size/shape/color change); [onDrawBehind]
 * then just re-modulates the paint's alpha from [alpha] and redraws -- zero per-frame
 * allocation. [alpha] is a lambda so reading the animated value happens at DRAW time and does
 * not invalidate the cache. Draw it BEFORE any opaque `.background()` so only the outer halo
 * shows (the element's fill covers the inner half), same as [tvGlow].
 */
fun Modifier.tvGlowCached(
    color: Color,
    shape: Shape,
    spread: Dp = 2.5.dp,
    alpha: () -> Float,
): Modifier = this.drawWithCache {
    val spreadPx = spread.toPx()
    val out = spreadPx * 0.5f
    val strokePx = 2.dp.toPx()
    val androidPath = Path().apply {
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
    val paint = NativePaint().apply {
        isAntiAlias = true
        style = NativePaint.Style.STROKE
        strokeWidth = strokePx
        this.color = color.toArgb()
        maskFilter = BlurMaskFilter(spreadPx, BlurMaskFilter.Blur.NORMAL)
    }
    onDrawBehind {
        val a = alpha()
        if (a > 0f) {
            paint.alpha = (0.20f * a * 255f).toInt().coerceIn(0, 255)
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawPath(androidPath, paint)
            }
        }
    }
}

/**
 * The focus *material* response (V2 focus ask: "make focus more glassy"). The ring + glow + scale
 * are a decal drawn AROUND the element; on real glass the light also catches the surface itself when
 * it comes forward. This fades a soft top-lit specular sheen INTO the fill on focus -- so the focused
 * card/button visibly brightens as glass would, not just gains an outline. Built once via
 * [drawWithCache]; [alpha] is a lambda read at draw time so the per-frame focus animation never
 * rebuilds the brush. Draw AFTER the fill and BEFORE the content so it lifts the surface, not the label.
 */
fun Modifier.focusSheen(shape: Shape, alpha: () -> Float): Modifier = drawWithCache {
    val path = Path().apply {
        when (val o = shape.createOutline(size, layoutDirection, this@drawWithCache)) {
            is Outline.Rounded -> addRoundRect(o.roundRect)
            is Outline.Rectangle -> addRect(o.rect)
            is Outline.Generic -> addPath(o.path)
        }
    }
    val brush = Brush.verticalGradient(
        0f to Color.White.copy(alpha = 0.09f),
        0.5f to Color.White.copy(alpha = 0.02f),
        1f to Color.Transparent,
    )
    onDrawBehind {
        val a = alpha()
        if (a > 0f) drawPath(path, brush, alpha = a)
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
): Modifier = this.drawBehind {
    if (alpha <= 0f) return@drawBehind
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
        when (val o = shape.createOutline(size, layoutDirection, this@drawBehind)) {
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
    }
    // One real Gaussian blur (BlurMaskFilter) = one smooth halo (no banded layered
    // strokes). minSdk 36 -> fully hardware-accelerated.
    val paint = NativePaint().apply {
        isAntiAlias = true
        style = NativePaint.Style.STROKE
        strokeWidth = strokePx
        this.color = color.copy(alpha = (0.38f * alpha).coerceIn(0f, 1f)).toArgb()
        maskFilter = BlurMaskFilter(spreadPx, BlurMaskFilter.Blur.NORMAL)
    }
    drawIntoCanvas { canvas ->
        canvas.nativeCanvas.drawPath(path.asAndroidPath(), paint)
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
    /** The focus specular sheen that brightens the surface as it comes forward. On the large media
     *  tiles/cards it reads as glass catching the light; on small buttons it was too heavy-handed,
     *  so those opt out (the ring + glow already carry their focus). */
    showFocusSheen: Boolean = true,
    content: @Composable BoxScope.(focused: Boolean, pressed: Boolean) -> Unit,
) {
    val focused by interactionSource.collectIsFocusedAsState()
    val pressed by interactionSource.collectIsPressedAsState()
    // Only surfaces with an actual fill have a material for the light to catch; a ghost/transparent
    // control would just show the sheen as a floating smudge.
    val hasFill = backgroundBrush != null || backgroundColor != Color.Transparent
    val motion = AreIptvTheme.motion
    val sheen by animateFloatAsState(
        targetValue = if (focused) 1f else 0f,
        animationSpec = tween(durationMillis = motion.durFastMs, easing = motion.easeOut),
        label = "tvFocusSheen",
    )
    Box(
        modifier = modifier
            .tvFocusable(interactionSource, shape, glowColor, disableScale = disableScale)
            .then(if (shadowElevation > 0.dp) Modifier.softShadow(shape) else Modifier)
            .then(
                if (backgroundBrush != null) Modifier.background(backgroundBrush, shape)
                else Modifier.background(backgroundColor, shape),
            )
            .then(if (hasFill && showFocusSheen) Modifier.focusSheen(shape) { sheen } else Modifier)
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
