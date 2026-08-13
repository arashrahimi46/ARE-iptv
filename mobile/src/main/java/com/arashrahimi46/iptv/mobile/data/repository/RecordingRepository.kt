package com.arashrahimi46.iptv.mobile.data.repository

import android.content.Context
import android.net.Uri
import com.arashrahimi46.iptv.mobile.data.db.AppDatabase
import com.arashrahimi46.iptv.mobile.data.model.Recording
import com.arashrahimi46.iptv.mobile.data.model.RecordingStatus
import com.arashrahimi46.iptv.mobile.data.recording.RecordingStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * DB access + launch-time reconciliation for Live TV Recording V1 (design §5/§6). The DB row is the
 * source of truth; UNAVAILABLE/MISSING are NOT stored but derived per-emission from live file/drive
 * presence, so a recording whose USB drive is unplugged shows as UNAVAILABLE and reconciles back to
 * its real terminal status automatically the moment the drive returns -- no status round-tripping.
 */
class RecordingRepository(context: Context) {
    private val dao = AppDatabase.get(context).recordingDao()
    private val continueWatchingDao = AppDatabase.get(context).continueWatchingDao()
    private val storage = RecordingStorage(context)

    /** Recordings for one playlist [sourceId], newest first, with UNAVAILABLE/MISSING layered on top
     * of the stored status. Recordings are per-playlist -- see [deleteForSource]. */
    fun observeRecordings(sourceId: Long): Flow<List<Recording>> =
        dao.observeBySource(sourceId).map { list -> list.map { it.withDerivedAvailability() } }.flowOn(Dispatchers.IO)

    private fun Recording.withDerivedAvailability(): Recording {
        // A live capture in progress, or a row that never captured a byte, has no file to probe.
        if (status == RecordingStatus.RECORDING || status == RecordingStatus.FAILED) return this
        val treeUri = runCatching { Uri.parse(storageTreeUri) }.getOrNull() ?: return this
        return when {
            storage.documentExists(treeUri, documentId) -> this
            !storage.driveAvailable(treeUri) -> copy(status = RecordingStatus.UNAVAILABLE)
            else -> copy(status = RecordingStatus.MISSING)
        }
    }

    suspend fun getById(id: Long): Recording? = withContext(Dispatchers.IO) { dao.getById(id) }

    /**
     * Launch-time reconciliation (design §6): any recording left RECORDING was orphaned by a crash /
     * power loss -- finalize it INTERRUPTED, approximating its duration from bytes/bitrate first so
     * the partial still shows a sensible length. Idempotent: a clean shutdown leaves nothing to fix.
     */
    suspend fun reconcileOnLaunch(crashReason: String) = withContext(Dispatchers.IO) {
        val orphaned = dao.orphanedRecordings()
        for (row in orphaned) {
            val approxDuration = row.bitrateBps?.takeIf { it > 0 }?.let { row.sizeBytes * 8 * 1000 / it }
            dao.finalize(row.id, RecordingStatus.INTERRUPTED, crashReason, approxDuration ?: row.durationMs)
        }
    }

    /** Delete a recording: its resume bookmark, its DB row, and (best-effort) its file(s) on disk.
     * When the drive is gone the file delete simply fails and is left as orphaned bytes on that drive. */
    suspend fun delete(recording: Recording) = withContext(Dispatchers.IO) {
        continueWatchingDao.deleteByRecording(recording.id)
        runCatching { Uri.parse(recording.storageTreeUri) }.getOrNull()?.let { treeUri ->
            storage.deleteDocument(treeUri, recording.documentId)
        }
        dao.delete(recording)
    }

    /**
     * Cascade when a playlist source is deleted: drop every recording row for [sourceId] and its
     * resume bookmark, AND delete the file on disk -- but only for recordings on INTERNAL storage.
     * Files on external drives (USB/SD/SAF) are left in place (the drive may be shared / removed).
     */
    suspend fun deleteForSource(sourceId: Long) = withContext(Dispatchers.IO) {
        val rows = dao.getBySource(sourceId)
        for (r in rows) {
            continueWatchingDao.deleteByRecording(r.id)
            val treeUri = runCatching { Uri.parse(r.storageTreeUri) }.getOrNull()
            if (treeUri != null && storage.isInternalStorage(treeUri)) {
                storage.deleteDocument(treeUri, r.documentId)
            }
        }
        dao.deleteBySource(sourceId)
    }
}
