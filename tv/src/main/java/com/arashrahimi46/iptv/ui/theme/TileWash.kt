package com.arashrahimi46.iptv.ui.theme

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
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
fun Modifier.tileWash(shape: Shape, hue: Color): Modifier = composed {
    val alpha = AreIptvTheme.colors.tileWashAlpha
    this.background(
        Brush.linearGradient(
            colors = listOf(hue.copy(alpha = alpha), Color.Transparent),
            start = Offset.Zero,
            end = Offset.Infinite,
        ),
        shape,
    )
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
fun sampleTileWashHue(logoUrl: String?, drawable: android.graphics.drawable.Drawable?) {
    if (logoUrl == null || DominantColorCache.peek(logoUrl) != null) return
    val bitmap = (drawable as? BitmapDrawable)?.bitmap ?: return
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
 */
private object DominantColorCache {
    private const val MAX = 1024
    private val cache = mutableStateMapOf<String, Color>()

    fun peek(key: String): Color? = cache[key]

    @Composable
    fun observe(key: String): Color? = cache[key]

    fun put(key: String, value: Color) {
        if (cache.size >= MAX) cache.clear()
        cache[key] = value
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
