package com.arashrahimi46.iptv.ui.streams

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arashrahimi46.iptv.data.db.AppDatabase
import com.arashrahimi46.iptv.data.model.DirectStream
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Backing state for the Streams tab ("Open network stream"). Owns the [DirectStream] history table
 * — the table IS the history — and the small set of actions the screen needs: open/save a pasted
 * URL, rename a box, delete a box. Playback itself reuses the existing player via
 * [com.arashrahimi46.iptv.ui.player.PlaybackSource.DirectStream]; this ViewModel never touches
 * ExoPlayer.
 */
class StreamsViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDatabase.get(app).directStreamDao()

    val streams: StateFlow<List<DirectStream>> =
        dao.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Normalize + save the pasted URL, then hand its history-row id to [onReady] so the screen can
     * navigate to the player. Dedup is by URL: an already-saved URL is bumped to the top (via
     * [com.arashrahimi46.iptv.data.db.DirectStreamDao.touch]) rather than duplicated. Blank/invalid
     * input is ignored (returns without calling [onReady]).
     */
    fun openStream(rawUrl: String, onReady: (Long) -> Unit) {
        val url = normalizeUrl(rawUrl) ?: return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val existing = dao.findByUrl(url)
            val id = if (existing != null) {
                dao.touch(existing.id, now)
                existing.id
            } else {
                dao.insert(DirectStream(url = url, createdAtMs = now, lastPlayedAtMs = now))
            }
            onReady(id)
        }
    }

    /** Rename a history box; blank clears it back to the URL-derived label. */
    fun rename(id: Long, name: String) {
        viewModelScope.launch { dao.rename(id, name.trim().takeIf { it.isNotEmpty() }) }
    }

    fun delete(stream: DirectStream) {
        viewModelScope.launch { dao.delete(stream) }
    }

    companion object {
        /**
         * Accept anything that looks like a URL: trim, and prepend `http://` when no scheme was
         * typed (so `example.com/live.m3u8` works). Returns null only for blank input — anything
         * else is handed to the player, which surfaces its own error if the format is unplayable.
         */
        fun normalizeUrl(raw: String): String? {
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) return null
            return if (trimmed.contains("://")) trimmed else "http://$trimmed"
        }

        fun factory(app: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = StreamsViewModel(app) as T
            }
    }
}
