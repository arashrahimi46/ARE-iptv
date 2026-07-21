package com.arashrahimi46.iptv.data.db

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.arashrahimi46.iptv.data.model.Category
import com.arashrahimi46.iptv.data.model.Channel
import com.arashrahimi46.iptv.data.model.ContentType
import com.arashrahimi46.iptv.data.model.ContinueWatchingEntry
import com.arashrahimi46.iptv.data.model.EPGProgram
import com.arashrahimi46.iptv.data.model.Favorite
import com.arashrahimi46.iptv.data.model.PlaylistSource
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

@Dao
interface PlaylistSourceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(source: PlaylistSource): Long

    @Query("SELECT * FROM playlist_sources ORDER BY createdAtMs DESC")
    fun observeAll(): Flow<List<PlaylistSource>>

    @Query("SELECT * FROM playlist_sources WHERE id = :id")
    suspend fun getById(id: Long): PlaylistSource?

    @Query("SELECT COUNT(*) FROM playlist_sources")
    suspend fun count(): Int

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

    @Query("SELECT * FROM channels WHERE sourceId = :sourceId ORDER BY name")
    fun observeForSource(sourceId: Long): Flow<List<Channel>>

    @Query("SELECT * FROM channels WHERE id = :channelId LIMIT 1")
    suspend fun getById(channelId: Long): Channel?

    @Query("SELECT * FROM channels WHERE sourceId = :sourceId ORDER BY name LIMIT :limit")
    fun observeForSourceLimited(sourceId: Long, limit: Int): Flow<List<Channel>>

    @Query("SELECT COUNT(*) FROM channels WHERE sourceId = :sourceId")
    suspend fun countForSource(sourceId: Long): Int

    // --- Large-catalog browse (Paging 3): only the visible window is ever in memory. ---

    @Query("SELECT * FROM channels WHERE sourceId = :sourceId ORDER BY name")
    fun pagingAll(sourceId: Long): PagingSource<Int, Channel>

    @Query("SELECT * FROM channels WHERE sourceId = :sourceId AND categoryName = :category ORDER BY name")
    fun pagingByCategory(sourceId: Long, category: String): PagingSource<Int, Channel>

    /** Category column data (GROUP BY) -- no catalog rows loaded. */
    @Query("SELECT categoryName AS name, COUNT(*) AS count FROM channels WHERE sourceId = :sourceId AND categoryName IS NOT NULL GROUP BY categoryName ORDER BY categoryName")
    fun observeCategoryCounts(sourceId: Long): Flow<List<CategoryCount>>

    @Query("SELECT COUNT(*) FROM channels WHERE sourceId = :sourceId")
    fun observeCountForSource(sourceId: Long): Flow<Int>

    /** Bounded rail/preview load (Home) -- never the whole catalog. */
    @Query("SELECT * FROM channels WHERE sourceId = :sourceId ORDER BY name LIMIT :limit")
    fun observeTop(sourceId: Long, limit: Int): Flow<List<Channel>>

    /** DB-side search (Search screen) -- LIKE + LIMIT instead of in-memory filtering. */
    @Query("SELECT * FROM channels WHERE sourceId = :sourceId AND name LIKE '%' || :query || '%' ORDER BY name LIMIT :limit")
    suspend fun search(sourceId: Long, query: String, limit: Int): List<Channel>

    @Query("SELECT * FROM channels WHERE sourceId = :sourceId AND categoryName = :category ORDER BY name LIMIT :limit")
    suspend fun byCategory(sourceId: Long, category: String, limit: Int): List<Channel>

    /** Resolve favorited ids to rows (Favorites) without loading the catalog. */
    @Query("SELECT * FROM channels WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<Channel>

    /** Ordered channel ids only (player prev/next nav) -- ids, not full rows, for large catalogs. */
    @Query("SELECT id FROM channels WHERE sourceId = :sourceId ORDER BY name")
    suspend fun idsForSource(sourceId: Long): List<Long>

    /** Stream-health fallback (P0.1): another catalog entry for the same logical channel
     * (same name, different row -- e.g. a duplicate listing from another category/source)
     * to switch to once retry/backoff on [excludeId] is exhausted. */
    @Query("SELECT * FROM channels WHERE name = :name AND id != :excludeId ORDER BY id LIMIT 1")
    suspend fun findAlternateByName(name: String, excludeId: Long): Channel?
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

    /** Recompute [VodTitle.episodeCount] from the grouped `series_episodes` rows for a source's
     * series (one correlated UPDATE, index-backed on seriesTitleId) -- run once after M3U import. */
    @Query("UPDATE vod_titles SET episodeCount = (SELECT COUNT(*) FROM series_episodes WHERE series_episodes.seriesTitleId = vod_titles.id) WHERE sourceId = :sourceId AND isSeries = 1")
    suspend fun refreshEpisodeCounts(sourceId: Long)

    /** Set one series' episode count -- Xtream loads episodes lazily on Detail view, not at import. */
    @Query("UPDATE vod_titles SET episodeCount = :count WHERE id = :id")
    suspend fun setEpisodeCount(id: Long, count: Int)

    // --- Large-catalog browse (Paging 3): only the visible window is ever in memory. ---

    @Query("SELECT * FROM vod_titles WHERE sourceId = :sourceId AND isSeries = :isSeries ORDER BY name")
    fun paging(sourceId: Long, isSeries: Boolean): PagingSource<Int, VodTitle>

    @Query("SELECT * FROM vod_titles WHERE sourceId = :sourceId AND isSeries = :isSeries AND categoryName = :category ORDER BY name")
    fun pagingByCategory(sourceId: Long, isSeries: Boolean, category: String): PagingSource<Int, VodTitle>

    /** Category column data (GROUP BY) -- no catalog rows loaded. */
    @Query("SELECT categoryName AS name, COUNT(*) AS count FROM vod_titles WHERE sourceId = :sourceId AND isSeries = :isSeries AND categoryName IS NOT NULL GROUP BY categoryName ORDER BY categoryName")
    fun observeCategoryCounts(sourceId: Long, isSeries: Boolean): Flow<List<CategoryCount>>

    @Query("SELECT COUNT(*) FROM vod_titles WHERE sourceId = :sourceId AND isSeries = :isSeries")
    fun observeCount(sourceId: Long, isSeries: Boolean): Flow<Int>

    /** Bounded rail/preview load (Home) -- never the whole catalog. */
    @Query("SELECT * FROM vod_titles WHERE sourceId = :sourceId AND isSeries = :isSeries ORDER BY name LIMIT :limit")
    fun observeTop(sourceId: Long, isSeries: Boolean, limit: Int): Flow<List<VodTitle>>

    /** DB-side search (Search screen) -- LIKE + LIMIT instead of in-memory filtering. */
    @Query("SELECT * FROM vod_titles WHERE sourceId = :sourceId AND isSeries = :isSeries AND name LIKE '%' || :query || '%' ORDER BY name LIMIT :limit")
    suspend fun search(sourceId: Long, isSeries: Boolean, query: String, limit: Int): List<VodTitle>

    @Query("SELECT * FROM vod_titles WHERE sourceId = :sourceId AND isSeries = :isSeries AND categoryName = :category ORDER BY name LIMIT :limit")
    suspend fun byCategory(sourceId: Long, isSeries: Boolean, category: String, limit: Int): List<VodTitle>

    /** Resolve favorited ids to rows (Favorites) without loading the catalog. */
    @Query("SELECT * FROM vod_titles WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<VodTitle>

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

    @Query("SELECT * FROM epg_programs WHERE channelId IN (:channelIds) AND endMs >= :windowStartMs AND startMs <= :windowEndMs ORDER BY startMs")
    fun observeForChannelsInWindow(channelIds: List<Long>, windowStartMs: Long, windowEndMs: Long): Flow<List<EPGProgram>>
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

    @Query("SELECT * FROM series_episodes WHERE seriesTitleId = :seriesTitleId ORDER BY season, episode")
    fun observeForSeries(seriesTitleId: Long): Flow<List<SeriesEpisode>>

    @Query("SELECT COUNT(*) FROM series_episodes WHERE seriesTitleId = :seriesTitleId")
    suspend fun countForSeries(seriesTitleId: Long): Int

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

    @Query("DELETE FROM favorites WHERE channelId = :channelId")
    suspend fun deleteByChannel(channelId: Long)

    @Query("DELETE FROM favorites WHERE vodTitleId = :vodTitleId")
    suspend fun deleteByVod(vodTitleId: Long)

    @Query("SELECT COUNT(*) FROM favorites WHERE channelId = :channelId")
    suspend fun countByChannel(channelId: Long): Int

    @Query("SELECT COUNT(*) FROM favorites WHERE vodTitleId = :vodTitleId")
    suspend fun countByVod(vodTitleId: Long): Int

    @Query("SELECT * FROM favorites ORDER BY addedAtMs DESC")
    fun observeAll(): Flow<List<Favorite>>

    @Query("SELECT channelId FROM favorites WHERE channelId IS NOT NULL")
    fun observeFavoriteChannelIds(): Flow<List<Long>>

    @Query("SELECT vodTitleId FROM favorites WHERE vodTitleId IS NOT NULL")
    fun observeFavoriteVodIds(): Flow<List<Long>>
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

    /** Recent in-progress entries, most-recent first -- bounded for the Home "Continue Watching" rail. */
    @Query("SELECT * FROM continue_watching ORDER BY updatedAtMs DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<ContinueWatchingEntry>>
}
