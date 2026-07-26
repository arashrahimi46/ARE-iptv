package com.arashrahimi46.iptv.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Thin product-analytics facade over Firebase/GA4, answering two questions: which parts of the app
 * users go to ([logScreen]) and what they watch ([logPlay]).
 *
 * Deliberately decoupled from build state: until a `google-services.json` is added and the
 * google-services plugin applied, no default [FirebaseApp] exists, [fa] stays null, and every call
 * here is a silent no-op — so the app compiles and runs identically with or without analytics wired.
 * Once the JSON lands, Firebase auto-initializes via its own ContentProvider and this activates.
 *
 * User opt-out ([enabled], driven by the Settings toggle) gates collection on top of that: it flips
 * Firebase's own collection flag AND short-circuits every log call, so a user who opts out sends
 * nothing.
 */
/**
 * User opt-out gate for Sentry crash/ANR diagnostics, and the reason the Privacy Policy can promise
 * one at all: before this existed, crash reporting ran unconditionally with no way to refuse.
 *
 * Deliberately a volatile flag consulted from Sentry's `beforeSend` rather than a conditional
 * `SentryAndroid.init`: the preference lives in DataStore (async), while Sentry has to be installed
 * in `Application.onCreate` to catch early crashes at all. Blocking startup on a disk read to decide
 * would cost cold-start time on every launch. So Sentry always installs, [enabled] starts at the
 * default (on), and the persisted choice lands a few ms later -- same shape as [Analytics].
 *
 * The gap that leaves: a crash in the first few ms of a cold start, before the read completes, is
 * still sent. It is bounded, it only ever applies to the default-on case, and closing it entirely
 * would mean losing exactly the startup crashes that matter most.
 */
object CrashReporting {
    @Volatile private var enabled: Boolean = true

    /** True when the user has not opted out; consulted per-event by Sentry's beforeSend hook. */
    fun isEnabled(): Boolean = enabled

    /** Apply a live opt-out change from Settings, or the persisted choice read at startup. */
    fun setEnabled(value: Boolean) {
        enabled = value
    }
}

object Analytics {
    private var fa: FirebaseAnalytics? = null
    @Volatile private var enabled: Boolean = true

    /** Call once on app start. [enabledInitially] is the persisted opt-out choice. */
    fun init(context: Context, enabledInitially: Boolean) {
        enabled = enabledInitially
        if (FirebaseApp.getApps(context).isNotEmpty()) {
            fa = FirebaseAnalytics.getInstance(context).apply {
                setAnalyticsCollectionEnabled(enabledInitially)
            }
        }
    }

    /** Apply a live opt-out change from Settings. */
    fun setEnabled(value: Boolean) {
        enabled = value
        fa?.setAnalyticsCollectionEnabled(value)
    }

    /** A screen/tab the user navigated to (Home, Live, Guide, Settings, …). */
    fun logScreen(name: String) {
        val fa = fa ?: return
        if (!enabled) return
        fa.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, name)
        })
    }

    /**
     * Content playback started. [type] is a coarse bucket (live/vod/episode/recording/direct),
     * [title] the channel or title name, [category] its group when known. Names are truncated to
     * keep GA4's per-param length limit and avoid unbounded cardinality.
     */
    fun logPlay(type: String, title: String, category: String?) {
        val fa = fa ?: return
        if (!enabled) return
        fa.logEvent("content_play", Bundle().apply {
            putString("content_type", type)
            putString("content_title", title.take(100))
            category?.takeIf { it.isNotBlank() }?.let { putString("content_category", it.take(100)) }
        })
    }
}
