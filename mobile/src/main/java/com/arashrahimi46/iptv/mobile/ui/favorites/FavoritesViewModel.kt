package com.arashrahimi46.iptv.mobile.ui.favorites

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arashrahimi46.iptv.data.model.Channel
import com.arashrahimi46.iptv.data.model.ContentType
import com.arashrahimi46.iptv.data.model.Favorite
import com.arashrahimi46.iptv.data.model.VodTitle
import com.arashrahimi46.iptv.data.repository.FavoritesRepository
import com.arashrahimi46.iptv.data.repository.PlaylistRepositoryImpl
import com.arashrahimi46.iptv.data.settings.ParentalFilter
import com.arashrahimi46.iptv.data.settings.UserSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class MobileFavoritesUiState(
    val hasSource: Boolean = false,
    val channels: List<Channel> = emptyList(),
    val movies: List<VodTitle> = emptyList(),
    val series: List<VodTitle> = emptyList(),
)

/** Phone Favorites: same resolution logic as :tv's FavoritesViewModel (stable-key lookup, most-
 * recently-favorited first), just without the D-pad-focused UI state. */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class FavoritesViewModel(application: Application) : AndroidViewModel(application) {
    private val favoritesRepository = FavoritesRepository(application)
    private val playlists = PlaylistRepositoryImpl(application)
    private val settings = UserSettings(application)

    private val _uiState = MutableStateFlow(MobileFavoritesUiState())
    val uiState: StateFlow<MobileFavoritesUiState> = _uiState.asStateFlow()

    /** Drives the pull-to-refresh indicator. Favourites themselves are DB-observed, so a pull
     * re-imports the active playlist -- that is what makes a stale/renamed entry resolve again. */
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        combine(
            settings.activeSourceId,
            favoritesRepository.observeAll(),
            settings.parentalFilter,
        ) { sid, favorites, parental -> Triple(sid, favorites, parental) }
            .mapLatest { (sid, favorites, parental) -> buildState(sid, favorites, parental) }
            .onEach { _uiState.value = it }
            .launchIn(viewModelScope)
    }

    /** [parental] drops adult favourites under the lock's HIDE mode -- favouriting an item before
     * the lock went on must not become a bypass around it. BLUR mode leaves
     * [com.arashrahimi46.iptv.data.settings.ParentalFilter.hideLocked] false, so the rows stay and
     * the tile obscures them instead. */
    private suspend fun buildState(
        sourceId: Long?,
        favorites: List<Favorite>,
        parental: ParentalFilter,
    ): MobileFavoritesUiState {
        if (sourceId == null) return MobileFavoritesUiState(hasSource = false)

        val channelKeys = favorites.filter { it.contentType == ContentType.LIVE }.map { it.streamKey }
        val vodKeys = favorites.filter { it.contentType != ContentType.LIVE }.map { it.streamKey }
        val channelByKey = favoritesRepository.channelsByKeys(sourceId, channelKeys)
            .filterNot { parental.hidden(it.categoryName) }
            .associateBy { FavoritesRepository.channelKey(it) }
        val vodTitles = favoritesRepository.titlesByKeys(sourceId, vodKeys)
            .filterNot { parental.hidden(it.categoryName) }
        val movieByKey = vodTitles.filter { !it.isSeries }.associateBy { FavoritesRepository.vodKey(it) }
        val seriesByKey = vodTitles.filter { it.isSeries }.associateBy { FavoritesRepository.vodKey(it) }

        val channels = favorites.filter { it.contentType == ContentType.LIVE }.mapNotNull { channelByKey[it.streamKey] }
        val movies = favorites.filter { it.contentType == ContentType.MOVIE }.mapNotNull { movieByKey[it.streamKey] }
        val series = favorites.filter { it.contentType == ContentType.SERIES }.mapNotNull { seriesByKey[it.streamKey] }

        return MobileFavoritesUiState(hasSource = true, channels = channels, movies = movies, series = series)
    }

    fun toggleChannelFavorite(channelId: Long) {
        viewModelScope.launch { favoritesRepository.toggleChannel(channelId) }
    }

    fun toggleVodFavorite(title: VodTitle) {
        val contentType = if (title.isSeries) ContentType.SERIES else ContentType.MOVIE
        viewModelScope.launch { favoritesRepository.toggleVod(title.id, contentType) }
    }

    /** Undo for the removal snackbar. Re-toggling is enough: the repository keys favourites on the
     * stable stream key, so the row comes back with its original ordering semantics. */
    fun restoreChannelFavorite(channelId: Long) = toggleChannelFavorite(channelId)

    fun restoreVodFavorite(title: VodTitle) = toggleVodFavorite(title)

    fun refresh() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            val sourceId = settings.activeSourceId.first()
            if (sourceId == null) return@launch
            _isRefreshing.value = true
            try {
                playlists.refreshSource(sourceId)
            } catch (_: Exception) {
                // A failed re-import leaves the existing favourites on screen; nothing to surface.
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}
