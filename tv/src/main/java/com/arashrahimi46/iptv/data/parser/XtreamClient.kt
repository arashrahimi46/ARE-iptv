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

/** Thrown for unreachable portals, HTTP errors, or a body that isn't the JSON shape Xtream returns (bad credentials). */
class XtreamException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Minimal Xtream Codes `player_api.php` client -- enough to populate the
 * top-level catalog (categories + live/VOD/series lists). Series episode
 * drill-down (`get_series_info`) is left for a later phase.
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

    private suspend fun fetch(action: String): String = withContext(Dispatchers.IO) {
        val url = "${baseUrl()}/player_api.php?username=$username&password=$password&action=$action"
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
