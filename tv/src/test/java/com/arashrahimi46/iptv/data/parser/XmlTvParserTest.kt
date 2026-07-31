package com.arashrahimi46.iptv.data.parser

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Calendar
import java.util.TimeZone

// XmlTvParser.parse() goes through android.util.Xml.newPullParser(), which is unmocked (and
// throws) on a plain JVM unit test -- Robolectric provides a real XmlPullParser implementation
// for that call without needing an emulator/device.
@RunWith(RobolectricTestRunner::class)
class XmlTvParserTest {

    private lateinit var originalDefaultZone: TimeZone

    @Before
    fun setUp() {
        originalDefaultZone = TimeZone.getDefault()
        // A non-UTC device zone with a large, unambiguous offset -- if the parser ever
        // regresses to interpreting a no-offset XMLTV timestamp using the device's local
        // zone (instead of UTC per the XMLTV spec), this shifts startMs/stopMs by hours
        // and the assertion below fails.
        TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"))
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(originalDefaultZone)
    }

    @Test
    fun `a no-offset XMLTV timestamp parses to the correct UTC epoch millis regardless of device default timezone`() {
        val xml = """
            <tv>
                <programme channel="bbc1.uk" start="20240115193000" stop="20240115200000">
                    <title>News at Ten</title>
                </programme>
            </tv>
        """.trimIndent()

        val programmes = XmlTvParser.parse(xml)

        assertEquals(1, programmes.size)
        val programme = programmes[0]

        val expectedStart = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(2024, Calendar.JANUARY, 15, 19, 30, 0)
        }.timeInMillis
        val expectedStop = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(2024, Calendar.JANUARY, 15, 20, 0, 0)
        }.timeInMillis

        assertEquals(expectedStart, programme.startMs)
        assertEquals(expectedStop, programme.stopMs)
    }

    @Test
    fun `an explicit-offset XMLTV timestamp still honors its own offset regardless of device default timezone`() {
        val xml = """
            <tv>
                <programme channel="bbc1.uk" start="20240115193000 +0100" stop="20240115200000 +0100">
                    <title>News at Ten</title>
                </programme>
            </tv>
        """.trimIndent()

        val programmes = XmlTvParser.parse(xml)

        assertEquals(1, programmes.size)
        val programme = programmes[0]

        // 19:30 +0100 == 18:30 UTC
        val expectedStart = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(2024, Calendar.JANUARY, 15, 18, 30, 0)
        }.timeInMillis

        assertEquals(expectedStart, programme.startMs)
    }

    // ---------------------------------------------------------------------------------------
    // parseXmlTvTime: hand-rolled since it runs 10^5-10^6 times per EPG import, so its exact
    // semantics are pinned here rather than inherited from SimpleDateFormat's leniency.
    // ---------------------------------------------------------------------------------------

    private fun utcMillis(y: Int, mo: Int, d: Int, h: Int, mi: Int, s: Int): Long =
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(y, mo - 1, d, h, mi, s)
        }.timeInMillis

    @Test
    fun `a bare timestamp is read as UTC, not device local time`() {
        assertEquals(utcMillis(2024, 1, 15, 19, 30, 0), XmlTvParser.parseXmlTvTime("20240115193000"))
    }

    @Test
    fun `positive and negative offsets are subtracted to reach UTC`() {
        assertEquals(utcMillis(2024, 1, 15, 18, 30, 0), XmlTvParser.parseXmlTvTime("20240115193000 +0100"))
        assertEquals(utcMillis(2024, 1, 15, 23, 0, 0), XmlTvParser.parseXmlTvTime("20240115193000 -0330"))
        // No space before the offset is also seen in the wild.
        assertEquals(utcMillis(2024, 1, 15, 18, 30, 0), XmlTvParser.parseXmlTvTime("20240115193000+0100"))
    }

    @Test
    fun `leap days and century boundaries land on the right day`() {
        assertEquals(utcMillis(2024, 2, 29, 12, 0, 0), XmlTvParser.parseXmlTvTime("20240229120000"))
        // 2000 is a leap year, 1900 is not -- the case a naive /4 rule gets wrong.
        assertEquals(utcMillis(2000, 2, 29, 0, 0, 0), XmlTvParser.parseXmlTvTime("20000229000000"))
        assertEquals(0L, XmlTvParser.parseXmlTvTime("19700101000000"))
    }

    @Test
    fun `malformed timestamps return null instead of a wrong instant`() {
        assertEquals(null, XmlTvParser.parseXmlTvTime(null))
        assertEquals(null, XmlTvParser.parseXmlTvTime(""))
        assertEquals(null, XmlTvParser.parseXmlTvTime("2024011519300"))      // too short
        assertEquals(null, XmlTvParser.parseXmlTvTime("2024011x193000"))     // non-digit
        assertEquals(null, XmlTvParser.parseXmlTvTime("20241315193000"))     // month 13
        assertEquals(null, XmlTvParser.parseXmlTvTime("20240115253000"))     // hour 25
        assertEquals(null, XmlTvParser.parseXmlTvTime("20240115193000 0100"))  // offset with no sign
        assertEquals(null, XmlTvParser.parseXmlTvTime("20240115193000 +01"))   // truncated offset
    }

    @Test
    fun `surrounding whitespace is tolerated`() {
        assertEquals(utcMillis(2024, 1, 15, 19, 30, 0), XmlTvParser.parseXmlTvTime("  20240115193000  "))
    }
}
