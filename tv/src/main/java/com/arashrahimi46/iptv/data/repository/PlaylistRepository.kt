package com.arashrahimi46.iptv.data.repository

import android.content.Context
import com.arashrahimi46.iptv.data.db.AppDatabase
import com.arashrahimi46.iptv.data.model.Category
import com.arashrahimi46.iptv.data.model.Channel
import com.arashrahimi46.iptv.data.model.ContentType
import com.arashrahimi46.iptv.data.model.PlaylistSource
import com.arashrahimi46.iptv.data.model.SeriesEpisode
import com.arashrahimi46.iptv.data.model.SourceType
import com.arashrahimi46.iptv.data.model.VodTitle
import com.arashrahimi46.iptv.data.parser.M3uParser
import com.arashrahimi46.iptv.data.parser.XtreamClient
import com.arashrahimi46.iptv.data.parser.XtreamException
import com.arashrahimi46.iptv.data.settings.UserSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/** Derived counts shown on the onboarding Confirm step, from a *real* parse -- never hardcoded. */
data class ImportSummary(val channels: Int, val movies: Int, val series: Int)

/**
 * Repository pattern wrapping Room DAOs + the M3U/Xtream parsers behind a
 * single surface. All network/parsing work runs on [Dispatchers.IO].
 */
interface PlaylistRepository {
    fun observeSources(): Flow<List<PlaylistSource>>
    suspend fun hasAnySource(): Boolean
    fun observeChannels(sourceId: Long): Flow<List<Channel>>
    fun observeMovies(sourceId: Long): Flow<List<VodTitle>>
    fun observeSeries(sourceId: Long): Flow<List<VodTitle>>

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
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    override fun observeSources(): Flow<List<PlaylistSource>> = db.playlistSourceDao().observeAll()

    override suspend fun hasAnySource(): Boolean = db.playlistSourceDao().count() > 0

    override fun observeChannels(sourceId: Long): Flow<List<Channel>> = db.channelDao().observeForSource(sourceId)
    override fun observeMovies(sourceId: Long): Flow<List<VodTitle>> = db.vodTitleDao().observeMoviesForSource(sourceId)
    override fun observeSeries(sourceId: Long): Flow<List<VodTitle>> = db.vodTitleDao().observeSeriesForSource(sourceId)

    override fun observeSeriesEpisodes(seriesTitleId: Long): Flow<List<SeriesEpisode>> =
        db.seriesEpisodeDao().observeForSeries(seriesTitleId)

    override suspend fun ensureSeriesEpisodesLoaded(vodTitle: VodTitle): Unit = withContext(Dispatchers.IO) {
        if (!vodTitle.isSeries || vodTitle.externalId == null) return@withContext
        if (db.seriesEpisodeDao().countForSeries(vodTitle.id) > 0) return@withContext
        val source = db.playlistSourceDao().getById(vodTitle.sourceId) ?: return@withContext
        if (source.type != SourceType.XTREAM || source.username == null || source.password == null) return@withContext

        val xtream = XtreamClient(source.url, source.username, source.password)
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
        if (entities.isNotEmpty()) db.seriesEpisodeDao().insertAll(entities)
    }

    override suspend fun addM3uSource(name: String, url: String, epgUrl: String?): ImportSummary =
        withContext(Dispatchers.IO) {
            val body = fetchText(url)
            val entries = M3uParser.parse(body)
            if (entries.isEmpty()) {
                throw IllegalStateException("No channels found -- check the playlist URL/format")
            }

            val source = PlaylistSource(name = name, type = SourceType.M3U, url = url, epgUrl = epgUrl)
            val sourceId = db.playlistSourceDao().insert(source)

            // M3U has no explicit content-type field; classify by group-title keyword as a best-effort
            // heuristic (real playlists commonly label VOD groups "Movies"/"VOD" and "Series"/"Shows").
            // Scoped to the M3U path ONLY -- addXtreamSource() below uses the authoritative
            // get_live_streams/get_vod_streams/get_series endpoints directly, no heuristic involved.
            // Known real-world false positive: this substring match misclassifies live/linear
            // channel groups whose names happen to contain these words, e.g. "USA Movies HD"
            // (a 24/7 linear movie channel, not on-demand) or "Kids Shows" (a live kids bouquet)
            // would be routed into Movies/Series instead of Live TV. Accepted tradeoff for v1
            // since M3U provides no authoritative type signal; revisit if this proves common
            // enough in real provider playlists (per qa-reviewer Phase 1 finding).
            val channels = mutableListOf<Channel>()
            val movies = mutableListOf<VodTitle>()
            val series = mutableListOf<VodTitle>()
            val categories = linkedMapOf<Pair<ContentType, String>, Category>()

            entries.forEach { entry ->
                val group = entry.groupTitle?.trim().orEmpty()
                val groupLower = group.lowercase()
                val type = when {
                    "series" in groupLower || "show" in groupLower -> ContentType.SERIES
                    "movie" in groupLower || "vod" in groupLower -> ContentType.MOVIE
                    else -> ContentType.LIVE
                }
                if (group.isNotEmpty()) {
                    categories.putIfAbsent(type to group, Category(sourceId, type, group))
                }
                when (type) {
                    ContentType.LIVE -> channels += Channel(
                        sourceId = sourceId,
                        name = entry.name,
                        streamUrl = entry.streamUrl,
                        logoUrl = entry.logoUrl,
                        categoryName = group.ifEmpty { null },
                        tvgId = entry.tvgId,
                    )
                    ContentType.MOVIE -> movies += VodTitle(
                        sourceId = sourceId,
                        name = entry.name,
                        isSeries = false,
                        posterUrl = entry.logoUrl,
                        categoryName = group.ifEmpty { null },
                        streamUrl = entry.streamUrl,
                    )
                    ContentType.SERIES -> series += VodTitle(
                        sourceId = sourceId,
                        name = entry.name,
                        isSeries = true,
                        posterUrl = entry.logoUrl,
                        categoryName = group.ifEmpty { null },
                        streamUrl = entry.streamUrl,
                    )
                }
            }

            db.categoryDao().upsertAll(categories.values.toList())
            db.channelDao().insertAll(channels)
            db.vodTitleDao().insertAll(movies + series)
            settings.setActiveSourceId(sourceId)

            ImportSummary(channels = channels.size, movies = movies.size, series = series.size)
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
            username = username,
            password = password,
            epgUrl = epgUrl,
        )
        val sourceId = db.playlistSourceDao().insert(source)

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

    private fun fetchText(url: String): String {
        val request = Request.Builder().url(url).build()
        return try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("Server returned HTTP ${response.code}")
                }
                response.body?.string() ?: throw IllegalStateException("Empty response body")
            }
        } catch (e: XtreamException) {
            throw e
        } catch (e: IllegalStateException) {
            throw e
        } catch (e: Exception) {
            throw IllegalStateException("Could not fetch playlist: ${e.message}", e)
        }
    }
}
