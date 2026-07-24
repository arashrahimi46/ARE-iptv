package com.arashrahimi46.iptv.data.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the pure Stalker catch-up helpers (docs/catchup-v1-design.md, Phase 5). The portal archive
 * protocol is the least-standardized of the three sources and can't be probed from the shell, so the
 * archive-window derivation and the `&start=` param shape are pinned here — a later change on a real
 * portal is then a deliberate, visible edit rather than a silent drift.
 */
class StalkerCatchupTest {

    // --- archive window (days) ---

    @Test
    fun `explicit tv_archive_duration in days wins`() {
        assertEquals(5, stalkerArchiveDays(archiveFlag = "1", archiveRangeHours = "48", archiveDurationDays = "5"))
    }

    @Test
    fun `archive_range hours are rounded up to whole days`() {
        // 48h -> 2 days; 49h -> 3 days (ceil).
        assertEquals(2, stalkerArchiveDays(archiveFlag = "1", archiveRangeHours = "48", archiveDurationDays = null))
        assertEquals(3, stalkerArchiveDays(archiveFlag = "1", archiveRangeHours = "49", archiveDurationDays = null))
    }

    @Test
    fun `a flagged channel with no range defaults to 7 days`() {
        assertEquals(7, stalkerArchiveDays(archiveFlag = "1", archiveRangeHours = null, archiveDurationDays = null))
    }

    @Test
    fun `no archive flag means no window`() {
        assertEquals(0, stalkerArchiveDays(archiveFlag = "0", archiveRangeHours = "48", archiveDurationDays = null))
        assertEquals(0, stalkerArchiveDays(archiveFlag = null, archiveRangeHours = null, archiveDurationDays = null))
    }

    // --- create_link extra (the archive &start= param) ---

    @Test
    fun `an archive start appends the start param`() {
        val extra = stalkerCreateLinkExtra(cmd = "ffmpeg http://portal/ch/1", series = null, startEpochSec = 1_700_000_000L)
        assertTrue(extra.contains("&start=1700000000"))
        assertTrue(extra.contains("&cmd="))
    }

    @Test
    fun `a live link carries no start param`() {
        val extra = stalkerCreateLinkExtra(cmd = "ffmpeg http://portal/ch/1", series = null, startEpochSec = null)
        assertFalse(extra.contains("&start="))
    }

    @Test
    fun `the cmd is url-encoded`() {
        val extra = stalkerCreateLinkExtra(cmd = "ffmpeg http://portal/ch/1", series = 3, startEpochSec = null)
        assertTrue(extra.contains("&cmd=ffmpeg+http"))
        assertTrue(extra.contains("&series=3"))
    }
}
