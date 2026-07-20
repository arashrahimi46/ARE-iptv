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

    /** Sustained buffering (no error, but stuck) this long counts as degraded
     * health and enters the same retry/backoff cycle as a hard playback error. */
    const val BUFFERING_GRACE_MS = 8_000L

    /** attempt 0 -> 1s, 1 -> 2s, 2 -> 4s -- the only values ever requested given
     * [MAX_RETRIES] = 3 (attempt is always < MAX_RETRIES when this is called). Coerced
     * defensively in case MAX_RETRIES grows without this comment being updated. */
    fun backoffMillis(attempt: Int): Long = 1_000L shl attempt.coerceIn(0, MAX_RETRIES - 1)
}
