package com.arashrahimi46.iptv.ui.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.arashrahimi46.iptv.ui.components.AreCategoryKind
import com.arashrahimi46.iptv.ui.components.AreCategoryRow
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme

/** A single entry in [BrowseLayout]'s left-hand filter column. */
data class BrowseCategoryOption(
    val name: String,
    val count: Int? = null,
    val kind: AreCategoryKind = AreCategoryKind.Default,
    val smart: Boolean = false,
)

/**
 * Shared "category filter column + content grid" layout (Browse.jsx / Live.jsx).
 * Parameterized by category list + content list + a per-item render lambda so
 * it can back Live TV now and Movies/Series later (Phase 3) without change --
 * only the [BrowseCategoryOption] list, the item type [T] and [itemContent]
 * differ per screen.
 *
 * Note: this is a plain (non-lazy) [FlowRow] grid, not [androidx.compose.foundation.lazy.grid.LazyVerticalGrid] --
 * the caller (Live/Home) already lives inside [com.arashrahimi46.iptv.ui.shell.AreIptvAppShell]'s
 * single outer `verticalScroll` Column, and nesting an unbounded-height lazy
 * grid inside another vertical scroll container isn't a valid Compose layout
 * (infinite constraints). A real virtualized grid is a polish item for a
 * later pass once catalogs are large enough for it to matter -- see report.
 * Likewise the left column reads as "sticky" in the design source (CSS
 * `position: sticky`) but here is a plain non-scrolling column; true
 * scroll-independent stickiness needs a custom two-pane layout, cut for time
 * -- see report.
 */
@Composable
fun <T> BrowseLayout(
    title: String,
    categories: List<BrowseCategoryOption>,
    selectedIndex: Int,
    onCategorySelected: (Int) -> Unit,
    items: List<T>,
    itemKey: (T) -> Any,
    modifier: Modifier = Modifier,
    categoryColumnHeader: String = "Categories",
    titleAccessory: @Composable (() -> Unit)? = null,
    sectionTitle: String? = null,
    sectionCountLabel: ((Int) -> String)? = null,
    emptyLabel: String = "No items in this category yet.",
    itemContent: @Composable (T) -> Unit,
) {
    val colors = AreIptvTheme.colors
    val spacing = AreIptvTheme.spacing

    Column(modifier = modifier.padding(top = spacing.sp6, bottom = spacing.sp10)) {
        Box(Modifier.padding(horizontal = spacing.safeX)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = title, style = AreIptvTheme.typography.display, color = colors.textPrimary)
                titleAccessory?.invoke()
            }
        }
        Box(Modifier.height(spacing.sp6))
        Row(
            modifier = Modifier.padding(horizontal = spacing.safeX),
            horizontalArrangement = Arrangement.spacedBy(spacing.sp8),
        ) {
            // Category filter column.
            Column(modifier = Modifier.width(300.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = categoryColumnHeader.uppercase(),
                    style = AreIptvTheme.typography.caption,
                    color = colors.textTertiary,
                    modifier = Modifier.padding(bottom = 8.dp, start = 16.dp),
                )
                categories.forEachIndexed { index, category ->
                    AreCategoryRow(
                        name = category.name,
                        onClick = { onCategorySelected(index) },
                        count = category.count,
                        kind = category.kind,
                        smart = category.smart,
                        active = index == selectedIndex,
                    )
                }
            }

            // Content grid for the selected category.
            Column(modifier = Modifier.weight(1f)) {
                if (sectionTitle != null) {
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(text = sectionTitle, style = AreIptvTheme.typography.h2, color = colors.textPrimary)
                        if (sectionCountLabel != null) {
                            Text(text = sectionCountLabel(items.size), style = AreIptvTheme.typography.mono, color = colors.textTertiary)
                        }
                    }
                    Box(Modifier.height(18.dp))
                }
                if (items.isEmpty()) {
                    Text(text = emptyLabel, style = AreIptvTheme.typography.body, color = colors.textSecondary)
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                    ) {
                        items.forEach { item -> itemContent(item) }
                    }
                }
            }
        }
    }
}
