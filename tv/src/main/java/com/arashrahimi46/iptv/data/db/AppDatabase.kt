package com.arashrahimi46.iptv.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
 *
 * Bumped to version 2: [PlaylistSource] dropped its plaintext username/password
 * columns (credentials moved to encrypted-at-rest storage, see
 * [com.arashrahimi46.iptv.data.settings.CredentialsStore]). No production installs
 * exist yet (app hasn't shipped), so a destructive fallback is used instead of a
 * real Migration -- acceptable pre-release; would need a real migration path once
 * this ships and real user data is at stake.
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
    version = 3,
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

        /**
         * v2 -> v3: add the browse/paging covering indices on `channels` and `vod_titles`
         * (see [com.arashrahimi46.iptv.data.model.Channel]/[com.arashrahimi46.iptv.data.model.VodTitle]).
         * A real (non-destructive) migration so an already-imported large catalog -- which can
         * take a long time to re-fetch -- is preserved rather than wiped. Index names must match
         * Room's generated `index_<table>_<cols>` names exactly or schema validation fails.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_channels_sourceId_name` ON `channels` (`sourceId`, `name`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_channels_sourceId_categoryName_name` ON `channels` (`sourceId`, `categoryName`, `name`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_vod_titles_sourceId_isSeries_name` ON `vod_titles` (`sourceId`, `isSeries`, `name`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_vod_titles_sourceId_isSeries_categoryName_name` ON `vod_titles` (`sourceId`, `isSeries`, `categoryName`, `name`)")
            }
        }

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "are_iptv.db",
                ).addMigrations(MIGRATION_2_3)
                    .fallbackToDestructiveMigration(true)
                    .build().also { instance = it }
            }
    }
}
