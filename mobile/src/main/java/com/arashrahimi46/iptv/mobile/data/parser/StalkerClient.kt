package com.arashrahimi46.iptv.mobile.data.parser

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.ConnectException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException

/**
 * Account/server metadata a Stalker portal returns from `type=account_info&action=get_main_info`
 * (the `js` block). Mirrors [XtreamAccountInfo]'s role — display-only, persisted as a JSON blob on
 * the source row rather than typed columns. Every field is nullable because portals populate them
 * inconsistently; [expiresRaw] is kept as the portal's own string (e.g. "October 21, 2025") because
 * Stalker reports expiry as free-form text, not a unix timestamp.
 */
data class StalkerAccountInfo(
    val mac: String? = null,
    val status: String? = null,
    val expiresRaw: String? = null,
    val phone: String? = null,
    val host: String? = null,
    /**
     * The full API endpoint (`host` + the path variant that answered — `/portal.php` etc.) that
     * [StalkerClient.resolveEndpoint] settled on at import. Persisted so resolve-on-play can skip
     * re-probing all path variants on every press (design §10, "cache the winner in accountInfoJson").
     */
    val endpoint: String? = null,
) {
    fun toJson(): String = JSONObject().apply {
        put("mac", mac ?: JSONObject.NULL)
        put("status", status ?: JSONObject.NULL)
        put("expiresRaw", expiresRaw ?: JSONObject.NULL)
        put("phone", phone ?: JSONObject.NULL)
        put("host", host ?: JSONObject.NULL)
        put("endpoint", endpoint ?: JSONObject.NULL)
    }.toString()

    companion object {
        fun fromJson(raw: String?): StalkerAccountInfo? {
            if (raw.isNullOrBlank()) return null
            val json = runCatching { JSONObject(raw) }.getOrNull() ?: return null
            return StalkerAccountInfo(
                mac = json.stalkerString("mac"),
                status = json.stalkerString("status"),
                expiresRaw = json.stalkerString("expiresRaw"),
                phone = json.stalkerString("phone"),
                host = json.stalkerString("host"),
                endpoint = json.stalkerString("endpoint"),
            )
        }
    }
}

/**
 * Thrown for unreachable portals, HTTP errors, or a body that isn't the `{"js": ...}` shape a
 * Stalker portal returns. [isAuthError] is set when the portal was reached but the MAC/identity was
 * rejected (no handshake credential, or a 401/403-shaped response) — an authoritative failure the
 * caller must surface rather than silently retrying forever.
 */
class StalkerException(message: String, cause: Throwable? = null, val isAuthError: Boolean = false) : Exception(message, cause)

// --- Catalog model (mirrors XtreamClient's Xtream* data classes). The play command [cmd] is the
// Stalker equivalent of a stream URL, except it is NOT playable directly: it is passed to
// `create_link` at press-time to mint a short-lived URL (Phase 3). We persist it in the row's
// `externalId`, never in `streamUrl`. Every field is nullable/defaulted because portals populate
// them inconsistently. ---

/** A live genre or VOD/series category (`get_genres` / `get_categories`). */
data class StalkerCategory(val id: String, val title: String)

/** A live channel from `itv&get_all_channels`. [categoryId] is the portal's `tv_genre_id`. */
data class StalkerChannel(
    val id: String,
    val name: String,
    val number: String?,
    val cmd: String,
    val logo: String?,
    val xmltvId: String?,
    val categoryId: String?,
    /** Archive window in days (docs/catchup-v1-design.md, Phase 5): >0 means the portal holds a
     *  catch-up archive for this channel (drives the Guide glyph); 0 = none. */
    val archiveDays: Int = 0,
)

/** A movie from `vod&get_ordered_list`. */
data class StalkerVod(
    val id: String,
    val name: String,
    val categoryId: String?,
    val cover: String?,
    val cmd: String,
    val year: String?,
)

/** A series parent from `series&get_ordered_list`; episodes are drilled down via [StalkerClient.getSeriesEpisodes]. */
data class StalkerSeries(val id: String, val name: String, val categoryId: String?, val cover: String?)

/**
 * One episode of a Stalker series. Stalker groups episodes under a season row that carries a single
 * [cmd] plus a `series` array of episode numbers; the episode number is later handed to `create_link`
 * as `&series=<n>`. So all episodes of a season share one [cmd] and differ by [episode].
 */
data class StalkerEpisode(val season: Int, val episode: Int, val name: String, val cmd: String)

/**
 * Minimal Stalker Portal (Ministra) client — Phase 1 covers the stateful session only: the
 * [handshake] → [getProfile] → account-info dance behind a single [authenticate] entry point, plus
 * transparent re-auth when the short-lived session credential expires. Catalog fetching and
 * `create_link` playback-URL resolution land in later phases (see `docs/stalker-portal-v1-design.md`).
 *
 * Structurally unlike [XtreamClient]: Xtream carries `?username=&password=` on every stateless call,
 * whereas Stalker requires a handshake that mints a bearer credential, then rides that as a header
 * plus the MAC as a cookie. The API path also varies per portal, so [resolveEndpoint] probes the
 * known variants once and caches the winner.
 */
class StalkerClient(
    portalUrl: String,
    mac: String,
    /**
     * A previously-resolved endpoint (from [StalkerAccountInfo.endpoint]) to reuse instead of
     * probing the path variants again. Resolve-on-play passes this so a play press doesn't spend up
     * to three round-trips rediscovering the portal's API path. A stale one self-heals: if the first
     * handshake against it fails, [ensureSession] falls back to a full [resolveEndpoint] probe once.
     */
    private val cachedEndpoint: String? = null,
    private val client: OkHttpClient = defaultClient,
) {
    private val base: String = run {
        val normalized = if (portalUrl.startsWith("http://") || portalUrl.startsWith("https://")) portalUrl else "http://$portalUrl"
        normalized.trimEnd('/')
    }
    private val identity = StalkerIdentity.fromMac(mac)

    /** Resolved by [resolveEndpoint] (or seeded from [cachedEndpoint]); the full URL up to `?` that this portal answers on. */
    private var endpoint: String? = cachedEndpoint?.takeIf { it.isNotBlank() }

    /** The short-lived bearer credential minted by [handshake] and sent on every later call. */
    private var bearer: String? = null

    /**
     * Reach the portal, authorize this MAC, and return its account metadata. Runs the full Stalker
     * session dance: probe the endpoint, [handshake] for a session credential, [getProfile] to
     * register the device, then read account info. Throws [StalkerException] (with
     * [StalkerException.isAuthError] set) when the portal rejects the MAC/identity.
     */
    suspend fun authenticate(): StalkerAccountInfo = withContext(Dispatchers.IO) {
        resolveEndpoint()
        handshake()
        getProfile()
        readAccountInfo()
    }

    /** `type=stb&action=handshake` — mints the bearer credential every later call carries. */
    private suspend fun handshake() {
        val js = call("stb", "handshake", withBearer = false)
        bearer = js.stalkerString("token")
            ?: throw StalkerException("Portal did not open a session — check the portal URL and MAC", isAuthError = true)
    }

    /** `type=stb&action=get_profile` — registers the derived device identity for this session. */
    private suspend fun getProfile() {
        // Portals that don't cross-check the derived fields simply ignore them; the MAC cookie is
        // what most gate on. We send the full set so stricter portals authorize too.
        call(
            "stb", "get_profile",
            extra = buildString {
                append("&sn=").append(identity.serialNumber)
                append("&device_id=").append(identity.deviceId)
                append("&device_id2=").append(identity.deviceId2)
                append("&signature=").append(identity.signature)
                append("&stb_type=MAG250&hd=1&auth_second_step=0&not_valid_token=0")
            },
        )
    }

    /** `type=account_info&action=get_main_info` — the display-only expiry/phone/status block. */
    private suspend fun readAccountInfo(): StalkerAccountInfo {
        val js = runCatching { call("account_info", "get_main_info") }.getOrNull()
        return StalkerAccountInfo(
            mac = identity.mac,
            status = js?.stalkerString("status") ?: js?.stalkerString("tariff_plan"),
            expiresRaw = js?.stalkerString("end_date"),
            phone = js?.stalkerString("phone"),
            host = base,
            endpoint = endpoint,
        )
    }

    // ---------------------------------------------------------------------------------------------
    // Catalog (Phase 2). Live genres + channels, VOD, series; the play command lands in `externalId`,
    // never `streamUrl` (there is no static URL — Phase 3's resolver mints one via `create_link`).
    // Genre/category endpoints answer with a bare `js` array; the `get_ordered_list` endpoints answer
    // with a paged `js: { total_items, data: [...] }` object, looped by [pagedData].
    // ---------------------------------------------------------------------------------------------

    /** `type=itv&action=get_genres` — the live-TV genres (the Stalker analogue of Xtream live categories). */
    suspend fun getLiveCategories(): List<StalkerCategory> = withContext(Dispatchers.IO) {
        parseStalkerCategories(callArray("itv", "get_genres"))
    }

    /** `type=vod&action=get_categories`. */
    suspend fun getVodCategories(): List<StalkerCategory> = withContext(Dispatchers.IO) {
        parseStalkerCategories(callArray("vod", "get_categories"))
    }

    /** `type=series&action=get_categories`. */
    suspend fun getSeriesCategories(): List<StalkerCategory> = withContext(Dispatchers.IO) {
        parseStalkerCategories(callArray("series", "get_categories"))
    }

    /** `type=itv&action=get_all_channels` — every live channel in one (unpaged) call, each carrying its `cmd`. */
    suspend fun getLiveChannels(): List<StalkerChannel> = withContext(Dispatchers.IO) {
        val js = call("itv", "get_all_channels")
        parseStalkerChannels(js.optJSONArray("data") ?: org.json.JSONArray())
    }

    /** `type=vod&action=get_ordered_list` across all categories, paged. */
    suspend fun getVod(): List<StalkerVod> = withContext(Dispatchers.IO) {
        parseStalkerVod(pagedData("vod", "get_ordered_list", "&category=*&sortby=added"))
    }

    /** `type=series&action=get_ordered_list` across all categories, paged. */
    suspend fun getSeries(): List<StalkerSeries> = withContext(Dispatchers.IO) {
        parseStalkerSeries(pagedData("series", "get_ordered_list", "&category=*&sortby=added"))
    }

    /**
     * Season/episode drill-down for one series (`type=series&action=get_ordered_list&movie_id=<id>`).
     * Stalker returns a row per season carrying a single `cmd` and a `series` array of episode numbers;
     * [parseStalkerEpisodes] expands those into per-episode entries that share the season's `cmd`.
     */
    suspend fun getSeriesEpisodes(seriesId: String): List<StalkerEpisode> = withContext(Dispatchers.IO) {
        // Unlike the import-time catalog calls (which run right after authenticate()), this drill-down
        // is invoked on a fresh client when the user opens a series, so bring the session up first.
        ensureSession()
        parseStalkerEpisodes(pagedData("series", "get_ordered_list", "&movie_id=$seriesId"))
    }

    /**
     * Resolve-on-play (Phase 3): `type=<itv|vod>&action=create_link&cmd=<cmd>` mints a short-lived,
     * session-scoped URL for [cmd] (a live channel's or VOD item's play command). The portal answers
     * with a launcher command like `ffmpeg http://…` or `auto http://…`; [stripStalkerCmdPrefix]
     * reduces it to the bare URL ExoPlayer needs. [series] is the episode number for a Stalker series
     * (sent as `&series=`, since all episodes of a season share the season's `cmd`). Ensures the
     * session first, so callers can resolve without a separate [authenticate] step.
     *
     * Catch-up (Phase 5): when [startEpochSec] is supplied, `&start=<epochSeconds>` is appended so the
     * portal mints an **archive** link for the programme that aired at that time (Ministra convention).
     */
    suspend fun createLink(cmd: String, type: String, series: Int? = null, startEpochSec: Long? = null): String = withContext(Dispatchers.IO) {
        ensureSession()
        val js = call(type, "create_link", stalkerCreateLinkExtra(cmd, series, startEpochSec))
        val raw = js.stalkerString("cmd") ?: js.stalkerString("url")
        if (raw.isNullOrBlank()) {
            // A reachable portal that mints no URL is almost always the connection cap (MAC tied to
            // one active stream) or a portal-side error string — surface it clearly, not as a hang.
            val portalError = js.stalkerString("error") ?: js.stalkerString("msg")
            throw StalkerException(
                portalError
                    ?: "The portal didn't return a stream — it may be at its connection limit. Close other streams and try again.",
            )
        }
        stripStalkerCmdPrefix(raw)
    }

    /**
     * Bring the session up if it isn't already (endpoint probed + handshake + get_profile), so a bare
     * [createLink] works without the caller having run [authenticate] first. If a [cachedEndpoint] was
     * seeded but its handshake fails (the portal moved its API path since import), fall back to a full
     * [resolveEndpoint] probe once before giving up — a stale cache self-heals rather than dead-ends.
     */
    private suspend fun ensureSession() {
        if (endpoint == null) resolveEndpoint()
        if (bearer == null) {
            try {
                handshake()
            } catch (e: StalkerException) {
                if (cachedEndpoint != null && endpoint == cachedEndpoint) {
                    endpoint = null
                    resolveEndpoint()
                    handshake()
                } else throw e
            }
            getProfile()
        }
    }

    /**
     * One authenticated portal call, returning its `js` payload as a [JSONObject]. Injects the MAG
     * headers + MAC cookie + bearer credential, and on an expired-session response ([isSessionExpired])
     * transparently re-handshakes once and retries — Stalker sessions are short-lived, so a single
     * silent refresh keeps a long import from dying mid-way.
     */
    private suspend fun call(type: String, action: String, extra: String = "", withBearer: Boolean = true, allowReauth: Boolean = true): JSONObject {
        val root = requestRoot(type, action, extra, withBearer, allowReauth)
        val js = root.optJSONObject("js")
        if (js == null && isSessionExpired(root) && allowReauth) {
            handshake()
            return call(type, action, extra, withBearer, allowReauth = false)
        }
        return js ?: throw StalkerException("Unexpected response from portal for action=$action")
    }

    /**
     * Variant of [call] for the genre/category listings, whose `js` is a bare array rather than an
     * object. Tolerates the portals that instead wrap the list as `js: { data: [...] }`.
     */
    private suspend fun callArray(type: String, action: String, extra: String = ""): org.json.JSONArray {
        val root = requestRoot(type, action, extra, withBearer = true, allowReauth = true)
        root.optJSONArray("js")?.let { return it }
        root.optJSONObject("js")?.optJSONArray("data")?.let { return it }
        return org.json.JSONArray()
    }

    /**
     * Walks a paged `get_ordered_list` endpoint (`&p=1,2,…`), accumulating every `js.data` row until
     * a page comes back empty or the reported `total_items` is reached. [MAX_PAGES] caps a misbehaving
     * portal that never signals the end, so an import can't spin forever.
     */
    private suspend fun pagedData(type: String, action: String, extra: String): List<JSONObject> {
        val out = ArrayList<JSONObject>()
        var page = 1
        while (page <= MAX_PAGES) {
            val js = call(type, action, "$extra&p=$page")
            val data = js.optJSONArray("data") ?: break
            if (data.length() == 0) break
            for (i in 0 until data.length()) data.optJSONObject(i)?.let(out::add)
            val total = js.stalkerString("total_items")?.toIntOrNull()
            if (total != null && out.size >= total) break
            page++
        }
        return out
    }

    /** HTTP + auth-retry core shared by [call]/[callArray]/[pagedData]: returns the `{"js": …}` wrapper. */
    private suspend fun requestRoot(type: String, action: String, extra: String, withBearer: Boolean, allowReauth: Boolean): JSONObject {
        val resolved = endpoint ?: throw StalkerException("Portal endpoint not resolved")
        val url = "$resolved?type=$type&action=$action$extra&JsHttpRequest=1-xml"
        val body = try {
            client.newCall(request(url, withBearer)).execute().use { response ->
                if (response.code == 401 || response.code == 403) {
                    if (allowReauth) { handshake(); return requestRoot(type, action, extra, withBearer, allowReauth = false) }
                    throw StalkerException("Portal rejected the MAC address", isAuthError = true)
                }
                if (!response.isSuccessful) throw StalkerException("Portal returned an error (HTTP ${response.code})")
                response.body?.string() ?: throw StalkerException("Empty response from portal for action=$action")
            }
        } catch (e: StalkerException) {
            throw e
        } catch (e: Exception) {
            throw StalkerException(networkErrorMessage(e), e)
        }
        return runCatching { JSONObject(body) }.getOrElse {
            throw StalkerException("Unexpected response from portal for action=$action")
        }
    }

    private fun request(url: String, withBearer: Boolean): Request {
        val builder = Request.Builder().url(url)
            .header("User-Agent", MAG_USER_AGENT)
            .header("X-User-Agent", "Model: MAG250; Link: WiFi")
            .header("Referer", "$base/c/")
            .header("Cookie", "mac=${identity.mac}; stb_lang=en; timezone=Europe/London")
        if (withBearer) bearer?.let { builder.header("Authorization", "Bearer $it") }
        return builder.build()
    }

    /**
     * Probe the known Stalker API paths and keep the first that answers with a handshake-shaped body.
     * Portals split between `/portal.php`, `/stalker_portal/server/load.php`, and `/c/portal.php`;
     * guessing wrong is the most common "it doesn't work" cause, so we detect rather than assume.
     */
    private suspend fun resolveEndpoint() {
        if (endpoint != null) return
        var lastError: Exception? = null
        for (path in ENDPOINT_PATHS) {
            val candidate = "$base$path"
            val url = "$candidate?type=stb&action=handshake&JsHttpRequest=1-xml"
            try {
                client.newCall(request(url, withBearer = false)).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string().orEmpty()
                        if (extractJs(body) != null) { endpoint = candidate; return }
                    }
                }
            } catch (e: Exception) {
                lastError = e
            }
        }
        throw StalkerException(
            lastError?.let { networkErrorMessage(it) } ?: "Could not find a Stalker portal at this address",
            lastError,
        )
    }

    /** Stalker wraps every response as `{"js": ...}`; returns the `js` object, or null if absent/non-object. */
    private fun extractJs(body: String): JSONObject? {
        val json = runCatching { JSONObject(body) }.getOrNull() ?: return null
        return json.optJSONObject("js")
    }

    /** An expired/invalid session often comes back as `js: false`/empty rather than a 401. */
    private fun isSessionExpired(root: JSONObject): Boolean =
        root.has("js") && root.optJSONObject("js") == null && root.optJSONArray("js") == null

    private fun networkErrorMessage(e: Exception): String = when (e) {
        is UnknownHostException -> "Could not find the portal server — check the address and your connection"
        is java.net.SocketTimeoutException -> "The portal took too long to respond — check your connection and try again"
        is ConnectException -> "Could not connect to the portal — the server may be down"
        is SSLException -> "Secure connection to the portal failed — check the portal address"
        else -> "Could not reach the portal: ${e.message}"
    }

    companion object {
        private const val MAG_USER_AGENT =
            "Mozilla/5.0 (QtEmbedded; U; Linux; C) AppleWebKit/533.3 (KHTML, like Gecko) MAG200 stbapp ver: 2 rev: 250 Std: 1 Dual: 1"

        /** API paths to probe, most-common first. */
        private val ENDPOINT_PATHS = listOf("/portal.php", "/stalker_portal/server/load.php", "/c/portal.php")

        /** Hard cap on paged `get_ordered_list` walks so a portal that never reports the end can't
         *  loop forever (a large real catalog is a few hundred pages of ~14 items each). */
        private const val MAX_PAGES = 2000

        private val defaultClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }
}

private fun JSONObject.stalkerString(key: String): String? =
    if (has(key) && !isNull(key)) opt(key)?.toString()?.takeIf { it.isNotBlank() } else null

// --- Pure catalog parsers, split out of [StalkerClient] so the fragile portal-JSON mapping is unit-
// testable against fixtures without a live portal (mirrors the top-level M3uParser/parseSeriesEpisode
// pattern). Every mapper tolerates the key-name variants real Ministra/Stalker portals use. ---

/** `js` array of `{id, title}` genre/category objects. The synthetic "All" pseudo-genre (id "*"/"0") is dropped — the app renders its own "All". */
internal fun parseStalkerCategories(arr: org.json.JSONArray): List<StalkerCategory> =
    arr.mapStalkerObjects { obj ->
        val id = obj.stalkerString("id") ?: return@mapStalkerObjects null
        if (id == "*" || id == "0") return@mapStalkerObjects null
        StalkerCategory(id = id, title = obj.stalkerString("title") ?: obj.stalkerString("name") ?: id)
    }

/** `js.data` array from `get_all_channels`. A channel with no `cmd` is skipped — it can never be resolved to a URL. */
internal fun parseStalkerChannels(data: org.json.JSONArray): List<StalkerChannel> =
    data.mapStalkerObjects { obj ->
        val id = obj.stalkerString("id") ?: return@mapStalkerObjects null
        val cmd = obj.stalkerString("cmd") ?: return@mapStalkerObjects null
        StalkerChannel(
            id = id,
            name = obj.stalkerString("name") ?: obj.stalkerString("title") ?: "Unnamed",
            number = obj.stalkerString("number"),
            cmd = cmd,
            logo = obj.stalkerString("logo") ?: obj.stalkerString("screenshot_uri"),
            xmltvId = obj.stalkerString("xmltv_id"),
            categoryId = obj.stalkerString("tv_genre_id") ?: obj.stalkerString("genre_id"),
            archiveDays = stalkerArchiveDays(
                archiveFlag = obj.stalkerString("archive"),
                archiveRangeHours = obj.stalkerString("archive_range"),
                archiveDurationDays = obj.stalkerString("tv_archive_duration"),
            ),
        )
    }

/**
 * The catch-up window in days for a Ministra/Stalker channel row (docs/catchup-v1-design.md, Phase 5).
 * Portal builds vary, so prefer an explicit day count (`tv_archive_duration`), else derive from the
 * `archive` capability flag + `archive_range` (hours), else default a flagged channel to 7 days.
 * 0 means no archive. Pure + top-level so the (proprietary, un-probeable) field matrix is unit-testable.
 */
internal fun stalkerArchiveDays(archiveFlag: String?, archiveRangeHours: String?, archiveDurationDays: String?): Int {
    archiveDurationDays?.trim()?.toIntOrNull()?.let { if (it > 0) return it }
    val enabled = archiveFlag?.trim() == "1"
    if (!enabled) return 0
    archiveRangeHours?.trim()?.toIntOrNull()?.let { if (it > 0) return ((it + 23) / 24).coerceAtLeast(1) }
    return 7
}

/** The `create_link` query tail. `&start=<epochSeconds>` (when [startEpochSec] set) turns an ordinary
 *  live/vod link into an archive request (Ministra convention). Pure + top-level so the param shape —
 *  the least-standardized bit of Stalker catch-up — is unit-testable without a live portal. */
internal fun stalkerCreateLinkExtra(cmd: String, series: Int?, startEpochSec: Long?): String = buildString {
    append("&cmd=").append(java.net.URLEncoder.encode(cmd, "UTF-8"))
    if (series != null) append("&series=").append(series)
    if (startEpochSec != null) append("&start=").append(startEpochSec)
    append("&forced_storage=undefined&disable_ad=0")
}

/** Accumulated `js.data` rows from paged `vod&get_ordered_list`. Falls back to the item id for `cmd` when the portal omits it. */
internal fun parseStalkerVod(items: List<JSONObject>): List<StalkerVod> =
    items.mapNotNull { obj ->
        val id = obj.stalkerString("id") ?: return@mapNotNull null
        StalkerVod(
            id = id,
            name = obj.stalkerString("name") ?: obj.stalkerString("o_name") ?: "Unnamed",
            categoryId = obj.stalkerString("category_id"),
            cover = obj.stalkerString("screenshot_uri") ?: obj.stalkerString("cover") ?: obj.stalkerString("pic"),
            cmd = obj.stalkerString("cmd") ?: id,
            year = obj.stalkerString("year"),
        )
    }

/** Accumulated `js.data` rows from paged `series&get_ordered_list`. */
internal fun parseStalkerSeries(items: List<JSONObject>): List<StalkerSeries> =
    items.mapNotNull { obj ->
        val id = obj.stalkerString("id") ?: return@mapNotNull null
        StalkerSeries(
            id = id,
            name = obj.stalkerString("name") ?: obj.stalkerString("o_name") ?: "Unnamed",
            categoryId = obj.stalkerString("category_id"),
            cover = obj.stalkerString("screenshot_uri") ?: obj.stalkerString("cover") ?: obj.stalkerString("pic"),
        )
    }

/**
 * Expands the per-season drill-down rows into per-episode entries. Each season row carries one `cmd`
 * and a `series` array of episode numbers (e.g. `"series":[1,2,3]`); every episode of that season
 * reuses the season `cmd` and differs only by its number. A season row with no `series` array is
 * treated as a single standalone entry.
 */
internal fun parseStalkerEpisodes(seasons: List<JSONObject>): List<StalkerEpisode> {
    val out = ArrayList<StalkerEpisode>()
    for (s in seasons) {
        val cmd = s.stalkerString("cmd") ?: continue
        val seasonName = s.stalkerString("name") ?: s.stalkerString("o_name")
        val seasonNum = Regex("(?i)season\\s*(\\d+)").find(seasonName.orEmpty())?.groupValues?.get(1)?.toIntOrNull()
            ?: s.stalkerString("season_number")?.toIntOrNull()
            ?: 1
        val eps = s.optJSONArray("series")
        if (eps != null && eps.length() > 0) {
            for (i in 0 until eps.length()) {
                val epNum = eps.optInt(i, i + 1)
                out += StalkerEpisode(season = seasonNum, episode = epNum, name = "Episode $epNum", cmd = cmd)
            }
        } else {
            out += StalkerEpisode(season = seasonNum, episode = 1, name = seasonName ?: "Episode 1", cmd = cmd)
        }
    }
    return out
}

/**
 * Reduce a Stalker `create_link` result to the bare URL ExoPlayer can open. Portals prefix the URL
 * with a launcher token — `ffmpeg http://…`, `auto http://…` — or return the bare URL already; we
 * cut everything before the first `http`. A result with no `http` (unexpected) is returned trimmed
 * and left for the player to reject, rather than mangled here.
 */
internal fun stripStalkerCmdPrefix(raw: String): String {
    val trimmed = raw.trim()
    val idx = trimmed.indexOf("http")
    return if (idx > 0) trimmed.substring(idx) else trimmed
}

private inline fun <T> org.json.JSONArray.mapStalkerObjects(transform: (JSONObject) -> T?): List<T> {
    val out = ArrayList<T>(length())
    for (i in 0 until length()) {
        val obj = optJSONObject(i) ?: continue
        transform(obj)?.let { out += it }
    }
    return out
}
