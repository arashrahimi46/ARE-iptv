package com.arashrahimi46.iptv.mobile.ui.series

/**
 * What the Series detail screen's primary button should start.
 *
 * A grouped series has no stream of its own -- only its episodes do -- so the button has to resolve
 * to a concrete row before it can navigate anywhere. The degenerate case is an M3U entry that was
 * imported as `isSeries = true` but never matched `SxxEyy`, so it has zero episode rows and a
 * perfectly playable [com.arashrahimi46.iptv.mobile.data.model.VodTitle.streamUrl] of its own. That case
 * is [WholeTitle], and it is exactly the branch whose absence produced the
 * "This item has no playable stream" dead end on :mobile.
 */
sealed interface SeriesPlayTarget {
    /** Play one episode row -- the normal case. Routes to `player/episode/{id}`. */
    data class Episode(val episodeId: Long) : SeriesPlayTarget

    /** Play the parent row itself. Routes to `player/movie/{id}`: [com.arashrahimi46.iptv.mobile.ui.player.PlayerTarget.Movie]
     * reads `VodTitle.streamUrl` and bookmarks by `vodTitleId`, which is the right semantics for an
     * episode-less series row. */
    data class WholeTitle(val vodTitleId: Long) : SeriesPlayTarget
}

/**
 * Pure resolution of the primary action, mirroring :tv's `DetailScreen.resolvePlayTarget`.
 *
 * Order matters: an in-progress episode wins over "start from the beginning", and the parent row's
 * own URL is only ever used when there is no episode structure at all.
 */
internal fun resolveSeriesPlayTarget(state: SeriesDetailUiState): SeriesPlayTarget? {
    val title = state.title ?: return null
    state.resumeEpisodeId?.let { return SeriesPlayTarget.Episode(it) }
    firstEpisode(state)?.let { return SeriesPlayTarget.Episode(it.id) }
    if (!title.streamUrl.isNullOrBlank()) return SeriesPlayTarget.WholeTitle(title.id)
    return null
}

/** Lowest episode of the lowest season, or null when there is no episode structure. */
internal fun firstEpisode(state: SeriesDetailUiState) =
    state.episodesBySeason.keys.minOrNull()
        ?.let { season -> state.episodesBySeason[season]?.minByOrNull { it.episode } }
