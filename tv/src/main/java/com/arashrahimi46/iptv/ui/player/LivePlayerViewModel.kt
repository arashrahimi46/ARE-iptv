package com.arashrahimi46.iptv.ui.player

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arashrahimi46.iptv.data.db.AppDatabase
import com.arashrahimi46.iptv.data.model.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** ExoPlayer-facing player state, driven by [androidx.media3.common.Player.Listener] callbacks in the screen. */
enum class PlaybackPhase { Idle, Buffering, Playing, Paused, Error }

data class LivePlayerUiState(
    val channel: Channel? = null,
    val loading: Boolean = true,
    val phase: PlaybackPhase = PlaybackPhase.Idle,
    val errorMessage: String? = null,
)

/**
 * Looks up the real [Channel] row for [channelId] from Room (a one-shot
 * [com.arashrahimi46.iptv.data.db.ChannelDao.getById] query -- channel id is
 * globally unique regardless of source) so [LivePlayerScreen] hands ExoPlayer
 * a real `streamUrl`, never a mocked/static one.
 */
class LivePlayerViewModel(app: Application, private val channelId: Long) : AndroidViewModel(app) {
    private val db = AppDatabase.get(app)

    private val _uiState = MutableStateFlow(LivePlayerUiState())
    val uiState: StateFlow<LivePlayerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val channel = db.channelDao().getById(channelId)
            _uiState.value = _uiState.value.copy(
                channel = channel,
                loading = false,
                errorMessage = if (channel == null) "Channel not found" else null,
                phase = if (channel == null) PlaybackPhase.Error else PlaybackPhase.Idle,
            )
        }
    }

    fun setPhase(phase: PlaybackPhase, errorMessage: String? = null) {
        _uiState.value = _uiState.value.copy(phase = phase, errorMessage = errorMessage)
    }

    companion object {
        fun factory(app: Application, channelId: Long): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = LivePlayerViewModel(app, channelId) as T
            }
    }
}
