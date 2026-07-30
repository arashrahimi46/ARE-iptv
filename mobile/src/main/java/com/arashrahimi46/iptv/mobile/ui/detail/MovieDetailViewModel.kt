package com.arashrahimi46.iptv.mobile.ui.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arashrahimi46.iptv.data.model.ContentType
import com.arashrahimi46.iptv.data.model.VodTitle
import com.arashrahimi46.iptv.data.repository.FavoritesRepository
import com.arashrahimi46.iptv.data.repository.PlaylistRepository
import com.arashrahimi46.iptv.data.repository.PlaylistRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MovieDetailUiState(
    val loading: Boolean = true,
    val title: VodTitle? = null,
)

/**
 * Touch-first movie detail -- the piece :mobile was missing entirely (movie tiles routed straight
 * into the player with no info screen at all). Mirrors :tv's `DetailViewModel`, scoped to the movie
 * half only (no episode-list concern -- that's [com.arashrahimi46.iptv.mobile.ui.series.SeriesDetailViewModel]).
 * Reuses :core's [PlaylistRepository.ensureMetadataLoaded] unchanged, same lazy plot/cast/rating
 * enrichment (Xtream info block, OMDb fallback) :tv's Detail screen uses.
 */
class MovieDetailViewModel(app: Application, private val vodTitleId: Long) : AndroidViewModel(app) {
    private val repository: PlaylistRepository = PlaylistRepositoryImpl(app)
    private val favoritesRepository = FavoritesRepository(app)

    val isFavorite: StateFlow<Boolean> = favoritesRepository.favoriteVodIds
        .map { vodTitleId in it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _uiState = MutableStateFlow(MovieDetailUiState())
    val uiState: StateFlow<MovieDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val title = repository.titlesByIds(listOf(vodTitleId)).firstOrNull()
            _uiState.update { it.copy(loading = false, title = title) }
            if (title == null) return@launch
            val enriched = runCatching { repository.ensureMetadataLoaded(title) }.getOrNull()
            if (enriched != null) _uiState.update { it.copy(title = enriched) }
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch { favoritesRepository.toggleVod(vodTitleId, ContentType.MOVIE) }
    }

    companion object {
        fun factory(app: Application, vodTitleId: Long): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = MovieDetailViewModel(app, vodTitleId) as T
            }
    }
}
