package com.arashrahimi46.iptv.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.arashrahimi46.iptv.data.model.Category
import com.arashrahimi46.iptv.data.model.Channel
import com.arashrahimi46.iptv.data.model.ContentType
import com.arashrahimi46.iptv.data.model.EPGProgram
import com.arashrahimi46.iptv.data.model.Favorite
import com.arashrahimi46.iptv.data.model.PlaylistSource
import com.arashrahimi46.iptv.data.model.SeriesEpisode
import com.arashrahimi46.iptv.data.model.VodTitle
import kotlinx.coroutines.flow.Flow

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
}

@Dao
interface VodTitleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(titles: List<VodTitle>)

    @Query("DELETE FROM vod_titles WHERE sourceId = :sourceId")
    suspend fun deleteForSource(sourceId: Long)

    @Query("SELECT * FROM vod_titles WHERE sourceId = :sourceId AND isSeries = 0 ORDER BY name LIMIT :limit")
    fun observeMoviesForSource(sourceId: Long, limit: Int = 200): Flow<List<VodTitle>>

    @Query("SELECT * FROM vod_titles WHERE sourceId = :sourceId AND isSeries = 1 ORDER BY name LIMIT :limit")
    fun observeSeriesForSource(sourceId: Long, limit: Int = 200): Flow<List<VodTitle>>

    @Query("SELECT COUNT(*) FROM vod_titles WHERE sourceId = :sourceId AND isSeries = 0")
    suspend fun countMoviesForSource(sourceId: Long): Int

    @Query("SELECT COUNT(*) FROM vod_titles WHERE sourceId = :sourceId AND isSeries = 1")
    suspend fun countSeriesForSource(sourceId: Long): Int

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
