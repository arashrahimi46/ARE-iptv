package com.arashrahimi46.iptv.mobile.ui.movies

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.arashrahimi46.iptv.data.model.VodTitle
import com.arashrahimi46.iptv.data.repository.PlaylistRepository
import com.arashrahimi46.iptv.data.repository.PlaylistRepositoryImpl
import com.arashrahimi46.iptv.data.settings.UserSettings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Backs both Movies and Series tabs (same catalog shape, filtered by [isSeries]) -- avoids
 * duplicating the paging/category-filter wiring for what is otherwise identical logic. */
@OptIn(ExperimentalCoroutinesApi::class)
abstract class VodGridViewModel(application: Application, private val isSeries: Boolean) : AndroidViewModel(application) {
    private val repository: PlaylistRepository = PlaylistRepositoryImpl(application)
    private val settings = UserSettings(application)

    private val category = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = category

    val categories: StateFlow<List<String>> = settings.activeSourceId
        .distinctUntilChanged()
        .flatMapLatest { sourceId ->
            if (sourceId == null) {
                flowOf(emptyList())
            } else if (isSeries) {
                repository.seriesCategoryCounts(sourceId).let { flow -> mapNames(flow) }
            } else {
                repository.movieCategoryCounts(sourceId).let { flow -> mapNames(flow) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun mapNames(flow: Flow<List<com.arashrahimi46.iptv.data.db.CategoryCount>>): Flow<List<String>> =
        flow.map { list -> list.map { it.name } }

    val items: Flow<PagingData<VodTitle>> = settings.activeSourceId
        .distinctUntilChanged()
        .flatMapLatest { sourceId ->
            category.flatMapLatest { cat ->
                if (sourceId == null) {
                    flowOf(PagingData.empty())
                } else {
                    Pager(
                        config = PagingConfig(pageSize = 60, prefetchDistance = 60, enablePlaceholders = false),
                        pagingSourceFactory = {
                            if (isSeries) repository.pagingSeries(sourceId, cat) else repository.pagingMovies(sourceId, cat)
                        },
                    ).flow
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
