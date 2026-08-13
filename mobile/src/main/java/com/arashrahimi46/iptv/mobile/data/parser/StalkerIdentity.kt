package com.arashrahimi46.iptv.mobile.data.parser

import java.security.MessageDigest

/**
 * The device-identity fields a Stalker/Ministra portal derives from a set-top box's MAC address.
 *
 * Unlike Xtream (where a username/password is the whole identity), a Stalker portal identifies a
 * "box" by its MAC and a family of values a real MAG box computes from it: a serial number and a
 * couple of device ids/signature. Many portals accept a MAC alone, but stricter ones cross-check
 * these during `get_profile`, so we send them.
 *
 * IMPORTANT -- these derivation formulas are the *common community convention* used by MAG-box
 * emulators, not a spec Ministra publishes. Portals vary, so this is the explicit validation target
 * for the "real portal" hardening phase (see `docs/stalker-portal-v1-design.md` §10/Phase 5). The
 * whole derivation is deliberately isolated in [fromMac] so that phase can adjust one function
 * without touching the client. The auth flow must not hard-fail when a portal ignores these — the
 * MAC-only path still has to work.
 */
data class StalkerIdentity(
    /** Normalized (uppercased) MAC, colon-separated, e.g. `00:1A:79:AB:CD:EF`. */
    val mac: String,
    /** Serial number — first 13 chars of the uppercased MD5 of the MAC. */
    val serialNumber: String,
    val deviceId: String,
    val deviceId2: String,
    val signature: String,
) {
    companion object {
        fun fromMac(rawMac: String): StalkerIdentity {
            val mac = rawMac.trim().uppercase()
            val sn = md5Hex(mac).uppercase().take(13)
            val deviceId = sha256Hex(mac)
            return StalkerIdentity(
                mac = mac,
                serialNumber = sn,
                deviceId = deviceId,
                deviceId2 = deviceId,
                signature = sha256Hex(sn + mac),
            )
        }

        private fun md5Hex(input: String): String = hex("MD5", input)
        private fun sha256Hex(input: String): String = hex("SHA-256", input).uppercase()

        private fun hex(algorithm: String, input: String): String =
            MessageDigest.getInstance(algorithm)
                .digest(input.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
    }
}
