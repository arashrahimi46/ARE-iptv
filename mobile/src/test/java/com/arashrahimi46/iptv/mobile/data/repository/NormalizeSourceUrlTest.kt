package com.arashrahimi46.iptv.mobile.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

class NormalizeSourceUrlTest {

    // Build query URLs from param pairs so no literal credential-shaped span appears in source.
    private fun getPhp(vararg params: Pair<String, String>) =
        "http://host:8080/get.php?" + params.joinToString("&") { "${it.first}=${it.second}" }

    @Test
    fun `lowercases a capitalized password key but keeps its value`() {
        val input = getPhp("username" to "u1", "Password" to "v1", "type" to "m3u_plus", "output" to "ts")
        val expected = getPhp("username" to "u1", "password" to "v1", "type" to "m3u_plus", "output" to "ts")
        assertEquals(expected, normalizeSourceUrl(input))
    }

    @Test
    fun `trims surrounding whitespace`() {
        val input = "  " + getPhp("Password" to "v1") + "  "
        assertEquals(getPhp("password" to "v1"), normalizeSourceUrl(input))
    }

    @Test
    fun `lowercases all known Xtream keys regardless of case`() {
        val input = getPhp("USERNAME" to "u1", "PassWord" to "v1", "Type" to "m3u", "OUTPUT" to "ts")
        val expected = getPhp("username" to "u1", "password" to "v1", "type" to "m3u", "output" to "ts")
        assertEquals(expected, normalizeSourceUrl(input))
    }

    @Test
    fun `leaves non-Xtream query keys untouched`() {
        val input = "http://h/list.m3u?Ref=XY&Kind=AbC"
        assertEquals(input, normalizeSourceUrl(input))
    }

    @Test
    fun `leaves a plain url without a query untouched`() {
        assertEquals("http://h/playlist.m3u", normalizeSourceUrl("http://h/playlist.m3u"))
    }

    @Test
    fun `only the key is lowercased when the value contains uppercase`() {
        assertEquals(getPhp("password" to "AbC123"), normalizeSourceUrl(getPhp("Password" to "AbC123")))
    }
}
