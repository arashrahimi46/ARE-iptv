package com.arashrahimi46.iptv.mobile.ui.onboarding

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arashrahimi46.iptv.data.model.SourceType
import com.arashrahimi46.iptv.data.repository.PlaylistRepository
import com.arashrahimi46.iptv.data.repository.PlaylistRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
)

class OnboardingViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: PlaylistRepository = PlaylistRepositoryImpl(application)

    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    fun setType(type: SourceType) {
        _state.value = _state.value.copy(type = type, error = null)
    }

    fun setName(v: String) { _state.value = _state.value.copy(name = v) }
    fun setUrl(v: String) { _state.value = _state.value.copy(url = v) }
    fun setUsername(v: String) { _state.value = _state.value.copy(username = v) }
    fun setPassword(v: String) { _state.value = _state.value.copy(password = v) }
    fun setMac(v: String) { _state.value = _state.value.copy(mac = v) }
    fun setEpgUrl(v: String) { _state.value = _state.value.copy(epgUrl = v) }

    fun submit() {
        val s = _state.value
        if (s.isSubmitting) return
        _state.value = s.copy(isSubmitting = true, error = null)
        viewModelScope.launch {
            try {
                val name = s.name.ifBlank { "My playlist" }
                val epg = s.epgUrl.trim().ifBlank { null }
                when (s.type) {
                    SourceType.XTREAM -> repository.addXtreamSource(name, s.url.trim(), s.username.trim(), s.password, epg)
                    SourceType.M3U -> repository.addM3uSource(name, s.url.trim(), epg)
                    SourceType.STALKER -> repository.addStalkerSource(name, s.url.trim(), s.mac.trim(), epg)
                }
                _state.value = _state.value.copy(isSubmitting = false, added = true)
            } catch (t: Throwable) {
                _state.value = _state.value.copy(isSubmitting = false, error = t.message ?: "Something went wrong")
            }
        }
    }
}
