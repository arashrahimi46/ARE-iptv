package com.arashrahimi46.iptv.ui.player

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arashrahimi46.iptv.R
import com.arashrahimi46.iptv.data.db.AppDatabase
import com.arashrahimi46.iptv.data.model.ContentType
import com.arashrahimi46.iptv.data.model.SourceType
import com.arashrahimi46.iptv.data.model.directStreamLabel
import com.arashrahimi46.iptv.data.player.DefaultStreamUrlResolver
import com.arashrahimi46.iptv.data.player.StreamKind
import com.arashrahimi46.iptv.data.settings.CredentialsStore
import android.net.Uri
import com.arashrahimi46.iptv.data.recording.RecordingSupervisor
import com.arashrahimi46.iptv.data.repository.ContinueWatchingRepository
import com.arashrahimi46.iptv.data.repository.EpgRepository
import com.arashrahimi46.iptv.data.repository.FavoritesRepository
import com.arashrahimi46.iptv.data.settings.UserSettings
import com.arashrahimi46.iptv.ui.multiview.MultiViewViewModel
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
    /** A local Live TV recording (V1) -- played back seekably from disk like VOD. */
    data class LocalRecording(val recordingId: Long) : PlaybackSource()

    /** Catch-up / archive: an already-aired programme on live [channelId], identified by its EPG
     * window [programStartMs]..[programEndMs]. The archive URL is minted at play-time from the
     * channel's source (resolve-on-play). Plays seekably like VOD. See docs/catchup-v1-design.md. */
    data class Catchup(val channelId: Long, val programStartMs: Long, val programEndMs: Long) : PlaybackSource()

    /** A user-pasted direct URL ("Open network stream"), resolved from the [com.arashrahimi46.iptv.data.model.DirectStream]
     *  history row [streamId]. No channel/EPG/favorites context -- just plays the row's URL, seekable. */
    data class DirectStream(val streamId: Long) : PlaybackSource()
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
    /** Authoritative duration we already know (recordings: the measured wall-clock length). Overrides
     * ExoPlayer's own estimate, which is unreliable for a raw recorded live-TS (PTS resets on ad
     * loops make it read a garbage duration and stop after ~one loop). Null => trust the player. */
    val knownDurationMs: Long? = null,
)

/** One upcoming/now-playing programme for the currently-playing channel's mini up-next panel. */
data class UpNextProgram(val title: String, val startMs: Long, val endMs: Long, val isNow: Boolean)

/**
 * Catch-up playback context (docs/catchup-v1-design.md, D7/D2/D11): non-null only while an archive
 * programme is playing. Drives the player HUD's ⟲ CATCH-UP pill, "aired …" label, Go Live button,
 * and ⏮/⏭ program-hop. [previous]/[next] are the immediate aired neighbours (start,end epoch ms) on
 * the same channel within the archive window, or null at an edge (no hop that way).
 */
data class CatchupPlayback(
    val channelId: Long,
    val programStartMs: Long,
    val programEndMs: Long,
    val programTitle: String?,
    val previous: Pair<Long, Long>? = null,
    val next: Pair<Long, Long>? = null,
)

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
    /** Continue-watching key for a local recording (Live TV Recording V1). */
    val resumeRecordingId: Long? = null,
    /** True when the current media came from a resolve-on-play source (Stalker): its `streamUrl` is a
     * short-lived minted link, so a manual Retry re-mints it ([reload]) instead of replaying a stale URL. */
    val resolveOnPlay: Boolean = false,
    /** Non-null only while a catch-up/archive programme plays -- drives the player's ⟲ pill, aired
     * label, Go Live button and ⏮/⏭ program-hop. Null for live/VOD/recording playback. */
    val catchup: CatchupPlayback? = null,
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
    private val recordingRepository = com.arashrahimi46.iptv.data.repository.RecordingRepository(app)
    private val recordingStorage = com.arashrahimi46.iptv.data.recording.RecordingStorage(app)
    private val settings = UserSettings(app)
    // Resolve-on-play seam (Stalker Phase 3): identity for M3U/Xtream, create_link for Stalker.
    private val streamResolver = DefaultStreamUrlResolver(CredentialsStore(app))

    /** The source currently loaded — kept so a manual retry can re-mint a stale Stalker link ([reload]). */
    private var currentSource: PlaybackSource? = null

    /** Message from the last failed resolve-on-play (Stalker portal outage), surfaced instead of the
     * generic "content not found" when a resolve rather than a missing row is why nothing loaded. */
    private var lastResolveError: String? = null

    /** Whether the last resolve went through the Stalker create_link path (feeds [LivePlayerUiState.resolveOnPlay]). */
    private var lastResolveOnPlay: Boolean = false

    private val _uiState = MutableStateFlow(LivePlayerUiState())
    val uiState: StateFlow<LivePlayerUiState> = _uiState.asStateFlow()

    private val _upNext = MutableStateFlow<List<UpNextProgram>>(emptyList())
    val upNext: StateFlow<List<UpNextProgram>> = _upNext.asStateFlow()

    /** Live TV Recording (V1): owns the capture tee + state machine for THIS player session. The tee
     * is wired into the ExoPlayer's DataSource.Factory by the screen ([recordingSink]); recording is
     * bound to this player's lifecycle, so leaving the player finalizes cleanly (see [onCleared]). */
    val recordingSupervisor = RecordingSupervisor(app)

    /** The media3 DataSink the screen tees playback bytes into (no-op until a recording starts). */
    val recordingSink: androidx.media3.datasource.DataSink get() = recordingSupervisor

    val recordingState: StateFlow<RecordingSupervisor.RecordingState> = recordingSupervisor.state

    /** Ticks 1/s while recording. Kept separate from [recordingState] so only the badge that renders
     *  it recomposes -- see the note on [RecordingSupervisor.RecordingState]. */
    val recordingElapsedMs: StateFlow<Long> = recordingSupervisor.elapsedMs

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
    /** Adds the currently-playing live channel to the curated multi-view list (scoped to its
     * source), evicting the oldest when the list is already full. No-op for VOD (no channel id). */
    fun addCurrentChannelToMultiView() {
        val channelId = _uiState.value.currentChannelId ?: return
        viewModelScope.launch {
            val channel = db.channelDao().getById(channelId) ?: return@launch
            settings.addMultiViewChannel(channel.sourceId, channelId, MultiViewViewModel.MAX_CHANNELS)
        }
    }

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

    /**
     * The one resolve-on-play call site (Stalker Phase 3). For M3U/Xtream this is pure identity — it
     * returns the row's precomputed [storedUrl] unchanged. For Stalker it mints a fresh session URL
     * from the stored portal `cmd` ([externalId]) via `create_link`. Returns null (and records
     * [lastResolveError]) when nothing playable can be produced, so the caller shows a clear error
     * instead of handing ExoPlayer a blank URL. [sourceId] null / row missing ⇒ not playable.
     */
    private suspend fun resolvePlayUrl(
        sourceId: Long?,
        kind: StreamKind,
        externalId: String?,
        storedUrl: String?,
        series: Int? = null,
        catchup: com.arashrahimi46.iptv.data.player.CatchupRequest? = null,
    ): String? {
        val source = sourceId?.let { db.playlistSourceDao().getById(it) } ?: return storedUrl?.takeIf { it.isNotBlank() }
        // A catch-up URL is minted fresh at press-time (like Stalker), so a manual Retry should re-mint
        // it via [reload] rather than replay a possibly-stale link.
        lastResolveOnPlay = source.type == SourceType.STALKER || catchup != null
        return try {
            streamResolver.resolve(source, kind, externalId, storedUrl, series, catchup).takeIf { it.isNotBlank() }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            lastResolveError = e.message
            null
        }
    }

    /** Re-run the load for the current source. Used by the player's manual Retry so a Stalker source
     *  re-mints its short-lived link (resolve-on-play) instead of replaying a URL that may have expired. */
    fun reload() {
        currentSource?.let { loadMedia(it) }
    }

    private fun loadMedia(source: PlaybackSource) {
        currentSource = source
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, errorMessage = null)
            lastResolveError = null
            lastResolveOnPlay = false
            val media = when (val s = source) {
                is PlaybackSource.Channel -> db.channelDao().getById(s.channelId)?.let { ch ->
                    resolvePlayUrl(ch.sourceId, StreamKind.LIVE, ch.externalId, ch.streamUrl)?.let { url ->
                        PlayableMedia(title = ch.name, subtitle = ch.categoryName, streamUrl = url, isLive = true)
                    }
                }
                is PlaybackSource.Vod -> db.vodTitleDao().getById(s.vodTitleId)?.let { title ->
                    resolvePlayUrl(title.sourceId, StreamKind.VOD, title.externalId, title.streamUrl)?.let { url ->
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
                    // Episodes match on the parent series name + S/E, not the (often per-episode) title.
                    val parent = db.vodTitleDao().getById(episode.seriesTitleId)
                    resolvePlayUrl(parent?.sourceId, StreamKind.SERIES, episode.externalId, episode.streamUrl, episode.episode)?.let { url ->
                        PlayableMedia(
                            title = episode.name,
                            subtitle = "S${episode.season} · E${episode.episode}",
                            streamUrl = url,
                            isLive = false,
                            searchName = parent?.name,
                            season = episode.season,
                            episode = episode.episode,
                        )
                    }
                }
                // A local recording plays seekably from disk (content:// URI) like VOD. // monolean:
                // plays the first part only; multi-part (>3.9 GB) stitching is a deferred follow-up.
                is PlaybackSource.LocalRecording -> recordingRepository.getById(s.recordingId)?.let { rec ->
                    runCatching { Uri.parse(rec.storageTreeUri) }.getOrNull()?.let { treeUri ->
                        PlayableMedia(
                            title = rec.channelName,
                            subtitle = rec.programTitle,
                            streamUrl = recordingStorage.documentUri(treeUri, rec.documentId).toString(),
                            isLive = false,
                            // Trust our measured record length over ExoPlayer's TS estimate.
                            knownDurationMs = rec.durationMs?.takeIf { it > 0 },
                        )
                    }
                }
                // Catch-up: mint the archive URL from the channel's source for the aired programme
                // window, then play it seekably (isLive = false => VOD/timeshift seek path).
                is PlaybackSource.Catchup -> db.channelDao().getById(s.channelId)?.let { ch ->
                    val request = com.arashrahimi46.iptv.data.player.CatchupRequest(
                        startMs = s.programStartMs,
                        endMs = s.programEndMs,
                        m3uSource = ch.catchupSource,
                        m3uType = ch.catchupType,
                    )
                    resolvePlayUrl(ch.sourceId, StreamKind.LIVE, ch.externalId, ch.streamUrl, catchup = request)?.let { url ->
                        PlayableMedia(title = ch.name, subtitle = ch.categoryName, streamUrl = url, isLive = false)
                    }
                }
                // A pasted direct URL, played seekably like VOD. Bump its history recency so
                // replaying an existing box floats it to the top of the Streams list.
                is PlaybackSource.DirectStream -> db.directStreamDao().getById(s.streamId)?.let { ds ->
                    db.directStreamDao().touch(ds.id, System.currentTimeMillis())
                    PlayableMedia(
                        title = directStreamLabel(ds.url, ds.name),
                        subtitle = null,
                        streamUrl = ds.url,
                        isLive = false,
                    )
                }
            }
            // Analytics: one play event per resolved media -- answers "which channels/titles get
            // watched most". Coarse type bucket + name + category; no-ops when analytics is off.
            if (media != null) {
                val type = when (source) {
                    is PlaybackSource.Channel -> "live"
                    is PlaybackSource.Vod -> "vod"
                    is PlaybackSource.Episode -> "episode"
                    is PlaybackSource.LocalRecording -> "recording"
                    is PlaybackSource.DirectStream -> "direct"
                    is PlaybackSource.Catchup -> "catchup"
                }
                com.arashrahimi46.iptv.analytics.Analytics.logPlay(type, media.title, media.subtitle)
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
                is PlaybackSource.LocalRecording -> null to null
                is PlaybackSource.DirectStream -> null to null
                // Catch-up favorites the underlying live channel.
                is PlaybackSource.Catchup -> s.channelId to ContentType.LIVE
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
            val resumeRecordingId = (source as? PlaybackSource.LocalRecording)?.recordingId
            // Catch-up HUD context: the archive programme's title + its immediate aired neighbours on
            // the same channel (for ⏮/⏭ hop). A neighbour is a valid hop target only if it too is
            // within the archive window and has already aired -- reuse the Guide's glyph predicate.
            val catchupPlayback = if (source is PlaybackSource.Catchup && media != null) {
                db.channelDao().getById(source.channelId)?.let { ch ->
                    val nowMs = System.currentTimeMillis()
                    val progs = db.epgProgramDao().programsForChannelInWindow(
                        ch.id, source.programStartMs - 86_400_000L, source.programEndMs + 86_400_000L,
                    )
                    val idx = progs.indexOfFirst { source.programStartMs >= it.startMs && source.programStartMs < it.endMs }
                    val prev = progs.getOrNull(idx - 1)?.takeIf { idx > 0 && com.arashrahimi46.iptv.ui.guide.isCatchupEligible(ch.catchupDays, it.startMs, nowMs) }
                    val next = progs.getOrNull(idx + 1)?.takeIf { idx >= 0 && com.arashrahimi46.iptv.ui.guide.isCatchupEligible(ch.catchupDays, it.startMs, nowMs) }
                    CatchupPlayback(
                        channelId = ch.id,
                        programStartMs = source.programStartMs,
                        programEndMs = source.programEndMs,
                        programTitle = progs.getOrNull(idx)?.title,
                        previous = prev?.let { it.startMs to it.endMs },
                        next = next?.let { it.startMs to it.endMs },
                    )
                }
            } else {
                null
            }
            _uiState.value = _uiState.value.copy(
                media = media,
                loading = false,
                errorMessage = if (media != null) null
                    else lastResolveError ?: getApplication<Application>().getString(R.string.player_content_not_found),
                phase = if (media == null) PlaybackPhase.Error else PlaybackPhase.Idle,
                currentChannelId = (source as? PlaybackSource.Channel)?.channelId,
                siblingChannelIds = siblingIds,
                currentEpisodeId = (source as? PlaybackSource.Episode)?.episodeId,
                siblingEpisodeIds = siblingEpisodeIds,
                favoriteTargetId = favoriteId,
                favoriteContentType = favoriteType,
                resumeVodTitleId = if (media == null) null else resumeVodId,
                resumeEpisodeId = if (media == null) null else resumeEpisodeId,
                resumeRecordingId = if (media == null) null else resumeRecordingId,
                resolveOnPlay = media != null && lastResolveOnPlay,
                catchup = catchupPlayback,
            )
        }
    }

    /** D7: leave catch-up and jump to the live edge -- plays the underlying live channel in place
     *  (loadMedia rebuilds the ExoPlayer on the live URL through the same screen/ViewModel). */
    fun goLive() {
        val channelId = _uiState.value.catchup?.channelId ?: return
        loadMedia(PlaybackSource.Channel(channelId))
    }

    /** D2/D11: hop to the previous ([direction] < 0) or next aired programme on the same channel,
     *  within the archive window. No-op at an edge (the button is hidden there anyway). */
    fun hopCatchupProgram(direction: Int) {
        val c = _uiState.value.catchup ?: return
        val target = (if (direction < 0) c.previous else c.next) ?: return
        loadMedia(PlaybackSource.Catchup(c.channelId, target.first, target.second))
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

    /**
     * Whether the currently-playing live channel can be recorded. V1 captures progressive `.ts`
     * (the tee records exactly what plays), so a plain `.ts`/extensionless stream is recordable
     * as-is. An Xtream HLS (`/live/…/<id>.m3u8`) channel is ALSO recordable because the identical
     * stream is served as raw MPEG-TS at `…/<id>.ts` -- pressing REC switches playback to that
     * `.ts` variant just for the recording session (see [startRecording]). A non-Xtream `.m3u8`
     * (an M3U/HLS-only source with no `.ts` sibling) stays non-recordable.
     */
    val isCurrentStreamRecordable: Boolean
        get() {
            val media = _uiState.value.media ?: return false
            if (!media.isLive || _uiState.value.currentChannelId == null) return false
            return !isHls(media.streamUrl) || xtreamTsVariant(media.streamUrl) != null
        }

    /** treeUri awaiting a playback switch to `.ts` before capture begins (Option B handshake). Null
     * when no record is pending. The screen calls [beginPendingRecording] once the `.ts` variant is
     * actually playing, so the file never captures a garbage HLS prefix. */
    private val _pendingRecord = MutableStateFlow<Uri?>(null)
    val pendingRecord: StateFlow<Uri?> = _pendingRecord.asStateFlow()
    private var pendingOnResult: ((RecordingSupervisor.StartResult) -> Unit)? = null
    /** Original HLS URL to restore if the `.ts` variant won't play (server is HLS-only / off-air),
     * so pressing REC never leaves the user stuck on a broken stream. */
    private var pendingRecordOriginalUrl: String? = null

    /** Picker-free storage destinations (internal + any mounted USB/SD) offered by the TV volume
     * picker, since SAF's folder picker has no handler on Android TV. */
    fun availableVolumes(): List<com.arashrahimi46.iptv.data.recording.RecordingStorage.Volume> =
        recordingStorage.availableVolumes()

    /**
     * Begin recording the current live channel into [treeUri]. If the channel is playing an Xtream
     * HLS stream, playback is first switched to its `.ts` variant and capture is deferred until that
     * variant is actually live ([beginPendingRecording]); an already-progressive stream records
     * immediately. [onResult] reports pre-flight/creation success or the failure reason to the screen.
     */
    fun startRecording(treeUri: Uri, onResult: (RecordingSupervisor.StartResult) -> Unit) {
        val channelId = _uiState.value.currentChannelId ?: return
        val media = _uiState.value.media ?: return
        val tsVariant = xtreamTsVariant(media.streamUrl)
        if (isHls(media.streamUrl) && tsVariant != null) {
            // Switch this channel's playback to the .ts variant (rebuilds the player), then wait for
            // it to actually play before starting the tee -- so the .ts file starts on a real TS byte.
            pendingOnResult = onResult
            pendingRecordOriginalUrl = media.streamUrl
            _pendingRecord.value = treeUri
            _uiState.value = _uiState.value.copy(media = media.copy(streamUrl = tsVariant))
        } else {
            // Already progressive (plain .ts / extensionless): record what's playing now.
            val program = _upNext.value.firstOrNull { it.isNow }?.title
            viewModelScope.launch {
                val sourceId = db.channelDao().getById(channelId)?.sourceId
                onResult(recordingSupervisor.start(channelId, media.title, program, treeUri, isHls = false, sourceId = sourceId))
            }
        }
    }

    /** Called by the screen once the `.ts` variant is confirmed playing -- actually starts capture. */
    fun beginPendingRecording() {
        val treeUri = _pendingRecord.value ?: return
        _pendingRecord.value = null
        val onResult = pendingOnResult
        pendingOnResult = null
        pendingRecordOriginalUrl = null
        val channelId = _uiState.value.currentChannelId ?: return
        val media = _uiState.value.media ?: return
        val program = _upNext.value.firstOrNull { it.isNow }?.title
        viewModelScope.launch {
            val sourceId = db.channelDao().getById(channelId)?.sourceId
            val result = recordingSupervisor.start(channelId, media.title, program, treeUri, isHls = false, sourceId = sourceId)
            onResult?.invoke(result)
        }
    }

    /** Called by the screen when the `.ts` variant fails to play (error or timeout): restore the
     * original HLS stream so the user keeps watching, and report that recording couldn't start. */
    fun abandonPendingRecording() {
        if (_pendingRecord.value == null) return
        _pendingRecord.value = null
        val onResult = pendingOnResult
        pendingOnResult = null
        pendingRecordOriginalUrl?.let { orig ->
            _uiState.value.media?.let { m -> _uiState.value = _uiState.value.copy(media = m.copy(streamUrl = orig)) }
        }
        pendingRecordOriginalUrl = null
        onResult?.invoke(RecordingSupervisor.StartResult.Failed(RecordingSupervisor.FailReason.HLS_STREAM))
    }

    fun stopRecording() = recordingSupervisor.stop()

    private fun isHls(url: String): Boolean = url.contains(".m3u8", ignoreCase = true)

    /**
     * The raw MPEG-TS (`.ts`) sibling of an Xtream live HLS URL, or null when [url] isn't a
     * derivable Xtream live stream. Xtream serves the same live stream at both
     * `…/live/<user>/<pass>/<id>.m3u8` and `…/live/<user>/<pass>/<id>.ts`; only that shape is
     * converted, so plain M3U/HLS-only sources (no `.ts` sibling) stay non-recordable.
     */
    private fun xtreamTsVariant(url: String): String? {
        val base = url.substringBefore('?')
        if (!base.endsWith(".m3u8", ignoreCase = true)) return null
        if (!base.contains("/live/", ignoreCase = true)) return null
        return base.dropLast(".m3u8".length) + ".ts"
    }

    /** Leaving the player finalizes any in-progress recording cleanly (COMPLETED) -- the accepted
     * "record while you watch" lifecycle. Runs on the supervisor's own scope, so it survives this
     * ViewModel's scope being cancelled. */
    override fun onCleared() {
        recordingSupervisor.stop()
        super.onCleared()
    }

    /** P1.2: saved VOD resume position (ms) for whatever is currently playing, or 0 for live
     * channels / titles with no bookmark. The screen seeks a freshly-built ExoPlayer to this. */
    suspend fun resumePositionMs(): Long {
        val state = _uiState.value
        if (state.resumeVodTitleId == null && state.resumeEpisodeId == null && state.resumeRecordingId == null) return 0L
        return continueWatchingRepository.resumePositionFor(state.resumeVodTitleId, state.resumeEpisodeId, state.resumeRecordingId)
    }

    /** P1.2: persists the VOD watch bookmark. No-op for live channels (no resume keys) and for
     * a not-yet-advanced position, so a still-buffering title never overwrites a real bookmark.
     * Once playback passes [COMPLETION_THRESHOLD] the title counts as finished: the bookmark is
     * cleared instead of saved, so a watched movie/episode drops off Continue Watching rather
     * than lingering with a full progress bar (and resuming from its very end). */
    fun saveProgress(positionMs: Long, durationMs: Long) {
        val state = _uiState.value
        if (state.resumeVodTitleId == null && state.resumeEpisodeId == null && state.resumeRecordingId == null) return
        if (positionMs <= 0) return
        val finished = durationMs > 0 && positionMs.toFloat() / durationMs >= COMPLETION_THRESHOLD
        viewModelScope.launch {
            if (finished) {
                continueWatchingRepository.clear(state.resumeVodTitleId, state.resumeEpisodeId, state.resumeRecordingId)
            } else {
                continueWatchingRepository.updateProgress(state.resumeVodTitleId, state.resumeEpisodeId, positionMs, durationMs, state.resumeRecordingId)
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
