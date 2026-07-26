package com.arashrahimi46.iptv.ui.theme

import android.graphics.BlurMaskFilter
import android.graphics.Paint as NativePaint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
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
 */
fun Modifier.softShadow(
    shape: Shape,
    color: Color = Color.Black,
    alpha: Float = 0.10f,
    blur: Dp = 18.dp,
    offsetY: Dp = 4.dp,
): Modifier = drawBehind {
    if (alpha <= 0f) return@drawBehind
    val offPx = offsetY.toPx()
    val paint = NativePaint().apply {
        isAntiAlias = true
        this.color = color.copy(alpha = alpha).toArgb()
        maskFilter = BlurMaskFilter(blur.toPx(), BlurMaskFilter.Blur.NORMAL)
    }
    val path = Path().apply {
        when (val o = shape.createOutline(size, layoutDirection, this@drawBehind)) {
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
    drawIntoCanvas { it.nativeCanvas.drawPath(path, paint) }
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
 *   behind actually shows: on blur tiers the sampled backdrop shows through softened, and on Tier C
 *   (no blur) the surface is still translucent over the dark ambient backdrop, just un-frosted. The
 *   sidebar floats over the dark ambient veil, so it stays legible without the dense fill.
 */
fun Modifier.glassSurface(
    shape: Shape,
    elevated: Boolean = false,
    shadow: Boolean = true,
    sheer: Boolean = false,
): Modifier = composed {
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
    if (backdrop != null && tier.hasBackdropBlur) {
        lifted
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = {
                    // Vibrancy is the "alive" ingredient -- a saturation boost on the blurred
                    // content. AGSL, so Tier A only; Tier B gets blur alone.
                    if (tier.hasShaders) vibrancy()
                    blur(if (elevated) BlurElevatedDp.toPx() else BlurBaseDp.toPx())
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

private val BlurBaseDp = 24.dp
private val BlurElevatedDp = 32.dp

/** The vertical "lit edge" gradient used by every glass surface and its border brush. */
@Composable
fun glassBorderBrush(): Brush {
    val c = AreIptvTheme.colors
    return Brush.verticalGradient(listOf(c.glassHighlight, c.borderGlass))
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
fun Modifier.glassChild(shape: Shape): Modifier = composed {
    val c = AreIptvTheme.colors
    this.background(c.glassChildTint, shape).border(1.dp, c.borderGlass, shape).clip(shape)
}

/**
 * Same rule as [glassChild], slightly more tint, for a *track* or *chip* whose own shape has to stay
 * legible against the surface it sits on -- switch tracks, the seek rail, nested text fields.
 * Carries the lit top edge so the shape reads without a fill.
 */
fun Modifier.glassTrack(shape: Shape): Modifier = composed {
    val c = AreIptvTheme.colors
    this
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
fun Modifier.glassWell(shape: Shape): Modifier = composed {
    val c = AreIptvTheme.colors
    val shadow = if (c.isDark) Color.Black.copy(alpha = 0.30f) else Color.Black.copy(alpha = 0.10f)
    this
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
fun Modifier.glassLens(shape: Shape): Modifier = composed {
    this
        .background(accentLensBrush(), shape)
        .border(1.dp, lensBorderBrush(), shape)
        .clip(shape)
}

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
