package com.arashrahimi46.iptv.ui.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arashrahimi46.iptv.data.model.Channel
import com.arashrahimi46.iptv.data.model.VodTitle
import com.arashrahimi46.iptv.data.repository.PlaylistRepository
import com.arashrahimi46.iptv.data.repository.PlaylistRepositoryImpl
import com.arashrahimi46.iptv.data.settings.UserSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

data class SearchUiState(
    val hasSource: Boolean = false,
    val query: String = "",
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

    private data class Catalog(val channels: List<Channel>, val movies: List<VodTitle>, val series: List<VodTitle>)

    private val _query = MutableStateFlow("")

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
        combine(catalog, _query) { c, query -> c to query }
            .onEach { (c, query) -> _uiState.value = buildState(c, query) }
            .launchIn(viewModelScope)
    }

    private fun buildState(catalog: Catalog?, query: String): SearchUiState {
        if (catalog == null) return SearchUiState(hasSource = false, query = query)
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
        _query.value = value
    }

    companion object {
        fun factory(app: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = SearchViewModel(app) as T
            }
    }
}
