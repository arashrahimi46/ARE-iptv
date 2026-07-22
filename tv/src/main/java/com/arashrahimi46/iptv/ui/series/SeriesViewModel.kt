package com.arashrahimi46.iptv.ui.series

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.arashrahimi46.iptv.R
import com.arashrahimi46.iptv.data.model.ContentType
import com.arashrahimi46.iptv.data.model.VodTitle
import com.arashrahimi46.iptv.data.repository.FavoritesRepository
import com.arashrahimi46.iptv.data.repository.PlaylistRepository
import com.arashrahimi46.iptv.data.repository.PlaylistRepositoryImpl
import com.arashrahimi46.iptv.data.settings.AdultContentFilter
import com.arashrahimi46.iptv.data.settings.UserSettings
import com.arashrahimi46.iptv.ui.browse.browsePager
import androidx.paging.filter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SeriesCategorySummary(val name: String, val count: Int, val pinned: Boolean = false)

data class SeriesUiState(
    val hasSource: Boolean = false,
    val categories: List<SeriesCategorySummary> = emptyList(),
    val selectedCategoryIndex: Int = 0,
) {
    /** Total count for the currently-selected category (category 0 = "All series"). */
    val selectedCount: Int get() = categories.getOrNull(selectedCategoryIndex)?.count ?: 0
}

/**
 * Series browse state: real [VodTitle] catalog (isSeries = true) for the active source.
 *
 * Large-catalog safe (the same way [com.arashrahimi46.iptv.ui.movies.MoviesViewModel] is):
 * the grid is a Paging 3 [PagingData] stream so only the visible window is in memory
 * (343k+ series), and the category column comes from a `GROUP BY` query.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SeriesViewModel(app: Application) : AndroidViewModel(app) {
    private val repository: PlaylistRepository = PlaylistRepositoryImpl(app)
    private val settings = UserSettings(app)
    private val favoritesRepository = FavoritesRepository(app)

    val favoriteVodIds: StateFlow<Set<Long>> = favoritesRepository.favoriteVodIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    /** null = the "All series" pseudo-category (no category filter). */
    private val selectedCategoryName = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SeriesUiState> = settings.activeSourceId
        .flatMapLatest { sourceId ->
            if (sourceId == null) {
                flowOf(SeriesUiState(hasSource = false))
            } else {
                combine(
                    repository.seriesCategoryCounts(sourceId),
                    repository.seriesCount(sourceId),
                    selectedCategoryName,
                    settings.pinnedCategories(PIN_NAMESPACE),
                    settings.isParentalLockEnabled,
                ) { counts, total, selectedName, pinned, parentalLock ->
                    // Parental lock: hide adult genres and subtract them from the "All" total.
                    val visibleCounts = if (parentalLock) counts.filterNot { AdultContentFilter.isAdult(it.name) } else counts
                    val allTotal = if (parentalLock) total - counts.filter { AdultContentFilter.isAdult(it.name) }.sumOf { it.count } else total
                    // Pinned genres float to the top (alphabetical); "All series" sits right after the "Favorites" row.
                    val (pinnedCats, others) = visibleCounts
                        .map { SeriesCategorySummary(it.name, it.count, it.name in pinned) }
                        .partition { it.pinned }
                    val nonFav = listOf(SeriesCategorySummary(getApplication<Application>().getString(R.string.browse_all_series), allTotal)) +
                        pinnedCats.sortedBy { it.name.lowercase() } + others
                    nonFav to selectedName
                }.combine(repository.favoriteSeriesCount(sourceId)) { (nonFav, selectedName), favCount ->
                    // "Favorites" always leads the column (order: favorite, [all], pinned, rest).
                    val categories = listOf(SeriesCategorySummary(getApplication<Application>().getString(R.string.nav_favorites), favCount)) + nonFav
                    val index = when (selectedName) {
                        FAVORITES -> 0
                        null -> 1
                        else -> categories.indexOfFirst { it.name == selectedName }.let { if (it >= 0) it else 1 }
                    }
                    SeriesUiState(hasSource = true, categories = categories, selectedCategoryIndex = index)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SeriesUiState())

    val pagingData: Flow<PagingData<VodTitle>> = combine(
        settings.activeSourceId,
        selectedCategoryName,
        settings.isParentalLockEnabled,
    ) { sourceId, category, parentalLock -> Triple(sourceId, category, parentalLock) }
        .flatMapLatest { (sourceId, category, parentalLock) ->
            if (sourceId == null) flowOf(PagingData.empty())
            else {
                val pager = if (category == FAVORITES) browsePager { repository.pagingFavoriteSeries(sourceId) }
                    else browsePager { repository.pagingSeries(sourceId, category) }
                pager.flow
                    .let { flow -> if (parentalLock) flow.map { it.filter { s -> !AdultContentFilter.isAdult(s.categoryName) } } else flow }
            }
        }
        .cachedIn(viewModelScope)

    fun selectCategory(index: Int) {
        selectedCategoryName.value = when (index) {
            0 -> FAVORITES
            1 -> null
            else -> uiState.value.categories.getOrNull(index)?.name
        }
    }

    /** Pin/unpin the genre at [index] (index 0 = "Favorites", 1 = "All series" -- neither is pinnable). */
    fun togglePin(index: Int) {
        if (index <= 1) return
        val name = uiState.value.categories.getOrNull(index)?.name ?: return
        viewModelScope.launch { settings.togglePinnedCategory(PIN_NAMESPACE, name) }
    }

    fun toggleFavorite(vodTitleId: Long) {
        viewModelScope.launch { favoritesRepository.toggleVod(vodTitleId, ContentType.SERIES) }
    }

    companion object {
        private const val PIN_NAMESPACE = "series"

        /** Sentinel [selectedCategoryName] value for the "Favorites" pseudo-category (null = "All").
         *  NUL-wrapped so it can never collide with a real provider category name. */
        private const val FAVORITES = " favorites "

        fun factory(app: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = SeriesViewModel(app) as T
            }
    }
}
