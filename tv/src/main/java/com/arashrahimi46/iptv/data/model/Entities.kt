package com.arashrahimi46.iptv.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Content type a [Category] or catalog item belongs to. */
enum class ContentType { LIVE, MOVIE, SERIES }

/** A configured playlist source (Xtream Codes portal, M3U playlist/file, or Stalker/Ministra portal). */
enum class SourceType { XTREAM, M3U, STALKER }

/**
 * A user-added playlist/portal. Xtream username/password are NOT stored on this entity --
 * they live encrypted-at-rest in [com.arashrahimi46.iptv.data.settings.CredentialsStore],
 * keyed by [id], hardened out of the plaintext-Room-column v1 state (tracked since Phase 1,
 * addressed in a dedicated Phase 4 pass). Never written to logs.
 */
@Entity(tableName = "playlist_sources")
data class PlaylistSource(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: SourceType,
    /** Xtream: base portal URL (scheme+host[:port]). M3U: the playlist URL (or a local file URI). */
    val url: String,
    /** Optional custom XMLTV EPG URL; null when using Xtream auto-derived EPG. */
    val epgUrl: String? = null,
    val createdAtMs: Long = System.currentTimeMillis(),
    /** Epoch-ms of the last successful catalog sync (initial import or a manual refresh). Null for
     * rows imported before this column existed (they read as "never refreshed" -> stale badge until
     * the first refresh). Drives the Settings "refresh overdue" badge and the "Last updated" label. */
    val lastRefreshedAtMs: Long? = null,
    /** Id of the curated Explore entry this was added from; null when the user entered it by hand.
     *  Survives renames (the name is the user's, this is provenance) and drives the picker badge. */
    val origin: String? = null,
    /** Xtream only: JSON blob of the provider account/server metadata (status, expiry, connection
     * limits, timezone…) captured from the auth response on import + each refresh, shown in the
     * Settings "Provider" panel. Null for M3U sources and for rows imported before this existed
     * (they read as "no details" until the next refresh). See
     * [com.arashrahimi46.iptv.data.parser.XtreamAccountInfo]. */
    val accountInfoJson: String? = null,
)

/** A genre/group, tagged by which catalog it filters (mirrors CategoryRow/CategoryCard `kind`). */
@Entity(tableName = "categories", primaryKeys = ["sourceId", "contentType", "name"])
data class Category(
    val sourceId: Long,
    val contentType: ContentType,
    val name: String,
    /** Xtream category_id, used to cross-reference get_*_streams responses; null for M3U (grouped by group-title text only). */
    val externalId: String? = null,
)

/**
 * A live TV channel.
 *
 * Indexed for large catalogs (100k+ channels): browse queries filter by [sourceId]
 * and order/group by [name]/[categoryName], so these covering indices turn full-table
 * scans + in-memory sorts into index range scans.
 */
@Entity(
    tableName = "channels",
    indices = [
        Index(value = ["sourceId", "name"]),
        Index(value = ["sourceId", "categoryName", "name"]),
    ],
)
data class Channel(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceId: Long,
    val name: String,
    val streamUrl: String,
    val logoUrl: String? = null,
    val categoryName: String? = null,
    val tvgId: String? = null,
    val number: String? = null,
    /** Xtream stream_id, used for EPG cross-reference; null for M3U. */
    val externalId: String? = null,
    /** Catch-up / archive window in days: 0 = the provider offers no archive on this channel; >0 both
     * marks the channel archive-capable AND bounds how far back the guide's catch-up glyph may appear.
     * Xtream: `tv_archive_duration`. Stalker: portal archive window. M3U: `catchup-days`.
     * See docs/catchup-v1-design.md. */
    val catchupDays: Int = 0,
    /** M3U only: the `catchup-source` URL template ({utc}/{start}/… placeholders) expanded at play-time
     * to mint an archive URL; null for Xtream/Stalker (they compute archive URLs without a template). */
    val catchupSource: String? = null,
    /** M3U only: the `catchup-type` convention (default/shift/append/flussonic/xc) that selects how
     * [catchupSource] is expanded; null otherwise. */
    val catchupType: String? = null,
)

/**
 * A movie or series (series = a [VodTitle] with [isSeries] true; episodes in [SeriesEpisode]).
 *
 * Indexed for large catalogs (300k+ titles): browse/paging filters by [sourceId] + [isSeries]
 * and orders/groups by [name]/[categoryName]. Without these, ORDER BY name and GROUP BY
 * categoryName are full-table scans on the whole catalog.
 */
@Entity(
    tableName = "vod_titles",
    indices = [
        Index(value = ["sourceId", "isSeries", "name"]),
        Index(value = ["sourceId", "isSeries", "categoryName", "name"]),
    ],
)
data class VodTitle(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceId: Long,
    val name: String,
    val isSeries: Boolean,
    val posterUrl: String? = null,
    val categoryName: String? = null,
    val year: String? = null,
    val rating: String? = null,
    val streamUrl: String? = null,
    /** Xtream vod/series id; null for M3U. */
    val externalId: String? = null,
    /** For a series, the number of [SeriesEpisode] rows grouped under it -- shown on the tile/detail.
     * 0 for movies and for series whose episodes haven't been loaded/grouped yet. */
    val episodeCount: Int = 0,
    // --- Rich metadata, populated lazily on first Detail view (Xtream's own info block first,
    // then the user's OMDb key as a fallback for ratings/plot/cast). All nullable: a title with no
    // metadata source (plain M3U, no OMDb key) simply keeps these null and the Detail sections
    // collapse. See [com.arashrahimi46.iptv.data.repository.PlaylistRepository.ensureMetadataLoaded]. ---
    val plot: String? = null,
    /** Named `castList` (not `cast`) because `cast` is a SQLite keyword. Comma-separated actor names. */
    val castList: String? = null,
    val director: String? = null,
    val genre: String? = null,
    /** IMDb score as returned by OMDb, e.g. "8.4" (0-10 scale). */
    val imdbRating: String? = null,
    /** Rotten Tomatoes score as returned by OMDb, e.g. "94%". */
    val rtRating: String? = null,
    /** True once a metadata fetch has been attempted against a real source, so Detail doesn't
     * re-hit the network every open. Stays false when no source was available (e.g. M3U with no
     * OMDb key yet), so connecting a key later still enriches the title on next open. */
    val metadataFetched: Boolean = false,
)

/**
 * A single episode belonging to a series [VodTitle]. Populated at M3U import time
 * (episodes grouped out of per-episode playlist entries) and lazily for Xtream on
 * first Detail view. Indexed by [seriesTitleId] -- every read filters on it.
 */
@Entity(tableName = "series_episodes", indices = [Index(value = ["seriesTitleId"])])
data class SeriesEpisode(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val seriesTitleId: Long,
    val season: Int,
    val episode: Int,
    val name: String,
    val streamUrl: String? = null,
    val externalId: String? = null,
)

/**
 * EPG programme entry.
 *
 * PERF: the `(channelId, startMs)` index is load-bearing, not housekeeping. This table had no index
 * at all, and a few days of XMLTV for a 20k-channel playlist is 10^5-10^6 rows -- so every guide read
 * planned as `SCAN epg_programs` plus `USE TEMP B-TREE FOR ORDER BY`. Worse, the guide observes this
 * table as a Flow, so ANY write re-ran that full scan for every active observer (the Guide grid, the
 * Home now-playing rail, and the player's now/next). The leading `channelId` turns the `IN (...)`
 * lookup into a per-channel range scan, and the trailing `startMs` lets `ORDER BY startMs` be served
 * from the index instead of a temp B-tree.
 */
@Entity(
    tableName = "epg_programs",
    indices = [Index(value = ["channelId", "startMs"])],
)
data class EPGProgram(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val channelId: Long,
    val title: String,
    val startMs: Long,
    val endMs: Long,
    val description: String? = null,
)

/**
 * A favorited channel or VOD title, keyed by the item's *stable* identity
 * ([streamKey] = its Xtream `externalId`, else its `streamUrl`, else its `name`)
 * rather than by an autogenerated Room row id. Row ids are re-assigned on every
 * re-import (the catalog is deleted + re-inserted under a fresh source id), so a
 * row-id-keyed favorite would silently vanish -- or, worse, resolve to whatever
 * unrelated row later reused that id -- after any refresh. Keying by [streamKey]
 * makes a favorite survive re-imports of the same provider, and it is re-resolved
 * to the current row id per active source at read time. [contentType] distinguishes
 * a channel favorite (LIVE) from a VOD favorite (MOVIE/SERIES). Populated via
 * [com.arashrahimi46.iptv.data.repository.FavoritesRepository].
 */
@Entity(tableName = "favorites", indices = [Index(value = ["streamKey"])])
data class Favorite(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contentType: ContentType,
    val streamKey: String,
    val addedAtMs: Long = System.currentTimeMillis(),
)

/** Resume-playback bookmark. Schema stub -- not populated/wired to UI in this phase. */
@Entity(tableName = "continue_watching")
data class ContinueWatchingEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vodTitleId: Long? = null,
    val seriesEpisodeId: Long? = null,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAtMs: Long = System.currentTimeMillis(),
    /** Set when this bookmark resumes a local [Recording] (Live TV Recording V1) rather than a
     * VOD/episode -- lets a half-watched recording resume through the same player path. Null for
     * ordinary VOD/series bookmarks. */
    val recordingId: Long? = null,
)

/**
 * Persisted lifecycle state of a [Recording]. RECORDING is the only live state; the rest are
 * terminal (see [com.arashrahimi46.iptv.data.recording.RecordingSupervisor] for the state machine).
 * UNAVAILABLE/MISSING are derived at read time from file/drive presence rather than stored (a row
 * whose drive comes back reconciles automatically), but the enum carries them so the UI has one
 * status type to render.
 */
enum class RecordingStatus { RECORDING, COMPLETED, INTERRUPTED, FAILED, UNAVAILABLE, MISSING }

/**
 * One captured live recording (Live TV Recording V1). The DB row is the source of truth; the file(s)
 * on disk are payloads it points at via [storageTreeUri] + [documentId]. See the design spec at
 * docs/recording-v1-design.md.
 */
@Entity(tableName = "recordings")
data class Recording(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val channelId: Long,
    val channelName: String,
    /** Programme title from EPG at record time, if one was airing; else null. */
    val programTitle: String? = null,
    /** Wall-clock captured once when capture started. */
    val startedAtMs: Long,
    /** Null while RECORDING; a real value when clean; approximate (from bytes/bitrate) when INTERRUPTED. */
    val durationMs: Long? = null,
    val sizeBytes: Long = 0,
    /** Running bitrate estimate (bytes*8/elapsed) -- feeds space math + duration approximation. */
    val bitrateBps: Long? = null,
    val status: RecordingStatus,
    /** Human-readable terminal reason, e.g. "disk full", "drive removed", "stream lost". */
    val statusReason: String? = null,
    /** SAF tree the recording was written into (persistable-permission URI, not a File path). */
    val storageTreeUri: String,
    /** SAF document id of the file (or the first part for a split recording). */
    val documentId: String,
    /** >1 for a FAT32 4 GB auto-split recording (…part-001.ts, …part-002.ts, stitched at playback). */
    val parts: Int = 1,
    /** Volume UUID of the destination drive -- verified before trusting a saved [storageTreeUri]
     * (same USB port, different stick), and used to resolve UNAVAILABLE on remount. */
    val volumeUuid: String? = null,
    /** Reserved: recordings of parental-locked channels play without PIN in V1 (§7 trade-off);
     * this lets that be tightened later without a migration. */
    val locked: Boolean = false,
    /** Owning playlist source -- recordings are per-playlist (filtered by the active source, and
     * cascade-deleted when the source is removed). Null for pre-v11 rows recorded before scoping. */
    val sourceId: Long? = null,
)

/**
 * A user-pasted direct video URL ("Open network stream", VLC-style) and its play history. Every
 * URL played through the Streams tab is a row here -- the table IS the history. Replaying an
 * existing URL bumps [lastPlayedAtMs] rather than inserting a duplicate (dedup by [url]); the
 * list is shown newest-played-first. [name] is an optional user-chosen label; when null the UI
 * derives one from the URL host (see [directStreamLabel]).
 */
@Entity(tableName = "direct_streams")
data class DirectStream(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val name: String? = null,
    val createdAtMs: Long = System.currentTimeMillis(),
    val lastPlayedAtMs: Long = System.currentTimeMillis(),
)

/** Display label for a saved direct stream: the user's custom [DirectStream.name] if set, else the
 *  URL's host (e.g. "example.com"), else the raw URL. Shared by the history box and the player title. */
fun directStreamLabel(url: String, name: String?): String {
    name?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
    val host = runCatching { java.net.URI(url).host }.getOrNull()
    return host?.takeIf { it.isNotBlank() } ?: url
}
