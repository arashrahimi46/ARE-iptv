package com.arashrahimi46.iptv.ui.player

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer

/** Whether an in-app live mini-player is currently docked in the corner. */
enum class LiveMiniMode { Closed, Mini }

/** The live channel behind a docked mini-player -- enough to re-enter its fullscreen player
 * (by [channelId]) and to adopt its still-running [ExoPlayer] back (matched on [streamUrl]). */
data class LiveMiniSession(
    val channelId: Long,
    val streamUrl: String,
    val title: String,
    val subtitle: String?,
)

/**
 * Owns the live [ExoPlayer] ONLY while it's docked as the in-app corner mini-player, so the same
 * decoder/instance keeps playing across a fullscreen -> minimize -> browse -> expand round trip
 * with zero re-buffer (the user's "seamless" choice). Ownership is single-writer and transfers
 * explicitly:
 *  - [minimize]: the fullscreen player hands its live [ExoPlayer] over; this becomes the owner and
 *    the mini overlay renders from it.
 *  - [takeIfAdopting]: the fullscreen player, on re-entry for the SAME stream, takes the instance
 *    back (ownership returns to the screen, which releases it on teardown as usual).
 *  - [closeMini]: releases the docked player (Stop & close, or a fullscreen entry for a DIFFERENT
 *    channel / any VOD).
 *
 * A single instance lives on the Activity and is provided via [LocalLivePlaybackController].
 */
class LivePlaybackController {
    var mode by mutableStateOf(LiveMiniMode.Closed)
        private set
    var session by mutableStateOf<LiveMiniSession?>(null)
        private set

    /** The docked player, or null when nothing is minimized. Not Compose state -- the overlay
     * reacts to [mode]; this is read imperatively when binding the surface. */
    var player: ExoPlayer? = null
        private set

    // Mirror of the docked player's live state, for the mini overlay's chrome.
    var isPlaying by mutableStateOf(false)
        private set
    var videoWidth by mutableStateOf(16)
        private set
    var videoHeight by mutableStateOf(9)
        private set

    private var listener: Player.Listener? = null

    /** Adopt [player] (already prepared and playing) as the docked mini-player. */
    fun minimize(player: ExoPlayer, session: LiveMiniSession) {
        // Defensive: never leak a previously docked instance.
        if (this.player != null && this.player !== player) closeMini()
        this.player = player
        this.session = session
        player.videoSize.let { if (it.width > 0 && it.height > 0) { videoWidth = it.width; videoHeight = it.height } }
        isPlaying = player.isPlaying
        val l = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
            override fun onVideoSizeChanged(size: VideoSize) {
                if (size.width > 0 && size.height > 0) { videoWidth = size.width; videoHeight = size.height }
            }
        }
        player.addListener(l)
        listener = l
        mode = LiveMiniMode.Mini
    }

    /** Hand the docked player back to the fullscreen screen when it re-opens the SAME stream --
     * ownership returns to the caller (which must release it). Null when there's nothing to adopt
     * or the stream differs. */
    fun takeIfAdopting(streamUrl: String): ExoPlayer? {
        val p = player
        if (mode == LiveMiniMode.Mini && p != null && session?.streamUrl == streamUrl) {
            listener?.let { p.removeListener(it) }
            listener = null
            player = null
            session = null
            mode = LiveMiniMode.Closed
            return p
        }
        return null
    }

    /** Release and clear the docked player (Stop & close, or superseded by another fullscreen open). */
    fun closeMini() {
        val p = player
        listener?.let { p?.removeListener(it) }
        listener = null
        p?.release()
        player = null
        session = null
        mode = LiveMiniMode.Closed
    }

    fun togglePlay() {
        player?.let { if (it.isPlaying) it.pause() else it.play() }
    }
}

/** Non-null once the Activity provides the shared controller; the mini-player is unavailable when null. */
val LocalLivePlaybackController = staticCompositionLocalOf<LivePlaybackController?> { null }
