package com.arashrahimi46.iptv.data.parser

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class XtreamCategory(val id: String, val name: String)
data class XtreamLiveStream(val id: String, val name: String, val categoryId: String?, val logo: String?)
data class XtreamVodStream(val id: String, val name: String, val categoryId: String?, val icon: String?)
data class XtreamSeries(val id: String, val name: String, val categoryId: String?, val cover: String?)
data class XtreamShortEpgEntry(val title: String, val description: String?, val startMs: Long, val stopMs: Long)

/** A single episode from `get_series_info`, grouped by [season] (as Xtream returns them, keyed by season number). */
data class XtreamSeriesEpisode(val id: String, val season: Int, val episode: Int, val title: String, val containerExtension: String?)

/** Thrown for unreachable portals, HTTP errors, or a body that isn't the JSON shape Xtream returns (bad credentials). */
class XtreamException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Minimal Xtream Codes `player_api.php` client -- enough to populate the
 * top-level catalog (categories + live/VOD/series lists) plus per-series
 * episode drill-down via [getSeriesInfo].
 */
class XtreamClient(
    private val host: String,
    private val username: String,
    private val password: String,
    private val client: OkHttpClient = defaultClient,
) {
    private fun baseUrl(): String {
        val normalized = if (host.startsWith("http://") || host.startsWith("https://")) host else "http://$host"
        return normalized.trimEnd('/')
    }

    fun streamUrl(kind: String, streamId: String, ext: String): String =
        "${baseUrl()}/$kind/$username/$password/$streamId.$ext"

    /** Bulk XMLTV export most Xtream portals expose -- one fetch for the whole EPG rather than N per-channel calls. */
    fun xmltvUrl(): String = "${baseUrl()}/xmltv.php?username=$username&password=$password"

    private suspend fun fetch(action: String, extraParams: String = ""): String = withContext(Dispatchers.IO) {
        val url = "${baseUrl()}/player_api.php?username=$username&password=$password&action=$action$extraParams"
        val request = Request.Builder().url(url).build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw XtreamException("Server returned HTTP ${response.code} for action=$action")
                }
                response.body?.string() ?: throw XtreamException("Empty response body for action=$action")
            }
        } catch (e: XtreamException) {
            throw e
        } catch (e: Exception) {
            throw XtreamException("Could not reach $host: ${e.message}", e)
        }
    }

    /** Validates credentials and reachability by hitting the bare `player_api.php` auth endpoint. */
    suspend fun authenticate() {
        val body = fetch("")
        val json = runCatching { JSONObject(body) }.getOrElse {
            throw XtreamException("Unexpected response from server -- check the server URL")
        }
        val auth = json.optJSONObject("user_info")
        if (auth == null || auth.optInt("auth", 0) != 1) {
            throw XtreamException("Invalid username or password")
        }
    }

    suspend fun getLiveCategories(): List<XtreamCategory> = parseCategories(fetch("get_live_categories"))
    suspend fun getVodCategories(): List<XtreamCategory> = parseCategories(fetch("get_vod_categories"))
    suspend fun getSeriesCategories(): List<XtreamCategory> = parseCategories(fetch("get_series_categories"))

    suspend fun getLiveStreams(): List<XtreamLiveStream> {
        val arr = parseArray(fetch("get_live_streams"))
        return arr.mapNotNullIndexed { obj ->
            val id = obj.optStringOrNull("stream_id") ?: return@mapNotNullIndexed null
            XtreamLiveStream(
                id = id,
                name = obj.optString("name", "Unnamed"),
                categoryId = obj.optStringOrNull("category_id"),
                logo = obj.optStringOrNull("stream_icon"),
            )
        }
    }

    suspend fun getVodStreams(): List<XtreamVodStream> {
        val arr = parseArray(fetch("get_vod_streams"))
        return arr.mapNotNullIndexed { obj ->
            val id = obj.optStringOrNull("stream_id") ?: return@mapNotNullIndexed null
            XtreamVodStream(
                id = id,
                name = obj.optString("name", "Unnamed"),
                categoryId = obj.optStringOrNull("category_id"),
                icon = obj.optStringOrNull("stream_icon"),
            )
        }
    }

    /** Per-channel short EPG (`action=get_short_epg`) -- fallback when the bulk XMLTV export isn't usable. */
    suspend fun getShortEpg(streamId: String, limit: Int = 8): List<XtreamShortEpgEntry> = withContext(Dispatchers.IO) {
        val url = "${baseUrl()}/player_api.php?username=$username&password=$password&action=get_short_epg&stream_id=$streamId&limit=$limit"
        val request = Request.Builder().url(url).build()
        val body = try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                response.body?.string() ?: return@withContext emptyList()
            }
        } catch (e: Exception) {
            return@withContext emptyList()
        }
        val json = runCatching { JSONObject(body) }.getOrNull() ?: return@withContext emptyList()
        val listings = json.optJSONArray("epg_listings") ?: return@withContext emptyList()
        (0 until listings.length()).mapNotNull { i ->
            val entry = listings.optJSONObject(i) ?: return@mapNotNull null
            val startMs = entry.optLong("start_timestamp", -1L) * 1000
            val stopMs = entry.optLong("stop_timestamp", -1L) * 1000
            if (startMs <= 0 || stopMs <= 0) return@mapNotNull null
            XtreamShortEpgEntry(
                title = decodeMaybeBase64(entry.optString("title")),
                description = entry.optStringOrNull("description")?.let(::decodeMaybeBase64),
                startMs = startMs,
                stopMs = stopMs,
            )
        }
    }

    suspend fun getSeries(): List<XtreamSeries> {
        val arr = parseArray(fetch("get_series"))
        return arr.mapNotNullIndexed { obj ->
            val id = obj.optStringOrNull("series_id") ?: return@mapNotNullIndexed null
            XtreamSeries(
                id = id,
                name = obj.optString("name", "Unnamed"),
                categoryId = obj.optStringOrNull("category_id"),
                cover = obj.optStringOrNull("cover"),
            )
        }
    }

    /**
     * `action=get_series_info` -- the season/episode drill-down for a single
     * series. Xtream returns `{"episodes": {"1": [ {...}, ... ], "2": [...] }}`
     * keyed by season number (as a string); each episode object carries its
     * own `id`/`episode_num`/`title`/`container_extension`. Malformed/missing
     * entries are skipped rather than throwing, so one bad episode doesn't
     * blank the whole season list.
     */
    suspend fun getSeriesInfo(seriesId: String): List<XtreamSeriesEpisode> {
        val body = fetch("get_series_info", "&series_id=$seriesId")
        val json = runCatching { JSONObject(body) }.getOrElse {
            throw XtreamException("Malformed JSON response for get_series_info")
        }
        val episodesObj = json.optJSONObject("episodes") ?: return emptyList()
        val seasonKeys = episodesObj.names() ?: JSONArray()
        val out = mutableListOf<XtreamSeriesEpisode>()
        for (i in 0 until seasonKeys.length()) {
            val seasonKey = seasonKeys.optString(i) ?: continue
            val season = seasonKey.toIntOrNull() ?: continue
            val arr = episodesObj.optJSONArray(seasonKey) ?: continue
            for (j in 0 until arr.length()) {
                val ep = arr.optJSONObject(j) ?: continue
                val id = ep.optStringOrNull("id") ?: continue
                val episodeNum = ep.optInt("episode_num", j + 1)
                out += XtreamSeriesEpisode(
                    id = id,
                    season = season,
                    episode = episodeNum,
                    title = ep.optString("title", "Episode $episodeNum"),
                    containerExtension = ep.optStringOrNull("container_extension"),
                )
            }
        }
        return out
    }

    private fun parseCategories(body: String): List<XtreamCategory> =
        parseArray(body).mapNotNullIndexed { obj ->
            val id = obj.optStringOrNull("category_id") ?: return@mapNotNullIndexed null
            XtreamCategory(id = id, name = obj.optString("category_name", id))
        }

    private fun parseArray(body: String): JSONArray =
        runCatching { JSONArray(body) }.getOrElse {
            // Some malformed portals return {} instead of [] when a section is empty.
            if (body.trim().startsWith("{")) JSONArray() else throw XtreamException("Malformed JSON response")
        }

    companion object {
        private val defaultClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }
}

/** Xtream commonly base64-encodes EPG title/description text; falls back to the raw string when it isn't. */
private fun decodeMaybeBase64(value: String): String =
    runCatching { String(android.util.Base64.decode(value, android.util.Base64.DEFAULT)) }.getOrDefault(value)

private fun JSONObject.optStringOrNull(key: String): String? =
    if (has(key) && !isNull(key)) opt(key)?.toString()?.takeIf { it.isNotBlank() } else null

private inline fun <T> JSONArray.mapNotNullIndexed(transform: (JSONObject) -> T?): List<T> {
    val out = ArrayList<T>(length())
    for (i in 0 until length()) {
        val obj = optJSONObject(i) ?: continue
        transform(obj)?.let { out += it }
    }
    return out
}
