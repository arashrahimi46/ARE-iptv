package com.arashrahimi46.iptv.mobile.ui.guide

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arashrahimi46.iptv.core.R as CoreR
import com.arashrahimi46.iptv.mobile.data.db.AppDatabase
import com.arashrahimi46.iptv.mobile.data.model.Channel
import com.arashrahimi46.iptv.mobile.data.model.EPGProgram
import com.arashrahimi46.iptv.mobile.data.model.PlaylistSource
import com.arashrahimi46.iptv.mobile.data.repository.EpgAvailability
import com.arashrahimi46.iptv.mobile.data.repository.EpgRepository
import com.arashrahimi46.iptv.mobile.data.repository.PlaylistRepository
import com.arashrahimi46.iptv.mobile.data.repository.PlaylistRepositoryImpl
import com.arashrahimi46.iptv.mobile.data.settings.ParentalFilter
import com.arashrahimi46.iptv.mobile.data.settings.UserSettings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId

/** Day selection (Yesterday/Today/Tomorrow), each a 12-hour rolling window from the current hour on
 * that date. Twelve rather than six so the agenda's "now + next two" is always populated even for a
 * channel running long programmes. */
enum class GuideDay(@StringRes val labelRes: Int, val dayOffset: Long) {
    Yesterday(CoreR.string.guide_day_yesterday, -1),
    Today(CoreR.string.guide_day_today, 0),
    Tomorrow(CoreR.string.guide_day_tomorrow, 1),
}

/**
 * One programme in the agenda. Times are the programme's REAL start/end -- the phone guide is a
 * vertical list, not a time-axis grid, so nothing is clamped to the window any more.
 *
 * [catchup] means the provider's archive window still covers this (already-finished) programme, so
 * a "watch from start" action is meaningful. [placeholder] marks the synthetic "no programme data"
 * row a channel gets when its EPG is empty -- it is not tappable.
 */
data class GuideProgramSlot(
    val id: Long,
    val title: String,
    val description: String?,
    val startMs: Long,
    val endMs: Long,
    val isNow: Boolean,
    val catchup: Boolean,
    val placeholder: Boolean = false,
)

/** A programme-less channel still renders -- a single "no programme data" placeholder slot. */
data class GuideChannelRow(val channel: Channel, val slots: List<GuideProgramSlot>)

/** Pure resolution of the effective category filter: falls back to the first available category
 * when [rawGroup] isn't (or is no longer) present in [groups]. Top-level so it's unit-testable. */
internal fun resolveGuideGroup(rawGroup: String, groups: List<String>): String =
    if (rawGroup in groups) rawGroup else groups.firstOrNull() ?: rawGroup

data class GuideUiState(
    val hasSource: Boolean = false,
    val groups: List<String> = emptyList(),
    val selectedGroup: String = "",
    val day: GuideDay = GuideDay.Today,
    val rows: List<GuideChannelRow> = emptyList(),
    /** True once [EpgRepository.refresh] finishes and its source couldn't be reached/parsed at
     * all -- distinct from "fetched fine, genuinely no programme data" (false in that case). */
    val epgUnavailable: Boolean = false,
    /** Drives the pull-to-refresh indicator; set only by [GuideViewModel.refresh]. */
    val isRefreshing: Boolean = false,
    /**
     * True until the first real emission resolves. Without it the initial state is
     * indistinguishable from two genuine failures -- `hasSource = false` rendered "no playlist
     * source" and `rows = []` rendered "nothing in this category" -- so opening the Guide flashed
     * two wrong error screens before the data it already had in Room arrived.
     */
    val isLoading: Boolean = true,
)

/**
 * TV Guide state for the touch UI: real EPG rows for a 12-hour rolling window, sourced from
 * [EpgRepository] (XMLTV or Xtream short-EPG). The UI renders this as a vertical now/next agenda;
 * this class stays a pure data provider and knows nothing about that layout.
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

    /** EPG already fetched for these "sourceId:category" pairs this session -- so switching back
     * to a category doesn't re-download the source's XMLTV export every time. */
    private val refreshedCategories = mutableSetOf<String>()

    /** What pull-to-refresh re-fetches: the source + channel set behind the newest emission. */
    private var currentSource: PlaylistSource? = null
    private var currentChannels: List<Channel> = emptyList()
    private var currentCategory: String = ""

    init {
        viewModelScope.launch {
            _selectedGroup.value = settings.guideSelectedCategory.first()
        }
        viewModelScope.launch {
            epgRepository.availability.collectLatest { availability ->
                _uiState.update { it.copy(epgUnavailable = availability is EpgAvailability.Unavailable) }
            }
        }
        viewModelScope.launch {
            // The guide is a full channel browser of its own, so the parental lock has to reach it
            // too -- otherwise an adult category hidden everywhere else is still one tab away here.
            combine(settings.activeSourceId, settings.parentalFilter) { sourceId, parental ->
                sourceId to parental
            }.collectLatest { (sourceId, parental) ->
                if (sourceId == null) {
                    _uiState.value = GuideUiState(hasSource = false, isLoading = false)
                    return@collectLatest
                }
                val source = db.playlistSourceDao().getById(sourceId)
                if (source == null) {
                    _uiState.value = GuideUiState(hasSource = false, isLoading = false)
                    return@collectLatest
                }
                repository.channelCategoryCounts(sourceId).collectLatest { counts ->
                    val groups = counts.map { it.name }.filterNot { parental.hidden(it) }
                    if (groups.isEmpty()) {
                        _uiState.value = GuideUiState(hasSource = true, isLoading = false)
                        return@collectLatest
                    }
                    observeRows(source, groups, parental)
                }
            }
        }
    }

    private suspend fun observeRows(source: PlaylistSource, groups: List<String>, parental: ParentalFilter) {
        combine(_day, _selectedGroup) { day, group -> day to group }
            .flatMapLatest { (day, rawGroup) ->
                // [groups] is already parental-filtered, so a persisted adult selection is no longer
                // a member and resolveGuideGroup falls back to the first visible one.
                val group = resolveGuideGroup(rawGroup, groups)
                val window = windowFor(day)
                val channels = repository.channelsByCategory(source.id, group, GUIDE_CHANNEL_LIMIT)
                    .filterNot { parental.hidden(it.categoryName) }
                currentSource = source
                currentChannels = channels
                currentCategory = group
                maybeRefreshEpg(source, group, channels)
                val ids = channels.map { it.id }
                val programsFlow = if (ids.isEmpty()) flowOf(emptyList()) else epgRepository.observeForChannels(ids, window.first, window.second)
                programsFlow.map { programs -> GuideEmission(day, group, groups, buildRows(channels, programs, window)) }
            }
            .collectLatest { e ->
                _uiState.update {
                    it.copy(
                        hasSource = true,
                        groups = e.groups,
                        selectedGroup = e.group,
                        day = e.day,
                        rows = e.rows,
                        isLoading = false,
                    )
                }
            }
    }

    private fun maybeRefreshEpg(source: PlaylistSource, category: String, channels: List<Channel>) {
        if (channels.isEmpty()) return
        if (!refreshedCategories.add("${source.id}:$category")) return
        viewModelScope.launch { epgRepository.refresh(source, channels) }
    }

    /**
     * Pull-to-refresh: force a re-fetch of the XMLTV / short-EPG for the category on screen, even
     * though this session already fetched it. The row flow is a Room query, so the new programmes
     * arrive through the existing collector -- nothing here pushes rows.
     */
    fun refresh() {
        val source = currentSource ?: return
        val channels = currentChannels
        if (channels.isEmpty()) return
        if (_uiState.value.isRefreshing) return
        _uiState.update { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            try {
                refreshedCategories.add("${source.id}:$currentCategory")
                epgRepository.refresh(source, channels)
            } finally {
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    private data class GuideEmission(
        val day: GuideDay,
        val group: String,
        val groups: List<String>,
        val rows: List<GuideChannelRow>,
    )

    private fun buildRows(channels: List<Channel>, programs: List<EPGProgram>, window: Pair<Long, Long>): List<GuideChannelRow> {
        val nowMs = System.currentTimeMillis()
        val byChannel = programs.groupBy { it.channelId }
        return channels.map { channel ->
            val archiveFloor = if (channel.catchupDays > 0) nowMs - channel.catchupDays * DAY_MS else Long.MAX_VALUE
            val slots = byChannel[channel.id].orEmpty()
                .sortedBy { it.startMs }
                .filter { it.endMs > it.startMs }
                .map { p ->
                    GuideProgramSlot(
                        id = p.id,
                        title = p.title,
                        description = p.description?.takeIf { it.isNotBlank() },
                        startMs = p.startMs,
                        endMs = p.endMs,
                        isNow = nowMs in p.startMs until p.endMs,
                        catchup = p.endMs <= nowMs && p.startMs >= archiveFloor,
                    )
                }
            GuideChannelRow(
                channel = channel,
                slots = slots.ifEmpty {
                    listOf(
                        GuideProgramSlot(
                            id = -channel.id,
                            title = getApplication<Application>().getString(CoreR.string.player_no_programme_data),
                            description = null,
                            startMs = window.first,
                            endMs = window.second,
                            isNow = false,
                            catchup = false,
                            placeholder = true,
                        ),
                    )
                },
            )
        }
    }

    private fun windowFor(day: GuideDay): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val floorHour = LocalDateTime.now(zone).withMinute(0).withSecond(0).withNano(0).plusDays(day.dayOffset)
        val start = floorHour.atZone(zone).toInstant().toEpochMilli()
        val end = floorHour.plusHours(WINDOW_HOURS).atZone(zone).toInstant().toEpochMilli()
        return start to end
    }

    fun selectDay(day: GuideDay) {
        _day.value = day
    }

    fun selectGroup(group: String) {
        _selectedGroup.value = group
        viewModelScope.launch { settings.setGuideSelectedCategory(group) }
    }

    companion object {
        /** Max channels rendered in the guide list -- bounds memory + EPG fetch on large catalogs. */
        private const val GUIDE_CHANNEL_LIMIT = 300
        private const val WINDOW_HOURS = 12L
        private const val DAY_MS = 24L * 60L * 60L * 1000L
    }
}
