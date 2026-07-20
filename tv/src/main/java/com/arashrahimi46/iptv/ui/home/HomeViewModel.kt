package com.arashrahimi46.iptv.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arashrahimi46.iptv.data.model.Channel
import com.arashrahimi46.iptv.data.model.VodTitle
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

data class HomeCategorySummary(val name: String, val count: Int)

data class HomeUiState(
    val hasSource: Boolean = false,
    val channels: List<Channel> = emptyList(),
    val movies: List<VodTitle> = emptyList(),
    val series: List<VodTitle> = emptyList(),
    val categories: List<HomeCategorySummary> = emptyList(),
    /** QA LOW defect: a real source existed, but Room hadn't emitted its first catalog read
     * yet (cold-start DB open on a large catalog took 1.5-7s in QA's test) -- [hasSource]'s
     * default-constructed `false` was indistinguishable from "confirmed no source", flashing
     * "No playlist yet" during that window. True only before the very first emission below;
     * every real emission (source or no-source) sets it false, whether or not the catalog
     * itself turns out empty. */
    val isInitializing: Boolean = true,
)

/**
 * Reads the active playlist's catalog from [PlaylistRepository] and shapes it
 * into the Home rails (Home.jsx). Degrades gracefully to empty lists when
 * there's no active source or an empty catalog -- a fresh install is a real
 * state, not an error.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val repository: PlaylistRepository = PlaylistRepositoryImpl(app)
    private val settings = UserSettings(app)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        settings.activeSourceId
            .flatMapLatest { sourceId ->
                if (sourceId == null) {
                    flowOf(HomeUiState(hasSource = false, isInitializing = false))
                } else {
                    combine(
                        repository.observeChannels(sourceId),
                        repository.observeMovies(sourceId),
                        repository.observeSeries(sourceId),
                    ) { channels, movies, series ->
                        val categoryCounts = linkedMapOf<String, Int>()
                        (channels.mapNotNull { it.categoryName } +
                            movies.mapNotNull { it.categoryName } +
                            series.mapNotNull { it.categoryName }).forEach { name ->
                            categoryCounts[name] = (categoryCounts[name] ?: 0) + 1
                        }
                        HomeUiState(
                            hasSource = true,
                            channels = channels,
                            movies = movies,
                            series = series,
                            categories = categoryCounts.map { (name, count) -> HomeCategorySummary(name, count) },
                            isInitializing = false,
                        )
                    }
                }
            }
            .onEach { _uiState.value = it }
            .launchIn(viewModelScope)
    }

    companion object {
        fun factory(app: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = HomeViewModel(app) as T
            }
    }
}
