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
import kotlinx.coroutines.launch

data class MultiViewUiState(
    val hasSource: Boolean = false,
    /** The channels the user has curated into multi-view, oldest-added first -- never mocked
     * panes, never an auto-filled catalog slice. Empty until the user adds some. */
    val channels: List<Channel> = emptyList(),
    /** Live channels offered in the "+" slot picker (favorites floated first). */
    val pickerCandidates: List<Channel> = emptyList(),
    val paneCount: Int = 4,
    val activeIndex: Int = 0,
) {
    /** Curated channels truncated to [paneCount] -- the panes actually rendered. */
    val panes: List<Channel> get() = channels.take(paneCount)
}

/**
 * MultiView -- a curated, persistent list of live channels the user explicitly adds (from the
 * live player's "add to multi-view" action, or the "+" slot picker here), scoped to the active
 * source and stored via [UserSettings.multiViewChannelIds]. Up to [MAX_CHANNELS] channels; adding
 * a fresh one when full evicts the oldest (FIFO). OK on a pane sets audio; long-press removes.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class MultiViewViewModel(app: Application) : AndroidViewModel(app) {
    private val repository: PlaylistRepository = PlaylistRepositoryImpl(app)
    private val settings = UserSettings(app)
    private val favoritesRepository = FavoritesRepository(app)

    private val _uiState = MutableStateFlow(MultiViewUiState())
    val uiState: StateFlow<MultiViewUiState> = _uiState.asStateFlow()

    /** Latest active source id, held so add/remove can scope their writes without re-querying. */
    private var activeSourceId: Long? = null

    init {
        settings.activeSourceId
            .flatMapLatest { sourceId ->
                activeSourceId = sourceId
                if (sourceId == null) {
                    flowOf(null)
                } else {
                    // The curated id list (persisted, ordered) resolved to rows, alongside a
                    // bounded picker of live channels (favorites first) for the "+" slots.
                    combine(
                        settings.multiViewChannelIds(sourceId),
                        repository.topChannels(sourceId, PICKER_LIMIT),
                        favoritesRepository.favoriteChannelIds,
                    ) { ids, top, favoriteIds ->
                        val byId = repository.channelsByIds(ids).associateBy { it.id }
                        val curated = ids.mapNotNull { byId[it] }        // preserve insertion order
                        val picker = top.sortedByDescending { it.id in favoriteIds }
                        curated to picker
                    }
                }
            }
            .onEach { resolved ->
                val previous = _uiState.value
                _uiState.value = if (resolved == null) {
                    MultiViewUiState(hasSource = false, paneCount = previous.paneCount)
                } else {
                    val (curated, picker) = resolved
                    previous.copy(
                        hasSource = true,
                        channels = curated,
                        pickerCandidates = picker,
                        activeIndex = previous.activeIndex.coerceIn(0, (previous.paneCount - 1).coerceAtLeast(0)),
                    )
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

    fun addChannel(channel: Channel) {
        val sourceId = activeSourceId ?: return
        viewModelScope.launch { settings.addMultiViewChannel(sourceId, channel.id, MAX_CHANNELS) }
    }

    fun removeChannel(channelId: Long) {
        val sourceId = activeSourceId ?: return
        viewModelScope.launch { settings.removeMultiViewChannel(sourceId, channelId) }
    }

    companion object {
        /** Hard cap on curated channels == the densest layout (4-up); a full list evicts FIFO. */
        const val MAX_CHANNELS = 4

        /** Bounded "+" slot picker size -- a full 100k+ channel list would OOM and is pointless
         * to scroll here; the picker is for quickly grabbing a favourite/top live channel. */
        private const val PICKER_LIMIT = 500

        fun factory(app: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = MultiViewViewModel(app) as T
            }
    }
}
