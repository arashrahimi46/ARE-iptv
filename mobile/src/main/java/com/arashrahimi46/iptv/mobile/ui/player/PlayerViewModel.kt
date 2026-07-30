package com.arashrahimi46.iptv.mobile.ui.player

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.arashrahimi46.iptv.data.db.AppDatabase
import com.arashrahimi46.iptv.data.model.directStreamLabel
import com.arashrahimi46.iptv.data.player.DefaultStreamUrlResolver
import com.arashrahimi46.iptv.data.player.StreamKind
import com.arashrahimi46.iptv.data.recording.RecordingStorage
import com.arashrahimi46.iptv.data.repository.ContinueWatchingRepository
import com.arashrahimi46.iptv.data.repository.PlaylistRepository
import com.arashrahimi46.iptv.data.repository.PlaylistRepositoryImpl
import com.arashrahimi46.iptv.data.repository.RecordingRepository
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
    data class Episode(val episodeId: Long) : PlayerTarget
    /** A saved "Open network stream" URL, resolved from [com.arashrahimi46.iptv.data.model.DirectStream]. */
    data class DirectStream(val streamId: Long) : PlayerTarget
    /** A completed/interrupted local [com.arashrahimi46.iptv.data.model.Recording], played straight
     * from its SAF document URI -- no source/credentials lookup needed. */
    data class Recording(val recordingId: Long) : PlayerTarget
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
    private val db = AppDatabase.get(application)
    private val recordingRepository = RecordingRepository(application)
    private val recordingStorage = RecordingStorage(application)

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
                // A direct stream or local recording plays straight from its own URI -- no
                // source/credentials lookup, no resolver, no continue-watching tracking (matches
                // :tv's LivePlayerViewModel, which doesn't track progress for either either).
                when (target) {
                    is PlayerTarget.DirectStream -> {
                        val stream = db.directStreamDao().getById(target.streamId) ?: error("Stream not found")
                        playUri(Uri.parse(stream.url), directStreamLabel(stream.url, stream.name))
                        return@launch
                    }
                    is PlayerTarget.Recording -> {
                        val recording = recordingRepository.getById(target.recordingId) ?: error("Recording not found")
                        val treeUri = Uri.parse(recording.storageTreeUri)
                        val videoUri = recordingStorage.documentUri(treeUri, recording.documentId)
                        playUri(videoUri, recording.programTitle ?: recording.channelName)
                        return@launch
                    }
                    else -> Unit
                }
                val (name, sourceId, kind, externalId, storedUrl, resumeMs, seriesNum) = when (target) {
                    is PlayerTarget.LiveChannel -> {
                        val channel = repository.channelsByIds(listOf(target.channelId)).firstOrNull()
                            ?: error("Channel not found")
                        PlayTarget(channel.name, channel.sourceId, StreamKind.LIVE, channel.externalId, channel.streamUrl, 0L, null)
                    }
                    is PlayerTarget.Movie -> {
                        val title = repository.titlesByIds(listOf(target.vodTitleId)).firstOrNull()
                            ?: error("Title not found")
                        val resume = continueWatchingRepo.resumePositionFor(vodTitleId = title.id, seriesEpisodeId = null)
                        PlayTarget(title.name, title.sourceId, StreamKind.VOD, title.externalId, title.streamUrl, resume, null)
                    }
                    is PlayerTarget.Episode -> {
                        val episode = db.seriesEpisodeDao().getById(target.episodeId) ?: error("Episode not found")
                        val series = repository.titlesByIds(listOf(episode.seriesTitleId)).firstOrNull()
                            ?: error("Series not found")
                        val resume = continueWatchingRepo.resumePositionFor(vodTitleId = null, seriesEpisodeId = episode.id)
                        PlayTarget(episode.name, series.sourceId, StreamKind.SERIES, episode.externalId, episode.streamUrl, resume, episode.episode)
                    }
                    is PlayerTarget.DirectStream, is PlayerTarget.Recording -> error("handled above")
                }
                val source = repository.observeSource(sourceId).first() ?: error("Source not found")
                val url = resolver.resolve(source, kind, externalId, storedUrl, series = seriesNum)
                player.setMediaItem(MediaItem.fromUri(url), resumeMs)
                player.prepare()
                player.playWhenReady = true
                _state.value = PlayerUiState(title = name, isLoading = false)
                when (target) {
                    is PlayerTarget.Movie -> startProgressTracking(vodTitleId = target.vodTitleId, seriesEpisodeId = null)
                    is PlayerTarget.Episode -> startProgressTracking(vodTitleId = null, seriesEpisodeId = target.episodeId)
                    else -> Unit
                }
            } catch (t: Throwable) {
                _state.value = PlayerUiState(isLoading = false, error = t.message ?: "Playback failed")
            }
        }
    }

    private fun playUri(uri: Uri, title: String) {
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
        player.playWhenReady = true
        _state.value = PlayerUiState(title = title, isLoading = false)
    }

    private fun startProgressTracking(vodTitleId: Long?, seriesEpisodeId: Long?) {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (true) {
                delay(5000)
                val duration = player.duration
                val position = player.currentPosition
                if (duration > 0) {
                    continueWatchingRepo.updateProgress(vodTitleId, seriesEpisodeId, position, duration)
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

private data class PlayTarget(
    val name: String,
    val sourceId: Long,
    val kind: StreamKind,
    val externalId: String?,
    val storedUrl: String?,
    val resumeMs: Long,
    /** Episode number for a Stalker series episode (sent as `&series=` at resolve time); null otherwise. */
    val seriesNum: Int?,
)
