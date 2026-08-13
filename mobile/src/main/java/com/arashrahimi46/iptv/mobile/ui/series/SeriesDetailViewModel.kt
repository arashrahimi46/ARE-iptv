package com.arashrahimi46.iptv.mobile.ui.series

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arashrahimi46.iptv.mobile.data.model.ContentType
import com.arashrahimi46.iptv.mobile.data.model.SeriesEpisode
import com.arashrahimi46.iptv.mobile.data.model.VodTitle
import com.arashrahimi46.iptv.mobile.data.repository.ContinueWatchingRepository
import com.arashrahimi46.iptv.mobile.data.repository.FavoritesRepository
import com.arashrahimi46.iptv.mobile.data.repository.PlaylistRepository
import com.arashrahimi46.iptv.mobile.data.repository.PlaylistRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SeriesDetailUiState(
    val loading: Boolean = true,
    val title: VodTitle? = null,
    val episodesBySeason: Map<Int, List<SeriesEpisode>> = emptyMap(),
    /** Room has answered at least once. Distinguishes "still fetching" from "genuinely empty" --
     * without it an Xtream series whose get_series_info returns zero rows spins forever. */
    val episodesLoaded: Boolean = false,
    /** True for a series-typed M3U entry with no grouped episode structure -- a real limitation
     * (no authoritative episode listing exists for it), mirrors :tv's Detail screen. It is still
     * playable as a single item, which is what the primary button falls back to. */
    val isM3uSeriesWithoutEpisodes: Boolean = false,
    val episodesLoadError: String? = null,
    /** Pull-to-refresh in flight. */
    val refreshing: Boolean = false,
    /** Season whose episodes the list is showing. Defaults to the lowest season once episodes
     * arrive and is never overwritten afterwards -- a user's pick survives later emissions. */
    val selectedSeason: Int? = null,
    /** Most recently watched in-progress episode of this series, if any -- what "Resume" reopens. */
    val resumeEpisodeId: Long? = null,
    /** 0f..1f watch progress per episode id, for the per-row bar. Finished episodes are absent
     * (the player clears their bookmark past the completion threshold). */
    val progressByEpisodeId: Map<Long, Float> = emptyMap(),
)

/**
 * Touch-first counterpart of :tv's `DetailViewModel` for series: loads the parent title, enriches it
 * with plot/cast/ratings, lazily populates and then observes the episode rows, groups them into
 * seasons, and joins in continue-watching bookmarks so the screen can offer Resume and per-episode
 * progress.
 *
 * Reuses the data layer unchanged -- [PlaylistRepository.ensureSeriesEpisodesLoaded] /
 * [PlaylistRepository.observeSeriesEpisodes] / [PlaylistRepository.ensureMetadataLoaded] are the
 * same calls :tv's Detail screen makes.
 */
class SeriesDetailViewModel(app: Application, private val vodTitleId: Long) : AndroidViewModel(app) {
    private val repository: PlaylistRepository = PlaylistRepositoryImpl(app)
    private val favoritesRepository = FavoritesRepository(app)
    private val continueWatchingRepo = ContinueWatchingRepository(app)

    val isFavorite: StateFlow<Boolean> = favoritesRepository.favoriteVodIds
        .map { vodTitleId in it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _uiState = MutableStateFlow(SeriesDetailUiState())
    val uiState: StateFlow<SeriesDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val title = repository.titlesByIds(listOf(vodTitleId)).firstOrNull()
            _uiState.update { it.copy(loading = false, title = title) }
            if (title == null || !title.isSeries) {
                _uiState.update { it.copy(episodesLoaded = true) }
                return@launch
            }

            // Plot/cast/ratings enrichment, concurrent with the episode load. Both writers go
            // through the atomic update {}, so whichever lands second keeps the other's fields.
            launch {
                runCatching { repository.ensureMetadataLoaded(title) }.getOrNull()
                    ?.let { enriched -> _uiState.update { it.copy(title = enriched) } }
            }

            // Xtream/Stalker load episodes lazily from the provider; M3U series were already
            // grouped into series_episodes at import time. Either way, observe the same table.
            launch { loadEpisodes(title) }

            launch {
                repository.observeSeriesEpisodes(title.id).collectLatest { episodes ->
                    _uiState.update {
                        it.copy(
                            episodesBySeason = episodes.groupBy { ep -> ep.season },
                            episodesLoaded = true,
                            isM3uSeriesWithoutEpisodes = title.externalId == null && episodes.isEmpty(),
                            selectedSeason = it.selectedSeason
                                ?: episodes.minOfOrNull { ep -> ep.season },
                        )
                    }
                    observeProgress(episodes)
                }
            }
        }
    }

    /** Suspends until the provider fetch settles so pull-to-refresh can hold its spinner. */
    private suspend fun loadEpisodes(title: VodTitle) {
        if (title.externalId == null) return
        runCatching { repository.ensureSeriesEpisodesLoaded(title) }
            .onFailure { e -> _uiState.update { it.copy(episodesLoadError = e.message) } }
    }

    /**
     * Joins the episode rows to their continue-watching bookmarks. Room re-emits on every write, so
     * coming back from the player repaints the progress bars and moves Resume with no lifecycle
     * plumbing. Runs inside [collectLatest] above, so a new episode list cancels the old join.
     */
    private suspend fun observeProgress(episodes: List<SeriesEpisode>) {
        continueWatchingRepo.observeEntriesForEpisodes(episodes.map { it.id }).collect { entries ->
            val progress = entries.mapNotNull { (episodeId, entry) ->
                if (entry.durationMs <= 0L) {
                    null
                } else {
                    episodeId to (entry.positionMs.toFloat() / entry.durationMs).coerceIn(0f, 1f)
                }
            }.toMap()
            val resumeId = entries.values.maxByOrNull { it.updatedAtMs }?.seriesEpisodeId
            _uiState.update { it.copy(progressByEpisodeId = progress, resumeEpisodeId = resumeId) }
        }
    }

    fun selectSeason(season: Int) {
        _uiState.update { it.copy(selectedSeason = season) }
    }

    /** Pull-to-refresh: re-asks the provider for the episode listing, clearing any prior error. */
    fun refreshEpisodes() {
        val title = _uiState.value.title ?: return
        if (_uiState.value.refreshing) return
        _uiState.update { it.copy(refreshing = true, episodesLoadError = null) }
        viewModelScope.launch {
            loadEpisodes(title)
            _uiState.update { it.copy(refreshing = false) }
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch { favoritesRepository.toggleVod(vodTitleId, ContentType.SERIES) }
    }

    companion object {
        fun factory(app: Application, vodTitleId: Long): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = SeriesDetailViewModel(app, vodTitleId) as T
            }
    }
}
