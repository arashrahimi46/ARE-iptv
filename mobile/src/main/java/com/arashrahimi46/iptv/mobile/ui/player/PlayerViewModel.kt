package com.arashrahimi46.iptv.mobile.ui.player

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.datasource.HttpDataSource
import com.arashrahimi46.iptv.core.R as CoreR
import com.arashrahimi46.iptv.mobile.data.db.AppDatabase
import com.arashrahimi46.iptv.mobile.data.model.directStreamLabel
import com.arashrahimi46.iptv.mobile.data.player.DefaultStreamUrlResolver
import com.arashrahimi46.iptv.mobile.data.player.StreamKind
import com.arashrahimi46.iptv.mobile.data.recording.RecordingStorage
import com.arashrahimi46.iptv.mobile.data.repository.ContinueWatchingRepository
import com.arashrahimi46.iptv.mobile.data.repository.PlaylistRepository
import com.arashrahimi46.iptv.mobile.data.repository.PlaylistRepositoryImpl
import com.arashrahimi46.iptv.mobile.data.repository.RecordingRepository
import com.arashrahimi46.iptv.mobile.data.settings.CredentialsStore
import com.arashrahimi46.iptv.mobile.data.settings.UserSettings
import java.net.UnknownHostException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What's being played -- mirrors [StreamKind] but also carries the row id so progress can be saved. */
sealed interface PlayerTarget {
    data class LiveChannel(val channelId: Long) : PlayerTarget
    data class Movie(val vodTitleId: Long) : PlayerTarget
    data class Episode(val episodeId: Long) : PlayerTarget
    /** A saved "Open network stream" URL, resolved from [com.arashrahimi46.iptv.mobile.data.model.DirectStream]. */
    data class DirectStream(val streamId: Long) : PlayerTarget
    /** A completed/interrupted local [com.arashrahimi46.iptv.mobile.data.model.Recording], played straight
     * from its SAF document URI -- no source/credentials lookup needed. */
    data class Recording(val recordingId: Long) : PlayerTarget
}

data class PlayerUiState(
    val title: String = "",
    /** Series name for an episode, provider label for a direct stream; null when there is nothing to add. */
    val subtitle: String? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    /** A live channel has no seekable timeline -- the HUD swaps the scrubber for a LIVE badge. */
    val isLive: Boolean = false,
    val playing: Boolean = false,
    val buffering: Boolean = false,
    /** Sibling episode ids, published only for [PlayerTarget.Episode]. */
    val prevEpisodeId: Long? = null,
    val nextEpisodeId: Long? = null,
    /** Non-null while the autoplay-next countdown is running; seconds remaining. */
    val upNextSeconds: Int? = null,
)

private const val COMPLETION_THRESHOLD = 0.95f

class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: PlaylistRepository = PlaylistRepositoryImpl(application)
    private val continueWatchingRepo = ContinueWatchingRepository(application)
    private val resolver = DefaultStreamUrlResolver(CredentialsStore(application))
    private val db = AppDatabase.get(application)
    private val recordingRepository = RecordingRepository(application)
    private val recordingStorage = RecordingStorage(application)
    private val settings = UserSettings(application)

    /** Outlives [viewModelScope] by design: the last progress write must survive the cancellation
     * that tearing the screen down triggers, otherwise leaving mid-poll loses up to 5s. */
    private val flushScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** The final progress write, so [onCleared] can cancel [flushScope] after it lands. */
    private var flushJob: Job? = null

    val player: ExoPlayer = ExoPlayer.Builder(application)
        // Request audio focus, exactly as :tv does (LivePlayerScreen.kt:563). The phone build was
        // constructing a bare player, so the stream never ducked or paused for an incoming call,
        // an alarm or another media app -- and never yielded focus back. This matters far more on
        // a phone than on a TV, where it was already handled.
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build(),
            /* handleAudioFocus = */ true,
        )
        // Unplugging headphones / disconnecting BT pauses instead of blasting the speaker.
        .setHandleAudioBecomingNoisy(true)
        .build()
        .apply {
            // Keep the network alive while the screen is off, so audio-only / PiP playback of a
            // network stream doesn't stall the moment the device idles.
            setWakeMode(C.WAKE_MODE_NETWORK)
        }

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private var target: PlayerTarget? = null
    private var progressJob: Job? = null
    private var countdownJob: Job? = null

    /** (vodTitleId, seriesEpisodeId) of whatever progress is currently being tracked, or null. */
    private var tracked: Pair<Long?, Long?>? = null

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.update { it.copy(playing = isPlaying) }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            _state.update { it.copy(buffering = playbackState == Player.STATE_BUFFERING) }
            if (playbackState == Player.STATE_ENDED) onPlaybackEnded()
        }

        override fun onPlayerError(error: PlaybackException) {
            _state.update {
                it.copy(isLoading = false, buffering = false, error = describeError(error))
            }
        }
    }

    init {
        player.addListener(listener)
    }

    /**
     * A message the user can act on, instead of ExoPlayer's own.
     *
     * `PlaybackException.message` is NOT usable here: for any source failure it is the literal
     * string "Source error", so a dead network, an expired line and a 403 all rendered identically
     * -- and "Source error" tells a phone user nothing about which. (Verified on device: an
     * UnknownHostException surfaced as "Source error".) The cause chain carries the real
     * information, so this mirrors what :tv already does in LivePlayerScreen.onPlayerError, with
     * the offline case added because a phone loses connectivity constantly and a TV rarely does.
     */
    private fun describeError(error: PlaybackException): String {
        val failed = getApplication<Application>().getString(CoreR.string.player_playback_failed)
        return when (val cause = error.cause) {
            // The provider's own rejection -- 403/461 for an expired, IP-locked or
            // connection-limited line. The number is the single most actionable thing we have.
            is HttpDataSource.InvalidResponseCodeException -> "$failed: HTTP ${cause.responseCode}"
            // No host / no route: the device is offline or DNS failed. Deliberately the same copy
            // as any other transport failure -- there is no "you're offline" string in :core, and
            // inventing one means translating it into 24 locales. Flagged as a follow-up rather
            // than machine-translated.
            is UnknownHostException -> failed
            is HttpDataSource.HttpDataSourceException -> failed
            else -> error.errorCodeName.replace('_', ' ').lowercase()
                .replaceFirstChar { it.uppercase() }
                .ifBlank { failed }
        }
    }

    fun load(target: PlayerTarget) {
        if (this.target == target) return
        this.target = target
        countdownJob?.cancel()
        progressJob?.cancel()
        tracked = null
        _state.value = PlayerUiState(isLoading = true, isLive = target is PlayerTarget.LiveChannel)
        viewModelScope.launch {
            try {
                seedTrackPreferences()
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
                var subtitle: String? = null
                var prevId: Long? = null
                var nextId: Long? = null
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
                        subtitle = series.name
                        // Siblings drive the HUD's prev/next and the autoplay-next chain. The DAO
                        // returns them in season/episode order.
                        val siblings = db.seriesEpisodeDao().episodeIdsForSeries(episode.seriesTitleId)
                        val at = siblings.indexOf(episode.id)
                        if (at >= 0) {
                            prevId = siblings.getOrNull(at - 1)
                            nextId = siblings.getOrNull(at + 1)
                        }
                        PlayTarget(episode.name, series.sourceId, StreamKind.SERIES, episode.externalId, episode.streamUrl, resume, episode.episode)
                    }
                    is PlayerTarget.DirectStream, is PlayerTarget.Recording -> error("handled above")
                }
                // Publish the name BEFORE the network work below. Resolving the URL and preparing
                // the stream is where playback actually fails, and until this landed the catch had
                // nothing to preserve -- a dead channel rendered as "Playback failed" on black with
                // no indication of WHICH channel had failed.
                _state.update { it.copy(title = name, subtitle = subtitle) }
                val source = repository.observeSource(sourceId).first() ?: error("Source not found")
                val url = resolver.resolve(source, kind, externalId, storedUrl, series = seriesNum)
                player.setMediaItem(MediaItem.fromUri(url), resumeMs)
                player.prepare()
                player.playWhenReady = true
                _state.value = PlayerUiState(
                    title = name,
                    subtitle = subtitle,
                    isLoading = false,
                    isLive = target is PlayerTarget.LiveChannel,
                    prevEpisodeId = prevId,
                    nextEpisodeId = nextId,
                )
                when (target) {
                    is PlayerTarget.Movie -> startProgressTracking(vodTitleId = target.vodTitleId, seriesEpisodeId = null)
                    is PlayerTarget.Episode -> startProgressTracking(vodTitleId = null, seriesEpisodeId = target.episodeId)
                    else -> Unit
                }
            } catch (t: Throwable) {
                // Was `_state.value = PlayerUiState(...)`, which built a BLANK state and so threw
                // away the title published above, plus isLive and the episode siblings. Copy
                // instead, so the error screen can still say what failed and Back/next still work.
                // The fallback was also the hardcoded English literal "Playback failed" -- the one
                // user-facing string in this file that skipped :core and shipped untranslated to
                // all 24 locales.
                val failed = getApplication<Application>().getString(CoreR.string.player_playback_failed)
                _state.update {
                    it.copy(isLoading = false, buffering = false, error = t.message ?: failed)
                }
            }
        }
    }

    /** Re-runs the current target from scratch -- the retry behind [PlayerUiState.error]. */
    fun retry() {
        val current = target ?: return
        target = null
        load(current)
    }

    /** Jump to a sibling episode. The same-target guard in [load] makes a repeat call idempotent. */
    fun playEpisode(episodeId: Long) {
        countdownJob?.cancel()
        _state.update { it.copy(upNextSeconds = null) }
        load(PlayerTarget.Episode(episodeId))
    }

    fun cancelUpNext() {
        countdownJob?.cancel()
        _state.update { it.copy(upNextSeconds = null) }
    }

    /** Makes the stored subtitle/audio language preferences actually do something: they are pushed
     * into the selector before the media item is set, so the first render already honours them. */
    private suspend fun seedTrackPreferences() {
        val text = settings.subtitleLanguage.first()
        val audio = settings.preferredAudioLanguage.first()
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .apply {
                if (text.isNotBlank()) setPreferredTextLanguage(text)
                if (audio.isNotBlank()) setPreferredAudioLanguage(audio)
            }
            .build()
    }

    private fun playUri(uri: Uri, title: String) {
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
        player.playWhenReady = true
        _state.value = PlayerUiState(title = title, isLoading = false)
    }

    private fun startProgressTracking(vodTitleId: Long?, seriesEpisodeId: Long?) {
        progressJob?.cancel()
        tracked = vodTitleId to seriesEpisodeId
        progressJob = viewModelScope.launch {
            while (true) {
                delay(5000)
                val duration = player.duration
                val position = player.currentPosition
                if (duration > 0) {
                    // Past the completion threshold the row leaves "Continue watching" instead of
                    // being pinned at 99% forever.
                    if (position.toFloat() / duration >= COMPLETION_THRESHOLD) {
                        continueWatchingRepo.clear(vodTitleId, seriesEpisodeId)
                    } else {
                        continueWatchingRepo.updateProgress(vodTitleId, seriesEpisodeId, position, duration)
                    }
                }
            }
        }
    }

    /** Autoplay-next: the phone replacement for :tv's D-pad chapter buttons. `0` seconds = off. */
    private fun onPlaybackEnded() {
        val next = _state.value.nextEpisodeId ?: return
        if (target !is PlayerTarget.Episode) return
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            val seconds = settings.autoplayNextDelaySeconds.first().toInt()
            if (seconds <= 0) return@launch
            for (remaining in seconds downTo 1) {
                _state.update { it.copy(upNextSeconds = remaining) }
                delay(1000)
            }
            _state.update { it.copy(upNextSeconds = null) }
            playEpisode(next)
        }
    }

    override fun onCleared() {
        progressJob?.cancel()
        countdownJob?.cancel()
        flushProgress()
        player.removeListener(listener)
        player.release()
        // flushProgress() launched the final write on flushScope precisely so it would outlive
        // viewModelScope's cancellation; cancelling the scope outright here would defeat that. So
        // cancel only once THAT job finishes -- flushScope is concurrent, so an empty marker
        // coroutine would race ahead of the write and cancel it mid-flight.
        flushJob?.invokeOnCompletion { flushScope.cancel() } ?: flushScope.cancel()
        super.onCleared()
    }

    /** One last write before [ExoPlayer.release] wipes the clock. */
    private fun flushProgress() {
        val ids = tracked ?: return
        val duration = player.duration
        val position = player.currentPosition
        if (duration <= 0) return
        val finished = position.toFloat() / duration >= COMPLETION_THRESHOLD
        flushJob = flushScope.launch {
            if (finished) continueWatchingRepo.clear(ids.first, ids.second)
            else continueWatchingRepo.updateProgress(ids.first, ids.second, position, duration)
        }
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
