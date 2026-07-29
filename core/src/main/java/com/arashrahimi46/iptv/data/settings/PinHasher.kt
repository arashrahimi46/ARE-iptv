package com.arashrahimi46.iptv.data.settings

import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Salted PBKDF2 (HMAC-SHA256) hashing for the local parental-lock PIN. A 4-digit PIN only has
 * 10,000 possible values, so a single-round hash (the original SHA-256 implementation here) is
 * crackable for every possible PIN in well under a second if local storage is ever compromised --
 * a real iteration count is what actually makes brute-forcing the whole keyspace expensive.
 * Still device-local, non-networked scope -- NOT full credential hardening. The broader
 * plaintext-credentials issue (Xtream username/password, stored plaintext in Room per
 * [com.arashrahimi46.iptv.data.model.PlaylistSource]'s doc comment) is a separate,
 * already-tracked concern and out of scope here.
 */
object PinHasher {
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS = 256

    /** Generates a new random salt (hex-encoded) for a freshly-set PIN. */
    fun newSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /** Hashes [pin] with [salt] -- deterministic, so it can be compared against a stored hash. */
    fun hash(pin: String, salt: String): String {
        val factory = SecretKeyFactory.getInstance(ALGORITHM)
        val spec = PBEKeySpec(pin.toCharArray(), salt.toByteArray(Charsets.UTF_8), ITERATIONS, KEY_LENGTH_BITS)
        val bytes = factory.generateSecret(spec).encoded
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /** True if [pin] hashes (with [salt]) to [expectedHash]. */
    fun verify(pin: String, salt: String, expectedHash: String): Boolean = hash(pin, salt) == expectedHash
}
