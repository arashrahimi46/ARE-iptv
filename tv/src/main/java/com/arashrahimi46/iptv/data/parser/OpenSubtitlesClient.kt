package com.arashrahimi46.iptv.data.parser

import com.arashrahimi46.iptv.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** One subtitle result from a `/subtitles` search -- [fileId] is what `/download` needs. */
data class OnlineSubtitle(val fileId: Long, val release: String, val language: String, val downloads: Int)

/**
 * OpenSubtitles REST client (api.opensubtitles.com/api/v1). Search needs only the user's API key;
 * download additionally needs a login token (it draws on the account's daily quota). The key and
 * account are the user's own (entered in Settings) -- nothing is bundled or hardcoded.
 */
object OpenSubtitlesClient {
    private const val BASE = "https://api.opensubtitles.com/api/v1"
    private val userAgent = "ARE-IPTV v${BuildConfig.VERSION_NAME}"
    private val jsonType = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private fun Request.Builder.commonHeaders(credential: String) = apply {
        header("Api-Key", credential.trim())
        header("User-Agent", userAgent)
        header("Accept", "application/json")
    }

    /** Outcome of validating a credential: [Valid] carries how many languages it could fetch (proof we can read data). */
    sealed interface KeyResult {
        data class Valid(val languageCount: Int) : KeyResult
        data class Invalid(val message: String) : KeyResult
    }

    /** A logged-in session: [bearer] authorizes `/download`; [remaining] is today's remaining quota. */
    sealed interface LoginResult {
        data class Success(val bearer: String, val remaining: Int) : LoginResult
        data class Failure(val message: String) : LoginResult
    }

    /**
     * Confirms [credential] works by fetching `/infos/languages` -- an authenticated endpoint that
     * returns real data, so a 200 with a non-empty list proves both the credential and our ability
     * to retrieve subtitles. 401/403 means a bad key; other failures surface as a network message.
     */
    suspend fun validateKey(credential: String): KeyResult = withContext(Dispatchers.IO) {
        val request = Request.Builder().url("$BASE/infos/languages").commonHeaders(credential).build()
        try {
            client.newCall(request).execute().use { response ->
                when {
                    response.code == 401 || response.code == 403 ->
                        KeyResult.Invalid("That API key was rejected -- check it and try again.")
                    !response.isSuccessful ->
                        KeyResult.Invalid("OpenSubtitles returned an error (HTTP ${response.code}). Try again later.")
                    else -> {
                        val body = response.body?.string().orEmpty()
                        val count = runCatching { JSONObject(body).optJSONArray("data")?.length() ?: 0 }.getOrDefault(0)
                        if (count > 0) KeyResult.Valid(count)
                        else KeyResult.Invalid("Connected, but no data came back -- the key may be inactive.")
                    }
                }
            }
        } catch (e: Exception) {
            KeyResult.Invalid("Could not reach OpenSubtitles -- check your connection.")
        }
    }

    /** Exchanges account credentials for a bearer token via `POST /login`. [phrase] is the account password. */
    suspend fun login(credential: String, username: String, phrase: String): LoginResult = withContext(Dispatchers.IO) {
        val payload = JSONObject().put("username", username.trim()).put("password", phrase).toString()
        val request = Request.Builder()
            .url("$BASE/login")
            .commonHeaders(credential)
            .post(payload.toRequestBody(jsonType))
            .build()
        try {
            client.newCall(request).execute().use { response ->
                when {
                    response.code == 401 || response.code == 403 ->
                        LoginResult.Failure("Wrong username or password.")
                    !response.isSuccessful ->
                        LoginResult.Failure("Sign-in failed (HTTP ${response.code}). Try again later.")
                    else -> {
                        val json = runCatching { JSONObject(response.body?.string().orEmpty()) }.getOrNull()
                        val bearer = json?.optString("token")?.takeIf { it.isNotBlank() }
                            ?: return@use LoginResult.Failure("Sign-in returned no token.")
                        val remaining = json.optJSONObject("user")?.optInt("allowed_downloads", 0) ?: 0
                        LoginResult.Success(bearer, remaining)
                    }
                }
            }
        } catch (e: Exception) {
            LoginResult.Failure("Could not reach OpenSubtitles -- check your connection.")
        }
    }

    /**
     * Searches `/subtitles` for [query] in [languages] (comma-separated ISO codes). [year] narrows a
     * movie; [season]/[episode] with `type=episode` narrows a TV episode. Returns most-downloaded
     * first; empty on any error (the caller shows "no matches").
     */
    suspend fun search(
        credential: String,
        query: String,
        languages: String,
        year: String? = null,
        season: Int? = null,
        episode: Int? = null,
    ): List<OnlineSubtitle> = withContext(Dispatchers.IO) {
        val url = StringBuilder("$BASE/subtitles?query=${enc(query)}&languages=${enc(languages)}")
        if (season != null && episode != null) {
            url.append("&type=episode&season_number=$season&episode_number=$episode")
        } else {
            url.append("&type=movie")
            if (!year.isNullOrBlank()) url.append("&year=${enc(year)}")
        }
        val request = Request.Builder().url(url.toString()).commonHeaders(credential).build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val data = JSONObject(response.body?.string().orEmpty()).optJSONArray("data") ?: return@withContext emptyList()
                (0 until data.length()).mapNotNull { i ->
                    val attrs = data.optJSONObject(i)?.optJSONObject("attributes") ?: return@mapNotNull null
                    val fileId = attrs.optJSONArray("files")?.optJSONObject(0)?.optLong("file_id", -1L) ?: -1L
                    if (fileId <= 0) return@mapNotNull null
                    OnlineSubtitle(
                        fileId = fileId,
                        release = attrs.optString("release").ifBlank { "Subtitle" },
                        language = attrs.optString("language").ifBlank { languages.substringBefore(',') },
                        downloads = attrs.optInt("download_count", 0),
                    )
                }.sortedByDescending { it.downloads }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Outcome of a `/download` request, split so the caller can give the user an actionable reason. */
    sealed interface LinkResult {
        data class Ok(val link: String) : LinkResult
        /** Token missing/expired (401) -- caller re-logs in and retries. */
        data object Unauthorized : LinkResult
        /** Daily download quota used up (406) -- [message] is OpenSubtitles' own wording when present. */
        data class Quota(val message: String) : LinkResult
        /** Too many requests (429) -- back off and retry shortly. */
        data object RateLimited : LinkResult
        data class Error(val message: String) : LinkResult
    }

    /**
     * `POST /download` -- turns a [fileId] into a one-time subtitle download URL, spending one unit of
     * the account's daily quota. Distinguishes expired-token (401), quota-exhausted (406) and
     * rate-limit (429) so the caller can tell the user exactly what to do.
     */
    suspend fun requestDownloadLink(credential: String, bearer: String, fileId: Long): LinkResult = withContext(Dispatchers.IO) {
        val payload = JSONObject().put("file_id", fileId).toString()
        val request = Request.Builder()
            .url("$BASE/download")
            .commonHeaders(credential)
            .header("Authorization", "Bearer $bearer")
            .post(payload.toRequestBody(jsonType))
            .build()
        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                val msg = runCatching { JSONObject(body).optString("message") }.getOrNull()?.takeIf { it.isNotBlank() }
                when {
                    response.code == 401 -> LinkResult.Unauthorized
                    response.code == 406 -> LinkResult.Quota(msg ?: "You've used your daily download quota. Try again in ~24 hours.")
                    response.code == 429 -> LinkResult.RateLimited
                    !response.isSuccessful -> LinkResult.Error(msg ?: "OpenSubtitles error (HTTP ${response.code}).")
                    else -> {
                        val link = runCatching { JSONObject(body).optString("link") }.getOrNull()?.takeIf { it.isNotBlank() }
                        if (link != null) LinkResult.Ok(link) else LinkResult.Error("OpenSubtitles returned no download link.")
                    }
                }
            }
        } catch (e: Exception) {
            LinkResult.Error("Couldn't reach OpenSubtitles -- check your connection.")
        }
    }

    /** Downloads the raw subtitle bytes from a `/download` link. */
    suspend fun fetchBytes(url: String): ByteArray? = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).header("User-Agent", userAgent).build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                response.body?.bytes()
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun enc(s: String): String = java.net.URLEncoder.encode(s, "UTF-8")
}
