package com.arashrahimi46.iptv.mobile.ui.live

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import com.arashrahimi46.iptv.data.model.Channel
import com.arashrahimi46.iptv.data.repository.FavoritesRepository
import com.arashrahimi46.iptv.data.repository.PlaylistRepository
import com.arashrahimi46.iptv.data.repository.PlaylistRepositoryImpl
import com.arashrahimi46.iptv.data.settings.UserSettings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class LiveViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: PlaylistRepository = PlaylistRepositoryImpl(application)
    private val settings = UserSettings(application)
    private val favoritesRepo = FavoritesRepository(application)

    /** Live favorite membership for the channel tile's heart toggle. */
    val favoriteChannelIds: StateFlow<Set<Long>> =
        favoritesRepo.favoriteChannelIds.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    fun toggleFavorite(channel: Channel) {
        viewModelScope.launch { favoritesRepo.toggleChannel(channel.id) }
    }

    private val category = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = category

    private val activeSourceIdState: StateFlow<Long?> = settings.activeSourceId
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    private val activeSourceId: Long? get() = activeSourceIdState.value

    /** False only while no playlist is active -- drives the "add a playlist" empty state. */
    val hasSource: StateFlow<Boolean> = activeSourceIdState
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /** Parental lock, HIDE mode: an adult category is absent from the chip strip entirely. */
    val categories: StateFlow<List<String>> = combine(
        settings.activeSourceId.distinctUntilChanged(),
        settings.parentalFilter,
    ) { sourceId, parental -> sourceId to parental }
        .flatMapLatest { (sourceId, parental) ->
            if (sourceId == null) {
                flowOf(emptyList())
            } else {
                repository.channelCategoryCounts(sourceId)
                    .map { counts -> counts.map { c -> c.name }.filterNot { parental.hidden(it) } }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val channels: Flow<PagingData<Channel>> = combine(
        settings.activeSourceId.distinctUntilChanged(),
        settings.parentalFilter,
    ) { sourceId, parental -> sourceId to parental }
        .flatMapLatest { (sourceId, parental) ->
            category.flatMapLatest { cat ->
                if (sourceId == null || parental.hidden(cat)) {
                    // A hidden category can still be *selected* (it was picked before the lock went
                    // on, or the lock re-engaged on relock) -- serve nothing rather than its rows.
                    flowOf(PagingData.empty())
                } else {
                    Pager(
                        config = PagingConfig(pageSize = 80, prefetchDistance = 80, enablePlaceholders = false),
                        pagingSourceFactory = { repository.pagingChannels(sourceId, cat) },
                    ).flow
                        // "All" (cat == null) pages across every category, so adult rows have to be
                        // dropped per item; Room can't express the keyword heuristic in SQL.
                        .map { data -> data.filter { !parental.hidden(it.categoryName) } }
                }
            }
        }
        .cachedIn(viewModelScope)

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    /**
     * Debounced by [SEARCH_DEBOUNCE_MS] -- the same window `SearchViewModel` uses -- so typing does
     * not put one `LIKE` scan per keystroke on the Room dispatcher. Clearing the field settles with
     * no delay because a too-short query needs no round-trip at all.
     */
    val searchResults: StateFlow<List<Channel>> =
        combine(
            _query.debounce { q -> if (q.length < MIN_QUERY_LENGTH) 0L else SEARCH_DEBOUNCE_MS },
            activeSourceIdState,
            settings.parentalFilter,
        ) { q, sourceId, parental -> Triple(q, sourceId, parental) }
            .flatMapLatest { (q, sourceId, parental) ->
                if (q.length < MIN_QUERY_LENGTH || sourceId == null) {
                    flowOf(emptyList())
                } else {
                    // Search is a second way into the catalog -- without this a hidden category is
                    // one typed word away from being on screen anyway.
                    flow { emit(repository.searchChannels(sourceId, q, SEARCH_LIMIT).filterNot { parental.hidden(it.categoryName) }) }
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    fun selectCategory(name: String?) {
        category.value = name
    }

    fun setQuery(q: String) {
        _query.value = q
    }

    /** Pull-to-refresh: re-imports the active source; Room invalidation re-pages the grid. */
    fun refresh() {
        val sourceId = activeSourceId ?: return
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                repository.refreshSource(sourceId)
            } catch (_: Exception) {
                // A failed refresh leaves the already-imported catalog on screen, which is the
                // useful outcome; load failures surface through the paging grid's error state.
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 250L
        const val MIN_QUERY_LENGTH = 2
        const val SEARCH_LIMIT = 60
    }
}
