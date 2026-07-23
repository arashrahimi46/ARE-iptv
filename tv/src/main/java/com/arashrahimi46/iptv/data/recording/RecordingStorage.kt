package com.arashrahimi46.iptv.data.recording

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.system.Os
import androidx.documentfile.provider.DocumentFile
import java.io.OutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * SAF (Storage Access Framework) side of Live TV Recording V1: creating the human-findable file
 * layout on any user-picked drive, opening append/output streams, drive-identity + free-space
 * checks, pre-flight validation, and deletion. Everything is keyed by a persistable-permission
 * `treeUri` and SAF document ids -- never a `File` path -- so internal storage and any external USB
 * drive are handled identically. See docs/recording-v1-design.md §4.
 */
class RecordingStorage(private val context: Context) {

    /** A freshly-created recording file (or split part) on disk. */
    data class CreatedFile(val documentUri: Uri, val documentId: String, val displayName: String)

    /** Pre-flight outcome (design §6): either OK, or a terminal reason the REC dot never flips for. */
    sealed class Preflight {
        data object Ok : Preflight()
        data class Failed(val reason: Reason) : Preflight()
        enum class Reason { NOT_WRITABLE, LOW_SPACE, HLS_STREAM }
    }

    /**
     * Validates the destination before capture begins. Order matters: HLS is rejected outright (V1
     * records progressive `.ts` only), then a real test-write catches read-only / revoked SAF
     * permission, then free space vs. an estimated bitrate is sanity-checked.
     */
    fun preflight(treeUri: Uri, isHls: Boolean, estimatedBitrateBps: Long): Preflight {
        if (isHls) return Preflight.Failed(Preflight.Reason.HLS_STREAM)
        val root = DocumentFile.fromTreeUri(context, treeUri)
        if (root == null || !root.canWrite()) return Preflight.Failed(Preflight.Reason.NOT_WRITABLE)
        // Test-write: create then delete a probe file. Catches providers that report canWrite() but
        // reject actual writes (some read-only mounts / revoked grants).
        val probe = runCatching { root.createFile(MIME_TS, ".are_rec_probe") }.getOrNull()
        if (probe == null) return Preflight.Failed(Preflight.Reason.NOT_WRITABLE)
        val wrote = runCatching {
            context.contentResolver.openOutputStream(probe.uri)?.use { it.write(0) } != null
        }.getOrDefault(false)
        probe.delete()
        if (!wrote) return Preflight.Failed(Preflight.Reason.NOT_WRITABLE)
        // Free space vs. ~10 minutes at the estimated bitrate -- a soft "tight" guard, not exact.
        val free = freeBytes(treeUri)
        if (free != null && estimatedBitrateBps > 0) {
            val tenMinutesBytes = estimatedBitrateBps / 8 * 600
            if (free < tenMinutesBytes.coerceAtLeast(LOW_SPACE_FLOOR_BYTES)) {
                return Preflight.Failed(Preflight.Reason.LOW_SPACE)
            }
        }
        return Preflight.Ok
    }

    /**
     * Creates `AreIPTV/Recordings/<Channel> — <Program> — <yyyy-MM-dd>[ · part-NNN].ts` under [treeUri]
     * and returns its document handle. [part] > 0 appends the `part-NNN` suffix for split recordings.
     * Filenames are FAT-sanitized; the SAF provider auto-suffixes on collision.
     */
    fun createRecordingFile(
        treeUri: Uri,
        channelName: String,
        programTitle: String?,
        startedAtMs: Long,
        part: Int = 0,
    ): CreatedFile? {
        val dir = recordingsDir(treeUri) ?: return null
        val name = buildDisplayName(channelName, programTitle, startedAtMs, part)
        val file = runCatching { dir.createFile(MIME_TS, name) }.getOrNull() ?: return null
        val docId = runCatching { DocumentsContract.getDocumentId(file.uri) }.getOrNull() ?: return null
        return CreatedFile(file.uri, docId, file.name ?: name)
    }

    /** Output stream for appending capture bytes to a created recording file. */
    fun openOutputStream(documentUri: Uri): OutputStream? =
        runCatching { context.contentResolver.openOutputStream(documentUri, "wa") }.getOrNull()

    /** Rebuild a document URI from a saved tree + document id (for playback / deletion / probing). */
    fun documentUri(treeUri: Uri, documentId: String): Uri =
        DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)

    /** True when the recording's file is currently readable (drive mounted + file present). */
    fun documentExists(treeUri: Uri, documentId: String): Boolean =
        runCatching { DocumentFile.fromSingleUri(context, documentUri(treeUri, documentId))?.exists() == true }
            .getOrDefault(false)

    /** True when the destination drive itself is currently mounted/readable (vs. the file just gone). */
    fun driveAvailable(treeUri: Uri): Boolean =
        runCatching { DocumentFile.fromTreeUri(context, treeUri)?.canRead() == true }.getOrDefault(false)

    /** Free bytes on the drive backing [treeUri], via fstatvfs on the tree fd. Null when the provider
     * doesn't support it (some cloud/network providers) -- callers treat null as "unknown, proceed". */
    fun freeBytes(treeUri: Uri): Long? {
        val treeDocId = runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull() ?: return null
        val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, treeDocId)
        return runCatching {
            context.contentResolver.openFileDescriptor(docUri, "r")?.use { pfd ->
                val stat = Os.fstatvfs(pfd.fileDescriptor)
                stat.f_bavail * stat.f_bsize
            }
        }.getOrNull()
    }

    /**
     * Volume UUID of the drive backing [treeUri] -- the part of the tree document id before ':'
     * (e.g. "1A2B-3C4D" for a USB stick, "primary" for internal). Used to detect "same USB port,
     * different stick" before trusting a saved treeUri, and to resolve UNAVAILABLE on remount.
     */
    fun volumeUuid(treeUri: Uri): String? =
        runCatching { DocumentsContract.getTreeDocumentId(treeUri).substringBefore(':').ifBlank { null } }.getOrNull()

    /** Human-readable drive label for the "location badge" (e.g. "SanDisk USB" / "Internal storage").
     * Best-effort: providers rarely expose a friendly name, so fall back to the volume UUID. */
    fun driveLabel(treeUri: Uri): String? {
        val uuid = volumeUuid(treeUri)
        return when {
            uuid == null -> null
            uuid.equals("primary", ignoreCase = true) -> "Internal storage"
            else -> "USB · $uuid"
        }
    }

    /** Delete a recording's file (or first part). Returns whether it succeeded (false when the drive
     * is gone -- the caller then queues the delete for remount, per design §6). */
    fun deleteDocument(treeUri: Uri, documentId: String): Boolean =
        runCatching { DocumentFile.fromSingleUri(context, documentUri(treeUri, documentId))?.delete() == true }
            .getOrDefault(false)

    /** Get-or-create `AreIPTV/Recordings/` under the picked tree. */
    private fun recordingsDir(treeUri: Uri): DocumentFile? {
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return null
        val app = root.findFile(APP_DIR)?.takeIf { it.isDirectory } ?: root.createDirectory(APP_DIR) ?: return null
        return app.findFile(REC_DIR)?.takeIf { it.isDirectory } ?: app.createDirectory(REC_DIR)
    }

    private fun buildDisplayName(channelName: String, programTitle: String?, startedAtMs: Long, part: Int): String {
        val date = DATE_FORMAT.format(Instant.ofEpochMilli(startedAtMs).atZone(ZoneId.systemDefault()))
        val base = buildString {
            append(sanitize(channelName))
            if (!programTitle.isNullOrBlank()) append(" — ").append(sanitize(programTitle))
            append(" — ").append(date)
            if (part > 0) append(" · part-%03d".format(part))
        }
        // SAF derives the .ts extension from MIME_TS; keep the base within a FAT-safe length.
        return base.take(MAX_NAME_LEN)
    }

    companion object {
        const val MIME_TS = "video/mp2t"
        private const val APP_DIR = "AreIPTV"
        private const val REC_DIR = "Recordings"
        private const val MAX_NAME_LEN = 120
        /** Absolute free-space floor for pre-flight even when the bitrate estimate is tiny. */
        private const val LOW_SPACE_FLOOR_BYTES = 500L * 1024 * 1024
        private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        /** Strip/replace FAT-illegal characters and collapse whitespace for a findable filename. */
        fun sanitize(raw: String): String =
            raw.replace(Regex("""[:/\\&<>|?*"']"""), " ")
                .replace(Regex("""\s+"""), " ")
                .trim()
                .ifBlank { "Recording" }
    }
}
