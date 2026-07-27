package com.arashrahimi46.iptv.data.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class M3uParserTest {

    @Test
    fun `extractEpgUrl reads url-tvg from the header`() {
        assertEquals("http://x/epg.xml", M3uParser.extractEpgUrl("""#EXTM3U url-tvg="http://x/epg.xml""""))
    }

    @Test
    fun `extractEpgUrl accepts x-tvg-url and takes the first of a comma list`() {
        assertEquals("http://a/1.xml", M3uParser.extractEpgUrl("""#EXTM3U x-tvg-url="http://a/1.xml,http://b/2.xml""""))
    }

    @Test
    fun `extractEpgUrl returns null for a header without an EPG attribute or a non-header line`() {
        assertEquals(null, M3uParser.extractEpgUrl("#EXTM3U"))
        assertEquals(null, M3uParser.extractEpgUrl("http://example.com/stream/1"))
    }

    @Test
    fun `parses a well-formed entry with all attributes`() {
        val playlist = """
            #EXTM3U
            #EXTINF:-1 tvg-id="bbc1.uk" tvg-name="BBC One" tvg-logo="http://x/bbc.png" group-title="News",BBC One HD
            http://example.com/stream/1
        """.trimIndent()

        val entries = M3uParser.parse(playlist)

        assertEquals(1, entries.size)
        val entry = entries[0]
        assertEquals("BBC One", entry.name) // tvg-name wins over the display name after the comma
        assertEquals("http://example.com/stream/1", entry.streamUrl)
        assertEquals("bbc1.uk", entry.tvgId)
        assertEquals("http://x/bbc.png", entry.logoUrl)
        assertEquals("News", entry.groupTitle)
    }

    @Test
    fun `parses catchup attributes with an explicit day count`() {
        val playlist = """
            #EXTM3U
            #EXTINF:-1 catchup="default" catchup-source="http://x/a?utc={utc}" catchup-days="5",Ch
            http://example.com/live/1.m3u8
        """.trimIndent()

        val entry = M3uParser.parse(playlist).single()
        assertEquals("default", entry.catchupType)
        assertEquals("http://x/a?utc={utc}", entry.catchupSource)
        assertEquals(5, entry.catchupDays)
    }

    @Test
    fun `catchup-type is read and tvg-rec supplies the window when catchup-days is absent`() {
        val playlist = """
            #EXTM3U
            #EXTINF:-1 catchup-type="flussonic" tvg-rec="3",Ch
            http://example.com/ch/index.m3u8
        """.trimIndent()

        val entry = M3uParser.parse(playlist).single()
        assertEquals("flussonic", entry.catchupType)
        assertEquals(3, entry.catchupDays)
    }

    @Test
    fun `a declared catchup with no day count defaults to a 7-day window`() {
        val playlist = """
            #EXTM3U
            #EXTINF:-1 catchup="append" catchup-source="?a={utc}",Ch
            http://example.com/live/2.ts
        """.trimIndent()

        assertEquals(7, M3uParser.parse(playlist).single().catchupDays)
    }

    @Test
    fun `a channel with no catchup attributes has a zero window and null template`() {
        val playlist = """
            #EXTM3U
            #EXTINF:-1 tvg-id="x",Ch
            http://example.com/live/3.ts
        """.trimIndent()

        val entry = M3uParser.parse(playlist).single()
        assertEquals(0, entry.catchupDays)
        assertEquals(null, entry.catchupType)
        assertEquals(null, entry.catchupSource)
    }

    @Test
    fun `falls back to the display name when tvg-name is absent`() {
        val playlist = """
            #EXTM3U
            #EXTINF:-1 group-title="Sports",Sky Sports Main Event
            http://example.com/stream/2
        """.trimIndent()

        val entries = M3uParser.parse(playlist)

        assertEquals(1, entries.size)
        assertEquals("Sky Sports Main Event", entries[0].name)
    }

    @Test
    fun `an EXTINF immediately followed by another EXTINF (no URL line between) still finds the next real URL line`() {
        // Documents actual current behavior rather than an assumption: the URL search
        // treats "#"-prefixed lines (including a second EXTINF) as non-URL lines to skip
        // over, so the FIRST EXTINF claims the next real URL line and the second EXTINF's
        // own metadata is effectively dropped (its name never gets attached to a URL).
        // This is a known tolerance-vs-correctness tradeoff in the current tolerant parser,
        // not something this pass is fixing -- pinning it here so a future change to this
        // behavior is a deliberate, visible diff to this test.
        val playlist = """
            #EXTM3U
            #EXTINF:-1,First Channel
            #EXTINF:-1,Second Channel
            http://example.com/stream/only-one
        """.trimIndent()

        val entries = M3uParser.parse(playlist)

        assertEquals(1, entries.size)
        assertEquals("First Channel", entries[0].name)
        assertEquals("http://example.com/stream/only-one", entries[0].streamUrl)
    }

    @Test
    fun `a genuinely trailing EXTINF with nothing after it is skipped entirely`() {
        val playlist = """
            #EXTM3U
            #EXTINF:-1,Real Channel
            http://example.com/stream/real
            #EXTINF:-1,Trailing Orphan
        """.trimIndent()

        val entries = M3uParser.parse(playlist)

        assertEquals(1, entries.size)
        assertEquals("Real Channel", entries[0].name)
    }

    @Test
    fun `tolerates a completely malformed line without aborting the whole parse`() {
        val playlist = """
            #EXTM3U
            this is not a valid line at all
            #EXTINF:-1,Good Channel
            http://example.com/stream/good
        """.trimIndent()

        val entries = M3uParser.parse(playlist)

        assertEquals(1, entries.size)
        assertEquals("Good Channel", entries[0].name)
    }

    @Test
    fun `empty playlist yields no entries`() {
        assertTrue(M3uParser.parse("").isEmpty())
        assertTrue(M3uParser.parse("#EXTM3U").isEmpty())
    }

    /**
     * Regression: the display-name split used the FIRST comma, but a User-Agent attribute contains
     * one inside its quotes -- "(KHTML, like Gecko)". Real line from the iptv-org Netherlands
     * playlist shipped in Explore, which named the channel
     * `like Gecko) Chrome/149.0.0.0 Safari/537.36" group-title="Kids",Disney Channel`
     * and dropped group-title along with it.
     */
    @Test
    fun `a comma inside a quoted attribute is not the name separator`() {
        val playlist = """
            #EXTM3U
            #EXTINF:-1 tvg-id="Disney.nl" user-agent="Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36" group-title="Kids",Disney Channel
            http://example.com/stream/disney
        """.trimIndent()

        val entries = M3uParser.parse(playlist)

        assertEquals(1, entries.size)
        assertEquals("Disney Channel", entries[0].name)
        assertEquals("Kids", entries[0].groupTitle)
        assertEquals("Disney.nl", entries[0].tvgId)
    }

    @Test
    fun `entry with no name at all falls back to Unnamed`() {
        val playlist = """
            #EXTM3U
            #EXTINF:-1,
            http://example.com/stream/noname
        """.trimIndent()

        val entries = M3uParser.parse(playlist)

        assertEquals(1, entries.size)
        assertEquals("Unnamed", entries[0].name)
    }

    /** Radio stations are declared at catalog time by `radio="true"` or `tvg-type="radio"` --
     * the tile has to badge them without playing anything first. */
    @Test
    fun `radio attributes flag a station as audio-only`() {
        val playlist = """
            #EXTM3U
            #EXTINF:-1 radio="true" group-title="Music",ZFM Zoetermeer
            http://example.com/stream/zfm
            #EXTINF:-1 tvg-type="radio",Sky Radio
            http://example.com/stream/sky
            #EXTINF:-1 group-title="News",BBC One
            http://example.com/stream/bbc
        """.trimIndent()

        val entries = M3uParser.parse(playlist)

        assertEquals(listOf(true, true, false), entries.map { it.isRadio })
    }
}
