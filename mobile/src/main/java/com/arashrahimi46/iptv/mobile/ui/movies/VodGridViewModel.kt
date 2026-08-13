package com.arashrahimi46.iptv.mobile.ui.movies

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import com.arashrahimi46.iptv.mobile.data.model.ContentType
import com.arashrahimi46.iptv.mobile.data.model.VodTitle
import com.arashrahimi46.iptv.mobile.data.repository.FavoritesRepository
import com.arashrahimi46.iptv.mobile.data.repository.PlaylistRepository
import com.arashrahimi46.iptv.mobile.data.repository.PlaylistRepositoryImpl
import com.arashrahimi46.iptv.mobile.data.settings.UserSettings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Backs both Movies and Series tabs (same catalog shape, filtered by [isSeries]) -- avoids
 * duplicating the paging/category-filter wiring for what is otherwise identical logic. */
@OptIn(ExperimentalCoroutinesApi::class)
abstract class VodGridViewModel(application: Application, val isSeries: Boolean) : AndroidViewModel(application) {
    private val repository: PlaylistRepository = PlaylistRepositoryImpl(application)
    private val settings = UserSettings(application)
    private val favoritesRepo = FavoritesRepository(application)

    private val _isRefreshing = MutableStateFlow(false)

    /** Drives the pull-to-refresh indicator. */
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    /**
     * Pull-to-refresh: re-imports the active source. The Room-backed PagingSource invalidates
     * itself once rows change, so the grid repopulates without any extra plumbing.
     */
    fun refresh() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val sourceId = settings.activeSourceId.first()
                if (sourceId != null) runCatching { repository.refreshSource(sourceId) }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    /** Live favorite membership for the grid's heart toggles. */
    val favoriteVodIds: StateFlow<Set<Long>> =
        favoritesRepo.favoriteVodIds.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    fun toggleFavorite(title: VodTitle) {
        viewModelScope.launch {
            favoritesRepo.toggleVod(title.id, if (isSeries) ContentType.SERIES else ContentType.MOVIE)
        }
    }

    private val category = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = category

    /** Parental lock, HIDE mode: an adult category is absent from the chip strip entirely. */
    val categories: StateFlow<List<String>> = combine(
        settings.activeSourceId.distinctUntilChanged(),
        settings.parentalFilter,
    ) { sourceId, parental -> sourceId to parental }
        .flatMapLatest { (sourceId, parental) ->
            val counts = when {
                sourceId == null -> return@flatMapLatest flowOf(emptyList())
                isSeries -> repository.seriesCategoryCounts(sourceId)
                else -> repository.movieCategoryCounts(sourceId)
            }
            mapNames(counts).map { names -> names.filterNot { parental.hidden(it) } }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun mapNames(flow: Flow<List<com.arashrahimi46.iptv.mobile.data.db.CategoryCount>>): Flow<List<String>> =
        flow.map { list -> list.map { it.name } }

    val items: Flow<PagingData<VodTitle>> = combine(
        settings.activeSourceId.distinctUntilChanged(),
        settings.parentalFilter,
    ) { sourceId, parental -> sourceId to parental }
        .flatMapLatest { (sourceId, parental) ->
            category.flatMapLatest { cat ->
                if (sourceId == null || parental.hidden(cat)) {
                    // A hidden category can still be *selected* (picked before the lock went on, or
                    // the session relocked underneath it) -- serve nothing rather than its rows.
                    flowOf(PagingData.empty())
                } else {
                    Pager(
                        config = PagingConfig(pageSize = 60, prefetchDistance = 60, enablePlaceholders = false),
                        pagingSourceFactory = {
                            if (isSeries) repository.pagingSeries(sourceId, cat) else repository.pagingMovies(sourceId, cat)
                        },
                    ).flow
                        // "All" (cat == null) pages across every category, so adult rows have to be
                        // dropped per item; Room can't express the keyword heuristic in SQL.
                        .map { data -> data.filter { !parental.hidden(it.categoryName) } }
                }
            }
        }
        .cachedIn(viewModelScope)

    fun selectCategory(name: String?) {
        category.value = name
    }
}

class MoviesViewModel(application: Application) : VodGridViewModel(application, isSeries = false)
class SeriesViewModel(application: Application) : VodGridViewModel(application, isSeries = true)
