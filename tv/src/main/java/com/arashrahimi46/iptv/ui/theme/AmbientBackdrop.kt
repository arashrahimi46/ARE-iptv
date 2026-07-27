package com.arashrahimi46.iptv.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kyant.backdrop.Backdrop
import kotlin.math.cos
import kotlin.math.sin

/**
 * The artwork the ambient backdrop should currently wash the page with -- normally the poster/logo of
 * whatever the user has focused. Browse screens publish into this from their tiles' focus handlers;
 * screens with no artwork (Settings, Onboarding) simply leave it null and get the accent mesh alone.
 *
 * Deliberately **focused-item only**, not a row-level dominant palette: it is cheaper, it responds
 * immediately, and the cross-fade already absorbs fast D-pad scrolling (design §13).
 */
val LocalAmbientArtwork = staticCompositionLocalOf<MutableState<String?>> {
    error("LocalAmbientArtwork not provided -- AreIptvAppShell owns it")
}

/**
 * The layer [Modifier.glassSurface] samples and blurs. Provided by [AreIptvAppShell] and captured
 * from [AmbientBackdrop] **only** -- deliberately not from the page content, which would feed glass
 * surfaces back into their own backdrop. Null outside the shell (dialogs, the player), where glass
 * falls through to the V1 translucent-fill path.
 */
val LocalAppBackdrop = staticCompositionLocalOf<Backdrop?> { null }

/**
 * The ambient backdrop **plus the page content** -- for the one surface class that genuinely floats
 * OVER the page: the expanded sidebar. Only ever provided while the sidebar is expanded (the shell
 * gates it), and only ever sampled by that panel, so there is no feedback: the sidebar is drawn as a
 * sibling *outside* this layer and so is never inside its own backdrop.
 *
 * Why it exists: [LocalAppBackdrop] captures the ambient wash only, and that wash starts with an
 * opaque `bgBase`. A surface sampling it therefore *repaints the page's own background over the page*
 * inside its bounds -- so however sheer the fill, the expanded sidebar could never show the rails it
 * covers. Arithmetic, not tuning: there was nothing but ambient behind it in the buffer.
 *
 * This is also the one place a per-surface blur is warranted (§4 says blur belongs on the layer that
 * captures *sharp* content, not re-derived per surface): posters and text behind the panel are sharp,
 * and the blur is what turns "transparent window" into frosted glass while keeping the labels legible.
 */
val LocalPageBackdrop = staticCompositionLocalOf<Backdrop?> { null }

/**
 * The thing glass refracts (design spec §3) -- **the single highest-value change in Glass V2**.
 *
 * V1 painted one flat opaque `bgBase` behind everything, so `surfaceGlass` at 55% composited to a
 * flat `#14171E`: glass over an opaque page is arithmetically just a lighter grey, and no border,
 * shadow, blur or shader can recover depth that was never in the buffer. This puts content there.
 *
 * Back to front:
 *  1. `bgBase` -- the contrast floor, unchanged.
 *  2. Artwork wash -- the focused item's image, heavily blurred at low alpha (Tier A/B only; `blur`
 *     is API 31+). Cross-fades on focus change via [AsyncImage]'s own fade.
 *  3. Accent mesh -- wide radial gradients in the active [AccentPreset], drifting very slowly. This
 *     is what keeps screens with no artwork alive.
 *  4. Veil + vignette -- pulls the edges back down so the sidebar and HUD keep a contrast anchor.
 *
 * Drawn ONCE at the shell level. Never per-screen, never per-card.
 */
@Composable
fun AmbientBackdrop(modifier: Modifier = Modifier) {
    val colors = AreIptvTheme.colors
    val tier = LocalGlassTier.current
    val artwork by LocalAmbientArtwork.current
    val context = LocalContext.current

    androidx.compose.foundation.layout.Box(modifier.fillMaxSize().background(colors.bgBase)) {
        if (artwork != null && tier.hasBackdropBlur) {
            AsyncImage(
                // PERF: decode SMALL. Coil otherwise sizes the bitmap to this composable -- i.e.
                // a full 1080p ARGB decode of every poster focus settles on -- only to hand it to
                // a 72dp blur that destroys all of that detail anyway. 128px upscaled through the
                // same blur is indistinguishable and costs a fraction of the decode and of the
                // texture upload feeding the RenderEffect. This runs on the focus-settle path, so
                // it is exactly the work that shows up as a dropped frame on a TV SoC.
                model = remember(artwork) {
                    ImageRequest.Builder(context).data(artwork).size(128).build()
                },
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = if (colors.isDark) 0.30f else 0.18f,
                modifier = Modifier.fillMaxSize().blur(72.dp),
            )
        }
        AmbientMesh(
            meshA = colors.accent.copy(alpha = if (colors.isDark) 0.20f else 0.13f),
            meshB = colors.accentHover.copy(alpha = if (colors.isDark) 0.13f else 0.09f),
            veil = colors.backdropVeil,
            vignette = colors.bgSunken.copy(alpha = 0.55f),
        )
    }
}

/**
 * The static accent mesh: two wide radial lobes + veil + vignette.
 *
 * Its own composable, taking only [Color]s, for one specific reason: it must be SKIPPABLE. The bake
 * below lives in `drawWithCache`, which re-runs when the modifier is recreated -- and inlined into
 * [AmbientBackdrop] it was recreated on every recomposition of that function, i.e. every time the
 * focused artwork changed. On a browse screen that is every D-pad step, so the mesh would have been
 * re-rasterized and a fresh multi-MB bitmap allocated per focus move: far worse than the per-frame
 * gradients it replaced. All four params are stable, so an artwork change now skips this entirely and
 * the bake happens once per size/theme.
 */
@Composable
private fun AmbientMesh(meshA: Color, meshB: Color, veil: Color, vignette: Color) {
    // V2 max-smoothness: the mesh is STATIC. It used to drift on a 42s loop (Tier A only), but that
    // animation invalidated this backdrop's draw every frame -- and since every glass surface samples
    // this layer, each one re-blurred continuously, a constant GPU cost even on a fully idle screen,
    // for a motion far too slow to perceive. A fixed phase keeps the exact look and lets an idle
    // screen do zero backdrop work. It is also what makes the bake below correct.
    val t = 0f
    androidx.compose.foundation.layout.Box(
            // PERF: the mesh is BAKED into a cached ImageBitmap and blitted, rather than re-rasterized
            // per frame. `drawWithCache` already hoisted the Brush *construction* out of the draw pass,
            // but the four fills themselves still ran every frame: two viewport-sized radial lobes, the
            // veil and the vignette, i.e. ~4 full-screen gradient evaluations at 1080p. And because this
            // subtree is ALSO captured by `Modifier.layerBackdrop` (the layer every glass surface
            // samples), it is drawn TWICE per frame -- ~8 full-screen gradient passes before a single
            // row of content is painted. Measured on the TV emulator that was the entire frame budget:
            // a Settings D-pad scroll sat at 27ms/frame with 13.5ms of GPU, while Compose itself
            // (composition + measure + layout) used 0.04ms. This was never a recomposition problem.
            //
            // Baking is safe because the mesh is STATIC -- `t` is fixed at 0 (see above), so nothing
            // here varies frame to frame; it is a pure function of size and theme. Compositing the four
            // layers into one transparent buffer and drawing that buffer is pixel-identical to drawing
            // them in sequence, because Porter-Duff source-over is associative. Baking the artwork or
            // `bgBase` in too would NOT be safe: the artwork cross-fades, and it has to stay BETWEEN
            // `bgBase` and this mesh.
            //
            // Rendered at half resolution and bilinearly upscaled. These are gradients whose smallest
            // feature spans ~1600px, so they carry no detail a half-res buffer can lose -- and it cuts
            // the cached bitmap from ~8MB to ~2MB, which matters more on a TV than on a phone.
            Modifier.fillMaxSize().drawWithCache {
                val bakeW = (size.width / 2f).toInt().coerceAtLeast(1)
                val bakeH = (size.height / 2f).toInt().coerceAtLeast(1)
                val bakeSize = Size(bakeW.toFloat(), bakeH.toFloat())
                val baked = ImageBitmap(bakeW, bakeH)
                // Geometry below is expressed as fractions of `size`, so evaluating it against the
                // half-res `bakeSize` scales it correctly -- the upscale puts it back exactly.
                CanvasDrawScope().draw(this, layoutDirection, Canvas(baked), bakeSize) {
                    // Two wide radial lobes, sized well past the viewport
                    // so only their soft interiors are ever on screen -- no visible gradient edge.
                    val r = size.maxDimension * 0.85f
                    val centerA = Offset(
                        size.width * (0.22f + 0.10f * cos(t * 6.2832f)),
                        size.height * (0.18f + 0.08f * sin(t * 6.2832f)),
                    )
                    val centerB = Offset(
                        size.width * (0.86f - 0.09f * sin(t * 6.2832f)),
                        size.height * (0.78f - 0.07f * cos(t * 6.2832f)),
                    )
                    val radiusB = r * 0.9f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(meshA, Color.Transparent),
                            center = centerA,
                            radius = r,
                        ),
                        radius = r,
                        center = centerA,
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(meshB, Color.Transparent),
                            center = centerB,
                            radius = radiusB,
                        ),
                        radius = radiusB,
                        center = centerB,
                    )
                    // Veil + vignette: the contrast floor every text token is measured against.
                    if (veil.alpha > 0f) drawRect(veil)
                    drawRect(
                        Brush.radialGradient(
                            colors = listOf(Color.Transparent, vignette),
                            center = Offset(size.width * 0.5f, size.height * 0.45f),
                            radius = size.maxDimension * 0.72f,
                        ),
                    )
                }
                val dst = IntSize(size.width.toInt(), size.height.toInt())
                onDrawBehind {
                    drawImage(image = baked, dstSize = dst, filterQuality = FilterQuality.Low)
                }
            },
    )
}
