package com.arashrahimi46.iptv.mobile.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arashrahimi46.iptv.data.db.AppDatabase
import com.arashrahimi46.iptv.data.model.Channel
import com.arashrahimi46.iptv.data.model.ContentType
import com.arashrahimi46.iptv.data.model.ContinueWatchingEntry
import com.arashrahimi46.iptv.data.model.VodTitle
import com.arashrahimi46.iptv.data.repository.ContinueWatchingRepository
import com.arashrahimi46.iptv.data.repository.FavoritesRepository
import com.arashrahimi46.iptv.data.repository.PlaylistRepository
import com.arashrahimi46.iptv.data.repository.PlaylistRepositoryImpl
import com.arashrahimi46.iptv.data.settings.UserSettings
import com.arashrahimi46.iptv.ui.home.HomeRailCurator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val hasSource: Boolean = true,
    val continueWatching: List<VodTitle> = emptyList(),
    val favoriteChannels: List<Channel> = emptyList(),
    val favoriteTitles: List<VodTitle> = emptyList(),
    val recommended: List<VodTitle> = emptyList(),
    val liveNow: List<Channel> = emptyList(),
    val isLoading: Boolean = true,
)

private const val SAMPLE_SIZE = 200

/** Phone Home: continue-watching, favorites, and a personalized "Recommended" rail reusing :tv's
 * [HomeRailCurator] (pure logic, no TV/Compose deps) so both apps rank rails identically. */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: PlaylistRepository = PlaylistRepositoryImpl(application)
    private val settings = UserSettings(application)
    private val continueWatchingRepo = ContinueWatchingRepository(application)
    private val favoritesRepo = FavoritesRepository(application)
    private val db = AppDatabase.get(application)

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    /** Live favorite membership for the tile heart toggles (Continue Watching/For You/Live now rails). */
    val favoriteChannelIds: StateFlow<Set<Long>> =
        favoritesRepo.favoriteChannelIds.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())
    val favoriteVodIds: StateFlow<Set<Long>> =
        favoritesRepo.favoriteVodIds.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    private val daySeed: Long = System.currentTimeMillis() / (24L * 60 * 60 * 1000)

    fun toggleChannelFavorite(channel: Channel) {
        viewModelScope.launch { favoritesRepo.toggleChannel(channel.id) }
    }

    fun toggleTitleFavorite(title: VodTitle) {
        viewModelScope.launch {
            favoritesRepo.toggleVod(title.id, if (title.isSeries) ContentType.SERIES else ContentType.MOVIE)
        }
    }

    init {
        settings.activeSourceId
            .distinctUntilChanged()
            .flatMapLatest { sourceId ->
                if (sourceId == null) {
                    flowOf(HomeUiState(hasSource = false, isLoading = false))
                } else {
                    val pools = combine(
                        repository.sampleChannels(sourceId, SAMPLE_SIZE),
                        repository.sampleMovies(sourceId, SAMPLE_SIZE),
                        repository.sampleSeries(sourceId, SAMPLE_SIZE),
                    ) { c, m, s -> Triple(c, m, s) }
                    val engagement = combine(
                        continueWatchingRepo.observeRecent(20),
                        favoritesRepo.favoriteChannelIds,
                        favoritesRepo.favoriteVodIds,
                    ) { cw, favCh, favVod -> Triple(cw, favCh, favVod) }

                    combine(pools, engagement) { (channelPool, moviePool, seriesPool), (cwEntries, favChannelIds, favVodIds) ->
                        Triple(channelPool, moviePool, seriesPool) to Triple(cwEntries, favChannelIds, favVodIds)
                    }.mapLatest { (poolTriple, engagementTriple) ->
                        val (channelPool, moviePool, seriesPool) = poolTriple
                        val (cwEntries, favChannelIds, favVodIds) = engagementTriple
                        // recordingId bookmarks stay out of scope (recording playback isn't wired
                        // on mobile yet); vodTitleId (movies) and seriesEpisodeId (series, resolved
                        // to their parent series title) both resolve.
                        val vodPool = moviePool + seriesPool
                        val watchedWeights = channelPool.groupBy { it.categoryName ?: "" }
                            .mapValues { it.value.size.toDouble() }
                        val liveNow = HomeRailCurator.curateChannels(channelPool, watchedWeights, daySeed, limit = 20)
                        val recommended = HomeRailCurator.recommend(moviePool, seriesPool, emptyMap(), daySeed, limit = 20)
                        HomeUiState(
                            hasSource = true,
                            continueWatching = resolveContinueWatching(cwEntries, sourceId),
                            favoriteChannels = channelPool.filter { it.id in favChannelIds }.take(20),
                            favoriteTitles = vodPool.filter { it.id in favVodIds }.take(20),
                            recommended = recommended,
                            liveNow = liveNow,
                            isLoading = false,
                        )
                    }
                }
            }
            .onEach { _state.value = it }
            .launchIn(viewModelScope)
    }

    /** Resolves raw [ContinueWatchingEntry] rows to [VodTitle]s for the rail -- a movie resolves
     * directly by [ContinueWatchingEntry.vodTitleId]; a series episode resolves via its
     * [com.arashrahimi46.iptv.data.model.SeriesEpisode.seriesTitleId] to the parent series title
     * (mirrors :tv's HomeViewModel). Only titles in [activeSourceId] resolve; unresolvable entries
     * (deleted title, other source, or a [ContinueWatchingEntry.recordingId] bookmark -- out of
     * mobile v1 scope) are dropped rather than shown blank. */
    private suspend fun resolveContinueWatching(entries: List<ContinueWatchingEntry>, activeSourceId: Long?): List<VodTitle> {
        val vodIds = entries.mapNotNull { it.vodTitleId }
        val episodeIds = entries.mapNotNull { it.seriesEpisodeId }
        val episodesById = episodeIds.mapNotNull { db.seriesEpisodeDao().getById(it) }.associateBy { it.id }
        val seriesIds = (vodIds + episodesById.values.map { it.seriesTitleId }).distinct()
        val titlesById = repository.titlesByIds(seriesIds)
            .filter { it.sourceId == activeSourceId }
            .associateBy { it.id }

        return entries.mapNotNull { entry ->
            when {
                entry.vodTitleId != null -> titlesById[entry.vodTitleId]
                entry.seriesEpisodeId != null -> episodesById[entry.seriesEpisodeId]?.let { titlesById[it.seriesTitleId] }
                else -> null
            }
        }
    }
}
