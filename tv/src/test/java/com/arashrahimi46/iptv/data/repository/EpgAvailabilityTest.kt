package com.arashrahimi46.iptv.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers P0.4's root-cause fix ("EpgRepository.refresh swallows all fetch failures silently"):
 * [resolveEpgAvailability] is the extracted pure resolution [EpgRepository.refresh] applies at
 * the end of every run, distinguishing "fetched fine, genuinely no programme data" from
 * "the source couldn't be reached/parsed at all".
 */
class EpgAvailabilityTest {

    @Test
    fun `rows present is always Available regardless of attempt bookkeeping`() {
        assertEquals(EpgAvailability.Available, resolveEpgAvailability(rowsEmpty = false, attemptedAny = false, succeededAny = false))
        assertEquals(EpgAvailability.Available, resolveEpgAvailability(rowsEmpty = false, attemptedAny = true, succeededAny = false))
    }

    @Test
    fun `empty rows with no attempt made is Unavailable`() {
        val result = resolveEpgAvailability(rowsEmpty = true, attemptedAny = false, succeededAny = false)
        assertTrue(result is EpgAvailability.Unavailable)
    }

    @Test
    fun `empty rows after a successful fetch is genuinely no programme data, not Unavailable`() {
        assertEquals(EpgAvailability.Available, resolveEpgAvailability(rowsEmpty = true, attemptedAny = true, succeededAny = true))
    }

    @Test
    fun `empty rows after every attempt failed is Unavailable`() {
        val result = resolveEpgAvailability(rowsEmpty = true, attemptedAny = true, succeededAny = false)
        assertTrue(result is EpgAvailability.Unavailable)
    }
}
