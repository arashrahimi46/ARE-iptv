package com.arashrahimi46.iptv

import android.app.Application
import io.sentry.android.core.SentryAndroid
import com.arashrahimi46.iptv.analytics.Analytics
import com.arashrahimi46.iptv.data.settings.UserSettings
import coil.ImageLoader
import kotlinx.coroutines.flow.first
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.arashrahimi46.iptv.data.repository.RecordingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

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

    /**
     * Live TV Recording (V1) launch-time reconciliation (design §6): any recording left RECORDING was
     * orphaned by a crash / power loss -- finalize it INTERRUPTED so no ghost "recording" survives a
     * restart. Runs once, off the main thread, on cold start.
     */
    override fun onCreate() {
        super.onCreate()

        // Sentry: DSN comes from the manifest; here we tag the build so issues group by release and
        // debug crashes stay filterable from real ones. Captures unhandled crashes + ANRs by default.
        SentryAndroid.init(this) { options ->
            options.environment = if (BuildConfig.DEBUG) "debug" else "production"
            // Log envelope traffic to logcat on debug builds only, so setup/wiring is verifiable.
            options.isDebug = BuildConfig.DEBUG
            options.release = "${BuildConfig.APPLICATION_ID}@${BuildConfig.VERSION_NAME}"
            // Sample a fifth of transactions for performance monitoring without flooding the quota.
            options.tracesSampleRate = 0.2
            // TV app: attaching screenshots to events is heavy and rarely useful on a 10-foot UI.
            options.isAttachScreenshot = false
        }

        // Product analytics: dormant until google-services.json is added (see Analytics doc). Seed
        // collection from the persisted opt-out before any screen/play event can fire.
        Analytics.init(this, enabledInitially = true)

        val crashReason = getString(R.string.recording_reason_crash)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            runCatching { Analytics.setEnabled(UserSettings(this@IptvApp).isAnalyticsEnabled.first()) }
            runCatching { RecordingRepository(this@IptvApp).reconcileOnLaunch(crashReason) }
        }
    }

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
