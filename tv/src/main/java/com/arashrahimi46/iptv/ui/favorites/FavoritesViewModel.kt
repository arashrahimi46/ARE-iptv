package com.arashrahimi46.iptv.ui.favorites

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
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

data class FavoritesUiState(
    val hasSource: Boolean = false,
    /** Channels tab -- favorited [Channel]s, most-recently-favorited first (see [FavoritesViewModel] doc). */
    val channels: List<Channel> = emptyList(),
    /** Movies tab -- favorited movie [VodTitle]s, most-recently-favorited first. */
    val movies: List<VodTitle> = emptyList(),
    /** Series tab -- favorited series [VodTitle]s, most-recently-favorited first. */
    val series: List<VodTitle> = emptyList(),
)

/**
 * Real Favorites screen state, entirely player-independent: observes
 * [FavoritesRepository]'s Room-backed [Favorite] rows directly (joined
 * against the active source's [Channel]/[VodTitle] catalog, the same scoping
 * every other browse screen already uses) -- never a fake/optimistic list.
 *
 * "Smart favorites" heuristic: there's no watch-history data yet
 * ([com.arashrahimi46.iptv.data.model.ContinueWatchingEntry] is still an
 * unpopulated Phase 1 schema stub), so a genuine "recently watched" ranking
 * isn't possible without fabricating data. Instead every tab here sorts by
 * [Favorite.addedAtMs] descending -- "most recently favorited", a real,
 * legitimate signal the schema already supports. See report for this call.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class FavoritesViewModel(app: Application) : AndroidViewModel(app) {
    private val favoritesRepository = FavoritesRepository(app)
    private val settings = UserSettings(app)

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    init {
        // Resolve only the favorited stable keys to rows instead of loading the whole
        // catalog to filter it -- favorites are few, the catalog can be 100k+/300k+ rows.
        combine(settings.activeSourceId, favoritesRepository.observeAll()) { sid, favorites -> sid to favorites }
            .mapLatest { (sid, favorites) -> buildState(sid, favorites) }
            .onEach { _uiState.value = it }
            .launchIn(viewModelScope)
    }

    private suspend fun buildState(sourceId: Long?, favorites: List<Favorite>): FavoritesUiState {
        if (sourceId == null) return FavoritesUiState(hasSource = false)

        // favorites is already ORDER BY addedAtMs DESC (see FavoriteDao.observeAll) -- "most recently favorited first".
        // Resolve each favorite's *stable* streamKey to the active source's current row (see Favorite doc);
        // favorites whose item is no longer in this source resolve to null and are silently dropped.
        val channelKeys = favorites.filter { it.contentType == ContentType.LIVE }.map { it.streamKey }
        val vodKeys = favorites.filter { it.contentType != ContentType.LIVE }.map { it.streamKey }
        val channelByKey = favoritesRepository.channelsByKeys(sourceId, channelKeys)
            .associateBy { FavoritesRepository.channelKey(it) }
        // Xtream vod_id and series_id are independent, overlapping id spaces, so a movie and a
        // series can share the same key -- keep them in SEPARATE maps (partitioned by isSeries)
        // so a MOVIE favorite resolves only to a movie row and a SERIES favorite only to a series row.
        val vodTitles = favoritesRepository.titlesByKeys(sourceId, vodKeys)
        val movieByKey = vodTitles.filter { !it.isSeries }.associateBy { FavoritesRepository.vodKey(it) }
        val seriesByKey = vodTitles.filter { it.isSeries }.associateBy { FavoritesRepository.vodKey(it) }

        val channels = favorites
            .filter { it.contentType == ContentType.LIVE }
            .mapNotNull { channelByKey[it.streamKey] }
        val movies = favorites
            .filter { it.contentType == ContentType.MOVIE }
            .mapNotNull { movieByKey[it.streamKey] }
        val series = favorites
            .filter { it.contentType == ContentType.SERIES }
            .mapNotNull { seriesByKey[it.streamKey] }

        return FavoritesUiState(hasSource = true, channels = channels, movies = movies, series = series)
    }

    fun toggleChannelFavorite(channelId: Long) {
        viewModelScope.launch { favoritesRepository.toggleChannel(channelId) }
    }

    fun toggleVodFavorite(title: VodTitle) {
        val contentType = if (title.isSeries) ContentType.SERIES else ContentType.MOVIE
        viewModelScope.launch { favoritesRepository.toggleVod(title.id, contentType) }
    }

    companion object {
        fun factory(app: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = FavoritesViewModel(app) as T
            }
    }
}
