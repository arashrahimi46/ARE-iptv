package com.arashrahimi46.iptv.data.recording

import android.content.Context
import android.net.Uri
import android.os.StatFs
import android.provider.DocumentsContract
import android.system.Os
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Storage side of Live TV Recording V1: creating the human-findable file layout on a drive, opening
 * append/output streams, drive-identity + free-space checks, pre-flight validation, and deletion.
 *
 * Two drive kinds are handled identically by every method, keyed on the `treeUri` scheme:
 * - **`content://` SAF tree** — a user-picked drive granted via `ACTION_OPEN_DOCUMENT_TREE`. Only
 *   works where the platform ships DocumentsUI (phones, some boxes).
 * - **`file://` app-specific volume** — the app's own dir on a mounted volume (internal / USB / SD),
 *   from [availableVolumes]. Needs no picker and no permission, so it's the default on Android TV,
 *   which ships **no** DocumentsUI (`ACTION_OPEN_DOCUMENT_TREE` there only resolves to a stub that
 *   shows "You don't have an app that can do this"). See docs/recording-v1-design.md §4.
 */
class RecordingStorage(private val context: Context) {

    /** A freshly-created recording file (or split part) on disk. */
    data class CreatedFile(val documentUri: Uri, val documentId: String, val displayName: String)

    /** A picker-free storage destination: the app-specific dir on one mounted volume. */
    data class Volume(val treeUri: Uri, val isInternal: Boolean, val freeBytes: Long?)

    /**
     * Storage destinations available without any file picker: the app-specific external dir on each
     * mounted volume (index 0 = internal/primary, the rest = USB/SD sticks). This is what the TV
     * volume picker offers, since SAF's `ACTION_OPEN_DOCUMENT_TREE` has no handler on Android TV.
     */
    fun availableVolumes(): List<Volume> =
        context.getExternalFilesDirs(null).mapIndexedNotNull { index, dir ->
            dir ?: return@mapIndexedNotNull null
            val uri = Uri.fromFile(dir)
            Volume(treeUri = uri, isInternal = index == 0, freeBytes = freeBytes(uri))
        }

    private fun isFileUri(uri: Uri): Boolean = uri.scheme == "file"

    /** Root [DocumentFile] for [treeUri], resolved for either a SAF tree or a `file://` volume dir. */
    private fun rootDir(treeUri: Uri): DocumentFile? =
        if (isFileUri(treeUri)) treeUri.path?.let { DocumentFile.fromFile(File(it)) }
        else DocumentFile.fromTreeUri(context, treeUri)

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
        val root = rootDir(treeUri)
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
        // File volumes have no SAF document id; the child's own uri string is the stable handle.
        val docId = if (isFileUri(treeUri)) file.uri.toString()
        else runCatching { DocumentsContract.getDocumentId(file.uri) }.getOrNull() ?: return null
        return CreatedFile(file.uri, docId, file.name ?: name)
    }

    /** Output stream for appending capture bytes to a created recording file. */
    fun openOutputStream(documentUri: Uri): OutputStream? =
        if (isFileUri(documentUri)) documentUri.path?.let { runCatching { FileOutputStream(File(it), true) }.getOrNull() }
        else runCatching { context.contentResolver.openOutputStream(documentUri, "wa") }.getOrNull()

    /** Rebuild a document URI from a saved tree + document id (for playback / deletion / probing). */
    fun documentUri(treeUri: Uri, documentId: String): Uri =
        if (isFileUri(treeUri)) Uri.parse(documentId)
        else DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)

    /** True when the recording's file is currently readable (drive mounted + file present). */
    fun documentExists(treeUri: Uri, documentId: String): Boolean =
        if (isFileUri(treeUri)) runCatching { documentUri(treeUri, documentId).path?.let { File(it).exists() } == true }.getOrDefault(false)
        else runCatching { DocumentFile.fromSingleUri(context, documentUri(treeUri, documentId))?.exists() == true }
            .getOrDefault(false)

    /** True when the destination drive itself is currently mounted/readable (vs. the file just gone). */
    fun driveAvailable(treeUri: Uri): Boolean =
        if (isFileUri(treeUri)) runCatching { treeUri.path?.let { File(it).canWrite() } == true }.getOrDefault(false)
        else runCatching { DocumentFile.fromTreeUri(context, treeUri)?.canRead() == true }.getOrDefault(false)

    /** Free bytes on the drive backing [treeUri]. For file volumes via [StatFs]; for SAF via fstatvfs
     * on the tree fd. Null when the provider doesn't support it -- callers treat null as "unknown, proceed". */
    fun freeBytes(treeUri: Uri): Long? {
        if (isFileUri(treeUri)) return treeUri.path?.let { runCatching { StatFs(it).availableBytes }.getOrNull() }
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
        if (isFileUri(treeUri)) fileVolumeId(treeUri)
        else runCatching { DocumentsContract.getTreeDocumentId(treeUri).substringBefore(':').ifBlank { null } }.getOrNull()

    /** Volume id for a `file://` app dir: "primary" for internal (path under /emulated/), else the
     * /storage/<id>/ segment (a USB stick's serial-ish id) -- mirrors SAF's tree-doc-id convention. */
    private fun fileVolumeId(treeUri: Uri): String? {
        val path = treeUri.path ?: return null
        if (path.contains("/emulated/")) return "primary"
        return Regex("/storage/([^/]+)/").find(path)?.groupValues?.getOrNull(1)?.ifBlank { null }
    }

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
        if (isFileUri(treeUri)) runCatching { documentUri(treeUri, documentId).path?.let { File(it).delete() } == true }.getOrDefault(false)
        else runCatching { DocumentFile.fromSingleUri(context, documentUri(treeUri, documentId))?.delete() == true }
            .getOrDefault(false)

    /** Get-or-create `AreIPTV/Recordings/` under the picked tree. */
    private fun recordingsDir(treeUri: Uri): DocumentFile? {
        val root = rootDir(treeUri) ?: return null
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
