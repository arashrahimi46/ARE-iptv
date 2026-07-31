package com.arashrahimi46.iptv.data.repository

import com.arashrahimi46.iptv.data.model.ContentType
import org.junit.Assert.assertEquals
import org.junit.Test

class M3uGroupClassifierTest {

    @Test
    fun `groups with no VOD-ish keyword classify as live`() {
        assertEquals(ContentType.LIVE, classifyM3uGroup("Sports"))
        assertEquals(ContentType.LIVE, classifyM3uGroup("News UK"))
        assertEquals(ContentType.LIVE, classifyM3uGroup(""))
    }

    @Test
    fun `groups clearly labeled as VOD classify as movie or series`() {
        assertEquals(ContentType.MOVIE, classifyM3uGroup("Movies"))
        assertEquals(ContentType.MOVIE, classifyM3uGroup("VOD - Action"))
        assertEquals(ContentType.SERIES, classifyM3uGroup("TV Series"))
        assertEquals(ContentType.SERIES, classifyM3uGroup("Shows"))
    }

    @Test
    fun `case insensitive`() {
        assertEquals(ContentType.MOVIE, classifyM3uGroup("MOVIES HD"))
        assertEquals(ContentType.SERIES, classifyM3uGroup("shOwS"))
    }

    /**
     * Documents the known real-world false positive qa-reviewer found in Phase 1 review:
     * a live/linear channel bouquet whose name happens to contain "movies"/"shows" gets
     * misclassified. This is an accepted M3U-only tradeoff (see classifyM3uGroup's doc
     * comment), NOT a regression -- this test pins the current (imperfect) behavior so a
     * future fix is a deliberate, visible change to this test, not a silent behavior shift.
     */
    @Test
    fun `known false positive - linear channel bouquets with VOD-ish names`() {
        assertEquals(ContentType.MOVIE, classifyM3uGroup("USA Movies HD"))
        assertEquals(ContentType.SERIES, classifyM3uGroup("Kids Shows"))
    }
}
