package com.arashrahimi46.iptv.ui.language

import android.content.Context
import android.content.res.Configuration
import com.arashrahimi46.iptv.R
import java.util.Locale

/** Title + Confirm/Cancel labels for the Settings language-switch confirmation modal, phrased in
 * the TARGET language being switched to (not whatever locale is currently active). */
data class LanguageConfirmCopy(val title: String, val confirm: String, val cancel: String)

/**
 * Looks up [LanguageConfirmCopy] for [languageTag] from that language's OWN string resources via
 * `createConfigurationContext`, independent of the app's currently-active locale -- so the modal
 * can be phrased in French when switching TO French even while the app is still running in English.
 */
fun localizedConfirmCopy(context: Context, languageTag: String): LanguageConfirmCopy {
    val locale = Locale.forLanguageTag(languageTag)
    val config = Configuration(context.resources.configuration)
    config.setLocale(locale)
    val localizedContext = context.createConfigurationContext(config)
    return LanguageConfirmCopy(
        title = localizedContext.getString(R.string.language_confirm_title),
        confirm = localizedContext.getString(R.string.language_confirm_action),
        cancel = localizedContext.getString(R.string.language_confirm_cancel),
    )
}
