package com.arashrahimi46.iptv.ui.onboarding

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arashrahimi46.iptv.data.repository.ImportSummary
import com.arashrahimi46.iptv.data.repository.PlaylistRepository
import com.arashrahimi46.iptv.data.repository.PlaylistRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class OnboardingSourceType { XTREAM, M3U }

data class OnboardingUiState(
    val sourceType: OnboardingSourceType = OnboardingSourceType.XTREAM,
    val portalName: String = "",
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
    val m3uUrl: String = "",
    val epgAuto: Boolean = true,
    val epgUrl: String = "",
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val result: ImportSummary? = null,
    val completedSourceId: Long? = null,
)

/**
 * Shared state for the whole onboarding flow, hoisted above the internal
 * Compose Navigation back stack (OnboardingFlow) so each step composable can
 * read/mutate the same wizard state and the Confirm step can trigger the
 * real parse.
 */
class OnboardingViewModel(app: Application) : AndroidViewModel(app) {
    private val repository: PlaylistRepository = PlaylistRepositoryImpl(app)

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
    ) {
        _uiState.value = _uiState.value.copy(
            portalName = portalName,
            serverUrl = serverUrl,
            username = username,
            password = password,
            m3uUrl = m3uUrl,
            error = null,
        )
    }

    fun setEpg(auto: Boolean = _uiState.value.epgAuto, url: String = _uiState.value.epgUrl) {
        _uiState.value = _uiState.value.copy(epgAuto = auto, epgUrl = url)
    }

    /** Runs the real fetch + parse and persists the result. Safe to call multiple times (idempotent per attempt). */
    fun submit(onDone: () -> Unit) {
        val state = _uiState.value
        if (state.isSubmitting) return
        _uiState.value = state.copy(isSubmitting = true, error = null)
        viewModelScope.launch {
            try {
                val name = state.portalName.ifBlank { "My playlist" }
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
                }
                _uiState.value = _uiState.value.copy(isSubmitting = false, result = summary)
                onDone()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSubmitting = false, error = e.message ?: "Something went wrong")
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
