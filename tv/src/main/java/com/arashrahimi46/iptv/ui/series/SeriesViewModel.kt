package com.arashrahimi46.iptv.ui.series

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arashrahimi46.iptv.data.model.VodTitle
import com.arashrahimi46.iptv.data.repository.PlaylistRepository
import com.arashrahimi46.iptv.data.repository.PlaylistRepositoryImpl
import com.arashrahimi46.iptv.data.settings.UserSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

data class SeriesCategorySummary(val name: String, val count: Int)

data class SeriesUiState(
    val hasSource: Boolean = false,
    val allSeries: List<VodTitle> = emptyList(),
    val categories: List<SeriesCategorySummary> = emptyList(),
    val selectedCategoryIndex: Int = 0,
) {
    /** "All series" is always category 0; anything else filters by [VodTitle.categoryName]. */
    val visibleSeries: List<VodTitle>
        get() = if (selectedCategoryIndex == 0 || categories.isEmpty()) {
            allSeries
        } else {
            val name = categories.getOrNull(selectedCategoryIndex)?.name
            if (name == null) allSeries else allSeries.filter { it.categoryName == name }
        }
}

/**
 * Series browse state: real [VodTitle] catalog (isSeries = true) for the
 * active source (Room via [PlaylistRepository]), grouped the same way
 * [com.arashrahimi46.iptv.ui.live.LiveViewModel] groups channels.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SeriesViewModel(app: Application) : AndroidViewModel(app) {
    private val repository: PlaylistRepository = PlaylistRepositoryImpl(app)
    private val settings = UserSettings(app)

    private val _uiState = MutableStateFlow(SeriesUiState())
    val uiState: StateFlow<SeriesUiState> = _uiState.asStateFlow()

    init {
        settings.activeSourceId
            .flatMapLatest { sourceId ->
                if (sourceId == null) flowOf<List<VodTitle>?>(null) else repository.observeSeries(sourceId)
            }
            .onEach { series ->
                _uiState.value = if (series == null) SeriesUiState(hasSource = false) else buildState(series)
            }
            .launchIn(viewModelScope)
    }

    private fun buildState(series: List<VodTitle>): SeriesUiState {
        val counts = linkedMapOf<String, Int>()
        series.forEach { title ->
            title.categoryName?.let { counts[it] = (counts[it] ?: 0) + 1 }
        }
        val categories = listOf(SeriesCategorySummary("All series", series.size)) +
            counts.map { (name, count) -> SeriesCategorySummary(name, count) }
        val previousSelection = _uiState.value.selectedCategoryIndex.coerceIn(0, categories.lastIndex.coerceAtLeast(0))
        return SeriesUiState(
            hasSource = true,
            allSeries = series,
            categories = categories,
            selectedCategoryIndex = previousSelection,
        )
    }

    fun selectCategory(index: Int) {
        _uiState.value = _uiState.value.copy(selectedCategoryIndex = index)
    }

    companion object {
        fun factory(app: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = SeriesViewModel(app) as T
            }
    }
}
