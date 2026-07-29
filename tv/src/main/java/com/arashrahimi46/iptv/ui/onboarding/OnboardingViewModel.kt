package com.arashrahimi46.iptv.ui.onboarding

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arashrahimi46.iptv.core.R
import com.arashrahimi46.iptv.data.repository.ImportSummary
import com.arashrahimi46.iptv.data.repository.PlaylistRepository
import com.arashrahimi46.iptv.data.repository.PlaylistRepositoryImpl
import com.arashrahimi46.iptv.data.settings.UserSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.arashrahimi46.iptv.data.parser.MAX_PLAYLISTS
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class OnboardingSourceType { XTREAM, M3U, STALKER }

data class OnboardingUiState(
    val sourceType: OnboardingSourceType = OnboardingSourceType.XTREAM,
    val portalName: String = "",
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
    val m3uUrl: String = "",
    /** Stalker only: the portal-issued MAC, kept auto-formatted as `AA:BB:CC:DD:EE:FF` (see [formatMacInput]).
     *  The portal URL reuses [serverUrl]. */
    val mac: String = "",
    val epgAuto: Boolean = true,
    val epgUrl: String = "",
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val result: ImportSummary? = null,
    val completedSourceId: Long? = null,
)

/** The editable credential fields for step 2, passed as one immutable snapshot so the step composable
 *  can update any single field via `copy(...)` without a wide positional callback. */
data class OnboardingCredentials(
    val portalName: String,
    val serverUrl: String,
    val username: String,
    val password: String,
    val m3uUrl: String,
    val mac: String,
)

/** Current credential fields as an [OnboardingCredentials] snapshot. */
fun OnboardingUiState.creds(): OnboardingCredentials =
    OnboardingCredentials(portalName, serverUrl, username, password, m3uUrl, mac)

/** The MAC a Stalker portal binds a subscription to, e.g. `00:1A:79:AB:CD:EF`. */
private val MAC_REGEX = Regex("^([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}$")

/** True when [mac] is a complete, well-formed colon-separated MAC address. */
internal fun isValidMac(mac: String): Boolean = MAC_REGEX.matches(mac.trim())

/**
 * Auto-format raw MAC keystrokes into `AA:BB:CC:DD:EE:FF` as the user types: keep only hex digits,
 * uppercase, cap at 12, and re-insert a colon every two. Lets the user paste `001A79ABCDEF` or type
 * freely and still land on a valid, readable MAC without fighting the colons.
 */
internal fun formatMacInput(raw: String): String =
    raw.filter { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }
        .uppercase()
        .take(12)
        .chunked(2)
        .joinToString(":")

/**
 * Shared state for the whole onboarding flow, hoisted above the internal
 * Compose Navigation back stack (OnboardingFlow) so each step composable can
 * read/mutate the same wizard state and the Confirm step can trigger the
 * real parse.
 */
class OnboardingViewModel(app: Application) : AndroidViewModel(app) {
    private val repository: PlaylistRepository = PlaylistRepositoryImpl(app)
    private val settings = UserSettings(app)

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun setSourceType(type: OnboardingSourceType) {
        _uiState.value = _uiState.value.copy(sourceType = type, error = null)
    }

    fun updateCredentials(
        portalName: String = _uiState.value.portalName,
        serverUrl: String = _uiState.value.serverUrl,
        username: String = _uiState.value.username,
        password: String = _uiState.value.password,
        m3uUrl: String = _uiState.value.m3uUrl,
        mac: String = _uiState.value.mac,
    ) {
        _uiState.value = _uiState.value.copy(
            portalName = portalName,
            serverUrl = serverUrl,
            username = username,
            password = password,
            m3uUrl = m3uUrl,
            // Auto-format on every keystroke so the field always shows a canonical MAC.
            mac = formatMacInput(mac),
            error = null,
        )
    }

    fun setEpg(auto: Boolean = _uiState.value.epgAuto, url: String = _uiState.value.epgUrl) {
        _uiState.value = _uiState.value.copy(epgAuto = auto, epgUrl = url, error = null)
    }

    /** Clears a stale submit error, e.g. when the user navigates between steps to review/edit. */
    fun clearError() {
        if (_uiState.value.error != null) _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Runs the real fetch + parse and persists the result. Safe to call multiple times (idempotent per attempt).
     * On success it populates [OnboardingUiState.result] + [OnboardingUiState.completedSourceId] and stops --
     * it does NOT navigate away, so the Confirm step can render the import summary. The "Go to Home" button
     * is what finishes onboarding (with [OnboardingUiState.completedSourceId]).
     */
    fun submit() {
        val state = _uiState.value
        if (state.isSubmitting) return
        _uiState.value = state.copy(isSubmitting = true, error = null)
        viewModelScope.launch {
            try {
                // The cap is enforced here as well as in Explore: it's advertised on the Explore
                // screen as a limit on playlists, so leaving this path unbounded would make it a
                // promise the app doesn't keep. Checked inside submit() rather than gating the
                // button because the count is a Flow and the check must be current at commit time.
                if (repository.observeSources().first().size >= MAX_PLAYLISTS) {
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        error = getApplication<Application>().getString(R.string.explore_cap_body),
                    )
                    return@launch
                }
                val name = state.portalName.ifBlank { getApplication<Application>().getString(R.string.onboarding_confirm_default_portal_name) }
                val epgUrl = state.epgUrl.takeIf { !state.epgAuto && it.isNotBlank() }
                val summary = when (state.sourceType) {
                    OnboardingSourceType.XTREAM -> repository.addXtreamSource(
                        name = name,
                        host = state.serverUrl.trim(),
                        username = state.username.trim(),
                        password = state.password,
                        epgUrl = epgUrl,
                    )
                    OnboardingSourceType.M3U -> repository.addM3uSource(
                        name = name,
                        url = state.m3uUrl.trim(),
                        epgUrl = epgUrl,
                    )
                    OnboardingSourceType.STALKER -> repository.addStalkerSource(
                        name = name,
                        portalUrl = state.serverUrl.trim(),
                        mac = state.mac.trim(),
                        epgUrl = epgUrl,
                    )
                }
                // The add methods return only the ImportSummary; the newly created source is made the
                // active source (setActiveSourceId), so read its id back to thread through onFinished.
                val sourceId = settings.activeSourceId.first()
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    result = summary,
                    completedSourceId = sourceId,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSubmitting = false, error = e.message ?: getApplication<Application>().getString(R.string.onboarding_error_generic))
            }
        }
    }

    companion object {
        fun factory(app: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = OnboardingViewModel(app) as T
            }
    }
}
