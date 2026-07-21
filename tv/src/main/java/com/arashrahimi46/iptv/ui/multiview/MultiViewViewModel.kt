package com.arashrahimi46.iptv.ui.multiview

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arashrahimi46.iptv.data.model.Channel
import com.arashrahimi46.iptv.data.repository.FavoritesRepository
import com.arashrahimi46.iptv.data.repository.PlaylistRepository
import com.arashrahimi46.iptv.data.repository.PlaylistRepositoryImpl
import com.arashrahimi46.iptv.data.settings.UserSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

data class MultiViewUiState(
    val hasSource: Boolean = false,
    /** Real [Channel] rows, favorited channels first, from Room -- never mocked panes. */
    val channels: List<Channel> = emptyList(),
    val paneCount: Int = 4,
    val activeIndex: Int = 0,
) {
    /** The panes actually rendered -- [channels] truncated to [paneCount], clamping [activeIndex] within range. */
    val panes: List<Channel> get() = channels.take(paneCount)
}

/**
 * Phase 5 -- MultiView.jsx: 2-up/4-up simultaneous live streams, one active
 * (audio) pane. Sources real [Channel] rows for the active playlist (Room via
 * [PlaylistRepository]), favorited channels first since those are the ones a
 * user is most likely to want to watch side-by-side, padded with the rest of
 * the live catalog -- never the design source's hardcoded mock panes.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class MultiViewViewModel(app: Application) : AndroidViewModel(app) {
    private val repository: PlaylistRepository = PlaylistRepositoryImpl(app)
    private val settings = UserSettings(app)
    private val favoritesRepository = FavoritesRepository(app)

    private val _uiState = MutableStateFlow(MultiViewUiState())
    val uiState: StateFlow<MultiViewUiState> = _uiState.asStateFlow()

    init {
        settings.activeSourceId
            .flatMapLatest { sourceId ->
                if (sourceId == null) {
                    flowOf<List<Channel>?>(null)
                } else {
                    // Bounded channel picker (favorites floated to the top) -- never the whole
                    // catalog, which OOM'd on large sources.
                    combine(repository.topChannels(sourceId, MULTIVIEW_CHANNEL_LIMIT), favoritesRepository.favoriteChannelIds) { channels, favoriteIds ->
                        channels.sortedByDescending { it.id in favoriteIds }
                    }
                }
            }
            .onEach { channels ->
                val previous = _uiState.value
                _uiState.value = if (channels == null) {
                    MultiViewUiState(hasSource = false)
                } else {
                    previous.copy(hasSource = true, channels = channels, activeIndex = previous.activeIndex.coerceIn(0, (previous.paneCount - 1).coerceAtLeast(0)))
                }
            }
            .launchIn(viewModelScope)
    }

    fun setPaneCount(count: Int) {
        _uiState.value = _uiState.value.copy(paneCount = count, activeIndex = _uiState.value.activeIndex.coerceIn(0, count - 1))
    }

    fun setActive(index: Int) {
        _uiState.value = _uiState.value.copy(activeIndex = index)
    }

    companion object {
        /** Bounded picker size -- multi-view only ever shows a handful of panes; a full 100k+
         * channel list would OOM and is pointless to scroll here. */
        private const val MULTIVIEW_CHANNEL_LIMIT = 500

        fun factory(app: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = MultiViewViewModel(app) as T
            }
    }
}
