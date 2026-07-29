package com.arashrahimi46.iptv.data.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the pure, deterministic surface of [StalkerIdentity]. These formulas are the community
 * MAG-box convention (not a published spec) and are the validation target for the real-portal
 * hardening phase; what we pin here is that the derivation is stable, well-formed, and MAC-sensitive
 * so a later tweak to the formulas is a deliberate, visible change rather than a silent drift.
 */
class StalkerIdentityTest {

    private val mac = "00:1A:79:AB:CD:EF"

    @Test
    fun `derivation is deterministic for the same MAC`() {
        assertEquals(StalkerIdentity.fromMac(mac), StalkerIdentity.fromMac(mac))
    }

    @Test
    fun `MAC is normalized to uppercase and whitespace-trimmed`() {
        val id = StalkerIdentity.fromMac("  00:1a:79:ab:cd:ef  ")
        assertEquals("00:1A:79:AB:CD:EF", id.mac)
        assertEquals(StalkerIdentity.fromMac(mac), id)
    }

    @Test
    fun `serial number is 13 uppercase hex chars`() {
        val sn = StalkerIdentity.fromMac(mac).serialNumber
        assertEquals(13, sn.length)
        assertTrue("SN was '$sn'", sn.matches(Regex("[0-9A-F]{13}")))
    }

    @Test
    fun `device id and signature are 64-char uppercase SHA-256 hex`() {
        val id = StalkerIdentity.fromMac(mac)
        for (value in listOf(id.deviceId, id.deviceId2, id.signature)) {
            assertTrue("value was '$value'", value.matches(Regex("[0-9A-F]{64}")))
        }
    }

    @Test
    fun `different MACs derive different identities`() {
        assertNotEquals(
            StalkerIdentity.fromMac("00:1A:79:00:00:01"),
            StalkerIdentity.fromMac("00:1A:79:00:00:02"),
        )
    }
}
