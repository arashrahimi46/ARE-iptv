package com.arashrahimi46.iptv.ui.guide

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Covers the root-cause fix for issue #2 ("EPG list not scrollable / no channels visible"):
 * [GuideViewModel.observeRows] used to validate the persisted category filter only once,
 * before [com.arashrahimi46.iptv.data.settings.UserSettings.guideSelectedCategory] finished
 * restoring asynchronously -- a stale/mismatched category landing after that point silently
 * filtered out every channel, leaving the guide empty. [resolveGuideGroup] is the extracted
 * pure resolution now applied on every emission.
 */
class GuideViewModelTest {

    @Test
    fun `valid group passes through unchanged`() {
        val groups = listOf("All", "Sports", "News")
        assertEquals("Sports", resolveGuideGroup("Sports", groups))
    }

    @Test
    fun `unknown group falls back to All`() {
        val groups = listOf("All", "Sports", "News")
        // e.g. a category persisted from a previous playlist that doesn't exist on this one.
        assertEquals("All", resolveGuideGroup("Kids", groups))
    }

    @Test
    fun `All is always valid`() {
        assertEquals("All", resolveGuideGroup("All", listOf("All")))
    }

    @Test
    fun `stale group falls back to All even when groups list is empty aside from All`() {
        assertEquals("All", resolveGuideGroup("Sports", listOf("All")))
    }
}
