package com.arashrahimi46.iptv.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "are_iptv_settings")

/**
 * Preferences DataStore for app-wide settings that outlive a single screen.
 * Not fully wired to UI yet (Settings screen is Phase 4) -- exists now so
 * Onboarding/Home can read/write "is there an active playlist" state.
 */
class UserSettings(private val context: Context) {
    private object Keys {
        val ACTIVE_SOURCE_ID = longPreferencesKey("active_source_id")
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val REDUCED_MOTION = booleanPreferencesKey("reduced_motion")
    }

    val activeSourceId: Flow<Long?> = context.dataStore.data.map { prefs ->
        prefs[Keys.ACTIVE_SOURCE_ID]?.takeIf { it > 0 }
    }

    val isDarkTheme: Flow<Boolean> = context.dataStore.data.map { it[Keys.DARK_THEME] ?: true }

    val isReducedMotion: Flow<Boolean> = context.dataStore.data.map { it[Keys.REDUCED_MOTION] ?: false }

    suspend fun setActiveSourceId(id: Long) {
        context.dataStore.edit { it[Keys.ACTIVE_SOURCE_ID] = id }
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DARK_THEME] = enabled }
    }

    suspend fun setReducedMotion(enabled: Boolean) {
        context.dataStore.edit { it[Keys.REDUCED_MOTION] = enabled }
    }

}
