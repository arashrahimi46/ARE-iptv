package com.arashrahimi46.iptv.mobile.ui.live

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.arashrahimi46.iptv.data.model.Channel
import com.arashrahimi46.iptv.data.repository.FavoritesRepository
import com.arashrahimi46.iptv.data.repository.PlaylistRepository
import com.arashrahimi46.iptv.data.repository.PlaylistRepositoryImpl
import com.arashrahimi46.iptv.data.settings.UserSettings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class LiveViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: PlaylistRepository = PlaylistRepositoryImpl(application)
    private val settings = UserSettings(application)
    private val favoritesRepo = FavoritesRepository(application)

    /** Live favorite membership for the channel row's heart toggle. */
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

    val categories: StateFlow<List<String>> = settings.activeSourceId
        .distinctUntilChanged()
        .flatMapLatest { sourceId ->
            if (sourceId == null) flowOf(emptyList()) else repository.channelCategoryCounts(sourceId).map { it.map { c -> c.name } }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val channels: Flow<PagingData<Channel>> = settings.activeSourceId
        .distinctUntilChanged()
        .flatMapLatest { sourceId ->
            category.flatMapLatest { cat ->
                if (sourceId == null) {
                    flowOf(PagingData.empty())
                } else {
                    Pager(
                        config = PagingConfig(pageSize = 80, prefetchDistance = 80, enablePlaceholders = false),
                        pagingSourceFactory = { repository.pagingChannels(sourceId, cat) },
                    ).flow
                }
            }
        }
        .cachedIn(viewModelScope)

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query
    private val _searchResults = MutableStateFlow<List<Channel>>(emptyList())
    val searchResults: StateFlow<List<Channel>> = _searchResults

    fun selectCategory(name: String?) {
        category.value = name
    }

    fun setQuery(q: String) {
        _query.value = q
        val sourceId = activeSourceId
        if (q.length < 2 || sourceId == null) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            _searchResults.value = repository.searchChannels(sourceId, q, 60)
        }
    }
}
