package com.arashrahimi46.iptv.data.settings

/**
 * Heuristic classifier for "adult" catalog categories, used to actually enforce the
 * Settings "Lock adult categories" toggle ([UserSettings.isParentalLockEnabled]) -- which
 * previously gated nothing. IPTV portals have no ratings field; they mark adult content in
 * the category/group NAME (e.g. "XXX", "FOR ADULTS 18+", "Adult Movies"), so this matches on
 * well-known markers case-insensitively. When the lock is on, browse / search / home hide any
 * category -- and any item whose [com.arashrahimi46.iptv.data.model.Channel.categoryName] /
 * [com.arashrahimi46.iptv.data.model.VodTitle.categoryName] -- that this flags.
 *
 * monolean: keyword heuristic -- upgrade path is a per-item age rating once portals expose one,
 * and/or PIN-gated reveal (the parental PIN already exists) instead of outright hiding.
 */
object AdultContentFilter {
    /** Lowercased substrings; deliberately conservative to avoid false positives on normal genres. */
    private val MARKERS = listOf(
        "xxx", "adult", "porn", "erotic", "erotik", "hardcore",
        "brazzers", "playboy", "hustler", "+18", "18+",
    )

    fun isAdult(categoryName: String?): Boolean {
        val name = categoryName?.lowercase() ?: return false
        return MARKERS.any { it in name }
    }
}
