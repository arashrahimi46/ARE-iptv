package com.arashrahimi46.iptv.ui.movies

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
import com.arashrahimi46.iptv.data.settings.UserSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MoviesCategorySummary(val name: String, val count: Int)

data class MoviesUiState(
    val hasSource: Boolean = false,
    val allMovies: List<VodTitle> = emptyList(),
    val categories: List<MoviesCategorySummary> = emptyList(),
    val selectedCategoryIndex: Int = 0,
) {
    /** "All movies" is always category 0; anything else filters by [VodTitle.categoryName]. */
    val visibleMovies: List<VodTitle>
        get() = if (selectedCategoryIndex == 0 || categories.isEmpty()) {
            allMovies
        } else {
            val name = categories.getOrNull(selectedCategoryIndex)?.name
            if (name == null) allMovies else allMovies.filter { it.categoryName == name }
        }
}

/**
 * Movies browse state: real [VodTitle] catalog (isSeries = false) for the
 * active source (Room via [PlaylistRepository]), grouped the same way
 * [com.arashrahimi46.iptv.ui.live.LiveViewModel] groups channels.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class MoviesViewModel(app: Application) : AndroidViewModel(app) {
    private val repository: PlaylistRepository = PlaylistRepositoryImpl(app)
    private val settings = UserSettings(app)
    private val favoritesRepository = FavoritesRepository(app)

    val favoriteVodIds: StateFlow<Set<Long>> = favoritesRepository.favoriteVodIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    private val _uiState = MutableStateFlow(MoviesUiState())
    val uiState: StateFlow<MoviesUiState> = _uiState.asStateFlow()

    init {
        settings.activeSourceId
            .flatMapLatest { sourceId ->
                if (sourceId == null) flowOf<List<VodTitle>?>(null) else repository.observeMovies(sourceId)
            }
            .onEach { movies ->
                _uiState.value = if (movies == null) MoviesUiState(hasSource = false) else buildState(movies)
            }
            .launchIn(viewModelScope)
    }

    private fun buildState(movies: List<VodTitle>): MoviesUiState {
        val counts = linkedMapOf<String, Int>()
        movies.forEach { movie ->
            movie.categoryName?.let { counts[it] = (counts[it] ?: 0) + 1 }
        }
        val categories = listOf(MoviesCategorySummary("All movies", movies.size)) +
            counts.map { (name, count) -> MoviesCategorySummary(name, count) }
        val previousSelection = _uiState.value.selectedCategoryIndex.coerceIn(0, categories.lastIndex.coerceAtLeast(0))
        return MoviesUiState(
            hasSource = true,
            allMovies = movies,
            categories = categories,
            selectedCategoryIndex = previousSelection,
        )
    }

    fun selectCategory(index: Int) {
        _uiState.value = _uiState.value.copy(selectedCategoryIndex = index)
    }

    fun toggleFavorite(vodTitleId: Long) {
        viewModelScope.launch { favoritesRepository.toggleVod(vodTitleId, ContentType.MOVIE) }
    }

    companion object {
        fun factory(app: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = MoviesViewModel(app) as T
            }
    }
}
