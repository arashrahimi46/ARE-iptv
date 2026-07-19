package com.arashrahimi46.iptv.data.repository

import android.content.Context
import com.arashrahimi46.iptv.data.db.AppDatabase
import com.arashrahimi46.iptv.data.model.ContentType
import com.arashrahimi46.iptv.data.model.Favorite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Wraps [com.arashrahimi46.iptv.data.db.FavoriteDao] behind a small surface
 * shared by every screen that shows a favorite-toggle affordance (Live,
 * Movies, Series, Search, Detail, and the real Favorites screen itself) so
 * the toggle/insert/delete logic lives in one place instead of being
 * duplicated per-ViewModel. All writes run on [Dispatchers.IO], matching
 * [PlaylistRepositoryImpl]'s pattern.
 */
class FavoritesRepository(context: Context) {
    private val dao = AppDatabase.get(context).favoriteDao()

    /** Real-time set of favorited channel ids -- cheap membership checks for tile favorite icons. */
    val favoriteChannelIds: Flow<Set<Long>> = dao.observeFavoriteChannelIds().map { it.toSet() }

    /** Real-time set of favorited VOD title ids (movies + series). */
    val favoriteVodIds: Flow<Set<Long>> = dao.observeFavoriteVodIds().map { it.toSet() }

    fun observeAll(): Flow<List<Favorite>> = dao.observeAll()

    suspend fun toggleChannel(channelId: Long): Unit = withContext(Dispatchers.IO) {
        if (dao.countByChannel(channelId) > 0) {
            dao.deleteByChannel(channelId)
        } else {
            dao.insert(Favorite(contentType = ContentType.LIVE, channelId = channelId))
        }
    }

    /** [contentType] should be MOVIE or SERIES, matching the [com.arashrahimi46.iptv.data.model.VodTitle.isSeries] flag. */
    suspend fun toggleVod(vodTitleId: Long, contentType: ContentType): Unit = withContext(Dispatchers.IO) {
        if (dao.countByVod(vodTitleId) > 0) {
            dao.deleteByVod(vodTitleId)
        } else {
            dao.insert(Favorite(contentType = contentType, vodTitleId = vodTitleId))
        }
    }
}
