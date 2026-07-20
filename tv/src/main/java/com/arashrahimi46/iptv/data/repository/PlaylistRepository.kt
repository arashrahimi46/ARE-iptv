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
    private val credentials = CredentialsStore(context)
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

            // Classification heuristic lives in M3uGroupClassifier.kt (unit-tested) -- see its
            // doc comment for the M3U-only scope and the known real-world false-positive pattern.
            val channels = mutableListOf<Channel>()
            val movies = mutableListOf<VodTitle>()
            val series = mutableListOf<VodTitle>()
            val categories = linkedMapOf<Pair<ContentType, String>, Category>()

            entries.forEach { entry ->
                val group = entry.groupTitle?.trim().orEmpty()
                val type = classifyM3uGroup(group)
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

    private fun fetchText(url: String): String {
        val request = Request.Builder().url(url).build()
        return try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("Server returned HTTP ${response.code} -- check the playlist URL")
                }
                response.body?.string() ?: throw IllegalStateException("Empty response body")
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
