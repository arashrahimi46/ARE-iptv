package com.arashrahimi46.iptv.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arashrahimi46.iptv.data.db.AppDatabase
import com.arashrahimi46.iptv.data.db.CategoryCount
import com.arashrahimi46.iptv.data.model.Channel
import com.arashrahimi46.iptv.data.model.ContinueWatchingEntry
import com.arashrahimi46.iptv.data.model.VodTitle
import com.arashrahimi46.iptv.data.repository.ContinueWatchingRepository
import com.arashrahimi46.iptv.data.repository.EpgRepository
import com.arashrahimi46.iptv.data.repository.PlaylistRepository
import com.arashrahimi46.iptv.data.repository.PlaylistRepositoryImpl
import com.arashrahimi46.iptv.data.settings.ParentalFilter
import com.arashrahimi46.iptv.data.settings.UserSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class HomeCategorySummary(val name: String, val count: Int)

/** Title for the personalized "Recommended" rail, resolved to a localized string in the UI.
 *  [BecauseYouLike] names the user's dominant liked category; [ForYou] is the mixed-taste label;
 *  [Popular] is the cold-start fallback shown before there's any pin/favorite/watch signal. */
sealed interface RecommendedLabel {
    data object ForYou : RecommendedLabel
    data object Popular : RecommendedLabel
    data class BecauseYouLike(val category: String) : RecommendedLabel
}

/** One catalog category not yet pinned to Home (step 6's "+ Add section" picker), with its
 * [CategoryKind] so the picker can label it ("Sports · Live") and the tile knows which tile
 * shape (channel vs poster) it'll render as once pinned. */
data class HomeAvailableCategory(val kind: CategoryKind, val name: String, val count: Int)

/** Resolved content for one pinned [HomeSection.Category] rail (step 5) -- [Live] renders as
 * [com.arashrahimi46.iptv.ui.components.AreChannelTile]s, [Vod] (movie or series) as
 * [com.arashrahimi46.iptv.ui.components.ArePosterTile]s. Single-type by construction: a pinned
 * category is always exactly one [CategoryKind], never a merge of catalogs. */
sealed interface HomeCategoryContent {
    data class Live(val channels: List<Channel>) : HomeCategoryContent
    data class Vod(val titles: List<VodTitle>) : HomeCategoryContent
}

/** Stable map key for a pinned category rail -- keyed by kind+name rather than the
 * [HomeSection.Category] value itself, since that data class's `hidden` field is part of its
 * equality and would otherwise mint a new map entry (and re-trigger a reload) every time the
 * section is hidden/shown instead of just its position/visibility changing. */
fun homeCategoryRailKey(kind: CategoryKind, name: String): String = "$kind|$name"

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
    /** On-poster season/episode chip (e.g. "S2·E5") for series entries; null for movies. */
    val badgeText: String?,
    val progress: Float,
)

data class HomeUiState(
    val hasSource: Boolean = false,
    val channels: List<Channel> = emptyList(),
    val movies: List<VodTitle> = emptyList(),
    val series: List<VodTitle> = emptyList(),
    /** Personalized best-of across movies+series for the "Recommended" rail (see [HomeRailCurator]). */
    val recommended: List<VodTitle> = emptyList(),
    /** Title for [recommended], resolved to a localized string in the UI (see [RecommendedLabel]). */
    val recommendedLabel: RecommendedLabel = RecommendedLabel.Popular,
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
    /** Persisted Home rail order/visibility (see [com.arashrahimi46.iptv.data.settings.UserSettings.homeLayout]);
     * defaults to [DEFAULT_HOME_LAYOUT] until the user customizes it. */
    val sections: List<HomeSection> = DEFAULT_HOME_LAYOUT,
    /** Step 5: resolved content for every pinned [HomeSection.Category] currently in [sections],
     * keyed by [homeCategoryRailKey]. A category filtered out by the parental lock, or not yet
     * loaded, is simply absent from this map -- callers treat "absent" the same as "empty". */
    val categoryRails: Map<String, HomeCategoryContent> = emptyMap(),
    /** Step 6: source categories not yet pinned to Home, for the "+ Add section" picker. */
    val availableCategories: List<HomeAvailableCategory> = emptyList(),
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

    /** Candidate-pool size the curator picks each rail's ~20 tiles from. Larger than a rail so
     *  de-dup + per-category diversity have room to work; still tiny vs a 300k catalog. */
    private val POOL_LIMIT = 200

    /** Epoch-day seed for the curator's daily rotation -- stable for the whole session (Home
     *  doesn't reshuffle mid-use); a new day yields a fresh selection from the same pool. */
    private val daySeed: Long = System.currentTimeMillis() / 86_400_000L

    /** Taste signal weights. Explicit intent (pinning a category to Home, favoriting) counts for
     *  more than a single implicit watch, but many watches still accumulate past one pin/favorite. */
    private val PIN_WEIGHT = 2.0
    private val FAVORITE_WEIGHT = 2.0
    private val WATCH_WEIGHT = 1.0

    /** Re-scales raw category weights so the top category is 1.0 (the curator's 0..1 taste input);
     *  empty in, empty out (no signal -> no personalization). */
    private fun Map<String, Double>.normalized(): Map<String, Double> {
        val max = values.maxOrNull()?.takeIf { it > 0 } ?: return emptyMap()
        return mapValues { it.value / max }
    }

    /** Adds each [counts] row into [into], scaled by [weight]. */
    private fun accumulate(into: MutableMap<String, Double>, counts: List<CategoryCount>, weight: Double) {
        counts.forEach { into[it.name] = (into[it.name] ?: 0.0) + it.count * weight }
    }

    /** Dynamic label for the personalized rail: a clearly-dominant liked category names the rail
     *  ("Because you like X"); mixed tastes fall back to "For You"; no signal at all -> "Popular"
     *  (the cold-start top-rated fallback). [taste] is already max-normalized (top == 1.0). */
    private fun recommendedLabelFor(taste: Map<String, Double>): RecommendedLabel {
        if (taste.isEmpty()) return RecommendedLabel.Popular
        val ranked = taste.entries.sortedByDescending { it.value }
        val runnerUp = ranked.getOrNull(1)?.value ?: 0.0
        // top is 1.0; call it "dominant" only when the runner-up trails clearly, else it's a toss-up.
        return if (runnerUp <= 0.6) RecommendedLabel.BecauseYouLike(ranked.first().key) else RecommendedLabel.ForYou
    }

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
                        repository.sampleChannels(sourceId, POOL_LIMIT),
                        repository.sampleMovies(sourceId, POOL_LIMIT),
                        repository.sampleSeries(sourceId, POOL_LIMIT),
                    ) { channels, movies, series -> Triple(channels, movies, series) }
                    // Personalization signals (raw category counts -- pinned categories are folded in
                    // downstream, where the current Home layout is in scope). Weighted + normalized in
                    // the main combine so a channel favorite never sways movie ranking and vice versa.
                    val taste = combine(
                        repository.watchedCategoryCounts(sourceId),
                        repository.favoriteVodCategoryCounts(sourceId),
                        repository.favoriteChannelCategoryCounts(sourceId),
                    ) { watched, favVod, favLive -> Triple(watched, favVod, favLive) }
                    // Same three GROUP BY count flows drive both the "Browse by category" rail
                    // (merged by name across catalogs, as before) and step 6's available-categories
                    // picker (kept per-kind, since a pin must be a single real source category).
                    val categoryData = combine(
                        repository.channelCategoryCounts(sourceId),
                        repository.movieCategoryCounts(sourceId),
                        repository.seriesCategoryCounts(sourceId),
                    ) { c, m, s ->
                        val merged = linkedMapOf<String, Int>()
                        (c + m + s).forEach { merged[it.name] = (merged[it.name] ?: 0) + it.count }
                        val summaries = merged.map { (name, count) -> HomeCategorySummary(name, count) }
                        // Dedup by (kind, name): real Xtream catalogs can list the same category
                        // name under more than one provider category id -- step 6's picker keys its
                        // LazyColumn rows by "kind|name" (a pin must resolve to one real category by
                        // name via channelsByCategory/moviesByCategory/seriesByCategory anyway, so a
                        // second same-named entry couldn't be pinned separately), and a duplicate key
                        // there crashes that LazyColumn's composition -- silently, since it's inside a
                        // dialog Compose can retry without visibly bringing down the whole screen.
                        val availableByKey = linkedMapOf<Pair<CategoryKind, String>, Int>()
                        c.forEach { availableByKey[CategoryKind.LIVE to it.name] = (availableByKey[CategoryKind.LIVE to it.name] ?: 0) + it.count }
                        m.forEach { availableByKey[CategoryKind.MOVIE to it.name] = (availableByKey[CategoryKind.MOVIE to it.name] ?: 0) + it.count }
                        s.forEach { availableByKey[CategoryKind.SERIES to it.name] = (availableByKey[CategoryKind.SERIES to it.name] ?: 0) + it.count }
                        val available = availableByKey.map { (key, count) -> HomeAvailableCategory(key.first, key.second, count) }
                        summaries to available
                    }
                    combine(rails, categoryData, taste, settings.parentalFilter, settings.homeLayout) { (channels, movies, series), (cats, available), (watched, favVod, favLive), parental, layout ->
                        // Parental lock: strip adult items from every pool and adult chips from the
                        // category row, so the toggle actually hides adult content on Home too. Filter
                        // the pools BEFORE curating so adult items can't leak into the picked rail.
                        val pinnedCategories = layout.filterIsInstance<HomeSection.Category>()
                        val pinned = pinnedCategories.map { it.kind to it.name }.toSet()
                        val availableCategories = available
                            .filterNot { (it.kind to it.name) in pinned }
                            .filterNot { a -> parental.hidden(a.name) }
                        // Taste = watches + favorites + PINS. Pinning a category to Home is the clearest
                        // "I like this" the user can give, so it feeds the same weights that rank every rail.
                        val vodTaste = mutableMapOf<String, Double>().also {
                            accumulate(it, watched, WATCH_WEIGHT)
                            accumulate(it, favVod, FAVORITE_WEIGHT)
                            pinnedCategories.filter { p -> p.kind != CategoryKind.LIVE }.forEach { p -> it[p.name] = (it[p.name] ?: 0.0) + PIN_WEIGHT }
                        }.normalized()
                        val liveTaste = mutableMapOf<String, Double>().also {
                            accumulate(it, favLive, FAVORITE_WEIGHT)
                            pinnedCategories.filter { p -> p.kind == CategoryKind.LIVE }.forEach { p -> it[p.name] = (it[p.name] ?: 0.0) + PIN_WEIGHT }
                        }.normalized()
                        val channelPool = channels.filterNot { parental.hidden(it.categoryName) }
                        val moviePool = movies.filterNot { parental.hidden(it.categoryName) }
                        val seriesPool = series.filterNot { parental.hidden(it.categoryName) }
                        HomeUiState(
                            hasSource = true,
                            channels = HomeRailCurator.curateChannels(channelPool, liveTaste, daySeed),
                            movies = HomeRailCurator.curateTitles(moviePool, vodTaste, daySeed),
                            series = HomeRailCurator.curateTitles(seriesPool, vodTaste, daySeed),
                            recommended = HomeRailCurator.recommend(moviePool, seriesPool, vodTaste, daySeed),
                            recommendedLabel = recommendedLabelFor(vodTaste),
                            categories = cats.filterNot { parental.hidden(it.name) },
                            isInitializing = false,
                            sections = layout,
                            availableCategories = availableCategories,
                        )
                    }
                }
            }
            // Preserves categoryRails AND continueWatching (each owned by a separate pipeline
            // below) instead of the plain `_uiState.value = it` this replaced -- that would reset
            // them to the fresh HomeUiState()'s empty defaults on every rail/category/layout
            // emission. Dropping continueWatching here is why a just-watched title only appeared
            // after an app restart: any main-rails re-emission wiped it until the CW pipeline
            // happened to emit last.
            .onEach { state ->
                _uiState.value = state.copy(
                    categoryRails = _uiState.value.categoryRails,
                    continueWatching = _uiState.value.continueWatching,
                )
            }
            .launchIn(viewModelScope)

        // Step 5: resolve every pinned category's real content whenever the active source or the
        // set of pinned categories (or the parental lock, which can hide a whole pinned adult
        // category) changes. Distinct-by kind+name so toggling `hidden` on a category section --
        // which HomeSection.Category's equality treats as a different value -- does NOT cause a
        // pointless requery; only the category set/order or the source itself does.
        combine(settings.activeSourceId, settings.homeLayout, settings.parentalFilter) { sourceId, layout, parental ->
            Triple(sourceId, layout.filterIsInstance<HomeSection.Category>().distinctBy { it.kind to it.name }, parental)
        }
            .distinctUntilChanged()
            .flatMapLatest { (sourceId, categorySections, parental) ->
                if (sourceId == null || categorySections.isEmpty()) {
                    flowOf(emptyMap())
                } else {
                    flow { emit(loadCategoryRails(sourceId, categorySections, parental)) }
                }
            }
            .onEach { rails -> _uiState.value = _uiState.value.copy(categoryRails = rails) }
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

        // Scope the rail to the active source: bookmarks from other playlists still live in the DB
        // (rows are only purged when a source is deleted, not when switched), so filter them out.
        combine(
            settings.activeSourceId,
            continueWatchingRepository.observeRecent(CONTINUE_WATCHING_LIMIT),
        ) { sourceId, entries -> sourceId to entries }
            .mapLatest { (sourceId, entries) -> resolveContinueWatching(entries, sourceId) }
            .onEach { _uiState.value = _uiState.value.copy(continueWatching = it) }
            .launchIn(viewModelScope)
    }

    /** Long-press action on a Continue Watching tile: drop the bookmark. The observeRecent flow
     * re-emits, so the rail updates itself -- no manual state edit here. */
    fun removeContinueWatching(item: HomeContinueWatchingItem) {
        viewModelScope.launch {
            continueWatchingRepository.clear(item.vodTitleId, item.seriesEpisodeId)
        }
    }

    /** Joins raw [ContinueWatchingEntry] rows against their real [VodTitle] (direct for a movie,
     * via the episode's [com.arashrahimi46.iptv.data.model.SeriesEpisode.seriesTitleId] for a
     * series) -- an entry whose title/episode has since been removed from the catalog is
     * dropped rather than shown with blank text. */
    private suspend fun resolveContinueWatching(entries: List<ContinueWatchingEntry>, activeSourceId: Long?): List<HomeContinueWatchingItem> {
        val vodIds = entries.mapNotNull { it.vodTitleId }
        val episodeIds = entries.mapNotNull { it.seriesEpisodeId }
        val episodesById = episodeIds.mapNotNull { db.seriesEpisodeDao().getById(it) }.associateBy { it.id }
        val seriesIds = (vodIds + episodesById.values.map { it.seriesTitleId }).distinct()
        // Only titles belonging to the active source resolve -- an entry whose title lives in
        // another (still-installed) playlist is dropped so the rail matches the current source.
        val titlesById = repository.titlesByIds(seriesIds)
            .filter { it.sourceId == activeSourceId }
            .associateBy { it.id }

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
                        badgeText = null,
                        progress = progress,
                    )
                }
                entry.seriesEpisodeId != null -> {
                    val episode = episodesById[entry.seriesEpisodeId] ?: return@mapNotNull null
                    // titlesById is source-filtered, so a null series means the episode belongs to
                    // another playlist -- drop it rather than falling back to the episode name.
                    val series = titlesById[episode.seriesTitleId] ?: return@mapNotNull null
                    HomeContinueWatchingItem(
                        vodTitleId = null,
                        seriesEpisodeId = episode.id,
                        title = series.name,
                        posterUrl = series.posterUrl,
                        // Season/episode moves onto the poster as a badge; footer meta stays the series category.
                        meta = series.categoryName,
                        badgeText = "S${episode.season}·E${episode.episode}",
                        progress = progress,
                    )
                }
                else -> null
            }
        }
    }

    /** Batch-loads real content for every pinned category rail (step 5) -- one suspend query per
     * pinned category via the same `xByCategory` DAO calls Browse screens use, typed by [CategoryKind]
     * so each rail stays single-type (no merging live/movie/series under one pin). A category the
     * parental lock hides is skipped entirely (its rail is simply absent from the result map). */
    private suspend fun loadCategoryRails(
        sourceId: Long,
        categorySections: List<HomeSection.Category>,
        parental: ParentalFilter,
    ): Map<String, HomeCategoryContent> {
        val result = linkedMapOf<String, HomeCategoryContent>()
        for (section in categorySections) {
            if (parental.hidden(section.name)) continue
            val content = when (section.kind) {
                CategoryKind.LIVE -> HomeCategoryContent.Live(repository.channelsByCategory(sourceId, section.name, RAIL_LIMIT))
                CategoryKind.MOVIE -> HomeCategoryContent.Vod(repository.moviesByCategory(sourceId, section.name, RAIL_LIMIT))
                CategoryKind.SERIES -> HomeCategoryContent.Vod(repository.seriesByCategory(sourceId, section.name, RAIL_LIMIT))
            }
            result[homeCategoryRailKey(section.kind, section.name)] = content
        }
        return result
    }

    /** Step 4: persists a reordered/hidden-toggled Home layout. [settings.homeLayout]'s DataStore
     * Flow re-emits from this write and drives the re-render -- this function only writes. */
    fun updateLayout(sections: List<HomeSection>) {
        viewModelScope.launch { settings.setHomeLayout(sections) }
    }

    /** Step 6: appends a newly-picked category to the end of the current layout and persists. */
    fun addCategorySection(kind: CategoryKind, name: String) {
        viewModelScope.launch {
            val current = settings.homeLayout.first()
            settings.setHomeLayout(current + HomeSection.Category(kind, name))
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
