package com.arashrahimi46.iptv.ui.player

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arashrahimi46.iptv.data.db.AppDatabase
import com.arashrahimi46.iptv.data.repository.EpgRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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

/** One upcoming/now-playing programme for the currently-playing channel's mini up-next panel. */
data class UpNextProgram(val title: String, val startMs: Long, val endMs: Long, val isNow: Boolean)

data class LivePlayerUiState(
    val media: PlayableMedia? = null,
    val loading: Boolean = true,
    val phase: PlaybackPhase = PlaybackPhase.Idle,
    val errorMessage: String? = null,
    /** Only set for [PlaybackSource.Channel] -- the currently-playing channel id and the full,
     * same-source channel list (already Room-ordered by name) it belongs to, so the screen's
     * channel-up/down handling can compute the next/previous id without its own query. */
    val currentChannelId: Long? = null,
    val siblingChannelIds: List<Long> = emptyList(),
)

/**
 * Looks up the real playable row for [source] from Room (one-shot queries --
 * channel/VOD-title/episode ids are all globally unique regardless of source)
 * so [LivePlayerScreen] hands ExoPlayer a real `streamUrl`, never a mocked one.
 *
 * Channel switching (QA MAJOR finding: "no in-player channel switcher") reuses
 * this same ViewModel instance rather than a fresh nav destination per channel --
 * [switchChannel] reloads in place, and [LivePlayerScreen]'s ExoPlayer already
 * keys off `media.streamUrl`, so a new channel naturally tears down/rebuilds the
 * player through the existing DisposableEffect(exoPlayer) path, no new plumbing.
 */
class LivePlayerViewModel(app: Application, initialSource: PlaybackSource) : AndroidViewModel(app) {
    private val db = AppDatabase.get(app)

    // Reuses the same EpgRepository/Room data the Guide screen renders, rather than a second
    // EPG-fetching path -- the mini up-next panel is just a narrower view (one channel, ordered
    // list) over the same [com.arashrahimi46.iptv.data.model.EPGProgram] rows.
    private val epgRepository = EpgRepository(app)

    private val _uiState = MutableStateFlow(LivePlayerUiState())
    val uiState: StateFlow<LivePlayerUiState> = _uiState.asStateFlow()

    private val _upNext = MutableStateFlow<List<UpNextProgram>>(emptyList())
    val upNext: StateFlow<List<UpNextProgram>> = _upNext.asStateFlow()

    init {
        loadMedia(initialSource)
        observeUpNext()
    }

    /** Re-queries the up-next list whenever the playing channel changes (initial load or [switchChannel]). */
    private fun observeUpNext() {
        viewModelScope.launch {
            _uiState.map { it.currentChannelId }.distinctUntilChanged().collectLatest { channelId ->
                if (channelId == null) {
                    _upNext.value = emptyList()
                    return@collectLatest
                }
                val nowMs = System.currentTimeMillis()
                epgRepository.observeForChannels(listOf(channelId), nowMs, nowMs + 6 * 3_600_000L).collectLatest { programs ->
                    _upNext.value = programs
                        .sortedBy { it.startMs }
                        .map { p -> UpNextProgram(title = p.title, startMs = p.startMs, endMs = p.endMs, isNow = nowMs in p.startMs until p.endMs) }
                }
            }
        }
    }

    private fun loadMedia(source: PlaybackSource) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, errorMessage = null)
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
            val siblingIds = if (source is PlaybackSource.Channel && media != null) {
                db.channelDao().getById(source.channelId)?.let { channel ->
                    db.channelDao().observeForSource(channel.sourceId).first().map { it.id }
                } ?: emptyList()
            } else {
                emptyList()
            }
            _uiState.value = _uiState.value.copy(
                media = media,
                loading = false,
                errorMessage = if (media == null) "Content not found" else null,
                phase = if (media == null) PlaybackPhase.Error else PlaybackPhase.Idle,
                currentChannelId = (source as? PlaybackSource.Channel)?.channelId,
                siblingChannelIds = siblingIds,
            )
        }
    }

    /** [direction] +1 = channel up (next), -1 = channel down (previous). No-op for VOD/episode
     * playback or a single-channel catalog (nothing meaningful to switch to). */
    fun switchChannel(direction: Int) {
        val state = _uiState.value
        val ids = state.siblingChannelIds
        val current = state.currentChannelId ?: return
        if (ids.size < 2) return
        val index = ids.indexOf(current)
        if (index == -1) return
        val nextIndex = ((index + direction) % ids.size + ids.size) % ids.size
        loadMedia(PlaybackSource.Channel(ids[nextIndex]))
    }

    fun setPhase(phase: PlaybackPhase, errorMessage: String? = null) {
        _uiState.value = _uiState.value.copy(phase = phase, errorMessage = errorMessage)
    }

    /** P0.1: called once auto-retry/backoff on the current channel's source is exhausted.
     * Switches playback to another catalog entry for the same channel name, if one exists,
     * so a dead source doesn't just retry forever. No-op (screen keeps showing the error/retry
     * state) when there's nothing to fall back to -- e.g. VOD/episode playback, or a channel
     * with only one listing. */
    fun fallbackToAlternateSource() {
        val currentId = _uiState.value.currentChannelId ?: return
        viewModelScope.launch {
            val current = db.channelDao().getById(currentId) ?: return@launch
            val alternate = db.channelDao().findAlternateByName(current.name, currentId)
            if (alternate != null) loadMedia(PlaybackSource.Channel(alternate.id))
        }
    }

    companion object {
        fun factory(app: Application, source: PlaybackSource): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = LivePlayerViewModel(app, source) as T
            }
    }
}
