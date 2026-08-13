package com.arashrahimi46.iptv.mobile.ui.streams

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arashrahimi46.iptv.mobile.data.db.AppDatabase
import com.arashrahimi46.iptv.mobile.data.model.DirectStream
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Backing state for the touch Streams screen ("Open network stream") -- same [DirectStream]
 * history table and dedup-by-URL logic as :tv's StreamsViewModel. Playback reuses the existing
 * player via [com.arashrahimi46.iptv.mobile.ui.player.PlayerTarget.DirectStream]; this ViewModel
 * never touches ExoPlayer.
 */
class StreamsViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDatabase.get(app).directStreamDao()

    val streams: StateFlow<List<DirectStream>> =
        dao.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Normalize + save the pasted URL, then hand its history-row id to [onReady] so the screen can
     * navigate to the player. Dedup is by URL: an already-saved URL is bumped to the top rather
     * than duplicated. Blank/invalid input is ignored (returns without calling [onReady]). */
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

    fun rename(id: Long, name: String) {
        viewModelScope.launch { dao.rename(id, name.trim().takeIf { it.isNotEmpty() }) }
    }

    fun delete(stream: DirectStream) {
        viewModelScope.launch { dao.delete(stream) }
    }

    companion object {
        fun normalizeUrl(raw: String): String? {
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) return null
            return if (trimmed.contains("://")) trimmed else "http://$trimmed"
        }
    }
}
