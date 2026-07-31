package com.arashrahimi46.iptv.data.repository

import com.arashrahimi46.iptv.data.db.ChannelTvgId
import com.arashrahimi46.iptv.data.parser.XmlTvParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * End-to-end demonstration that the EPG pipeline actually produces programmes: a realistic
 * XMLTV document is run through the REAL [XmlTvParser] and the REAL [matchXmlTvProgrammes]
 * matcher that [EpgRepository.refresh] uses to populate the Guide -- no mocks, no Room.
 *
 * It also pins the fix for "some channels have EPG but I can't see them": the M3U `tvg-id`
 * and the XMLTV `channel` id here differ only by case/whitespace ("BBCOne.uk" vs "bbcone.uk ").
 * The old exact-string match dropped every such channel; the normalized matcher keeps them.
 */
@RunWith(RobolectricTestRunner::class)
class EpgMatchTest {

    private val xmltv = """
        <?xml version="1.0" encoding="UTF-8"?>
        <tv>
          <programme channel="bbcone.uk " start="20260721180000 +0000" stop="20260721190000 +0000">
            <title>Six O'Clock News</title>
            <desc>The day's headlines.</desc>
          </programme>
          <programme channel="BBCONE.UK" start="20260721190000 +0000" stop="20260721200000 +0000">
            <title>Nature Documentary</title>
          </programme>
          <programme channel="itv1.uk" start="20260721180000 +0000" stop="20260721193000 +0000">
            <title>Evening Drama</title>
          </programme>
          <programme channel="ghost.channel" start="20260721180000 +0000" stop="20260721190000 +0000">
            <title>Should Not Appear</title>
          </programme>
        </tv>
    """.trimIndent()

    // Note the casing differs from the XMLTV ids on purpose -- this is the real-world mismatch.
    private val channels = listOf(
        ChannelTvgId(id = 1, tvgId = "BBCOne.uk"),
        ChannelTvgId(id = 2, tvgId = "ITV1.uk"),
        ChannelTvgId(id = 3, tvgId = "unmatched.id"),
    )

    @Test
    fun `real XMLTV parses and matches to channels despite id case-slash-whitespace differences`() {
        val programmes = XmlTvParser.parse(xmltv)
        assertEquals("parser reads all four <programme> entries", 4, programmes.size)

        val rows = matchXmlTvProgrammes(channels, programmes)

        // BBC One (2 programmes) + ITV 1 (1) = 3 matched; the "ghost.channel" programme and the
        // channel with no matching XMLTV id both drop out.
        assertEquals(3, rows.size)
        assertEquals(listOf(1L, 1L, 2L), rows.map { it.channelId }.sorted())
        assertTrue(rows.any { it.title == "Six O'Clock News" && it.channelId == 1L })
        assertTrue(rows.any { it.title == "Nature Documentary" && it.channelId == 1L })
        assertTrue(rows.any { it.title == "Evening Drama" && it.channelId == 2L })
        assertTrue("unmatched programme must not leak in", rows.none { it.title == "Should Not Appear" })
    }

    @Test
    fun `an exact-string match would have found nothing here -- proving why EPG was invisible`() {
        val programmes = XmlTvParser.parse(xmltv)
        // Reproduce the OLD behaviour: exact associateBy on raw tvgId.
        val byExact = channels.associateBy { it.tvgId }
        val exactMatches = programmes.count { byExact[it.channelRef] != null }
        assertEquals("case/whitespace mismatch -> old code matched zero", 0, exactMatches)

        // The shipped matcher recovers them.
        assertTrue(matchXmlTvProgrammes(channels, programmes).isNotEmpty())
    }
}
