package com.arashrahimi46.iptv.ui.player

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arashrahimi46.iptv.data.db.AppDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** ExoPlayer-facing player state, driven by [androidx.media3.common.Player.Listener] callbacks in the screen. */
enum class PlaybackPhase { Idle, Buffering, Playing, Paused, Error }

/**
 * Content-id-driven source for [LivePlayerScreen] -- mirrors the existing
 * "player/{channelId}" convention rather than passing a raw streamUrl through
 * nav args. [Vod] and [Episode] let Detail's Play action reuse the same
 * screen/ViewModel/ExoPlayer lifecycle as live channels instead of a second
 * near-duplicate VOD player.
 */
sealed class PlaybackSource {
    data class Channel(val channelId: Long) : PlaybackSource()
    data class Vod(val vodTitleId: Long) : PlaybackSource()
    data class Episode(val episodeId: Long) : PlaybackSource()
}

/** Normalized playable content -- whichever [PlaybackSource] resolved it, the screen only sees this. */
data class PlayableMedia(val title: String, val subtitle: String?, val streamUrl: String, val isLive: Boolean)

data class LivePlayerUiState(
    val media: PlayableMedia? = null,
    val loading: Boolean = true,
    val phase: PlaybackPhase = PlaybackPhase.Idle,
    val errorMessage: String? = null,
)

/**
 * Looks up the real playable row for [source] from Room (one-shot queries --
 * channel/VOD-title/episode ids are all globally unique regardless of source)
 * so [LivePlayerScreen] hands ExoPlayer a real `streamUrl`, never a mocked one.
 */
class LivePlayerViewModel(app: Application, private val source: PlaybackSource) : AndroidViewModel(app) {
    private val db = AppDatabase.get(app)

    private val _uiState = MutableStateFlow(LivePlayerUiState())
    val uiState: StateFlow<LivePlayerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val media = when (val s = source) {
                is PlaybackSource.Channel -> db.channelDao().getById(s.channelId)?.let {
                    PlayableMedia(title = it.name, subtitle = it.categoryName, streamUrl = it.streamUrl, isLive = true)
                }
                is PlaybackSource.Vod -> db.vodTitleDao().getById(s.vodTitleId)?.let { title ->
                    title.streamUrl?.let { url ->
                        PlayableMedia(title = title.name, subtitle = title.categoryName, streamUrl = url, isLive = false)
                    }
                }
                is PlaybackSource.Episode -> db.seriesEpisodeDao().getById(s.episodeId)?.let { episode ->
                    episode.streamUrl?.let { url ->
                        PlayableMedia(
                            title = episode.name,
                            subtitle = "S${episode.season} · E${episode.episode}",
                            streamUrl = url,
                            isLive = false,
                        )
                    }
                }
            }
            _uiState.value = _uiState.value.copy(
                media = media,
                loading = false,
                errorMessage = if (media == null) "Content not found" else null,
                phase = if (media == null) PlaybackPhase.Error else PlaybackPhase.Idle,
            )
        }
    }

    fun setPhase(phase: PlaybackPhase, errorMessage: String? = null) {
        _uiState.value = _uiState.value.copy(phase = phase, errorMessage = errorMessage)
    }

    companion object {
        fun factory(app: Application, source: PlaybackSource): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = LivePlayerViewModel(app, source) as T
            }
    }
}
