package com.arashrahimi46.iptv

import android.app.Application
import android.content.pm.PackageManager
import com.arashrahimi46.iptv.core.R
import io.sentry.android.core.SentryAndroid
import com.arashrahimi46.iptv.analytics.Analytics
import com.arashrahimi46.iptv.config.RemoteFlags
import com.arashrahimi46.iptv.analytics.CrashReporting
import com.arashrahimi46.iptv.data.settings.UserSettings
import coil.ImageLoader
import kotlinx.coroutines.flow.first
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.arashrahimi46.iptv.data.repository.RecordingRepository
// :mobile's independent copies of the same two classes. This Application is the single entry point
// for BOTH form factors, so it is the one place in the app that is allowed to know about both data
// layers -- and it has to, because they persist to different files (see onCreate).
import com.arashrahimi46.iptv.mobile.data.settings.UserSettings as MobileUserSettings
import com.arashrahimi46.iptv.mobile.data.repository.RecordingRepository as MobileRecordingRepository
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
            // User opt-out gate. Dropping the event here (rather than not installing Sentry) is what
            // lets the handler still be in place from the very first line of onCreate -- see
            // [CrashReporting] for why the check is per-event instead of per-init.
            options.setBeforeSend { event, _ -> if (CrashReporting.isEnabled()) event else null }
        }

        // Product analytics: dormant until google-services.json is added (see Analytics doc). Seed
        // collection from the persisted opt-out before any screen/play event can fire.
        Analytics.init(this, enabledInitially = true)

        // Remote kill switch. Seeds from the cached config and refreshes in the background; never
        // blocks startup and fails open on every path (see RemoteFlags).
        RemoteFlags.init(this)

        val crashReason = getString(R.string.recording_reason_crash)
        // One APK now serves TVs and phones, and :tv and :mobile own SEPARATE data layers writing to
        // separate files (are_iptv.db / are_iptv_settings vs are_iptv_mobile.db /
        // are_iptv_mobile_settings). So this launch work has to read the store the running UI
        // actually writes to. Doing it unconditionally against :tv's -- as it did when :tv was a
        // TV-only app -- would mean a phone user's crash-reporting and analytics opt-outs were read
        // from a store their Settings screen never touches, i.e. silently ignoring a privacy choice.
        // It would also open a second Room instance on a database the phone UI never uses.
        //
        // FEATURE_LEANBACK, not a screen-size heuristic: it is the same signal the manifest's
        // launcher-category split resolves to, so this branch can never disagree with which Activity
        // the system actually launched.
        val isTv = packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            if (isTv) {
                val settings = UserSettings(this@IptvApp)
                runCatching { CrashReporting.setEnabled(settings.isCrashReportingEnabled.first()) }
                runCatching { Analytics.setEnabled(settings.isAnalyticsEnabled.first()) }
                runCatching { RecordingRepository(this@IptvApp).reconcileOnLaunch(crashReason) }
            } else {
                val settings = MobileUserSettings(this@IptvApp)
                runCatching { CrashReporting.setEnabled(settings.isCrashReportingEnabled.first()) }
                runCatching { Analytics.setEnabled(settings.isAnalyticsEnabled.first()) }
                runCatching { MobileRecordingRepository(this@IptvApp).reconcileOnLaunch(crashReason) }
            }
        }
    }

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .crossfade(false)
            .allowRgb565(true)
            // Lets AsyncImage decode a frame out of a recorded video URI (Recordings thumbnails).
            .components { add(VideoFrameDecoder.Factory()) }
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
