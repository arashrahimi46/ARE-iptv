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
}
