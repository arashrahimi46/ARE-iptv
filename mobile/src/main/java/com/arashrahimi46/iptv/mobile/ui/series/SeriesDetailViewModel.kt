package com.arashrahimi46.iptv.mobile.ui.series

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arashrahimi46.iptv.data.model.SeriesEpisode
import com.arashrahimi46.iptv.data.model.VodTitle
import com.arashrahimi46.iptv.data.repository.PlaylistRepository
import com.arashrahimi46.iptv.data.repository.PlaylistRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SeriesDetailUiState(
    val loading: Boolean = true,
    val title: VodTitle? = null,
    val episodesBySeason: Map<Int, List<SeriesEpisode>> = emptyMap(),
    /** True for a series-typed M3U entry with no grouped episode structure -- a real limitation
     * (no authoritative episode listing exists for it), mirrors :tv's Detail screen. */
    val isM3uSeriesWithoutEpisodes: Boolean = false,
    val episodesLoadError: String? = null,
)

/**
 * Touch-first counterpart of :tv's `DetailViewModel`, scoped to just the series/episode-picker
 * concern (no metadata enrichment, no favorite toggle -- those already exist elsewhere in
 * :mobile). Reuses :core's [PlaylistRepository.ensureSeriesEpisodesLoaded]/[PlaylistRepository.observeSeriesEpisodes]
 * unchanged, same data layer :tv's Detail screen uses.
 */
class SeriesDetailViewModel(app: Application, private val vodTitleId: Long) : AndroidViewModel(app) {
    private val repository: PlaylistRepository = PlaylistRepositoryImpl(app)

    private val _uiState = MutableStateFlow(SeriesDetailUiState())
    val uiState: StateFlow<SeriesDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val title = repository.titlesByIds(listOf(vodTitleId)).firstOrNull()
            _uiState.update { it.copy(loading = false, title = title) }
            if (title == null || !title.isSeries) return@launch

            // Xtream loads episodes lazily from get_series_info; M3U series were already grouped
            // into series_episodes at import time. Either way, observe the same table.
            if (title.externalId != null) {
                runCatching { repository.ensureSeriesEpisodesLoaded(title) }
                    .onFailure { e -> _uiState.update { it.copy(episodesLoadError = e.message) } }
            }
            repository.observeSeriesEpisodes(title.id)
                .onEach { episodes ->
                    _uiState.update {
                        it.copy(
                            episodesBySeason = episodes.groupBy { ep -> ep.season },
                            isM3uSeriesWithoutEpisodes = title.externalId == null && episodes.isEmpty(),
                        )
                    }
                }
                .launchIn(viewModelScope)
        }
    }

    companion object {
        fun factory(app: Application, vodTitleId: Long): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = SeriesDetailViewModel(app, vodTitleId) as T
            }
    }
}
