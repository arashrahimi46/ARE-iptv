package com.arashrahimi46.iptv.mobile.data.settings

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Process-scoped (never persisted) runtime unlock for the parental lock. The lock itself lives in
 * [UserSettings.isParentalLockEnabled]; this holds the *session* reveal a correct PIN grants, so
 * content re-hides on its own after [AutoRelock] (or on app restart, since nothing is stored).
 *
 * A session unlock intentionally does not survive process death: relaunching re-locks, which is the
 * whole point of a parental gate. The delayed relock runs on a private app-lifetime scope so a
 * single timer serves every screen -- collectors of [unlocked] recompute when it flips back to false.
 */
object ParentalGate {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var relockJob: Job? = null

    private val _unlocked = MutableStateFlow(false)
    /** True while adult content is revealed for the current session; false when locked. */
    val unlocked: StateFlow<Boolean> = _unlocked.asStateFlow()

    /**
     * Grant a session unlock lasting [autoRelock]. [AutoRelock.IMMEDIATELY] grants nothing lingering
     * (the grid stays locked; each blurred item must be opened with its own PIN), [AutoRelock.NEVER]
     * stays unlocked until the app restarts, and the timed values relock after their window.
     */
    fun unlock(autoRelock: AutoRelock) {
        relockJob?.cancel()
        if (autoRelock == AutoRelock.IMMEDIATELY) {
            _unlocked.value = false
            return
        }
        _unlocked.value = true
        if (autoRelock.durationMs != Long.MAX_VALUE) {
            relockJob = scope.launch {
                delay(autoRelock.durationMs)
                _unlocked.value = false
            }
        }
    }

    /** Re-lock immediately (e.g. the user turned the lock back on, or app went to background). */
    fun relock() {
        relockJob?.cancel()
        _unlocked.value = false
    }
}
