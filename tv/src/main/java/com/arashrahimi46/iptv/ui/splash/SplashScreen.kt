package com.arashrahimi46.iptv.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.arashrahimi46.iptv.R
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.theme.LocalReducedMotion
import com.arashrahimi46.iptv.ui.theme.Violet400
import com.arashrahimi46.iptv.ui.theme.White
import kotlin.math.cos
import kotlin.math.sin

/**
 * Cold-start splash (Issue #13), rebuilt as a choreographed reveal in the Glass
 * design language (docs/glass-redesign-v1-design.md).
 *
 * A one-shot intro plays on launch -- an accent bloom blooms, the frosted
 * **glass plate** scales in and settles with a soft overshoot, the mark and
 * brand fade up in turn, and a shimmer rail draws underneath. Layered on top,
 * continuous ambient life runs for as long as the splash holds: a drifting
 * accent/violet mesh, concentric "broadcast" rings emanating from the plate
 * (echoing the play-with-signal mark), orbiting spark particles, a breathing
 * bloom, and a specular light sweep travelling across the glass.
 *
 * The backdrop is ALWAYS the dark ink ramp regardless of app theme, so the
 * plate/brand contrast is fixed rather than dark-on-light in light mode; the
 * accent hue comes from the active theme via [AreIptvTheme.colors.accent].
 *
 * No `androidx.core.splashscreen` dependency exists yet (manifest has no
 * windowSplashScreen theme), so this is a plain Compose screen shown by
 * [com.arashrahimi46.iptv.MainActivity] before the app routes to the privacy
 * gate / onboarding / shell.
 *
 * Everything is gated on [LocalReducedMotion]: the intro snaps to its final
 * frame and every loop freezes, leaving a still, fully-composed image.
 */
@Composable
fun AreSplashScreen() {
    val accent = AreIptvTheme.colors.accent
    val reduced = LocalReducedMotion.current

    // One-shot entrance clock. Sub-elements read eased slices of it via [seg].
    val intro = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!reduced) intro.animateTo(1f, tween(1900, easing = LinearEasing))
    }
    val p = intro.value
    val bloomIn = seg(p, 0f, 0.45f, FastOutSlowInEasing)
    val markIn = seg(p, 0.30f, 0.72f, EaseOutBack)
    val brandIn = seg(p, 0.62f, 0.92f, FastOutSlowInEasing)
    val ambientIn = seg(p, 0.55f, 1f, FastOutSlowInEasing) // rings/particles fade in last

    val transition = rememberInfiniteTransition(label = "splash")
    val breathe by transition.pulse(reduced, mid = 0.7f, from = 0.45f, to = 1f, durationMs = 2600)
    val sweep by transition.sweepPass(reduced, durationMs = 3400)
    val drift by transition.pulse(reduced, mid = 0.5f, from = 0f, to = 1f, durationMs = 22_000)
    val orbit by transition.sweepPass(reduced, durationMs = 18_000)
    val ripple by transition.sweepPass(reduced, durationMs = 4200)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                // 1. Cinematic base: true black, with the accent living only as a faint centred pool
                //    of light that the glass sits in. Mesh is subdued and pulled toward the middle;
                //    a hard vignette crushes the frame edges to pure black (letterbox-dark mood).
                drawRect(Color.Black)
                val r = size.maxDimension * 0.72f
                val t = drift
                meshLobe(accent, 0.16f, r, size.width * (0.42f + 0.05f * cos(t * TAU)), size.height * (0.40f + 0.04f * sin(t * TAU)))
                meshLobe(Violet400, 0.08f, r * 0.8f, size.width * (0.62f - 0.05f * sin(t * TAU)), size.height * (0.60f - 0.04f * cos(t * TAU)))
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color.Transparent, Color.Black),
                        center = Offset(size.width * 0.5f, size.height * 0.46f),
                        radius = size.maxDimension * 0.62f,
                    ),
                )

                // 2. Broadcast rings + orbiting sparks, centred on the floating mark (which sits a little
                //    above screen centre because the brand + rail hang beneath it in the column).
                if (!reduced && ambientIn > 0f) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f - 46.dp.toPx()
                    val markHalf = 60.dp.toPx()
                    // Three concentric rings, staggered, expanding out and fading.
                    for (i in 0 until 3) {
                        val phase = frac(ripple + i / 3f)
                        val rad = markHalf * 1.3f + phase * markHalf * 3.4f
                        val a = (1f - phase) * 0.42f * ambientIn
                        if (a > 0.01f) {
                            drawCircle(
                                color = accent.copy(alpha = a),
                                radius = rad,
                                center = Offset(cx, cy),
                                style = Stroke(width = 1.5.dp.toPx()),
                            )
                        }
                    }
                    // Orbiting spark particles on a gentle ellipse.
                    val orbitR = markHalf * 2.6f
                    for (i in 0 until 6) {
                        val ang = orbit * TAU + i * (TAU / 6f)
                        val px = cx + cos(ang) * orbitR * 1.15f
                        val py = cy + sin(ang) * orbitR * 0.78f
                        val twinkle = 0.35f + 0.35f * (0.5f + 0.5f * sin(ang * 3f))
                        val dot = 2.6.dp.toPx()
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(accent.copy(alpha = twinkle * ambientIn), Color.Transparent),
                                center = Offset(px, py),
                                radius = dot * 3f,
                            ),
                            radius = dot * 3f,
                            center = Offset(px, py),
                        )
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                // Accent bloom + anamorphic lens streak, both centred on the floating mark. No plate --
                // the mark is the hero, suspended in the pool of light against pure black.
                Box(
                    modifier = Modifier
                        .size(440.dp)
                        .graphicsLayer { alpha = bloomIn.coerceIn(0f, 1f) }
                        .drawBehind {
                            // Soft radial bloom the mark floats inside.
                            val glow = 0.30f + 0.34f * breathe
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(accent.copy(alpha = glow), Color.Transparent),
                                    radius = size.minDimension * (0.20f + 0.14f * bloomIn),
                                ),
                                radius = size.minDimension * 0.5f,
                            )
                            // Anamorphic horizontal lens flare through the mark: stacked thin lines with a
                            // bright core falling off to a wide, faint bleed -- the classic cinematic streak.
                            val cy = size.height / 2f
                            val flareA = markIn * (0.45f + 0.55f * breathe)
                            listOf(1.5.dp to 0.85f, 4.dp to 0.26f, 11.dp to 0.09f).forEach { (half, a) ->
                                val h = half.toPx() * 2f
                                drawRect(
                                    brush = Brush.horizontalGradient(
                                        listOf(Color.Transparent, White.copy(alpha = a * flareA), Color.Transparent),
                                    ),
                                    topLeft = Offset(0f, cy - h / 2f),
                                    size = Size(size.width, h),
                                )
                            }
                        },
                )

                // The mark, floating free -- scales in with a soft settle, then breathes with the bloom.
                Image(
                    painter = painterResource(R.drawable.ic_logo_mark),
                    contentDescription = stringResource(R.string.splash_logo_content_description),
                    modifier = Modifier
                        .size(120.dp)
                        .graphicsLayer {
                            val s = (0.78f + 0.22f * markIn) * (0.98f + 0.03f * breathe)
                            scaleX = s
                            scaleY = s
                            alpha = markIn.coerceIn(0f, 1f)
                        },
                )
            }

            Spacer(Modifier.height(30.dp))
            Text(
                text = stringResource(R.string.brand_name),
                style = AreIptvTheme.typography.h2,
                color = White,
                modifier = Modifier.graphicsLayer {
                    alpha = brandIn.coerceIn(0f, 1f)
                    translationY = (1f - brandIn) * 16.dp.toPx()
                },
            )

            // Loading shimmer: a slim glass rail with a bright accent highlight sliding across it.
            Spacer(Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .width(128.dp)
                    .height(3.dp)
                    .graphicsLayer { alpha = brandIn.coerceIn(0f, 1f) }
                    .clip(RoundedCornerShape(AreIptvTheme.radius.pill))
                    .drawBehind {
                        drawRect(White.copy(alpha = 0.08f))
                        val head = sweep * 1.4f - 0.2f
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color.Transparent, accent, Color.Transparent),
                                startX = (head - 0.35f) * size.width,
                                endX = (head + 0.35f) * size.width,
                            ),
                        )
                    },
            )
        }
    }
}

private const val TAU = 6.2831855f

/** One wide radial mesh lobe, sized past the viewport so only its soft interior shows. */
private fun DrawScope.meshLobe(color: Color, alpha: Float, radius: Float, cx: Float, cy: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = alpha), Color.Transparent),
            center = Offset(cx, cy),
            radius = radius,
        ),
        radius = radius,
        center = Offset(cx, cy),
    )
}

/** Fractional part in [0,1). */
private fun frac(v: Float): Float = v - kotlin.math.floor(v)

/** Eased slice of the intro clock [p], mapping [start]..[end] onto 0..1. */
private fun seg(
    p: Float,
    start: Float,
    end: Float,
    easing: androidx.compose.animation.core.Easing,
): Float = easing.transform(((p - start) / (end - start)).coerceIn(0f, 1f))

/**
 * A ping-pong pulse in [from]..[to]. Under reduced motion it collapses to a still [mid] value so the
 * splash renders one calm frame -- mirroring AmbientBackdrop's reduced-motion path.
 */
@Composable
private fun androidx.compose.animation.core.InfiniteTransition.pulse(
    reduced: Boolean,
    mid: Float,
    from: Float,
    to: Float,
    durationMs: Int,
): State<Float> = if (reduced) {
    remember { mutableStateOf(mid) }
} else {
    animateFloat(
        initialValue = from,
        targetValue = to,
        animationSpec = infiniteRepeatable(tween(durationMs, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse",
    )
}

/** A restarting 0->1 pass for one-directional travelling motion; frozen if reduced. */
@Composable
private fun androidx.compose.animation.core.InfiniteTransition.sweepPass(
    reduced: Boolean,
    durationMs: Int,
): State<Float> = if (reduced) {
    remember { mutableStateOf(0f) }
} else {
    animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMs, easing = LinearEasing), RepeatMode.Restart),
        label = "sweep",
    )
}
