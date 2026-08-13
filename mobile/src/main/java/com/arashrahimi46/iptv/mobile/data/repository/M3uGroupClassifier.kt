package com.arashrahimi46.iptv.mobile.data.repository

import com.arashrahimi46.iptv.mobile.data.model.ContentType

/**
 * M3U has no explicit content-type field; classify by group-title keyword as a
 * best-effort heuristic (real playlists commonly label VOD groups "Movies"/"VOD"
 * and "Series"/"Shows"). Extracted from [PlaylistRepositoryImpl.addM3uSource] so
 * it's independently unit-testable.
 *
 * Scoped to the M3U path ONLY -- [PlaylistRepositoryImpl.addXtreamSource] uses the
 * authoritative get_live_streams/get_vod_streams/get_series endpoints directly, no
 * heuristic involved.
 *
 * Known real-world false positive (qa-reviewer Phase 1 finding): this substring
 * match misclassifies live/linear channel groups whose names happen to contain
 * these words, e.g. "USA Movies HD" (a 24/7 linear movie channel, not on-demand)
 * or "Kids Shows" (a live kids bouquet) would be routed into Movies/Series instead
 * of Live TV. Accepted tradeoff for v1 since M3U provides no authoritative type
 * signal; revisit if this proves common enough in real provider playlists.
 */
internal fun classifyM3uGroup(group: String): ContentType {
    val groupLower = group.lowercase()
    return when {
        "series" in groupLower || "show" in groupLower -> ContentType.SERIES
        "movie" in groupLower || "vod" in groupLower -> ContentType.MOVIE
        else -> ContentType.LIVE
    }
}
