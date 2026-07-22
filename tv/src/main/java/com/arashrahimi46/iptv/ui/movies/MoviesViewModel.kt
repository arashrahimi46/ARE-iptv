package com.arashrahimi46.iptv.ui.movies

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.arashrahimi46.iptv.data.model.ContentType
import com.arashrahimi46.iptv.data.model.VodTitle
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

data class MoviesCategorySummary(val name: String, val count: Int, val pinned: Boolean = false)

data class MoviesUiState(
    val hasSource: Boolean = false,
    val categories: List<MoviesCategorySummary> = emptyList(),
    val selectedCategoryIndex: Int = 0,
) {
    /** Total count for the currently-selected category (category 0 = "All movies"). */
    val selectedCount: Int get() = categories.getOrNull(selectedCategoryIndex)?.count ?: 0
}

/**
 * Movies browse state: real [VodTitle] catalog (isSeries = false) for the active source.
 *
 * Large-catalog safe: the grid is a Paging 3 [PagingData] stream ([pagingData]) so only
 * the visible window is in memory (33k+ movies), and the category column comes from a
 * `GROUP BY` query ([uiState]) rather than from the fully-loaded list.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class MoviesViewModel(app: Application) : AndroidViewModel(app) {
    private val repository: PlaylistRepository = PlaylistRepositoryImpl(app)
    private val settings = UserSettings(app)
    private val favoritesRepository = FavoritesRepository(app)

    val favoriteVodIds: StateFlow<Set<Long>> = favoritesRepository.favoriteVodIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    /** null = the "All movies" pseudo-category (no category filter). */
    private val selectedCategoryName = MutableStateFlow<String?>(null)

    val uiState: StateFlow<MoviesUiState> = settings.activeSourceId
        .flatMapLatest { sourceId ->
            if (sourceId == null) {
                flowOf(MoviesUiState(hasSource = false))
            } else {
                combine(
                    repository.movieCategoryCounts(sourceId),
                    repository.movieCount(sourceId),
                    selectedCategoryName,
                    settings.pinnedCategories(PIN_NAMESPACE),
                ) { counts, total, selectedName, pinned ->
                    // Pinned genres float to the top (alphabetical); "All movies" is always first.
                    val (pinnedCats, others) = counts
                        .map { MoviesCategorySummary(it.name, it.count, it.name in pinned) }
                        .partition { it.pinned }
                    val categories = listOf(MoviesCategorySummary("All movies", total)) +
                        pinnedCats.sortedBy { it.name.lowercase() } + others
                    val index = if (selectedName == null) 0
                        else categories.indexOfFirst { it.name == selectedName }.let { if (it >= 0) it else 0 }
                    MoviesUiState(hasSource = true, categories = categories, selectedCategoryIndex = index)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MoviesUiState())

    val pagingData: Flow<PagingData<VodTitle>> = combine(
        settings.activeSourceId,
        selectedCategoryName,
    ) { sourceId, category -> sourceId to category }
        .flatMapLatest { (sourceId, category) ->
            if (sourceId == null) flowOf(PagingData.empty())
            else browsePager { repository.pagingMovies(sourceId, category) }.flow
        }
        .cachedIn(viewModelScope)

    fun selectCategory(index: Int) {
        selectedCategoryName.value = if (index == 0) null else uiState.value.categories.getOrNull(index)?.name
    }

    /** Pin/unpin the genre at [index] (index 0 = "All movies" is not pinnable). */
    fun togglePin(index: Int) {
        if (index == 0) return
        val name = uiState.value.categories.getOrNull(index)?.name ?: return
        viewModelScope.launch { settings.togglePinnedCategory(PIN_NAMESPACE, name) }
    }

    fun toggleFavorite(vodTitleId: Long) {
        viewModelScope.launch { favoritesRepository.toggleVod(vodTitleId, ContentType.MOVIE) }
    }

    companion object {
        private const val PIN_NAMESPACE = "movies"

        fun factory(app: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = MoviesViewModel(app) as T
            }
    }
}
