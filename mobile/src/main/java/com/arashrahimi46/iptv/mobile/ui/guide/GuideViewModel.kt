package com.arashrahimi46.iptv.mobile.ui.guide

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arashrahimi46.iptv.data.db.AppDatabase
import com.arashrahimi46.iptv.data.model.Channel
import com.arashrahimi46.iptv.data.model.EPGProgram
import com.arashrahimi46.iptv.data.model.PlaylistSource
import com.arashrahimi46.iptv.data.repository.EpgAvailability
import com.arashrahimi46.iptv.data.repository.EpgRepository
import com.arashrahimi46.iptv.data.repository.PlaylistRepository
import com.arashrahimi46.iptv.data.repository.PlaylistRepositoryImpl
import com.arashrahimi46.iptv.mobile.R
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

/** Day chip selection (mirrors :tv's Guide -- Yesterday/Today/Tomorrow), each a 6-hour rolling
 * window from the current hour on that date. */
enum class GuideDay(@StringRes val labelRes: Int, val dayOffset: Long) {
    Yesterday(R.string.guide_day_yesterday, -1),
    Today(R.string.guide_day_today, 0),
    Tomorrow(R.string.guide_day_tomorrow, 1),
}

data class GuideProgramSlot(
    val title: String,
    val startMs: Long,
    val endMs: Long,
    val isNow: Boolean,
)

/** A programme-less row still renders -- a single "no programme data" placeholder slot. */
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
)

/**
 * TV Guide state for the touch UI: real EPG rows for a 6-hour rolling window, sourced from
 * [EpgRepository] (XMLTV or Xtream short-EPG) -- same data pipeline as :tv's GuideViewModel, laid
 * out as a scrollable channel list instead of a D-pad timeline grid. Channels with no programme
 * data for the window still get a row with a placeholder slot rather than being dropped.
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
                val group = resolveGuideGroup(rawGroup, groups)
                val window = windowFor(day)
                val channels = repository.channelsByCategory(source.id, group, GUIDE_CHANNEL_LIMIT)
                maybeRefreshEpg(source, group, channels)
                val ids = channels.map { it.id }
                val programsFlow = if (ids.isEmpty()) flowOf(emptyList()) else epgRepository.observeForChannels(ids, window.first, window.second)
                programsFlow.map { programs -> GuideEmission(day, group, groups, buildRows(channels, programs, window)) }
            }
            .collectLatest { e ->
                _uiState.update {
                    it.copy(hasSource = true, groups = e.groups, selectedGroup = e.group, day = e.day, rows = e.rows)
                }
            }
    }

    private fun maybeRefreshEpg(source: PlaylistSource, category: String, channels: List<Channel>) {
        if (channels.isEmpty()) return
        if (!refreshedCategories.add("${source.id}:$category")) return
        viewModelScope.launch { epgRepository.refresh(source, channels) }
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
            val slots = byChannel[channel.id].orEmpty()
                .sortedBy { it.startMs }
                .map { p ->
                    GuideProgramSlot(
                        title = p.title,
                        startMs = p.startMs.coerceAtLeast(window.first),
                        endMs = p.endMs.coerceAtMost(window.second),
                        isNow = nowMs in p.startMs until p.endMs,
                    )
                }
                .filter { it.endMs > it.startMs }
            GuideChannelRow(
                channel = channel,
                slots = slots.ifEmpty {
                    listOf(GuideProgramSlot(title = getApplication<Application>().getString(R.string.player_no_programme_data), startMs = window.first, endMs = window.second, isNow = false))
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

    companion object {
        /** Max channels rendered in the guide list -- bounds memory + EPG fetch on large catalogs. */
        private const val GUIDE_CHANNEL_LIMIT = 300
    }
}
