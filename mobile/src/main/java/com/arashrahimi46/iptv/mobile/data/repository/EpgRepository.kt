package com.arashrahimi46.iptv.mobile.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.arashrahimi46.iptv.mobile.data.db.AppDatabase
import com.arashrahimi46.iptv.mobile.data.model.Channel
import com.arashrahimi46.iptv.mobile.data.model.EPGProgram
import com.arashrahimi46.iptv.mobile.data.model.PlaylistSource
import com.arashrahimi46.iptv.mobile.data.model.SourceType
import com.arashrahimi46.iptv.mobile.data.parser.M3uParser
import com.arashrahimi46.iptv.mobile.data.parser.XmlTvParser
import com.arashrahimi46.iptv.mobile.data.parser.XmlTvProgramme
import com.arashrahimi46.iptv.mobile.data.parser.XtreamClient
import com.arashrahimi46.iptv.mobile.data.settings.CredentialsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.InputStream
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream

/** Safe chunk size for `IN (...)` binds -- under SQLite's historical 999 host-parameter limit. */
private const val SQLITE_MAX_VARIABLES = 900

/**
 * P0.4: distinguishes "we successfully talked to the EPG source and it genuinely has no
 * programme data" from "we couldn't reach/parse the source at all" -- [EpgRepository.refresh]
 * updates [EpgRepository.availability] with this at the end of every run so the Guide screen
 * can show a real message for an unreachable source instead of silently rendering blank rows.
 */
sealed class EpgAvailability {
    object Idle : EpgAvailability()
    object Available : EpgAvailability()
    data class Unavailable(val reason: String? = null) : EpgAvailability()
}

/**
 * Pure resolution of [EpgAvailability] from [EpgRepository.refresh]'s network-attempt
 * bookkeeping -- extracted as a top-level function so it's unit-testable without standing up
 * Room/OkHttp. [rowsEmpty]: no [EPGProgram] rows were produced this run. [attemptedAny]: at
 * least one real network call (bulk XMLTV fetch or a per-channel short-EPG call) was actually
 * issued. [succeededAny]: at least one of those calls completed without throwing.
 */
/** Normalizes a tvg-id / XMLTV channel id for matching. Providers routinely differ only in
 * case or surrounding whitespace between the M3U `tvg-id` and the XMLTV `channel` id, so a raw
 * string compare silently drops those channels' programmes. */
private fun normalizeTvgId(raw: String): String = raw.trim().lowercase()

/**
 * Pure XMLTV-to-channel match: pairs each parsed [XmlTvProgramme] with the [Channel] whose
 * `tvgId` matches its `channelRef` (case/whitespace-insensitive -- see [normalizeTvgId]) and
 * returns the [EPGProgram] rows to persist. Extracted so the matching that actually populates
 * the Guide is unit-testable without Room/OkHttp. Unmatched programmes and channels without a
 * usable `tvgId` are simply dropped.
 */
internal fun matchXmlTvProgrammes(channels: List<Channel>, programmes: List<XmlTvProgramme>): List<EPGProgram> {
    if (channels.isEmpty() || programmes.isEmpty()) return emptyList()
    // groupBy (not associateBy): playlists routinely list SD/HD/backup variants of a channel that
    // share one tvg-id. associateBy kept only the LAST variant, so every other one rendered "No
    // programme data". Fan each programme out to ALL channels sharing its id instead.
    val byKey = channels.filter { !it.tvgId.isNullOrBlank() }.groupBy { normalizeTvgId(it.tvgId!!) }
    return programmes.flatMap { p ->
        val matched = byKey[normalizeTvgId(p.channelRef)] ?: return@flatMap emptyList()
        matched.map { channel ->
            EPGProgram(
                channelId = channel.id,
                title = p.title,
                startMs = p.startMs,
                endMs = p.stopMs,
                description = p.description,
            )
        }
    }
}

internal fun resolveEpgAvailability(rowsEmpty: Boolean, attemptedAny: Boolean, succeededAny: Boolean): EpgAvailability = when {
    !rowsEmpty -> EpgAvailability.Available
    !attemptedAny -> EpgAvailability.Unavailable("No EPG source configured for this playlist")
    succeededAny -> EpgAvailability.Available
    else -> EpgAvailability.Unavailable("Couldn't reach the EPG source")
}

/**
 * Populates [EPGProgram] rows for the Guide screen (Room schema existed
 * since Phase 1 but was unpopulated). Two real EPG sources, per the
 * playlist's [PlaylistSource]:
 *  - an explicit XMLTV [PlaylistSource.epgUrl] (M3U or Xtream): one bulk
 *    fetch + parse, matched to channels by `tvgId`.
 *  - an Xtream portal with no custom EPG URL: prefers the bulk XMLTV export
 *    (`xmltv.php`) over N per-channel `get_short_epg` calls; falls back to
 *    per-channel short EPG only if the bulk export isn't reachable/parseable.
 * If neither is available, callers see an empty result and render a
 * "no programme data" placeholder rather than crashing. [availability] surfaces WHY rows are
 * empty (genuinely no programme data vs a real fetch failure) -- see [EpgAvailability]/
 * [resolveEpgAvailability].
 */
class EpgRepository(context: Context) {
    private val db = AppDatabase.get(context)
    private val credentials = CredentialsStore(context)
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val _availability = MutableStateFlow<EpgAvailability>(EpgAvailability.Idle)
    val availability: StateFlow<EpgAvailability> = _availability.asStateFlow()

    fun observeForChannels(channelIds: List<Long>, windowStartMs: Long, windowEndMs: Long): Flow<List<EPGProgram>> {
        if (channelIds.isEmpty()) return flowOf(emptyList())
        // SQLite caps host params at 999, so a large playlist's `channelId IN (...)` overflows
        // ("too many SQL variables" crash). Chunk the IN-list under the cap and merge the results.
        val chunks = channelIds.chunked(SQLITE_MAX_VARIABLES)
        if (chunks.size == 1) {
            return db.epgProgramDao().observeForChannelsInWindow(chunks[0], windowStartMs, windowEndMs)
        }
        return combine(chunks.map { db.epgProgramDao().observeForChannelsInWindow(it, windowStartMs, windowEndMs) }) {
            it.asList().flatten()
        }
    }

    /** Fetches + persists EPG for [channels] of [source]. Best-effort per-channel/source, but
     * unlike before, a total failure isn't silent: [availability] is updated at the end (see
     * [resolveEpgAvailability]) so callers can tell "genuinely no programme data" apart from
     * "the source couldn't be reached at all". */
    suspend fun refresh(source: PlaylistSource, channels: List<Channel>) = withContext(Dispatchers.IO) {
        if (channels.isEmpty()) return@withContext

        val xmlTvUrl = source.epgUrl ?: when (source.type) {
            SourceType.XTREAM -> {
                val (xtUser, xtSecret) = credentials.forSource(source.id)
                if (xtUser != null && xtSecret != null) {
                    runCatching { XtreamClient(source.url, xtUser, xtSecret).xmltvUrl() }.getOrNull()
                } else {
                    null
                }
            }
            SourceType.M3U ->
                // Heal M3U sources imported before header parsing existed (or that failed to capture it):
                // fetch just the `#EXTM3U` header line, read its `url-tvg`, and persist it so later
                // refreshes short-circuit here via `source.epgUrl` above instead of re-fetching.
                backfillM3uEpgUrl(source)
            // Stalker: EPG comes only from an explicit XMLTV override (source.epgUrl, handled above).
            // Native per-channel short-EPG needs the portal's numeric channel id (not stored in V1's
            // no-migration schema) and is deferred to Phase 5's real-portal validation.
            SourceType.STALKER -> null
        }

        val bulkRows = mutableListOf<EPGProgram>()
        // Channels the bulk XMLTV export actually produced a programme for -- tracked
        // per-channel (not a single source-wide flag) so channels lacking a usable tvgId
        // (e.g. an Xtream portal that doesn't expose `epg_channel_id`) still fall through
        // to the per-channel fallback below instead of being starved whenever *any other*
        // channel in the batch happened to match.
        val coveredChannelIds = mutableSetOf<Long>()

        // P0.4: tracks whether a real network call was made for the bulk XMLTV export, and
        // whether it actually succeeded -- feeds resolveEpgAvailability below.
        var xmlTvAttempted = false
        var xmlTvSucceeded = false

        if (xmlTvUrl != null) {
            xmlTvAttempted = true
            val body = runCatching { fetchText(xmlTvUrl) }.getOrNull()
            if (body != null) {
                xmlTvSucceeded = true
                val programmes = runCatching { XmlTvParser.parse(body) }.getOrDefault(emptyList())
                val matched = matchXmlTvProgrammes(channels, programmes)
                bulkRows += matched
                coveredChannelIds += matched.map { it.channelId }
            }
        }

        // Fallback: per-channel Xtream short EPG, only for channels the bulk export didn't cover.
        val fallbackRows = mutableListOf<EPGProgram>()
        var shortEpgAttempted = false
        var shortEpgSucceeded = false
        val uncoveredChannels = channels.filter { it.id !in coveredChannelIds }
        if (uncoveredChannels.isNotEmpty() && source.type == SourceType.XTREAM) {
            val fallbackUsername = credentials.username(source.id)
            val fallbackPassword = credentials.password(source.id)
            if (fallbackUsername != null && fallbackPassword != null) {
                val xtream = XtreamClient(source.url, fallbackUsername, fallbackPassword)
                uncoveredChannels.forEach { channel ->
                    val streamId = channel.externalId ?: return@forEach
                    shortEpgAttempted = true
                    val result = runCatching { xtream.getShortEpg(streamId) }
                    if (result.isSuccess) shortEpgSucceeded = true
                    val entries = result.getOrDefault(emptyList())
                    entries.forEach { entry ->
                        fallbackRows += EPGProgram(
                            channelId = channel.id,
                            title = entry.title,
                            startMs = entry.startMs,
                            endMs = entry.stopMs,
                            description = entry.description,
                        )
                    }
                }
            }
        }

        val rows = bulkRows + fallbackRows
        if (rows.isNotEmpty()) {
            // ONE transaction for the whole replace. Two reasons, both felt on a TV:
            //  - each chunked delete and the insert were separate transactions, so each paid its own
            //    WAL commit + fsync on slow flash;
            //  - more visibly, every one of those commits invalidated `epg_programs`, and the guide
            //    observes it via a Flow -- so the grid re-queried once per chunk mid-refresh and
            //    could briefly render empty between the delete and the insert. Wrapping them makes
            //    the swap atomic: observers see the old rows, then the new ones, never neither.
            db.withTransaction {
                // Same SQLite host-param cap as observeForChannels -- chunk the delete's IN(...) list.
                channels.map { it.id }.chunked(SQLITE_MAX_VARIABLES)
                    .forEach { db.epgProgramDao().deleteForChannels(it) }
                db.epgProgramDao().insertAll(rows)
            }
        }

        _availability.value = resolveEpgAvailability(
            rowsEmpty = rows.isEmpty(),
            attemptedAny = xmlTvAttempted || shortEpgAttempted,
            succeededAny = xmlTvSucceeded || shortEpgSucceeded,
        )
    }

    /** Fetches only the `#EXTM3U` header of [source]'s M3U, extracts its declared EPG feed URL,
     * and persists it on the source. Returns the URL (or null if the header carries none / is
     * unreachable). Best-effort -- the stream is closed as soon as the header line is read, so
     * this never downloads the (potentially 100s of MB) playlist body. */
    private suspend fun backfillM3uEpgUrl(source: PlaylistSource): String? {
        val headerUrl = runCatching { fetchM3uHeaderEpgUrl(source.url) }.getOrNull() ?: return null
        db.playlistSourceDao().setEpgUrl(source.id, headerUrl)
        return headerUrl
    }

    private fun fetchM3uHeaderEpgUrl(url: String): String? {
        val request = Request.Builder().url(url).build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val reader = response.body?.charStream()?.buffered() ?: return null
            // The header is the first non-blank line of a valid M3U -- stop there.
            reader.useLines { lines ->
                for (raw in lines) {
                    if (raw.isBlank()) continue
                    return M3uParser.extractEpgUrl(raw)
                }
            }
        }
        return null
    }

    private fun fetchText(url: String): String {
        val request = Request.Builder().url(url).build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IllegalStateException("Server returned HTTP ${response.code}")
            val body = response.body ?: throw IllegalStateException("Empty response body")
            return decodeMaybeGzip(body.byteStream())
        }
    }
}

/**
 * Reads [input] to a UTF-8 String, transparently inflating it when the payload is gzipped.
 * Almost the entire open-source XMLTV ecosystem (epgshare01, and the guides that iptv-org /
 * Free-TV playlists point to via `url-tvg`) is served as a gzipped `.xml.gz` *file body* --
 * OkHttp only inflates transport-level `Content-Encoding: gzip`, never a gzipped payload, so
 * without this every such guide parsed to nothing and the Guide showed "no programme data".
 * Sniffs the two-byte gzip magic (0x1f 0x8b) rather than trusting the URL/extension, since some
 * hosts serve gzip without a `.gz` suffix and vice versa. Top-level + `internal` so it's unit-
 * testable without standing up OkHttp.
 */
internal fun decodeMaybeGzip(input: InputStream): String {
    val buffered = input.buffered()
    buffered.mark(2)
    val isGzip = buffered.read() == 0x1f && buffered.read() == 0x8b
    buffered.reset()
    val stream: InputStream = if (isGzip) GZIPInputStream(buffered) else buffered
    return stream.reader(Charsets.UTF_8).use { it.readText() }
}
