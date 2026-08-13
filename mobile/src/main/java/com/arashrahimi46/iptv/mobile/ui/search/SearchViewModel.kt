package com.arashrahimi46.iptv.mobile.ui.search

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arashrahimi46.iptv.mobile.data.model.Channel
import com.arashrahimi46.iptv.mobile.data.model.ContentType
import com.arashrahimi46.iptv.mobile.data.model.VodTitle
import com.arashrahimi46.iptv.mobile.data.repository.FavoritesRepository
import com.arashrahimi46.iptv.mobile.data.repository.PlaylistRepository
import com.arashrahimi46.iptv.mobile.data.repository.PlaylistRepositoryImpl
import com.arashrahimi46.iptv.mobile.data.settings.ParentalFilter
import com.arashrahimi46.iptv.mobile.data.settings.UserSettings
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

/** Search's scope tabs (All / Live TV / Movies / Series) -- one [androidx.compose.foundation.pager.HorizontalPager] page each. */
enum class SearchScope { All, LiveTv, Movies, Series }

data class SearchUiState(
    val hasSource: Boolean = false,
    val query: String = "",
    val scope: SearchScope = SearchScope.All,
    val channelResults: List<Channel> = emptyList(),
    val movieResults: List<VodTitle> = emptyList(),
    val seriesResults: List<VodTitle> = emptyList(),
) {
    val isEmpty: Boolean
        get() = channelResults.isEmpty() && movieResults.isEmpty() && seriesResults.isEmpty()
}

/**
 * Local-only search (no ranking backend, no external service): substring match over the
 * already-loaded [Channel]/[VodTitle] catalog in Room for the active source, driven by the phone
 * search field in [SearchScreen].
 *
 * Movies and series are kept in separate lists (not merged into one "titles" list as the TV build
 * did) because the phone UI has a page per scope plus an "All" page that sections them.
 *
 * Recent queries are persisted in a tiny private [android.content.SharedPreferences] file owned by
 * this screen -- see [recentQueries].
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class, FlowPreview::class)
class SearchViewModel(app: Application) : AndroidViewModel(app) {
    private val repository: PlaylistRepository = PlaylistRepositoryImpl(app)
    private val settings = UserSettings(app)
    private val favoritesRepository = FavoritesRepository(app)
    private val recentStore = app.getSharedPreferences(RECENT_PREFS, Context.MODE_PRIVATE)

    val favoriteChannelIds: StateFlow<Set<Long>> = favoritesRepository.favoriteChannelIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())
    val favoriteVodIds: StateFlow<Set<Long>> = favoritesRepository.favoriteVodIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    private val _query = MutableStateFlow("")
    private val _scope = MutableStateFlow(SearchScope.All)

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    /** Most-recent-first, capped at [RECENT_LIMIT]. Survives process death. */
    private val _recentQueries = MutableStateFlow(readRecent())
    val recentQueries: StateFlow<List<String>> = _recentQueries.asStateFlow()

    init {
        // The field's displayed query is updated synchronously in setQuery -- never gated behind
        // the DB query -- so typing is instant. The actual search is debounced.
        val debouncedQuery = _query.debounce { q -> if (q.isEmpty()) 0L else SEARCH_DEBOUNCE_MS }
        combine(settings.activeSourceId, debouncedQuery, _scope, settings.parentalFilter) { sid, query, scope, parental ->
            SearchInputs(sid, query, scope, parental)
        }
            .mapLatest { buildResults(it) }
            .onEach { r ->
                _uiState.update {
                    it.copy(
                        hasSource = r.hasSource,
                        channelResults = r.channels,
                        movieResults = r.movies,
                        seriesResults = r.series,
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private data class SearchInputs(val sourceId: Long?, val query: String, val scope: SearchScope, val parental: ParentalFilter)

    private data class SearchResults(
        val hasSource: Boolean,
        val channels: List<Channel>,
        val movies: List<VodTitle>,
        val series: List<VodTitle>,
    )

    private suspend fun buildResults(inputs: SearchInputs): SearchResults {
        val (sourceId, query, scope, parental) = inputs
        if (sourceId == null) return SearchResults(false, emptyList(), emptyList(), emptyList())
        val wantChannels = scope == SearchScope.All || scope == SearchScope.LiveTv
        val wantMovies = scope == SearchScope.All || scope == SearchScope.Movies
        val wantSeries = scope == SearchScope.All || scope == SearchScope.Series

        val trimmed = query.trim()
        if (trimmed.length < MIN_QUERY_LENGTH) return SearchResults(true, emptyList(), emptyList(), emptyList())
        val channelResults = (if (wantChannels) repository.searchChannels(sourceId, trimmed, RESULT_LIMIT) else emptyList())
            .filterNot { parental.hidden(it.categoryName) }
        val movieResults = (if (wantMovies) repository.searchMovies(sourceId, trimmed, RESULT_LIMIT) else emptyList())
            .filterNot { parental.hidden(it.categoryName) }
        val seriesResults = (if (wantSeries) repository.searchSeries(sourceId, trimmed, RESULT_LIMIT) else emptyList())
            .filterNot { parental.hidden(it.categoryName) }
        return SearchResults(true, channelResults, movieResults, seriesResults)
    }

    fun setQuery(value: String) {
        _query.value = value
        val belowMin = value.trim().length < MIN_QUERY_LENGTH
        _uiState.update {
            if (belowMin) it.copy(query = value, channelResults = emptyList(), movieResults = emptyList(), seriesResults = emptyList())
            else it.copy(query = value)
        }
    }

    fun setScope(scope: SearchScope) {
        _scope.value = scope
        _uiState.update { it.copy(scope = scope) }
    }

    /** Called when the user presses the IME "Search" action -- the query becomes a Recent. */
    fun commitQuery(q: String) {
        val trimmed = q.trim()
        if (trimmed.length < MIN_QUERY_LENGTH) return
        val next = (listOf(trimmed) + _recentQueries.value.filterNot { it.equals(trimmed, ignoreCase = true) })
            .take(RECENT_LIMIT)
        _recentQueries.value = next
        writeRecent(next)
    }

    /** Removes one recent query, or all of them when [q] is null. */
    fun clearRecent(q: String?) {
        val next = if (q == null) emptyList() else _recentQueries.value.filterNot { it == q }
        _recentQueries.value = next
        writeRecent(next)
    }

    fun toggleChannelFavorite(channelId: Long) {
        viewModelScope.launch { favoritesRepository.toggleChannel(channelId) }
    }

    fun toggleVodFavorite(vodTitle: VodTitle) {
        val contentType = if (vodTitle.isSeries) ContentType.SERIES else ContentType.MOVIE
        viewModelScope.launch { favoritesRepository.toggleVod(vodTitle.id, contentType) }
    }

    // A String set would lose the most-recent-first ordering, so the list is stored as one string
    // joined on a separator no query can contain.
    private fun readRecent(): List<String> =
        recentStore.getString(RECENT_KEY, null)
            ?.split(RECENT_SEPARATOR)
            ?.filter { it.isNotBlank() }
            ?.take(RECENT_LIMIT)
            .orEmpty()

    private fun writeRecent(values: List<String>) {
        recentStore.edit().putString(RECENT_KEY, values.joinToString(RECENT_SEPARATOR)).apply()
    }

    companion object {
        private const val RESULT_LIMIT = 30

        /** Minimum typed characters before a text search runs. Public so [SearchScreen] can show
         * a "keep typing" hint. */
        const val MIN_QUERY_LENGTH = 2
        private const val SEARCH_DEBOUNCE_MS = 250L

        /** How many results each section shows on the "All" page before the see-all row. */
        const val ALL_PAGE_SECTION_LIMIT = 6

        private const val RECENT_PREFS = "are_iptv_search"
        private const val RECENT_KEY = "search_recent"
        private const val RECENT_SEPARATOR = "\u0000"
        private const val RECENT_LIMIT = 8
    }
}
