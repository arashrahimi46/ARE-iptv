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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.arashrahimi46.iptv.R
import com.arashrahimi46.iptv.core.R as CoreR
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.theme.LocalReducedMotion
import com.arashrahimi46.iptv.ui.theme.White
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Cold-start splash (Issue #13): the brand mark held high on pure black, with a slowly turning
 * dotted **globe** rising from the bottom edge -- only its top half is on screen, so the frame
 * reads as standing on a planet rather than looking at a ball.
 *
 * The globe is drawn procedurally, not textured. There is no map asset in the repo and per-pixel
 * sphere-mapping a bitmap is far too slow for TV hardware, so [WORLD] carries a 72x36 land/water
 * mask (5-degree cells) that is projected onto a sphere every frame. Per-cell trig is precomputed
 * once into [landCells]; a frame costs two `sin`/`cos` calls for the spin plus multiply-adds, and
 * the visible dots are flushed as four bucketed [DrawScope.drawPoints] batches rather than ~700
 * individual `drawCircle`s.
 *
 * The backdrop is ALWAYS black regardless of app theme, so the white mark's contrast is fixed
 * rather than white-on-white in light mode; the globe's hue comes from the active theme via
 * [AreIptvTheme.colors.accent].
 *
 * No `androidx.core.splashscreen` dependency exists yet (manifest has no windowSplashScreen theme),
 * so this is a plain Compose screen shown by [com.arashrahimi46.iptv.MainActivity] before the app
 * routes to the privacy gate / onboarding / shell.
 *
 * Everything is gated on [LocalReducedMotion]: the intro snaps to its final frame and the globe
 * freezes mid-rotation, leaving a still, fully-composed image.
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
    // PERF: none of the four animated values below is unwrapped here. `intro` is a 1900ms one-shot
    // and the other three are INFINITE, so a composition-scope read (`val p = intro.value`, or `by`
    // on the transitions) re-executed this entire composable at 60fps for the whole
    // SPLASH_FLOOR_MS = 3400ms cold start -- rebuilding the root drawBehind lambda, the Column, the
    // 132dp mark with its own drawBehind + graphicsLayer, two painterResource Images, a Text with a
    // stringResource lookup and the shimmer. That is pure waste competing with catalogue load and
    // Room open on a 4x A55, on the one screen whose whole job is to get out of the way quickly.
    // Every consumer is already a drawBehind/graphicsLayer lambda, so the reads just move inside
    // them and nothing about the animation changes.
    val transition = rememberInfiniteTransition(label = "splash")
    val breathe = transition.pulse(reduced, mid = 0.7f, from = 0.45f, to = 1f, durationMs = 2600)
    val sweep = transition.sweepPass(reduced, durationMs = 3400)
    val spin = transition.sweepPass(reduced, durationMs = 20_000)

    val cells = remember { landCells() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                val globeIn = seg(intro.value, 0.20f, 1f, FastOutSlowInEasing)
                drawRect(Color.Black)

                // Globe: centred exactly on the bottom edge so precisely half the sphere shows,
                // grown in on entrance so it appears to rise into frame.
                val radius = size.height * 0.40f * (0.90f + 0.10f * globeIn)
                val cx = size.width / 2f
                val cy = size.height

                // Atmosphere: a soft accent halo hugging the limb, plus a wide bleed above it.
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color.Transparent, accent.copy(alpha = 0.22f * globeIn), Color.Transparent),
                        center = Offset(cx, cy),
                        radius = radius * 1.30f,
                    ),
                    radius = radius * 1.30f,
                    center = Offset(cx, cy),
                )
                // Body: a barely-lifted disc so the dots read as sitting on a solid sphere, lit
                // from the upper left and falling to black at the limb.
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.16f * globeIn),
                            accent.copy(alpha = 0.05f * globeIn),
                            Color.Transparent,
                        ),
                        center = Offset(cx - radius * 0.34f, cy - radius * 0.42f),
                        radius = radius * 1.5f,
                    ),
                    radius = radius,
                    center = Offset(cx, cy),
                )
                // Terminator rim: a thin bright arc where the lit edge catches the light.
                drawCircle(
                    color = accent.copy(alpha = 0.30f * globeIn),
                    radius = radius,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.dp.toPx()),
                )

                drawGlobeDots(cells, cx, cy, radius, spin.value, accent, globeIn * (0.85f + 0.15f * breathe.value))
            },
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(0.16f))

            // Two layers: the glyph face in white, its detail pass punched back to black so the play
            // triangle and crossbar read as cut-outs. Both assets are alpha-only masks.
            //
            // The bloom is a drawBehind rather than a larger sibling Box on purpose: drawBehind is
            // unclipped, so the glow can bleed well past the 132dp mark without the mark's *layout*
            // slot growing to match. As a sibling it reserved its full diameter of vertical space and
            // shoved the wordmark down onto the globe.
            Box(
                modifier = Modifier
                    .size(132.dp)
                    .drawBehind {
                        val markIn = seg(intro.value, 0.05f, 0.50f, EaseOutBack)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(accent.copy(alpha = (0.16f + 0.13f * breathe.value) * markIn), Color.Transparent),
                                center = center,
                                radius = size.minDimension * 1.30f,
                            ),
                            radius = size.minDimension * 1.30f,
                        )
                    }
                    .graphicsLayer {
                        val markIn = seg(intro.value, 0.05f, 0.50f, EaseOutBack)
                        val s = 0.80f + 0.20f * markIn
                        scaleX = s
                        scaleY = s
                        alpha = markIn.coerceIn(0f, 1f)
                    },
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_logo_glyph),
                    contentDescription = stringResource(CoreR.string.splash_logo_content_description),
                    colorFilter = ColorFilter.tint(White, BlendMode.SrcIn),
                    modifier = Modifier.fillMaxSize(),
                )
                Image(
                    painter = painterResource(R.drawable.ic_logo_glyph_detail),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(Color.Black, BlendMode.SrcIn),
                    modifier = Modifier.fillMaxSize(),
                )
            }

            Spacer(Modifier.height(26.dp))
            Text(
                text = stringResource(CoreR.string.brand_name),
                style = AreIptvTheme.typography.h2,
                color = White,
                modifier = Modifier.graphicsLayer {
                    val brandIn = seg(intro.value, 0.34f, 0.68f, FastOutSlowInEasing)
                    alpha = brandIn.coerceIn(0f, 1f)
                    translationY = (1f - brandIn) * 16.dp.toPx()
                },
            )

            // Loading shimmer: a slim rail with a bright accent highlight sliding across it.
            Spacer(Modifier.height(22.dp))
            Box(
                modifier = Modifier
                    .width(128.dp)
                    .height(3.dp)
                    .graphicsLayer { alpha = seg(intro.value, 0.34f, 0.68f, FastOutSlowInEasing).coerceIn(0f, 1f) }
                    .clip(RoundedCornerShape(AreIptvTheme.radius.pill))
                    .drawBehind {
                        drawRect(White.copy(alpha = 0.08f))
                        val head = sweep.value * 1.4f - 0.2f
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color.Transparent, accent, Color.Transparent),
                                startX = (head - 0.35f) * size.width,
                                endX = (head + 0.35f) * size.width,
                            ),
                        )
                    },
            )

            Spacer(Modifier.weight(1f))
        }
    }
}

private const val TAU = 6.2831855f

/** Axial tilt, radians -- enough to sell "planet" without hiding the northern landmasses. */
private const val TILT = 0.30f

/**
 * Coarse world land mask, 72 columns x 36 rows of 5-degree cells.
 *
 * Row `r` is latitude `87.5 - 5r`, column `c` is longitude `-180 + 5c + 2.5`; `#` is land. Deliberately
 * low-fidelity -- at ~4px dots the continents only need to be recognisable in silhouette.
 */
private val WORLD = arrayOf(
    "........................................................................",
    "................#########..#####........................................",
    "..............####################.....##............####...............",
    "........##################.#######.....#################################",
    ".....#####################..#####...####################################",
    "....######################...####.######################################",
    "...#######################........######################################",
    "....#######################.......##.###################################",
    ".....#######################.......#####################################",
    ".......######################......#####################################",
    "........#####################......#####################################",
    "...........##################....######################################.",
    "...............#############.....#######################################",
    ".................############...###########################.####........",
    "...................###########..#################################.......",
    "..................########......#####################.##############....",
    "....................########.....##############........###.########.....",
    "....................#########.....#############...........#############.",
    "...................###########....##############..........#############.",
    "...................############...##############..........#############.",
    "....................###########...##############............####.##.....",
    ".....................###########..##############...........#########....",
    "......................##########..#############...........##########....",
    ".......................#########..############............#########.....",
    "........................#######....########................#######......",
    ".........................#####..............................#####.....##",
    "..........................####.......................................###",
    "..........................####..........................................",
    "...........................###..........................................",
    "............................#...........................................",
    "..........................#####.........................................",
    "#####################...################################################",
    "########################################################################",
    "########################################################################",
    "########################################################################",
    "########################################################################",
)

/**
 * Flattens [WORLD] into a packed `[cosLat, sinLat, sinLon, cosLon]` quad per land cell.
 *
 * Splitting longitude into its sine and cosine here is what makes the per-frame cost trivial: the
 * spin only rotates about the polar axis, so `sin(lon + spin)` expands to a multiply-add against
 * the frame's single `sin(spin)`/`cos(spin)` pair -- no trig inside the hot loop.
 *
 * Rows are thinned by `1/cos(lat)`. A fixed 5-degree longitude step is a *shrinking* arc as you
 * climb toward a pole, so keeping every cell packs Siberia and northern Canada into dense concentric
 * arcs while the tropics stay sparse -- it reads as a coiled spring, not a planet. Dropping cells in
 * proportion to the convergence holds the surface density roughly even from equator to pole.
 */
private fun landCells(): FloatArray {
    val out = ArrayList<Float>(4096)
    for (r in WORLD.indices) {
        val lat = (87.5f - 5f * r) * TAU / 360f
        val cosLat = cos(lat)
        val sinLat = sin(lat)
        val step = (1f / cosLat.coerceAtLeast(0.04f)).toInt().coerceIn(1, 24)
        val row = WORLD[r]
        for (c in row.indices) {
            if (row[c] != '#' || c % step != 0) continue
            val lon = (-177.5f + 5f * c) * TAU / 360f
            out.add(cosLat)
            out.add(sinLat)
            out.add(sin(lon))
            out.add(cos(lon))
        }
    }
    return out.toFloatArray()
}

/** Alpha buckets the dots are quantised into, so the whole globe flushes in [BUCKETS] draw calls. */
private const val BUCKETS = 4

/**
 * Projects every land cell onto the sphere and draws the front-facing ones.
 *
 * Back-facing cells (`z <= 0`) and cells below the bottom edge are dropped, and the survivors are
 * quantised into [BUCKETS] brightness tiers -- one [DrawScope.drawPoints] per tier. Brightness
 * combines a fixed upper-left key light with a limb falloff, which is what gives the flat dot field
 * its curvature.
 */
private fun DrawScope.drawGlobeDots(
    cells: FloatArray,
    cx: Float,
    cy: Float,
    radius: Float,
    spin: Float,
    accent: Color,
    alpha: Float,
) {
    if (alpha <= 0.01f) return
    val a = spin * TAU
    val sinS = sin(a)
    val cosS = cos(a)
    val sinT = sin(TILT)
    val cosT = cos(TILT)
    // Key light, normalised: up and to the left, tipped toward the viewer.
    val lx = -0.46f
    val ly = 0.55f
    val lz = 0.70f

    val buckets = Array(BUCKETS) { ArrayList<Offset>(256) }
    var i = 0
    while (i < cells.size) {
        val cosLat = cells[i]
        val sinLat = cells[i + 1]
        val sinLon0 = cells[i + 2]
        val cosLon0 = cells[i + 3]
        i += 4

        // Spin about the polar axis, then tilt about X.
        val sinLon = sinLon0 * cosS + cosLon0 * sinS
        val cosLon = cosLon0 * cosS - sinLon0 * sinS
        val x = cosLat * sinLon
        val y0 = sinLat
        val z0 = cosLat * cosLon
        val y = y0 * cosT - z0 * sinT
        val z = y0 * sinT + z0 * cosT
        if (z <= 0.02f) continue

        val sy = cy - y * radius
        if (sy > cy) continue // below the screen edge -- the hidden half of the disc

        val lit = (x * lx + y * ly + z * lz).coerceAtLeast(0f)
        // Limb falloff: dots near the silhouette dim out so the sphere doesn't end in a hard ring.
        val v = (0.20f + 0.80f * lit) * sqrt(z) * alpha
        val b = (v * BUCKETS).toInt().coerceIn(0, BUCKETS - 1)
        if (v < 0.05f) continue
        buckets[b].add(Offset(cx + x * radius, sy))
    }

    val dot = radius * 0.0125f
    for (b in 0 until BUCKETS) {
        val pts = buckets[b]
        if (pts.isEmpty()) continue
        val tier = (b + 1f) / BUCKETS
        drawPoints(
            points = pts,
            pointMode = PointMode.Points,
            color = accent.copy(alpha = (0.22f + 0.78f * tier) * alpha),
            strokeWidth = dot * (0.72f + 0.42f * tier),
            cap = StrokeCap.Round,
        )
    }
}

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
