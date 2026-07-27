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
import com.arashrahimi46.iptv.data.model.DirectStream
import com.arashrahimi46.iptv.data.model.EPGProgram
import com.arashrahimi46.iptv.data.model.Favorite
import com.arashrahimi46.iptv.data.model.PlaylistSource
import com.arashrahimi46.iptv.data.model.Recording
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
        Recording::class,
        DirectStream::class,
    ],
    version = 14,
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
    abstract fun recordingDao(): RecordingDao
    abstract fun directStreamDao(): DirectStreamDao

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
                            credentials.saveSync(cursor.getLong(idIndex), username, password)
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

        /**
         * v6 -> v7: manual catalog refresh (id-preserving upsert sync). Adds nullable
         * `playlist_sources.lastRefreshedAtMs` -- the epoch-ms of the last successful sync, set on
         * initial import and every manual refresh, read by the Settings "refresh overdue" badge and
         * "Last updated" label. Plain non-destructive ADD COLUMN; existing sources keep their whole
         * catalog and simply read as "never refreshed" (null) until the first refresh.
         */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `playlist_sources` ADD COLUMN `lastRefreshedAtMs` INTEGER")
            }
        }

        /**
         * v7 -> v8: Provider account panel. Adds nullable `playlist_sources.accountInfoJson` -- the
         * JSON blob of Xtream account/server metadata (status, expiry, connection limits, timezone…)
         * captured from the auth response, shown in Settings. Plain non-destructive ADD COLUMN;
         * existing sources keep everything and read as "no details" until their next refresh.
         */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `playlist_sources` ADD COLUMN `accountInfoJson` TEXT")
            }
        }

        /**
         * v8 -> v9: Live TV Recording V1. Creates the `recordings` table and adds nullable
         * `continue_watching.recordingId` so a half-watched recording resumes through the existing
         * player path. Both are additive/non-destructive -- an existing catalog, favorites and
         * continue-watching are untouched. The `recordings` CREATE mirrors Room's generated schema
         * exactly (column order + types + no SQL defaults, since [Recording]'s defaults are Kotlin-side)
         * or schema validation fails on first open.
         */
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `continue_watching` ADD COLUMN `recordingId` INTEGER")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `recordings` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`channelId` INTEGER NOT NULL, " +
                        "`channelName` TEXT NOT NULL, " +
                        "`programTitle` TEXT, " +
                        "`startedAtMs` INTEGER NOT NULL, " +
                        "`durationMs` INTEGER, " +
                        "`sizeBytes` INTEGER NOT NULL, " +
                        "`bitrateBps` INTEGER, " +
                        "`status` TEXT NOT NULL, " +
                        "`statusReason` TEXT, " +
                        "`storageTreeUri` TEXT NOT NULL, " +
                        "`documentId` TEXT NOT NULL, " +
                        "`parts` INTEGER NOT NULL, " +
                        "`volumeUuid` TEXT, " +
                        "`locked` INTEGER NOT NULL)",
                )
            }
        }

        /**
         * v9 -> v10: "Open network stream" (VLC-style direct URLs). Creates the `direct_streams`
         * table -- the play history behind the Streams tab. Purely additive/non-destructive; the
         * CREATE mirrors Room's generated schema exactly (column order + types, no SQL defaults --
         * [DirectStream]'s defaults are Kotlin-side) or schema validation fails on first open.
         */
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `direct_streams` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`url` TEXT NOT NULL, " +
                        "`name` TEXT, " +
                        "`createdAtMs` INTEGER NOT NULL, " +
                        "`lastPlayedAtMs` INTEGER NOT NULL)",
                )
            }
        }

        /** v10 -> v11: recordings become per-playlist. Add a nullable `sourceId` FK column to
         * `recordings`, then backfill each existing row from the channel it captured (so old
         * recordings keep showing under their playlist). A row whose channel is already gone stays
         * NULL (unscoped) rather than being wiped. */
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `recordings` ADD COLUMN `sourceId` INTEGER")
                db.execSQL(
                    "UPDATE `recordings` SET `sourceId` = " +
                        "(SELECT `sourceId` FROM `channels` WHERE `channels`.`id` = `recordings`.`channelId`)",
                )
            }
        }

        /**
         * v11 -> v12: catch-up / archive (docs/catchup-v1-design.md). Adds three columns to `channels`:
         * `catchupDays` (0 = no archive; >0 = window length, which drives both capability and the guide
         * catch-up glyph) plus the M3U-only `catchupSource`/`catchupType` template fields. Plain additive
         * ADD COLUMNs -- an existing catalog is preserved and simply reads as "no archive" (catchupDays 0)
         * until the next refresh repopulates the columns from the provider.
         */
        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `channels` ADD COLUMN `catchupDays` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `channels` ADD COLUMN `catchupSource` TEXT")
                db.execSQL("ALTER TABLE `channels` ADD COLUMN `catchupType` TEXT")
            }
        }

        /**
         * v12 -> v13: Explore (docs/playlist-explore-legal-v1.html). Adds nullable
         * `playlist_sources.origin` -- the id of the curated Explore entry a playlist was added from,
         * or NULL for one the user entered by hand. Drives the "From Explore" badge and records
         * provenance across renames. Existing rows read as NULL, i.e. hand-added, which is correct.
         */
        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `playlist_sources` ADD COLUMN `origin` TEXT")
            }
        }

        /**
         * v13 -> v14: index `epg_programs(channelId, startMs)`. Pure index creation -- no column or
         * data change, so it cannot fail on existing rows and needs no backfill. See the note on
         * [com.arashrahimi46.iptv.data.model.EPGProgram] for why the table was the app's worst query.
         *
         * `IF NOT EXISTS` because a user who lands here via a destructive fallback or a fresh install
         * already has the index from Room's generated schema; creating it twice would throw.
         */
        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_epg_programs_channelId_startMs` " +
                        "ON `epg_programs` (`channelId`, `startMs`)",
                )
            }
        }

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "are_iptv.db",
                ).addMigrations(migration1To2(context.applicationContext), MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14)
                    .build().also { instance = it }
            }
    }
}
