package com.arashrahimi46.iptv.mobile.data.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinHasherTest {

    @Test
    fun `verify succeeds for the correct PIN and salt`() {
        val salt = PinHasher.newSalt()
        val hash = PinHasher.hash("1234", salt)
        assertTrue(PinHasher.verify("1234", salt, hash))
    }

    @Test
    fun `verify fails for a wrong PIN`() {
        val salt = PinHasher.newSalt()
        val hash = PinHasher.hash("1234", salt)
        assertFalse(PinHasher.verify("4321", salt, hash))
    }

    @Test
    fun `same PIN with different salts hashes differently`() {
        val saltA = PinHasher.newSalt()
        val saltB = PinHasher.newSalt()
        assertNotEquals(PinHasher.hash("1234", saltA), PinHasher.hash("1234", saltB))
    }

    @Test
    fun `newSalt is not deterministic`() {
        assertNotEquals(PinHasher.newSalt(), PinHasher.newSalt())
    }
}
