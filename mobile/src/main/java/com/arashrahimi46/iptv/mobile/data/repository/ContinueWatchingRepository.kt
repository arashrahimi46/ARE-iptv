package com.arashrahimi46.iptv.mobile.data.repository

import android.content.Context
import com.arashrahimi46.iptv.mobile.data.db.AppDatabase
import com.arashrahimi46.iptv.mobile.data.model.ContinueWatchingEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Resume-playback bookmarks (P1.2) for VOD/series content -- live TV doesn't resume.
 * [ContinueWatchingEntry] has no unique DB constraint on (vodTitleId, seriesEpisodeId), so this
 * wraps the DAO's find-then-upsert pattern to keep exactly one row per title/episode instead of
 * inserting a new row every time [updateProgress] is called during playback.
 */
class ContinueWatchingRepository(context: Context) {
    private val dao = AppDatabase.get(context).continueWatchingDao()
    private val seriesEpisodeDao = AppDatabase.get(context).seriesEpisodeDao()

    /** Persists (or replaces) the bookmark for exactly one of [vodTitleId]/[seriesEpisodeId]/[recordingId]. */
    suspend fun updateProgress(vodTitleId: Long?, seriesEpisodeId: Long?, positionMs: Long, durationMs: Long, recordingId: Long? = null): Unit =
        withContext(Dispatchers.IO) {
            val existing = vodTitleId?.let { dao.findByVod(it) }
                ?: seriesEpisodeId?.let { dao.findByEpisode(it) }
                ?: recordingId?.let { dao.findByRecording(it) }
            dao.upsert(
                ContinueWatchingEntry(
                    id = existing?.id ?: 0,
                    vodTitleId = vodTitleId,
                    seriesEpisodeId = seriesEpisodeId,
                    recordingId = recordingId,
                    positionMs = positionMs,
                    durationMs = durationMs,
                    updatedAtMs = System.currentTimeMillis(),
                ),
            )
            dao.trimToMostRecent(MAX_ENTRIES)
        }

    /** Clears the bookmark -- called once playback reaches "near the end" so a finished
     * title drops off the Continue Watching rail instead of resuming from its very end. */
    suspend fun clear(vodTitleId: Long?, seriesEpisodeId: Long?, recordingId: Long? = null): Unit = withContext(Dispatchers.IO) {
        vodTitleId?.let { dao.deleteByVod(it) }
        seriesEpisodeId?.let { dao.deleteByEpisode(it) }
        recordingId?.let { dao.deleteByRecording(it) }
    }

    /** Saved resume position for [vodTitleId]/[seriesEpisodeId]/[recordingId], or 0 if there's no bookmark. */
    suspend fun resumePositionFor(vodTitleId: Long?, seriesEpisodeId: Long?, recordingId: Long? = null): Long = withContext(Dispatchers.IO) {
        val entry = vodTitleId?.let { dao.findByVod(it) }
            ?: seriesEpisodeId?.let { dao.findByEpisode(it) }
            ?: recordingId?.let { dao.findByRecording(it) }
        entry?.positionMs ?: 0L
    }

    /** Every bookmark among [episodeIds], keyed by episode id, re-emitted whenever one changes.
     * Used by the Series detail screen to draw per-episode progress and to pick which episode
     * "Resume" reopens (the most recently updated one). */
    fun observeEntriesForEpisodes(episodeIds: List<Long>): Flow<Map<Long, ContinueWatchingEntry>> =
        if (episodeIds.isEmpty()) {
            flowOf(emptyMap())
        } else {
            dao.observeByEpisodes(episodeIds).map { entries ->
                entries.mapNotNull { entry -> entry.seriesEpisodeId?.let { it to entry } }.toMap()
            }
        }

    /** Most-recently-updated in-progress entries, bounded for the Home rail -- at most one entry
     * per series (see [dedupeBySeries]); movie/recording entries pass through untouched. */
    fun observeRecent(limit: Int): Flow<List<ContinueWatchingEntry>> = dao.observeRecent(limit).map { dedupeBySeries(it) }

    /** Collapses multiple in-progress-episode rows of the same series down to the single
     * most-recently-updated one -- Product decision: Continue Watching shows one tile per series,
     * not one per in-progress episode. [dao.observeRecent] is already `ORDER BY updatedAtMs DESC`,
     * so keeping the first row seen per series id keeps the newest.
     *
     * This is also the fix for a P0 crash-loop: two in-progress episodes of the same series both
     * resolve to the same parent series/title id downstream (Home's rail joins each entry to its
     * VodTitle/series), and both UI layers keyed their LazyRow items by that resolved id --
     * producing a duplicate Compose key and an IllegalArgumentException crash on every launch.
     * Because this runs on every emission of the underlying query, it self-heals installs that
     * already have duplicate rows persisted -- no migration/wipe needed. */
    private suspend fun dedupeBySeries(entries: List<ContinueWatchingEntry>): List<ContinueWatchingEntry> {
        val episodeIds = entries.mapNotNull { it.seriesEpisodeId }
        if (episodeIds.isEmpty()) return entries
        val seriesIdByEpisodeId = episodeIds.distinct().associateWith { seriesEpisodeDao.getById(it)?.seriesTitleId }
        val seenSeriesIds = mutableSetOf<Long>()
        return entries.filter { entry ->
            val seriesId = entry.seriesEpisodeId?.let { seriesIdByEpisodeId[it] } ?: return@filter true
            seenSeriesIds.add(seriesId)
        }
    }

    /** Clears every resume bookmark (Settings "Clear continue-watching / history"). */
    suspend fun clearAll(): Unit = withContext(Dispatchers.IO) { dao.deleteAll() }

    companion object {
        /** Retention cap -- keep only the most-recent bookmarks so the rail stays bounded. */
        private const val MAX_ENTRIES = 15
    }
}
