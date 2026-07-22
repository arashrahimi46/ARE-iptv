package com.arashrahimi46.iptv.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "are_iptv_settings")

/** Which player handles playback -- persisted only, see [UserSettings.externalPlayer] doc. */
enum class ExternalPlayerChoice { BUILT_IN, VLC, MX }

/**
 * Preferences DataStore for app-wide settings that outlive a single screen.
 * Fully wired to the real Settings screen (Phase 4) -- see
 * [com.arashrahimi46.iptv.ui.settings.SettingsScreen].
 */
class UserSettings(private val context: Context) {
    private object Keys {
        val ACTIVE_SOURCE_ID = longPreferencesKey("active_source_id")
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val REDUCED_MOTION = booleanPreferencesKey("reduced_motion")
        val HARDWARE_DECODING = booleanPreferencesKey("hardware_decoding")
        val AUTOPLAY_NEXT_EPISODE = booleanPreferencesKey("autoplay_next_episode")
        val PICTURE_IN_PICTURE = booleanPreferencesKey("picture_in_picture")
        val EXTERNAL_PLAYER = stringPreferencesKey("external_player")
        val PARENTAL_LOCK_ENABLED = booleanPreferencesKey("parental_lock_enabled")
        val PARENTAL_PIN_HASH = stringPreferencesKey("parental_pin_hash")
        val PARENTAL_PIN_SALT = stringPreferencesKey("parental_pin_salt")
        val GUIDE_SELECTED_CATEGORY = stringPreferencesKey("guide_selected_category")
        val BROWSE_LIST_MODE = booleanPreferencesKey("browse_list_mode")
        val TERMS_ACCEPTED = booleanPreferencesKey("terms_accepted")
        /** Pinned category names, namespaced per browse screen (see [pinnedCategoriesKey]). */
        fun pinnedCategoriesKey(namespace: String) = stringSetPreferencesKey("pinned_categories_$namespace")
    }

    val activeSourceId: Flow<Long?> = context.dataStore.data.map { prefs ->
        prefs[Keys.ACTIVE_SOURCE_ID]?.takeIf { it > 0 }
    }

    val isDarkTheme: Flow<Boolean> = context.dataStore.data.map { it[Keys.DARK_THEME] ?: true }

    val isReducedMotion: Flow<Boolean> = context.dataStore.data.map { it[Keys.REDUCED_MOTION] ?: false }

    /** ExoPlayer decoder-fallback preference -- see [com.arashrahimi46.iptv.ui.player.LivePlayerScreen]'s `ExoPlayer.Builder`. */
    val isHardwareDecoding: Flow<Boolean> = context.dataStore.data.map { it[Keys.HARDWARE_DECODING] ?: true }

    /**
     * Preference only in this phase -- persisted for real, but no auto-advance-on-completion
     * listener is wired yet (documented follow-up; see report). [com.arashrahimi46.iptv.ui.detail.DetailScreen]'s
     * episode list already lets a user manually pick the next episode.
     */
    val isAutoplayNextEpisode: Flow<Boolean> = context.dataStore.data.map { it[Keys.AUTOPLAY_NEXT_EPISODE] ?: true }

    /**
     * Storage-only per explicit product-lead scoping -- no enter-PiP-mode implementation is
     * wired to this flag. See [com.arashrahimi46.iptv.ui.player.LivePlayerScreen]'s doc comment
     * on why PiP needs Phase 2's device verification to land first.
     */
    val isPictureInPicture: Flow<Boolean> = context.dataStore.data.map { it[Keys.PICTURE_IN_PICTURE] ?: false }

    /** Persisted choice only -- doesn't launch an external player intent yet. */
    val externalPlayer: Flow<ExternalPlayerChoice> = context.dataStore.data.map { prefs ->
        prefs[Keys.EXTERNAL_PLAYER]?.let { runCatching { ExternalPlayerChoice.valueOf(it) }.getOrNull() } ?: ExternalPlayerChoice.BUILT_IN
    }

    val isParentalLockEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.PARENTAL_LOCK_ENABLED] ?: false }

    /** Salted SHA-256 hash -- never the raw PIN. Null when no PIN has been set yet. */
    val parentalPinHash: Flow<String?> = context.dataStore.data.map { it[Keys.PARENTAL_PIN_HASH] }
    val parentalPinSalt: Flow<String?> = context.dataStore.data.map { it[Keys.PARENTAL_PIN_SALT] }

    /** Last channel-group filter picked on the Guide screen (see [com.arashrahimi46.iptv.ui.guide.GuideViewModel]); "All" when unset. */
    val guideSelectedCategory: Flow<String> = context.dataStore.data.map { it[Keys.GUIDE_SELECTED_CATEGORY] ?: "All" }

    /** First-run Privacy & Terms acceptance (Issue #11) -- false until the user explicitly accepts once. */
    val hasAcceptedTerms: Flow<Boolean> = context.dataStore.data.map { it[Keys.TERMS_ACCEPTED] ?: false }

    /** True renders Live TV/Movies/Series as a list instead of the default tile grid (see [com.arashrahimi46.iptv.ui.browse.BrowseLayout]). */
    val isBrowseListMode: Flow<Boolean> = context.dataStore.data.map { it[Keys.BROWSE_LIST_MODE] ?: false }

    suspend fun setActiveSourceId(id: Long) {
        context.dataStore.edit { it[Keys.ACTIVE_SOURCE_ID] = id }
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DARK_THEME] = enabled }
    }

    suspend fun setReducedMotion(enabled: Boolean) {
        context.dataStore.edit { it[Keys.REDUCED_MOTION] = enabled }
    }

    suspend fun setHardwareDecoding(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HARDWARE_DECODING] = enabled }
    }

    suspend fun setAutoplayNextEpisode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTOPLAY_NEXT_EPISODE] = enabled }
    }

    suspend fun setPictureInPicture(enabled: Boolean) {
        context.dataStore.edit { it[Keys.PICTURE_IN_PICTURE] = enabled }
    }

    suspend fun setExternalPlayer(choice: ExternalPlayerChoice) {
        context.dataStore.edit { it[Keys.EXTERNAL_PLAYER] = choice.name }
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

    suspend fun setTermsAccepted(accepted: Boolean) {
        context.dataStore.edit { it[Keys.TERMS_ACCEPTED] = accepted }
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
}
