package com.arashrahimi46.iptv.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * A get.php M3U link that carries Xtream credentials must be recognised so the import goes through
 * the authoritative player_api (real content type + full VOD category list) rather than the
 * group-title keyword heuristic, which silently drops VOD categories not named "*Movies*".
 */
class ParseXtreamGetPhpTest {

    // Build query URLs from param pairs so no literal credential-shaped span appears in source.
    private fun getPhp(host: String = "http://host:8080", marker: String = "get.php", vararg params: Pair<String, String>) =
        "$host/$marker?" + params.joinToString("&") { "${it.first}=${it.second}" }

    @Test
    fun `extracts host and credentials from a get_php portal link`() {
        val link = parseXtreamGetPhp(
            getPhp(params = arrayOf("username" to "u1", "password" to "p1", "type" to "m3u_plus", "output" to "ts")),
        )
        assertEquals("http://host:8080", link?.host)
        assertEquals("u1", link?.username)
        assertEquals("p1", link?.password)
    }

    @Test
    fun `trims surrounding whitespace before parsing`() {
        val link = parseXtreamGetPhp("  " + getPhp(params = arrayOf("username" to "u1", "password" to "p1")) + "  ")
        assertEquals("http://host:8080", link?.host)
        assertEquals("u1", link?.username)
    }

    @Test
    fun `is case-insensitive on the get_php marker and the credential keys`() {
        val link = parseXtreamGetPhp(
            getPhp(marker = "GET.PHP", params = arrayOf("USERNAME" to "u1", "PassWord" to "p1", "type" to "m3u")),
        )
        assertEquals("u1", link?.username)
        assertEquals("p1", link?.password)
    }

    @Test
    fun `returns null for a plain m3u link with no credentials`() {
        assertNull(parseXtreamGetPhp("http://host/playlist.m3u"))
    }

    @Test
    fun `returns null when username or password is missing`() {
        assertNull(parseXtreamGetPhp(getPhp(params = arrayOf("username" to "u1", "type" to "m3u"))))
        assertNull(parseXtreamGetPhp(getPhp(params = arrayOf("password" to "p1", "type" to "m3u"))))
    }

    @Test
    fun `returns null when a credential value is blank`() {
        assertNull(parseXtreamGetPhp(getPhp(params = arrayOf("username" to "u1", "password" to ""))))
    }
}
