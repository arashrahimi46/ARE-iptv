package com.arashrahimi46.iptv.data.parser

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/** One `<programme>` entry from an XMLTV document, keyed by the XMLTV `channel` id (matches [com.arashrahimi46.iptv.data.model.Channel.tvgId]). */
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
    // XMLTV datetime: "20240115193000 +0000" (offset optional). Per the XMLTV spec, a
    // datetime with no offset is UTC -- NOT the parsing device's local time -- so each
    // pattern is paired with the [TimeZone] SimpleDateFormat should assume when the string
    // itself carries no zone info ("yyyyMMddHHmmss Z"'s literal " +0000"/etc always wins;
    // the device default here previously silently shifted every no-offset programme by the
    // device's UTC offset, corrupting both display time and window filtering -- see report).
    private val formats = listOf(
        "yyyyMMddHHmmss Z" to null,
        "yyyyMMddHHmmss" to TimeZone.getTimeZone("UTC"),
    )

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

    private fun parseXmlTvTime(raw: String?): Long? {
        if (raw.isNullOrBlank()) return null
        for ((pattern, fallbackZone) in formats) {
            val parsed = runCatching {
                val sdf = SimpleDateFormat(pattern, Locale.US)
                if (fallbackZone != null) sdf.timeZone = fallbackZone
                sdf.parse(raw.trim())
            }.getOrNull()
            if (parsed != null) return parsed.time
        }
        return null
    }
}
