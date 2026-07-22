package com.arashrahimi46.iptv.ui.guide

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arashrahimi46.iptv.data.db.AppDatabase
import com.arashrahimi46.iptv.data.model.Channel
import com.arashrahimi46.iptv.data.model.EPGProgram
import com.arashrahimi46.iptv.data.model.PlaylistSource
import com.arashrahimi46.iptv.data.repository.EpgAvailability
import com.arashrahimi46.iptv.data.repository.EpgRepository
import com.arashrahimi46.iptv.data.repository.PlaylistRepository
import com.arashrahimi46.iptv.data.repository.PlaylistRepositoryImpl
import com.arashrahimi46.iptv.data.settings.UserSettings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId

/** Day chip selection (Guide.jsx: Yesterday / Today / Tomorrow), each a 6-hour rolling window from the current hour on that date. */
enum class GuideDay(val label: String, val dayOffset: Long) {
    Yesterday("Yesterday", -1),
    Today("Today", 0),
    Tomorrow("Tomorrow", 1),
}

data class GuideProgramSlot(
    val title: String,
    val description: String?,
    val startMs: Long,
    val endMs: Long,
    val isNow: Boolean,
)

/** A programme-less row still renders -- a single "no programme data" placeholder slot spanning the window. */
data class GuideChannelRow(val channel: Channel, val slots: List<GuideProgramSlot>)

data class GuideFocusedInfo(val channel: Channel, val slot: GuideProgramSlot)

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

data class GuideUiState(
    val hasSource: Boolean = false,
    val groups: List<String> = emptyList(),
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

    /** EPG already fetched for these "sourceId:category" pairs this session -- so switching back
     * to a tab doesn't re-download the source's XMLTV export every time. */
    private val refreshedCategories = mutableSetOf<String>()

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
        viewModelScope.launch {
            epgRepository.availability.collectLatest { availability ->
                _uiState.update { it.copy(epgUnavailable = availability is EpgAvailability.Unavailable) }
            }
        }
        viewModelScope.launch {
            settings.activeSourceId.collectLatest { sourceId ->
                if (sourceId == null) {
                    _uiState.value = GuideUiState(hasSource = false)
                    return@collectLatest
                }
                val source = db.playlistSourceDao().getById(sourceId)
                if (source == null) {
                    _uiState.value = GuideUiState(hasSource = false)
                    return@collectLatest
                }
                // Tabs are the source's FULL live-category list (a GROUP BY, no catalog rows
                // loaded) -- not the categories that happen to appear in a bounded channel
                // window, which showed only whichever category dominated the first N channels
                // by name.
                repository.channelCategoryCounts(sourceId).collectLatest { counts ->
                    val groups = counts.map { it.name }
                    if (groups.isEmpty()) {
                        _uiState.value = GuideUiState(hasSource = true)
                        return@collectLatest
                    }
                    observeRows(source, groups)
                }
            }
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
                maybeRefreshEpg(source, group, channels)
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

    /** Fetches EPG for [category]'s [channels] once per session. monolean: re-downloads the
     * source's XMLTV export per distinct category visited -- upgrade path is caching the parsed
     * programmes by tvgId in [EpgRepository] so later categories reuse one fetch. */
    private fun maybeRefreshEpg(source: PlaylistSource, category: String, channels: List<Channel>) {
        if (channels.isEmpty()) return
        if (!refreshedCategories.add("${source.id}:$category")) return
        viewModelScope.launch { epgRepository.refresh(source, channels) }
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
                    )
                }
                .filter { it.endMs > it.startMs }
            GuideChannelRow(
                channel = channel,
                slots = slots.ifEmpty {
                    listOf(GuideProgramSlot(title = "No programme data", description = null, startMs = window.first, endMs = window.second, isNow = false))
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

    fun setFocused(info: GuideFocusedInfo?) {
        _focused.value = info
    }

    companion object {
        /** Max channels rendered in the EPG grid -- bounds memory + EPG fetch on large catalogs. */
        private const val GUIDE_CHANNEL_LIMIT = 300

        fun factory(app: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = GuideViewModel(app) as T
            }
    }
}
