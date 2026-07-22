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
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
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
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class, FlowPreview::class)
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
        // The text field's displayed query ([SearchUiState.query]) is updated SYNCHRONOUSLY in
        // setQuery -- never gated behind the DB query -- so typing is instant and never drops a
        // keystroke (the old pipeline only surfaced the query after the search returned, which on
        // a huge catalog lagged the field enough to swallow the next character).
        //
        // The actual search is debounced: it runs only after typing pauses (empty query is
        // instant, so clearing / category browsing isn't delayed), and only its RESULTS are
        // merged into the state. DB-side LIKE + category queries stay bounded by LIMIT and
        // cancel-on-newer via mapLatest -- never loads the catalog into memory.
        val debouncedQuery = _query.debounce { q -> if (q.isEmpty()) 0L else SEARCH_DEBOUNCE_MS }
        combine(settings.activeSourceId, debouncedQuery, _categoryFilter, _scope) { sid, query, categoryFilter, scope ->
            SearchInputs(sid, query, categoryFilter, scope)
        }
            .mapLatest { buildResults(it) }
            .onEach { r -> _uiState.update { it.copy(hasSource = r.hasSource, channelResults = r.channels, titleResults = r.titles) } }
            .launchIn(viewModelScope)
    }

    private data class SearchInputs(val sourceId: Long?, val query: String, val categoryFilter: String?, val scope: SearchScope)

    private data class SearchResults(val hasSource: Boolean, val channels: List<Channel>, val titles: List<VodTitle>)

    private suspend fun buildResults(inputs: SearchInputs): SearchResults {
        val (sourceId, query, categoryFilter, scope) = inputs
        if (sourceId == null) return SearchResults(hasSource = false, channels = emptyList(), titles = emptyList())
        val wantChannels = scope == SearchScope.All || scope == SearchScope.LiveTv
        val wantMovies = scope == SearchScope.All || scope == SearchScope.Movies
        val wantSeries = scope == SearchScope.All || scope == SearchScope.Series

        if (categoryFilter != null) {
            val channelResults = if (wantChannels) repository.channelsByCategory(sourceId, categoryFilter, RESULT_LIMIT) else emptyList()
            val titleResults = (if (wantMovies) repository.moviesByCategory(sourceId, categoryFilter, RESULT_LIMIT) else emptyList()) +
                (if (wantSeries) repository.seriesByCategory(sourceId, categoryFilter, RESULT_LIMIT) else emptyList())
            return SearchResults(hasSource = true, channels = channelResults, titles = titleResults)
        }
        val trimmed = query.trim()
        // Require at least MIN_QUERY_LENGTH characters -- a single letter matches almost the whole
        // catalog and isn't a useful search.
        if (trimmed.length < MIN_QUERY_LENGTH) return SearchResults(hasSource = true, channels = emptyList(), titles = emptyList())
        val channelResults = if (wantChannels) repository.searchChannels(sourceId, trimmed, RESULT_LIMIT) else emptyList()
        val titleResults = (if (wantMovies) repository.searchMovies(sourceId, trimmed, RESULT_LIMIT) else emptyList()) +
            (if (wantSeries) repository.searchSeries(sourceId, trimmed, RESULT_LIMIT) else emptyList())
        return SearchResults(hasSource = true, channels = channelResults, titles = titleResults)
    }

    fun setQuery(value: String) {
        _categoryFilter.value = null
        _query.value = value
        // Reflect the typed text instantly, independent of the debounced search below.
        _uiState.update { it.copy(query = value, categoryFilter = null) }
    }

    /** Null clears back to normal text search (e.g. the category header's "Clear" action). */
    fun setCategoryFilter(category: String?) {
        _categoryFilter.value = category
        _query.value = ""
        _uiState.update { it.copy(categoryFilter = category, query = "") }
    }

    fun setScope(scope: SearchScope) {
        _scope.value = scope
        _uiState.update { it.copy(scope = scope) }
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

        /** Minimum typed characters before a text search runs (a single letter is not a useful query). */
        private const val MIN_QUERY_LENGTH = 2

        /** How long typing must pause before the search fires -- keeps a huge catalog from being
         * queried on every keystroke while staying responsive. */
        private const val SEARCH_DEBOUNCE_MS = 250L

        fun factory(app: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = SearchViewModel(app) as T
            }
    }
}
