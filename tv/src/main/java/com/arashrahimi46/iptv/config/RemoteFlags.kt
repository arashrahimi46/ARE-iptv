package com.arashrahimi46.iptv.config

import android.content.Context
import com.arashrahimi46.iptv.BuildConfig
import com.arashrahimi46.iptv.R
import com.google.firebase.FirebaseApp
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Remote kill switch, backed by Firebase Remote Config.
 *
 * Two server-controlled keys decide whether this build is allowed to run:
 *
 *  - `app_disabled` (bool) -- a global off switch for every version. **True means off.**
 *  - `min_supported_version_code` (long) -- the lowest [BuildConfig.VERSION_CODE] still allowed. 0
 *    disables the gate. Prefer this over `app_disabled`: it retires a specific broken build while
 *    leaving everyone who has already updated alone.
 *
 * The negative polarity of `app_disabled` is chosen for safety, not style. Firebase hands back
 * `false` for any boolean it has no value for -- a missing key, a config that never loaded, a typo
 * in the console. With this naming every one of those accidents resolves to "not disabled", so the
 * app keeps working. Phrased as `app_enabled`, that same fallback would read as "not enabled" and
 * would take the app down.
 *
 * Remote Config also supports per-version *conditions* in its console, so `app_disabled` can be
 * targeted at one version without touching anyone else. The explicit version key exists anyway
 * because a number in a config is far harder to misread at 3am than a condition expression.
 *
 * **This fails open, deliberately and on every path.** No FirebaseApp, no network, a fetch error, a
 * malformed value, an exception anywhere in here -- all of them leave [state] at [AppGate.Allowed].
 * The defaults in `res/xml/remote_config_defaults.xml` describe a working app. The failure this
 * guards against is not "someone bypasses the kill switch"; it is "a Firebase outage bricks every
 * install simultaneously and no user can do anything about it". A kill switch that fails closed is
 * a remote self-destruct with an unreliable trigger.
 *
 * Nothing here blocks startup: [init] returns immediately and the fetch completes whenever it
 * completes. A cold start with no network is therefore indistinguishable from a normal one.
 */
object RemoteFlags {

    /** Whether this build may run, as far as we currently know. */
    sealed interface AppGate {
        /** Normal operation -- and the value we hold whenever we cannot prove otherwise. */
        data object Allowed : AppGate

        /** The server has explicitly retired this build. [updateAvailable] drives the store CTA. */
        data class Blocked(val updateAvailable: Boolean) : AppGate
    }

    private const val KEY_APP_DISABLED = "app_disabled"
    private const val KEY_MIN_VERSION = "min_supported_version_code"

    /**
     * How stale a cached config may be before a fetch actually hits the network. Twelve hours on
     * release keeps the kill switch responsive within a day without one fetch per launch; debug
     * builds refetch every time so a console change is testable immediately.
     */
    private val MIN_FETCH_INTERVAL_SECONDS: Long = if (BuildConfig.DEBUG) 0 else 12 * 60 * 60

    private val _state = MutableStateFlow<AppGate>(AppGate.Allowed)

    /** The current gate. Starts [AppGate.Allowed] and only ever tightens after a successful fetch. */
    val state: StateFlow<AppGate> = _state.asStateFlow()

    /**
     * Seeds from the last cached config and kicks off a refresh. Safe to call before Firebase is
     * configured -- it no-ops, exactly like [com.arashrahimi46.iptv.analytics.Analytics].
     */
    fun init(context: Context) {
        if (FirebaseApp.getApps(context).isEmpty()) return
        runCatching {
            val rc = FirebaseRemoteConfig.getInstance()
            rc.setConfigSettingsAsync(
                FirebaseRemoteConfigSettings.Builder()
                    .setMinimumFetchIntervalInSeconds(MIN_FETCH_INTERVAL_SECONDS)
                    .build(),
            )
            // Defaults load ASYNCHRONOUSLY, so the first apply() has to wait for them -- see the
            // VALUE_SOURCE_STATIC guard below for what reading too early actually costs.
            rc.setDefaultsAsync(R.xml.remote_config_defaults).addOnCompleteListener {
                // Values already on disk from a previous run, applied before the network is
                // consulted so a retired build is gated on the very first frame, not one launch late.
                apply(rc)
                rc.fetchAndActivate().addOnCompleteListener { apply(rc) }
            }
        }
    }

    /** Recomputes the gate from [rc]'s currently active values. Any failure leaves it open. */
    private fun apply(rc: FirebaseRemoteConfig) {
        runCatching {
            // STATIC means Remote Config holds neither a fetched value nor a loaded default for a
            // key, and hands back the type's zero value. Both reads below are written so that zero
            // value ALREADY means "allowed" -- `false` = not disabled, `0` = no version floor -- so
            // this is belt-and-braces rather than the only thing standing between users and a dead
            // app. An earlier revision keyed on `app_enabled`, where the same fallback meant "not
            // enabled", and blocked the app on every cold start before setDefaultsAsync finished.
            // Device log: "W FirebaseRemoteConfig: No value of type 'Boolean' exists for parameter
            // key 'app_enabled'". Absence of an answer must never be read as a "no".
            val disabled = rc.getValue(KEY_APP_DISABLED)
                .takeIf { it.source != FirebaseRemoteConfig.VALUE_SOURCE_STATIC }
                ?.asBoolean() ?: false
            val minVersion = rc.getValue(KEY_MIN_VERSION)
                .takeIf { it.source != FirebaseRemoteConfig.VALUE_SOURCE_STATIC }
                ?.asLong() ?: 0L
            val tooOld = minVersion > 0 && BuildConfig.VERSION_CODE < minVersion
            _state.value = when {
                // Retired by version: a newer build necessarily exists, so point at the store.
                tooOld -> AppGate.Blocked(updateAvailable = true)
                // Killed outright: updating may not help, so do not promise that it will.
                disabled -> AppGate.Blocked(updateAvailable = false)
                else -> AppGate.Allowed
            }
        }
    }
}
