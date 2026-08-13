package com.arashrahimi46.iptv.mobile.data.parser

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Covers the pure URL-construction surface of [XtreamClient] only -- the network-calling
 * methods (`authenticate`, `getLiveStreams`, etc.) go through a real `OkHttpClient` and
 * aren't exercised here without a mock server / dependency injection seam, which is out
 * of scope for this pass. `streamUrl`/`xmltvUrl` are the two pieces that were sanity-checked
 * by hand against the real Xtream Codes API convention during Phase 2 -- pinning that
 * behavior in a test now that it's proven correct.
 */
class XtreamClientTest {

    @Test
    fun `streamUrl builds the standard Xtream path shape`() {
        val client = XtreamClient(host = "http://portal.example.com:8080", username = "user1", password = "pass1")

        assertEquals(
            "http://portal.example.com:8080/live/user1/pass1/12345.m3u8",
            client.streamUrl("live", "12345", "m3u8"),
        )
        assertEquals(
            "http://portal.example.com:8080/movie/user1/pass1/999.mp4",
            client.streamUrl("movie", "999", "mp4"),
        )
        assertEquals(
            "http://portal.example.com:8080/series/user1/pass1/42.mkv",
            client.streamUrl("series", "42", "mkv"),
        )
    }

    @Test
    fun `host without an explicit scheme is normalized to http`() {
        val client = XtreamClient(host = "portal.example.com", username = "u", password = "p")

        assertEquals("http://portal.example.com/live/u/p/1.m3u8", client.streamUrl("live", "1", "m3u8"))
    }

    @Test
    fun `host with https scheme is preserved`() {
        val client = XtreamClient(host = "https://secure.example.com", username = "u", password = "p")

        assertEquals("https://secure.example.com/live/u/p/1.m3u8", client.streamUrl("live", "1", "m3u8"))
    }

    @Test
    fun `trailing slash on host is trimmed, not doubled`() {
        val client = XtreamClient(host = "http://portal.example.com/", username = "u", password = "p")

        assertEquals("http://portal.example.com/live/u/p/1.m3u8", client.streamUrl("live", "1", "m3u8"))
    }

    @Test
    fun `xmltvUrl builds the bulk EPG export path`() {
        val client = XtreamClient(host = "http://portal.example.com", username = "user1", password = "pass1")

        assertEquals(
            "http://portal.example.com/xmltv.php?username=user1&password=pass1",
            client.xmltvUrl(),
        )
    }
}
