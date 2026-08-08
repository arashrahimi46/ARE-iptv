package com.arashrahimi46.iptv.ui.player

/**
 * Shared exponential-backoff policy for stream-health-driven auto retry (P0.1),
 * used by both [LivePlayerScreen] (single player) and MultiViewScreen's per-pane
 * players -- same "how many attempts, how long between them" rule in one place
 * rather than two independently-tuned copies.
 */
object StreamRetryPolicy {
    /** Auto-retries attempted before giving up on this source and falling back
     * to the next available source for the channel. */
    const val MAX_RETRIES = 3

    /**
     * HTTP statuses that mean "this line is already at its concurrent-connection cap" rather than
     * "try again". Xtream panels answer **458** for it; proxies in front of them sometimes use 509.
     *
     * These are the one failure class retry makes WORSE. The slot does not exist until something
     * else stops streaming, so no amount of backoff conjures it -- and each attempt the server
     * counts before rejecting keeps the line full, so the retry loop sustains the error it is
     * trying to recover from. Both players treat these as give-up-immediately, which also skips the
     * alternate-source fallback (on Xtream that is usually another URL on the same capped line).
     */
    val CONNECTION_LIMIT_STATUSES = setOf(458, 509)

    /** Sustained buffering (no error, but stuck) this long counts as degraded
     * health and enters the same retry/backoff cycle as a hard playback error. */
    const val BUFFERING_GRACE_MS = 8_000L

    /**
     * [BUFFERING_GRACE_MS] scaled for N players starting at once (multiview).
     *
     * A flat 8s is fine for one player but is *shorter than four concurrent cold starts* on a weak
     * box, so every pane tripped the grace, bumped its retry counter, and had its ExoPlayer released
     * and rebuilt -- a release/allocate/decoder-acquire storm at the precise moment the device was
     * most loaded, which then reset the clock and could repeat up to [MAX_RETRIES] times per source
     * before falling back and starting over. The recovery mechanism was manufacturing the failure it
     * was meant to recover from. Scaling by pane count lets four cold starts finish.
     */
    fun bufferingGraceMillis(concurrentPlayers: Int): Long =
        BUFFERING_GRACE_MS * concurrentPlayers.coerceAtLeast(1)

    /** attempt 0 -> 1s, 1 -> 2s, 2 -> 4s -- the only values ever requested given
     * [MAX_RETRIES] = 3 (attempt is always < MAX_RETRIES when this is called). Coerced
     * defensively in case MAX_RETRIES grows without this comment being updated. */
    fun backoffMillis(attempt: Int): Long = 1_000L shl attempt.coerceIn(0, MAX_RETRIES - 1)
}
