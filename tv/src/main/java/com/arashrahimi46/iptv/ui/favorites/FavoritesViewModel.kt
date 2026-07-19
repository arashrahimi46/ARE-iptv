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
import kotlinx.coroutines.launch

/** A favorited channel or VOD title resolved to its real row, for the mixed Sports/Kids tabs. */
sealed class FavoriteContent {
    data class ChannelItem(val channel: Channel) : FavoriteContent()
    data class TitleItem(val title: VodTitle) : FavoriteContent()
}

data class FavoritesUiState(
    val hasSource: Boolean = false,
    /** Channels tab -- favorited [Channel]s, most-recently-favorited first (see [FavoritesViewModel] doc). */
    val channels: List<Channel> = emptyList(),
    /** Movies tab -- favorited non-series [VodTitle]s, most-recently-favorited first. */
    val movies: List<VodTitle> = emptyList(),
    /** Sports tab -- favorited channels/movies/series whose category name contains "sport". */
    val sports: List<FavoriteContent> = emptyList(),
    /** Kids tab -- favorited movies/series whose category name contains "kid". */
    val kids: List<FavoriteContent> = emptyList(),
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
    private val repository: PlaylistRepository = PlaylistRepositoryImpl(app)
    private val favoritesRepository = FavoritesRepository(app)
    private val settings = UserSettings(app)

    private data class Catalog(val channels: List<Channel>, val movies: List<VodTitle>, val series: List<VodTitle>)

    private val catalog = settings.activeSourceId.flatMapLatest { sourceId ->
        if (sourceId == null) {
            flowOf<Catalog?>(null)
        } else {
            combine(repository.observeChannels(sourceId), repository.observeMovies(sourceId), repository.observeSeries(sourceId)) { c, m, s ->
                Catalog(c, m, s)
            }
        }
    }

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    init {
        combine(catalog, favoritesRepository.observeAll()) { cat, favorites -> cat to favorites }
            .onEach { (cat, favorites) -> _uiState.value = buildState(cat, favorites) }
            .launchIn(viewModelScope)
    }

    private fun buildState(catalog: Catalog?, favorites: List<Favorite>): FavoritesUiState {
        if (catalog == null) return FavoritesUiState(hasSource = false)

        // favorites is already ORDER BY addedAtMs DESC (see FavoriteDao.observeAll) -- "most recently favorited first".
        val channelById = catalog.channels.associateBy { it.id }
        val vodById = (catalog.movies + catalog.series).associateBy { it.id }

        val channels = favorites.mapNotNull { fav -> fav.channelId?.let(channelById::get) }
        val movies = favorites
            .filter { it.contentType == ContentType.MOVIE }
            .mapNotNull { fav -> fav.vodTitleId?.let(vodById::get) }

        val resolved: List<FavoriteContent> = favorites.mapNotNull { fav ->
            fav.channelId?.let(channelById::get)?.let { FavoriteContent.ChannelItem(it) }
                ?: fav.vodTitleId?.let(vodById::get)?.let { FavoriteContent.TitleItem(it) }
        }
        fun categoryOf(item: FavoriteContent): String? = when (item) {
            is FavoriteContent.ChannelItem -> item.channel.categoryName
            is FavoriteContent.TitleItem -> item.title.categoryName
        }
        val sports = resolved.filter { categoryOf(it)?.contains("sport", ignoreCase = true) == true }
        val kids = resolved.filter { categoryOf(it)?.contains("kid", ignoreCase = true) == true }

        return FavoritesUiState(hasSource = true, channels = channels, movies = movies, sports = sports, kids = kids)
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
