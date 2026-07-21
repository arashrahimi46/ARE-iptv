package com.arashrahimi46.iptv.ui.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
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
 * P0.2: the content grid is now a real [LazyVerticalGrid] (list mode: [LazyColumn]),
 * not a plain [FlowRow] eagerly composing the whole catalog -- callers (Live/Movies/
 * Series) no longer wrap this in [com.arashrahimi46.iptv.MainActivity]'s scrolling
 * `ScrollableTab`; they get a plain fillMaxSize tab instead (see MainActivity's
 * `FullSizeTab`), so this Composable owns its own bounded-height layout end to end
 * (root fillMaxSize -> weighted category/content Row -> weighted grid) instead of
 * inheriting an outer `verticalScroll`'s unbounded height, which is what made a real
 * lazy grid invalid here before (nesting an unbounded-height lazy layout inside
 * another vertical scroll container throws at runtime). No item cap anymore -- lazy
 * composition only builds what's actually on screen regardless of catalog size.
 * The left column reads as "sticky" in the design source (CSS `position: sticky`);
 * true scroll-independent stickiness still needs a custom two-pane layout (cut for
 * time -- see report), but it now scrolls independently of the content grid instead
 * of just clipping once the category list is taller than the screen.
 */
@Composable
fun <T : Any> BrowseLayout(
    title: String,
    categories: List<BrowseCategoryOption>,
    selectedIndex: Int,
    onCategorySelected: (Int) -> Unit,
    items: LazyPagingItems<T>,
    itemKey: (T) -> Any,
    modifier: Modifier = Modifier,
    categoryColumnHeader: String = "Categories",
    titleAccessory: @Composable (() -> Unit)? = null,
    sectionTitle: String? = null,
    /** Authoritative total for the section label (from the catalog's COUNT/GROUP BY), since
     * [items] is now a paged window and its `itemCount` only reflects loaded pages. */
    sectionCount: Int? = null,
    sectionCountLabel: ((Int) -> String)? = null,
    emptyLabel: String = "No items in this category yet.",
    /** Table/list rendering (Settings' "List view" toggle, Issue #9): items stack in a single
     * column instead of wrapping across the grid. Switching this resets scroll to
     * the top by design (product decision) -- no scroll-position preservation across modes. */
    listMode: Boolean = false,
    /** Minimum column width for the [LazyVerticalGrid]'s [GridCells.Adaptive] -- matches
     * whatever [itemContent] actually renders (channel tiles vs. narrower poster tiles) so
     * the grid wraps the same number of columns per row the old [FlowRow] did. */
    minItemWidth: Dp = AreIptvTheme.spacing.tileLandWidth,
    itemContent: @Composable (T) -> Unit,
) {
    val colors = AreIptvTheme.colors
    val spacing = AreIptvTheme.spacing

    // Follow-up on the QA MEDIUM text-wrap defect: fillMaxWidth on the inner
    // categories+content Row alone did not fix the on-device repro -- this outer
    // root Column (built from the caller's plain `modifier` param, itself never
    // fillMaxWidth'd by any BrowseLayout caller) was still sizing itself to wrap
    // content, so the weight(1f) content column's real available width stayed
    // undersized. Claiming the full size here too so the whole chain resolves
    // against the actual screen size (height, now that the content grid below is a
    // real lazy layout and needs a genuine bounded height to lay out against), not
    // each level's own wrap-content guess.
    Column(modifier = modifier.fillMaxSize().padding(top = spacing.sp1, bottom = spacing.sp3)) {
        // Header band: the page title sits above the category column (matched 240dp width) and the
        // section title sits on the SAME line, above the content grid -- so both read at one height
        // and the grid starts as high as possible (poster covers were otherwise pushed down/clipped).
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.safeX),
            horizontalArrangement = Arrangement.spacedBy(spacing.sp8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.width(240.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(text = title, style = AreIptvTheme.typography.display, color = colors.textPrimary)
                titleAccessory?.invoke()
            }
            if (sectionTitle != null) {
                FlowRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(text = sectionTitle, style = AreIptvTheme.typography.h2, color = colors.textPrimary)
                    if (sectionCountLabel != null) {
                        Text(text = sectionCountLabel(sectionCount ?: items.itemCount), style = AreIptvTheme.typography.mono, color = colors.textTertiary)
                    }
                }
            } else {
                Box(Modifier.weight(1f))
            }
        }
        Box(Modifier.height(spacing.sp2))
        // QA MEDIUM defect (same class as SettingsRow's fix): this Row held the fixed-width
        // category column plus a weight(1f) content column but never claimed the full width
        // itself, so the weight(1f) column had no real remaining space to expand into -- its
        // Text children (e.g. Live TV's "N channels" label) wrapped one character per line.
        // fillMaxWidth is the real fix here too, shared by every BrowseLayout caller (Live,
        // Movies, Series). weight(1f) here (this Row is the last child of the fillMaxSize
        // root Column above) claims the remaining height after the title row -- that's what
        // gives both the category column and the content grid below a real bounded height.
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = spacing.safeX),
            horizontalArrangement = Arrangement.spacedBy(spacing.sp8),
        ) {
            // Category filter column -- scrolls independently of the content grid now that
            // it has a real bounded height, rather than just clipping once there are enough
            // categories to exceed the screen (previously masked by the whole screen scrolling
            // together as one unbounded column).
            Column(
                // 240dp (was 300dp): the wide filter column left too little room for the content
                // grid on a ~960dp TV, forcing the poster grid down to a single oversized column.
                modifier = Modifier.width(240.dp).fillMaxHeight().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
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

            // Content grid for the selected category. The section title/count now lives in the
            // shared header band above (aligned with the page title), not here.
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                // Paged: only "empty" once the first load has settled (avoid flashing the empty
                // label during the initial page fetch on a huge catalog).
                val settled = items.loadState.refresh !is LoadState.Loading
                if (items.itemCount == 0 && settled) {
                    Text(text = emptyLabel, style = AreIptvTheme.typography.body, color = colors.textSecondary)
                } else if (listMode) {
                    // List/table mode: one item per row. Paging only holds the visible window
                    // in memory regardless of catalog size (300k+ titles).
                    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        items(count = items.itemCount, key = items.itemKey(itemKey)) { index ->
                            items[index]?.let { itemContent(it) }
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minItemWidth),
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                    ) {
                        items(count = items.itemCount, key = items.itemKey(itemKey)) { index ->
                            items[index]?.let { itemContent(it) }
                        }
                    }
                }
            }
        }
    }
}
