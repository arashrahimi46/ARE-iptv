package com.arashrahimi46.iptv.mobile.ui.movies

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import com.arashrahimi46.iptv.mobile.R
import com.arashrahimi46.iptv.data.model.VodTitle
import com.arashrahimi46.iptv.mobile.ui.components.PosterTile

/** Shared by Movies and Series: a category filter row over an adaptive poster grid, sized for
 * phone widths -- GridCells.Adaptive computed from the actual viewport (like :tv's Browse), not
 * a fixed TV tile-size token. See CLAUDE.md's tile-sizing lesson. */
@Composable
fun VodGridScreen(viewModel: VodGridViewModel, onOpenTitle: (VodTitle) -> Unit) {
    val categories by viewModel.categories.collectAsState()
    val selected by viewModel.selectedCategory.collectAsState()
    val pagingItems = viewModel.items.collectAsLazyPagingItems()

    Column(Modifier.fillMaxSize()) {
        if (categories.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    FilterChip(
                        selected = selected == null,
                        onClick = { viewModel.selectCategory(null) },
                        label = { Text(stringResource(R.string.search_scope_all)) },
                    )
                }
                items(categories) { name ->
                    FilterChip(
                        selected = selected == name,
                        onClick = { viewModel.selectCategory(name) },
                        label = { Text(name) },
                    )
                }
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 110.dp),
            modifier = Modifier.fillMaxWidth().fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 52.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            gridItems(
                items = (0 until pagingItems.itemCount).toList(),
                key = { index -> pagingItems.peek(index)?.id ?: index },
            ) { index ->
                val title = pagingItems[index] ?: return@gridItems
                PosterTile(title, onClick = { onOpenTitle(title) })
            }
        }
    }
}
