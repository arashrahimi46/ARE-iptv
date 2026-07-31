package com.arashrahimi46.iptv.ui.guide

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arashrahimi46.iptv.data.db.AppDatabase
import com.arashrahimi46.iptv.data.db.CategoryCount
import com.arashrahimi46.iptv.data.db.EpgSearchHit
import com.arashrahimi46.iptv.data.model.Channel
import com.arashrahimi46.iptv.data.model.EPGProgram
import com.arashrahimi46.iptv.data.model.PlaylistSource
import com.arashrahimi46.iptv.data.repository.EpgAvailability
import com.arashrahimi46.iptv.data.repository.EpgRepository
import com.arashrahimi46.iptv.data.repository.PlaylistRepository
import com.arashrahimi46.iptv.data.repository.PlaylistRepositoryImpl
import com.arashrahimi46.iptv.core.R
import com.arashrahimi46.iptv.data.settings.UserSettings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId

/** Day chip selection (Guide.jsx: Yesterday / Today / Tomorrow), each a 6-hour rolling window from the current hour on that date. */
enum class GuideDay(@StringRes val labelRes: Int, val dayOffset: Long) {
    Yesterday(R.string.guide_day_yesterday, -1),
    Today(R.string.guide_day_today, 0),
    Tomorrow(R.string.guide_day_tomorrow, 1),
}

data class GuideProgramSlot(
    val title: String,
    val description: String?,
    val startMs: Long,
    val endMs: Long,
    val isNow: Boolean,
    /** True *aired* programme bounds (unclamped by the visible window) — [startMs]/[endMs] are clamped
     * for grid layout, but catch-up must mint the archive URL from the real broadcast start/end. */
    val programStartMs: Long = startMs,
    val programEndMs: Long = endMs,
    /** Catch-up playable: the channel has an archive window, this programme has already started (aired
     * or now-airing, for Start-Over), and it falls inside that window. Drives the ⟲ glyph + action menu.
     * See docs/catchup-v1-design.md (D5/D6/D8). */
    val catchupEligible: Boolean = false,
    /** The synthetic "no programme data" filler, not a real broadcast. The screen recomputes
     * live/elapsed state from a ticking clock, and a placeholder spanning the current time would
     * otherwise light up as on-air with a progress bar. */
    val isPlaceholder: Boolean = false,
)

/** A programme-less row still renders -- a single "no programme data" placeholder slot spanning the window. */
data class GuideChannelRow(val channel: Channel, val slots: List<GuideProgramSlot>)

data class GuideFocusedInfo(val channel: Channel, val slot: GuideProgramSlot)

/**
 * A pending "show me this programme" navigation from the search dialog: the day/category have
 * already been switched, and the screen still has to scroll the grid to [channelId] at [startMs]
 * and land D-pad focus on that cell. Cleared via [GuideViewModel.consumeJump] once focus lands.
 */
data class GuideJump(val channelId: Long, val startMs: Long)

/**
 * Pure resolution of the effective category filter: falls back to the first available category
 * when [rawGroup] isn't (or is no longer) present in [groups] -- e.g. a persisted
 * [UserSettings.guideSelectedCategory] restored asynchronously in [GuideViewModel.init] for a
 * category that doesn't exist on the current source, or the legacy "All" default now that the
 * Guide is strictly per-category. Extracted as a top-level pure function so the race-condition
 * fix in [GuideViewModel.observeRows] is unit-testable without standing up Room/AndroidViewModel.
 */
internal fun resolveGuideGroup(rawGroup: String, groups: List<String>): String =
    if (rawGroup in groups) rawGroup else groups.firstOrNull() ?: rawGroup

/**
 * Whether a programme is catch-up-eligible (docs/catchup-v1-design.md, D6): the channel advertises an
 * archive window ([catchupDays] > 0), the programme has already started ([programStartMs] < [nowMs] —
 * which also covers the now-airing cell, for Start-Over), and its start is still inside that window.
 * Top-level + internal so the window arithmetic is unit-testable without standing up Room/AndroidViewModel.
 */
internal fun isCatchupEligible(catchupDays: Int, programStartMs: Long, nowMs: Long): Boolean =
    catchupDays > 0 && programStartMs < nowMs && programStartMs >= nowMs - catchupDays * 86_400_000L

data class GuideUiState(
    val hasSource: Boolean = false,
    val groups: List<String> = emptyList(),
    /** [groups] with their channel counts, for the category picker dialog's rows. */
    val groupCounts: List<CategoryCount> = emptyList(),
    /** Categories the user pinned in the picker -- floated to the top of its list. */
    val pinnedGroups: Set<String> = emptySet(),
    val selectedGroup: String = "",
    val day: GuideDay = GuideDay.Today,
    val windowStartMs: Long = 0L,
    val windowEndMs: Long = 0L,
    val rows: List<GuideChannelRow> = emptyList(),
    /** P0.4: true once [EpgRepository.refresh] finishes and its source couldn't be reached/parsed
     * at all -- distinct from "fetched fine, genuinely no programme data" (false in that case).
     * Drives a small banner in [com.arashrahimi46.iptv.ui.guide.GuideScreen], not a full-screen
     * replacement -- channel rows still render with their per-row "No programme data" placeholder. */
    val epgUnavailable: Boolean = false,
    /** A guide refresh is in flight. The grid keeps rendering the cached rows underneath -- this
     *  only drives the header's "Refreshing…" pill, so the update is visible but never blocking. */
    val refreshing: Boolean = false,
    /** Pending search navigation; see [GuideJump]. */
    val jump: GuideJump? = null,
)

/**
 * TV Guide state: real EPG rows for a 6-hour rolling window, sourced from
 * [EpgRepository] (XMLTV or Xtream short-EPG, see that class). Channels with
 * no programme data for the window still get a row with a placeholder slot
 * rather than being dropped.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GuideViewModel(app: Application) : AndroidViewModel(app) {
    private val repository: PlaylistRepository = PlaylistRepositoryImpl(app)
    private val epgRepository = EpgRepository(app)
    private val settings = UserSettings(app)
    private val db = AppDatabase.get(app)

    private val _day = MutableStateFlow(GuideDay.Today)
    private val _selectedGroup = MutableStateFlow("All")

    private val _uiState = MutableStateFlow(GuideUiState())
    val uiState: StateFlow<GuideUiState> = _uiState.asStateFlow()

    private val _focused = MutableStateFlow<GuideFocusedInfo?>(null)
    val focused: StateFlow<GuideFocusedInfo?> = _focused.asStateFlow()

    /**
     * Wall clock, re-sampled every 30s.
     *
     * The Guide had no clock at all: `isNow` was baked into each slot by [buildRows] from a single
     * `System.currentTimeMillis()` and only re-evaluated when Room re-emitted, so the "on now" cell
     * drifted out of date and nothing could animate. This drives the now-line, the elapsed fill and
     * the live cell. 30s is the coarsest tick that keeps the now-line's motion continuous rather
     * than steppy at the guide's dp-per-minute scale.
     */
    val nowMs: StateFlow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            kotlinx.coroutines.delay(30_000L)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), System.currentTimeMillis())

    /** EPG already fetched for these source ids this session. Keyed by SOURCE, not by
     * "sourceId:category" as it once was -- [EpgRepository.refresh] now matches the whole XMLTV
     * export in one pass, so a second category is already covered by the first fetch. */
    private val refreshedSources = mutableSetOf<Long>()

    /** The active source, for [searchProgrammes] -- which is a one-shot query, not part of the
     *  rows pipeline, so it needs the id without re-reading DataStore per keystroke. */
    private var activeSourceId: Long? = null

    init {
        // Restore the last-selected category filter (persisted in [UserSettings]) so
        // reopening the Guide keeps the user's chosen channel group instead of resetting.
        viewModelScope.launch {
            _selectedGroup.value = settings.guideSelectedCategory.first()
        }
        // P0.4: mirrors epgRepository.availability into epgUnavailable, independent of the
        // rows pipeline below -- a plain .copy() (not a full GuideUiState(...) replacement) so
        // it doesn't race the rows collector's own state writes; observeRows carries the
        // current value forward on every emission (see there) for the same reason.
        // Same .copy() discipline as the availability collector below -- an independent stream
        // folded into the shared state without clobbering the rows pipeline's writes.
        viewModelScope.launch {
            settings.pinnedCategories(PIN_NAMESPACE).collectLatest { pinned ->
                _uiState.update { it.copy(pinnedGroups = pinned) }
            }
        }
        viewModelScope.launch {
            epgRepository.availability.collectLatest { availability ->
                _uiState.update { it.copy(epgUnavailable = availability is EpgAvailability.Unavailable) }
            }
        }
        viewModelScope.launch {
            epgRepository.refreshing.collectLatest { busy ->
                _uiState.update { it.copy(refreshing = busy) }
            }
        }
        viewModelScope.launch {
            settings.activeSourceId.collectLatest { sourceId ->
                activeSourceId = sourceId
                if (sourceId == null) {
                    resetCatalog(hasSource = false)
                    return@collectLatest
                }
                val source = db.playlistSourceDao().getById(sourceId)
                if (source == null) {
                    resetCatalog(hasSource = false)
                    return@collectLatest
                }
                // Tabs are the source's FULL live-category list (a GROUP BY, no catalog rows
                // loaded) -- not the categories that happen to appear in a bounded channel
                // window, which showed only whichever category dominated the first N channels
                // by name.
                repository.channelCategoryCounts(sourceId).collectLatest { counts ->
                    val groups = counts.map { it.name }
                    if (groups.isEmpty()) {
                        resetCatalog(hasSource = true)
                        return@collectLatest
                    }
                    _uiState.update { it.copy(groupCounts = counts) }
                    observeRows(source, groups)
                }
            }
        }
    }

    /**
     * Clears everything the current source contributed, keeping the cross-source bits
     * ([GuideUiState.pinnedGroups], which is user preference, not catalog). Was a wholesale
     * `_uiState.value = GuideUiState(...)`, which dropped the pinned set on every source switch --
     * the pinned collector only re-emits when the *preference* changes, so it never came back.
     */
    private fun resetCatalog(hasSource: Boolean) {
        _uiState.update {
            GuideUiState(hasSource = hasSource, pinnedGroups = it.pinnedGroups)
        }
    }

    private suspend fun observeRows(source: PlaylistSource, groups: List<String>) {
        combine(_day, _selectedGroup) { day, group -> day to group }
            .flatMapLatest { (day, rawGroup) ->
                // A restored/previously-picked group that doesn't exist on this source (e.g. the
                // user switched playlists, or the persisted category from UserSettings finishes
                // loading -- asynchronously, in `init` -- only after this pipeline has already
                // started) falls back to the first category rather than rendering zero rows.
                val group = resolveGuideGroup(rawGroup, groups)
                val window = windowFor(day)
                // Only the selected category's channels are loaded (bounded) -- the guide is
                // strictly per-category now, so we never materialize the 100k+ catalog.
                val channels = repository.channelsByCategory(source.id, group, GUIDE_CHANNEL_LIMIT)
                maybeRefreshEpg(source, channels)
                val ids = channels.map { it.id }
                val programsFlow = if (ids.isEmpty()) flowOf(emptyList()) else epgRepository.observeForChannels(ids, window.first, window.second)
                programsFlow.map { programs -> GuideEmission(day, group, groups, window, buildRows(channels, programs, window)) }
            }
            .collectLatest { e ->
                // Atomic read-modify-write: epgUnavailable is carried forward from `it` inside
                // the same update lambda, so a concurrent availability write can't be lost.
                _uiState.update {
                    it.copy(
                        hasSource = true,
                        groups = e.groups,
                        selectedGroup = e.group,
                        day = e.day,
                        windowStartMs = e.window.first,
                        windowEndMs = e.window.second,
                        rows = e.rows,
                    )
                }
            }
    }

    /**
     * Fetches EPG for [source] at most once per session, and then only when the persisted cache has
     * aged out ([EpgRepository.isFresh]).
     *
     * Two guards, because they cover different things: [refreshedSources] stops a second category
     * in the same session from re-entering while the first fetch is still running, and `isFresh`
     * stops a cold start from re-downloading a multi-MB export that is still perfectly good. The
     * grid renders the cached rows either way -- this only ever adds to what is already on screen.
     *
     * [channels] is still passed through because the Xtream per-channel short-EPG fallback can only
     * work on a bounded set; the bulk XMLTV path inside [EpgRepository] ignores it and matches the
     * whole catalog.
     */
    private fun maybeRefreshEpg(source: PlaylistSource, channels: List<Channel>) {
        if (channels.isEmpty()) return
        if (!refreshedSources.add(source.id)) return
        viewModelScope.launch {
            if (epgRepository.isFresh(source.id)) return@launch
            epgRepository.refresh(source, channels)
        }
    }

    /**
     * Programme-title search across the whole cached guide for the active source (every category,
     * not just visited ones -- see [maybeRefreshEpg]). One-shot per query rather than a Flow: the
     * dialog debounces keystrokes itself and there is nothing to observe between them.
     */
    suspend fun searchProgrammes(query: String): List<EpgSearchHit> {
        val sourceId = activeSourceId ?: return emptyList()
        return epgRepository.searchProgrammes(sourceId, query)
    }

    /**
     * Navigate the grid to a search hit: switch to whichever day's window contains it and to its
     * channel's category, then hand the screen a [GuideJump] to scroll and focus with. Category and
     * day both have to move first -- the row simply isn't in [GuideUiState.rows] otherwise.
     */
    fun jumpTo(hit: EpgSearchHit) {
        hit.categoryName?.let { selectGroup(it) }
        _day.value = GuideDay.entries.firstOrNull { day ->
            val (start, end) = windowFor(day)
            hit.startMs < end && hit.endMs > start
        } ?: GuideDay.Today
        _uiState.update { it.copy(jump = GuideJump(hit.channelId, hit.startMs)) }
    }

    fun consumeJump() {
        _uiState.update { it.copy(jump = null) }
    }

    private data class GuideEmission(
        val day: GuideDay,
        val group: String,
        val groups: List<String>,
        val window: Pair<Long, Long>,
        val rows: List<GuideChannelRow>,
    )

    private fun buildRows(channels: List<Channel>, programs: List<EPGProgram>, window: Pair<Long, Long>): List<GuideChannelRow> {
        val nowMs = System.currentTimeMillis()
        val byChannel = programs.groupBy { it.channelId }
        return channels.map { channel ->
            val slots = byChannel[channel.id].orEmpty()
                .sortedBy { it.startMs }
                .map { p ->
                    GuideProgramSlot(
                        title = p.title,
                        description = p.description,
                        startMs = p.startMs.coerceAtLeast(window.first),
                        endMs = p.endMs.coerceAtMost(window.second),
                        isNow = nowMs in p.startMs until p.endMs,
                        programStartMs = p.startMs,
                        programEndMs = p.endMs,
                        catchupEligible = isCatchupEligible(channel.catchupDays, p.startMs, nowMs),
                    )
                }
                .filter { it.endMs > it.startMs }
            GuideChannelRow(
                channel = channel,
                slots = slots.ifEmpty {
                    // Cap the "no programme data" placeholder to a normal ~90-min cell rather than
                    // spanning the whole 6h window: a full-window cell is so wide that the focus
                    // scale-up overflows and gets cropped ("moves out of the box") at the guide
                    // lane's clip edge. A normal-width cell focuses cleanly like a real programme.
                    val placeholderEnd = (window.first + 90 * 60_000L).coerceAtMost(window.second)
                    listOf(GuideProgramSlot(title = getApplication<Application>().getString(R.string.player_no_programme_data), description = null, startMs = window.first, endMs = placeholderEnd, isNow = false, isPlaceholder = true))
                },
            )
        }
    }

    private fun windowFor(day: GuideDay): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val floorHour = LocalDateTime.now(zone).withMinute(0).withSecond(0).withNano(0).plusDays(day.dayOffset)
        val start = floorHour.atZone(zone).toInstant().toEpochMilli()
        val end = floorHour.plusHours(6).atZone(zone).toInstant().toEpochMilli()
        return start to end
    }

    fun selectDay(day: GuideDay) {
        _day.value = day
    }

    fun selectGroup(group: String) {
        _selectedGroup.value = group
        viewModelScope.launch { settings.setGuideSelectedCategory(group) }
    }

    fun togglePinnedGroup(group: String) {
        viewModelScope.launch { settings.togglePinnedCategory(PIN_NAMESPACE, group) }
    }

    fun setFocused(info: GuideFocusedInfo?) {
        _focused.value = info
    }

    companion object {
        /** Max channels rendered in the EPG grid -- bounds memory + EPG fetch on large catalogs. */
        private const val GUIDE_CHANNEL_LIMIT = 300

        /** Its own [UserSettings.pinnedCategories] namespace, matching Series/MultiView: the Guide's
         *  useful categories aren't necessarily the ones pinned for browsing Live TV. */
        private const val PIN_NAMESPACE = "guide"

        fun factory(app: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = GuideViewModel(app) as T
            }
    }
}
