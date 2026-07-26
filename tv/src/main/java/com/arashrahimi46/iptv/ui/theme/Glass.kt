package com.arashrahimi46.iptv.ui.theme

import android.graphics.BlurMaskFilter
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
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
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
 * PERF: built with [drawWithCache], so the shadow [Path] and the [BlurMaskFilter] paint are created
 * ONCE per size/shape and reused every frame. The old `drawBehind` version allocated a Path, a
 * NativePaint and a BlurMaskFilter on EVERY draw pass of EVERY glass surface -- a dense Settings pane
 * carries ~7 of them, so any repaint (the sidebar expanding over the page, a focus move) churned the
 * GC and re-uploaded a fresh blur mask instead of hitting HWUI's paint cache. This was the single
 * largest per-frame CPU cost in the glass language, and the one weak TV SoCs felt most.
 */
fun Modifier.softShadow(
    shape: Shape,
    color: Color = Color.Black,
    alpha: Float = 0.10f,
    blur: Dp = 18.dp,
    offsetY: Dp = 4.dp,
): Modifier = if (alpha <= 0f) this else drawWithCache {
    val offPx = offsetY.toPx()
    val paint = NativePaint().apply {
        isAntiAlias = true
        this.color = color.copy(alpha = alpha).toArgb()
        maskFilter = BlurMaskFilter(blur.toPx(), BlurMaskFilter.Blur.NORMAL)
    }
    val path = Path().apply {
        when (val o = shape.createOutline(size, layoutDirection, this@drawWithCache)) {
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
    }.asAndroidPath()
    onDrawBehind { drawIntoCanvas { it.nativeCanvas.drawPath(path, paint) } }
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
    val lifted = this.then(if (shadow) Modifier.softShadow(shape) else Modifier)

    // V2: sample and blur what's actually behind this surface. Only possible where the shell has
    // published a backdrop layer AND the device can render it -- Tier C falls through to the V1
    // path, which is why its token alphas are deliberately left at V1's denser values (§7/§8).
    return if (backdrop != null && tier.hasBackdropBlur) {
        lifted
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = {
                    // Vibrancy is the "alive" ingredient -- a saturation boost on the sampled
                    // content. AGSL, so Tier A only.
                    if (tier.hasShaders) vibrancy()
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
 * TRUE frosted glass -- a panel that floats over the page and shows the **page content** softly
 * blurred behind it. Currently the expanded sidebar, and deliberately nothing else.
 *
 * [glassSurface] can't do this: it samples [LocalAppBackdrop], the ambient wash, whose first draw op
 * is an opaque `bgBase`. Sampling it paints the page's background back over the page inside the
 * panel's bounds, so the rails the panel covers can never show through no matter how sheer the fill.
 * This samples [LocalPageBackdrop] instead -- ambient *and* content -- and blurs it.
 *
 * The blur is the whole point and is the sanctioned exception to §4's "no per-surface blur": what is
 * behind here is sharp artwork and text, so without it the panel reads as a transparent window and
 * the nav labels fight the posters underneath. One surface, one blur.
 *
 * Falls back to sheer [glassSurface] when the shell hasn't published a page layer (i.e. the sidebar is
 * collapsed, where there is no content behind it anyway) or the TV can't render one (Tier C).
 */
@Composable
fun Modifier.frostedPanel(shape: Shape): Modifier {
    val c = AreIptvTheme.colors
    val tier = LocalGlassTier.current
    val page = LocalPageBackdrop.current
    return if (page != null && tier.hasBackdropBlur) {
        this
            .softShadow(shape)
            .drawBackdrop(
                backdrop = page,
                shape = { shape },
                effects = { blur(28.dp.toPx()) },
                onDrawSurface = { drawRect(c.surfaceGlassSheer) },
            )
            .border(1.dp, glassBorderBrush(), shape)
            .clip(shape)
    } else {
        this.glassSurface(shape, elevated = true, sheer = true)
    }
}

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
    return Brush.verticalGradient(listOf(c.accentHover, c.accent))
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
        .drawBehind {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(shadow, Color.Transparent),
                    startY = 0f,
                    endY = size.height * 0.55f,
                ),
            )
        }
        .border(1.dp, wellBorderBrush(), shape)
}

/** [glassBorderBrush] inverted -- faint top, lit bottom. The hairline that reads as "recessed". */
@Composable
fun wellBorderBrush(): Brush {
    val c = AreIptvTheme.colors
    return Brush.verticalGradient(listOf(c.borderGlass, c.glassHighlight))
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
    return if (c.isDark) {
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

/** The lens's rim -- a brighter, more saturated hairline than [glassBorderBrush]'s neutral one. */
@Composable
fun lensBorderBrush(): Brush {
    val c = AreIptvTheme.colors
    return if (c.isDark) {
        Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.46f), c.accentHover.copy(alpha = 0.42f)),
        )
    } else {
        Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.90f), c.accent.copy(alpha = 0.42f)),
        )
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
