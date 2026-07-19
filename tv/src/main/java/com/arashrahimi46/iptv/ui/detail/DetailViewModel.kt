package com.arashrahimi46.iptv.ui.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arashrahimi46.iptv.data.db.AppDatabase
import com.arashrahimi46.iptv.data.model.SeriesEpisode
import com.arashrahimi46.iptv.data.model.VodTitle
import com.arashrahimi46.iptv.data.repository.PlaylistRepository
import com.arashrahimi46.iptv.data.repository.PlaylistRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class DetailUiState(
    val loading: Boolean = true,
    val title: VodTitle? = null,
    val episodesBySeason: Map<Int, List<SeriesEpisode>> = emptyMap(),
    /** True for a series-typed M3U entry -- no authoritative episode structure exists for M3U (real limitation, not a bug). */
    val isM3uSeriesWithoutEpisodes: Boolean = false,
    val episodesLoadError: String? = null,
)

/**
 * Loads the REAL [VodTitle] for [contentId] from Room -- the bug the spec
 * calls out ("the prototype always shows one hardcoded record") is avoided
 * by construction: this id comes straight from the tapped tile's own
 * [VodTitle.id] via the NavHost arg, so different tiles resolve different rows.
 * For a series with an Xtream `externalId`, lazily populates+observes its
 * episode list via [PlaylistRepository.ensureSeriesEpisodesLoaded].
 */
class DetailViewModel(app: Application, private val contentId: Long) : AndroidViewModel(app) {
    private val db = AppDatabase.get(app)
    private val repository: PlaylistRepository = PlaylistRepositoryImpl(app)

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val title = db.vodTitleDao().getById(contentId)
            _uiState.value = _uiState.value.copy(loading = false, title = title)

            if (title != null && title.isSeries) {
                if (title.externalId != null) {
                    runCatching { repository.ensureSeriesEpisodesLoaded(title) }
                        .onFailure { e -> _uiState.value = _uiState.value.copy(episodesLoadError = e.message) }
                    repository.observeSeriesEpisodes(title.id)
                        .onEach { episodes ->
                            _uiState.value = _uiState.value.copy(episodesBySeason = episodes.groupBy { it.season })
                        }
                        .launchIn(viewModelScope)
                } else {
                    // M3U series entries have no authoritative season/episode structure --
                    // the series itself is the single playable item (documented limitation).
                    _uiState.value = _uiState.value.copy(isM3uSeriesWithoutEpisodes = true)
                }
            }
        }
    }

    companion object {
        fun factory(app: Application, contentId: Long): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = DetailViewModel(app, contentId) as T
            }
    }
}
