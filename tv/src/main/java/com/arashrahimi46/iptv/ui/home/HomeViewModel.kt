package com.arashrahimi46.iptv.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arashrahimi46.iptv.data.db.AppDatabase
import com.arashrahimi46.iptv.data.model.Channel
import com.arashrahimi46.iptv.data.model.ContinueWatchingEntry
import com.arashrahimi46.iptv.data.model.VodTitle
import com.arashrahimi46.iptv.data.repository.ContinueWatchingRepository
import com.arashrahimi46.iptv.data.repository.EpgRepository
import com.arashrahimi46.iptv.data.repository.PlaylistRepository
import com.arashrahimi46.iptv.data.repository.PlaylistRepositoryImpl
import com.arashrahimi46.iptv.data.settings.UserSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach

data class HomeCategorySummary(val name: String, val count: Int)

/**
 * One resolved Continue Watching rail entry (P1.2) -- a [ContinueWatchingEntry] joined against
 * its real [VodTitle] (movie, or the series a bookmarked episode belongs to) so the Home rail
 * has real title/poster/progress, not just the raw id-pair the entry stores. Resuming always
 * targets the exact bookmarked item ([vodTitleId] for a movie, [seriesEpisodeId] for an episode)
 * -- never the series' first episode.
 */
data class HomeContinueWatchingItem(
    val vodTitleId: Long?,
    val seriesEpisodeId: Long?,
    val title: String,
    val posterUrl: String?,
    val meta: String?,
    val progress: Float,
)

data class HomeUiState(
    val hasSource: Boolean = false,
    val channels: List<Channel> = emptyList(),
    val movies: List<VodTitle> = emptyList(),
    val series: List<VodTitle> = emptyList(),
    val categories: List<HomeCategorySummary> = emptyList(),
    /** P1.2: most-recently-updated resume bookmarks, resolved to real titles/posters. Source-
     * independent (same reasoning as Favorites -- vod/episode ids are globally unique), so this
     * isn't gated behind [hasSource]/[isInitializing] like the rest of this state. */
    val continueWatching: List<HomeContinueWatchingItem> = emptyList(),
    /** QA LOW defect: a real source existed, but Room hadn't emitted its first catalog read
     * yet (cold-start DB open on a large catalog took 1.5-7s in QA's test) -- [hasSource]'s
     * default-constructed `false` was indistinguishable from "confirmed no source", flashing
     * "No playlist yet" during that window. True only before the very first emission below;
     * every real emission (source or no-source) sets it false, whether or not the catalog
     * itself turns out empty. */
    val isInitializing: Boolean = true,
)

/**
 * Reads the active playlist's catalog from [PlaylistRepository] and shapes it
 * into the Home rails (Home.jsx). Degrades gracefully to empty lists when
 * there's no active source or an empty catalog -- a fresh install is a real
 * state, not an error.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val repository: PlaylistRepository = PlaylistRepositoryImpl(app)
    private val settings = UserSettings(app)
    private val epgRepository = EpgRepository(app)
    private val continueWatchingRepository = ContinueWatchingRepository(app)
    private val db = AppDatabase.get(app)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /** Per-rail preview size -- Home shows a "browse for more" affordance, never the full catalog. */
    private val RAIL_LIMIT = 40

    private val _nowPlayingTitles = MutableStateFlow<Map<Long, String>>(emptyMap())
    /** channelId -> current EPG programme title, for the "Live now" rail. Issue #15: the rail
     * previously showed `channel.categoryName` in the "now playing" slot -- never a program
     * title at all (a resolution-tag string like "1080" embedded in some playlists' channel
     * names would be exactly as wrong). Recomputed whenever the channel catalog/EPG data
     * changes (Room Flow), not on a clock tick -- matches product-lead's "no live-ticking
     * recompute" ruling on the EPG timezone fix. Channels absent from this map render with no
     * "now playing" line in the UI -- per product decision, "no title" is an acceptable
     * fallback state, but falling back to categoryName (or any other non-program-name field)
     * is not, since that's the exact class of bug #15 reported. */
    val nowPlayingTitles: StateFlow<Map<Long, String>> = _nowPlayingTitles.asStateFlow()

    init {
        settings.activeSourceId
            .flatMapLatest { sourceId ->
                if (sourceId == null) {
                    flowOf(HomeUiState(hasSource = false, isInitializing = false))
                } else {
                    // Rails only need a bounded preview, never the whole catalog (which OOM'd
                    // on large sources). Category counts come from GROUP BY, merged across the
                    // three catalogs by name -- same shape the old in-memory aggregation produced.
                    val rails = combine(
                        repository.topChannels(sourceId, RAIL_LIMIT),
                        repository.topMovies(sourceId, RAIL_LIMIT),
                        repository.topSeries(sourceId, RAIL_LIMIT),
                    ) { channels, movies, series -> Triple(channels, movies, series) }
                    val categories = combine(
                        repository.channelCategoryCounts(sourceId),
                        repository.movieCategoryCounts(sourceId),
                        repository.seriesCategoryCounts(sourceId),
                    ) { c, m, s ->
                        val merged = linkedMapOf<String, Int>()
                        (c + m + s).forEach { merged[it.name] = (merged[it.name] ?: 0) + it.count }
                        merged.map { (name, count) -> HomeCategorySummary(name, count) }
                    }
                    combine(rails, categories) { (channels, movies, series), cats ->
                        HomeUiState(
                            hasSource = true,
                            channels = channels,
                            movies = movies,
                            series = series,
                            categories = cats,
                            isInitializing = false,
                        )
                    }
                }
            }
            .onEach { _uiState.value = it }
            .launchIn(viewModelScope)

        _uiState.map { state -> state.channels.map { it.id } }
            .distinctUntilChanged()
            .flatMapLatest { channelIds ->
                if (channelIds.isEmpty()) {
                    flowOf(emptyMap())
                } else {
                    val nowMs = System.currentTimeMillis()
                    epgRepository.observeForChannels(channelIds, nowMs, nowMs).map { programs ->
                        programs.associate { it.channelId to it.title }
                    }
                }
            }
            .onEach { _nowPlayingTitles.value = it }
            .launchIn(viewModelScope)

        continueWatchingRepository.observeRecent(CONTINUE_WATCHING_LIMIT)
            .mapLatest { entries -> resolveContinueWatching(entries) }
            .onEach { _uiState.value = _uiState.value.copy(continueWatching = it) }
            .launchIn(viewModelScope)
    }

    /** Joins raw [ContinueWatchingEntry] rows against their real [VodTitle] (direct for a movie,
     * via the episode's [com.arashrahimi46.iptv.data.model.SeriesEpisode.seriesTitleId] for a
     * series) -- an entry whose title/episode has since been removed from the catalog is
     * dropped rather than shown with blank text. */
    private suspend fun resolveContinueWatching(entries: List<ContinueWatchingEntry>): List<HomeContinueWatchingItem> {
        val vodIds = entries.mapNotNull { it.vodTitleId }
        val episodeIds = entries.mapNotNull { it.seriesEpisodeId }
        val episodesById = episodeIds.mapNotNull { db.seriesEpisodeDao().getById(it) }.associateBy { it.id }
        val seriesIds = (vodIds + episodesById.values.map { it.seriesTitleId }).distinct()
        val titlesById = repository.titlesByIds(seriesIds).associateBy { it.id }

        return entries.mapNotNull { entry ->
            val progress = if (entry.durationMs > 0) (entry.positionMs.toFloat() / entry.durationMs).coerceIn(0f, 1f) else 0f
            when {
                entry.vodTitleId != null -> {
                    val title = titlesById[entry.vodTitleId] ?: return@mapNotNull null
                    HomeContinueWatchingItem(
                        vodTitleId = title.id,
                        seriesEpisodeId = null,
                        title = title.name,
                        posterUrl = title.posterUrl,
                        meta = listOfNotNull(title.year, title.categoryName).joinToString(" · ").ifEmpty { null },
                        progress = progress,
                    )
                }
                entry.seriesEpisodeId != null -> {
                    val episode = episodesById[entry.seriesEpisodeId] ?: return@mapNotNull null
                    val series = titlesById[episode.seriesTitleId]
                    HomeContinueWatchingItem(
                        vodTitleId = null,
                        seriesEpisodeId = episode.id,
                        title = series?.name ?: episode.name,
                        posterUrl = series?.posterUrl,
                        meta = "S${episode.season} · E${episode.episode}",
                        progress = progress,
                    )
                }
                else -> null
            }
        }
    }

    companion object {
        /** Rail preview size for Continue Watching -- same reasoning as [RAIL_LIMIT]. */
        private const val CONTINUE_WATCHING_LIMIT = 20

        fun factory(app: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = HomeViewModel(app) as T
            }
    }
}
