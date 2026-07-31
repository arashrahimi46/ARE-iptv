package com.arashrahimi46.iptv.data.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Round-trips the display-only [StalkerAccountInfo] blob (the JSON we persist on the source row and
 * rehydrate for the Settings panel), including the null-field and garbage-input paths.
 *
 * Robolectric-backed because [StalkerAccountInfo] uses `org.json`, which is an unmocked stub on a
 * plain JVM unit test (same reason as [XmlTvParserTest]).
 */
@RunWith(RobolectricTestRunner::class)
class StalkerAccountInfoTest {

    @Test
    fun `toJson then fromJson preserves all fields`() {
        val info = StalkerAccountInfo(
            mac = "00:1A:79:AB:CD:EF",
            status = "Active",
            expiresRaw = "October 21, 2025",
            phone = "12345",
            host = "http://portal.example.com",
        )
        assertEquals(info, StalkerAccountInfo.fromJson(info.toJson()))
    }

    @Test
    fun `null fields survive the round-trip as null`() {
        val info = StalkerAccountInfo(mac = "00:1A:79:AB:CD:EF")
        val restored = StalkerAccountInfo.fromJson(info.toJson())
        assertEquals("00:1A:79:AB:CD:EF", restored?.mac)
        assertNull(restored?.expiresRaw)
        assertNull(restored?.status)
    }

    @Test
    fun `blank or malformed input yields null`() {
        assertNull(StalkerAccountInfo.fromJson(null))
        assertNull(StalkerAccountInfo.fromJson(""))
        assertNull(StalkerAccountInfo.fromJson("not json"))
    }
}
