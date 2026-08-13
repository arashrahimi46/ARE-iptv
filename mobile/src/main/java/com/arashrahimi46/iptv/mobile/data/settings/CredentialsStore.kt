package com.arashrahimi46.iptv.mobile.data.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Encrypted-at-rest storage for [com.arashrahimi46.iptv.mobile.data.model.PlaylistSource] Xtream
 * credentials (username/password), keyed by the source's Room row id.
 *
 * Replaces the plaintext `username`/`password` columns that used to live directly on the
 * [com.arashrahimi46.iptv.mobile.data.model.PlaylistSource] Room entity (tracked since Phase 1 as
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
class CredentialsStore(private val context: Context) {
    /**
     * PERF: `by lazy`, NOT eager. Building this does an AndroidKeyStore round trip
     * ([MasterKey.Builder.build] generates an AES-256-GCM key via Keymaster on first run),
     * statically initialises Tink, and then reads a file -- and [CredentialsStore] is
     * constructed by [com.arashrahimi46.iptv.mobile.data.repository.PlaylistRepositoryImpl]'s
     * constructor, which used to run on the composition thread before the first frame. Nothing
     * on the cold-start path reads a credential; only playing/refreshing an Xtream source does.
     */
    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "are_iptv_mobile_credentials",
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

    /**
     * Same as [save] but blocks until the write is durable on disk ([SharedPreferences.Editor.commit])
     * instead of scheduling it asynchronously ([SharedPreferences.Editor.apply]). Used by
     * [com.arashrahimi46.iptv.mobile.data.db.AppDatabase.migration1To2], which drops the plaintext
     * `playlist_sources` columns immediately after this call -- with `apply()`, a process kill in
     * that window could lose the encrypted write while the plaintext source is already gone,
     * destroying the credential irrecoverably.
     */
    fun saveSync(sourceId: Long, username: String, password: String) {
        prefs.edit()
            .putString(usernameKey(sourceId), username)
            .putString(passwordKey(sourceId), password)
            .commit()
    }

    /**
     * A Stalker Portal source authenticates by MAC address, not username/password. The MAC grants
     * portal access (it is the identity a portal binds the subscription to), so it is a secret and
     * lives here encrypted rather than in the plaintext `playlist_sources.url`/columns — same
     * reasoning as the Xtream credentials above.
     */
    fun saveStalker(sourceId: Long, mac: String) {
        prefs.edit().putString(macKey(sourceId), mac).apply()
    }

    fun username(sourceId: Long): String? = prefs.getString(usernameKey(sourceId), null)

    fun password(sourceId: Long): String? = prefs.getString(passwordKey(sourceId), null)

    fun mac(sourceId: Long): String? = prefs.getString(macKey(sourceId), null)

    /** Both stored credentials for [sourceId] as `(username, secret)` -- either half is null when absent. */
    fun forSource(sourceId: Long): Pair<String?, String?> =
        prefs.getString(usernameKey(sourceId), null) to prefs.getString(passwordKey(sourceId), null)

    fun clear(sourceId: Long) {
        prefs.edit()
            .remove(usernameKey(sourceId))
            .remove(passwordKey(sourceId))
            .remove(macKey(sourceId))
            .apply()
    }

    private fun usernameKey(sourceId: Long) = "username_$sourceId"
    private fun passwordKey(sourceId: Long) = "password_$sourceId"
    private fun macKey(sourceId: Long) = "mac_$sourceId"
}
