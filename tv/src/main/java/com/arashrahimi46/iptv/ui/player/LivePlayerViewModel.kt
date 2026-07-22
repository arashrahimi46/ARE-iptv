package com.arashrahimi46.iptv.ui.player

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arashrahimi46.iptv.data.db.AppDatabase
import com.arashrahimi46.iptv.data.model.ContentType
import com.arashrahimi46.iptv.data.repository.ContinueWatchingRepository
import com.arashrahimi46.iptv.data.repository.EpgRepository
import com.arashrahimi46.iptv.data.repository.FavoritesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
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

/**
 * Normalized playable content -- whichever [PlaybackSource] resolved it, the screen only sees this.
 * [searchName]/[year]/[season]/[episode] feed the online subtitle search (null for live channels,
 * which aren't searchable): a movie carries name+year; an episode carries its series name + S/E.
 */
data class PlayableMedia(
    val title: String,
    val subtitle: String?,
    val streamUrl: String,
    val isLive: Boolean,
    val searchName: String? = null,
    val year: String? = null,
    val season: Int? = null,
    val episode: Int? = null,
)

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
    /** Only set for [PlaybackSource.Episode] -- the playing episode id and its parent series'
     * episodes (Room-ordered by season, episode), so the player's prev/next-episode buttons can
     * step through them (and hide at the first/last). Empty for live/movie playback. */
    val currentEpisodeId: Long? = null,
    val siblingEpisodeIds: List<Long> = emptyList(),
    /** The favoritable item behind whatever is currently playing: a channel row id (LIVE), or a
     * VOD title row id (MOVIE, or SERIES -- for a movie, a series, or the parent series of an
     * episode). Null while unresolved. Drives the player HUD's favorite toggle. */
    val favoriteTargetId: Long? = null,
    val favoriteContentType: ContentType? = null,
    val isFavorite: Boolean = false,
    /** Continue-watching bookmark keys for VOD -- exactly one is set for a movie/series title
     * ([resumeVodTitleId]) or an episode ([resumeEpisodeId]); both null for live channels, which
     * never resume. Drive [resumePositionMs]/[saveProgress]. */
    val resumeVodTitleId: Long? = null,
    val resumeEpisodeId: Long? = null,
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
    private val favoritesRepository = FavoritesRepository(app)
    private val continueWatchingRepository = ContinueWatchingRepository(app)

    private val _uiState = MutableStateFlow(LivePlayerUiState())
    val uiState: StateFlow<LivePlayerUiState> = _uiState.asStateFlow()

    private val _upNext = MutableStateFlow<List<UpNextProgram>>(emptyList())
    val upNext: StateFlow<List<UpNextProgram>> = _upNext.asStateFlow()

    init {
        loadMedia(initialSource)
        observeUpNext()
        observeFavorite()
    }

    /** Keeps [LivePlayerUiState.isFavorite] in sync with the live favorites sets as either the
     * favorites change (toggled here or elsewhere) or the playing item changes ([switchChannel]). */
    private fun observeFavorite() {
        viewModelScope.launch {
            combine(
                _uiState.map { it.favoriteTargetId to it.favoriteContentType }.distinctUntilChanged(),
                favoritesRepository.favoriteChannelIds,
                favoritesRepository.favoriteVodIds,
            ) { (id, type), channelIds, vodIds ->
                when (type) {
                    ContentType.LIVE -> id != null && id in channelIds
                    ContentType.MOVIE, ContentType.SERIES -> id != null && id in vodIds
                    null -> false
                }
            }.distinctUntilChanged().collectLatest { fav ->
                _uiState.value = _uiState.value.copy(isFavorite = fav)
            }
        }
    }

    /** Favorite/unfavorite whatever is currently playing (channel, movie, or the episode's series). */
    fun toggleFavorite() {
        val id = _uiState.value.favoriteTargetId ?: return
        val type = _uiState.value.favoriteContentType ?: return
        viewModelScope.launch {
            if (type == ContentType.LIVE) favoritesRepository.toggleChannel(id)
            else favoritesRepository.toggleVod(id, type)
        }
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
                        PlayableMedia(
                            title = title.name,
                            subtitle = title.categoryName,
                            streamUrl = url,
                            isLive = false,
                            searchName = title.name,
                            year = title.year,
                        )
                    }
                }
                is PlaybackSource.Episode -> db.seriesEpisodeDao().getById(s.episodeId)?.let { episode ->
                    episode.streamUrl?.let { url ->
                        // Episodes match on the parent series name + S/E, not the (often per-episode) title.
                        val seriesName = db.vodTitleDao().getById(episode.seriesTitleId)?.name
                        PlayableMedia(
                            title = episode.name,
                            subtitle = "S${episode.season} · E${episode.episode}",
                            streamUrl = url,
                            isLive = false,
                            searchName = seriesName,
                            season = episode.season,
                            episode = episode.episode,
                        )
                    }
                }
            }
            // Resolve the favoritable target for whatever resolved: a channel favorites the channel;
            // a movie/series favorites the VOD title; an episode favorites its parent series.
            val (favoriteId, favoriteType) = if (media == null) {
                null to null
            } else when (val s = source) {
                is PlaybackSource.Channel -> s.channelId to ContentType.LIVE
                is PlaybackSource.Vod -> db.vodTitleDao().getById(s.vodTitleId)?.let { t ->
                    t.id to (if (t.isSeries) ContentType.SERIES else ContentType.MOVIE)
                } ?: (null to null)
                is PlaybackSource.Episode -> db.seriesEpisodeDao().getById(s.episodeId)?.let { e ->
                    e.seriesTitleId to ContentType.SERIES
                } ?: (null to null)
            }

            val siblingIds = if (source is PlaybackSource.Channel && media != null) {
                db.channelDao().getById(source.channelId)?.let { channel ->
                    // ids only (not full rows) so prev/next nav doesn't materialize a 100k+ catalog.
                    db.channelDao().idsForSource(channel.sourceId)
                } ?: emptyList()
            } else {
                emptyList()
            }
            // Sibling episodes for prev/next-episode nav (ids only, S/E-ordered) -- empty unless an
            // episode is playing.
            val siblingEpisodeIds = if (source is PlaybackSource.Episode && media != null) {
                db.seriesEpisodeDao().getById(source.episodeId)?.let { episode ->
                    db.seriesEpisodeDao().episodeIdsForSeries(episode.seriesTitleId)
                } ?: emptyList()
            } else {
                emptyList()
            }
            // Continue-watching keys: VOD title / episode resume against exactly the id that
            // played (movie/series title -> vodTitleId, episode -> its own episode id); live
            // channels resume nothing.
            val resumeVodId = (source as? PlaybackSource.Vod)?.vodTitleId
            val resumeEpisodeId = (source as? PlaybackSource.Episode)?.episodeId
            _uiState.value = _uiState.value.copy(
                media = media,
                loading = false,
                errorMessage = if (media == null) "Content not found" else null,
                phase = if (media == null) PlaybackPhase.Error else PlaybackPhase.Idle,
                currentChannelId = (source as? PlaybackSource.Channel)?.channelId,
                siblingChannelIds = siblingIds,
                currentEpisodeId = (source as? PlaybackSource.Episode)?.episodeId,
                siblingEpisodeIds = siblingEpisodeIds,
                favoriteTargetId = favoriteId,
                favoriteContentType = favoriteType,
                resumeVodTitleId = if (media == null) null else resumeVodId,
                resumeEpisodeId = if (media == null) null else resumeEpisodeId,
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

    /** [direction] +1 = next episode, -1 = previous. No wrap (unlike channels): the buttons are
     * hidden at the first/last episode, and this no-ops if called out of range anyway. */
    fun switchEpisode(direction: Int) {
        val state = _uiState.value
        val ids = state.siblingEpisodeIds
        val current = state.currentEpisodeId ?: return
        val index = ids.indexOf(current)
        if (index == -1) return
        val target = index + direction
        if (target < 0 || target >= ids.size) return
        loadMedia(PlaybackSource.Episode(ids[target]))
    }

    fun setPhase(phase: PlaybackPhase, errorMessage: String? = null) {
        _uiState.value = _uiState.value.copy(phase = phase, errorMessage = errorMessage)
    }

    /** P1.2: saved VOD resume position (ms) for whatever is currently playing, or 0 for live
     * channels / titles with no bookmark. The screen seeks a freshly-built ExoPlayer to this. */
    suspend fun resumePositionMs(): Long {
        val state = _uiState.value
        if (state.resumeVodTitleId == null && state.resumeEpisodeId == null) return 0L
        return continueWatchingRepository.resumePositionFor(state.resumeVodTitleId, state.resumeEpisodeId)
    }

    /** P1.2: persists the VOD watch bookmark. No-op for live channels (no resume keys) and for
     * a not-yet-advanced position, so a still-buffering title never overwrites a real bookmark.
     * Once playback passes [COMPLETION_THRESHOLD] the title counts as finished: the bookmark is
     * cleared instead of saved, so a watched movie/episode drops off Continue Watching rather
     * than lingering with a full progress bar (and resuming from its very end). */
    fun saveProgress(positionMs: Long, durationMs: Long) {
        val state = _uiState.value
        if (state.resumeVodTitleId == null && state.resumeEpisodeId == null) return
        if (positionMs <= 0) return
        val finished = durationMs > 0 && positionMs.toFloat() / durationMs >= COMPLETION_THRESHOLD
        viewModelScope.launch {
            if (finished) {
                continueWatchingRepository.clear(state.resumeVodTitleId, state.resumeEpisodeId)
            } else {
                continueWatchingRepository.updateProgress(state.resumeVodTitleId, state.resumeEpisodeId, positionMs, durationMs)
            }
        }
    }

    /** P0.1: called once auto-retry/backoff on the current channel's source is exhausted.
     * Switches playback to another catalog entry for the same channel name, if one exists,
     * so a dead source doesn't just retry forever. Returns whether an alternate was found and
     * switched to -- the caller needs this (QA finding) to surface a real error/manual-retry
     * state instead of leaving the screen stuck (e.g. permanently buffering) when there's
     * nothing to fall back to -- VOD/episode playback, or a channel with only one listing. */
    suspend fun fallbackToAlternateSource(): Boolean {
        val currentId = _uiState.value.currentChannelId ?: return false
        val current = db.channelDao().getById(currentId) ?: return false
        val alternate = db.channelDao().findAlternateByName(current.name, currentId) ?: return false
        loadMedia(PlaybackSource.Channel(alternate.id))
        return true
    }

    companion object {
        /** Fraction of a title watched past which it counts as finished (drops off Continue Watching). */
        private const val COMPLETION_THRESHOLD = 0.95f

        fun factory(app: Application, source: PlaybackSource): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = LivePlayerViewModel(app, source) as T
            }
    }
}
