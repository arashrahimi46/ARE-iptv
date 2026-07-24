package com.arashrahimi46.iptv.data.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers [expandCatchup] -- the M3U `catchup-source` expander (docs/catchup-v1-design.md, Phase 4).
 * Anchor time: startMs = 1_700_000_000_000 = 2023-11-14 22:13:20 UTC; endMs = +30 min; now = +2h.
 */
class CatchupTemplateTest {

    private val start = 1_700_000_000_000L          // 2023-11-14 22:13:20 UTC
    private val end = start + 30 * 60_000L           // +30 min
    private val now = start + 2 * 3_600_000L         // +2h
    private val startSec = start / 1000              // 1700000000
    private val nowSec = now / 1000

    @Test
    fun `default type with no source appends utc and lutc`() {
        val out = expandCatchup("http://h/live/ch1.m3u8", source = null, type = "default", startMs = start, endMs = end, nowMs = now)
        assertEquals("http://h/live/ch1.m3u8?utc=$startSec&lutc=$nowSec", out)
    }

    @Test
    fun `default with no source respects an existing query separator`() {
        val out = expandCatchup("http://h/live?id=5", source = null, type = null, startMs = start, endMs = end, nowMs = now)
        assertEquals("http://h/live?id=5&utc=$startSec&lutc=$nowSec", out)
    }

    @Test
    fun `default with an absolute-URL template replaces the live URL`() {
        val out = expandCatchup(
            "http://h/live/ch1.m3u8",
            source = "http://h/archive/ch1?utc={utc}&dur={duration}",
            type = "default", startMs = start, endMs = end, nowMs = now,
        )
        assertEquals("http://h/archive/ch1?utc=$startSec&dur=1800", out)
    }

    @Test
    fun `append concatenates the expanded template onto the live URL`() {
        val out = expandCatchup(
            "http://h/live/ch1.m3u8",
            source = "?archive={utc}&end={utcend}",
            type = "append", startMs = start, endMs = end, nowMs = now,
        )
        assertEquals("http://h/live/ch1.m3u8?archive=$startSec&end=${end / 1000}", out)
    }

    @Test
    fun `flussonic rewrites the last hls segment to an archive segment`() {
        val out = expandCatchup("http://h/ch1/index.m3u8", source = null, type = "flussonic", startMs = start, endMs = end, nowMs = now)
        assertEquals("http://h/ch1/archive-$startSec-1800.m3u8", out)
    }

    @Test
    fun `flussonic ts live maps to timeshift_abs`() {
        val out = expandCatchup("http://h/ch1/mpegts?token=x", source = null, type = "flussonic", startMs = start, endMs = end, nowMs = now)
        assertEquals("http://h/ch1/timeshift_abs-$startSec.ts?token=x", out)
    }

    @Test
    fun `xc builds the xtream timeshift path preserving user pass id and ext`() {
        val out = expandCatchup("http://h:80/live/user/pass/123.ts", source = null, type = "xc", startMs = start, endMs = end, nowMs = now)
        // 30-min duration -> 30; start formatted in UTC as yyyy-MM-dd:HH-mm.
        assertEquals("http://h:80/timeshift/user/pass/30/2023-11-14:22-13/123.ts", out)
    }

    @Test
    fun `xc handles a feed without the live path segment`() {
        val out = expandCatchup("http://h:80/user/pass/123.m3u8", source = null, type = "xc", startMs = start, endMs = end, nowMs = now)
        assertEquals("http://h:80/timeshift/user/pass/30/2023-11-14:22-13/123.m3u8", out)
    }

    @Test
    fun `padded date components expand for a single-digit month`() {
        // 2021-03-05 04:07:09 UTC -> Y=2021 m=03 d=05 H=04 M=07.
        val s = 1_614_917_229_000L
        val out = expandCatchup("http://h/x", source = "?t={Y}{m}{d}-{H}{M}", type = "default", startMs = s, endMs = s + 60_000L, nowMs = s)
        assertEquals("http://h/x?t=20210305-0407", out)
    }

    @Test
    fun `duration is at least one second even for a zero-length window`() {
        val out = expandCatchup("http://h/x", source = "?d={duration}", type = "append", startMs = start, endMs = start, nowMs = now)
        assertTrue(out.endsWith("d=1"))
    }
}
