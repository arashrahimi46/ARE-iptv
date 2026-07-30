package com.arashrahimi46.iptv.mobile.ui.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arashrahimi46.iptv.data.model.Channel
import com.arashrahimi46.iptv.data.model.ContentType
import com.arashrahimi46.iptv.data.model.VodTitle
import com.arashrahimi46.iptv.data.repository.FavoritesRepository
import com.arashrahimi46.iptv.data.repository.PlaylistRepository
import com.arashrahimi46.iptv.data.repository.PlaylistRepositoryImpl
import com.arashrahimi46.iptv.data.settings.ParentalFilter
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

/** Search's scope chips (All / Live TV / Movies / Series) -- same scope cut as :tv's Search. */
enum class SearchScope { All, LiveTv, Movies, Series }

data class SearchUiState(
    val hasSource: Boolean = false,
    val query: String = "",
    val scope: SearchScope = SearchScope.All,
    val channelResults: List<Channel> = emptyList(),
    val titleResults: List<VodTitle> = emptyList(),
)

/**
 * Local-only search (no ranking backend, no external service): substring match over the
 * already-loaded [Channel]/[VodTitle] catalog in Room for the active source -- same data pipeline
 * as :tv's SearchViewModel, driven here by the phone's own [com.arashrahimi46.iptv.mobile.ui.components.AreTextField].
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
    private val _scope = MutableStateFlow(SearchScope.All)

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        // The field's displayed query is updated synchronously in setQuery -- never gated behind
        // the DB query -- so typing is instant. The actual search is debounced.
        val debouncedQuery = _query.debounce { q -> if (q.isEmpty()) 0L else SEARCH_DEBOUNCE_MS }
        combine(settings.activeSourceId, debouncedQuery, _scope, settings.parentalFilter) { sid, query, scope, parental ->
            SearchInputs(sid, query, scope, parental)
        }
            .mapLatest { buildResults(it) }
            .onEach { r -> _uiState.update { it.copy(hasSource = r.hasSource, channelResults = r.channels, titleResults = r.titles) } }
            .launchIn(viewModelScope)
    }

    private data class SearchInputs(val sourceId: Long?, val query: String, val scope: SearchScope, val parental: ParentalFilter)

    private data class SearchResults(val hasSource: Boolean, val channels: List<Channel>, val titles: List<VodTitle>)

    private suspend fun buildResults(inputs: SearchInputs): SearchResults {
        val (sourceId, query, scope, parental) = inputs
        if (sourceId == null) return SearchResults(hasSource = false, channels = emptyList(), titles = emptyList())
        val wantChannels = scope == SearchScope.All || scope == SearchScope.LiveTv
        val wantMovies = scope == SearchScope.All || scope == SearchScope.Movies
        val wantSeries = scope == SearchScope.All || scope == SearchScope.Series

        val trimmed = query.trim()
        if (trimmed.length < MIN_QUERY_LENGTH) return SearchResults(hasSource = true, channels = emptyList(), titles = emptyList())
        val channelResults = (if (wantChannels) repository.searchChannels(sourceId, trimmed, RESULT_LIMIT) else emptyList())
            .filterNot { parental.hidden(it.categoryName) }
        val titleResults = ((if (wantMovies) repository.searchMovies(sourceId, trimmed, RESULT_LIMIT) else emptyList()) +
            (if (wantSeries) repository.searchSeries(sourceId, trimmed, RESULT_LIMIT) else emptyList()))
            .filterNot { parental.hidden(it.categoryName) }
        return SearchResults(hasSource = true, channels = channelResults, titles = titleResults)
    }

    fun setQuery(value: String) {
        _query.value = value
        val belowMin = value.trim().length < MIN_QUERY_LENGTH
        _uiState.update {
            if (belowMin) it.copy(query = value, channelResults = emptyList(), titleResults = emptyList())
            else it.copy(query = value)
        }
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
        private const val RESULT_LIMIT = 30

        /** Minimum typed characters before a text search runs. Public so [SearchScreen] can show
         * a "keep typing" hint. */
        const val MIN_QUERY_LENGTH = 2
        private const val SEARCH_DEBOUNCE_MS = 250L
    }
}
