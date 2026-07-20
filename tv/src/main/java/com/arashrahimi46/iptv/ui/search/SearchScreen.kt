package com.arashrahimi46.iptv.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Text
import com.arashrahimi46.iptv.data.model.Channel
import com.arashrahimi46.iptv.data.model.VodTitle
import com.arashrahimi46.iptv.ui.components.AreButton
import com.arashrahimi46.iptv.ui.components.AreButtonSize
import com.arashrahimi46.iptv.ui.components.AreButtonVariant
import com.arashrahimi46.iptv.ui.components.AreChannelTile
import com.arashrahimi46.iptv.ui.components.AreOnScreenKeyboard
import com.arashrahimi46.iptv.ui.components.ArePosterTile
import com.arashrahimi46.iptv.ui.components.AreTextField
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme

/**
 * Local search (Search.jsx): a query field driven entirely by
 * [AreOnScreenKeyboard] key presses (D-pad + select, no IME) plus grouped
 * result rows -- live channels and movies/series -- reusing
 * [AreChannelTile]/[ArePosterTile]. No ranking backend: plain substring
 * match over the already-loaded catalog, per spec.
 */
@Composable
fun SearchScreen(
    onChannelSelected: (Channel) -> Unit,
    onTitleSelected: (VodTitle) -> Unit,
    modifier: Modifier = Modifier,
    /** Home's "Browse by category" cards have no dedicated category-browse screen of
     * their own -- they route here with an exact category to filter by (see report),
     * distinct from a typed text query. */
    initialCategory: String? = null,
) {
    val context = LocalContext.current
    val viewModel: SearchViewModel = viewModel(
        factory = SearchViewModel.factory(context.applicationContext as android.app.Application),
    )
    LaunchedEffect(initialCategory) {
        if (!initialCategory.isNullOrBlank()) viewModel.setCategoryFilter(initialCategory)
    }
    val state by viewModel.uiState.collectAsState()
    val favoriteChannelIds by viewModel.favoriteChannelIds.collectAsState()
    val favoriteVodIds by viewModel.favoriteVodIds.collectAsState()
    val colors = AreIptvTheme.colors
    val spacing = AreIptvTheme.spacing

    if (!state.hasSource) {
        Text(
            text = "Add a playlist from the sidebar to search your catalog.",
            style = AreIptvTheme.typography.body,
            color = colors.textSecondary,
            modifier = modifier.padding(horizontal = spacing.safeX, vertical = spacing.sp10),
        )
        return
    }

    Column(modifier = modifier.padding(horizontal = spacing.safeX, vertical = spacing.sp6)) {
        Text(text = "Search", style = AreIptvTheme.typography.display, color = colors.textPrimary)
        Box(Modifier.padding(top = spacing.sp6))

        Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            Column(modifier = Modifier.width(420.dp)) {
                AreTextField(
                    value = state.query,
                    onValueChange = viewModel::setQuery,
                    placeholder = "Search channels, movies, series…",
                    icon = Icons.Filled.Search,
                )
                Box(Modifier.padding(top = 20.dp))
                AreOnScreenKeyboard(
                    onCharacter = viewModel::appendCharacter,
                    onSpace = viewModel::appendSpace,
                    onBackspace = viewModel::backspace,
                    onClear = viewModel::clear,
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                if (state.categoryFilter != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(text = "Category: ${state.categoryFilter}", style = AreIptvTheme.typography.h3, color = colors.textPrimary)
                        AreButton(text = "Clear", onClick = { viewModel.setCategoryFilter(null) }, variant = AreButtonVariant.Ghost, size = AreButtonSize.Small)
                    }
                    Box(Modifier.padding(top = 16.dp))
                }
                if (state.categoryFilter == null && state.query.isBlank()) {
                    Text(
                        text = "Type on the keyboard to search your catalog.",
                        style = AreIptvTheme.typography.body,
                        color = colors.textSecondary,
                    )
                } else if (state.channelResults.isEmpty() && state.titleResults.isEmpty()) {
                    val label = state.categoryFilter ?: state.query
                    Text(text = "No results for \"$label\".", style = AreIptvTheme.typography.body, color = colors.textSecondary)
                } else {
                    if (state.channelResults.isNotEmpty()) {
                        Text(text = "Live TV", style = AreIptvTheme.typography.h3, color = colors.textSecondary)
                        Box(Modifier.padding(top = 12.dp, bottom = 22.dp)) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                                state.channelResults.forEach { channel ->
                                    AreChannelTile(
                                        channel = channel.name,
                                        onClick = { onChannelSelected(channel) },
                                        number = channel.number,
                                        now = channel.categoryName,
                                        isFavorite = channel.id in favoriteChannelIds,
                                        onToggleFavorite = { viewModel.toggleChannelFavorite(channel.id) },
                                    )
                                }
                            }
                        }
                    }
                    if (state.titleResults.isNotEmpty()) {
                        Text(text = "Movies & series", style = AreIptvTheme.typography.h3, color = colors.textSecondary)
                        Box(Modifier.padding(top = 12.dp)) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                                state.titleResults.forEach { title ->
                                    ArePosterTile(
                                        title = title.name,
                                        onClick = { onTitleSelected(title) },
                                        meta = listOfNotNull(title.year, title.categoryName).joinToString(" · ").ifEmpty { null },
                                        rating = title.rating,
                                        isFavorite = title.id in favoriteVodIds,
                                        onToggleFavorite = { viewModel.toggleVodFavorite(title) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
