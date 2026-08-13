package com.arashrahimi46.iptv.mobile.data.parser

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader

/** One `<programme>` entry from an XMLTV document, keyed by the XMLTV `channel` id (matches [com.arashrahimi46.iptv.mobile.data.model.Channel.tvgId]). */
data class XmlTvProgramme(
    val channelRef: String,
    val title: String,
    val description: String?,
    val startMs: Long,
    val stopMs: Long,
)

/**
 * Minimal XMLTV pull-parser -- just enough of the format to populate the
 * Guide grid (`<programme channel=".." start=".." stop="..">` with a
 * `<title>` and optional `<desc>`). Uses the platform's built-in
 * [XmlPullParser] rather than a third-party XML library.
 */
object XmlTvParser {
    fun parse(xml: String): List<XmlTvProgramme> {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(StringReader(xml))

        val results = mutableListOf<XmlTvProgramme>()
        var event = parser.eventType
        var inProgramme = false
        var channelRef: String? = null
        var startMs: Long? = null
        var stopMs: Long? = null
        var title: String? = null
        var description: String? = null
        var textBuffer = StringBuilder()

        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "programme" -> {
                            inProgramme = true
                            channelRef = parser.getAttributeValue(null, "channel")
                            startMs = parseXmlTvTime(parser.getAttributeValue(null, "start"))
                            stopMs = parseXmlTvTime(parser.getAttributeValue(null, "stop"))
                            title = null
                            description = null
                        }
                        "title", "desc" -> textBuffer = StringBuilder()
                    }
                }
                XmlPullParser.TEXT -> if (inProgramme) textBuffer.append(parser.text)
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "title" -> if (inProgramme) title = textBuffer.toString().trim()
                        "desc" -> if (inProgramme) description = textBuffer.toString().trim()
                        "programme" -> {
                            val ch = channelRef
                            val start = startMs
                            val stop = stopMs
                            val t = title
                            if (ch != null && start != null && stop != null && !t.isNullOrBlank()) {
                                results += XmlTvProgramme(ch, t, description?.ifBlank { null }, start, stop)
                            }
                            inProgramme = false
                        }
                    }
                }
            }
            event = runCatching { parser.next() }.getOrElse { XmlPullParser.END_DOCUMENT }
        }
        return results
    }

    /**
     * `yyyyMMddHHmmss` with an optional ` ±HHMM` offset -> epoch millis, or null if malformed.
     *
     * PERF: hand-parsed rather than via [SimpleDateFormat], because this runs once per `start` and
     * once per `stop` on every programme -- 10^5 to 10^6 times for a large provider's EPG. The old
     * version constructed a NEW `SimpleDateFormat` per attempt inside `runCatching`, and since the
     * offset-less form is very common in the wild it failed the offsetted pattern first: so a typical
     * timestamp cost two SimpleDateFormat constructions (each compiling the pattern and building a
     * Calendar + DateFormatSymbols) plus a thrown, stack-filled ParseException. At 200k programmes
     * that is ~800k format constructions and ~400k exceptions -- many seconds of pure CPU on a weak
     * TV SoC, and enough garbage to trigger repeated GCs mid-import. This does zero allocation
     * beyond one trim and throws nothing.
     *
     * Per the XMLTV spec a datetime with no offset is UTC -- deliberately NOT the device's local
     * zone, which previously shifted every no-offset programme by the device's offset and corrupted
     * both the displayed time and the guide's window filtering.
     */
    internal fun parseXmlTvTime(raw: String?): Long? {
        if (raw == null) return null
        val s = raw.trim()
        if (s.length < 14) return null
        for (i in 0 until 14) if (s[i] !in '0'..'9') return null

        val year = digits(s, 0, 4)
        val month = digits(s, 4, 2)
        val day = digits(s, 6, 2)
        val hour = digits(s, 8, 2)
        val minute = digits(s, 10, 2)
        val second = digits(s, 12, 2)
        if (month !in 1..12 || day !in 1..31 || hour > 23 || minute > 59 || second > 59) return null

        // Optional " +0100" / "-0430" tail. Anything else after the digits is malformed, not ignored.
        var offsetMs = 0L
        var i = 14
        while (i < s.length && s[i] == ' ') i++
        if (i < s.length) {
            val sign = when (s[i]) {
                '+' -> 1
                '-' -> -1
                else -> return null
            }
            if (i + 5 > s.length) return null
            for (k in i + 1..i + 4) if (s[k] !in '0'..'9') return null
            val offHours = digits(s, i + 1, 2)
            val offMinutes = digits(s, i + 3, 2)
            if (offMinutes > 59) return null
            offsetMs = sign * (offHours * 3_600_000L + offMinutes * 60_000L)
        }

        val utcMs = daysFromCivil(year, month, day) * 86_400_000L +
            hour * 3_600_000L + minute * 60_000L + second * 1_000L
        return utcMs - offsetMs
    }

    /** Fixed-width unsigned decimal at [start], [len] chars. Callers have already range-checked. */
    private fun digits(s: String, start: Int, len: Int): Int {
        var acc = 0
        for (i in start until start + len) acc = acc * 10 + (s[i] - '0')
        return acc
    }

    /**
     * Days since 1970-01-01 for a proleptic-Gregorian date (Howard Hinnant's `days_from_civil`).
     * Used instead of [java.util.Calendar] so the conversion allocates nothing and is not affected
     * by the device's default TimeZone or Locale.
     */
    private fun daysFromCivil(year: Int, month: Int, day: Int): Long {
        val y = year.toLong() - if (month <= 2) 1 else 0
        val era = (if (y >= 0) y else y - 399) / 400
        val yearOfEra = y - era * 400
        val dayOfYear = (153 * (month + (if (month > 2) -3 else 9)) + 2) / 5 + day - 1
        val dayOfEra = yearOfEra * 365 + yearOfEra / 4 - yearOfEra / 100 + dayOfYear
        return era * 146_097 + dayOfEra - 719_468
    }
}
