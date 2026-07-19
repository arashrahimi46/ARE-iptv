package com.arashrahimi46.iptv.data.settings

import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Salted SHA-256 hashing for the local parental-lock PIN (Phase 4). Sufficient
 * for a device-local, non-networked 4-digit gate -- NOT full credential
 * hardening. The broader plaintext-credentials issue (Xtream username/password,
 * stored plaintext in Room per [com.arashrahimi46.iptv.data.model.PlaylistSource]'s
 * doc comment) is a separate, already-tracked concern and out of scope here.
 */
object PinHasher {
    private const val ALGORITHM = "SHA-256"

    /** Generates a new random salt (hex-encoded) for a freshly-set PIN. */
    fun newSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /** Hashes [pin] with [salt] -- deterministic, so it can be compared against a stored hash. */
    fun hash(pin: String, salt: String): String {
        val digest = MessageDigest.getInstance(ALGORITHM)
        digest.update(salt.toByteArray(Charsets.UTF_8))
        val bytes = digest.digest(pin.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /** True if [pin] hashes (with [salt]) to [expectedHash]. */
    fun verify(pin: String, salt: String, expectedHash: String): Boolean = hash(pin, salt) == expectedHash
}
