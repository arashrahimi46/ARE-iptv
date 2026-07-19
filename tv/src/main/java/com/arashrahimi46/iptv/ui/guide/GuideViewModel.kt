package com.arashrahimi46.iptv.ui.guide

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arashrahimi46.iptv.data.db.AppDatabase
import com.arashrahimi46.iptv.data.model.Channel
import com.arashrahimi46.iptv.data.model.EPGProgram
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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
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

data class GuideUiState(
    val hasSource: Boolean = false,
    val groups: List<String> = listOf("All"),
    val selectedGroup: String = "All",
    val day: GuideDay = GuideDay.Today,
    val windowStartMs: Long = 0L,
    val windowEndMs: Long = 0L,
    val rows: List<GuideChannelRow> = emptyList(),
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

    private var epgRefreshedForSource: Long? = null

    init {
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
                repository.observeChannels(sourceId).collectLatest { channels ->
                    if (channels.isEmpty()) {
                        _uiState.value = GuideUiState(hasSource = true)
                        return@collectLatest
                    }
                    if (epgRefreshedForSource != sourceId) {
                        epgRefreshedForSource = sourceId
                        launch { epgRepository.refresh(source, channels) }
                    }
                    observeRows(channels)
                }
            }
        }
    }

    private suspend fun observeRows(channels: List<Channel>) {
        val groups = listOf("All") + channels.mapNotNull { it.categoryName }.distinct().sorted()

        combine(_day, _selectedGroup) { day, group -> day to group }
            .flatMapLatest { (day, group) ->
                val window = windowFor(day)
                val visible = if (group == "All") channels else channels.filter { it.categoryName == group }
                val ids = visible.map { it.id }
                val programsFlow = if (ids.isEmpty()) flowOf(emptyList()) else epgRepository.observeForChannels(ids, window.first, window.second)
                programsFlow.map { programs -> Triple(day, group, buildRows(visible, programs, window)) }
            }
            .collectLatest { (day, group, rows) ->
                val window = windowFor(day)
                _uiState.value = GuideUiState(
                    hasSource = true,
                    groups = groups,
                    selectedGroup = group,
                    day = day,
                    windowStartMs = window.first,
                    windowEndMs = window.second,
                    rows = rows,
                )
            }
    }

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
    }

    fun setFocused(info: GuideFocusedInfo?) {
        _focused.value = info
    }

    companion object {
        fun factory(app: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = GuideViewModel(app) as T
            }
    }
}
