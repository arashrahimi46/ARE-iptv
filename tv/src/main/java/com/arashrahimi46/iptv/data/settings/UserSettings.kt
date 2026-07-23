package com.arashrahimi46.iptv.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.arashrahimi46.iptv.ui.home.DEFAULT_HOME_LAYOUT
import com.arashrahimi46.iptv.ui.home.HomeSection
import com.arashrahimi46.iptv.ui.home.decodeHomeLayout
import com.arashrahimi46.iptv.ui.home.encodeHomeLayout
import com.arashrahimi46.iptv.ui.theme.AccentPreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "are_iptv_settings")

/** How the docked mini-player avoids covering the tab content behind it: [DODGE] slides it to the
 * free corner when focus reaches its slot (returning home when focus leaves); [FADE] keeps it put
 * but fades+shrinks it while the user browses. See [com.arashrahimi46.iptv.ui.player.LiveMiniPlayerOverlay]. */
enum class MiniPlayerBehavior { DODGE, FADE }

/**
 * Preferences DataStore for app-wide settings that outlive a single screen.
 * Fully wired to the real Settings screen (Phase 4) -- see
 * [com.arashrahimi46.iptv.ui.settings.SettingsScreen].
 */
class UserSettings(private val context: Context) {
    private object Keys {
        val ACTIVE_SOURCE_ID = longPreferencesKey("active_source_id")
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        /** Selected accent preset id per theme mode (see [AccentPreset]); independent choices. */
        val ACCENT_DARK = stringPreferencesKey("accent_dark")
        val ACCENT_LIGHT = stringPreferencesKey("accent_light")
        val REDUCED_MOTION = booleanPreferencesKey("reduced_motion")
        val HARDWARE_DECODING = booleanPreferencesKey("hardware_decoding")
        val AUTOPLAY_NEXT_EPISODE = booleanPreferencesKey("autoplay_next_episode")
        val MINI_PLAYER_BEHAVIOR = stringPreferencesKey("mini_player_behavior")
        val PARENTAL_LOCK_ENABLED = booleanPreferencesKey("parental_lock_enabled")
        val PARENTAL_PIN_HASH = stringPreferencesKey("parental_pin_hash")
        val PARENTAL_PIN_SALT = stringPreferencesKey("parental_pin_salt")
        val GUIDE_SELECTED_CATEGORY = stringPreferencesKey("guide_selected_category")
        val BROWSE_LIST_MODE = booleanPreferencesKey("browse_list_mode")
        val TERMS_ACCEPTED = booleanPreferencesKey("terms_accepted")
        val ANALYTICS_ENABLED = booleanPreferencesKey("analytics_enabled")
        val RECORDING_INDICATOR = booleanPreferencesKey("recording_indicator")
        val OPENSUBS_CRED = stringPreferencesKey("opensubs_cred")
        val SUBTITLE_LANGUAGE = stringPreferencesKey("subtitle_language")
        val OPENSUBS_U = stringPreferencesKey("opensubs_u")
        val OPENSUBS_P = stringPreferencesKey("opensubs_p")
        val OMDB_KEY = stringPreferencesKey("omdb_key")
        val HOME_LAYOUT = stringPreferencesKey("home_layout")
        /** BCP-47 app language tag ("en", "es", "fr", "de", "it", "pt-BR"); see [languageTag]. */
        val LANGUAGE_TAG = stringPreferencesKey("language_tag")
        /** Whether the first-run language selector has been completed; see [hasSelectedLanguage]. */
        val LANGUAGE_CHOSEN = booleanPreferencesKey("language_chosen")
        /** Pinned category names, namespaced per browse screen (see [pinnedCategoriesKey]). */
        fun pinnedCategoriesKey(namespace: String) = stringSetPreferencesKey("pinned_categories_$namespace")

        /** Curated multi-view channel ids, per source (see [multiViewChannelIds]). */
        fun multiViewChannelsKey(sourceId: Long) = stringPreferencesKey("multiview_channels_$sourceId")
    }

    val activeSourceId: Flow<Long?> = context.dataStore.data.map { prefs ->
        prefs[Keys.ACTIVE_SOURCE_ID]?.takeIf { it > 0 }
    }

    val isDarkTheme: Flow<Boolean> = context.dataStore.data.map { it[Keys.DARK_THEME] ?: true }

    val isReducedMotion: Flow<Boolean> = context.dataStore.data.map { it[Keys.REDUCED_MOTION] ?: false }

    /** Accent preset chosen for dark mode; defaults to [AccentPreset.BLUE] (the original accent). */
    val darkAccent: Flow<AccentPreset> = context.dataStore.data.map { AccentPreset.fromId(it[Keys.ACCENT_DARK]) }

    /** Accent preset chosen for light mode; independent of [darkAccent], defaults to BLUE. */
    val lightAccent: Flow<AccentPreset> = context.dataStore.data.map { AccentPreset.fromId(it[Keys.ACCENT_LIGHT]) }

    /** ExoPlayer decoder-fallback preference -- see [com.arashrahimi46.iptv.ui.player.LivePlayerScreen]'s `ExoPlayer.Builder`. */
    val isHardwareDecoding: Flow<Boolean> = context.dataStore.data.map { it[Keys.HARDWARE_DECODING] ?: true }

    /**
     * Preference only in this phase -- persisted for real, but no auto-advance-on-completion
     * listener is wired yet (documented follow-up; see report). [com.arashrahimi46.iptv.ui.detail.DetailScreen]'s
     * episode list already lets a user manually pick the next episode.
     */
    val isAutoplayNextEpisode: Flow<Boolean> = context.dataStore.data.map { it[Keys.AUTOPLAY_NEXT_EPISODE] ?: true }

    /** Docked mini-player anti-occlusion behavior; defaults to [MiniPlayerBehavior.DODGE]. */
    val miniPlayerBehavior: Flow<MiniPlayerBehavior> = context.dataStore.data.map { prefs ->
        prefs[Keys.MINI_PLAYER_BEHAVIOR]?.let { runCatching { MiniPlayerBehavior.valueOf(it) }.getOrNull() } ?: MiniPlayerBehavior.DODGE
    }

    val isParentalLockEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.PARENTAL_LOCK_ENABLED] ?: false }

    /** Salted SHA-256 hash -- never the raw PIN. Null when no PIN has been set yet. */
    val parentalPinHash: Flow<String?> = context.dataStore.data.map { it[Keys.PARENTAL_PIN_HASH] }
    val parentalPinSalt: Flow<String?> = context.dataStore.data.map { it[Keys.PARENTAL_PIN_SALT] }

    /** Last channel-group filter picked on the Guide screen (see [com.arashrahimi46.iptv.ui.guide.GuideViewModel]); "All" when unset. */
    val guideSelectedCategory: Flow<String> = context.dataStore.data.map { it[Keys.GUIDE_SELECTED_CATEGORY] ?: "All" }

    /** First-run Privacy & Terms acceptance (Issue #11) -- false until the user explicitly accepts once. */
    val hasAcceptedTerms: Flow<Boolean> = context.dataStore.data.map { it[Keys.TERMS_ACCEPTED] ?: false }

    /** Anonymous usage analytics (Firebase/GA4) opt-out; on by default, a Settings switch flips it.
     * Read once at startup to seed [com.arashrahimi46.iptv.analytics.Analytics] collection state. */
    val isAnalyticsEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.ANALYTICS_ENABLED] ?: true }

    /** Show the pulsing "REC" badge in the player HUD while recording; on by default (see
     * [com.arashrahimi46.iptv.ui.components.RecordingIndicator]). */
    val isRecordingIndicatorEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.RECORDING_INDICATOR] ?: true }

    /** True renders Live TV/Movies/Series as a list instead of the default tile grid (see [com.arashrahimi46.iptv.ui.browse.BrowseLayout]). */
    val isBrowseListMode: Flow<Boolean> = context.dataStore.data.map { it[Keys.BROWSE_LIST_MODE] ?: false }

    /** Preferred subtitle language (ISO code) pre-selected when searching subtitles online; defaults to English. */
    val subtitleLanguage: Flow<String> = context.dataStore.data.map { it[Keys.SUBTITLE_LANGUAGE] ?: "en" }

    /** User's personal OpenSubtitles API key for online subtitle search; null until they connect one in Settings. */
    val openSubsCredential: Flow<String?> = context.dataStore.data.map { it[Keys.OPENSUBS_CRED]?.takeIf { k -> k.isNotBlank() } }

    /** User's personal OMDb API key for movie/series metadata (IMDb & Rotten Tomatoes ranks, plot,
     * cast); null until they connect one in Settings. See [com.arashrahimi46.iptv.data.parser.OmdbClient]. */
    val omdbKey: Flow<String?> = context.dataStore.data.map { it[Keys.OMDB_KEY]?.takeIf { k -> k.isNotBlank() } }

    /** OpenSubtitles account username -- needed (with [openSubsPhrase]) to log in for downloads. */
    val openSubsUsername: Flow<String?> = context.dataStore.data.map { it[Keys.OPENSUBS_U]?.takeIf { k -> k.isNotBlank() } }

    /** OpenSubtitles account password, stored to re-authenticate when the login token expires (~24h). */
    val openSubsPhrase: Flow<String?> = context.dataStore.data.map { it[Keys.OPENSUBS_P]?.takeIf { k -> k.isNotBlank() } }

    /** Persisted Home rail order/visibility; defaults to [DEFAULT_HOME_LAYOUT] until the user
     * customizes it (or if the stored value decodes to nothing usable). */
    val homeLayout: Flow<List<HomeSection>> = context.dataStore.data.map { prefs ->
        prefs[Keys.HOME_LAYOUT]?.let(::decodeHomeLayout)?.takeIf { it.isNotEmpty() } ?: DEFAULT_HOME_LAYOUT
    }

    /** BCP-47 app language tag ("en", "es", "fr", "de", "it", "pt-BR"); mirrors whatever was last
     * applied via `AppCompatDelegate.setApplicationLocales` so app logic can read "current
     * language" without touching AppCompatDelegate internals directly. Defaults to "en". */
    val languageTag: Flow<String> = context.dataStore.data.map { it[Keys.LANGUAGE_TAG] ?: "en" }

    /** Whether the first-run language selector has already been completed once; gates that screen
     * so it is shown exactly once, on first app open. Defaults to false. */
    val hasSelectedLanguage: Flow<Boolean> = context.dataStore.data.map { it[Keys.LANGUAGE_CHOSEN] ?: false }

    suspend fun setActiveSourceId(id: Long) {
        context.dataStore.edit { it[Keys.ACTIVE_SOURCE_ID] = id }
    }

    /** Drop the active-source pointer (e.g. the active playlist was deleted) so nothing points at a
     *  gone source; [activeSourceId] then reads null until the user picks another. */
    suspend fun clearActiveSourceId() {
        context.dataStore.edit { it.remove(Keys.ACTIVE_SOURCE_ID) }
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DARK_THEME] = enabled }
    }

    suspend fun setReducedMotion(enabled: Boolean) {
        context.dataStore.edit { it[Keys.REDUCED_MOTION] = enabled }
    }

    /** Persists the accent preset for a single mode; the other mode's accent is untouched. */
    suspend fun setAccent(isDark: Boolean, preset: AccentPreset) {
        context.dataStore.edit { it[if (isDark) Keys.ACCENT_DARK else Keys.ACCENT_LIGHT] = preset.id }
    }

    suspend fun setHardwareDecoding(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HARDWARE_DECODING] = enabled }
    }

    suspend fun setAutoplayNextEpisode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTOPLAY_NEXT_EPISODE] = enabled }
    }

    suspend fun setMiniPlayerBehavior(choice: MiniPlayerBehavior) {
        context.dataStore.edit { it[Keys.MINI_PLAYER_BEHAVIOR] = choice.name }
    }

    suspend fun setParentalLockEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.PARENTAL_LOCK_ENABLED] = enabled }
    }

    /** Stores only the salted hash produced by [com.arashrahimi46.iptv.data.settings.PinHasher]. */
    suspend fun setParentalPin(hash: String, salt: String) {
        context.dataStore.edit {
            it[Keys.PARENTAL_PIN_HASH] = hash
            it[Keys.PARENTAL_PIN_SALT] = salt
        }
    }

    suspend fun clearParentalPin() {
        context.dataStore.edit {
            it.remove(Keys.PARENTAL_PIN_HASH)
            it.remove(Keys.PARENTAL_PIN_SALT)
        }
    }

    suspend fun setGuideSelectedCategory(category: String) {
        context.dataStore.edit { it[Keys.GUIDE_SELECTED_CATEGORY] = category }
    }

    suspend fun setBrowseListMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.BROWSE_LIST_MODE] = enabled }
    }

    suspend fun setSubtitleLanguage(code: String) {
        context.dataStore.edit { it[Keys.SUBTITLE_LANGUAGE] = code }
    }

    /** Persists a validated OpenSubtitles API key, or clears it when [key] is null/blank. */
    suspend fun setOpenSubsCredential(key: String?) {
        context.dataStore.edit {
            if (key.isNullOrBlank()) it.remove(Keys.OPENSUBS_CRED) else it[Keys.OPENSUBS_CRED] = key.trim()
        }
    }

    /** Persists a validated OMDb API key, or clears it when [key] is null/blank. */
    suspend fun setOmdbKey(key: String?) {
        context.dataStore.edit {
            if (key.isNullOrBlank()) it.remove(Keys.OMDB_KEY) else it[Keys.OMDB_KEY] = key.trim()
        }
    }

    /** Persists (or clears) the OpenSubtitles account login used for downloads. */
    suspend fun setOpenSubsAccount(username: String?, phrase: String?) {
        context.dataStore.edit {
            if (username.isNullOrBlank() || phrase.isNullOrBlank()) {
                it.remove(Keys.OPENSUBS_U)
                it.remove(Keys.OPENSUBS_P)
            } else {
                it[Keys.OPENSUBS_U] = username.trim()
                it[Keys.OPENSUBS_P] = phrase
            }
        }
    }

    suspend fun setTermsAccepted(accepted: Boolean) {
        context.dataStore.edit { it[Keys.TERMS_ACCEPTED] = accepted }
    }

    suspend fun setAnalyticsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.ANALYTICS_ENABLED] = enabled }
    }

    suspend fun setRecordingIndicatorEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.RECORDING_INDICATOR] = enabled }
    }

    suspend fun setHomeLayout(sections: List<HomeSection>) {
        context.dataStore.edit { it[Keys.HOME_LAYOUT] = encodeHomeLayout(sections) }
    }

    /** Persists the chosen app language tag and marks the first-run selector as completed. Callers
     * are also expected to apply the locale via `AppCompatDelegate.setApplicationLocales` -- this
     * only mirrors the choice into DataStore (see [languageTag] doc). */
    suspend fun setLanguageTag(tag: String) {
        context.dataStore.edit {
            it[Keys.LANGUAGE_TAG] = tag
            it[Keys.LANGUAGE_CHOSEN] = true
        }
    }

    /**
     * Category names the user pinned to the top of a browse screen's group column.
     * Namespaced per screen ("live"/"movies"/"series") so pinning "Sports" in Live TV
     * doesn't also pin a same-named genre under Movies. Empty until the user pins one.
     */
    fun pinnedCategories(namespace: String): Flow<Set<String>> =
        context.dataStore.data.map { it[Keys.pinnedCategoriesKey(namespace)] ?: emptySet() }

    suspend fun togglePinnedCategory(namespace: String, name: String) {
        context.dataStore.edit { prefs ->
            val key = Keys.pinnedCategoriesKey(namespace)
            val current = prefs[key] ?: emptySet()
            prefs[key] = if (name in current) current - name else current + name
        }
    }

    /**
     * Multi-view is a curated, persistent list of live channels the user explicitly added --
     * per source, in insertion order (oldest first) so a full list evicts FIFO. Stored as a CSV
     * of channel ids; see [com.arashrahimi46.iptv.ui.multiview.MultiViewViewModel].
     */
    fun multiViewChannelIds(sourceId: Long): Flow<List<Long>> =
        context.dataStore.data.map { it[Keys.multiViewChannelsKey(sourceId)].parseIdCsv() }

    /** Append [channelId] (no duplicates); when already at [max], evict the oldest (FIFO). */
    suspend fun addMultiViewChannel(sourceId: Long, channelId: Long, max: Int) {
        context.dataStore.edit { prefs ->
            val key = Keys.multiViewChannelsKey(sourceId)
            val current = prefs[key].parseIdCsv()
            if (channelId in current) return@edit
            val next = (current + channelId).let { if (it.size > max) it.takeLast(max) else it }
            prefs[key] = next.joinToString(",")
        }
    }

    suspend fun removeMultiViewChannel(sourceId: Long, channelId: Long) {
        context.dataStore.edit { prefs ->
            val key = Keys.multiViewChannelsKey(sourceId)
            prefs[key] = prefs[key].parseIdCsv().filterNot { it == channelId }.joinToString(",")
        }
    }
}

private fun String?.parseIdCsv(): List<Long> =
    this?.split(',')?.mapNotNull(String::toLongOrNull) ?: emptyList()
