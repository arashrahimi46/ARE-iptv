package com.arashrahimi46.iptv.data.repository

import android.content.Context
import com.arashrahimi46.iptv.data.db.AppDatabase
import com.arashrahimi46.iptv.data.model.Channel
import com.arashrahimi46.iptv.data.model.EPGProgram
import com.arashrahimi46.iptv.data.model.PlaylistSource
import com.arashrahimi46.iptv.data.model.SourceType
import com.arashrahimi46.iptv.data.parser.XmlTvParser
import com.arashrahimi46.iptv.data.parser.XtreamClient
import com.arashrahimi46.iptv.data.settings.CredentialsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

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
 * "no programme data" placeholder rather than crashing.
 */
class EpgRepository(context: Context) {
    private val db = AppDatabase.get(context)
    private val credentials = CredentialsStore(context)
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    fun observeForChannels(channelIds: List<Long>, windowStartMs: Long, windowEndMs: Long) =
        db.epgProgramDao().observeForChannelsInWindow(channelIds, windowStartMs, windowEndMs)

    /** Fetches + persists EPG for [channels] of [source]. Best-effort: swallows network/parse failures per-channel/source. */
    suspend fun refresh(source: PlaylistSource, channels: List<Channel>) = withContext(Dispatchers.IO) {
        if (channels.isEmpty()) return@withContext

        val xmlTvUrl = source.epgUrl ?: if (source.type == SourceType.XTREAM) {
            val username = credentials.username(source.id)
            val password = credentials.password(source.id)
            if (username != null && password != null) {
                runCatching { XtreamClient(source.url, username, password).xmltvUrl() }.getOrNull()
            } else {
                null
            }
        } else {
            null
        }

        val byTvgId = channels.filter { !it.tvgId.isNullOrBlank() }.associateBy { it.tvgId }
        val bulkRows = mutableListOf<EPGProgram>()
        // Channels the bulk XMLTV export actually produced a programme for -- tracked
        // per-channel (not a single source-wide flag) so channels lacking a usable tvgId
        // (e.g. an Xtream portal that doesn't expose `epg_channel_id`) still fall through
        // to the per-channel fallback below instead of being starved whenever *any other*
        // channel in the batch happened to match.
        val coveredChannelIds = mutableSetOf<Long>()

        if (xmlTvUrl != null) {
            val body = runCatching { fetchText(xmlTvUrl) }.getOrNull()
            if (body != null) {
                val programmes = runCatching { XmlTvParser.parse(body) }.getOrDefault(emptyList())
                programmes.forEach { p ->
                    val channel = byTvgId[p.channelRef] ?: return@forEach
                    bulkRows += EPGProgram(
                        channelId = channel.id,
                        title = p.title,
                        startMs = p.startMs,
                        endMs = p.stopMs,
                        description = p.description,
                    )
                    coveredChannelIds += channel.id
                }
            }
        }

        // Fallback: per-channel Xtream short EPG, only for channels the bulk export didn't cover.
        val fallbackRows = mutableListOf<EPGProgram>()
        val uncoveredChannels = channels.filter { it.id !in coveredChannelIds }
        if (uncoveredChannels.isNotEmpty() && source.type == SourceType.XTREAM) {
            val fallbackUsername = credentials.username(source.id)
            val fallbackPassword = credentials.password(source.id)
            if (fallbackUsername != null && fallbackPassword != null) {
                val xtream = XtreamClient(source.url, fallbackUsername, fallbackPassword)
                uncoveredChannels.forEach { channel ->
                    val streamId = channel.externalId ?: return@forEach
                    val entries = runCatching { xtream.getShortEpg(streamId) }.getOrDefault(emptyList())
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
            db.epgProgramDao().deleteForChannels(channels.map { it.id })
            db.epgProgramDao().insertAll(rows)
        }
    }

    private fun fetchText(url: String): String {
        val request = Request.Builder().url(url).build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IllegalStateException("Server returned HTTP ${response.code}")
            return response.body?.string() ?: throw IllegalStateException("Empty response body")
        }
    }
}
