package com.arashrahimi46.iptv.data.db

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.arashrahimi46.iptv.data.model.Category
import com.arashrahimi46.iptv.data.model.Channel
import com.arashrahimi46.iptv.data.model.ContentType
import com.arashrahimi46.iptv.data.model.ContinueWatchingEntry
import com.arashrahimi46.iptv.data.model.DirectStream
import com.arashrahimi46.iptv.data.model.EPGProgram
import com.arashrahimi46.iptv.data.model.Favorite
import com.arashrahimi46.iptv.data.model.PlaylistSource
import com.arashrahimi46.iptv.data.model.Recording
import com.arashrahimi46.iptv.data.model.RecordingStatus
import com.arashrahimi46.iptv.data.model.SeriesEpisode
import com.arashrahimi46.iptv.data.model.VodTitle
import kotlinx.coroutines.flow.Flow

/**
 * One `(categoryName, count)` row from a browse catalog's `GROUP BY categoryName`.
 * Drives the browse category column without loading the catalog rows themselves --
 * essential for large catalogs (300k+ titles) where the old "derive categories from
 * the fully-loaded list" approach OOM'd.
 */
data class CategoryCount(val name: String, val count: Int)

/**
 * A catalog row's stable identity for id-preserving refresh sync: its Room row [id] plus the
 * [matchKey] = `COALESCE(externalId, name, streamUrl)` (the same expression favorites key on). The
 * refresh diff matches incoming provider items to existing rows by [matchKey] so an unchanged item
 * keeps its row [id] -- continue-watching and lazily-loaded metadata reference that id and must
 * survive a refresh. See [com.arashrahimi46.iptv.data.repository.PlaylistRepository.refreshSource].
 */
data class RowKey(val id: Long, val matchKey: String?)

/**
 * A channel's EPG identity, without the rest of the row. [EpgRepository.refresh] matches a whole
 * XMLTV export against every channel of a source, and loading full [Channel] rows for a 100k-channel
 * catalog to read two fields is exactly the allocation that made "just match everything" look
 * expensive. Only channels with a usable `tvgId` can match, so the query filters them out in SQL.
 */
data class ChannelTvgId(val id: Long, val tvgId: String)

/**
 * One programme-search hit: the programme plus enough of its channel to render a result row and to
 * navigate the guide to it ([categoryName] is what the Guide's category filter is keyed on, so
 * jumping to a hit outside the current category needs it).
 */
data class EpgSearchHit(
    val channelId: Long,
    val channelName: String,
    val logoUrl: String?,
    val categoryName: String?,
    val title: String,
    val startMs: Long,
    val endMs: Long,
)

@Dao
interface PlaylistSourceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(source: PlaylistSource): Long

    @Query("SELECT * FROM playlist_sources ORDER BY createdAtMs DESC")
    fun observeAll(): Flow<List<PlaylistSource>>

    /** Live single-source read -- drives the Settings "Last updated" label + refresh-overdue badge. */
    @Query("SELECT * FROM playlist_sources WHERE id = :id")
    fun observeById(id: Long): Flow<PlaylistSource?>

    /** Stamp a successful sync (initial import or manual refresh) so staleness can be computed. */
    @Query("UPDATE playlist_sources SET lastRefreshedAtMs = :ts WHERE id = :id")
    suspend fun setLastRefreshed(id: Long, ts: Long)

    /** Store the provider account/server metadata JSON captured on the latest auth (Provider panel). */
    @Query("UPDATE playlist_sources SET accountInfoJson = :json WHERE id = :id")
    suspend fun setAccountInfo(id: Long, json: String?)

    @Query("SELECT * FROM playlist_sources WHERE id = :id")
    suspend fun getById(id: Long): PlaylistSource?

    @Query("SELECT COUNT(*) FROM playlist_sources")
    suspend fun count(): Int

    @Query("UPDATE playlist_sources SET epgUrl = :epgUrl WHERE id = :id")
    suspend fun setEpgUrl(id: Long, epgUrl: String?)

    /** Display-name only. Everything else is keyed by id, so a rename touches nothing downstream. */
    @Query("UPDATE playlist_sources SET name = :name WHERE id = :id")
    suspend fun setName(id: Long, name: String)

    @Delete
    suspend fun delete(source: PlaylistSource)
}

@Dao
interface CategoryDao {
    @Upsert
    suspend fun upsertAll(categories: List<Category>)

    @Query("DELETE FROM categories WHERE sourceId = :sourceId")
    suspend fun deleteForSource(sourceId: Long)

    @Query("SELECT * FROM categories WHERE sourceId = :sourceId AND contentType = :type ORDER BY name")
    fun observeForSourceAndType(sourceId: Long, type: ContentType): Flow<List<Category>>
}

@Dao
interface ChannelDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(channels: List<Channel>)

    @Query("DELETE FROM channels WHERE sourceId = :sourceId")
    suspend fun deleteForSource(sourceId: Long)

    /** Refresh sync: id + stable key of every existing channel, to diff against the incoming set. */
    @Query("SELECT id, COALESCE(externalId, name, streamUrl) AS matchKey FROM channels WHERE sourceId = :sourceId")
    suspend fun matchKeys(sourceId: Long): List<RowKey>

    /** Refresh sync: drop the rows the provider no longer lists (matched out by id). */
    @Query("DELETE FROM channels WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("SELECT * FROM channels WHERE sourceId = :sourceId ORDER BY name")
    fun observeForSource(sourceId: Long): Flow<List<Channel>>

    @Query("SELECT * FROM channels WHERE id = :channelId LIMIT 1")
    suspend fun getById(channelId: Long): Channel?

    @Query("SELECT * FROM channels WHERE sourceId = :sourceId ORDER BY name LIMIT :limit")
    fun observeForSourceLimited(sourceId: Long, limit: Int): Flow<List<Channel>>

    @Query("SELECT COUNT(*) FROM channels WHERE sourceId = :sourceId")
    suspend fun countForSource(sourceId: Long): Int

    /** EPG match set: every channel of the source that carries a `tvg-id`, as id + id only.
     *  See [ChannelTvgId] for why this isn't `observeForSource`. */
    @Query("SELECT id, tvgId FROM channels WHERE sourceId = :sourceId AND tvgId IS NOT NULL AND tvgId != ''")
    suspend fun tvgIdsForSource(sourceId: Long): List<ChannelTvgId>

    // --- Large-catalog browse (Paging 3): only the visible window is ever in memory. ---

    @Query("SELECT * FROM channels WHERE sourceId = :sourceId ORDER BY name, id")
    fun pagingAll(sourceId: Long): PagingSource<Int, Channel>

    @Query("SELECT * FROM channels WHERE sourceId = :sourceId AND categoryName = :category ORDER BY name, id")
    fun pagingByCategory(sourceId: Long, category: String): PagingSource<Int, Channel>

    /** Favorited channels of the active source, most-recently-favorited first -- backs the
     *  "Favorites" pseudo-category at the top of the browse column. Keyed the same way as the
     *  favorite-icon queries (COALESCE(externalId, name, streamUrl)). */
    @Query(
        "SELECT c.* FROM channels c JOIN favorites f ON f.streamKey = COALESCE(c.externalId, c.name, c.streamUrl) " +
            "WHERE c.sourceId = :sourceId AND f.contentType = 'LIVE' ORDER BY f.addedAtMs DESC, c.id",
    )
    fun pagingFavorites(sourceId: Long): PagingSource<Int, Channel>

    /** Live count of favorited channels in the source -- the "Favorites" browse-row total. */
    @Query(
        "SELECT COUNT(*) FROM channels c JOIN favorites f ON f.streamKey = COALESCE(c.externalId, c.name, c.streamUrl) " +
            "WHERE c.sourceId = :sourceId AND f.contentType = 'LIVE'",
    )
    fun observeFavoriteCount(sourceId: Long): Flow<Int>

    /** Category column data (GROUP BY) -- no catalog rows loaded. */
    @Query("SELECT categoryName AS name, COUNT(*) AS count FROM channels WHERE sourceId = :sourceId AND categoryName IS NOT NULL GROUP BY categoryName ORDER BY categoryName")
    fun observeCategoryCounts(sourceId: Long): Flow<List<CategoryCount>>

    @Query("SELECT COUNT(*) FROM channels WHERE sourceId = :sourceId")
    fun observeCountForSource(sourceId: Long): Flow<Int>

    /** Bounded rail/preview load (Home) -- never the whole catalog. */
    @Query("SELECT * FROM channels WHERE sourceId = :sourceId ORDER BY name LIMIT :limit")
    fun observeTop(sourceId: Long, limit: Int): Flow<List<Channel>>

    /**
     * Diverse bounded candidate pool for the curated Live-now rail: a deterministic id-hash spread
     * instead of the alphabetical head, so the sample covers the whole catalog (not just "A…"
     * channels, which clustered near-duplicate quality variants) before [HomeRailCurator] de-dups
     * and personalizes. id-based, so the Flow doesn't re-shuffle on every emission.
     * // monolean: full-table scan on catalog change (no index on the hash) -- acceptable, same
     * // trigger/cost class as the GROUP BY category counts; upgrade to a materialized sample if slow.
     */
    @Query("SELECT * FROM channels WHERE sourceId = :sourceId ORDER BY (id * 2654435761) % 2147483647 LIMIT :limit")
    fun observeSample(sourceId: Long, limit: Int): Flow<List<Channel>>

    /** Category weights from the user's favorite channels -- Live-now personalization signal. */
    @Query(
        "SELECT c.categoryName AS name, COUNT(*) AS count FROM channels c " +
            "JOIN favorites f ON f.streamKey = COALESCE(c.externalId, c.name, c.streamUrl) " +
            "WHERE c.sourceId = :sourceId AND f.contentType = 'LIVE' AND c.categoryName IS NOT NULL " +
            "GROUP BY c.categoryName",
    )
    fun favoriteCategoryCounts(sourceId: Long): Flow<List<CategoryCount>>

    /** DB-side search (Search screen) -- LIKE + LIMIT instead of in-memory filtering. */
    @Query("SELECT * FROM channels WHERE sourceId = :sourceId AND name LIKE '%' || :query || '%' ORDER BY name LIMIT :limit")
    suspend fun search(sourceId: Long, query: String, limit: Int): List<Channel>

    @Query("SELECT * FROM channels WHERE sourceId = :sourceId AND categoryName = :category ORDER BY name LIMIT :limit")
    suspend fun byCategory(sourceId: Long, category: String, limit: Int): List<Channel>

    /** Resolve favorited ids to rows (Favorites) without loading the catalog. */
    @Query("SELECT * FROM channels WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<Channel>

    /** Resolve favorited channel keys to the active source's current rows (see [Favorite.streamKey]). */
    @Query("SELECT * FROM channels WHERE sourceId = :sourceId AND COALESCE(externalId, name, streamUrl) IN (:keys)")
    suspend fun channelsByFavoriteKeys(sourceId: Long, keys: List<String>): List<Channel>

    /** Ordered channel ids only (player prev/next nav) -- ids, not full rows, for large catalogs. */
    @Query("SELECT id FROM channels WHERE sourceId = :sourceId ORDER BY name")
    suspend fun idsForSource(sourceId: Long): List<Long>

    /** Stream-health fallback (P0.1): another catalog entry for the same logical channel
     * (same name, different row -- e.g. a duplicate listing from another category/source)
     * to switch to once retry/backoff on [excludeId] is exhausted. */
    @Query("SELECT * FROM channels WHERE name = :name AND id != :excludeId ORDER BY id LIMIT 1")
    suspend fun findAlternateByName(name: String, excludeId: Long): Channel?

    /** Same as [findAlternateByName] but returns SEVERAL candidates, so the player can skip the
     *  ones it has already exhausted this session. With only one candidate the fallback ping-ponged
     *  A -> B -> A forever on any duplicated channel name, reconnecting to the provider every hop. */
    @Query("SELECT * FROM channels WHERE name = :name AND id != :excludeId ORDER BY id LIMIT 8")
    suspend fun alternatesByName(name: String, excludeId: Long): List<Channel>
}

@Dao
interface VodTitleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(titles: List<VodTitle>)

    /** Single insert returning the generated id -- used to create a series parent row before its
     * grouped [SeriesEpisode]s (which reference it) are batched in during M3U import. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(title: VodTitle): Long

    @Query("DELETE FROM vod_titles WHERE sourceId = :sourceId")
    suspend fun deleteForSource(sourceId: Long)

    /** Refresh sync: id + stable key of every existing movie/series (kept per isSeries so a movie
     * and a series that share an externalId never collide), to diff against the incoming set. */
    @Query("SELECT id, COALESCE(externalId, name, streamUrl) AS matchKey FROM vod_titles WHERE sourceId = :sourceId AND isSeries = :isSeries")
    suspend fun matchKeys(sourceId: Long, isSeries: Boolean): List<RowKey>

    /** Refresh sync: the enriched titles (lazily-loaded plot/cast/ratings/episodeCount) whose data
     * must be carried over onto the refreshed row instead of being wiped by a plain re-insert. */
    @Query("SELECT * FROM vod_titles WHERE sourceId = :sourceId AND metadataFetched = 1")
    suspend fun enrichedTitles(sourceId: Long): List<VodTitle>

    /** Refresh sync: drop the titles the provider no longer lists (matched out by id). */
    @Query("DELETE FROM vod_titles WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    /** Recompute [VodTitle.episodeCount] from the grouped `series_episodes` rows for a source's
     * series (one correlated UPDATE, index-backed on seriesTitleId) -- run once after M3U import. */
    @Query("UPDATE vod_titles SET episodeCount = (SELECT COUNT(*) FROM series_episodes WHERE series_episodes.seriesTitleId = vod_titles.id) WHERE sourceId = :sourceId AND isSeries = 1")
    suspend fun refreshEpisodeCounts(sourceId: Long)

    /** Set one series' episode count -- Xtream loads episodes lazily on Detail view, not at import. */
    @Query("UPDATE vod_titles SET episodeCount = :count WHERE id = :id")
    suspend fun setEpisodeCount(id: Long, count: Int)

    /** Persist rich metadata fetched lazily on first Detail view (Xtream info + OMDb). [fetched]
     * marks whether a real source was actually queried, so a later-added OMDb key can still enrich. */
    @Query(
        "UPDATE vod_titles SET plot = :plot, castList = :castList, director = :director, genre = :genre, " +
            "year = :year, rating = :rating, imdbRating = :imdbRating, rtRating = :rtRating, metadataFetched = :fetched WHERE id = :id",
    )
    suspend fun setMetadata(
        id: Long,
        plot: String?,
        castList: String?,
        director: String?,
        genre: String?,
        year: String?,
        rating: String?,
        imdbRating: String?,
        rtRating: String?,
        fetched: Boolean,
    )

    // --- Large-catalog browse (Paging 3): only the visible window is ever in memory. ---

    @Query("SELECT * FROM vod_titles WHERE sourceId = :sourceId AND isSeries = :isSeries ORDER BY name, id")
    fun paging(sourceId: Long, isSeries: Boolean): PagingSource<Int, VodTitle>

    @Query("SELECT * FROM vod_titles WHERE sourceId = :sourceId AND isSeries = :isSeries AND categoryName = :category ORDER BY name, id")
    fun pagingByCategory(sourceId: Long, isSeries: Boolean, category: String): PagingSource<Int, VodTitle>

    /** Favorited movies/series of the active source, most-recently-favorited first -- backs the
     *  "Favorites" pseudo-category at the top of the browse column. [contentType] (MOVIE/SERIES)
     *  is correlated with [isSeries] so a favorited movie never surfaces on the series page. */
    @Query(
        "SELECT v.* FROM vod_titles v JOIN favorites f ON f.streamKey = COALESCE(v.externalId, v.name, v.streamUrl) " +
            "WHERE v.sourceId = :sourceId AND v.isSeries = :isSeries AND f.contentType = :contentType ORDER BY f.addedAtMs DESC, v.id",
    )
    fun pagingFavorites(sourceId: Long, isSeries: Boolean, contentType: ContentType): PagingSource<Int, VodTitle>

    /** Live count of favorited movies/series in the source -- the "Favorites" browse-row total (per type). */
    @Query(
        "SELECT COUNT(*) FROM vod_titles v JOIN favorites f ON f.streamKey = COALESCE(v.externalId, v.name, v.streamUrl) " +
            "WHERE v.sourceId = :sourceId AND v.isSeries = :isSeries AND f.contentType = :contentType",
    )
    fun observeFavoriteCount(sourceId: Long, isSeries: Boolean, contentType: ContentType): Flow<Int>

    /** Category column data (GROUP BY) -- no catalog rows loaded. */
    @Query("SELECT categoryName AS name, COUNT(*) AS count FROM vod_titles WHERE sourceId = :sourceId AND isSeries = :isSeries AND categoryName IS NOT NULL GROUP BY categoryName ORDER BY categoryName")
    fun observeCategoryCounts(sourceId: Long, isSeries: Boolean): Flow<List<CategoryCount>>

    @Query("SELECT COUNT(*) FROM vod_titles WHERE sourceId = :sourceId AND isSeries = :isSeries")
    fun observeCount(sourceId: Long, isSeries: Boolean): Flow<Int>

    /** Bounded rail/preview load (Home) -- never the whole catalog. */
    @Query("SELECT * FROM vod_titles WHERE sourceId = :sourceId AND isSeries = :isSeries ORDER BY name LIMIT :limit")
    fun observeTop(sourceId: Long, isSeries: Boolean, limit: Int): Flow<List<VodTitle>>

    /** Diverse bounded candidate pool for the curated Movies/Series rails -- see
     *  [ChannelDao.observeSample] for the id-hash-spread rationale. */
    @Query("SELECT * FROM vod_titles WHERE sourceId = :sourceId AND isSeries = :isSeries ORDER BY (id * 2654435761) % 2147483647 LIMIT :limit")
    fun observeSample(sourceId: Long, isSeries: Boolean, limit: Int): Flow<List<VodTitle>>

    /** Category weights from favorited movies+series -- VOD personalization signal. */
    @Query(
        "SELECT v.categoryName AS name, COUNT(*) AS count FROM vod_titles v " +
            "JOIN favorites f ON f.streamKey = COALESCE(v.externalId, v.name, v.streamUrl) " +
            "WHERE v.sourceId = :sourceId AND v.categoryName IS NOT NULL AND " +
            "((f.contentType = 'MOVIE' AND v.isSeries = 0) OR (f.contentType = 'SERIES' AND v.isSeries = 1)) " +
            "GROUP BY v.categoryName",
    )
    fun favoriteCategoryCounts(sourceId: Long): Flow<List<CategoryCount>>

    /** Category weights from continue-watching (the strongest "you watch this" signal), covering
     *  both resumed movies (by title) and resumed series (episode -> series title). */
    @Query(
        "SELECT categoryName AS name, COUNT(*) AS count FROM (" +
            "SELECT v.categoryName FROM continue_watching w JOIN vod_titles v ON v.id = w.vodTitleId " +
            "WHERE v.sourceId = :sourceId AND v.categoryName IS NOT NULL " +
            "UNION ALL " +
            "SELECT v.categoryName FROM continue_watching w JOIN series_episodes e ON e.id = w.seriesEpisodeId " +
            "JOIN vod_titles v ON v.id = e.seriesTitleId WHERE v.sourceId = :sourceId AND v.categoryName IS NOT NULL" +
            ") GROUP BY categoryName",
    )
    fun watchedCategoryCounts(sourceId: Long): Flow<List<CategoryCount>>

    /** DB-side search (Search screen) -- LIKE + LIMIT instead of in-memory filtering. */
    @Query("SELECT * FROM vod_titles WHERE sourceId = :sourceId AND isSeries = :isSeries AND name LIKE '%' || :query || '%' ORDER BY name LIMIT :limit")
    suspend fun search(sourceId: Long, isSeries: Boolean, query: String, limit: Int): List<VodTitle>

    @Query("SELECT * FROM vod_titles WHERE sourceId = :sourceId AND isSeries = :isSeries AND categoryName = :category ORDER BY name LIMIT :limit")
    suspend fun byCategory(sourceId: Long, isSeries: Boolean, category: String, limit: Int): List<VodTitle>

    /** Resolve favorited ids to rows (Favorites) without loading the catalog. */
    @Query("SELECT * FROM vod_titles WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<VodTitle>

    /** Resolve favorited VOD keys to the active source's current rows (see [Favorite.streamKey]). */
    @Query("SELECT * FROM vod_titles WHERE sourceId = :sourceId AND COALESCE(externalId, name, streamUrl) IN (:keys)")
    suspend fun titlesByFavoriteKeys(sourceId: Long, keys: List<String>): List<VodTitle>

    /** Content-id-driven lookup for Detail/Search -- mirrors [ChannelDao.getById]. */
    @Query("SELECT * FROM vod_titles WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): VodTitle?
}

@Dao
interface EPGProgramDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(programs: List<EPGProgram>)

    @Query("DELETE FROM epg_programs WHERE channelId IN (:channelIds)")
    suspend fun deleteForChannels(channelIds: List<Long>)

    /** Purge-a-source cleanup: drop EPG for every channel of a source. MUST run before the
     *  source's channels are deleted -- the subquery reads them. */
    @Query("DELETE FROM epg_programs WHERE channelId IN (SELECT id FROM channels WHERE sourceId = :sourceId)")
    suspend fun deleteForSource(sourceId: Long)

    @Query("SELECT * FROM epg_programs WHERE channelId IN (:channelIds) AND endMs >= :windowStartMs AND startMs <= :windowEndMs ORDER BY startMs")
    fun observeForChannelsInWindow(channelIds: List<Long>, windowStartMs: Long, windowEndMs: Long): Flow<List<EPGProgram>>

    /** Catch-up program-hop / title lookup: one channel's programmes overlapping the window, ordered
     *  by start. One-shot (not a Flow) -- read once when an archive programme loads to find its title
     *  and its immediate aired neighbours for the ⏮/⏭ hop. See docs/catchup-v1-design.md. */
    @Query("SELECT * FROM epg_programs WHERE channelId = :channelId AND endMs >= :windowStartMs AND startMs <= :windowEndMs ORDER BY startMs")
    suspend fun programsForChannelInWindow(channelId: Long, windowStartMs: Long, windowEndMs: Long): List<EPGProgram>

    /** Drop programmes that fall entirely outside the retained window -- [EpgRepository.refresh]
     *  only ever persists yesterday..tomorrow, so anything older is dead weight in a table that is
     *  already the app's largest. */
    @Query("DELETE FROM epg_programs WHERE endMs < :beforeMs")
    suspend fun deleteEndedBefore(beforeMs: Long)

    /**
     * Programme title search across one source's whole cached guide, newest-first from [fromMs].
     *
     * A `LIKE '%needle%'` cannot use the `(channelId, startMs)` index, so this is a scan of
     * `epg_programs` -- bounded by [limit] and by the fact that refresh only ever retains ~3 days.
     * The join is what lets a hit outside the currently-selected category still be navigable.
     */
    @Query(
        "SELECT p.channelId AS channelId, c.name AS channelName, c.logoUrl AS logoUrl, " +
            "c.categoryName AS categoryName, p.title AS title, p.startMs AS startMs, p.endMs AS endMs " +
            "FROM epg_programs p JOIN channels c ON c.id = p.channelId " +
            "WHERE c.sourceId = :sourceId AND p.endMs >= :fromMs AND p.title LIKE :pattern " +
            "ORDER BY p.startMs LIMIT :limit",
    )
    suspend fun searchByTitle(sourceId: Long, pattern: String, fromMs: Long, limit: Int): List<EpgSearchHit>
}

/**
 * Episodes for a series [VodTitle] (Xtream `get_series_info` only -- see
 * [com.arashrahimi46.iptv.data.repository.PlaylistRepository.ensureSeriesEpisodesLoaded]).
 * Populated lazily on first Detail view rather than at import time.
 */
@Dao
interface SeriesEpisodeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(episodes: List<SeriesEpisode>)

    /** Rollback cleanup: drop grouped episodes for a half-imported source's series parents. */
    @Query("DELETE FROM series_episodes WHERE seriesTitleId IN (:seriesTitleIds)")
    suspend fun deleteForSeries(seriesTitleIds: List<Long>)

    /** Re-import dedup cleanup: drop all episodes of a duplicate source's series (there's no FK
     * cascade). MUST run before the source's vod_titles rows are deleted -- the subquery reads them. */
    @Query("DELETE FROM series_episodes WHERE seriesTitleId IN (SELECT id FROM vod_titles WHERE sourceId = :sourceId)")
    suspend fun deleteForSource(sourceId: Long)

    @Query("SELECT * FROM series_episodes WHERE seriesTitleId = :seriesTitleId ORDER BY season, episode")
    fun observeForSeries(seriesTitleId: Long): Flow<List<SeriesEpisode>>

    @Query("SELECT COUNT(*) FROM series_episodes WHERE seriesTitleId = :seriesTitleId")
    suspend fun countForSeries(seriesTitleId: Long): Int

    // ids only (ordered S/E) so the player's prev/next-episode nav can compute neighbours without
    // materializing full episode rows -- mirrors ChannelDao.idsForSource for channel switching.
    @Query("SELECT id FROM series_episodes WHERE seriesTitleId = :seriesTitleId ORDER BY season, episode")
    suspend fun episodeIdsForSeries(seriesTitleId: Long): List<Long>

    @Query("SELECT * FROM series_episodes WHERE id = :episodeId LIMIT 1")
    suspend fun getById(episodeId: Long): SeriesEpisode?
}

/**
 * Real favorites persistence (Phase 4) -- see [com.arashrahimi46.iptv.data.repository.FavoritesRepository]
 * for the toggle/query surface consumed by the UI layer.
 */
@Dao
interface FavoriteDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(favorite: Favorite): Long

    @Query("DELETE FROM favorites WHERE contentType = :contentType AND streamKey = :streamKey")
    suspend fun deleteByKey(contentType: ContentType, streamKey: String)

    /**
     * Purge-a-source cleanup: drop every favorite whose stable key belongs to this source's catalog
     * -- LIVE favorites keyed to its channels, MOVIE/SERIES favorites to its VOD titles (contentType
     * is matched so a movie and a channel that share a streamKey don't cross-delete). MUST run before
     * the source's channels/vod_titles are deleted -- the subqueries read them.
     */
    @Query(
        "DELETE FROM favorites WHERE " +
            "(contentType = 'LIVE' AND streamKey IN (SELECT COALESCE(externalId, name, streamUrl) FROM channels WHERE sourceId = :sourceId)) OR " +
            "(contentType IN ('MOVIE', 'SERIES') AND streamKey IN (SELECT COALESCE(externalId, name, streamUrl) FROM vod_titles WHERE sourceId = :sourceId))",
    )
    suspend fun deleteForSource(sourceId: Long)

    @Query("SELECT COUNT(*) FROM favorites WHERE contentType = :contentType AND streamKey = :streamKey")
    suspend fun countByKey(contentType: ContentType, streamKey: String): Int

    @Query("SELECT * FROM favorites ORDER BY addedAtMs DESC")
    fun observeAll(): Flow<List<Favorite>>

    // The stable-key expressions below MUST stay in sync with FavoritesRepository.channelKey/vodKey
    // (Kotlin) -- both compute the same COALESCE(externalId, name, streamUrl) identity. name is
    // preferred over streamUrl so M3U favorites (externalId null, tokenized/rotating streamUrl)
    // survive re-imports (see FavoritesRepository.vodKey/channelKey for the tradeoff).

    /** Row ids of the active source's channels that are favorited, for tile favorite-icon state. */
    @Query(
        "SELECT c.id FROM channels c JOIN favorites f ON f.streamKey = COALESCE(c.externalId, c.name, c.streamUrl) " +
            "WHERE c.sourceId = :sourceId AND f.contentType = 'LIVE'",
    )
    fun observeFavoriteChannelIdsInSource(sourceId: Long): Flow<List<Long>>

    /**
     * Row ids of the active source's VOD titles (movies + series) that are favorited.
     * The join correlates the favorite's contentType with the row's isSeries flag
     * (MOVIE -> isSeries 0, SERIES -> isSeries 1) so favoriting movie "100" never lights
     * the heart on series "100" (their externalId spaces overlap).
     */
    @Query(
        "SELECT v.id FROM vod_titles v JOIN favorites f ON f.streamKey = COALESCE(v.externalId, v.name, v.streamUrl) " +
            "WHERE v.sourceId = :sourceId AND " +
            "((f.contentType = 'MOVIE' AND v.isSeries = 0) OR (f.contentType = 'SERIES' AND v.isSeries = 1))",
    )
    fun observeFavoriteVodIdsInSource(sourceId: Long): Flow<List<Long>>
}

/**
 * Continue-watching / on-deck persistence (P1.2). VOD/series only (live TV doesn't resume) --
 * see [com.arashrahimi46.iptv.data.repository.ContinueWatchingRepository] for the
 * find-existing-then-upsert dedup logic that keeps one row per title/episode.
 */
@Dao
interface ContinueWatchingDao {
    @Query("SELECT * FROM continue_watching WHERE vodTitleId = :vodTitleId LIMIT 1")
    suspend fun findByVod(vodTitleId: Long): ContinueWatchingEntry?

    @Query("SELECT * FROM continue_watching WHERE seriesEpisodeId = :episodeId LIMIT 1")
    suspend fun findByEpisode(episodeId: Long): ContinueWatchingEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: ContinueWatchingEntry): Long

    @Query("DELETE FROM continue_watching WHERE vodTitleId = :vodTitleId")
    suspend fun deleteByVod(vodTitleId: Long)

    @Query("DELETE FROM continue_watching WHERE seriesEpisodeId = :episodeId")
    suspend fun deleteByEpisode(episodeId: Long)

    /** Purge-a-source cleanup: drop resume points for the source's movies (by title) and series
     *  (episode -> series title). MUST run before the source's vod_titles/series_episodes are deleted. */
    @Query(
        "DELETE FROM continue_watching WHERE " +
            "vodTitleId IN (SELECT id FROM vod_titles WHERE sourceId = :sourceId) OR " +
            "seriesEpisodeId IN (SELECT id FROM series_episodes WHERE seriesTitleId IN (SELECT id FROM vod_titles WHERE sourceId = :sourceId))",
    )
    suspend fun deleteForSource(sourceId: Long)

    /** Recent in-progress entries, most-recent first -- bounded for the Home "Continue Watching" rail. */
    @Query("SELECT * FROM continue_watching ORDER BY updatedAtMs DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<ContinueWatchingEntry>>

    /** Retention cap: drop everything past the [keep] most-recent bookmarks so the list stays bounded. */
    @Query("DELETE FROM continue_watching WHERE id NOT IN (SELECT id FROM continue_watching ORDER BY updatedAtMs DESC LIMIT :keep)")
    suspend fun trimToMostRecent(keep: Int)

    /** Resume bookmark for a local recording (Live TV Recording V1). */
    @Query("SELECT * FROM continue_watching WHERE recordingId = :recordingId LIMIT 1")
    suspend fun findByRecording(recordingId: Long): ContinueWatchingEntry?

    /** Drop a recording's resume point (deleting the recording, or it finished playing). */
    @Query("DELETE FROM continue_watching WHERE recordingId = :recordingId")
    suspend fun deleteByRecording(recordingId: Long)

    /** Wipe every resume bookmark -- backs the Settings "Clear continue-watching / history" action. */
    @Query("DELETE FROM continue_watching")
    suspend fun deleteAll()
}

/**
 * Live TV recordings (Live TV Recording V1) -- see
 * [com.arashrahimi46.iptv.data.repository.RecordingRepository] for the observe/reconcile surface and
 * [com.arashrahimi46.iptv.data.recording.RecordingSupervisor] for the capture-time writes.
 */
@Dao
interface RecordingDao {
    @Insert
    suspend fun insert(recording: Recording): Long

    @Update
    suspend fun update(recording: Recording)

    @Query("SELECT * FROM recordings WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Recording?

    /** Recordings for one playlist source, newest first -- backs the (per-playlist) Recordings tab. */
    @Query("SELECT * FROM recordings WHERE sourceId = :sourceId ORDER BY startedAtMs DESC")
    fun observeBySource(sourceId: Long): Flow<List<Recording>>

    /** All recordings for a source -- read before a cascade delete so their files can be removed. */
    @Query("SELECT * FROM recordings WHERE sourceId = :sourceId")
    suspend fun getBySource(sourceId: Long): List<Recording>

    /** Drop all recording rows for a source (files handled separately by the repository). */
    @Query("DELETE FROM recordings WHERE sourceId = :sourceId")
    suspend fun deleteBySource(sourceId: Long)

    /** Live running totals for an in-progress recording (size/duration/bitrate/parts). */
    @Query("UPDATE recordings SET sizeBytes = :sizeBytes, durationMs = :durationMs, bitrateBps = :bitrateBps, parts = :parts WHERE id = :id")
    suspend fun updateProgress(id: Long, sizeBytes: Long, durationMs: Long?, bitrateBps: Long?, parts: Int)

    /** Finalize: terminal status + reason + final duration. */
    @Query("UPDATE recordings SET status = :status, statusReason = :reason, durationMs = :durationMs WHERE id = :id")
    suspend fun finalize(id: Long, status: RecordingStatus, reason: String?, durationMs: Long?)

    /** Launch-time crash reconciliation: any row left RECORDING (orphaned by crash/power-loss)
     * becomes INTERRUPTED. Idempotent -- a clean shutdown leaves no RECORDING rows to touch. */
    @Query("UPDATE recordings SET status = 'INTERRUPTED', statusReason = :reason WHERE status = 'RECORDING'")
    suspend fun reconcileOrphaned(reason: String)

    /** Rows still marked RECORDING at launch (need [reconcileOrphaned] + duration approximation). */
    @Query("SELECT * FROM recordings WHERE status = 'RECORDING'")
    suspend fun orphanedRecordings(): List<Recording>

    @Delete
    suspend fun delete(recording: Recording)
}

/**
 * "Open network stream" (VLC-style) direct URLs + their play history. Newest-played first.
 * Dedup is by [DirectStream.url]: the ViewModel looks the URL up with [findByUrl] and either
 * [touch]es the existing row's recency or [insert]s a new one, so replaying never duplicates.
 */
@Dao
interface DirectStreamDao {
    @Query("SELECT * FROM direct_streams ORDER BY lastPlayedAtMs DESC")
    fun observeAll(): Flow<List<DirectStream>>

    @Query("SELECT * FROM direct_streams WHERE id = :id")
    suspend fun getById(id: Long): DirectStream?

    @Query("SELECT * FROM direct_streams WHERE url = :url LIMIT 1")
    suspend fun findByUrl(url: String): DirectStream?

    @Insert
    suspend fun insert(stream: DirectStream): Long

    /** Bump a row to the top of the history (called when it (re)plays). */
    @Query("UPDATE direct_streams SET lastPlayedAtMs = :ts WHERE id = :id")
    suspend fun touch(id: Long, ts: Long)

    /** Rename a history box; pass null/blank to clear back to the URL-derived label. */
    @Query("UPDATE direct_streams SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String?)

    @Delete
    suspend fun delete(stream: DirectStream)
}
