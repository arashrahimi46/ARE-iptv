package com.arashrahimi46.iptv.mobile.ui.sources

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arashrahimi46.iptv.mobile.data.model.PlaylistSource
import com.arashrahimi46.iptv.mobile.data.parser.MAX_PLAYLISTS
import com.arashrahimi46.iptv.mobile.data.repository.PlaylistRepository
import com.arashrahimi46.iptv.mobile.data.repository.PlaylistRepositoryImpl
import com.arashrahimi46.iptv.mobile.data.settings.UserSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Backs [SourcesScreen]. Same repository/settings calls as :tv's `SelectSourceViewModel` -- the
 * playlists were always persisted and switchable, the phone app just never listed them, so a user
 * was locked to whatever they onboarded with.
 *
 * Nothing here invalidates or reloads a catalog: every catalog view-model reads
 * [UserSettings.activeSourceId] as a Flow, so writing the pointer is the whole switch.
 */
class SourcesViewModel(app: Application) : AndroidViewModel(app) {
    private val repository: PlaylistRepository = PlaylistRepositoryImpl(app)
    private val settings = UserSettings(app)

    val sources: StateFlow<List<PlaylistSource>> = repository.observeSources()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val activeSourceId: StateFlow<Long?> = settings.activeSourceId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** The cap counts every playlist, Explore-added or hand-typed, exactly as :tv's Explore does. */
    val isAtCap: StateFlow<Boolean> = repository.observeSources()
        .map { it.size >= MAX_PLAYLISTS }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** Marks [id] active; every catalog screen re-collects off the pointer on its own. */
    fun select(id: Long) {
        viewModelScope.launch { settings.setActiveSourceId(id) }
    }

    /** Renames a playlist; [sources] refreshes itself via its Flow. Blank names are ignored. */
    fun rename(id: Long, name: String) {
        viewModelScope.launch { repository.renameSource(id, name) }
    }

    /**
     * Deletes a playlist and everything derived from it (see [PlaylistRepository.deleteSource]).
     *
     * The repository clears the active pointer when the deleted playlist was the active one. On :tv
     * that is harmless -- the startup picker is the next thing the user sees. The phone app has no
     * such gate, so a cleared pointer would leave Home/Live/Guide silently empty with no visible
     * cause, which reads as a broken app rather than a deleted playlist. Fall forward to the newest
     * remaining playlist; only a genuinely empty list leaves the pointer unset, and then the screen
     * the user is already on is the one that offers Add.
     */
    fun delete(id: Long) {
        viewModelScope.launch {
            repository.deleteSource(id)
            if (settings.activeSourceId.first() == null) {
                repository.observeSources().first()
                    .maxByOrNull { it.createdAtMs }
                    ?.let { settings.setActiveSourceId(it.id) }
            }
        }
    }
}
