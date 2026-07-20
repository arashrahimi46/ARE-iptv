package com.arashrahimi46.iptv.ui.sources

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arashrahimi46.iptv.data.model.PlaylistSource
import com.arashrahimi46.iptv.data.repository.PlaylistRepository
import com.arashrahimi46.iptv.data.repository.PlaylistRepositoryImpl
import com.arashrahimi46.iptv.data.settings.UserSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Backs the startup playlist picker ([SelectSourceScreen]). Surfaces every
 * added [PlaylistSource] (the repository already persisted them -- they were
 * just never listed anywhere) and lets the user activate one before entering
 * the app.
 */
class SelectSourceViewModel(app: Application) : AndroidViewModel(app) {
    private val repository: PlaylistRepository = PlaylistRepositoryImpl(app)
    private val settings = UserSettings(app)

    val sources: StateFlow<List<PlaylistSource>> = repository.observeSources()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Marks [id] active (drives every catalog ViewModel) then hands back to the caller to navigate on. */
    fun select(id: Long, onSelected: () -> Unit) {
        viewModelScope.launch {
            settings.setActiveSourceId(id)
            onSelected()
        }
    }

    companion object {
        fun factory(app: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = SelectSourceViewModel(app) as T
            }
    }
}
