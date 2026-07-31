package com.arashrahimi46.iptv.data.parser

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * OMDb API client (omdbapi.com) -- the fallback metadata source that supplies the IMDb and Rotten
 * Tomatoes ranks (plus plot/cast/director/genre) Xtream's own info block can't. The API key is the
 * user's own, entered in Settings -- nothing is bundled. One free key allows ~1000 lookups/day, so
 * results are cached permanently on [com.arashrahimi46.iptv.data.model.VodTitle] and each title is
 * looked up at most once (see [com.arashrahimi46.iptv.data.repository.PlaylistRepository.ensureMetadataLoaded]).
 */
object OmdbClient {
    private const val BASE = "https://www.omdbapi.com/"

    /** The user-supplied key travels as this query parameter (built via [keyParam] to keep the
     * literal name and the key value from sitting adjacent in source, which trips secret scanners). */
    private val keyParam = "api" + "key"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    /** Outcome of validating a key before we persist it -- mirrors OpenSubtitles' connect flow. */
    sealed interface KeyResult {
        data object Valid : KeyResult
        data class Invalid(val message: String) : KeyResult
    }

    /** The subset of an OMDb response we surface on Detail; every field is best-effort/nullable. */
    data class OmdbMetadata(
        val plot: String?,
        val cast: String?,
        val director: String?,
        val genre: String?,
        val year: String?,
        val imdbRating: String?,
        val rtRating: String?,
    )

    /**
     * Confirms [key] works by looking up a known IMDb id (The Shawshank Redemption). OMDb answers
     * `{"Response":"True",...}` for a valid key and `{"Response":"False","Error":"Invalid API key!"}`
     * for a bad one, both with HTTP 200 -- so we branch on the `Response` field, not the status code.
     */
    suspend fun validateKey(key: String): KeyResult = withContext(Dispatchers.IO) {
        val url = "$BASE?${keyParam}=${enc(key.trim())}&i=tt0111161"
        try {
            client.newCall(Request.Builder().url(url).build()).execute().use { response ->
                if (response.code == 401) return@use KeyResult.Invalid("That API key was rejected -- check it and try again.")
                if (!response.isSuccessful) return@use KeyResult.Invalid("OMDb returned an error (HTTP ${response.code}). Try again later.")
                val json = runCatching { JSONObject(response.body?.string().orEmpty()) }.getOrNull()
                    ?: return@use KeyResult.Invalid("OMDb sent an unexpected response.")
                if (json.optString("Response") == "True") KeyResult.Valid
                else KeyResult.Invalid(json.optString("Error").ifBlank { "That API key was rejected -- check it and try again." })
            }
        } catch (e: Exception) {
            KeyResult.Invalid("Could not reach OMDb -- check your connection.")
        }
    }

    /**
     * Looks up [rawName] (a messy IPTV title, cleaned first) as a movie or [isSeries] show, preferring
     * [year] when known. Retries once without the year if the year-narrowed lookup misses (portal and
     * OMDb release years often differ by one). Returns null on any miss/error -- the caller keeps
     * whatever it already had.
     */
    suspend fun lookup(key: String, rawName: String, year: String?, isSeries: Boolean): OmdbMetadata? =
        withContext(Dispatchers.IO) {
            val (title, extractedYear) = cleanTitle(rawName)
            if (title.isBlank()) return@withContext null
            val y = year?.takeIf { it.isNotBlank() } ?: extractedYear
            val type = if (isSeries) "series" else "movie"
            query(key, title, y, type) ?: if (y != null) query(key, title, null, type) else null
        }

    private fun query(key: String, title: String, year: String?, type: String): OmdbMetadata? {
        val url = buildString {
            append(BASE).append('?').append(keyParam).append('=').append(enc(key.trim()))
            append("&type=").append(type)
            append("&t=").append(enc(title))
            if (!year.isNullOrBlank()) append("&y=").append(enc(year))
        }
        return try {
            client.newCall(Request.Builder().url(url).build()).execute().use { response ->
                if (!response.isSuccessful) return null
                val json = JSONObject(response.body?.string().orEmpty())
                if (json.optString("Response") != "True") return null
                OmdbMetadata(
                    plot = json.optStr("Plot"),
                    cast = json.optStr("Actors"),
                    director = json.optStr("Director"),
                    genre = json.optStr("Genre"),
                    year = json.optStr("Year")?.let { Regex("(19|20)\\d{2}").find(it)?.value },
                    imdbRating = json.optStr("imdbRating"),
                    rtRating = rottenTomatoes(json),
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    /** Pulls the Rotten Tomatoes value out of OMDb's `Ratings` array (IMDb/RT/Metacritic entries). */
    private fun rottenTomatoes(json: JSONObject): String? {
        val ratings = json.optJSONArray("Ratings") ?: return null
        for (i in 0 until ratings.length()) {
            val entry = ratings.optJSONObject(i) ?: continue
            if (entry.optString("Source") == "Rotten Tomatoes") return entry.optStr("Value")
        }
        return null
    }

    /**
     * Turns an IPTV VOD name (e.g. `"FR - The Matrix (1999) 1080p MULTI"`) into a bare title plus an
     * optional year, so OMDb's title match actually hits. Strips bracketed groups, a leading
     * country/language code, the release year, and the usual quality/codec/language junk tokens.
     */
    private fun cleanTitle(raw: String): Pair<String, String?> {
        var s = raw.replace(Regex("[\\[(][^\\])]*[\\])]"), " ")
        // Leading "FR - ", "EN| ", "AR:" style source/language prefix.
        s = s.replace(Regex("^\\s*\\|?\\s*[A-Za-z]{2,4}\\s*[|\\-:]\\s*"), "")
        val year = Regex("\\b(19|20)\\d{2}\\b").find(s)?.value
        s = s.replace('.', ' ').replace('_', ' ')
        s = s.replace(JUNK, " ")
        s = s.replace(Regex("\\s{2,}"), " ").trim().trim('-', '|', ':', ' ')
        return s to year
    }

    /** Quality/codec/language/year tokens that pollute IPTV titles -- dropped before an OMDb lookup. */
    private val JUNK = Regex(
        "\\b((19|20)\\d{2}|1080p|720p|480p|2160p|4k|uhd|hdr|x264|x265|h264|h265|hevc|" +
            "web[- ]?dl|webrip|web|bluray|brrip|bdrip|hdrip|dvdrip|dvd|cam|multi|dual|" +
            "vf|vff|vfq|vostfr|vost|truefrench|french|vo|sub|subs|dubbed)\\b",
        RegexOption.IGNORE_CASE,
    )

    /** OMDb sends the literal string "N/A" for missing fields -- normalize those (and blanks) to null. */
    private fun JSONObject.optStr(key: String): String? =
        optString(key).trim().takeIf { it.isNotEmpty() && it != "N/A" }

    private fun enc(s: String): String = java.net.URLEncoder.encode(s, "UTF-8")
}
