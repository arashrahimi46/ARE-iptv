package com.arashrahimi46.iptv.ui.live

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.arashrahimi46.iptv.data.model.Channel
import com.arashrahimi46.iptv.data.repository.FavoritesRepository
import com.arashrahimi46.iptv.data.repository.PlaylistRepository
import com.arashrahimi46.iptv.data.repository.PlaylistRepositoryImpl
import com.arashrahimi46.iptv.data.settings.UserSettings
import com.arashrahimi46.iptv.ui.browse.browsePager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LiveCategorySummary(val name: String, val count: Int, val pinned: Boolean = false)

data class LiveUiState(
    val hasSource: Boolean = false,
    val categories: List<LiveCategorySummary> = emptyList(),
    val selectedCategoryIndex: Int = 0,
) {
    /** Total count for the currently-selected group (category 0 = "All channels"). */
    val selectedCount: Int get() = categories.getOrNull(selectedCategoryIndex)?.count ?: 0
}

/**
 * Live TV browse state: real [Channel] catalog for the active source.
 *
 * Large-catalog safe: the grid is a Paging 3 [PagingData] stream so only the visible
 * window is in memory (100k+ channels), and the channel-group column comes from a
 * `GROUP BY` query rather than from the fully-loaded list (which OOM'd at this scale).
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class LiveViewModel(app: Application) : AndroidViewModel(app) {
    private val repository: PlaylistRepository = PlaylistRepositoryImpl(app)
    private val settings = UserSettings(app)
    private val favoritesRepository = FavoritesRepository(app)

    val favoriteChannelIds: StateFlow<Set<Long>> = favoritesRepository.favoriteChannelIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    /** null = the "All channels" pseudo-category. Tracked by name so it survives the round
     * trip through the player screen and a rebuilt (possibly reordered) category list. */
    private val selectedCategoryName = MutableStateFlow<String?>(null)

    val uiState: StateFlow<LiveUiState> = settings.activeSourceId
        .flatMapLatest { sourceId ->
            if (sourceId == null) {
                flowOf(LiveUiState(hasSource = false))
            } else {
                combine(
                    repository.channelCategoryCounts(sourceId),
                    repository.channelCount(sourceId),
                    selectedCategoryName,
                    settings.pinnedCategories(PIN_NAMESPACE),
                ) { counts, total, selectedName, pinned ->
                    // Pinned groups float to the top (alphabetical), then the rest in catalog order,
                    // with "All channels" always first (not pinnable).
                    val (pinnedCats, others) = counts
                        .map { LiveCategorySummary(it.name, it.count, it.name in pinned) }
                        .partition { it.pinned }
                    val categories = listOf(LiveCategorySummary("All channels", total)) +
                        pinnedCats.sortedBy { it.name.lowercase() } + others
                    val index = if (selectedName == null) 0
                        else categories.indexOfFirst { it.name == selectedName }.let { if (it >= 0) it else 0 }
                    LiveUiState(hasSource = true, categories = categories, selectedCategoryIndex = index)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LiveUiState())

    val pagingData: Flow<PagingData<Channel>> = combine(
        settings.activeSourceId,
        selectedCategoryName,
    ) { sourceId, category -> sourceId to category }
        .flatMapLatest { (sourceId, category) ->
            if (sourceId == null) flowOf(PagingData.empty())
            else browsePager { repository.pagingChannels(sourceId, category) }.flow
        }
        .cachedIn(viewModelScope)

    fun selectCategory(index: Int) {
        selectedCategoryName.value = if (index == 0) null else uiState.value.categories.getOrNull(index)?.name
    }

    /** Pin/unpin the category at [index] (index 0 = "All channels" is not pinnable). */
    fun togglePin(index: Int) {
        if (index == 0) return
        val name = uiState.value.categories.getOrNull(index)?.name ?: return
        viewModelScope.launch { settings.togglePinnedCategory(PIN_NAMESPACE, name) }
    }

    fun toggleFavorite(channelId: Long) {
        viewModelScope.launch { favoritesRepository.toggleChannel(channelId) }
    }

    companion object {
        private const val PIN_NAMESPACE = "live"

        fun factory(app: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = LiveViewModel(app) as T
            }
    }
}
