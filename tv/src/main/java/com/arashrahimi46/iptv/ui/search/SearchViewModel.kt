package com.arashrahimi46.iptv.ui.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arashrahimi46.iptv.data.model.Channel
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Search.jsx's scope chips (All / Live TV / Movies / Series) -- Catch-up is an accepted v1
 * scope cut (see product-lead ruling), so it's not one of these. */
enum class SearchScope { All, LiveTv, Movies, Series }

data class SearchUiState(
    val hasSource: Boolean = false,
    val query: String = "",
    /** Set when arriving from Home's "Browse by category" cards (see SearchScreen) --
     * an exact categoryName match, distinct from [query]'s substring-on-name search. */
    val categoryFilter: String? = null,
    val scope: SearchScope = SearchScope.All,
    val channelResults: List<Channel> = emptyList(),
    val titleResults: List<VodTitle> = emptyList(),
)

/**
 * Local-only search (no ranking backend, no external service, per spec):
 * substring match over the already-loaded [Channel]/[VodTitle] catalog in
 * Room for the active source. Query text comes from [setQuery], driven by
 * Android TV's native IME via [com.arashrahimi46.iptv.ui.components.AreTextField]
 * (issue #10 -- no custom on-screen keyboard).
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SearchViewModel(app: Application) : AndroidViewModel(app) {
    private val repository: PlaylistRepository = PlaylistRepositoryImpl(app)
    private val settings = UserSettings(app)
    private val favoritesRepository = FavoritesRepository(app)

    val favoriteChannelIds: StateFlow<Set<Long>> = favoritesRepository.favoriteChannelIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())
    val favoriteVodIds: StateFlow<Set<Long>> = favoritesRepository.favoriteVodIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    private val _query = MutableStateFlow("")
    private val _categoryFilter = MutableStateFlow<String?>(null)
    private val _scope = MutableStateFlow(SearchScope.All)

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        // DB-side search: LIKE + category queries with a bounded LIMIT, run per keystroke via
        // mapLatest (older in-flight queries cancel). Never loads the catalog into memory --
        // essential at 100k+ channels / 300k+ titles.
        combine(settings.activeSourceId, _query, _categoryFilter, _scope) { sid, query, categoryFilter, scope ->
            SearchInputs(sid, query, categoryFilter, scope)
        }
            .mapLatest { buildState(it) }
            .onEach { _uiState.value = it }
            .launchIn(viewModelScope)
    }

    private data class SearchInputs(val sourceId: Long?, val query: String, val categoryFilter: String?, val scope: SearchScope)

    private suspend fun buildState(inputs: SearchInputs): SearchUiState {
        val (sourceId, query, categoryFilter, scope) = inputs
        if (sourceId == null) return SearchUiState(hasSource = false, query = query, categoryFilter = categoryFilter, scope = scope)
        val wantChannels = scope == SearchScope.All || scope == SearchScope.LiveTv
        val wantMovies = scope == SearchScope.All || scope == SearchScope.Movies
        val wantSeries = scope == SearchScope.All || scope == SearchScope.Series

        if (categoryFilter != null) {
            val channelResults = if (wantChannels) repository.channelsByCategory(sourceId, categoryFilter, RESULT_LIMIT) else emptyList()
            val titleResults = (if (wantMovies) repository.moviesByCategory(sourceId, categoryFilter, RESULT_LIMIT) else emptyList()) +
                (if (wantSeries) repository.seriesByCategory(sourceId, categoryFilter, RESULT_LIMIT) else emptyList())
            return SearchUiState(hasSource = true, query = query, categoryFilter = categoryFilter, scope = scope, channelResults = channelResults, titleResults = titleResults)
        }
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return SearchUiState(hasSource = true, query = query, scope = scope)
        val channelResults = if (wantChannels) repository.searchChannels(sourceId, trimmed, RESULT_LIMIT) else emptyList()
        val titleResults = (if (wantMovies) repository.searchMovies(sourceId, trimmed, RESULT_LIMIT) else emptyList()) +
            (if (wantSeries) repository.searchSeries(sourceId, trimmed, RESULT_LIMIT) else emptyList())
        return SearchUiState(hasSource = true, query = query, scope = scope, channelResults = channelResults, titleResults = titleResults)
    }

    fun setQuery(value: String) {
        _categoryFilter.value = null
        _query.value = value
    }

    /** Null clears back to normal text search (e.g. the category header's "Clear" action). */
    fun setCategoryFilter(category: String?) {
        _categoryFilter.value = category
        _query.value = ""
    }

    fun setScope(scope: SearchScope) {
        _scope.value = scope
    }

    fun toggleChannelFavorite(channelId: Long) {
        viewModelScope.launch { favoritesRepository.toggleChannel(channelId) }
    }

    fun toggleVodFavorite(vodTitle: VodTitle) {
        val contentType = if (vodTitle.isSeries) ContentType.SERIES else ContentType.MOVIE
        viewModelScope.launch { favoritesRepository.toggleVod(vodTitle.id, contentType) }
    }

    companion object {
        /** Max results per catalog (channels / movies / series) -- matches the old in-memory `.take(30)`. */
        private const val RESULT_LIMIT = 30

        fun factory(app: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = SearchViewModel(app) as T
            }
    }
}
