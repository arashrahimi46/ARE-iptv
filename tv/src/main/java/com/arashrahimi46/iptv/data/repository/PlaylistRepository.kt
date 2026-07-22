package com.arashrahimi46.iptv.data.repository

import android.content.Context
import androidx.paging.PagingSource
import com.arashrahimi46.iptv.data.db.AppDatabase
import com.arashrahimi46.iptv.data.db.CategoryCount
import com.arashrahimi46.iptv.data.model.Category
import com.arashrahimi46.iptv.data.model.Channel
import com.arashrahimi46.iptv.data.model.ContentType
import com.arashrahimi46.iptv.data.model.PlaylistSource
import com.arashrahimi46.iptv.data.model.SeriesEpisode
import com.arashrahimi46.iptv.data.model.SourceType
import com.arashrahimi46.iptv.data.model.VodTitle
import com.arashrahimi46.iptv.data.parser.M3uParser
import com.arashrahimi46.iptv.data.parser.OmdbClient
import com.arashrahimi46.iptv.data.parser.XtreamClient
import com.arashrahimi46.iptv.data.parser.parseSeriesEpisode
import com.arashrahimi46.iptv.data.parser.XtreamException
import androidx.room.withTransaction
import com.arashrahimi46.iptv.data.settings.CredentialsStore
import com.arashrahimi46.iptv.data.settings.UserSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/** Derived counts shown on the onboarding Confirm step, from a *real* parse -- never hardcoded. */
data class ImportSummary(val channels: Int, val movies: Int, val series: Int)

/** Keep the existing value if it's already present (non-blank), else take [fallback] -- so a lower-
 * priority metadata source (OMDb) only fills fields a higher-priority one (Xtream) left empty. */
private fun String?.orTake(fallback: String?): String? =
    this?.takeIf { it.isNotBlank() } ?: fallback?.takeIf { it.isNotBlank() }

/** Xtream `get.php` keys are lowercase by spec, and the server is case-sensitive on them. */
private val XTREAM_QUERY_KEYS = setOf("username", "password", "type", "output")

/** Rows flushed to Room per batch during a streaming M3U import (bounds peak memory). */
private const val IMPORT_BATCH_SIZE = 1500

/** Ids per `DELETE ... WHERE id IN (...)` batch during a refresh -- keeps the bound-variable count
 * well under SQLite's ~999 limit when a provider drops a large chunk of a catalog. */
private const val DELETE_CHUNK = 500

/**
 * Fallback category for Xtream items whose `category_id` is absent or not present in the
 * `get_*_categories` response (portals commonly use "0" or omit it). Without this the item's
 * categoryName is null, so it's reachable only via the "All" pseudo-category and vanishes from
 * every real, filterable category tab (which GROUP BY a non-null categoryName).
 */
private const val UNCATEGORIZED_CATEGORY = "Uncategorized"

/**
 * Trim the URL and lowercase only the known Xtream query-parameter KEYS (values left
 * untouched), so a pasted link like `...&Password=...` -- which 404s because the server
 * only recognizes `password` -- just works. Non-Xtream query keys are left alone, so
 * ordinary M3U URLs with case-sensitive params aren't disturbed.
 */
internal fun normalizeSourceUrl(raw: String): String {
    val trimmed = raw.trim()
    val q = trimmed.indexOf('?')
    if (q < 0) return trimmed
    val base = trimmed.substring(0, q)
    val query = trimmed.substring(q + 1)
    if (query.isEmpty()) return trimmed
    val normalized = query.split('&').joinToString("&") { pair ->
        val eq = pair.indexOf('=')
        if (eq < 0) return@joinToString pair
        val key = pair.substring(0, eq)
        if (key.lowercase() in XTREAM_QUERY_KEYS) key.lowercase() + pair.substring(eq) else pair
    }
    return "$base?$normalized"
}

/** Host + credentials pulled out of an Xtream `get.php` portal link. */
internal data class XtreamPortalLink(val host: String, val username: String, val password: String)

/**
 * Detects the very common case of an Xtream portal handed out as an M3U link -- a `get.php` URL
 * with `username`/`password` query params. That link carries the exact credentials `player_api.php`
 * needs, so the source can be imported via the authoritative Xtream API -- which returns each item's
 * real content type and the full VOD/live/series category lists -- instead of the `group-title`
 * keyword guess, which silently drops every VOD category not literally named "*Movies*"/"*VOD*"
 * (e.g. "Persian Dubbed", "Arabic Animation", "Netflix| Multi-Language") into Live TV. Returns null
 * for a plain (non-Xtream) M3U link, which stays on the heuristic path.
 */
internal fun parseXtreamGetPhp(raw: String): XtreamPortalLink? {
    val trimmed = raw.trim()
    val marker = trimmed.indexOf("get.php", ignoreCase = true)
    val q = trimmed.indexOf('?')
    if (marker < 0 || q < marker) return null
    val params = trimmed.substring(q + 1).split('&').mapNotNull { pair ->
        val eq = pair.indexOf('=')
        if (eq < 0) null else pair.substring(0, eq).lowercase() to pair.substring(eq + 1)
    }.toMap()
    val username = params["username"]?.takeIf { it.isNotBlank() } ?: return null
    val secret = params["password"]?.takeIf { it.isNotBlank() } ?: return null
    val host = trimmed.substring(0, marker).trimEnd('/')
    if (host.isEmpty()) return null
    return XtreamPortalLink(host, username, secret)
}

/**
 * Repository pattern wrapping Room DAOs + the M3U/Xtream parsers behind a
 * single surface. All network/parsing work runs on [Dispatchers.IO].
 */
interface PlaylistRepository {
    fun observeSources(): Flow<List<PlaylistSource>>
    suspend fun hasAnySource(): Boolean

    // --- Browse (Paging 3). category == null means the "All" pseudo-category (no filter). ---
    fun pagingChannels(sourceId: Long, category: String?): PagingSource<Int, Channel>
    fun pagingMovies(sourceId: Long, category: String?): PagingSource<Int, VodTitle>
    fun pagingSeries(sourceId: Long, category: String?): PagingSource<Int, VodTitle>

    // --- Browse "Favorites" pseudo-category: only the source's favorited items (paged). ---
    fun pagingFavoriteChannels(sourceId: Long): PagingSource<Int, Channel>
    fun pagingFavoriteMovies(sourceId: Long): PagingSource<Int, VodTitle>
    fun pagingFavoriteSeries(sourceId: Long): PagingSource<Int, VodTitle>
    fun favoriteChannelCount(sourceId: Long): Flow<Int>
    fun favoriteMovieCount(sourceId: Long): Flow<Int>
    fun favoriteSeriesCount(sourceId: Long): Flow<Int>

    // --- Category columns + totals (GROUP BY / COUNT -- no catalog rows loaded). ---
    fun channelCategoryCounts(sourceId: Long): Flow<List<CategoryCount>>
    fun movieCategoryCounts(sourceId: Long): Flow<List<CategoryCount>>
    fun seriesCategoryCounts(sourceId: Long): Flow<List<CategoryCount>>
    fun channelCount(sourceId: Long): Flow<Int>
    fun movieCount(sourceId: Long): Flow<Int>
    fun seriesCount(sourceId: Long): Flow<Int>

    // --- Bounded loads (Home rails, Guide, MultiView) -- never the whole catalog. ---
    fun topChannels(sourceId: Long, limit: Int): Flow<List<Channel>>
    fun topMovies(sourceId: Long, limit: Int): Flow<List<VodTitle>>
    fun topSeries(sourceId: Long, limit: Int): Flow<List<VodTitle>>

    // --- Diverse candidate pools + taste signals for the curated Home rails (HomeRailCurator). ---
    fun sampleChannels(sourceId: Long, limit: Int): Flow<List<Channel>>
    fun sampleMovies(sourceId: Long, limit: Int): Flow<List<VodTitle>>
    fun sampleSeries(sourceId: Long, limit: Int): Flow<List<VodTitle>>
    fun watchedCategoryCounts(sourceId: Long): Flow<List<CategoryCount>>
    fun favoriteVodCategoryCounts(sourceId: Long): Flow<List<CategoryCount>>
    fun favoriteChannelCategoryCounts(sourceId: Long): Flow<List<CategoryCount>>

    // --- DB-side search (Search screen). ---
    suspend fun searchChannels(sourceId: Long, query: String, limit: Int): List<Channel>
    suspend fun searchMovies(sourceId: Long, query: String, limit: Int): List<VodTitle>
    suspend fun searchSeries(sourceId: Long, query: String, limit: Int): List<VodTitle>
    suspend fun channelsByCategory(sourceId: Long, category: String, limit: Int): List<Channel>
    suspend fun moviesByCategory(sourceId: Long, category: String, limit: Int): List<VodTitle>
    suspend fun seriesByCategory(sourceId: Long, category: String, limit: Int): List<VodTitle>

    // --- Resolve ids -> rows (Favorites); all ordered channel ids (player prev/next nav). ---
    suspend fun channelsByIds(ids: List<Long>): List<Channel>
    suspend fun titlesByIds(ids: List<Long>): List<VodTitle>
    suspend fun channelIds(sourceId: Long): List<Long>

    /** Fetches + parses an M3U playlist and persists the derived catalog. Throws on network/parse failure. */
    suspend fun addM3uSource(name: String, url: String, epgUrl: String?): ImportSummary

    /** Authenticates against the Xtream portal, pulls the top-level catalog, and persists it. Throws on failure. */
    suspend fun addXtreamSource(
        name: String,
        host: String,
        username: String,
        password: String,
        epgUrl: String?,
    ): ImportSummary

    /** Real per-series episode list (Room-cached) -- empty for M3U titles/titles with no episodes loaded yet. */
    fun observeSeriesEpisodes(seriesTitleId: Long): Flow<List<SeriesEpisode>>

    /**
     * Populates [observeSeriesEpisodes] for [vodTitle] from Xtream's
     * `get_series_info` on first view (Room-cached after that). No-op for
     * non-series titles, M3U titles (no `externalId` -- no authoritative
     * episode structure available), or once already cached. Throws on
     * network failure so the caller can surface it.
     */
    suspend fun ensureSeriesEpisodesLoaded(vodTitle: VodTitle)

    /**
     * Enriches [vodTitle] with plot/cast/ratings on first Detail view (Room-cached after that):
     * Xtream's own `info` block first (free, no key), then the user's OMDb key as a fallback for
     * anything still missing -- especially the IMDb/Rotten Tomatoes ranks Xtream doesn't carry.
     * Returns the (possibly enriched) row so the caller can render it without a re-query; a no-op
     * returns [vodTitle] unchanged. Never throws -- a metadata miss must not break the screen.
     */
    suspend fun ensureMetadataLoaded(vodTitle: VodTitle): VodTitle

    /** Live read of one source -- backs the Settings "Last updated" label and refresh-overdue badge. */
    fun observeSource(sourceId: Long): Flow<PlaylistSource?>

    /**
     * Re-syncs an already-added source's catalog with the provider so items the provider added/
     * removed show up/disappear -- WITHOUT wiping the catalog. Xtream sources sync via an
     * id-preserving upsert (unchanged items keep their Room row id, so continue-watching and
     * lazily-loaded metadata/episodes survive; only genuinely new items are inserted and removed
     * ones deleted). Safe under a one-connection line: the whole provider catalog is fetched FIRST
     * and, if that fails or comes back empty (e.g. an ip-limit response), nothing is deleted -- the
     * existing catalog is left intact and the error is rethrown. Stamps `lastRefreshedAtMs` on
     * success. Throws on network/auth failure. (M3U sources fall back to a full URL-keyed re-import.)
     */
    suspend fun refreshSource(sourceId: Long): ImportSummary

    /**
     * Permanently deletes a playlist source and EVERYTHING derived from it -- channels, movies,
     * series + their episodes, categories, the source's EPG, and any favorites/continue-watching
     * that pointed at its items -- plus the source row and its stored credentials. The catalog wipe
     * runs in one transaction (all-or-nothing). If the deleted source was the active one, the active
     * pointer is cleared so nothing references a gone source. No-op if the id doesn't exist.
     */
    suspend fun deleteSource(sourceId: Long)
}

class PlaylistRepositoryImpl(context: Context) : PlaylistRepository {
    private val db = AppDatabase.get(context)
    private val settings = UserSettings(context)
    private val credentials = CredentialsStore(context)
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    override fun observeSources(): Flow<List<PlaylistSource>> = db.playlistSourceDao().observeAll()

    override fun observeSource(sourceId: Long): Flow<PlaylistSource?> = db.playlistSourceDao().observeById(sourceId)

    override suspend fun hasAnySource(): Boolean = db.playlistSourceDao().count() > 0

    private val channelDao get() = db.channelDao()
    private val vodDao get() = db.vodTitleDao()

    override fun pagingChannels(sourceId: Long, category: String?): PagingSource<Int, Channel> =
        if (category == null) channelDao.pagingAll(sourceId) else channelDao.pagingByCategory(sourceId, category)
    override fun pagingMovies(sourceId: Long, category: String?): PagingSource<Int, VodTitle> =
        if (category == null) vodDao.paging(sourceId, false) else vodDao.pagingByCategory(sourceId, false, category)
    override fun pagingSeries(sourceId: Long, category: String?): PagingSource<Int, VodTitle> =
        if (category == null) vodDao.paging(sourceId, true) else vodDao.pagingByCategory(sourceId, true, category)

    override fun pagingFavoriteChannels(sourceId: Long): PagingSource<Int, Channel> = channelDao.pagingFavorites(sourceId)
    override fun pagingFavoriteMovies(sourceId: Long): PagingSource<Int, VodTitle> =
        vodDao.pagingFavorites(sourceId, false, ContentType.MOVIE)
    override fun pagingFavoriteSeries(sourceId: Long): PagingSource<Int, VodTitle> =
        vodDao.pagingFavorites(sourceId, true, ContentType.SERIES)
    override fun favoriteChannelCount(sourceId: Long): Flow<Int> = channelDao.observeFavoriteCount(sourceId)
    override fun favoriteMovieCount(sourceId: Long): Flow<Int> = vodDao.observeFavoriteCount(sourceId, false, ContentType.MOVIE)
    override fun favoriteSeriesCount(sourceId: Long): Flow<Int> = vodDao.observeFavoriteCount(sourceId, true, ContentType.SERIES)

    override fun channelCategoryCounts(sourceId: Long): Flow<List<CategoryCount>> = channelDao.observeCategoryCounts(sourceId)
    override fun movieCategoryCounts(sourceId: Long): Flow<List<CategoryCount>> = vodDao.observeCategoryCounts(sourceId, false)
    override fun seriesCategoryCounts(sourceId: Long): Flow<List<CategoryCount>> = vodDao.observeCategoryCounts(sourceId, true)
    override fun channelCount(sourceId: Long): Flow<Int> = channelDao.observeCountForSource(sourceId)
    override fun movieCount(sourceId: Long): Flow<Int> = vodDao.observeCount(sourceId, false)
    override fun seriesCount(sourceId: Long): Flow<Int> = vodDao.observeCount(sourceId, true)

    override fun topChannels(sourceId: Long, limit: Int): Flow<List<Channel>> = channelDao.observeTop(sourceId, limit)
    override fun topMovies(sourceId: Long, limit: Int): Flow<List<VodTitle>> = vodDao.observeTop(sourceId, false, limit)
    override fun topSeries(sourceId: Long, limit: Int): Flow<List<VodTitle>> = vodDao.observeTop(sourceId, true, limit)

    override fun sampleChannels(sourceId: Long, limit: Int): Flow<List<Channel>> = channelDao.observeSample(sourceId, limit)
    override fun sampleMovies(sourceId: Long, limit: Int): Flow<List<VodTitle>> = vodDao.observeSample(sourceId, false, limit)
    override fun sampleSeries(sourceId: Long, limit: Int): Flow<List<VodTitle>> = vodDao.observeSample(sourceId, true, limit)
    override fun watchedCategoryCounts(sourceId: Long): Flow<List<CategoryCount>> = vodDao.watchedCategoryCounts(sourceId)
    override fun favoriteVodCategoryCounts(sourceId: Long): Flow<List<CategoryCount>> = vodDao.favoriteCategoryCounts(sourceId)
    override fun favoriteChannelCategoryCounts(sourceId: Long): Flow<List<CategoryCount>> = channelDao.favoriteCategoryCounts(sourceId)

    override suspend fun searchChannels(sourceId: Long, query: String, limit: Int): List<Channel> = channelDao.search(sourceId, query, limit)
    override suspend fun searchMovies(sourceId: Long, query: String, limit: Int): List<VodTitle> = vodDao.search(sourceId, false, query, limit)
    override suspend fun searchSeries(sourceId: Long, query: String, limit: Int): List<VodTitle> = vodDao.search(sourceId, true, query, limit)
    override suspend fun channelsByCategory(sourceId: Long, category: String, limit: Int): List<Channel> = channelDao.byCategory(sourceId, category, limit)
    override suspend fun moviesByCategory(sourceId: Long, category: String, limit: Int): List<VodTitle> = vodDao.byCategory(sourceId, false, category, limit)
    override suspend fun seriesByCategory(sourceId: Long, category: String, limit: Int): List<VodTitle> = vodDao.byCategory(sourceId, true, category, limit)

    override suspend fun channelsByIds(ids: List<Long>): List<Channel> = if (ids.isEmpty()) emptyList() else channelDao.getByIds(ids)
    override suspend fun titlesByIds(ids: List<Long>): List<VodTitle> = if (ids.isEmpty()) emptyList() else vodDao.getByIds(ids)
    override suspend fun channelIds(sourceId: Long): List<Long> = channelDao.idsForSource(sourceId)

    override fun observeSeriesEpisodes(seriesTitleId: Long): Flow<List<SeriesEpisode>> =
        db.seriesEpisodeDao().observeForSeries(seriesTitleId)

    override suspend fun ensureSeriesEpisodesLoaded(vodTitle: VodTitle): Unit = withContext(Dispatchers.IO) {
        if (!vodTitle.isSeries || vodTitle.externalId == null) return@withContext
        if (db.seriesEpisodeDao().countForSeries(vodTitle.id) > 0) return@withContext
        val source = db.playlistSourceDao().getById(vodTitle.sourceId) ?: return@withContext
        if (source.type != SourceType.XTREAM) return@withContext
        val username = credentials.username(source.id) ?: return@withContext
        val password = credentials.password(source.id) ?: return@withContext

        val xtream = XtreamClient(source.url, username, password)
        val episodes = xtream.getSeriesInfo(vodTitle.externalId)
        val entities = episodes.map { ep ->
            SeriesEpisode(
                seriesTitleId = vodTitle.id,
                season = ep.season,
                episode = ep.episode,
                name = ep.title,
                streamUrl = xtream.streamUrl("series", ep.id, ep.containerExtension ?: "mp4"),
                externalId = ep.id,
            )
        }
        if (entities.isNotEmpty()) {
            db.seriesEpisodeDao().insertAll(entities)
            db.vodTitleDao().setEpisodeCount(vodTitle.id, entities.size)
        }
    }

    override suspend fun ensureMetadataLoaded(vodTitle: VodTitle): VodTitle = withContext(Dispatchers.IO) {
        if (vodTitle.metadataFetched) return@withContext vodTitle

        var plot = vodTitle.plot
        var castList = vodTitle.castList
        var director = vodTitle.director
        var genre = vodTitle.genre
        var year = vodTitle.year
        var rating = vodTitle.rating
        var imdbRating = vodTitle.imdbRating
        var rtRating = vodTitle.rtRating
        var attempted = false

        // 1. Xtream's own info block -- free, no key. Best-effort: any failure just falls through to OMDb.
        val source = db.playlistSourceDao().getById(vodTitle.sourceId)
        if (source?.type == SourceType.XTREAM && vodTitle.externalId != null) {
            val (user, secret) = credentials.forSource(source.id)
            if (user != null && secret != null) {
                val xtream = XtreamClient(source.url, user, secret)
                val info = runCatching {
                    if (vodTitle.isSeries) xtream.getSeriesInfoMeta(vodTitle.externalId)
                    else xtream.getVodInfo(vodTitle.externalId)
                }.getOrNull()
                if (info != null) {
                    attempted = true
                    plot = plot.orTake(info.plot)
                    castList = castList.orTake(info.cast)
                    director = director.orTake(info.director)
                    genre = genre.orTake(info.genre)
                    year = year ?: info.year
                    rating = rating ?: info.rating
                }
            }
        }

        // 2. OMDb fallback for whatever's still missing (the IMDb/RT ranks especially), if the user
        // connected a key. Cleaned title + year matching lives in OmdbClient.
        val omdbKey = settings.omdbKey.first()
        if (!omdbKey.isNullOrBlank()) {
            attempted = true
            val needsMore = imdbRating == null || rtRating == null || plot.isNullOrBlank() || castList.isNullOrBlank()
            if (needsMore) {
                val meta = OmdbClient.lookup(omdbKey, vodTitle.name, year, vodTitle.isSeries)
                if (meta != null) {
                    plot = plot.orTake(meta.plot)
                    castList = castList.orTake(meta.cast)
                    director = director.orTake(meta.director)
                    genre = genre.orTake(meta.genre)
                    year = year ?: meta.year
                    imdbRating = imdbRating ?: meta.imdbRating
                    rtRating = rtRating ?: meta.rtRating
                }
            }
        }

        // Only mark fetched when a real source was queried, so connecting an OMDb key later still
        // enriches a plain-M3U title that had nothing to fetch on its first open.
        db.vodTitleDao().setMetadata(vodTitle.id, plot, castList, director, genre, year, rating, imdbRating, rtRating, attempted)
        vodTitle.copy(
            plot = plot, castList = castList, director = director, genre = genre,
            year = year, rating = rating, imdbRating = imdbRating, rtRating = rtRating, metadataFetched = attempted,
        )
    }

    override suspend fun addM3uSource(name: String, url: String, epgUrl: String?): ImportSummary =
        withContext(Dispatchers.IO) {
            // An Xtream portal handed out as a get.php M3U link is imported via its authoritative
            // player_api instead of guessing content type from group-title keywords (which drops
            // VOD categories not named "*Movies*" into Live TV -- see parseXtreamGetPhp). Fall back
            // to raw M3U parsing if the portal's player_api is unreachable/disabled (addXtreamSource
            // fully rolls back its half-imported source first, so no orphan row is left behind).
            parseXtreamGetPhp(url)?.let { portal ->
                try {
                    return@withContext addXtreamSource(name, portal.host, portal.username, portal.password, epgUrl)
                } catch (e: Exception) {
                    // A reachable portal that rejected the credentials is authoritative -- surface it
                    // instead of silently degrading to the lossy raw-M3U keyword path (which would
                    // "succeed" with a mis-classified catalog and hide the real auth problem). Any
                    // other failure means this URL isn't a usable Xtream portal, so fall through to
                    // raw-M3U parsing (addXtreamSource has already rolled back its half-import).
                    if (e is XtreamException && e.isAuthError) throw e
                }
            }
            // Normalize a pasted link (e.g. capitalized `Password`) so it doesn't 404, then
            // persist the normalized form so later refreshes hit the same working URL.
            val normalizedUrl = normalizeSourceUrl(url)

            val source = PlaylistSource(
                name = name, type = SourceType.M3U, url = normalizedUrl, epgUrl = epgUrl,
                lastRefreshedAtMs = System.currentTimeMillis(),
            )
            val sourceId = db.playlistSourceDao().insert(source)

            // Classification heuristic lives in M3uGroupClassifier.kt (unit-tested) -- see its
            // doc comment for the M3U-only scope and the known real-world false-positive pattern.
            val categories = linkedMapOf<Pair<ContentType, String>, Category>()
            val channelBatch = mutableListOf<Channel>()
            val vodBatch = mutableListOf<VodTitle>()
            val episodeBatch = mutableListOf<SeriesEpisode>()
            // Distinct series parent ids, keyed by lowercased series name -- bounded by the number of
            // distinct series (thousands), not episodes (100k+), so it stays in memory safely while
            // per-episode rows batch-flush to Room like everything else.
            val seriesIdByName = HashMap<String, Long>()
            var channelCount = 0
            var movieCount = 0
            var seriesCount = 0
            // EPG feed URL declared on the `#EXTM3U` header (`url-tvg`), captured lazily on the
            // first line as the stream is pulled -- this is how most M3U playlists point at their
            // XMLTV guide, and without it the source has no EPG at all (blank Guide grid).
            var headerEpgUrl: String? = null
            var headerParsed = false

            try {
                // Stream + batch: a large m3u_plus (100s of MB) is parsed line-by-line and
                // flushed to Room in bounded chunks, so the whole body is never held in memory
                // at once (loading it as one String OOM'd -- the response can exceed the heap).
                streamPlaylist(normalizedUrl) { lines ->
                    val tapped = lines.onEach { raw ->
                        if (!headerParsed && raw.trim().startsWith("#EXTM3U", ignoreCase = true)) {
                            headerEpgUrl = M3uParser.extractEpgUrl(raw)
                            headerParsed = true
                        }
                    }
                    M3uParser.parse(tapped).forEach { entry ->
                        val group = entry.groupTitle?.trim().orEmpty()
                        val type = classifyM3uGroup(group)
                        if (group.isNotEmpty()) {
                            categories.putIfAbsent(type to group, Category(sourceId, type, group))
                        }
                        when (type) {
                            ContentType.LIVE -> {
                                channelBatch += Channel(
                                    sourceId = sourceId,
                                    name = entry.name,
                                    streamUrl = entry.streamUrl,
                                    logoUrl = entry.logoUrl,
                                    categoryName = group.ifEmpty { null },
                                    tvgId = entry.tvgId,
                                )
                                channelCount++
                            }
                            ContentType.MOVIE -> {
                                vodBatch += VodTitle(
                                    sourceId = sourceId,
                                    name = entry.name,
                                    isSeries = false,
                                    posterUrl = entry.logoUrl,
                                    categoryName = group.ifEmpty { null },
                                    streamUrl = entry.streamUrl,
                                )
                                movieCount++
                            }
                            ContentType.SERIES -> {
                                // Group per-episode entries (e.g. "Breaking Bad S01E02") under one
                                // series VodTitle; the episode itself goes to series_episodes. An
                                // entry with no season/episode marker is treated as a standalone
                                // series title (the old behaviour).
                                val info = parseSeriesEpisode(entry.name)
                                if (info != null) {
                                    val key = info.seriesName.lowercase()
                                    val seriesId = seriesIdByName[key] ?: run {
                                        val newId = db.vodTitleDao().insert(
                                            VodTitle(
                                                sourceId = sourceId,
                                                name = info.seriesName,
                                                isSeries = true,
                                                posterUrl = entry.logoUrl,
                                                categoryName = group.ifEmpty { null },
                                            ),
                                        )
                                        seriesIdByName[key] = newId
                                        seriesCount++
                                        newId
                                    }
                                    episodeBatch += SeriesEpisode(
                                        seriesTitleId = seriesId,
                                        season = info.season,
                                        episode = info.episode,
                                        name = info.episodeTitle,
                                        streamUrl = entry.streamUrl,
                                    )
                                } else {
                                    vodBatch += VodTitle(
                                        sourceId = sourceId,
                                        name = entry.name,
                                        isSeries = true,
                                        posterUrl = entry.logoUrl,
                                        categoryName = group.ifEmpty { null },
                                        streamUrl = entry.streamUrl,
                                    )
                                    seriesCount++
                                }
                            }
                        }
                        if (channelBatch.size >= IMPORT_BATCH_SIZE) {
                            db.channelDao().insertAll(channelBatch); channelBatch.clear()
                        }
                        if (vodBatch.size >= IMPORT_BATCH_SIZE) {
                            db.vodTitleDao().insertAll(vodBatch); vodBatch.clear()
                        }
                        if (episodeBatch.size >= IMPORT_BATCH_SIZE) {
                            db.seriesEpisodeDao().insertAll(episodeBatch); episodeBatch.clear()
                        }
                    }
                }
                if (channelBatch.isNotEmpty()) db.channelDao().insertAll(channelBatch)
                if (vodBatch.isNotEmpty()) db.vodTitleDao().insertAll(vodBatch)
                if (episodeBatch.isNotEmpty()) db.seriesEpisodeDao().insertAll(episodeBatch)
                // Populate each series' episodeCount from its grouped rows (shown on tile/detail).
                if (seriesIdByName.isNotEmpty()) db.vodTitleDao().refreshEpisodeCounts(sourceId)
            } catch (e: Throwable) {
                // Roll back a half-imported source so a failed/interrupted import never leaves a
                // dead, selectable entry (and its orphaned rows) behind.
                db.seriesEpisodeDao().deleteForSeries(seriesIdByName.values.toList())
                db.channelDao().deleteForSource(sourceId)
                db.vodTitleDao().deleteForSource(sourceId)
                db.playlistSourceDao().delete(source.copy(id = sourceId))
                throw e
            }

            if (channelCount + movieCount + seriesCount == 0) {
                db.playlistSourceDao().delete(source.copy(id = sourceId))
                throw IllegalStateException("No channels found -- check the playlist URL/format")
            }

            db.categoryDao().upsertAll(categories.values.toList())
            // Fall back to the playlist's own declared EPG feed when the user didn't supply one,
            // so an M3U with a `url-tvg` header populates the Guide automatically.
            if (epgUrl == null && headerEpgUrl != null) db.playlistSourceDao().setEpgUrl(sourceId, headerEpgUrl)
            // Import succeeded -- drop any prior same-URL source so a re-import replaces it.
            replaceDuplicateSources(sourceId)
            settings.setActiveSourceId(sourceId)

            ImportSummary(channels = channelCount, movies = movieCount, series = seriesCount)
        }

    override suspend fun addXtreamSource(
        name: String,
        host: String,
        username: String,
        password: String,
        epgUrl: String?,
    ): ImportSummary = withContext(Dispatchers.IO) {
        val xtream = XtreamClient(host, username, password)
        xtream.authenticate()

        val liveCategories = xtream.getLiveCategories()
        val vodCategories = xtream.getVodCategories()
        val seriesCategories = xtream.getSeriesCategories()
        val liveStreams = xtream.getLiveStreams()
        val vodStreams = xtream.getVodStreams()
        val seriesList = xtream.getSeries()

        val source = PlaylistSource(
            name = name,
            type = SourceType.XTREAM,
            url = host,
            epgUrl = epgUrl,
            lastRefreshedAtMs = System.currentTimeMillis(),
        )
        val sourceId = db.playlistSourceDao().insert(source)
        // Credentials never touch the Room row -- encrypted-at-rest, keyed by the id Room
        // just generated (see CredentialsStore's doc comment for why).
        credentials.save(sourceId, username, password)

        try {
            val liveCatNames = liveCategories.associate { it.id to it.name }
            val vodCatNames = vodCategories.associate { it.id to it.name }
            val seriesCatNames = seriesCategories.associate { it.id to it.name }

            val categories = liveCategories.map { Category(sourceId, ContentType.LIVE, it.name, it.id) } +
                vodCategories.map { Category(sourceId, ContentType.MOVIE, it.name, it.id) } +
                seriesCategories.map { Category(sourceId, ContentType.SERIES, it.name, it.id) }

            val channels = liveStreams.map {
                Channel(
                    sourceId = sourceId,
                    name = it.name,
                    streamUrl = xtream.streamUrl("live", it.id, "m3u8"),
                    logoUrl = it.logo,
                    categoryName = it.categoryId?.let(liveCatNames::get) ?: UNCATEGORIZED_CATEGORY,
                    externalId = it.id,
                    tvgId = it.epgChannelId,
                )
            }
            val movies = vodStreams.map {
                VodTitle(
                    sourceId = sourceId,
                    name = it.name,
                    isSeries = false,
                    posterUrl = it.icon,
                    categoryName = it.categoryId?.let(vodCatNames::get) ?: UNCATEGORIZED_CATEGORY,
                    streamUrl = xtream.streamUrl("movie", it.id, it.containerExtension ?: "mp4"),
                    externalId = it.id,
                )
            }
            val series = seriesList.map {
                VodTitle(
                    sourceId = sourceId,
                    name = it.name,
                    isSeries = true,
                    posterUrl = it.cover,
                    categoryName = it.categoryId?.let(seriesCatNames::get) ?: UNCATEGORIZED_CATEGORY,
                    externalId = it.id,
                )
            }

            db.categoryDao().upsertAll(categories)
            db.channelDao().insertAll(channels)
            db.vodTitleDao().insertAll(movies + series)
            // Import succeeded -- drop any prior same-URL source so a re-import replaces it.
            replaceDuplicateSources(sourceId)
            settings.setActiveSourceId(sourceId)

            ImportSummary(channels = channels.size, movies = movies.size, series = series.size)
        } catch (e: Throwable) {
            // Mirrors addM3uSource's rollback below -- without this, a failure partway through
            // (e.g. categories/channels inserted but the VOD insert throws) left a half-imported,
            // un-rolled-back source row behind: the user sees the real error from this call, but
            // Settings/the source picker would still list a dead, selectable "playlist" with a
            // partial or empty catalog and orphaned rows under it.
            db.channelDao().deleteForSource(sourceId)
            db.vodTitleDao().deleteForSource(sourceId)
            db.categoryDao().deleteForSource(sourceId)
            credentials.clear(sourceId)
            db.playlistSourceDao().delete(source.copy(id = sourceId))
            throw e
        }
    }

    override suspend fun refreshSource(sourceId: Long): ImportSummary = withContext(Dispatchers.IO) {
        val source = db.playlistSourceDao().getById(sourceId)
            ?: throw IllegalStateException("Playlist not found")

        // M3U has no stable per-item id namespace and a streaming/batched importer -- an
        // id-preserving diff isn't worth building for it. Re-import by URL instead: it de-dups on
        // the normalized URL (favorites survive via streamKey; continue-watching is the known M3U
        // tradeoff). Xtream (the id-preserving path) is below.
        if (source.type != SourceType.XTREAM) {
            return@withContext addM3uSource(source.name, source.url, source.epgUrl)
        }

        val (username, password) = credentials.forSource(sourceId)
        if (username == null || password == null) {
            throw IllegalStateException("Saved login for this playlist is missing -- re-add it to refresh.")
        }

        // 1. Fetch the ENTIRE provider catalog first, OUTSIDE any DB write. If the line is limited
        // (ip-limit / connection cap) any of these throws, and we abort before touching a single
        // existing row -- a failed refresh must never degrade a working catalog.
        val xtream = XtreamClient(source.url, username, password)
        xtream.authenticate()
        val liveCategories = xtream.getLiveCategories()
        val vodCategories = xtream.getVodCategories()
        val seriesCategories = xtream.getSeriesCategories()
        val liveStreams = xtream.getLiveStreams()
        val vodStreams = xtream.getVodStreams()
        val seriesList = xtream.getSeries()

        // 2. Empty-guard: a provider that answers 200 with empty arrays (a common ip-limit / glitch
        // shape) must NOT be read as "the user's whole catalog was removed". Abort, keep everything.
        if (liveStreams.isEmpty() && vodStreams.isEmpty() && seriesList.isEmpty()) {
            throw IllegalStateException("The provider returned an empty catalog -- your line may be limited. Nothing was changed.")
        }

        val liveCatNames = liveCategories.associate { it.id to it.name }
        val vodCatNames = vodCategories.associate { it.id to it.name }
        val seriesCatNames = seriesCategories.associate { it.id to it.name }

        val categories = liveCategories.map { Category(sourceId, ContentType.LIVE, it.name, it.id) } +
            vodCategories.map { Category(sourceId, ContentType.MOVIE, it.name, it.id) } +
            seriesCategories.map { Category(sourceId, ContentType.SERIES, it.name, it.id) }
        val channels = liveStreams.map {
            Channel(
                sourceId = sourceId,
                name = it.name,
                streamUrl = xtream.streamUrl("live", it.id, "m3u8"),
                logoUrl = it.logo,
                categoryName = it.categoryId?.let(liveCatNames::get) ?: UNCATEGORIZED_CATEGORY,
                externalId = it.id,
                tvgId = it.epgChannelId,
            )
        }
        val movies = vodStreams.map {
            VodTitle(
                sourceId = sourceId,
                name = it.name,
                isSeries = false,
                posterUrl = it.icon,
                categoryName = it.categoryId?.let(vodCatNames::get) ?: UNCATEGORIZED_CATEGORY,
                streamUrl = xtream.streamUrl("movie", it.id, it.containerExtension ?: "mp4"),
                externalId = it.id,
            )
        }
        val series = seriesList.map {
            VodTitle(
                sourceId = sourceId,
                name = it.name,
                isSeries = true,
                posterUrl = it.cover,
                categoryName = it.categoryId?.let(seriesCatNames::get) ?: UNCATEGORIZED_CATEGORY,
                externalId = it.id,
            )
        }

        // 3. Apply the whole diff atomically: readers never see a half-synced catalog, and a crash
        // mid-write rolls back to the pre-refresh state.
        db.withTransaction {
            upsertChannels(sourceId, channels)
            upsertTitles(sourceId, isSeries = false, incoming = movies)
            upsertTitles(sourceId, isSeries = true, incoming = series)
            // Categories aren't referenced by row id anywhere (favorites/continue-watching key on
            // items, not categories), so a plain rebuild is safe and simplest.
            db.categoryDao().deleteForSource(sourceId)
            db.categoryDao().upsertAll(categories)
            db.playlistSourceDao().setLastRefreshed(sourceId, System.currentTimeMillis())
        }

        ImportSummary(channels = channels.size, movies = movies.size, series = series.size)
    }

    override suspend fun deleteSource(sourceId: Long): Unit = withContext(Dispatchers.IO) {
        val source = db.playlistSourceDao().getById(sourceId) ?: return@withContext
        // Order matters: the favorites/continue-watching/EPG purges read the catalog rows they key
        // off, so they must run BEFORE those rows are deleted; series_episodes before vod_titles
        // (its delete subquery reads vod_titles). All atomic so a crash mid-wipe rolls back.
        db.withTransaction {
            db.favoriteDao().deleteForSource(sourceId)
            db.continueWatchingDao().deleteForSource(sourceId)
            db.epgProgramDao().deleteForSource(sourceId)
            db.seriesEpisodeDao().deleteForSource(sourceId)
            db.channelDao().deleteForSource(sourceId)
            db.vodTitleDao().deleteForSource(sourceId)
            db.categoryDao().deleteForSource(sourceId)
            db.playlistSourceDao().delete(source)
        }
        // Credentials live in encrypted prefs (not Room), so clear them outside the DB transaction.
        credentials.clear(sourceId)
        // Don't leave the active pointer dangling at a source that no longer exists.
        if (settings.activeSourceId.first() == sourceId) settings.clearActiveSourceId()
    }

    /**
     * Id-preserving channel sync: match incoming provider channels to existing rows by
     * [RowKey.matchKey] (`externalId ?: name ?: streamUrl`). Matched rows are re-inserted under
     * their EXISTING id (REPLACE updates in place -- the id, which nothing here changes, is what
     * continue-watching/favorites resolve against), new items insert fresh (id 0 -> autogenerate),
     * and existing rows the provider no longer lists are deleted.
     */
    private suspend fun upsertChannels(sourceId: Long, incoming: List<Channel>) {
        val existing = db.channelDao().matchKeys(sourceId)
        val existingIdByKey = HashMap<String, Long>(existing.size)
        for (r in existing) r.matchKey?.let { existingIdByKey.putIfAbsent(it, r.id) }

        val usedIds = HashSet<Long>()
        val seenKeys = HashSet<String>()
        val toInsert = ArrayList<Channel>(incoming.size)
        for (c in incoming) {
            val key = c.externalId ?: c.name ?: c.streamUrl
            if (key != null && !seenKeys.add(key)) continue // one row per provider item
            val existingId = key?.let { existingIdByKey[it] }
            if (existingId != null) {
                toInsert += c.copy(id = existingId)
                usedIds += existingId
            } else {
                toInsert += c.copy(id = 0)
            }
        }
        db.channelDao().insertAll(toInsert)

        val staleIds = existing.mapNotNull { it.id.takeUnless(usedIds::contains) }
        staleIds.chunked(DELETE_CHUNK).forEach { db.channelDao().deleteByIds(it) }
    }

    /**
     * Id-preserving movie/series sync (see [upsertChannels]). Additionally carries over the lazily-
     * loaded enrichment (plot/cast/ratings/episodeCount/metadataFetched) of any matched title, so a
     * refresh doesn't wipe metadata the user already fetched by opening the Detail screen. Episodes
     * themselves stay valid because the series' row id is preserved (they reference it).
     */
    private suspend fun upsertTitles(sourceId: Long, isSeries: Boolean, incoming: List<VodTitle>) {
        val existing = db.vodTitleDao().matchKeys(sourceId, isSeries)
        val existingIdByKey = HashMap<String, Long>(existing.size)
        for (r in existing) r.matchKey?.let { existingIdByKey.putIfAbsent(it, r.id) }
        val enrichedByKey = db.vodTitleDao().enrichedTitles(sourceId)
            .filter { it.isSeries == isSeries }
            .associateBy { it.externalId ?: it.name ?: it.streamUrl.orEmpty() }

        val usedIds = HashSet<Long>()
        val seenKeys = HashSet<String>()
        val toInsert = ArrayList<VodTitle>(incoming.size)
        for (v in incoming) {
            val key = v.externalId ?: v.name ?: v.streamUrl
            if (key != null && !seenKeys.add(key)) continue
            val existingId = key?.let { existingIdByKey[it] }
            if (existingId != null) {
                val enriched = key?.let { enrichedByKey[it] }
                toInsert += if (enriched != null) {
                    v.copy(
                        id = existingId,
                        plot = enriched.plot, castList = enriched.castList, director = enriched.director,
                        genre = enriched.genre, year = v.year ?: enriched.year, rating = v.rating ?: enriched.rating,
                        imdbRating = enriched.imdbRating, rtRating = enriched.rtRating,
                        episodeCount = enriched.episodeCount, metadataFetched = enriched.metadataFetched,
                    )
                } else {
                    v.copy(id = existingId)
                }
                usedIds += existingId
            } else {
                toInsert += v.copy(id = 0)
            }
        }
        db.vodTitleDao().insertAll(toInsert)

        // Removed titles: their grouped episodes go first (no FK cascade), then the titles. Only the
        // ids actually being deleted, so a preserved series keeps its lazily-loaded episodes.
        val staleIds = existing.mapNotNull { it.id.takeUnless(usedIds::contains) }
        if (staleIds.isNotEmpty()) {
            staleIds.chunked(DELETE_CHUNK).forEach { db.seriesEpisodeDao().deleteForSeries(it) }
            staleIds.chunked(DELETE_CHUNK).forEach { db.vodTitleDao().deleteByIds(it) }
        }
    }

    /**
     * De-dup on re-import: once a fresh import of [keepId] has fully succeeded, drop any *other*
     * source whose URL normalizes to the same value (plus its catalog rows, categories, and stored
     * credentials), so re-adding the same portal/URL replaces the old entry instead of leaving a
     * duplicate source and its orphaned catalog behind. Runs only after success -- a failed import
     * has already rolled its own new source back and never touches the user's existing working one.
     */
    private suspend fun replaceDuplicateSources(keepId: Long) {
        val sources = db.playlistSourceDao().observeAll().first()
        val keepKey = sources.firstOrNull { it.id == keepId }?.let { normalizeSourceUrl(it.url) } ?: return
        sources.filter { it.id != keepId && normalizeSourceUrl(it.url) == keepKey }.forEach { dup ->
            db.channelDao().deleteForSource(dup.id)
            // Before vod_titles: the episode delete subquery reads vod_titles for this source.
            db.seriesEpisodeDao().deleteForSource(dup.id)
            db.vodTitleDao().deleteForSource(dup.id)
            db.categoryDao().deleteForSource(dup.id)
            credentials.clear(dup.id)
            db.playlistSourceDao().delete(dup)
        }
    }

    /**
     * Streams the playlist body line-by-line to [consume] instead of buffering the whole
     * response into a String (a large m3u_plus can exceed the app heap -> OutOfMemoryError).
     * The reader and HTTP response are closed once [consume] returns.
     */
    private suspend fun <T> streamPlaylist(url: String, consume: suspend (Sequence<String>) -> T): T {
        val request = Request.Builder().url(url).build()
        return try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("Server returned HTTP ${response.code} -- check the playlist URL")
                }
                val body = response.body ?: throw IllegalStateException("Empty response body")
                body.charStream().buffered().use { reader -> consume(reader.lineSequence()) }
            }
        } catch (e: XtreamException) {
            throw e
        } catch (e: IllegalStateException) {
            throw e
        } catch (e: Exception) {
            // Distinguishes common network-failure shapes instead of one catch-all bucket
            // (mirrors XtreamClient's networkErrorMessage), per qa-reviewer's Phase 1 finding.
            val message = when (e) {
                is java.net.UnknownHostException -> "Could not find that server -- check the playlist URL and your connection"
                is java.net.SocketTimeoutException -> "The server took too long to respond -- check your connection and try again"
                is java.net.ConnectException -> "Could not connect to the server -- it may be down"
                is javax.net.ssl.SSLException -> "Secure connection failed -- check the playlist URL"
                else -> "Could not fetch playlist: ${e.message}"
            }
            throw IllegalStateException(message, e)
        }
    }
}
