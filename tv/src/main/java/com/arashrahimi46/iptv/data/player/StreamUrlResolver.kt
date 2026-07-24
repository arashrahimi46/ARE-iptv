package com.arashrahimi46.iptv.data.player

import com.arashrahimi46.iptv.data.model.PlaylistSource
import com.arashrahimi46.iptv.data.model.SourceType
import com.arashrahimi46.iptv.data.parser.StalkerClient
import com.arashrahimi46.iptv.data.settings.CredentialsStore

/** Which catalog a to-be-resolved item belongs to — selects the Stalker `create_link` endpoint. */
enum class StreamKind { LIVE, VOD, SERIES }

/** Thrown when a playable URL can't be produced (no stored URL, or the portal couldn't mint one). */
class StreamResolveException(message: String, cause: Throwable? = null) : Exception(message, cause)

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
     * @return a ready-to-play URL. Throws [StreamResolveException] when none can be produced.
     */
    suspend fun resolve(
        source: PlaylistSource,
        kind: StreamKind,
        externalId: String?,
        storedUrl: String?,
        series: Int? = null,
    ): String
}

class DefaultStreamUrlResolver(private val credentials: CredentialsStore) : StreamUrlResolver {
    override suspend fun resolve(
        source: PlaylistSource,
        kind: StreamKind,
        externalId: String?,
        storedUrl: String?,
        series: Int?,
    ): String {
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
        return try {
            StalkerClient(source.url, mac).createLink(cmd, stalkerType, series)
        } catch (e: com.arashrahimi46.iptv.data.parser.StalkerException) {
            // Surface a clear "can't start" rather than a silent null/hang (design §5 accepted
            // consequence: Stalker playback needs the portal reachable at press-time).
            throw StreamResolveException(e.message ?: "The portal couldn't start this stream", e)
        }
    }
}
