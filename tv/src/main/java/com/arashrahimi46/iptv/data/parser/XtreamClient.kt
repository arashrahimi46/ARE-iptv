package com.arashrahimi46.iptv.data.parser

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.ConnectException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException

data class XtreamCategory(val id: String, val name: String)
data class XtreamLiveStream(val id: String, val name: String, val categoryId: String?, val logo: String?, val epgChannelId: String? = null, val archiveDays: Int = 0)
data class XtreamVodStream(val id: String, val name: String, val categoryId: String?, val icon: String?, val containerExtension: String?)
data class XtreamSeries(val id: String, val name: String, val categoryId: String?, val cover: String?)
data class XtreamShortEpgEntry(val title: String, val description: String?, val startMs: Long, val stopMs: Long)

/** A single episode from `get_series_info`, grouped by [season] (as Xtream returns them, keyed by season number). */
data class XtreamSeriesEpisode(val id: String, val season: Int, val episode: Int, val title: String, val containerExtension: String?)

/**
 * Rich metadata Xtream already returns for a VOD/series in its `info` block (get_vod_info /
 * get_series_info) but which the catalog listing endpoints omit. All fields best-effort/nullable --
 * portals populate them inconsistently. Feeds [com.arashrahimi46.iptv.data.model.VodTitle]'s
 * metadata columns as the free first source before the OMDb fallback.
 */
data class XtreamVodInfo(
    val plot: String?,
    val cast: String?,
    val director: String?,
    val genre: String?,
    val rating: String?,
    val year: String?,
)

/**
 * Account + server metadata Xtream returns for free in the bare `player_api.php` response
 * (`user_info` / `server_info` blocks) -- captured during [XtreamClient.authenticate] and surfaced
 * in the Settings "Provider" panel. Every field is nullable because providers omit or misreport
 * many of them; [expiresAtMs] `null` means unlimited/never, [activeConnections] is a point-in-time
 * snapshot (only as fresh as the last catalog refresh). Persisted as a JSON blob on the source row
 * ([toJson]/[fromJson]) rather than as typed columns -- it's display-only and never queried.
 */
data class XtreamAccountInfo(
    val status: String? = null,
    val expiresAtMs: Long? = null,
    val isTrial: Boolean = false,
    val maxConnections: Int? = null,
    val activeConnections: Int? = null,
    val timezone: String? = null,
    val serverTime: String? = null,
    val host: String? = null,
    val port: String? = null,
) {
    fun toJson(): String = JSONObject().apply {
        put("status", status ?: JSONObject.NULL)
        put("expiresAtMs", expiresAtMs ?: JSONObject.NULL)
        put("isTrial", isTrial)
        put("maxConnections", maxConnections ?: JSONObject.NULL)
        put("activeConnections", activeConnections ?: JSONObject.NULL)
        put("timezone", timezone ?: JSONObject.NULL)
        put("serverTime", serverTime ?: JSONObject.NULL)
        put("host", host ?: JSONObject.NULL)
        put("port", port ?: JSONObject.NULL)
    }.toString()

    companion object {
        /** Parse the `user_info` + `server_info` blocks straight off the auth response. */
        fun fromApi(userInfo: JSONObject, serverInfo: JSONObject?): XtreamAccountInfo {
            val expSeconds = userInfo.optStringOrNull("exp_date")?.toLongOrNull()?.takeIf { it > 0 }
            return XtreamAccountInfo(
                status = userInfo.optStringOrNull("status"),
                expiresAtMs = expSeconds?.let { it * 1000 },
                isTrial = userInfo.optStringOrNull("is_trial") == "1",
                maxConnections = userInfo.optStringOrNull("max_connections")?.toIntOrNull(),
                activeConnections = userInfo.optStringOrNull("active_cons")?.toIntOrNull(),
                timezone = serverInfo?.optStringOrNull("timezone"),
                serverTime = serverInfo?.optStringOrNull("time_now"),
                host = serverInfo?.optStringOrNull("url"),
                port = serverInfo?.optStringOrNull("port"),
            )
        }

        /** Rehydrate from the JSON blob stored on the source row; null/garbage -> null. */
        fun fromJson(raw: String?): XtreamAccountInfo? {
            if (raw.isNullOrBlank()) return null
            val json = runCatching { JSONObject(raw) }.getOrNull() ?: return null
            return XtreamAccountInfo(
                status = json.optStringOrNull("status"),
                expiresAtMs = if (json.isNull("expiresAtMs")) null else json.optLong("expiresAtMs").takeIf { it > 0 },
                isTrial = json.optBoolean("isTrial", false),
                maxConnections = if (json.isNull("maxConnections")) null else json.optInt("maxConnections"),
                activeConnections = if (json.isNull("activeConnections")) null else json.optInt("activeConnections"),
                timezone = json.optStringOrNull("timezone"),
                serverTime = json.optStringOrNull("serverTime"),
                host = json.optStringOrNull("host"),
                port = json.optStringOrNull("port"),
            )
        }
    }
}

/**
 * Thrown for unreachable portals, HTTP errors, or a body that isn't the JSON shape Xtream returns.
 * [isAuthError] is set when the portal was reached but rejected the credentials (auth==0, or a
 * 401/403-shaped HTTP response) -- an authoritative failure the caller must surface rather than
 * silently degrade a get.php import to the lossy raw-M3U path.
 */
class XtreamException(message: String, cause: Throwable? = null, val isAuthError: Boolean = false) : Exception(message, cause)

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

    /**
     * Catch-up / archive stream URL (docs/catchup-v1-design.md). Xtream serves a recorded programme at
     * `{base}/timeshift/{user}/{pass}/{durationMin}/{yyyy-MM-dd:HH-mm}/{streamId}.{ext}`. [startProviderLocal]
     * is the programme start already formatted in the PROVIDER's timezone -- NOT the device's; a wrong
     * offset returns the right programme shifted by hours (see the resolver's formatting + the spec's
     * resilience notes). Mirrors [streamUrl]'s builder so the same base-URL normalization applies.
     */
    fun timeshiftUrl(streamId: String, durationMin: Int, startProviderLocal: String, ext: String = "m3u8"): String =
        "${baseUrl()}/timeshift/$username/$password/$durationMin/$startProviderLocal/$streamId.$ext"

    /** Bulk XMLTV export most Xtream portals expose -- one fetch for the whole EPG rather than N per-channel calls. */
    fun xmltvUrl(): String = "${baseUrl()}/xmltv.php?username=$username&password=$password"

    private suspend fun fetch(action: String, extraParams: String = ""): String = withContext(Dispatchers.IO) {
        val url = "${baseUrl()}/player_api.php?username=$username&password=$password&action=$action$extraParams"
        val request = Request.Builder().url(url).build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw XtreamException(
                        httpErrorMessage(response.code),
                        isAuthError = response.code == 401 || response.code == 403,
                    )
                }
                response.body?.string() ?: throw XtreamException("Empty response body for action=$action")
            }
        } catch (e: XtreamException) {
            throw e
        } catch (e: Exception) {
            throw XtreamException(networkErrorMessage(e), e)
        }
    }

    /** Validates credentials and reachability by hitting the bare `player_api.php` auth endpoint, and
     *  returns the account/server metadata that same response carries (for the Settings Provider panel). */
    suspend fun authenticate(): XtreamAccountInfo {
        val body = fetch("")
        val json = runCatching { JSONObject(body) }.getOrElse {
            throw XtreamException("Unexpected response from server -- check the server URL")
        }
        val userInfo = json.optJSONObject("user_info")
        if (userInfo == null || userInfo.optInt("auth", 0) != 1) {
            throw XtreamException("Invalid username or password", isAuthError = true)
        }
        return XtreamAccountInfo.fromApi(userInfo, json.optJSONObject("server_info"))
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
                // Xtream's per-stream XMLTV channel id -- lets us match this live
                // stream to the bulk xmltv.php export by tvgId instead of relying
                // solely on the N-per-channel get_short_epg fallback (see EpgRepository).
                epgChannelId = obj.optStringOrNull("epg_channel_id")?.ifBlank { null },
                // Catch-up / archive: `tv_archive` (0/1) gates it; `tv_archive_duration` is the window
                // length in days. 0 => no archive on this channel. See docs/catchup-v1-design.md.
                archiveDays = if (obj.optStringOrNull("tv_archive") == "1")
                    obj.optStringOrNull("tv_archive_duration")?.toIntOrNull()?.coerceAtLeast(0) ?: 0
                else 0,
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
                // Real container from the portal (mkv/avi/mp4/...). Movies were hardcoded to
                // ".mp4", so any non-mp4 movie got a wrong stream URL and failed to play; series
                // episodes already used their real extension, which is why they worked.
                containerExtension = obj.optStringOrNull("container_extension"),
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

    /** `action=get_vod_info` -- the per-movie rich metadata block. Null when the portal returns
     * nothing usable (many do for some titles); the caller then falls back to OMDb. */
    suspend fun getVodInfo(vodId: String): XtreamVodInfo? {
        val body = fetch("get_vod_info", "&vod_id=$vodId")
        val json = runCatching { JSONObject(body) }.getOrNull() ?: return null
        return parseInfoBlock(json.optJSONObject("info"))
    }

    /** Series-level `info` block from `action=get_series_info` (separate from the per-episode list
     * in [getSeriesInfo]). Null when the portal returns nothing usable. */
    suspend fun getSeriesInfoMeta(seriesId: String): XtreamVodInfo? {
        val body = fetch("get_series_info", "&series_id=$seriesId")
        val json = runCatching { JSONObject(body) }.getOrNull() ?: return null
        return parseInfoBlock(json.optJSONObject("info"))
    }

    /** Maps an Xtream `info` object to [XtreamVodInfo], tolerating the key-name variants portals use.
     * Returns null when every field is absent, so the caller can cleanly fall through to OMDb. */
    private fun parseInfoBlock(info: JSONObject?): XtreamVodInfo? {
        if (info == null) return null
        val plot = info.optStringOrNull("plot") ?: info.optStringOrNull("description")
        val cast = info.optStringOrNull("cast") ?: info.optStringOrNull("actors")
        val director = info.optStringOrNull("director")
        val genre = info.optStringOrNull("genre")
        // Portals commonly send "0"/"0.0" for "no rating" -- treat those as absent.
        val rating = info.optStringOrNull("rating")?.takeUnless { it == "0" || it == "0.0" }
        val release = info.optStringOrNull("releasedate")
            ?: info.optStringOrNull("releaseDate")
            ?: info.optStringOrNull("year")
        val year = release?.let { Regex("(19|20)\\d{2}").find(it)?.value }
        if (plot == null && cast == null && director == null && genre == null && rating == null && year == null) return null
        return XtreamVodInfo(plot, cast, director, genre, rating, year)
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

    /**
     * Distinguishes credentials-shaped HTTP failures (401/403) from generic server
     * errors, per qa-reviewer's Phase 1 finding -- was previously a single
     * "Server returned HTTP N" bucket for every non-2xx response.
     */
    private fun httpErrorMessage(code: Int): String = when (code) {
        401, 403 -> "Invalid username or password"
        else -> "Server returned an error (HTTP $code) -- check the server URL and try again"
    }

    /**
     * Distinguishes common network-failure shapes instead of one catch-all
     * "Could not reach host: <exception message>" bucket, per qa-reviewer's
     * Phase 1 finding -- a bad URL/DNS, a down server, and a slow/unreachable
     * network now read differently to the user instead of depending on
     * whatever incidental string the underlying exception happens to produce.
     */
    private fun networkErrorMessage(e: Exception): String = when (e) {
        is UnknownHostException -> "Could not find server \"$host\" -- check the address and your connection"
        is java.net.SocketTimeoutException -> "Server \"$host\" took too long to respond -- check your connection and try again"
        is ConnectException -> "Could not connect to \"$host\" -- the server may be down"
        is SSLException -> "Secure connection to \"$host\" failed -- check the server address"
        else -> "Could not reach $host: ${e.message}"
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
