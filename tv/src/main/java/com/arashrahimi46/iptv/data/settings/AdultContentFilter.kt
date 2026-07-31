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

    /**
     * [extraMarkers] are the user's custom blocked keywords ([UserSettings.parentalKeywords]),
     * merged with the built-ins so a portal that uses an unusual label can still be caught.
     */
    fun isAdult(categoryName: String?, extraMarkers: Set<String> = emptySet()): Boolean {
        val name = categoryName?.lowercase() ?: return false
        return MARKERS.any { it in name } || extraMarkers.any { it.isNotBlank() && it in name }
    }
}

/**
 * The effective parental filter for a catalog list (see [UserSettings.parentalFilter]).
 * [hidden] is true only when the item should be dropped entirely -- i.e. the lock is on, the user
 * chose HIDE (not BLUR), and no session unlock is active. BLUR mode returns false here (items stay
 * in the list and are obscured at the tile instead), and an active unlock reveals everything.
 */
data class ParentalFilter(
    val hideLocked: Boolean = false,
    val keywords: Set<String> = emptySet(),
) {
    fun hidden(categoryName: String?): Boolean = hideLocked && AdultContentFilter.isAdult(categoryName, keywords)
}
