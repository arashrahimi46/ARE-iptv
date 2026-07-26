package com.arashrahimi46.iptv.ui.explore

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arashrahimi46.iptv.data.parser.ExploreCatalog
import com.arashrahimi46.iptv.data.parser.ExploreEntry
import com.arashrahimi46.iptv.data.parser.MAX_PLAYLISTS
import com.arashrahimi46.iptv.data.repository.PlaylistRepository
import com.arashrahimi46.iptv.data.repository.PlaylistRepositoryImpl
import com.arashrahimi46.iptv.data.settings.UserSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** What the Explore screen is doing right now. */
sealed interface ExploreStatus {
    data object Idle : ExploreStatus
    /** An import is running; the UI blocks further adds and shows progress. */
    data class Adding(val entry: ExploreEntry) : ExploreStatus
    data class Failed(val entry: ExploreEntry) : ExploreStatus
}

/**
 * Backs the curated Explore screen. Adding an entry is deliberately just an M3U import with an
 * [ExploreEntry.id] recorded as its origin -- it reuses the whole existing add/parse/play pipeline
 * rather than introducing a parallel one.
 */
class ExploreViewModel(app: Application) : AndroidViewModel(app) {
    private val repository: PlaylistRepository = PlaylistRepositoryImpl(app)
    private val settings = UserSettings(app)

    private val _entries = MutableStateFlow<List<ExploreEntry>>(emptyList())
    val entries: StateFlow<List<ExploreEntry>> = _entries.asStateFlow()

    private val _status = MutableStateFlow<ExploreStatus>(ExploreStatus.Idle)
    val status: StateFlow<ExploreStatus> = _status.asStateFlow()

    /** How many playlists exist in total -- the cap counts every playlist, not just Explore ones. */
    val playlistCount: StateFlow<Int> = repository.observeSources()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val isAtCap: StateFlow<Boolean> = playlistCount
        .map { it >= MAX_PLAYLISTS }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    init {
        viewModelScope.launch { _entries.value = ExploreCatalog.load(getApplication()) }
    }

    /**
     * Imports [entry] as a normal M3U playlist called [name] and activates it, then hands the new id
     * back so the caller can navigate on. On failure the status carries the entry so the message can
     * name what failed -- a dead curated URL should read as "this playlist is broken", not "your TV is".
     */
    fun add(entry: ExploreEntry, name: String, onAdded: (Long) -> Unit) {
        if (_status.value is ExploreStatus.Adding) return
        _status.value = ExploreStatus.Adding(entry)
        viewModelScope.launch {
            runCatching {
                repository.addM3uSource(name.trim(), entry.url, epgUrl = null, origin = entry.id)
                // The import returns counts, not the row id, so read it back: newest row for this
                // origin. maxBy(createdAtMs) rather than first() because adding the same entry twice
                // is allowed, and the one just created is the one to open.
                repository.observeSources().first()
                    .filter { it.origin == entry.id }
                    .maxByOrNull { it.createdAtMs }
                    ?.id
            }
                .onSuccess { id ->
                    _status.value = ExploreStatus.Idle
                    if (id != null) {
                        settings.setActiveSourceId(id)
                        onAdded(id)
                    }
                }
                .onFailure { _status.value = ExploreStatus.Failed(entry) }
        }
    }

    fun dismissError() { _status.value = ExploreStatus.Idle }

    companion object {
        fun factory(app: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = ExploreViewModel(app) as T
            }
    }
}
