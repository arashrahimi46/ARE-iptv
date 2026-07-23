package com.arashrahimi46.iptv.ui.recordings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arashrahimi46.iptv.R
import com.arashrahimi46.iptv.data.model.Recording
import com.arashrahimi46.iptv.data.model.RecordingStatus
import com.arashrahimi46.iptv.data.recording.RecordingStorage
import com.arashrahimi46.iptv.data.recording.RecordingSupervisor
import com.arashrahimi46.iptv.data.repository.RecordingRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** Which grouped section of the Recordings tab a row renders under (design §7). */
enum class RecordingGroup { RECORDING_NOW, COMPLETED, INTERRUPTED, UNAVAILABLE }

/** A recording plus everything the row needs pre-formatted off the main thread. */
data class RecordingRow(
    val recording: Recording,
    val title: String,
    val subtitle: String?,
    val metaLabel: String,
    val locationLabel: String?,
    val statusLabel: String?,
    val reconnectMessage: String?,
    val group: RecordingGroup,
    val playable: Boolean,
)

class RecordingsViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = RecordingRepository(app)
    private val storage = RecordingStorage(app)

    val rows: StateFlow<List<RecordingRow>> =
        repository.observeRecordings()
            .map { list -> list.map { it.toRow() } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun delete(recording: Recording) {
        viewModelScope.launch { repository.delete(recording) }
    }

    private fun Recording.toRow(): RecordingRow {
        val ctx = getApplication<Application>()
        val group = when (status) {
            RecordingStatus.RECORDING -> RecordingGroup.RECORDING_NOW
            RecordingStatus.COMPLETED -> RecordingGroup.COMPLETED
            RecordingStatus.INTERRUPTED, RecordingStatus.FAILED -> RecordingGroup.INTERRUPTED
            RecordingStatus.UNAVAILABLE, RecordingStatus.MISSING -> RecordingGroup.UNAVAILABLE
        }
        val treeUri = runCatching { Uri.parse(storageTreeUri) }.getOrNull()
        val location = treeUri?.let { storage.driveLabel(it) }
        val date = DATE_FORMAT.format(Instant.ofEpochMilli(startedAtMs).atZone(ZoneId.systemDefault()))
        val durationText = durationMs?.takeIf { it > 0 }?.let { formatDuration(it) }?.let { d ->
            if (status == RecordingStatus.INTERRUPTED) ctx.getString(R.string.recordings_duration_approx, d) else d
        }
        val sizeText = formatSize(sizeBytes)
        val partsText = if (parts > 1) ctx.getString(R.string.recordings_parts, parts) else null
        val metaLabel = listOfNotNull(date, durationText, sizeText, location, partsText).joinToString("  ·  ")
        val statusLabel = when (status) {
            RecordingStatus.INTERRUPTED -> statusReason?.let { localizeReason(ctx, it) } ?: ctx.getString(R.string.recordings_status_interrupted)
            RecordingStatus.FAILED -> ctx.getString(R.string.recordings_status_failed)
            RecordingStatus.MISSING -> ctx.getString(R.string.recordings_missing)
            else -> null
        }
        val reconnectMessage = if (status == RecordingStatus.UNAVAILABLE) {
            location?.let { ctx.getString(R.string.recordings_unavailable_reconnect, it) }
                ?: ctx.getString(R.string.recordings_unavailable_generic)
        } else {
            null
        }
        return RecordingRow(
            recording = this,
            title = channelName,
            subtitle = programTitle,
            metaLabel = metaLabel,
            locationLabel = location,
            statusLabel = statusLabel,
            reconnectMessage = reconnectMessage,
            group = group,
            playable = status == RecordingStatus.COMPLETED || status == RecordingStatus.INTERRUPTED,
        )
    }

    companion object {
        private val DATE_FORMAT: DateTimeFormatter =
            DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault())

        /** Maps the supervisor's English reason sentinels to localized strings. */
        fun localizeReason(ctx: Application, reason: String): String = when (reason) {
            RecordingSupervisor.STREAM_LOST -> ctx.getString(R.string.recording_reason_stream_lost)
            RecordingSupervisor.DISK_FULL -> ctx.getString(R.string.recording_reason_disk_full)
            RecordingSupervisor.DRIVE_REMOVED -> ctx.getString(R.string.recording_reason_drive_removed)
            RecordingSupervisor.SLOW_DISK -> ctx.getString(R.string.recording_reason_slow_disk)
            else -> reason
        }

        private fun formatDuration(ms: Long): String {
            val totalSeconds = ms / 1000
            val h = totalSeconds / 3600
            val m = (totalSeconds % 3600) / 60
            val s = totalSeconds % 60
            return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
        }

        private fun formatSize(bytes: Long): String {
            if (bytes <= 0) return "0 MB"
            val mb = bytes / (1024.0 * 1024.0)
            return if (mb >= 1024) "%.1f GB".format(mb / 1024) else "%.0f MB".format(mb)
        }

        fun factory(app: Application): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = RecordingsViewModel(app) as T
        }
    }
}
