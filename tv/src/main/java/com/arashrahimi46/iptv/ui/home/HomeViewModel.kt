package com.arashrahimi46.iptv.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arashrahimi46.iptv.data.model.Channel
import com.arashrahimi46.iptv.data.model.VodTitle
import com.arashrahimi46.iptv.data.repository.EpgRepository
import com.arashrahimi46.iptv.data.repository.PlaylistRepository
import com.arashrahimi46.iptv.data.repository.PlaylistRepositoryImpl
import com.arashrahimi46.iptv.data.settings.UserSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

data class HomeCategorySummary(val name: String, val count: Int)

data class HomeUiState(
    val hasSource: Boolean = false,
    val channels: List<Channel> = emptyList(),
    val movies: List<VodTitle> = emptyList(),
    val series: List<VodTitle> = emptyList(),
    val categories: List<HomeCategorySummary> = emptyList(),
    /** QA LOW defect: a real source existed, but Room hadn't emitted its first catalog read
     * yet (cold-start DB open on a large catalog took 1.5-7s in QA's test) -- [hasSource]'s
     * default-constructed `false` was indistinguishable from "confirmed no source", flashing
     * "No playlist yet" during that window. True only before the very first emission below;
     * every real emission (source or no-source) sets it false, whether or not the catalog
     * itself turns out empty. */
    val isInitializing: Boolean = true,
)

/**
 * Reads the active playlist's catalog from [PlaylistRepository] and shapes it
 * into the Home rails (Home.jsx). Degrades gracefully to empty lists when
 * there's no active source or an empty catalog -- a fresh install is a real
 * state, not an error.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val repository: PlaylistRepository = PlaylistRepositoryImpl(app)
    private val settings = UserSettings(app)
    private val epgRepository = EpgRepository(app)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /** Per-rail preview size -- Home shows a "browse for more" affordance, never the full catalog. */
    private val RAIL_LIMIT = 40

    private val _nowPlayingTitles = MutableStateFlow<Map<Long, String>>(emptyMap())
    /** channelId -> current EPG programme title, for the "Live now" rail. Issue #15: the rail
     * previously showed `channel.categoryName` in the "now playing" slot -- never a program
     * title at all (a resolution-tag string like "1080" embedded in some playlists' channel
     * names would be exactly as wrong). Recomputed whenever the channel catalog/EPG data
     * changes (Room Flow), not on a clock tick -- matches product-lead's "no live-ticking
     * recompute" ruling on the EPG timezone fix. Channels absent from this map render with no
     * "now playing" line in the UI -- per product decision, "no title" is an acceptable
     * fallback state, but falling back to categoryName (or any other non-program-name field)
     * is not, since that's the exact class of bug #15 reported. */
    val nowPlayingTitles: StateFlow<Map<Long, String>> = _nowPlayingTitles.asStateFlow()

    init {
        settings.activeSourceId
            .flatMapLatest { sourceId ->
                if (sourceId == null) {
                    flowOf(HomeUiState(hasSource = false, isInitializing = false))
                } else {
                    // Rails only need a bounded preview, never the whole catalog (which OOM'd
                    // on large sources). Category counts come from GROUP BY, merged across the
                    // three catalogs by name -- same shape the old in-memory aggregation produced.
                    val rails = combine(
                        repository.topChannels(sourceId, RAIL_LIMIT),
                        repository.topMovies(sourceId, RAIL_LIMIT),
                        repository.topSeries(sourceId, RAIL_LIMIT),
                    ) { channels, movies, series -> Triple(channels, movies, series) }
                    val categories = combine(
                        repository.channelCategoryCounts(sourceId),
                        repository.movieCategoryCounts(sourceId),
                        repository.seriesCategoryCounts(sourceId),
                    ) { c, m, s ->
                        val merged = linkedMapOf<String, Int>()
                        (c + m + s).forEach { merged[it.name] = (merged[it.name] ?: 0) + it.count }
                        merged.map { (name, count) -> HomeCategorySummary(name, count) }
                    }
                    combine(rails, categories) { (channels, movies, series), cats ->
                        HomeUiState(
                            hasSource = true,
                            channels = channels,
                            movies = movies,
                            series = series,
                            categories = cats,
                            isInitializing = false,
                        )
                    }
                }
            }
            .onEach { _uiState.value = it }
            .launchIn(viewModelScope)

        _uiState.map { state -> state.channels.map { it.id } }
            .distinctUntilChanged()
            .flatMapLatest { channelIds ->
                if (channelIds.isEmpty()) {
                    flowOf(emptyMap())
                } else {
                    val nowMs = System.currentTimeMillis()
                    epgRepository.observeForChannels(channelIds, nowMs, nowMs).map { programs ->
                        programs.associate { it.channelId to it.title }
                    }
                }
            }
            .onEach { _nowPlayingTitles.value = it }
            .launchIn(viewModelScope)
    }

    companion object {
        fun factory(app: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = HomeViewModel(app) as T
            }
    }
}
