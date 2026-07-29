package com.arashrahimi46.iptv.data.repository

import android.content.Context
import com.arashrahimi46.iptv.data.db.AppDatabase
import com.arashrahimi46.iptv.data.model.ContinueWatchingEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Resume-playback bookmarks (P1.2) for VOD/series content -- live TV doesn't resume.
 * [ContinueWatchingEntry] has no unique DB constraint on (vodTitleId, seriesEpisodeId), so this
 * wraps the DAO's find-then-upsert pattern to keep exactly one row per title/episode instead of
 * inserting a new row every time [updateProgress] is called during playback.
 */
class ContinueWatchingRepository(context: Context) {
    private val dao = AppDatabase.get(context).continueWatchingDao()

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

    /** Most-recently-updated in-progress entries, bounded for the Home rail. */
    fun observeRecent(limit: Int): Flow<List<ContinueWatchingEntry>> = dao.observeRecent(limit)

    /** Clears every resume bookmark (Settings "Clear continue-watching / history"). */
    suspend fun clearAll(): Unit = withContext(Dispatchers.IO) { dao.deleteAll() }

    companion object {
        /** Retention cap -- keep only the most-recent bookmarks so the rail stays bounded. */
        private const val MAX_ENTRIES = 15
    }
}
