package com.arashrahimi46.iptv.data.player

import com.arashrahimi46.iptv.data.model.PlaylistSource
import com.arashrahimi46.iptv.data.model.SourceType
import com.arashrahimi46.iptv.data.parser.StalkerClient
import com.arashrahimi46.iptv.data.parser.XtreamAccountInfo
import com.arashrahimi46.iptv.data.parser.XtreamClient
import com.arashrahimi46.iptv.data.settings.CredentialsStore

/** Which catalog a to-be-resolved item belongs to — selects the Stalker `create_link` endpoint. */
enum class StreamKind { LIVE, VOD, SERIES }

/** Thrown when a playable URL can't be produced (no stored URL, or the portal couldn't mint one). */
class StreamResolveException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * A request to mint a **catch-up / archive** URL for an already-aired programme (docs/catchup-v1-design.md).
 * The raw material everywhere is the programme window ([startMs]..[endMs], epoch ms); [m3uSource]/[m3uType]
 * carry the M3U `catchup-source` template + convention (null for Xtream/Stalker, which compute archive
 * URLs without a template). Passed to [StreamUrlResolver.resolve]; null means an ordinary live/VOD play.
 */
data class CatchupRequest(
    val startMs: Long,
    val endMs: Long,
    val m3uSource: String? = null,
    val m3uType: String? = null,
)

/**
 * The one seam between "we picked what to play" and "we build the ExoPlayer MediaItem". For M3U and
 * Xtream it is pure identity — the real stream URL was computed at import and already lives on the
 * row, so we hand it straight back with zero behaviour change. For **Stalker** there is no static
 * URL: the portal mints a short-lived, session-scoped URL only at play time via `create_link`, so
 * this is where that call happens (resolve-on-play). Keeping it behind one interface stops Stalker's
 * session model from leaking into every playback site. See `docs/stalker-portal-v1-design.md` §5.
 */
interface StreamUrlResolver {
    /**
     * @param source the owning playlist source (its [SourceType] selects identity vs. resolve-on-play).
     * @param kind live/VOD/series — picks the Stalker endpoint (`itv` vs `vod`).
     * @param externalId the row's `externalId`: the portal `cmd` for Stalker, ignored otherwise.
     * @param storedUrl the row's precomputed `streamUrl` (the answer for M3U/Xtream).
     * @param series episode number for a Stalker series episode (sent as `&series=`), else null.
     * @param catchup when non-null, mint a catch-up/archive URL for that programme window instead of the
     *   ordinary live/VOD URL (docs/catchup-v1-design.md); per-source construction, still press-time.
     * @return a ready-to-play URL. Throws [StreamResolveException] when none can be produced.
     */
    suspend fun resolve(
        source: PlaylistSource,
        kind: StreamKind,
        externalId: String?,
        storedUrl: String?,
        series: Int? = null,
        catchup: CatchupRequest? = null,
    ): String
}

class DefaultStreamUrlResolver(private val credentials: CredentialsStore) : StreamUrlResolver {
    override suspend fun resolve(
        source: PlaylistSource,
        kind: StreamKind,
        externalId: String?,
        storedUrl: String?,
        series: Int?,
        catchup: CatchupRequest?,
    ): String {
        // Catch-up / archive: mint a provider-specific archive URL from the programme window, before
        // the ordinary live/identity paths (docs/catchup-v1-design.md).
        if (catchup != null) return resolveCatchup(source, externalId, storedUrl, catchup)
        if (source.type != SourceType.STALKER) {
            // Identity pass-through: M3U/Xtream resolved everything at import.
            return storedUrl?.takeIf { it.isNotBlank() }
                ?: throw StreamResolveException("This item has no playable stream")
        }
        // Stalker: mint a fresh session URL from the stored portal cmd.
        val cmd = externalId?.takeIf { it.isNotBlank() }
            ?: throw StreamResolveException("This item has no portal command to play")
        val mac = credentials.mac(source.id)
            ?: throw StreamResolveException("Saved MAC for this portal is missing — re-add it to play")
        val stalkerType = if (kind == StreamKind.LIVE) "itv" else "vod"
        // Reuse the API endpoint discovered at import so a play press doesn't re-probe path variants.
        val cachedEndpoint = com.arashrahimi46.iptv.data.parser.StalkerAccountInfo
            .fromJson(source.accountInfoJson)?.endpoint
        return try {
            StalkerClient(source.url, mac, cachedEndpoint = cachedEndpoint).createLink(cmd, stalkerType, series)
        } catch (e: com.arashrahimi46.iptv.data.parser.StalkerException) {
            // Surface a clear "can't start" rather than a silent null/hang (design §5 accepted
            // consequence: Stalker playback needs the portal reachable at press-time).
            throw StreamResolveException(e.message ?: "The portal couldn't start this stream", e)
        }
    }

    /**
     * Mint a catch-up/archive URL for [req]'s programme window, per source type (docs/catchup-v1-design.md).
     * Xtream is wired here (Phase 1); Stalker and M3U throw a clear "not available yet" until their phases
     * land, so an accidental call degrades to the D9 message rather than a broken stream.
     */
    private fun resolveCatchup(
        source: PlaylistSource,
        externalId: String?,
        storedUrl: String?,
        req: CatchupRequest,
    ): String {
        val durationMin = ((req.endMs - req.startMs) / 60_000L).coerceAtLeast(1).toInt()
        return when (source.type) {
            SourceType.XTREAM -> {
                val streamId = externalId?.takeIf { it.isNotBlank() }
                    ?: throw StreamResolveException("This channel has no archive stream id")
                val (user, pass) = credentials.forSource(source.id)
                if (user.isNullOrBlank() || pass.isNullOrBlank()) {
                    throw StreamResolveException("Saved login for this provider is missing — re-add it to catch up")
                }
                // Format the start in the PROVIDER's timezone (from the captured account info), falling
                // back to UTC when unknown — the #1 correctness trap (see spec resilience notes).
                val tz = XtreamAccountInfo.fromJson(source.accountInfoJson)?.timezone
                val start = xtreamCatchupStart(req.startMs, tz)
                XtreamClient(source.url, user, pass).timeshiftUrl(streamId, durationMin, start, xtreamCatchupExt(storedUrl))
            }
            SourceType.STALKER ->
                throw StreamResolveException("Catch-up isn't available for this portal yet")
            SourceType.M3U -> {
                // Expand the channel's catchup-source template (or derive from the live URL) for the
                // programme window -- pure, per catchup-type. No stream to rewrite ⇒ D9 message.
                val live = storedUrl?.takeIf { it.isNotBlank() }
                    ?: throw StreamResolveException("This channel has no stream to catch up")
                com.arashrahimi46.iptv.data.parser.expandCatchup(
                    liveUrl = live,
                    source = req.m3uSource,
                    type = req.m3uType,
                    startMs = req.startMs,
                    endMs = req.endMs,
                    nowMs = System.currentTimeMillis(),
                )
            }
        }
    }
}

/** Epoch-ms → Xtream's `yyyy-MM-dd:HH-mm`, in [providerTz] when known, else UTC (deterministic default).
 *  Top-level + internal so the timezone formatting — the #1 catch-up correctness trap — is unit-testable
 *  without a Room/credentials harness. */
internal fun xtreamCatchupStart(startMs: Long, providerTz: String?): String {
    val zone = if (!providerTz.isNullOrBlank()) java.util.TimeZone.getTimeZone(providerTz)
    else java.util.TimeZone.getTimeZone("UTC")
    return java.text.SimpleDateFormat("yyyy-MM-dd:HH-mm", java.util.Locale.US)
        .apply { timeZone = zone }
        .format(java.util.Date(startMs))
}

/** Reuse the live stream's file extension for the archive URL (Xtream live is `.m3u8`); default m3u8. */
internal fun xtreamCatchupExt(storedUrl: String?): String {
    val ext = storedUrl?.substringBefore('?')?.substringAfterLast('/', "")?.substringAfterLast('.', "").orEmpty()
    return ext.takeIf { it.isNotBlank() && it.length <= 5 } ?: "m3u8"
}
