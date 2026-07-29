package com.arashrahimi46.iptv.mobile.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arashrahimi46.iptv.data.model.Channel
import com.arashrahimi46.iptv.data.model.VodTitle
import com.arashrahimi46.iptv.data.repository.ContinueWatchingRepository
import com.arashrahimi46.iptv.data.repository.FavoritesRepository
import com.arashrahimi46.iptv.data.repository.PlaylistRepository
import com.arashrahimi46.iptv.data.repository.PlaylistRepositoryImpl
import com.arashrahimi46.iptv.data.settings.UserSettings
import com.arashrahimi46.iptv.ui.home.HomeRailCurator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

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

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private val daySeed: Long = System.currentTimeMillis() / (24L * 60 * 60 * 1000)

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
                        // monolean: v1 continue-watching resolves movies/series titles only --
                        // series-episode/recording bookmarks need extra id resolution not yet
                        // wired on mobile (tracked as Phase 3 follow-up).
                        val cwTitleIds = cwEntries.mapNotNull { it.vodTitleId }
                        val vodPool = moviePool + seriesPool
                        val watchedWeights = channelPool.groupBy { it.categoryName ?: "" }
                            .mapValues { it.value.size.toDouble() }
                        val liveNow = HomeRailCurator.curateChannels(channelPool, watchedWeights, daySeed, limit = 20)
                        val recommended = HomeRailCurator.recommend(moviePool, seriesPool, emptyMap(), daySeed, limit = 20)
                        HomeUiState(
                            hasSource = true,
                            continueWatching = if (cwTitleIds.isEmpty()) emptyList() else repository.titlesByIds(cwTitleIds),
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
}
