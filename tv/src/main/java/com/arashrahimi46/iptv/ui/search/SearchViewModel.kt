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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SearchUiState(
    val hasSource: Boolean = false,
    val query: String = "",
    /** Set when arriving from Home's "Browse by category" cards (see SearchScreen) --
     * an exact categoryName match, distinct from [query]'s substring-on-name search. */
    val categoryFilter: String? = null,
    val channelResults: List<Channel> = emptyList(),
    val titleResults: List<VodTitle> = emptyList(),
)

/**
 * Local-only search (no ranking backend, no external service, per spec):
 * substring match over the already-loaded [Channel]/[VodTitle] catalog in
 * Room for the active source. Query text is driven by [AreOnScreenKeyboard]
 * button presses (append/backspace), not hardware-keyboard IME input.
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

    private data class Catalog(val channels: List<Channel>, val movies: List<VodTitle>, val series: List<VodTitle>)

    private val _query = MutableStateFlow("")
    private val _categoryFilter = MutableStateFlow<String?>(null)

    private val catalog = settings.activeSourceId.flatMapLatest { sourceId ->
        if (sourceId == null) {
            flowOf<Catalog?>(null)
        } else {
            combine(repository.observeChannels(sourceId), repository.observeMovies(sourceId), repository.observeSeries(sourceId)) { c, m, s ->
                Catalog(c, m, s)
            }
        }
    }

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        combine(catalog, _query, _categoryFilter) { c, query, categoryFilter -> Triple(c, query, categoryFilter) }
            .onEach { (c, query, categoryFilter) -> _uiState.value = buildState(c, query, categoryFilter) }
            .launchIn(viewModelScope)
    }

    private fun buildState(catalog: Catalog?, query: String, categoryFilter: String?): SearchUiState {
        if (catalog == null) return SearchUiState(hasSource = false, query = query, categoryFilter = categoryFilter)
        if (categoryFilter != null) {
            val channelResults = catalog.channels.filter { it.categoryName == categoryFilter }.take(30)
            val titleResults = (catalog.movies + catalog.series).filter { it.categoryName == categoryFilter }.take(30)
            return SearchUiState(hasSource = true, query = query, categoryFilter = categoryFilter, channelResults = channelResults, titleResults = titleResults)
        }
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return SearchUiState(hasSource = true, query = query)
        val channelResults = catalog.channels.filter { it.name.contains(trimmed, ignoreCase = true) }.take(30)
        val titleResults = (catalog.movies + catalog.series)
            .filter { it.name.contains(trimmed, ignoreCase = true) }
            .take(30)
        return SearchUiState(hasSource = true, query = query, channelResults = channelResults, titleResults = titleResults)
    }

    fun appendCharacter(char: Char) {
        _query.value += char
    }

    fun appendSpace() {
        _query.value += " "
    }

    fun backspace() {
        _query.value = _query.value.dropLast(1)
    }

    fun clear() {
        _query.value = ""
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

    fun toggleChannelFavorite(channelId: Long) {
        viewModelScope.launch { favoritesRepository.toggleChannel(channelId) }
    }

    fun toggleVodFavorite(vodTitle: VodTitle) {
        val contentType = if (vodTitle.isSeries) ContentType.SERIES else ContentType.MOVIE
        viewModelScope.launch { favoritesRepository.toggleVod(vodTitle.id, contentType) }
    }

    companion object {
        fun factory(app: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = SearchViewModel(app) as T
            }
    }
}
