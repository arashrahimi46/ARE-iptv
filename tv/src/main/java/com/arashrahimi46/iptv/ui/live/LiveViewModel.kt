package com.arashrahimi46.iptv.ui.live

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arashrahimi46.iptv.data.model.Channel
import com.arashrahimi46.iptv.data.repository.PlaylistRepository
import com.arashrahimi46.iptv.data.repository.PlaylistRepositoryImpl
import com.arashrahimi46.iptv.data.settings.UserSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

data class LiveCategorySummary(val name: String, val count: Int)

data class LiveUiState(
    val hasSource: Boolean = false,
    val allChannels: List<Channel> = emptyList(),
    val categories: List<LiveCategorySummary> = emptyList(),
    val selectedCategoryIndex: Int = 0,
) {
    /** "All channels" is always category 0; anything else filters by [Channel.categoryName]. */
    val visibleChannels: List<Channel>
        get() = if (selectedCategoryIndex == 0 || categories.isEmpty()) {
            allChannels
        } else {
            val name = categories.getOrNull(selectedCategoryIndex)?.name
            if (name == null) allChannels else allChannels.filter { it.categoryName == name }
        }
}

/**
 * Live TV browse state: real [Channel] catalog for the active source (Room
 * via [PlaylistRepository]), grouped into an "All channels" pseudo-category
 * plus one row per real [com.arashrahimi46.iptv.data.model.Category] (contentType = LIVE).
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class LiveViewModel(app: Application) : AndroidViewModel(app) {
    private val repository: PlaylistRepository = PlaylistRepositoryImpl(app)
    private val settings = UserSettings(app)

    private val _uiState = MutableStateFlow(LiveUiState())
    val uiState: StateFlow<LiveUiState> = _uiState.asStateFlow()

    init {
        settings.activeSourceId
            .flatMapLatest { sourceId ->
                if (sourceId == null) flowOf<List<Channel>?>(null) else repository.observeChannels(sourceId)
            }
            .onEach { channels ->
                _uiState.value = if (channels == null) LiveUiState(hasSource = false) else buildState(channels)
            }
            .launchIn(viewModelScope)
    }

    private fun buildState(channels: List<Channel>): LiveUiState {
        val counts = linkedMapOf<String, Int>()
        channels.forEach { channel ->
            channel.categoryName?.let { counts[it] = (counts[it] ?: 0) + 1 }
        }
        val categories = listOf(LiveCategorySummary("All channels", channels.size)) +
            counts.map { (name, count) -> LiveCategorySummary(name, count) }
        val previousSelection = _uiState.value.selectedCategoryIndex.coerceIn(0, categories.lastIndex.coerceAtLeast(0))
        return LiveUiState(
            hasSource = true,
            allChannels = channels,
            categories = categories,
            selectedCategoryIndex = previousSelection,
        )
    }

    fun selectCategory(index: Int) {
        _uiState.value = _uiState.value.copy(selectedCategoryIndex = index)
    }

    companion object {
        fun factory(app: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = LiveViewModel(app) as T
            }
    }
}
