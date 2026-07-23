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
