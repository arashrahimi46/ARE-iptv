package com.arashrahimi46.iptv.mobile.design

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import java.util.concurrent.ConcurrentHashMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.core.graphics.drawable.toBitmap
import coil.Coil
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlin.math.abs

/**
 * Per-tile artwork wash (design spec §6.3). §3 gives the *page* something behind its glass; a tile
 * needs the same one level down, and the material is already loaded -- so sample one dominant colour
 * from the logo Coil has already decoded and lay a two-stop gradient behind the tile's contents.
 *
 * A backdrop derived from *the same logo* is what makes the tile's logo well safe to turn
 * translucent: an arbitrary backdrop can clash with an arbitrary logo, a sampled one cannot.
 *
 * **Two gradient stops, never a blurred bitmap.** A live grid holds 40+ tiles; forty blurred bitmaps
 * mid-scroll is the whole jank budget, and at tile size a blurred logo is mud rather than colour.
 * Being this cheap is also why the wash is the one part of Glass V2 that ships on Tier C too.
 */
@Composable
fun Modifier.tileWash(shape: Shape, hue: Color): Modifier {
    val alpha = LocalAreIptvColors.current.tileWashAlpha
    // PERF: a plain @Composable extension, not `composed {}` (which is per-node and unskippable --
    // see the note in Glass.kt), and the Brush is remembered so 40 visible tiles don't each mint a
    // fresh gradient on every recomposition.
    val brush = remember(hue, alpha) {
        Brush.linearGradient(
            colors = listOf(hue.copy(alpha = alpha), Color.Transparent),
            start = Offset.Zero,
            end = Offset.Infinite,
        )
    }
    return this.background(brush, shape)
}

/**
 * Decoded artwork for a rail tile -- null until it arrives, and null forever if the load fails.
 *
 * Deliberately NOT Coil's `AsyncImage`. `AsyncImagePainter` is a `RememberObserver` that resolves its
 * own size from the layout constraints, tracks load state and drives recomposition through it -- and
 * on a TV rail that machinery, not the decode, is the cost. Measured on the XL95: composing one Home
 * rail spent ~29ms of a ~107ms frame inside `rememberAsyncImagePainter` + `AsyncImagePainter
 * .onRemembered`, and replacing it with one suspend `execute` plus a plain `Image` took that frame to
 * 76ms -- i.e. to within noise of drawing no artwork at all. Everything Coil is actually needed for
 * (HTTP, the disk and memory caches, RGB565, the video-frame decoder) still runs; only the Compose
 * painter layer is bypassed.
 *
 * [maxDimension] bounds the decode. Passing it is not optional: `execute` with no size decodes at the
 * source's full resolution, which on a 20k-title catalogue is how a rail scroll runs the heap out.
 *
 * [onBitmap] sees the raw bitmap before it is wrapped -- [AreChannelTile] samples its wash hue from
 * exactly this bitmap rather than issuing a second request (see [sampleTileWashHue]).
 */
@Composable
fun rememberTileArtwork(
    url: String?,
    maxDimension: Dp,
    onBitmap: (Bitmap) -> Unit = {},
): ImageBitmap? {
    val context = LocalContext.current
    val sizePx = with(LocalDensity.current) { maxDimension.roundToPx() }
    val artwork = remember(url) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(url, sizePx) {
        if (url == null) return@LaunchedEffect
        // toBitmap() rather than a BitmapDrawable cast: a provider can serve anything (animated GIF
        // logos are real), and a cast would silently leave those tiles on their initials forever.
        val bitmap = runCatching {
            val result = Coil.imageLoader(context).execute(
                ImageRequest.Builder(context).data(url).size(sizePx).build(),
            )
            (result as? SuccessResult)?.drawable?.toBitmap()
        }.getOrNull() ?: return@LaunchedEffect
        onBitmap(bitmap)
        artwork.value = bitmap.asImageBitmap()
    }
    return artwork.value
}

/**
 * Dominant colour for a tile, resolved once per [logoUrl] and memoised for the process lifetime.
 * Falls back to a hue derived deterministically from [seed] (the channel/title name) so logo-less
 * tiles still get colour, and so the *same* channel always gets the *same* hue across scroll and
 * across sessions.
 */
@Composable
fun rememberTileWashHue(logoUrl: String?, seed: String): Color {
    val fallback = remember(seed) { hueFromSeed(seed) }
    // Reads an observable cache that [sampleTileWashHue] fills from the logo the tile ALREADY
    // loaded, so a hue that arrives later recomposes just this tile.
    return logoUrl?.let { DominantColorCache.observe(it) } ?: fallback
}

/**
 * Derive the wash hue from a logo Coil has **already** decoded for this tile. Call from
 * `AsyncImage`'s `onState` on success.
 *
 * The first cut of this issued its own 48px `imageLoader.execute()` per tile to sample from. That
 * doubled the number of requests hitting the provider, and Xtream/M3U servers rate-limit by IP --
 * on a real playlist the logos stopped loading altogether and every tile fell back to initials.
 * Sampling the drawable we were handed costs one bitmap read and zero requests.
 */
fun sampleTileWashHue(logoUrl: String?, bitmap: Bitmap) {
    if (logoUrl == null || DominantColorCache.peek(logoUrl) != null) return
    // A hardware bitmap has no pixels to read on the CPU; copying one back is allowed but costs a
    // GPU readback, so skip it and keep the seeded fallback rather than stall a scrolling grid.
    if (bitmap.config == Bitmap.Config.HARDWARE) return
    runCatching { dominantColorOf(bitmap) }.getOrNull()?.let {
        DominantColorCache.put(logoUrl, Color(it))
    }
}

/**
 * Snapshot-backed so a late-arriving hue recomposes the tile, and bounded so a 20k-channel playlist
 * can't grow it without limit.
 *
 * PERF: one [MutableState] **per key**, deliberately NOT a `mutableStateMapOf`. Reading a
 * SnapshotStateMap subscribes the reader to the whole map, so with a map every logo that finished
 * decoding recomposed *every other tile* observing a hue -- on a 40-tile grid loading its logos that
 * is ~40x40 recompositions, all during the one moment the grid is least able to afford them. A
 * per-key state means a tile only ever wakes for its own hue.
 */
private object DominantColorCache {
    private const val MAX = 1024

    /** Concurrent because [put] is driven from Coil's `onState` callback while composition reads. */
    private val cache = ConcurrentHashMap<String, MutableState<Color?>>()

    fun peek(key: String): Color? = cache[key]?.value

    @Composable
    fun observe(key: String): Color? = remember(key) { entry(key) }.value

    fun put(key: String, value: Color) {
        entry(key).value = value
    }

    private fun entry(key: String): MutableState<Color?> {
        // A tile holds its own entry across recomposition, so a wholesale clear orphans the tiles
        // observing a dropped key -- they keep their seeded fallback rather than picking up a later
        // hue. That is cosmetic and only past 1024 distinct logos, which is why the cheap bound stays.
        if (cache.size >= MAX && !cache.containsKey(key)) cache.clear()
        return cache.getOrPut(key) { mutableStateOf(null) }
    }
}

private const val SAMPLE_PX = 48

/**
 * The average of the bitmap's saturated, non-transparent pixels, then pushed back to a usable
 * saturation. Plain averaging over *every* pixel returns grey for the typical white-on-transparent
 * provider logo -- precisely the tile we most need colour for -- so near-neutral pixels are skipped,
 * and if nothing survives we report null and the caller keeps its seeded fallback.
 */
internal fun dominantColorOf(bitmap: Bitmap): Int? {
    val w = bitmap.width.coerceAtMost(SAMPLE_PX)
    val h = bitmap.height.coerceAtMost(SAMPLE_PX)
    if (w <= 0 || h <= 0) return null
    val pixels = IntArray(w * h)
    bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

    var r = 0L
    var g = 0L
    var b = 0L
    var n = 0
    for (p in pixels) {
        if ((p ushr 24 and 0xFF) < 128) continue // transparent
        val pr = p shr 16 and 0xFF
        val pg = p shr 8 and 0xFF
        val pb = p and 0xFF
        if (maxOf(pr, pg, pb) - minOf(pr, pg, pb) < 24) continue // near-neutral: carries no hue
        r += pr
        g += pg
        b += pb
        n++
    }
    if (n == 0) return null

    val hsv = FloatArray(3)
    android.graphics.Color.RGBToHSV((r / n).toInt(), (g / n).toInt(), (b / n).toInt(), hsv)
    // Averaging desaturates; push it back up and pin value so every wash lands in the same band.
    hsv[1] = hsv[1].coerceIn(0.45f, 0.85f)
    hsv[2] = 0.72f
    return android.graphics.Color.HSVToColor(hsv)
}

/**
 * Deterministic hue from a name, for tiles with no logo. Stable across scroll and sessions because
 * it is a pure function of the string -- the same channel must never flicker between colours.
 */
internal fun hueFromSeed(seed: String): Color {
    var hash = 0
    for (c in seed) hash = hash * 31 + c.code
    val hue = abs(hash % 360).toFloat()
    return Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.62f, 0.72f)))
}
