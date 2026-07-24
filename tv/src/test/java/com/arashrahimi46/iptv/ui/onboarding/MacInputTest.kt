package com.arashrahimi46.iptv.ui.onboarding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure MAC entry helpers — auto-formatting keystrokes and validating the finished address. */
class MacInputTest {

    @Test
    fun `formats raw hex into colon groups as typed`() {
        assertEquals("00:1A:79", formatMacInput("001a79"))
        assertEquals("00:1A:79:AB:CD:EF", formatMacInput("001A79ABCDEF"))
        // A pasted colon-form is normalized (colons dropped then re-inserted, uppercased).
        assertEquals("00:1A:79:AB:CD:EF", formatMacInput("00:1a:79:ab:cd:ef"))
    }

    @Test
    fun `drops non-hex and caps at 12 digits`() {
        assertEquals("AB:CD:EF", formatMacInput("g h AB!CD@EF"))
        assertEquals("00:1A:79:AB:CD:EF", formatMacInput("001A79ABCDEF99")) // extra pair ignored
    }

    @Test
    fun `validates only a complete well-formed MAC`() {
        assertTrue(isValidMac("00:1A:79:AB:CD:EF"))
        assertTrue(isValidMac("00:1a:79:ab:cd:ef"))
        assertFalse(isValidMac("00:1A:79:AB:CD"))   // too short
        assertFalse(isValidMac("001A79ABCDEF"))     // no colons
        assertFalse(isValidMac("00:1A:79:AB:CD:GG")) // non-hex
        assertFalse(isValidMac(""))
    }
}
