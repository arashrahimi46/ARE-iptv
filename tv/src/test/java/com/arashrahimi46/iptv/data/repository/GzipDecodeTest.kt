package com.arashrahimi46.iptv.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream

/**
 * Covers the gzip-decompression fix that lets the app read the open-source XMLTV ecosystem
 * (epgshare01 etc.), which serves guides as gzipped `.xml.gz` file bodies. [decodeMaybeGzip]
 * must inflate gzipped input and pass plain input through unchanged, sniffing the payload
 * rather than any URL/extension.
 */
class GzipDecodeTest {

    private val xml = "<tv><programme channel=\"BBC.uk\"><title>News</title></programme></tv>"

    @Test
    fun `plain xml passes through unchanged`() {
        assertEquals(xml, decodeMaybeGzip(ByteArrayInputStream(xml.toByteArray())))
    }

    @Test
    fun `gzipped xml is inflated`() {
        val gzipped = ByteArrayOutputStream().also { out ->
            GZIPOutputStream(out).use { it.write(xml.toByteArray()) }
        }.toByteArray()
        assertEquals(xml, decodeMaybeGzip(ByteArrayInputStream(gzipped)))
    }
}
