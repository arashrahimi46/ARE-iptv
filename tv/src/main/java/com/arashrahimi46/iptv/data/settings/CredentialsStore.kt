package com.arashrahimi46.iptv.data.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Encrypted-at-rest storage for [com.arashrahimi46.iptv.data.model.PlaylistSource] Xtream
 * credentials (username/password), keyed by the source's Room row id.
 *
 * Replaces the plaintext `username`/`password` columns that used to live directly on the
 * [com.arashrahimi46.iptv.data.model.PlaylistSource] Room entity (tracked since Phase 1 as
 * a known limitation, hardened here per product-lead's "Phase 4 or a dedicated pass before
 * RC" ruling). Backed by [EncryptedSharedPreferences] (AES256-GCM values, AES256-SIV keys,
 * keystore-backed master key) rather than a full SQLCipher-encrypted Room database -- lower
 * setup cost for the two fields that actually need protection, without re-encrypting the
 * rest of the catalog data (channel/VOD titles, categories, etc.) that isn't sensitive.
 *
 * M3U sources have no username/password (the playlist URL itself may embed credentials,
 * which is a URL-parsing/display concern, not something this store manages) -- callers
 * simply won't have anything to read/write for those source ids.
 */
class CredentialsStore(context: Context) {
    private val prefs: SharedPreferences = run {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "are_iptv_credentials",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun save(sourceId: Long, username: String, password: String) {
        prefs.edit()
            .putString(usernameKey(sourceId), username)
            .putString(passwordKey(sourceId), password)
            .apply()
    }

    fun username(sourceId: Long): String? = prefs.getString(usernameKey(sourceId), null)

    fun password(sourceId: Long): String? = prefs.getString(passwordKey(sourceId), null)

    fun clear(sourceId: Long) {
        prefs.edit()
            .remove(usernameKey(sourceId))
            .remove(passwordKey(sourceId))
            .apply()
    }

    private fun usernameKey(sourceId: Long) = "username_$sourceId"
    private fun passwordKey(sourceId: Long) = "password_$sourceId"
}
