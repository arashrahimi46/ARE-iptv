package com.arashrahimi46.iptv.ui.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import androidx.tv.material3.Text
import com.arashrahimi46.iptv.R
import com.arashrahimi46.iptv.ui.components.AreButton
import com.arashrahimi46.iptv.ui.components.AreButtonVariant
import com.arashrahimi46.iptv.ui.components.AreCategoryKind
import com.arashrahimi46.iptv.ui.components.AreCategoryRow
import com.arashrahimi46.iptv.ui.components.AreDialog
import com.arashrahimi46.iptv.ui.components.AreTextField
import com.arashrahimi46.iptv.ui.components.AreTileActionDialog
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import java.text.Normalizer

/** A single entry in [BrowseLayout]'s left-hand filter column. */
data class BrowseCategoryOption(
    val name: String,
    val count: Int? = null,
    val kind: AreCategoryKind = AreCategoryKind.Default,
    val smart: Boolean = false,
    val pinned: Boolean = false,
    /** Whether long-press pin/unpin is offered. False for the "Favorites" and "All" pseudo-rows. */
    val pinnable: Boolean = true,
)

/**
 * What long-pressing a CONTENT tile offers (see [com.arashrahimi46.iptv.ui.components.AreTileActionDialog]).
 * Null on [BrowseLayout] means content tiles have no long-press menu at all -- category-row
 * long-press (pin/unpin) is a separate, independent thing.
 */
data class BrowseTileActions<T>(
    val title: (T) -> String,
    val isFavorite: (T) -> Boolean,
    val onToggleFavorite: (T) -> Unit,
    /** Null when the tile's normal click opens a detail screen rather than starting playback --
     * the dialog then offers favourite + cancel only. */
    val onPlay: ((T) -> Unit)? = null,
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
    categories: List<BrowseCategoryOption>,
    selectedIndex: Int,
    onCategorySelected: (Int) -> Unit,
    items: LazyPagingItems<T>,
    itemKey: (T) -> Any,
    modifier: Modifier = Modifier,
    /** Long-press OK on a category row invokes this to pin/unpin it (index into [categories]).
     * Null on screens without pinning; index 0 (the "All" pseudo-category) is never pinnable. */
    onCategoryPinToggle: ((Int) -> Unit)? = null,
    categoryColumnHeader: String = stringResource(R.string.browse_categories_header),
    titleAccessory: @Composable (() -> Unit)? = null,
    sectionTitle: String? = null,
    /** Authoritative total for the section label (from the catalog's COUNT/GROUP BY), since
     * [items] is now a paged window and its `itemCount` only reflects loaded pages. */
    sectionCount: Int? = null,
    sectionCountLabel: ((Int) -> String)? = null,
    emptyLabel: String = stringResource(R.string.browse_empty_default),
    /** Table/list rendering (Settings' "List view" toggle, Issue #9): items stack in a single
     * column instead of wrapping across the grid. Switching this resets scroll to
     * the top by design (product decision) -- no scroll-position preservation across modes. */
    listMode: Boolean = false,
    /** Minimum column width for the [LazyVerticalGrid]'s [GridCells.Adaptive] -- matches
     * whatever [itemContent] actually renders (channel tiles vs. narrower poster tiles) so
     * the grid wraps the same number of columns per row the old [FlowRow] did. */
    minItemWidth: Dp = AreIptvTheme.spacing.tileLandWidth,
    /** Optional target for a screen's initial D-pad focus: attached to the first (index 0)
     * category row so the caller can drive focus into the browse content on first entry
     * instead of leaving it stranded on the persistent shell's sidebar. Null = no auto-focus
     * target (the sidebar keeps focus, as before). */
    contentFocusRequester: FocusRequester? = null,
    /** Long-press menu for a content tile. Null = no menu (tiles get a null onLongClick). */
    tileActions: BrowseTileActions<T>? = null,
    /** Second param is the tile's long-press handler (null when [tileActions] is null) --
     * pass it straight to the tile's `onLongClick`. */
    itemContent: @Composable (T, (() -> Unit)?) -> Unit,
) {
    val colors = AreIptvTheme.colors
    val spacing = AreIptvTheme.spacing

    // Which category row's pin/unpin dialog is open (index into [categories]); null = closed.
    var pinDialogIndex by remember { mutableStateOf<Int?>(null) }

    // Which content tile's action dialog is open; null = closed. Deliberately a raw MutableState
    // that this function NEVER reads: the read happens inside [BrowseTileActionHost] below, so
    // opening/closing the menu invalidates only that dialog's restart scope, not this one --
    // reading it here would drag the paged grid (and every visible tile) into the recomposition,
    // the same trap BrowseCategoryColumn's doc comment describes for the search query.
    val tileActionItem = remember { mutableStateOf<T?>(null) }

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
        // Header band: the on-air badge sits above the category column (matched 240dp width) and the
        // section title sits on the SAME line, above the content grid -- so both read at one height
        // and the grid starts as high as possible (poster covers were otherwise pushed down/clipped).
        // The page title itself lives in the shell top bar (see AreTopBar.title).
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.safeX),
            horizontalArrangement = Arrangement.spacedBy(spacing.sp8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                // Matches the 280dp category column so the badge aligns with the list beneath it.
                modifier = Modifier.widthIn(min = 280.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                titleAccessory?.invoke()
            }
            if (sectionTitle != null) {
                FlowRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    // Center the smaller count against the title -- FlowRow top-aligns items by
                    // default, which floated "N channels" above the title's cap height.
                    itemVerticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = sectionTitle, style = AreIptvTheme.typography.h3, color = colors.textPrimary)
                    if (sectionCountLabel != null) {
                        Text(text = sectionCountLabel(sectionCount ?: items.itemCount), style = AreIptvTheme.typography.mono, color = colors.textTertiary)
                    }
                }
            } else {
                Box(Modifier.weight(1f))
            }
        }
        Box(Modifier.height(spacing.sp1))
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
            // Category filter column (header + search field + list). Extracted into its own
            // composable so the search query state lives THERE: typing recomposes only the
            // sidebar, never this Row's other child (the paged content grid).
            BrowseCategoryColumn(
                categories = categories,
                selectedIndex = selectedIndex,
                onCategorySelected = onCategorySelected,
                header = categoryColumnHeader,
                contentFocusRequester = contentFocusRequester,
                // Stable across recompositions (captures nothing unstable), so the column can skip.
                onCategoryLongPress = if (onCategoryPinToggle != null) {
                    { index -> pinDialogIndex = index }
                } else {
                    null
                },
            )

            // Content grid for the selected category. The section title/count now lives in the
            // shared header band above (aligned with the page title), not here.
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                // Paged refresh outcome. A failed refresh (query/DB error) is ALSO "settled",
                // so it must be split out from the genuine empty state -- otherwise a real
                // failure silently renders the friendly "nothing here yet" label with no way
                // to recover. Error -> short message + focusable Retry; empty -> empty label.
                val refresh = items.loadState.refresh
                if (refresh is LoadState.Error) {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.sp3)) {
                        Text(text = stringResource(R.string.browse_load_error), style = AreIptvTheme.typography.body, color = colors.textSecondary)
                        AreButton(stringResource(R.string.action_retry), onClick = { items.retry() }, variant = AreButtonVariant.Primary)
                    }
                } else if (items.itemCount == 0 && refresh !is LoadState.Loading) {
                    Text(text = emptyLabel, style = AreIptvTheme.typography.body, color = colors.textSecondary)
                } else if (listMode) {
                    // List/table mode: one item per row. Paging only holds the visible window
                    // in memory regardless of catalog size (300k+ titles).
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(vertical = 10.dp),
                    ) {
                        items(count = items.itemCount, key = items.itemKey(itemKey)) { index ->
                            items[index]?.let { item ->
                                itemContent(item, if (tileActions == null) null else ({ tileActionItem.value = item }))
                            }
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minItemWidth),
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                        // Top: headroom for the focus scale (1.06x) so a focused top row isn't
                        // clipped under the header. Bottom: room for a focused row's title/meta
                        // (which sit below the focusable poster) to scroll fully into view.
                        contentPadding = PaddingValues(top = 10.dp, bottom = 52.dp),
                    ) {
                        items(count = items.itemCount, key = items.itemKey(itemKey)) { index ->
                            items[index]?.let { item ->
                                itemContent(item, if (tileActions == null) null else ({ tileActionItem.value = item }))
                            }
                        }
                    }
                }
            }
        }
    }

    // Pin/unpin overlay for the long-pressed category. Rendered in its own platform Dialog
    // window so it traps D-pad focus (arrow keys stay inside the dialog, never move the
    // category column behind it) -- same reasoning as ParentalPinDialog's doc comment.
    val dialogIndex = pinDialogIndex
    val dialogCategory = dialogIndex?.let { categories.getOrNull(it) }
    if (dialogIndex != null && dialogCategory != null && onCategoryPinToggle != null) {
        Dialog(
            onDismissRequest = { pinDialogIndex = null },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            AreDialog(
                onDismiss = { pinDialogIndex = null },
                title = dialogCategory.name,
                width = 420.dp,
                actions = {
                    AreButton(stringResource(R.string.action_cancel), onClick = { pinDialogIndex = null }, variant = AreButtonVariant.Ghost)
                    AreButton(
                        text = if (dialogCategory.pinned) stringResource(R.string.browse_unpin) else stringResource(R.string.browse_pin),
                        onClick = { onCategoryPinToggle(dialogIndex); pinDialogIndex = null },
                        variant = AreButtonVariant.Primary,
                    )
                },
            ) {
                Text(
                    text = if (dialogCategory.pinned) {
                        stringResource(R.string.browse_pin_dialog_pinned_body)
                    } else {
                        stringResource(R.string.browse_pin_dialog_unpinned_body)
                    },
                    style = AreIptvTheme.typography.body,
                    color = colors.textSecondary,
                )
            }
        }
    }

    if (tileActions != null) {
        BrowseTileActionHost(tileActionItem, tileActions)
    }
}

/**
 * Renders the long-pressed tile's action menu. Split out purely so the "which tile" state read
 * lives in ITS restart scope, not [BrowseLayout]'s -- see the state's declaration.
 */
@Composable
private fun <T : Any> BrowseTileActionHost(state: MutableState<T?>, actions: BrowseTileActions<T>) {
    val item = state.value ?: return
    val onPlay = actions.onPlay
    AreTileActionDialog(
        title = actions.title(item),
        isFavorite = actions.isFavorite(item),
        onToggleFavorite = { actions.onToggleFavorite(item) },
        onPlay = if (onPlay == null) null else ({ onPlay(item) }),
        onDismiss = { state.value = null },
    )
}

/** Combining marks left behind by an NFD decomposition -- stripping them is what makes the
 *  category filter diacritic-insensitive ("Deportes" matches "Déportes", "Turkiye" matches "Türkiye"). */
private val CombiningMarks = Regex("\\p{Mn}+")

private fun String.foldForSearch(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD).replace(CombiningMarks, "").lowercase()

/**
 * The 280dp left column: pinned header + pinned search field + the scrolling category list.
 *
 * Owns the search query itself. That is deliberate and load-bearing for performance: a
 * `query` state read in [BrowseLayout]'s own scope would invalidate the whole layout on every
 * keystroke, dragging the paged content grid (and every visible tile's content lambda) into the
 * recomposition -- exactly the measure/layout cost `docs/glass-render-perf-findings.md` traces
 * to Home's rail. Here the keystroke invalidates this function only.
 *
 * Filtering is a pure in-memory pass over the already-loaded [categories] list -- it never
 * round-trips through the ViewModel, so the Paging flow is untouched and the grid keeps both
 * its scroll position and its loaded pages while the user types.
 */
@Composable
private fun BrowseCategoryColumn(
    categories: List<BrowseCategoryOption>,
    selectedIndex: Int,
    onCategorySelected: (Int) -> Unit,
    header: String,
    contentFocusRequester: FocusRequester?,
    onCategoryLongPress: ((Int) -> Unit)?,
) {
    val colors = AreIptvTheme.colors
    var query by remember { mutableStateOf("") }

    // Folded once per catalog, not once per keystroke -- Normalizer over a few hundred names on
    // every character typed is the kind of per-frame work that shows up as jank on a TV.
    val foldedNames = remember(categories) { categories.map { it.name.foldForSearch() } }
    // Indices INTO [categories], so [selectedIndex] and the pin/select callbacks keep speaking the
    // caller's original index space -- filtering never renumbers a category out from under the
    // ViewModel, and clearing the query restores the full list with the selection intact.
    val visibleIndices = remember(foldedNames, query) {
        val folded = query.foldForSearch().trim()
        if (folded.isEmpty()) {
            foldedNames.indices.toList()
        } else {
            foldedNames.indices.filter { foldedNames[it].contains(folded) }
        }
    }

    Column(modifier = Modifier.width(280.dp).fillMaxHeight()) {
        Text(
            text = header.uppercase(),
            style = AreIptvTheme.typography.caption,
            color = colors.textTertiary,
            modifier = Modifier.padding(bottom = 8.dp, start = 16.dp),
        )
        // activateOnClick: D-pad'ing DOWN past this field to reach the list must NOT pop the IME
        // (CLAUDE.md's TV text-input rule). OK enters edit mode, Back/Done leaves it.
        AreTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = stringResource(R.string.browse_category_search_placeholder),
            icon = Icons.Filled.Search,
            activateOnClick = true,
        )
        // Only 2dp here: the list's own top contentPadding supplies the rest of the gap. Putting
        // the space in a sibling Spacer instead left it OUTSIDE the list's viewport, so the first
        // row's focus ring had no headroom inside it and got shaved off at the top edge.
        Box(Modifier.height(2.dp))
        if (visibleIndices.isEmpty()) {
            Text(
                text = stringResource(R.string.browse_category_search_empty),
                style = AreIptvTheme.typography.body,
                color = colors.textSecondary,
                modifier = Modifier.padding(start = 16.dp),
            )
        } else {
            // A LazyColumn (not a verticalScroll Column) so only the on-screen rows are composed.
            // With a large catalog this column holds hundreds of country/genre entries; eagerly
            // composing every one (each a focusable + glow) was what made a fast flick stutter.
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                // Room INSIDE the viewport at both ends so a focused row's ring/glow is never
                // shaved off against the column edge. Bottom was already here; top was not, so the
                // very first row -- "Favorites", the one focus lands on -- had its ring clipped
                // flat along its top edge at rest. AreCategoryRow's ring/glow is drawn outside the
                // row's own bounds, so the headroom has to come from the list, not from the row.
                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
            ) {
                // Keyed by name, not by position. Index-keyed, any change to the list -- pinning
                // floats a row to the top, a count changes, the filter narrows -- made Compose reuse
                // slots BY POSITION: every visible row recomposed instead of moving, and each row's
                // remembered MutableInteractionSource stayed with the slot rather than the category,
                // so the pressed/focus visual briefly attached to the wrong name.
                // contentType is uniform because this list is homogeneous now (the header and the
                // search field are pinned OUTSIDE it) -- stated explicitly so it stays that way.
                itemsIndexed(
                    visibleIndices,
                    key = { _, index -> categories[index].name },
                    contentType = { _, _ -> "category" },
                ) { position, index ->
                    val category = categories[index]
                    AreCategoryRow(
                        name = category.name,
                        onClick = { onCategorySelected(index) },
                        // The first row is always composed (top of the column) so it's a reliable
                        // initial-focus target for the caller's contentFocusRequester.
                        modifier = if (position == 0 && contentFocusRequester != null) {
                            Modifier.focusRequester(contentFocusRequester)
                        } else {
                            Modifier
                        },
                        count = category.count,
                        kind = category.kind,
                        smart = category.smart,
                        active = index == selectedIndex,
                        pinned = category.pinned,
                        // Only real categories are pinnable -- the leading "Favorites"/"All" pseudo-rows aren't.
                        onLongClick = if (onCategoryLongPress != null && category.pinnable) {
                            { onCategoryLongPress(index) }
                        } else {
                            null
                        },
                    )
                }
            }
        }
    }
}
