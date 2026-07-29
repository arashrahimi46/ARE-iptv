package com.arashrahimi46.iptv.mobile.ui.player

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.arashrahimi46.iptv.data.player.DefaultStreamUrlResolver
import com.arashrahimi46.iptv.data.player.StreamKind
import com.arashrahimi46.iptv.data.repository.ContinueWatchingRepository
import com.arashrahimi46.iptv.data.repository.PlaylistRepository
import com.arashrahimi46.iptv.data.repository.PlaylistRepositoryImpl
import com.arashrahimi46.iptv.data.settings.CredentialsStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** What's being played -- mirrors [StreamKind] but also carries the row id so progress can be saved. */
sealed interface PlayerTarget {
    data class LiveChannel(val channelId: Long) : PlayerTarget
    data class Movie(val vodTitleId: Long) : PlayerTarget
}

data class PlayerUiState(
    val title: String = "",
    val isLoading: Boolean = true,
    val error: String? = null,
)

class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: PlaylistRepository = PlaylistRepositoryImpl(application)
    private val continueWatchingRepo = ContinueWatchingRepository(application)
    private val resolver = DefaultStreamUrlResolver(CredentialsStore(application))

    val player: ExoPlayer = ExoPlayer.Builder(application).build()

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private var target: PlayerTarget? = null
    private var progressJob: Job? = null

    fun load(target: PlayerTarget) {
        if (this.target == target) return
        this.target = target
        _state.value = PlayerUiState(isLoading = true)
        viewModelScope.launch {
            try {
                val (name, sourceId, kind, externalId, storedUrl, resumeMs) = when (target) {
                    is PlayerTarget.LiveChannel -> {
                        val channel = repository.channelsByIds(listOf(target.channelId)).firstOrNull()
                            ?: error("Channel not found")
                        Play6(channel.name, channel.sourceId, StreamKind.LIVE, channel.externalId, channel.streamUrl, 0L)
                    }
                    is PlayerTarget.Movie -> {
                        val title = repository.titlesByIds(listOf(target.vodTitleId)).firstOrNull()
                            ?: error("Title not found")
                        val resume = continueWatchingRepo.resumePositionFor(vodTitleId = title.id, seriesEpisodeId = null)
                        Play6(title.name, title.sourceId, StreamKind.VOD, title.externalId, title.streamUrl, resume)
                    }
                }
                val source = repository.observeSource(sourceId).first() ?: error("Source not found")
                val url = resolver.resolve(source, kind, externalId, storedUrl)
                player.setMediaItem(MediaItem.fromUri(url), resumeMs)
                player.prepare()
                player.playWhenReady = true
                _state.value = PlayerUiState(title = name, isLoading = false)
                if (target is PlayerTarget.Movie) startProgressTracking(target.vodTitleId)
            } catch (t: Throwable) {
                _state.value = PlayerUiState(isLoading = false, error = t.message ?: "Playback failed")
            }
        }
    }

    private fun startProgressTracking(vodTitleId: Long) {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (true) {
                delay(5000)
                val duration = player.duration
                val position = player.currentPosition
                if (duration > 0) {
                    continueWatchingRepo.updateProgress(vodTitleId, null, position, duration)
                }
            }
        }
    }

    override fun onCleared() {
        progressJob?.cancel()
        player.release()
        super.onCleared()
    }
}

private data class Play6(
    val name: String,
    val sourceId: Long,
    val kind: StreamKind,
    val externalId: String?,
    val storedUrl: String?,
    val resumeMs: Long,
)
