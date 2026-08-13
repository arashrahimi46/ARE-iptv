package com.arashrahimi46.iptv.mobile.data.recording

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import androidx.media3.datasource.DataSink
import androidx.media3.datasource.DataSpec
import com.arashrahimi46.iptv.mobile.data.db.AppDatabase
import com.arashrahimi46.iptv.mobile.data.model.Recording
import com.arashrahimi46.iptv.mobile.data.model.RecordingStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Owns one live recording end-to-end (design §3/§6): it IS the media3 [DataSink] the capture tee
 * writes into, and it runs the state machine, the bounded-buffer writer thread, the byte-flow /
 * free-space watchdog, FAT32 auto-split, and idempotent finalize.
 *
 * **Playback is sacred.** [write] (called on ExoPlayer's loader thread) never blocks and never
 * throws: bytes are copied into a bounded queue drained by a dedicated IO thread; if the disk can't
 * keep up and the queue overflows we drop recording bytes and mark the recording degraded rather
 * than stall the player. Continuous flush keeps the half-written `.ts` playable up to the last byte.
 *
 * Scoped to [LivePlayerViewModel][com.arashrahimi46.iptv.mobile.ui.player.LivePlayerViewModel]: leaving the
 * player finalizes any active recording cleanly (COMPLETED), which is the accepted "record while you
 * watch" lifecycle. The tee is always wired but this sink no-ops until [start] opens a session.
 */
class RecordingSupervisor(
    private val context: Context,
) : DataSink {
    private val dao = AppDatabase.get(context).recordingDao()
    private val storage = RecordingStorage(context)

    // Independent of any ViewModel scope: a clean finalize on "leave the player" must complete even
    // though the caller's viewModelScope is cancelled before onCleared() runs. finalize is
    // NonCancellable regardless; this scope keeps the writer/watchdog coroutines alive until then.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    enum class Phase { Idle, Recording, Reconnecting, Stopped }

    /**
     * The recording's *phase*, which changes only on real transitions (start / stall / stop).
     *
     * PERF: deliberately carries NO running counters. `elapsedMs`/`sizeBytes` used to live here and
     * were rewritten by [watchdogLoop] every second, so this StateFlow emitted a new value 1/s while
     * recording. LivePlayerScreen collects it at the very top of the screen function, which made the
     * *whole screen* -- every settings read, the media-source factory's remember, the entire player
     * subtree -- the invalidation scope for a counter that only one small badge renders. The counter
     * now lives in [elapsedMs] and is collected by that badge alone.
     */
    data class RecordingState(
        val phase: Phase = Phase.Idle,
        val recordingId: Long? = null,
        /** Set when the last session ended on a fault -- surfaced to the user, then cleared on next start. */
        val stoppedReason: String? = null,
    )

    enum class FailReason { NOT_WRITABLE, LOW_SPACE, HLS_STREAM, CREATE_FAILED }

    sealed class StartResult {
        data class Started(val id: Long) : StartResult()
        data class Failed(val reason: FailReason) : StartResult()
    }

    private val _state = MutableStateFlow(RecordingState())
    val state: StateFlow<RecordingState> = _state.asStateFlow()

    /** The running recording's elapsed wall-clock, ticked 1/s by [watchdogLoop]. Collected only by
     *  the recording badge -- see the note on [RecordingState]. */
    private val _elapsedMs = MutableStateFlow(0L)
    val elapsedMs: StateFlow<Long> = _elapsedMs.asStateFlow()

    val isRecording: Boolean get() = session != null

    @Volatile private var session: Session? = null

    private class Session(
        val recordingId: Long,
        val treeUri: Uri,
        val channelName: String,
        val programTitle: String?,
        val startedAtMs: Long,
        val startedElapsed: Long,
        var stream: OutputStream,
        var documentId: String,
    ) {
        val queue = ArrayBlockingQueue<ByteArray>(QUEUE_CAPACITY)
        val bytesTotal = AtomicLong(0)
        var currentPartBytes = 0L
        var part = 1
        val degraded = AtomicBoolean(false)
        val lastByteAt = AtomicLong(SystemClock.elapsedRealtime())
        val finalized = AtomicBoolean(false)
        @Volatile var running = true
        var writerThread: Thread? = null
        var watchdog: Job? = null
    }

    // --- DataSink (capture tee target). File lifecycle is owned by start/stop, NOT open/close --
    // ExoPlayer opens/closes the DataSource many times over one recording (reconnects), all
    // appending into the same file so the TS tolerates the gaps. ---

    override fun open(dataSpec: DataSpec) { /* no-op */ }

    override fun write(buffer: ByteArray, offset: Int, length: Int) {
        val s = session ?: return
        if (length <= 0) return
        val chunk = buffer.copyOfRange(offset, offset + length)
        if (!s.queue.offer(chunk)) {
            // Bounded buffer overflow -> disk can't keep up. Drop these bytes (never block the
            // player) and mark degraded so the recording finalizes INTERRUPTED, not COMPLETED.
            s.degraded.set(true)
        }
    }

    override fun close() { /* continuous flush handled by the writer thread */ }

    /**
     * Pre-flight then begin capturing [channelName]/[channelId] into [treeUri]. Returns [StartResult.Failed]
     * (no ghost REC) on any pre-flight/creation failure. Safe to call off the main thread.
     */
    suspend fun start(
        channelId: Long,
        channelName: String,
        programTitle: String?,
        treeUri: Uri,
        isHls: Boolean,
        sourceId: Long?,
    ): StartResult = withContext(Dispatchers.IO) {
        if (session != null) return@withContext StartResult.Started(session!!.recordingId)
        when (val pre = storage.preflight(treeUri, isHls, PREFLIGHT_BITRATE_BPS)) {
            is RecordingStorage.Preflight.Failed -> return@withContext StartResult.Failed(
                when (pre.reason) {
                    RecordingStorage.Preflight.Reason.NOT_WRITABLE -> FailReason.NOT_WRITABLE
                    RecordingStorage.Preflight.Reason.LOW_SPACE -> FailReason.LOW_SPACE
                    RecordingStorage.Preflight.Reason.HLS_STREAM -> FailReason.HLS_STREAM
                },
            )
            RecordingStorage.Preflight.Ok -> Unit
        }
        val startedAt = System.currentTimeMillis()
        val created = storage.createRecordingFile(treeUri, channelName, programTitle, startedAt, part = 0)
            ?: return@withContext StartResult.Failed(FailReason.CREATE_FAILED)
        val out = storage.openOutputStream(created.documentUri)
            ?: return@withContext StartResult.Failed(FailReason.CREATE_FAILED)
        val id = dao.insert(
            Recording(
                channelId = channelId,
                channelName = channelName,
                programTitle = programTitle,
                startedAtMs = startedAt,
                status = RecordingStatus.RECORDING,
                storageTreeUri = treeUri.toString(),
                documentId = created.documentId,
                volumeUuid = storage.volumeUuid(treeUri),
                sourceId = sourceId,
            ),
        )
        val s = Session(
            recordingId = id,
            treeUri = treeUri,
            channelName = channelName,
            programTitle = programTitle,
            startedAtMs = startedAt,
            startedElapsed = SystemClock.elapsedRealtime(),
            stream = out,
            documentId = created.documentId,
        )
        session = s
        startWriter(s)
        s.watchdog = scope.launch { watchdogLoop(s) }
        _elapsedMs.value = 0
        _state.value = RecordingState(phase = Phase.Recording, recordingId = id)
        StartResult.Started(id)
    }

    /** User-initiated clean stop (also the path when leaving the player). COMPLETED, or INTERRUPTED
     * if the recording degraded (dropped bytes) or never captured anything. */
    fun stop() {
        val s = session ?: return
        scope.launch {
            val status = when {
                s.bytesTotal.get() == 0L -> RecordingStatus.FAILED
                s.degraded.get() -> RecordingStatus.INTERRUPTED
                else -> RecordingStatus.COMPLETED
            }
            val reason = if (status == RecordingStatus.INTERRUPTED) SLOW_DISK else null
            finalize(s, status, reason)
        }
    }

    /** Fault-driven stop (watchdog / writer IO error). Always INTERRUPTED (or FAILED if no bytes). */
    private fun faultFinalize(s: Session, reason: String) {
        scope.launch {
            val status = if (s.bytesTotal.get() == 0L) RecordingStatus.FAILED else RecordingStatus.INTERRUPTED
            finalize(s, status, reason)
        }
    }

    private fun startWriter(s: Session) {
        val t = Thread({
            try {
                while (s.running) {
                    val chunk = s.queue.poll(500, TimeUnit.MILLISECONDS) ?: continue
                    s.stream.write(chunk)
                    s.stream.flush()
                    val total = s.bytesTotal.addAndGet(chunk.size.toLong())
                    s.currentPartBytes += chunk.size
                    s.lastByteAt.set(SystemClock.elapsedRealtime())
                    if (s.currentPartBytes >= SPLIT_THRESHOLD_BYTES) rollToNextPart(s)
                    // (total referenced to keep the addAndGet, and available for future tuning)
                    if (total < 0) break
                }
                // Drain whatever's queued at clean stop so the tail isn't lost.
                var tail = s.queue.poll()
                while (tail != null) {
                    s.stream.write(tail); tail = s.queue.poll()
                }
                s.stream.flush()
            } catch (e: Exception) {
                // Write failed mid-record -> drive pulled / disk error. The already-flushed partial
                // stays playable; finalize INTERRUPTED. Guarded so a second fault can't double-run.
                if (!s.finalized.get()) faultFinalize(s, DRIVE_REMOVED)
            }
        }, "recording-writer")
        t.isDaemon = true
        s.writerThread = t
        t.start()
    }

    /** FAT32 4 GB ceiling: close the current part and open the next before the limit is hit. */
    private fun rollToNextPart(s: Session) {
        runCatching { s.stream.flush(); s.stream.close() }
        val nextPart = s.part + 1
        val created = storage.createRecordingFile(s.treeUri, s.channelName, s.programTitle, s.startedAtMs, part = nextPart)
        if (created == null) {
            throw java.io.IOException("split part create failed")
        }
        val out = storage.openOutputStream(created.documentUri) ?: throw java.io.IOException("split part open failed")
        s.stream = out
        s.part = nextPart
        s.currentPartBytes = 0
        scope.launch(Dispatchers.IO) {
            dao.updateProgress(s.recordingId, s.bytesTotal.get(), elapsedOf(s), bitrateOf(s), s.part)
        }
    }

    private suspend fun watchdogLoop(s: Session) {
        var dbTick = 0
        while (scope.isActive && !s.finalized.get()) {
            delay(1_000)
            if (s.finalized.get()) break
            val elapsed = elapsedOf(s)
            val size = s.bytesTotal.get()
            val sinceBytes = SystemClock.elapsedRealtime() - s.lastByteAt.get()
            // Byte-flow state machine.
            // The counter goes to its own flow; `phase` is assigned through a StateFlow, which drops
            // an identical value -- so a steady recording now emits ONE phase value, not 3600/hour.
            _elapsedMs.value = elapsed
            when {
                sinceBytes > GIVEUP_MS -> { faultFinalize(s, STREAM_LOST); break }
                sinceBytes > STALL_MS -> _state.value = _state.value.copy(phase = Phase.Reconnecting)
                else -> _state.value = _state.value.copy(phase = Phase.Recording)
            }
            // Persist running totals every ~3s.
            if (++dbTick >= 3) {
                dbTick = 0
                withContext(Dispatchers.IO) { dao.updateProgress(s.recordingId, size, elapsed, bitrateOf(s), s.part) }
                // Free-space guard: auto-stop cleanly before the disk fills so the file finalizes.
                val free = storage.freeBytes(s.treeUri)
                if (free != null && free < DISK_FULL_MARGIN_BYTES) { faultFinalize(s, DISK_FULL); break }
            }
        }
    }

    /** Idempotent finalize: stop the writer, close the stream, persist the terminal row, reset state.
     * Two faults at once (e.g. drive pulled while the watchdog is giving up) collapse to one run. */
    private suspend fun finalize(s: Session, status: RecordingStatus, reason: String?) {
        if (!s.finalized.compareAndSet(false, true)) return
        session = null
        s.running = false
        s.watchdog?.cancel()
        withContext(NonCancellable + Dispatchers.IO) {
            runCatching { s.writerThread?.join(3_000) }
            runCatching { s.stream.flush(); s.stream.close() }
            dao.updateProgress(s.recordingId, s.bytesTotal.get(), elapsedOf(s), bitrateOf(s), s.part)
            dao.finalize(s.recordingId, status, reason, elapsedOf(s))
        }
        _state.value = RecordingState(
            phase = if (reason == null) Phase.Idle else Phase.Stopped,
            stoppedReason = reason,
        )
    }

    private fun elapsedOf(s: Session): Long = SystemClock.elapsedRealtime() - s.startedElapsed
    private fun bitrateOf(s: Session): Long? {
        val elapsed = elapsedOf(s)
        return if (elapsed > 0) s.bytesTotal.get() * 8 * 1000 / elapsed else null
    }

    companion object {
        private const val QUEUE_CAPACITY = 512
        /** Proactive split boundary, safely below the FAT32 4 GB (4_294_967_295) hard ceiling. */
        private const val SPLIT_THRESHOLD_BYTES = 3_900_000_000L
        /** Auto-stop margin so the file finalizes properly before the disk is actually full. */
        private const val DISK_FULL_MARGIN_BYTES = 300L * 1024 * 1024
        /** Assumed bitrate for the free-space pre-flight before real data flows (~8 Mbps SD/HD). */
        private const val PREFLIGHT_BITRATE_BPS = 8_000_000L
        private const val STALL_MS = 20_000L
        private const val GIVEUP_MS = 60_000L

        // Terminal reason sentinels (mapped to localized strings in the UI layer).
        const val STREAM_LOST = "stream lost"
        const val DISK_FULL = "disk full"
        const val DRIVE_REMOVED = "drive removed"
        const val SLOW_DISK = "slow disk"
    }
}
