package com.arashrahimi46.iptv.mobile.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arashrahimi46.iptv.mobile.data.db.AppDatabase
import com.arashrahimi46.iptv.mobile.data.model.Channel
import com.arashrahimi46.iptv.mobile.data.model.ContentType
import com.arashrahimi46.iptv.mobile.data.model.ContinueWatchingEntry
import com.arashrahimi46.iptv.mobile.data.model.VodTitle
import com.arashrahimi46.iptv.mobile.data.repository.ContinueWatchingRepository
import com.arashrahimi46.iptv.mobile.data.repository.FavoritesRepository
import com.arashrahimi46.iptv.mobile.data.repository.PlaylistRepository
import com.arashrahimi46.iptv.mobile.data.repository.PlaylistRepositoryImpl
import com.arashrahimi46.iptv.mobile.data.settings.ParentalFilter
import com.arashrahimi46.iptv.mobile.data.settings.UserSettings
import com.arashrahimi46.iptv.mobile.ui.home.HomeRailCurator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
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
    /** For a series entry in [continueWatching] (keyed by its resolved parent series-title id),
     * the specific episode to resume -- so tapping it jumps straight back into that episode
     * instead of re-opening the season/episode picker the user already got past. */
    val continueWatchingEpisodeIds: Map<Long, Long> = emptyMap(),
    /** 0f..1f watched fraction per [continueWatching] title id, for the tile's progress bar. */
    val continueWatchingProgress: Map<Long, Float> = emptyMap(),
    val favoriteChannels: List<Channel> = emptyList(),
    val favoriteTitles: List<VodTitle> = emptyList(),
    val recommended: List<VodTitle> = emptyList(),
    val liveNow: List<Channel> = emptyList(),
    val isLoading: Boolean = true,
) {
    /** Every rail is empty -- Home renders its "no playlist / nothing yet" state instead. */
    val isEmpty: Boolean
        get() = continueWatching.isEmpty() && liveNow.isEmpty() && recommended.isEmpty() &&
            favoriteChannels.isEmpty() && favoriteTitles.isEmpty()
}

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

    private val _isRefreshing = MutableStateFlow(false)
    /** Drives the pull-to-refresh spinner. */
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    /**
     * Pull-to-refresh: re-imports the active playlist. The rails themselves are Room-backed flows,
     * so they repaint on their own once the import lands -- this only has to run the fetch and hold
     * the spinner. A failed refresh leaves the existing catalog intact (see
     * [PlaylistRepository.refreshSource]), so there is nothing to surface but the spinner settling.
     */
    fun refresh() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val sourceId = settings.activeSourceId.first()
                if (sourceId != null) repository.refreshSource(sourceId)
            } catch (_: Exception) {
                // Network/parse failure -- the catalog on screen is still the last good one.
            } finally {
                _isRefreshing.value = false
            }
        }
    }

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

                    combine(pools, engagement, settings.parentalFilter) { poolTriple, engagementTriple, parental ->
                        Triple(poolTriple, engagementTriple, parental)
                    }.mapLatest { (poolTriple, engagementTriple, parental) ->
                        val (rawChannelPool, rawMoviePool, rawSeriesPool) = poolTriple
                        val (cwEntries, favChannelIds, favVodIds) = engagementTriple
                        // Parental lock, HIDE mode: strip adult items from every pool BEFORE the
                        // curator ranks them, so a hidden item can't consume a rail slot and leave
                        // a short rail behind (same ordering :tv's HomeViewModel uses). BLUR mode
                        // leaves ParentalFilter.hideLocked false -- those items stay and are
                        // obscured at the tile via LocalParentalBlur instead.
                        val channelPool = rawChannelPool.filterNot { parental.hidden(it.categoryName) }
                        val moviePool = rawMoviePool.filterNot { parental.hidden(it.categoryName) }
                        val seriesPool = rawSeriesPool.filterNot { parental.hidden(it.categoryName) }
                        // recordingId bookmarks stay out of scope (recording playback isn't wired
                        // on mobile yet); vodTitleId (movies) and seriesEpisodeId (series, resolved
                        // to their parent series title) both resolve.
                        val vodPool = moviePool + seriesPool
                        // Continue-watching resolves straight from Room, not from the sampled
                        // pools, so it needs the same filter applied at its own source.
                        val continueWatching = resolveContinueWatching(cwEntries, sourceId, parental)
                        // Resolved by id from Room, NOT by filtering the sampled pools. The pools
                        // are a bounded 200-row hashed curation sample, so any favorite outside
                        // that sample simply never appeared -- the rail silently under-reported,
                        // and on a large catalog it could come up empty for a user who definitely
                        // has favorites. Same reason continue-watching resolves from Room above.
                        val favoriteChannels = repository.channelsByIds(favChannelIds.toList())
                            .filter { it.sourceId == sourceId && !parental.hidden(it.categoryName) }
                            .take(20)
                        val favoriteTitles = repository.titlesByIds(favVodIds.toList())
                            .filter { it.sourceId == sourceId && !parental.hidden(it.categoryName) }
                            .take(20)
                        // Real taste signal, from what this user actually watches and favourites.
                        // `recommend` was being handed an EMPTY map, which zeroes the 0.6 category
                        // term inside curateTitles and collapses "For You" to rating + year +
                        // daily jitter -- byte-identical for every user of the app.
                        val vodWeights = categoryWeights(
                            continueWatching.titles.map { it.categoryName } + favoriteTitles.map { it.categoryName },
                        )
                        // Live had a weight map, but it counted how many channels each category
                        // CONTAINS, so it just floated the provider's fattest bundle to the top and
                        // called it personalisation. Prefer favourites; fall back to that density
                        // heuristic only when there's no signal yet, which at least beats raw order.
                        val channelWeights = categoryWeights(favoriteChannels.map { it.categoryName })
                            .ifEmpty {
                                channelPool.groupBy { it.categoryName ?: "" }
                                    .mapValues { it.value.size.toDouble() }
                            }
                        val liveNow = HomeRailCurator.curateChannels(channelPool, channelWeights, daySeed, limit = 20)
                        val recommended = HomeRailCurator.recommend(moviePool, seriesPool, vodWeights, daySeed, limit = 20)
                        HomeUiState(
                            hasSource = true,
                            continueWatching = continueWatching.titles,
                            continueWatchingEpisodeIds = continueWatching.episodeIdsByTitleId,
                            continueWatchingProgress = continueWatching.progressByTitleId,
                            favoriteChannels = favoriteChannels,
                            favoriteTitles = favoriteTitles,
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

    /**
     * Counts how often each category shows up in the user's engagement, normalized so the most-
     * engaged category is 1.0. `curateTitles`/`curateChannels` mix this in at 0.6 against rating
     * (0.25) and recency (0.15), so the values only need to be relative to each other. Blank/absent
     * categories are dropped rather than bucketed under "", which would otherwise become the single
     * heaviest "category" on any playlist with sloppy group tags.
     */
    private fun categoryWeights(categories: List<String?>): Map<String, Double> {
        val counts = categories.filterNot { it.isNullOrBlank() }
            .groupingBy { it!! }
            .eachCount()
        val top = counts.values.maxOrNull()?.toDouble() ?: return emptyMap()
        return counts.mapValues { it.value / top }
    }

    private data class ResolvedContinueWatching(
        val titles: List<VodTitle>,
        val episodeIdsByTitleId: Map<Long, Long>,
        val progressByTitleId: Map<Long, Float>,
    )

    /** Resolves raw [ContinueWatchingEntry] rows to [VodTitle]s for the rail -- a movie resolves
     * directly by [ContinueWatchingEntry.vodTitleId]; a series episode resolves via its
     * [com.arashrahimi46.iptv.mobile.data.model.SeriesEpisode.seriesTitleId] to the parent series title
     * (mirrors :tv's HomeViewModel), and its episode id is kept alongside so the rail can resume
     * that exact episode (see [HomeUiState.continueWatchingEpisodeIds]) instead of only reopening
     * the series' episode picker. Only titles in [activeSourceId] resolve; unresolvable entries
     * (deleted title, other source, or a [ContinueWatchingEntry.recordingId] bookmark -- out of
     * mobile v1 scope) are dropped rather than shown blank, as is anything [parental] hides. */
    private suspend fun resolveContinueWatching(
        entries: List<ContinueWatchingEntry>,
        activeSourceId: Long?,
        parental: ParentalFilter,
    ): ResolvedContinueWatching {
        val vodIds = entries.mapNotNull { it.vodTitleId }
        val episodeIds = entries.mapNotNull { it.seriesEpisodeId }
        val episodesById = episodeIds.mapNotNull { db.seriesEpisodeDao().getById(it) }.associateBy { it.id }
        val seriesIds = (vodIds + episodesById.values.map { it.seriesTitleId }).distinct()
        val titlesById = repository.titlesByIds(seriesIds)
            .filter { it.sourceId == activeSourceId && !parental.hidden(it.categoryName) }
            .associateBy { it.id }

        val titles = mutableListOf<VodTitle>()
        val episodeIdsByTitleId = mutableMapOf<Long, Long>()
        val progressByTitleId = mutableMapOf<Long, Float>()
        // One row per TITLE, not per entry. Two in-progress episodes of the same series (or a movie
        // bookmarked twice) both resolve to the same VodTitle, and the rail renders with
        // `key = { it.id }` -- a repeated id makes Compose throw "Key was already used" and takes
        // the whole Home screen down. `entries` arrives newest-first, so keeping the first
        // occurrence keeps the most recent progress, which is also what the maps below already did.
        val seenTitleIds = mutableSetOf<Long>()
        entries.forEach { entry ->
            when {
                entry.vodTitleId != null -> titlesById[entry.vodTitleId]?.let {
                    if (seenTitleIds.add(it.id)) {
                        titles += it
                        progressByTitleId[it.id] = entry.watchedFraction()
                    }
                }
                entry.seriesEpisodeId != null -> {
                    val episode = episodesById[entry.seriesEpisodeId] ?: return@forEach
                    val title = titlesById[episode.seriesTitleId] ?: return@forEach
                    if (seenTitleIds.add(title.id)) {
                        titles += title
                        episodeIdsByTitleId[title.id] = episode.id
                        progressByTitleId[title.id] = entry.watchedFraction()
                    }
                }
            }
        }
        return ResolvedContinueWatching(titles, episodeIdsByTitleId, progressByTitleId)
    }
}

/** 0f..1f watched fraction; 0f when the duration is unknown (a live/unseekable bookmark). */
private fun ContinueWatchingEntry.watchedFraction(): Float =
    if (durationMs <= 0L) 0f else (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
