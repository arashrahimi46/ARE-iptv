package com.arashrahimi46.iptv.data.parser

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
) {
    fun toJson(): String = JSONObject().apply {
        put("mac", mac ?: JSONObject.NULL)
        put("status", status ?: JSONObject.NULL)
        put("expiresRaw", expiresRaw ?: JSONObject.NULL)
        put("phone", phone ?: JSONObject.NULL)
        put("host", host ?: JSONObject.NULL)
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
    private val client: OkHttpClient = defaultClient,
) {
    private val base: String = run {
        val normalized = if (portalUrl.startsWith("http://") || portalUrl.startsWith("https://")) portalUrl else "http://$portalUrl"
        normalized.trimEnd('/')
    }
    private val identity = StalkerIdentity.fromMac(mac)

    /** Resolved once by [resolveEndpoint]; the full URL up to `?` that this portal answers on. */
    private var endpoint: String? = null

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
        )
    }

    /**
     * One authenticated portal call, returning its `js` payload as a [JSONObject]. Injects the MAG
     * headers + MAC cookie + bearer credential, and on an expired-session response ([isSessionExpired])
     * transparently re-handshakes once and retries — Stalker sessions are short-lived, so a single
     * silent refresh keeps a long import from dying mid-way.
     */
    private suspend fun call(type: String, action: String, extra: String = "", withBearer: Boolean = true, allowReauth: Boolean = true): JSONObject {
        val resolved = endpoint ?: throw StalkerException("Portal endpoint not resolved")
        val url = "$resolved?type=$type&action=$action$extra&JsHttpRequest=1-xml"
        val body = try {
            client.newCall(request(url, withBearer)).execute().use { response ->
                if (response.code == 401 || response.code == 403) {
                    if (allowReauth) { handshake(); return call(type, action, extra, withBearer, allowReauth = false) }
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
        val js = extractJs(body)
        if (js == null && isSessionExpired(body) && allowReauth) {
            handshake()
            return call(type, action, extra, withBearer, allowReauth = false)
        }
        return js ?: throw StalkerException("Unexpected response from portal for action=$action")
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
    private fun isSessionExpired(body: String): Boolean {
        val json = runCatching { JSONObject(body) }.getOrNull() ?: return false
        return json.has("js") && json.optJSONObject("js") == null
    }

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

        private val defaultClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }
}

private fun JSONObject.stalkerString(key: String): String? =
    if (has(key) && !isNull(key)) opt(key)?.toString()?.takeIf { it.isNotBlank() } else null
