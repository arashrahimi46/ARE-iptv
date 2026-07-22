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
        val groups = listOf("News", "Sports")
        assertEquals("Sports", resolveGuideGroup("Sports", groups))
    }

    @Test
    fun `unknown group falls back to the first category`() {
        val groups = listOf("News", "Sports")
        // e.g. a category persisted from a previous playlist that doesn't exist on this one.
        assertEquals("News", resolveGuideGroup("Kids", groups))
    }

    @Test
    fun `legacy All default falls back to the first category`() {
        // "All" is no longer a real tab -- the Guide is strictly per-category now.
        assertEquals("News", resolveGuideGroup("All", listOf("News", "Sports")))
    }

    @Test
    fun `empty groups returns the raw group unchanged`() {
        assertEquals("Sports", resolveGuideGroup("Sports", emptyList()))
    }
}
