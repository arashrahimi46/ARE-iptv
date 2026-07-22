package com.arashrahimi46.iptv

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache

/**
 * App-wide Coil [ImageLoader] tuned for a set-top box driving a large catalog:
 *
 * - `crossfade(false)` — no per-image fade animation, so newly-loaded logos/posters
 *   snap in instantly instead of animating during a fast D-pad scroll (fades read as
 *   jank on a lazy grid where dozens of tiles resolve at once).
 * - `allowRgb565(true)` — halves the memory each decoded bitmap costs. On cheap TV
 *   hardware with a 300k-title catalog this is the difference between smooth scrolling
 *   and GC thrash / OOM. Posters/logos don't need an alpha channel, so quality is unaffected.
 * - Bounded memory + on-disk cache so re-focusing a row reuses decoded bitmaps instead
 *   of re-downloading and re-decoding.
 */
class IptvApp : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .crossfade(false)
            .allowRgb565(true)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(256L * 1024 * 1024)
                    .build()
            }
            .build()
}
