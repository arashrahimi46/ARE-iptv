package com.arashrahimi46.iptv.ui.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arashrahimi46.iptv.data.db.AppDatabase
import com.arashrahimi46.iptv.data.model.ContentType
import com.arashrahimi46.iptv.data.model.SeriesEpisode
import com.arashrahimi46.iptv.data.model.VodTitle
import com.arashrahimi46.iptv.data.repository.FavoritesRepository
import com.arashrahimi46.iptv.data.repository.PlaylistRepository
import com.arashrahimi46.iptv.data.repository.PlaylistRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
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
    private val favoritesRepository = FavoritesRepository(app)

    /** True once this title's own id shows up in the real favorites table -- drives Detail's favorite toggle button. */
    val isFavorite: StateFlow<Boolean> = favoritesRepository.favoriteVodIds
        .map { contentId in it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val title = db.vodTitleDao().getById(contentId)
            _uiState.update { it.copy(loading = false, title = title) }
            if (title == null) return@launch

            // Enrich plot/cast/IMDb+RT ranks on first view (Room-cached after). Runs concurrently with
            // the episode load below; both use atomic update {} so neither clobbers the other's field.
            launch {
                val enriched = runCatching { repository.ensureMetadataLoaded(title) }.getOrNull()
                if (enriched != null) _uiState.update { it.copy(title = enriched) }
            }

            if (title.isSeries) {
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
                                // A series with no grouped episodes and no Xtream backing (e.g. a lone
                                // M3U entry with no SxxExx marker) plays as a single item.
                                isM3uSeriesWithoutEpisodes = title.externalId == null && episodes.isEmpty(),
                            )
                        }
                    }
                    .launchIn(viewModelScope)
            }
        }
    }

    fun toggleFavorite() {
        val isSeries = _uiState.value.title?.isSeries ?: return
        val contentType = if (isSeries) ContentType.SERIES else ContentType.MOVIE
        viewModelScope.launch { favoritesRepository.toggleVod(contentId, contentType) }
    }

    companion object {
        fun factory(app: Application, contentId: Long): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = DetailViewModel(app, contentId) as T
            }
    }
}
