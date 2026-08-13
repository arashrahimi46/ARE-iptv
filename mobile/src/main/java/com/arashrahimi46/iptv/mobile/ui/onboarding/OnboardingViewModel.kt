package com.arashrahimi46.iptv.mobile.ui.onboarding

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arashrahimi46.iptv.core.R as CoreR
import com.arashrahimi46.iptv.mobile.data.model.SourceType
import com.arashrahimi46.iptv.mobile.data.repository.PlaylistRepository
import com.arashrahimi46.iptv.mobile.data.repository.PlaylistRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Which field failed local validation. The screen maps these to `CoreR` copy. */
enum class OnboardingFieldError { NAME, URL, MAC }

/** Mirrors :tv's onboarding source-type choice; reuses the same [PlaylistRepository] import calls. */
data class OnboardingUiState(
    val type: SourceType = SourceType.XTREAM,
    val name: String = "",
    val url: String = "", // Xtream/Stalker: portal host. M3U: playlist URL.
    val username: String = "",
    val password: String = "",
    val mac: String = "",
    val epgUrl: String = "",
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val added: Boolean = false,
    /** Populated by [OnboardingViewModel.submit] BEFORE any network work is attempted. */
    val fieldErrors: Set<OnboardingFieldError> = emptySet(),
)

class OnboardingViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: PlaylistRepository = PlaylistRepositoryImpl(application)

    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    fun setType(type: SourceType) {
        // Switching source type re-scopes which fields matter, so stale field errors are dropped.
        _state.value = _state.value.copy(type = type, error = null, fieldErrors = emptySet())
    }

    fun setName(v: String) = update { it.copy(name = v) - OnboardingFieldError.NAME }
    fun setUrl(v: String) = update { it.copy(url = v) - OnboardingFieldError.URL }
    fun setUsername(v: String) = update { it.copy(username = v) }
    fun setPassword(v: String) = update { it.copy(password = v) }
    fun setMac(v: String) = update { it.copy(mac = v) - OnboardingFieldError.MAC }
    fun setEpgUrl(v: String) = update { it.copy(epgUrl = v) }

    private inline fun update(block: (OnboardingUiState) -> OnboardingUiState) {
        _state.value = block(_state.value)
    }

    private operator fun OnboardingUiState.minus(error: OnboardingFieldError) =
        if (error in fieldErrors) copy(fieldErrors = fieldErrors - error) else this

    fun submit() {
        val s = _state.value
        if (s.isSubmitting) return

        // Local validation runs first: a malformed URL must never reach the provider. Xtream hosts
        // rate-limit by IP, so a speculative request from a typo'd portal is a real cost.
        val errors = buildSet {
            if (s.name.isBlank()) add(OnboardingFieldError.NAME)
            if (!isValidSourceUrl(s.url)) add(OnboardingFieldError.URL)
            if (s.type == SourceType.STALKER && !MAC_REGEX.matches(s.mac.trim())) {
                add(OnboardingFieldError.MAC)
            }
        }
        if (errors.isNotEmpty()) {
            _state.value = s.copy(fieldErrors = errors, error = null)
            return
        }

        _state.value = s.copy(isSubmitting = true, error = null, fieldErrors = emptySet())
        viewModelScope.launch {
            try {
                val name = s.name.trim()
                val epg = s.epgUrl.trim().ifBlank { null }
                when (s.type) {
                    SourceType.XTREAM -> repository.addXtreamSource(name, s.url.trim(), s.username.trim(), s.password, epg)
                    SourceType.M3U -> repository.addM3uSource(name, s.url.trim(), epg)
                    SourceType.STALKER -> repository.addStalkerSource(name, s.url.trim(), s.mac.trim(), epg)
                }
                _state.value = _state.value.copy(isSubmitting = false, added = true)
            } catch (t: Throwable) {
                val fallback = getApplication<Application>().getString(CoreR.string.onboarding_error_generic)
                _state.value = _state.value.copy(isSubmitting = false, error = t.message ?: fallback)
            }
        }
    }

    /** Dismiss the submit-failure banner so the user can correct and retry. */
    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    private companion object {
        val MAC_REGEX = Regex("^([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}$")

        /**
         * Accepts an absolute http(s) URL. A bare `host:8080` is accepted too and normalised by the
         * repository, matching the placeholder copy the field shows -- but anything with a space,
         * no host, or a non-http scheme is rejected inline.
         */
        fun isValidSourceUrl(raw: String): Boolean {
            val value = raw.trim()
            if (value.isBlank() || value.any { it.isWhitespace() }) return false
            val withScheme = if (value.contains("://")) value else "http://$value"
            val uri = runCatching { java.net.URI(withScheme) }.getOrNull() ?: return false
            val scheme = uri.scheme?.lowercase()
            if (scheme != "http" && scheme != "https") return false
            return !uri.host.isNullOrBlank()
        }
    }
}
