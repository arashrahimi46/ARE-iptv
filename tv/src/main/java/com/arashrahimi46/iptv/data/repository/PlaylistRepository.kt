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
import com.arashrahimi46.iptv.data.parser.XtreamClient
import com.arashrahimi46.iptv.data.parser.parseSeriesEpisode
import com.arashrahimi46.iptv.data.parser.XtreamException
import com.arashrahimi46.iptv.data.settings.CredentialsStore
import com.arashrahimi46.iptv.data.settings.UserSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/** Derived counts shown on the onboarding Confirm step, from a *real* parse -- never hardcoded. */
data class ImportSummary(val channels: Int, val movies: Int, val series: Int)

/** Xtream `get.php` keys are lowercase by spec, and the server is case-sensitive on them. */
private val XTREAM_QUERY_KEYS = setOf("username", "password", "type", "output")

/** Rows flushed to Room per batch during a streaming M3U import (bounds peak memory). */
private const val IMPORT_BATCH_SIZE = 1500

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

    override suspend fun hasAnySource(): Boolean = db.playlistSourceDao().count() > 0

    private val channelDao get() = db.channelDao()
    private val vodDao get() = db.vodTitleDao()

    override fun pagingChannels(sourceId: Long, category: String?): PagingSource<Int, Channel> =
        if (category == null) channelDao.pagingAll(sourceId) else channelDao.pagingByCategory(sourceId, category)
    override fun pagingMovies(sourceId: Long, category: String?): PagingSource<Int, VodTitle> =
        if (category == null) vodDao.paging(sourceId, false) else vodDao.pagingByCategory(sourceId, false, category)
    override fun pagingSeries(sourceId: Long, category: String?): PagingSource<Int, VodTitle> =
        if (category == null) vodDao.paging(sourceId, true) else vodDao.pagingByCategory(sourceId, true, category)

    override fun channelCategoryCounts(sourceId: Long): Flow<List<CategoryCount>> = channelDao.observeCategoryCounts(sourceId)
    override fun movieCategoryCounts(sourceId: Long): Flow<List<CategoryCount>> = vodDao.observeCategoryCounts(sourceId, false)
    override fun seriesCategoryCounts(sourceId: Long): Flow<List<CategoryCount>> = vodDao.observeCategoryCounts(sourceId, true)
    override fun channelCount(sourceId: Long): Flow<Int> = channelDao.observeCountForSource(sourceId)
    override fun movieCount(sourceId: Long): Flow<Int> = vodDao.observeCount(sourceId, false)
    override fun seriesCount(sourceId: Long): Flow<Int> = vodDao.observeCount(sourceId, true)

    override fun topChannels(sourceId: Long, limit: Int): Flow<List<Channel>> = channelDao.observeTop(sourceId, limit)
    override fun topMovies(sourceId: Long, limit: Int): Flow<List<VodTitle>> = vodDao.observeTop(sourceId, false, limit)
    override fun topSeries(sourceId: Long, limit: Int): Flow<List<VodTitle>> = vodDao.observeTop(sourceId, true, limit)

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

    override suspend fun addM3uSource(name: String, url: String, epgUrl: String?): ImportSummary =
        withContext(Dispatchers.IO) {
            // Normalize a pasted link (e.g. capitalized `Password`) so it doesn't 404, then
            // persist the normalized form so later refreshes hit the same working URL.
            val normalizedUrl = normalizeSourceUrl(url)

            val source = PlaylistSource(name = name, type = SourceType.M3U, url = normalizedUrl, epgUrl = epgUrl)
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

            try {
                // Stream + batch: a large m3u_plus (100s of MB) is parsed line-by-line and
                // flushed to Room in bounded chunks, so the whole body is never held in memory
                // at once (loading it as one String OOM'd -- the response can exceed the heap).
                streamPlaylist(normalizedUrl) { lines ->
                    M3uParser.parse(lines).forEach { entry ->
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
        )
        val sourceId = db.playlistSourceDao().insert(source)
        // Credentials never touch the Room row -- encrypted-at-rest, keyed by the id Room
        // just generated (see CredentialsStore's doc comment for why).
        credentials.save(sourceId, username, password)

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
                categoryName = it.categoryId?.let(liveCatNames::get),
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
                categoryName = it.categoryId?.let(vodCatNames::get),
                streamUrl = xtream.streamUrl("movie", it.id, "mp4"),
                externalId = it.id,
            )
        }
        val series = seriesList.map {
            VodTitle(
                sourceId = sourceId,
                name = it.name,
                isSeries = true,
                posterUrl = it.cover,
                categoryName = it.categoryId?.let(seriesCatNames::get),
                externalId = it.id,
            )
        }

        db.categoryDao().upsertAll(categories)
        db.channelDao().insertAll(channels)
        db.vodTitleDao().insertAll(movies + series)
        settings.setActiveSourceId(sourceId)

        ImportSummary(channels = channels.size, movies = movies.size, series = series.size)
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
