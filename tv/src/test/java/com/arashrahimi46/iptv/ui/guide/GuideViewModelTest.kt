package com.arashrahimi46.iptv.ui.guide

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Covers the root-cause fix for issue #2 ("EPG list not scrollable / no channels visible"):
 * [GuideViewModel.observeRows] used to validate the persisted category filter only once,
 * before [com.arashrahimi46.iptv.data.settings.UserSettings.guideSelectedCategory] finished
 * restoring asynchronously -- a stale/mismatched category landing after that point silently
 * filtered out every channel, leaving the guide empty. [resolveGuideGroup] is the extracted
 * pure resolution now applied on every emission.
 */
class GuideViewModelTest {

    @Test
    fun `valid group passes through unchanged`() {
        val groups = listOf("News", "Sports")
        assertEquals("Sports", resolveGuideGroup("Sports", groups))
    }

    @Test
    fun `unknown group falls back to the first category`() {
        val groups = listOf("News", "Sports")
        // e.g. a category persisted from a previous playlist that doesn't exist on this one.
        assertEquals("News", resolveGuideGroup("Kids", groups))
    }

    @Test
    fun `legacy All default falls back to the first category`() {
        // "All" is no longer a real tab -- the Guide is strictly per-category now.
        assertEquals("News", resolveGuideGroup("All", listOf("News", "Sports")))
    }

    @Test
    fun `empty groups returns the raw group unchanged`() {
        assertEquals("Sports", resolveGuideGroup("Sports", emptyList()))
    }

    // --- Catch-up glyph eligibility (docs/catchup-v1-design.md, D6) ---

    private val now = 1_700_000_000_000L
    private val hour = 3_600_000L
    private val day = 86_400_000L

    @Test
    fun `no archive window is never eligible`() {
        assertEquals(false, isCatchupEligible(catchupDays = 0, programStartMs = now - 2 * hour, nowMs = now))
    }

    @Test
    fun `an aired programme inside the window is eligible`() {
        assertEquals(true, isCatchupEligible(catchupDays = 7, programStartMs = now - 2 * hour, nowMs = now))
    }

    @Test
    fun `the now-airing programme is eligible (start-over)`() {
        // Started 10 min ago, still on air: startMs < now -> eligible for Watch-from-start.
        assertEquals(true, isCatchupEligible(catchupDays = 7, programStartMs = now - 10 * 60_000L, nowMs = now))
    }

    @Test
    fun `a future programme is never eligible`() {
        assertEquals(false, isCatchupEligible(catchupDays = 7, programStartMs = now + hour, nowMs = now))
    }

    @Test
    fun `a programme older than the window is not eligible`() {
        // 8 days back with a 7-day window -> outside the archive, no glyph.
        assertEquals(false, isCatchupEligible(catchupDays = 7, programStartMs = now - 8 * day, nowMs = now))
        // exactly at the window edge is still in.
        assertEquals(true, isCatchupEligible(catchupDays = 7, programStartMs = now - 7 * day, nowMs = now))
    }
}
