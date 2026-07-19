package com.arashrahimi46.iptv.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.arashrahimi46.iptv.data.model.Category
import com.arashrahimi46.iptv.data.model.Channel
import com.arashrahimi46.iptv.data.model.ContinueWatchingEntry
import com.arashrahimi46.iptv.data.model.EPGProgram
import com.arashrahimi46.iptv.data.model.Favorite
import com.arashrahimi46.iptv.data.model.PlaylistSource
import com.arashrahimi46.iptv.data.model.SeriesEpisode
import com.arashrahimi46.iptv.data.model.VodTitle

/**
 * Local Room database. [ContinueWatchingEntry] remains a schema stub
 * registered now so a later phase populating it doesn't need a breaking
 * migration -- it has no DAO yet. [EPGProgram] is populated starting Phase 2
 * (Guide/EPG). [SeriesEpisode] is populated starting Phase 3 (Detail) via
 * [seriesEpisodeDao], Xtream sources only -- see
 * [com.arashrahimi46.iptv.data.repository.PlaylistRepository.ensureSeriesEpisodesLoaded].
 * [Favorite] is populated starting Phase 4 via [favoriteDao] -- see
 * [com.arashrahimi46.iptv.data.repository.FavoritesRepository].
 */
@Database(
    entities = [
        PlaylistSource::class,
        Category::class,
        Channel::class,
        VodTitle::class,
        SeriesEpisode::class,
        EPGProgram::class,
        Favorite::class,
        ContinueWatchingEntry::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playlistSourceDao(): PlaylistSourceDao
    abstract fun categoryDao(): CategoryDao
    abstract fun channelDao(): ChannelDao
    abstract fun vodTitleDao(): VodTitleDao
    abstract fun epgProgramDao(): EPGProgramDao
    abstract fun seriesEpisodeDao(): SeriesEpisodeDao
    abstract fun favoriteDao(): FavoriteDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "are_iptv.db",
                ).build().also { instance = it }
            }
    }
}
