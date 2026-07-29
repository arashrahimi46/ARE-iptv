package com.arashrahimi46.iptv.data.player

import com.arashrahimi46.iptv.data.parser.XtreamClient
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Catch-up URL construction (docs/catchup-v1-design.md, Phase 1 — Xtream). Pure string/timezone
 * building, so no Room/credentials harness — the same "don't probe providers" rule as the other
 * parser tests. The timezone formatting is the #1 documented correctness trap, so it's covered
 * explicitly for UTC (default) and a real offset.
 */
class CatchupUrlTest {

    // 1_700_000_000_000 ms == Tue 14 Nov 2023 22:13:20 UTC (a well-known epoch).
    private val startMs = 1_700_000_000_000L

    @Test
    fun `timeshift url mirrors the live builder with duration and start`() {
        val client = XtreamClient("http://portal.example:8080/", "joe", "secret")
        assertEquals(
            "http://portal.example:8080/timeshift/joe/secret/90/2023-11-14:22-13/511.m3u8",
            client.timeshiftUrl(streamId = "511", durationMin = 90, startProviderLocal = "2023-11-14:22-13"),
        )
    }

    @Test
    fun `start formats in UTC when the provider timezone is unknown`() {
        assertEquals("2023-11-14:22-13", xtreamCatchupStart(startMs, null))
        assertEquals("2023-11-14:22-13", xtreamCatchupStart(startMs, ""))
    }

    @Test
    fun `start honours the provider timezone`() {
        // America/New_York is UTC-5 (EST) in mid-November: 22:13 UTC -> 17:13 local.
        assertEquals("2023-11-14:17-13", xtreamCatchupStart(startMs, "America/New_York"))
    }

    @Test
    fun `ext is reused from the live url, else defaults to m3u8`() {
        assertEquals("m3u8", xtreamCatchupExt("http://p:8080/live/u/p/511.m3u8"))
        assertEquals("ts", xtreamCatchupExt("http://p:8080/live/u/p/511.ts"))
        assertEquals("m3u8", xtreamCatchupExt(null))
        assertEquals("m3u8", xtreamCatchupExt("http://p:8080/live/u/p/511")) // no extension -> default
    }
}
