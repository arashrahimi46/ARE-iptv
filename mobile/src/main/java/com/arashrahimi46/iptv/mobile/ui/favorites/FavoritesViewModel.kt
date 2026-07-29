package com.arashrahimi46.iptv.mobile.ui.favorites

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arashrahimi46.iptv.data.model.Channel
import com.arashrahimi46.iptv.data.model.ContentType
import com.arashrahimi46.iptv.data.model.Favorite
import com.arashrahimi46.iptv.data.model.VodTitle
import com.arashrahimi46.iptv.data.repository.FavoritesRepository
import com.arashrahimi46.iptv.data.settings.UserSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
    private val settings = UserSettings(application)

    private val _uiState = MutableStateFlow(MobileFavoritesUiState())
    val uiState: StateFlow<MobileFavoritesUiState> = _uiState.asStateFlow()

    init {
        combine(settings.activeSourceId, favoritesRepository.observeAll()) { sid, favorites -> sid to favorites }
            .mapLatest { (sid, favorites) -> buildState(sid, favorites) }
            .onEach { _uiState.value = it }
            .launchIn(viewModelScope)
    }

    private suspend fun buildState(sourceId: Long?, favorites: List<Favorite>): MobileFavoritesUiState {
        if (sourceId == null) return MobileFavoritesUiState(hasSource = false)

        val channelKeys = favorites.filter { it.contentType == ContentType.LIVE }.map { it.streamKey }
        val vodKeys = favorites.filter { it.contentType != ContentType.LIVE }.map { it.streamKey }
        val channelByKey = favoritesRepository.channelsByKeys(sourceId, channelKeys)
            .associateBy { FavoritesRepository.channelKey(it) }
        val vodTitles = favoritesRepository.titlesByKeys(sourceId, vodKeys)
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
}
