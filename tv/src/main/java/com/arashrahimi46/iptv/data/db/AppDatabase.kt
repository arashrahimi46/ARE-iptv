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
import com.arashrahimi46.iptv.data.settings.CredentialsStore

/**
 * Local Room database. [ContinueWatchingEntry] remains a schema stub
 * registered now so a later phase populating it doesn't need a breaking
 * migration -- it has no DAO yet. [EPGProgram] is populated starting Phase 2
 * (Guide/EPG). [SeriesEpisode] is populated starting Phase 3 (Detail) via
 * [seriesEpisodeDao], Xtream sources only -- see
 * [com.arashrahimi46.iptv.data.repository.PlaylistRepository.ensureSeriesEpisodesLoaded].
 * [Favorite] is populated starting Phase 4 via [favoriteDao] -- see
 * [com.arashrahimi46.iptv.data.repository.FavoritesRepository]. [ContinueWatchingEntry] is
 * populated starting P1.2 via [continueWatchingDao] -- see
 * [com.arashrahimi46.iptv.data.repository.ContinueWatchingRepository]. Adding its DAO is not a
 * schema change (the table/columns were already registered), so no migration bump was needed.
 *
 * Bumped to version 2: [PlaylistSource] dropped its plaintext username/password
 * columns (credentials moved to encrypted-at-rest storage, see
 * [com.arashrahimi46.iptv.data.settings.CredentialsStore]).
 *
 * Every version step (1->2->3->4) now has a real, non-destructive [Migration] --
 * see [migration1To2] below. There is deliberately no
 * `fallbackToDestructiveMigration` anymore: that fallback would silently wipe an
 * upgrading user's whole catalog/favorites/continue-watching on any version bump
 * it didn't have an exact migration for, which is a real data-loss bug once real
 * installs exist, not just a "safe pre-release shortcut". If a future version
 * bump is ever added without its own Migration, Room now fails loudly
 * (`IllegalStateException`) instead of wiping data -- that failure is the
 * intended forcing function to add the missing migration before release.
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
    version = 6,
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
    abstract fun continueWatchingDao(): ContinueWatchingDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        /**
         * v1 -> v2: [PlaylistSource] drops its plaintext `username`/`password` columns. Each
         * row's credentials are copied into encrypted-at-rest [CredentialsStore] BEFORE the
         * columns are dropped, so an upgrading user's saved Xtream logins survive instead of
         * silently vanishing. SQLite's `ALTER TABLE ... DROP COLUMN` isn't available on every
         * Android-bundled SQLite version, so the table is recreated without those two columns
         * (the standard SQLite "copy to a new table" pattern) rather than altered in place --
         * every other column/row, and every other table, is untouched.
         */
        fun migration1To2(context: Context): Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val credentials = CredentialsStore(context)
                db.query("SELECT id, username, password FROM playlist_sources").use { cursor ->
                    val idIndex = cursor.getColumnIndex("id")
                    val usernameIndex = cursor.getColumnIndex("username")
                    val passwordIndex = cursor.getColumnIndex("password")
                    while (cursor.moveToNext()) {
                        val username = usernameIndex.takeIf { it >= 0 && !cursor.isNull(it) }?.let(cursor::getString)
                        val password = passwordIndex.takeIf { it >= 0 && !cursor.isNull(it) }?.let(cursor::getString)
                        if (username != null && password != null) {
                            credentials.save(cursor.getLong(idIndex), username, password)
                        }
                    }
                }

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `playlist_sources_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, `type` TEXT NOT NULL, `url` TEXT NOT NULL, `epgUrl` TEXT, `createdAtMs` INTEGER NOT NULL)",
                )
                db.execSQL(
                    "INSERT INTO `playlist_sources_new` (id, name, type, url, epgUrl, createdAtMs) " +
                        "SELECT id, name, type, url, epgUrl, createdAtMs FROM `playlist_sources`",
                )
                db.execSQL("DROP TABLE `playlist_sources`")
                db.execSQL("ALTER TABLE `playlist_sources_new` RENAME TO `playlist_sources`")
            }
        }

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

        /**
         * v3 -> v4: series-episode grouping. Adds `vod_titles.episodeCount` (grouped-episode count
         * shown on the series tile/detail) and an index on `series_episodes.seriesTitleId` (every
         * episode read filters on it, and the count-refresh UPDATE joins on it). Non-destructive so
         * an existing large catalog is preserved -- though M3U series only regroup on re-import.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `vod_titles` ADD COLUMN `episodeCount` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_series_episodes_seriesTitleId` ON `series_episodes` (`seriesTitleId`)")
            }
        }

        /**
         * v4 -> v5: [Favorite] is re-keyed from autogenerated row ids (`channelId`/`vodTitleId`) to
         * each item's stable identity (`streamKey` = `COALESCE(externalId, streamUrl[, name])`), so a
         * favorite survives re-imports instead of dangling once the catalog is re-inserted with fresh
         * row ids -- see [Favorite]. Existing favorites are preserved by resolving their current row id
         * to that stable key here; a favorite whose referenced row is already gone (i.e. was already
         * dangling) is dropped by the JOIN, which is the intended "hide silently" behaviour. SQLite has
         * no in-place column reshape, so the standard copy-to-a-new-table pattern is used.
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `favorites_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`contentType` TEXT NOT NULL, `streamKey` TEXT NOT NULL, `addedAtMs` INTEGER NOT NULL)",
                )
                db.execSQL(
                    "INSERT INTO `favorites_new` (id, contentType, streamKey, addedAtMs) " +
                        "SELECT f.id, f.contentType, COALESCE(c.externalId, c.streamUrl), f.addedAtMs " +
                        "FROM `favorites` f JOIN `channels` c ON c.id = f.channelId " +
                        "WHERE f.channelId IS NOT NULL AND COALESCE(c.externalId, c.streamUrl) IS NOT NULL",
                )
                db.execSQL(
                    "INSERT INTO `favorites_new` (id, contentType, streamKey, addedAtMs) " +
                        "SELECT f.id, f.contentType, COALESCE(v.externalId, v.streamUrl, v.name), f.addedAtMs " +
                        "FROM `favorites` f JOIN `vod_titles` v ON v.id = f.vodTitleId " +
                        "WHERE f.vodTitleId IS NOT NULL AND COALESCE(v.externalId, v.streamUrl, v.name) IS NOT NULL",
                )
                db.execSQL("DROP TABLE `favorites`")
                db.execSQL("ALTER TABLE `favorites_new` RENAME TO `favorites`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_favorites_streamKey` ON `favorites` (`streamKey`)")
            }
        }

        /**
         * v5 -> v6: rich VOD metadata for the Detail screen. Adds nullable `plot`/`castList`/
         * `director`/`genre`/`imdbRating`/`rtRating` and a `metadataFetched` flag to `vod_titles`,
         * populated lazily on first Detail view (Xtream info block + user's OMDb key). Non-destructive
         * plain ADD COLUMNs -- an existing catalog is preserved and simply enriches as titles are opened.
         * (`cast` is a SQLite keyword, hence `castList`.)
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `vod_titles` ADD COLUMN `plot` TEXT")
                db.execSQL("ALTER TABLE `vod_titles` ADD COLUMN `castList` TEXT")
                db.execSQL("ALTER TABLE `vod_titles` ADD COLUMN `director` TEXT")
                db.execSQL("ALTER TABLE `vod_titles` ADD COLUMN `genre` TEXT")
                db.execSQL("ALTER TABLE `vod_titles` ADD COLUMN `imdbRating` TEXT")
                db.execSQL("ALTER TABLE `vod_titles` ADD COLUMN `rtRating` TEXT")
                db.execSQL("ALTER TABLE `vod_titles` ADD COLUMN `metadataFetched` INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "are_iptv.db",
                ).addMigrations(migration1To2(context.applicationContext), MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .build().also { instance = it }
            }
    }
}
