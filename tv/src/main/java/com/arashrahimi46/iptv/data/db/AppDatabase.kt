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
 * Local Room database. [EPGProgram], [Favorite] and [ContinueWatchingEntry]
 * are schema stubs registered now so a later phase populating them doesn't
 * need a breaking migration -- they have no DAOs yet.
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
