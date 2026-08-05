package com.arashrahimi46.iptv.ui.theme

import android.graphics.BlurMaskFilter
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint as NativePaint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.vibrancy

/** Sentinel "this surface should lift" value; the actual look comes from [softShadow], not from a
 *  hard platform elevation. Kept as a Dp so call sites read naturally (`shadowElevation = GlassElevation`). */
val GlassElevation: Dp = 6.dp

/**
 * A soft, diffuse drop shadow for the glass language -- a wide, low-alpha Gaussian blur of the shape,
 * nudged down a few dp. Deliberately NOT [androidx.compose.ui.draw.shadow]: the platform elevation
 * shadow reads too hard/dark for glass ("still strong shadows"). This is a whisper of depth that
 * mostly shows in light mode; in dark the lit edge carries the separation. Draw BEFORE the fill.
 *
 * PERF: the blurred shadow is RASTERIZED ONCE **per size/shape** into a cached bitmap and blitted
 * thereafter -- but only because the block below is `remember`ed. [drawWithCache]'s element compares
 * its lambda by reference IDENTITY (`DrawWithCacheElement.equals` uses `!==`), and that lambda captures
 * shape/colour/alpha/blur/offset, so an unremembered call minted a fresh instance every time the OWNER
 * recomposed -- taking the `update(node)` path, whose `block` setter calls `invalidateDrawCache()`, so
 * the whole `ImageBitmap` allocation plus the CPU `BlurMaskFilter` pass re-ran inside the next draw.
 * "Rasterized once" was therefore never true for any owner that recomposes, which is most of them:
 * [Modifier.glassSurface] re-invokes this on every recomposition of its caller (Guide's focused-info
 * bar recomposes on EVERY D-pad step -- a ~630 KB bitmap and a Gaussian blur ~8x/second), and
 * [TvFocusable] rebuilds its chain on every focus enter AND exit, so every elevated chip, button and
 * tab re-baked twice per keypress. Remembering cannot stale the bake: `CacheDrawModifierNodeImpl`
 * still invalidates on measure-result, density and layout-direction changes, which is exactly what
 * the `bake = false` sidebar relies on.
 *
 * A previous pass moved the [Path] and [BlurMaskFilter] paint construction into [drawWithCache] so
 * they were built once per size/shape rather than per draw. That fixed the allocation churn but not
 * the real cost: `drawPath` with a mask filter still had to produce the blurred mask on EVERY frame,
 * and a Gaussian mask over a card-sized rounded rect is expensive. Measured on the TV emulator by
 * disabling it, this one modifier was worth ~1.7ms of RenderThread time per frame on a Settings
 * scroll -- the largest single draw cost left in the glass language, and precisely the kind of work
 * a weak TV SoC is worst at. Compose's own composition/measure/layout, for comparison, was 0.03ms.
 *
 * Baking is valid because the result is STATIC: a pure function of size, shape, colour, alpha, blur
 * and offset, none of which change between frames. The mask is rendered at half resolution and
 * bilinearly upscaled -- the output of a wide Gaussian blur is band-limited by construction, so there
 * is no detail a half-res buffer can lose, and it keeps the cached bitmap to a quarter of the pixels.
 * Verified against the un-baked version by pixel diff of the Settings screen: 1.3% of pixels differ,
 * all by a max of 2/255 -- below the quantisation floor of a shadow drawn at 10% alpha.
 */
@Composable
fun Modifier.softShadow(
    shape: Shape,
    color: Color = Color.Black,
    alpha: Float = 0.10f,
    blur: Dp = 18.dp,
    offsetY: Dp = 4.dp,
    /**
     * Set FALSE on a surface whose measured SIZE animates.
     *
     * [drawWithCache] re-runs whenever the node's size changes, so on a size-animating surface baking
     * would allocate and rasterize a fresh bitmap on EVERY frame of the animation -- strictly worse
     * than just drawing the blurred path. The expanding sidebar is the one such surface in the app
     * (`widthFrom` remeasures it per frame), so it keeps the direct path; everything else is static
     * once laid out and bakes.
     */
    bake: Boolean = true,
): Modifier = this.then(
    // The element is remembered so its lambda keeps ONE identity across the owner's recompositions --
    // see the PERF paragraph above for why that is the whole point of this modifier.
    remember(shape, color, alpha, blur, offsetY, bake) {
        if (alpha <= 0f) Modifier else softShadowElement(shape, color, alpha, blur, offsetY, bake)
    },
)

/** [Modifier.softShadow]'s draw element. Non-composable so the `remember` above owns its identity. */
private fun softShadowElement(
    shape: Shape,
    color: Color,
    alpha: Float,
    blur: Dp,
    offsetY: Dp,
    bake: Boolean,
): Modifier = if (!bake) Modifier.drawWithCache {
    val offPx = offsetY.toPx()
    val paint = NativePaint().apply {
        isAntiAlias = true
        this.color = color.copy(alpha = alpha).toArgb()
        maskFilter = BlurMaskFilter(blur.toPx(), BlurMaskFilter.Blur.NORMAL)
    }
    val path = shadowPath(shape, size, layoutDirection, this, offPx).asAndroidPath()
    onDrawBehind { drawIntoCanvas { it.nativeCanvas.drawPath(path, paint) } }
} else Modifier.drawWithCache {
    val offPx = offsetY.toPx()
    val blurPx = blur.toPx()
    // The blur fringe spills well past the shape. Pad by 3x sigma so the mask is never clipped, and
    // include the downward offset so the bottom fringe fits too.
    val pad = blurPx * 3f
    val scale = 0.5f
    val bakeW = ((size.width + pad * 2f) * scale).toInt().coerceAtLeast(1)
    val bakeH = ((size.height + pad * 2f + offPx) * scale).toInt().coerceAtLeast(1)

    val paint = NativePaint().apply {
        isAntiAlias = true
        this.color = color.copy(alpha = alpha).toArgb()
        maskFilter = BlurMaskFilter(blurPx * scale, BlurMaskFilter.Blur.NORMAL)
    }
    // Shape outline in bake space: shifted by `pad` so the fringe has room, then scaled.
    val path = shadowPath(shape, size, layoutDirection, this, offPx).asAndroidPath().apply {
        transform(
            android.graphics.Matrix().apply {
                setTranslate(pad, pad)
                postScale(scale, scale)
            },
        )
    }

    val baked = ImageBitmap(bakeW, bakeH)
    AndroidCanvas(baked.asAndroidBitmap()).drawPath(path, paint)

    // Blitted back at full size, anchored `pad` up and left so the shape lands where it was drawn.
    val dstOffset = IntOffset((-pad).toInt(), (-pad).toInt())
    val dstSize = IntSize((bakeW / scale).toInt(), (bakeH / scale).toInt())
    onDrawBehind {
        drawImage(
            image = baked,
            dstOffset = dstOffset,
            dstSize = dstSize,
            filterQuality = FilterQuality.Low,
        )
    }
}

/** The shape's outline as a [Path], nudged down by [offPx] -- the silhouette both softShadow paths blur. */
private fun shadowPath(
    shape: Shape,
    size: androidx.compose.ui.geometry.Size,
    layoutDirection: androidx.compose.ui.unit.LayoutDirection,
    density: androidx.compose.ui.unit.Density,
    offPx: Float,
): Path = Path().apply {
    when (val o = shape.createOutline(size, layoutDirection, density)) {
        is Outline.Rounded -> {
            val rr = o.roundRect
            addRoundRect(
                RoundRect(
                    left = rr.left,
                    top = rr.top + offPx,
                    right = rr.right,
                    bottom = rr.bottom + offPx,
                    cornerRadius = CornerRadius(rr.topLeftCornerRadius.x, rr.topLeftCornerRadius.y),
                ),
            )
        }
        is Outline.Rectangle -> addRect(o.rect.translate(Offset(0f, offPx)))
        is Outline.Generic -> addPath(o.path, Offset(0f, offPx))
    }
}

/**
 * The one shared "glass" surface language: a translucent fill + a hairline gradient border whose
 * bright top stop reads as a lit edge (bright top -> faint bottom) + a subtle drop shadow. The fill
 * alpha IS the glass -- content behind shows through; there is deliberately NO backdrop blur here
 * (see the design spec: real blur is reserved for dialogs via the window API, and is impossible over
 * the SurfaceView video).
 *
 * Compose [TvFocusable] / [Modifier.tvFocusable] outside this so the accent ring + glow still draw
 * on top -- focus is intentionally not baked into the glass.
 *
 * @param elevated denser fill for modals/HUD over media so text stays legible.
 * @param shadow set false for surfaces that shouldn't lift (e.g. an inline track already inside a
 *   glass panel, or a divider) to avoid a shadow-on-shadow smudge.
 * @param sheer a large hero surface (the floating sidebar) that should read as TRUE see-through
 *   glass -- drops the fill to [AreIptvColors.surfaceGlassSheer] (~30%) on EVERY tier so the content
 *   behind actually shows: the sampled ambient backdrop reads through it (and it is already a soft
 *   wash -- see the blur note below), and on Tier C the surface is still translucent over the dark
 *   ambient backdrop. The sidebar floats over the dark ambient veil, so it stays legible without the
 *   dense fill.
 */
@Composable
fun Modifier.glassSurface(
    shape: Shape,
    elevated: Boolean = false,
    shadow: Boolean = true,
    sheer: Boolean = false,
): Modifier {
    val c = AreIptvTheme.colors
    val tier = LocalGlassTier.current
    val backdrop = LocalAppBackdrop.current
    // A sheer surface uses the much lighter fill on every path so the content behind shows through.
    val fill = when {
        sheer -> c.surfaceGlassSheer
        elevated -> c.surfaceGlassElevated
        else -> c.surfaceGlass
    }
    // `sheer` is only ever the expanding sidebar, whose width -- and therefore this node's measured
    // size -- animates per frame. That is exactly the case the shadow must NOT bake; see `bake`.
    val lifted = this.then(if (shadow) Modifier.softShadow(shape, bake = !sheer) else Modifier)

    // V2: sample what's actually behind this surface. Gated on there being an EFFECT to apply
    // (`hasShaders`), NOT merely on the tier being blur-capable -- see the PERF note below. Tier B/C
    // take the plain-fill path; Tier C's token alphas are deliberately left at V1's denser values
    // (§7/§8), while Tier B keeps the retuned alphas (`withBlurredBackdrop()`) it always had.
    //
    // PERF (measured on the real Sony XL95 -- see docs/glass-render-perf-findings.md): sampling the
    // backdrop with an EMPTY effects block is not free and is not a no-op-shaped cost. It is a full
    // offscreen capture per surface, and a dense Settings pane has a dozen. Gating it here took
    // RenderThread from 14.5ms to 5.0ms and total frame time from ~34ms to ~18ms (~29 -> ~55 fps) on
    // a 16.7ms budget. Settings was pixel-identical; Home showed no card-shaped difference at 8x
    // amplification.
    //
    // Why it can be dropped without changing pixels: [AmbientBackdrop] is a REAL LAYER in the
    // hierarchy, so a translucent `fill` over it already composites to the right thing through
    // ordinary alpha blending. Sampling only buys the ability to run an effect on what's behind; with
    // no effect it re-draws the pixels that would have shown through anyway. Anything that needs an
    // actual effect must keep the sample -- that is why Tier A (vibrancy) still takes this path.
    //
    // NOTE this supersedes an earlier conclusion that the backdrop system had to stay because turning
    // it off changed the surfaces. That test disabled the token retune AND the sampling together; the
    // visual change came from the retune. They are separable, and this separates them.
    return if (backdrop != null && tier.hasShaders) {
        lifted
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = {
                    // Vibrancy is the "alive" ingredient -- a saturation boost on the sampled
                    // content. AGSL, so Tier A only -- and now the sole reason this path exists.
                    vibrancy()
                    // PERF (§4 retune): NO per-surface blur pass. The layer this samples is
                    // [AmbientBackdrop], which is low-frequency by construction -- a flat base, two
                    // wide radial gradient lobes, a veil and a vignette, plus artwork that is ALREADY
                    // blurred at 72dp. Blurring that again is arithmetically a near-identity op, so
                    // every surface was paying a full offscreen blur render to reproduce the pixels it
                    // sampled. A dense Settings pane ran ~7 of these per repaint. Dropping the effect
                    // turns each surface into a plain clipped texture sample and leaves exactly one
                    // blur in the app -- the artwork's -- which is where the softening actually comes
                    // from. If a glass surface ever has to sit over sharp content, blur belongs on
                    // THAT layer's capture, not re-derived per surface.
                },
                onDrawSurface = { drawRect(fill) },
            )
            .border(1.dp, glassBorderBrush(), shape)
            .clip(shape)
    } else {
        lifted
            .background(fill, shape)
            .border(1.dp, glassBorderBrush(), shape)
            .clip(shape)
    }
}

/**
 * PERF NOTE for every modifier in this file: these are plain `@Composable` extensions, NOT
 * `Modifier.composed {}`. `composed` modifiers are materialized per layout node, cannot be
 * equality-compared, and re-execute whenever their element recomposes -- it is the documented way to
 * defeat modifier skipping. A 40-tile grid instantiated ~4-5 of them per tile (`tvFocusable`,
 * `glassChild` in the logo well, `tileWash`, the favourite button's own `tvFocusable`), i.e. ~180
 * non-reusable nodes all re-running on scroll and focus travel. None of these needed `composed`'s
 * per-node scope: they only read theme/composition-local values, which a `@Composable` function reads
 * just as well and far more cheaply.
 */

/**
 * The expanded sidebar's frosted fill -- **fill only**: no shadow, no border, no clip, and no size of
 * its own. The caller pins this node at the panel's OPEN width inside its own `graphicsLayer` and
 * animates a rounded clip around it; the wrapper draws the shadow and the edge.
 *
 * That split is not stylistic, it is the fix for the expand/collapse jank. `drawBackdrop` re-pulls its
 * source layer every frame the node draws, and `layerBackdrop` satisfies that pull by re-rasterizing
 * the WHOLE page -- ~14 ms/frame on the XL95 (docs/glass-render-perf-findings.md). While this node's
 * own width animated, that happened on every frame of the tween, at ~28 fps. Pinned at the open width
 * it draws once, the page is captured once, and the per-frame work is a rounded clip.
 *
 * Same pixels: the page behind does not move during the tween, so the blur is the same image either
 * way -- only how much of it is revealed changes. Note this is the ONE genuine blur in the app; see
 * [Modifier.glassSurface] for why every other surface samples nothing.
 */
/** The vertical "lit edge" gradient used by every glass surface and its border brush. */
@Composable
fun glassBorderBrush(): Brush {
    val c = AreIptvTheme.colors
    return remember(c.glassHighlight, c.borderGlass) {
        Brush.verticalGradient(listOf(c.glassHighlight, c.borderGlass))
    }
}

/**
 * The accent-gradient fill for the SELECTED / current state (design §6b: "accent-gradient glass
 * chip, denser than plain focus"). A glossy lighter-top -> accent-bottom sheen that separates a
 * flat *selection* from the *focus* ring -- the piece that was missing everywhere and made active
 * items read as the old flat wash. Pass to [TvFocusable]'s `backgroundBrush`.
 *
 * V2 note: this is now reserved for **actions** (Primary button, active icon button). *Selection*
 * indicators use [accentLensBrush] instead -- see [glassLens].
 */
@Composable
fun accentGradientBrush(): Brush {
    val c = AreIptvTheme.colors
    // remember-ed like glassBorderBrush: this is on every primary button, chip and the brand mark,
    // so an unremembered factory minted a gradient + its list per call per recomposition.
    return remember(c.accentHover, c.accent) { Brush.verticalGradient(listOf(c.accentHover, c.accent)) }
}

/**
 * A control **inside** a glass surface: tint + hairline, no fill, no shadow, and deliberately no
 * second blur (V2 §6). Glass never stacks -- `surfaceGlass` on `surfaceGlassElevated` compounds to
 * ~87% effective opacity, which is why V1's HUD buttons read as opaque squares on a translucent bar.
 */
@Composable
fun Modifier.glassChild(shape: Shape): Modifier {
    val c = AreIptvTheme.colors
    return this.background(c.glassChildTint, shape).border(1.dp, c.borderGlass, shape).clip(shape)
}

/**
 * Same rule as [glassChild], slightly more tint, for a *track* or *chip* whose own shape has to stay
 * legible against the surface it sits on -- switch tracks, the seek rail, nested text fields.
 * Carries the lit top edge so the shape reads without a fill.
 */
@Composable
fun Modifier.glassTrack(shape: Shape): Modifier {
    val c = AreIptvTheme.colors
    return this
        .background(c.glassTrackTint, shape)
        .border(1.dp, glassBorderBrush(), shape)
        .clip(shape)
}

/**
 * An INPUT well -- a text field, search box or any control the user types into.
 *
 * Fields are the one control class that must read as **recessed**, not raised. `glassTrack` made
 * them arithmetically translucent but they still read as flat slabs, because on a dark page every
 * "raised" cue (lit top edge, tint lighter than its parent) says *this sits on top of the glass* --
 * the opposite of what a field is. So the cues are inverted here:
 *
 *  - the fill goes slightly **darker** than its parent rather than lighter,
 *  - a soft inner shadow runs down from the top edge, as if the surface were carved,
 *  - the hairline gradient is flipped: faint at the top, **lit along the bottom**, which is where
 *    light would actually catch on a recess.
 *
 * Depth is carried by the direction of the lighting, not by opacity -- which is why simply raising
 * the field's alpha never fixed this.
 */
@Composable
fun Modifier.glassWell(shape: Shape): Modifier {
    val c = AreIptvTheme.colors
    val shadow = if (c.isDark) Color.Black.copy(alpha = 0.30f) else Color.Black.copy(alpha = 0.10f)
    return this
        .background(c.glassWellTint, shape)
        .clip(shape)
        // drawWithCache, not drawBehind: this gradient is size-dependent, so it can't be a plain
        // remember -- but built in drawBehind it was re-created on every DRAW pass, i.e. on every
        // focus-ring alpha frame of every text field. The cache re-runs only on a size change.
        .drawWithCache {
            val brush = Brush.verticalGradient(
                colors = listOf(shadow, Color.Transparent),
                startY = 0f,
                endY = size.height * 0.55f,
            )
            onDrawBehind { drawRect(brush) }
        }
        .border(1.dp, wellBorderBrush(), shape)
}

/** [glassBorderBrush] inverted -- faint top, lit bottom. The hairline that reads as "recessed". */
@Composable
fun wellBorderBrush(): Brush {
    val c = AreIptvTheme.colors
    return remember(c.borderGlass, c.glassHighlight) { Brush.verticalGradient(listOf(c.borderGlass, c.glassHighlight)) }
}

/**
 * The SELECTION indicator (V2 §6.2). Apple never paints selection: their segmented marker is
 * *clearer and brighter* than the track it slides in, separating by being **more glass** rather than
 * by being opaque. V1 did the opposite -- [accentGradientBrush] laid a fully opaque accent pill on
 * top of a glass track, the same "hole punched through the material" defect as an opaque switch
 * track, just in accent instead of grey.
 *
 * Contrast is the tradeoff: a ~30% accent fill has far less contrast against its label than a solid
 * gradient, so the label must keep [lensContentColor] and the lens must sit on [glassTrack] (or
 * another tinted surface) rather than directly on the ambient backdrop.
 */
@Composable
fun Modifier.glassLens(shape: Shape): Modifier =
    this
        .background(accentLensBrush(), shape)
        .border(1.dp, lensBorderBrush(), shape)
        .clip(shape)

/** [glassLens] as a plain [Brush], for the [TvFocusable] `backgroundBrush` funnel. */
@Composable
fun accentLensBrush(): Brush {
    val c = AreIptvTheme.colors
    // remember-ed: glassLens calls this AND lensBorderBrush, and the sidebar's sliding selection
    // pill is a glassLens -- so every recomposition of the nav body was 2 gradients + 4 Color.copy.
    return remember(c.isDark, c.accentHover, c.accent) {
        if (c.isDark) {
            Brush.verticalGradient(
                listOf(c.accentHover.copy(alpha = 0.40f), c.accent.copy(alpha = 0.26f)),
            )
        } else {
            // Light theme flips it, as Apple's does: a white lens over a low accent wash, label in accent.
            Brush.verticalGradient(
                listOf(Color.White.copy(alpha = 0.72f), c.accentHover.copy(alpha = 0.26f)),
            )
        }
    }
}

/** The lens's rim -- a brighter, more saturated hairline than [glassBorderBrush]'s neutral one. */
@Composable
fun lensBorderBrush(): Brush {
    val c = AreIptvTheme.colors
    return remember(c.isDark, c.accentHover, c.accent) {
        if (c.isDark) {
            Brush.verticalGradient(
                listOf(Color.White.copy(alpha = 0.46f), c.accentHover.copy(alpha = 0.42f)),
            )
        } else {
            Brush.verticalGradient(
                listOf(Color.White.copy(alpha = 0.90f), c.accent.copy(alpha = 0.42f)),
            )
        }
    }
}

/**
 * Label/icon colour that reads on [accentLensBrush] (§6.2 contrast note).
 *
 * [textPrimary] in BOTH themes, which is also what Apple does: their light-mode segmented indicator
 * is white glass with a *dark* label, not a tinted one. Measured on device first with an accent-hued
 * label, which is where the lens's contrast risk actually bites -- `accent` on the pale light lens
 * came out around 3:1 for the cooler presets (Cyan/Teal), and even `accentPress` only reached ~4.4:1.
 * The hue cue lives in the lens FILL; the label doesn't have to carry it too.
 */
@Composable
fun lensContentColor(): Color = AreIptvTheme.colors.textPrimary
