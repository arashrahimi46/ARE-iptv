package com.arashrahimi46.iptv.ui.guide

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Text
import com.arashrahimi46.iptv.core.R
import com.arashrahimi46.iptv.ui.components.AreChip
import com.arashrahimi46.iptv.ui.components.AreSegmentedControl
import com.arashrahimi46.iptv.ui.components.AreDialog
import com.arashrahimi46.iptv.ui.components.AreGuideCell
import com.arashrahimi46.iptv.ui.components.rememberClockFormatter
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.theme.glassSurface
import com.arashrahimi46.iptv.ui.theme.rememberTileArtwork
import com.arashrahimi46.iptv.ui.theme.rememberPlaybackFocusRequester
import com.arashrahimi46.iptv.ui.theme.requestFocusWhenReady
import java.time.Instant
import java.time.ZoneId

/**
 * px-per-minute scale for proportional GuideCell widths (mirrors Guide.jsx `PX`).
 *
 * Left at 3dp deliberately. It was tried at 2.4 to fit more of the window on screen, and that is the
 * wrong axis to compress: a 30-minute programme is then 72dp wide, which after the cell's padding
 * leaves ~52dp of title -- every real programme rendered as "Hom...". The screen's wasted space was
 * vertical (the category chip strip and 64dp rows), and that is where the density came from.
 */
private val DpPerMinute = 3.dp

/**
 * TV Guide (Guide.jsx): 6-hour rolling window, day chips, channel-group
 * filter, timeline header, and the sticky "focused-program info bar" (no
 * tooltips on TV -- driven by [AreGuideCell]'s `onFocusChange`). Selecting any
 * cell in a channel's row (any programme, not just the live one) immediately
 * plays that channel via the real [com.arashrahimi46.iptv.ui.player.LivePlayerScreen].
 */
@Composable
fun GuideScreen(
    onChannelSelected: (channelId: Long) -> Unit,
    modifier: Modifier = Modifier,
    onCatchup: (channelId: Long, startMs: Long, endMs: Long) -> Unit = { _, _, _ -> },
) {
    val context = LocalContext.current
    val viewModel: GuideViewModel = viewModel(
        factory = GuideViewModel.factory(context.applicationContext as android.app.Application),
    )
    val state by viewModel.uiState.collectAsState()
    // PERF: `focused` is deliberately NOT read here. Every D-pad move writes it, and reading it at
    // this level made the ENTIRE Guide recompose on every keypress -- the grid, the timeline header
    // and every visible cell -- to update one info bar. FocusedInfoBar collects it itself, so the
    // recomposition scope is that one Row. See its doc.
    val colors = AreIptvTheme.colors
    val spacing = AreIptvTheme.spacing
    val timeFormatter = rememberClockFormatter()
    // Issue #5: which cell started playback -- channel id alone doesn't disambiguate
    // which of a row's several programme cells was clicked (any cell can start playback,
    // not just the live one -- see issue #4 above), so the clicked slot's start time is
    // tracked alongside it. See HomeScreen's rememberPlaybackFocusRequester for the rest
    // of the explanation (survives the screen pausing while the player is on top).
    var lastPlayedChannelId by rememberSaveable { mutableStateOf<Long?>(null) }
    var lastPlayedSlotStartMs by rememberSaveable { mutableStateOf<Long?>(null) }
    // Catch-up action menu: the (channel, slot) whose past cell was pressed, or null when closed.
    var catchupMenu by remember { mutableStateOf<Pair<com.arashrahimi46.iptv.data.model.Channel, GuideProgramSlot>?>(null) }

    if (!state.hasSource) {
        Text(
            text = stringResource(R.string.guide_no_source),
            style = AreIptvTheme.typography.body,
            color = colors.textSecondary,
            modifier = modifier.padding(horizontal = spacing.safeX, vertical = spacing.sp10),
        )
        return
    }

    // remember-ed so it's a stable `remember` key for the per-cell time labels below.
    val zone = remember { ZoneId.systemDefault() }

    // Initial D-pad focus, matching every other tab: without it the shell leaves focus on the
    // sidebar and the Guide reads as dead. Lands on the category button rather than a programme
    // cell -- from there Down enters the grid, whereas starting inside the grid would mean
    // travelling back up past the info bar to change category.
    // Stands down when returning from the player (the played cell is restored instead).
    val groupFocusRequester = remember { FocusRequester() }
    // Keyed on whether categories have ARRIVED, not on which one is selected: they load
    // asynchronously (so LaunchedEffect(Unit) would fire before there is anything to focus), but
    // re-firing on every category change would yank focus back out of the grid mid-browse.
    val hasGroups = state.groups.isNotEmpty()
    LaunchedEffect(hasGroups) {
        if (hasGroups && lastPlayedChannelId == null) groupFocusRequester.requestFocusWhenReady()
    }
    var categoryPickerOpen by remember { mutableStateOf(false) }

    // P0.2: fillMaxSize (not just padding) so this root Column has a real bounded height to
    // hand down -- GuideScreen's caller (MainActivity) no longer wraps this tab in a
    // `verticalScroll` (see FullSizeTab there); this Composable owns its own layout end to
    // end instead. That bounded height is what makes the LazyColumn below (and the .weight(1f)
    // it and its sibling Row get further down) a valid virtualized layout rather than throwing
    // "measured with an unbounded amount of height" -- a lazy layout can't live inside another
    // unbounded-height vertical scroll container (same-axis nesting), which is exactly what
    // wrapping this tab in a scrolling container would still do.
    // bottom = sp5, not sp10: the grid's own last row needs headroom for its focus ring, but 40dp of
    // it was a whole channel row's worth of dead space on a 540dp-tall screen.
    Column(modifier = modifier.fillMaxSize().padding(top = spacing.sp2, bottom = spacing.sp5)) {
        // Header: category button + day chips (the page title lives in the shell top bar).
        //
        // The category used to be a horizontally-scrolling strip of one chip per group, on its own
        // row. On a real playlist that is dozens of chips the user has to D-pad through one at a
        // time, and it cost a whole row of vertical space on the one screen whose entire value is
        // density. It is now a single button opening a searchable, pinnable picker -- the same
        // trade Browse's category column already makes.
        Row(
            modifier = Modifier.padding(horizontal = spacing.safeX).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AreChip(
                text = state.selectedGroup.ifEmpty { stringResource(R.string.browse_categories_header) },
                onClick = { categoryPickerOpen = true },
                icon = Icons.Filled.FilterList,
                selected = true,
                modifier = Modifier.focusRequester(groupFocusRequester),
            )
            Box(Modifier.weight(1f))
            AreSegmentedControl(
                options = GuideDay.entries,
                selected = state.day,
                label = { stringResource(it.labelRes) },
                onSelect = { viewModel.selectDay(it) },
            )
        }
        Box(Modifier.height(spacing.sp3))

        // P0.4: EPG source unreachable -- a small banner, not a full-screen replacement. Channel
        // rows below still render (each with its own "No programme data" placeholder slot).
        if (state.epgUnavailable) {
            Box(Modifier.padding(horizontal = spacing.safeX)) {
                Text(
                    text = stringResource(R.string.guide_epg_unavailable),
                    style = AreIptvTheme.typography.caption,
                    color = colors.danger,
                )
            }
            Box(Modifier.height(spacing.sp2))
        }

        // Sticky focused-program info bar (no hover/tooltips on TV -- last focused cell stays shown).
        Box(Modifier.padding(horizontal = spacing.safeX)) {
            FocusedInfoBar(viewModel)
        }
        Box(Modifier.height(spacing.sp3))

        // Shared horizontal ScrollState -- the SAME instance applied to the timeline header
        // and every row below keeps them scrolling in lockstep (Compose's standard
        // synced-header pattern), now that rows are virtualized (below) and can no longer
        // all live under one shared-scroll Column together.
        val scrollState = rememberScrollState()
        // weight(1f) -- this is the last child of the fillMaxSize root Column above, so it
        // claims exactly the height left over after the header rows/chips/info-bar -- the real
        // bounded height the LazyColumn below needs to be a valid lazy layout.
        // BoxWithConstraints, not an onGloballyPositioned measurement, because the lanes need the
        // viewport width DURING their first composition -- that is the expensive frame this is here to
        // make cheap, and a post-layout callback arrives one frame too late to save it. One
        // subcomposition for the whole grid; the lanes take the number as a plain Int.
        BoxWithConstraints(modifier = Modifier.weight(1f).padding(horizontal = spacing.safeX)) {
            // What a lane can actually show: the grid width less the pinned channel column and the
            // 8dp gap the row puts between them.
            val laneWidthPx = with(LocalDensity.current) {
                (maxWidth - spacing.guideChannelWidth - 8.dp).coerceAtLeast(0.dp).roundToPx()
            }
        Column(modifier = Modifier.fillMaxSize()) {
            // Channel column is PINNED (drawn outside the shared horizontalScroll) on both the
            // timeline header and every row, so the channel you're browsing stays visible no
            // matter how far right you scroll through its programmes. Only the programme lane
            // (weight(1f) + horizontalScroll) moves, keeping header + rows in lockstep.
            TimelineHeader(windowStartMs = state.windowStartMs, windowEndMs = state.windowEndMs, zone = zone, scrollState = scrollState)
            Box(Modifier.height(8.dp))
            // P0.2: was a plain Column.forEach that eagerly composed every channel row for
            // the whole ~6h window regardless of what's on screen -- with large catalogs
            // that's hundreds of rows composed up front. LazyColumn only composes the rows
            // actually visible (plus a small buffer). weight(1f) fills the remaining height
            // left in this Column after the timeline header row above.
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                // Headroom INSIDE the viewport (CLAUDE.md's grid rule): a focused cell's ring and
                // glow are drawn outside the row's own bounds, so without this the first and last
                // rows have theirs shaved flat against the viewport edge.
                contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 4.dp, bottom = 20.dp),
            ) {
                items(state.rows, key = { it.channel.id }) { row ->
                    // A focused cell scales up (1.06) + draws an outward glow that overflows the
                    // row height. LazyColumn draws items in order, so the NEXT row would paint over
                    // this row's downward glow -- clipping it flat. Raise the focused row's zIndex
                    // so it draws above its siblings and the glow stays whole.
                    var rowFocused by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier
                            .zIndex(if (rowFocused) 1f else 0f)
                            .onFocusChanged { rowFocused = it.hasFocus },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ChannelHeaderCell(channel = row.channel)
                        // PERF: the lane is virtualized BY HAND -- see [rememberGuideLane]. It cannot be
                        // a LazyRow: every lane and the timeline header share one pixel [ScrollState] so
                        // the grid stays aligned by TIME, and a LazyRow scrolls by item index, which
                        // would let rows with different programme boundaries drift out of lockstep. So
                        // the lane keeps the shared scroll and simply skips composing the cells that are
                        // off screen.
                        val lane = rememberGuideLane(row.slots)
                        val visibleRange by remember(lane, laneWidthPx, scrollState) {
                            derivedStateOf { lane.visibleRange(scrollState.value, laneWidthPx) }
                        }
                        Box(
                            modifier = Modifier.weight(1f).horizontalScroll(scrollState),
                        ) {
                            // Full-width spacer: the lane's scrollable extent must stay the extent of the
                            // whole window no matter how few cells are composed, or the shared scroll
                            // range would shrink to the visible slice and the header would desync.
                            Box(Modifier.width(lane.totalWidth).height(spacing.guideRowHeight))
                            for (index in visibleRange) {
                                val cell = lane.cells[index]
                                val slot = cell.slot
                                // `key`, and it is not optional. A plain loop memoizes by POSITION, so as
                                // the visible range slides the cell at slot 0 becomes a different
                                // programme -- Compose would rebuild each cell's state (its remembered
                                // time label, its focus requester, its interaction source) under the
                                // focused node and drop D-pad focus mid-scroll. Keyed by start time, a
                                // cell keeps its identity as the window moves around it; this is the same
                                // guarantee `items(key = ...)` gives a lazy list.
                                key(slot.startMs) {
                                // PERF: a pure function of the slot's fixed timestamps, but it ran on
                                // EVERY recomposition of this row -- and a ZonedDateTime + format() per
                                // cell, times every cell of every visible row, is real allocation and CPU
                                // on the D-pad path. Remembered against the slot instead.
                                val timeLabel = remember(slot.startMs, zone, timeFormatter) {
                                    Instant.ofEpochMilli(slot.startMs).atZone(zone).format(timeFormatter)
                                }
                                val focusRequester = rememberPlaybackFocusRequester(
                                    savedId = lastPlayedChannelId?.takeIf { lastPlayedSlotStartMs == slot.startMs },
                                    itemId = row.channel.id,
                                ) {
                                    lastPlayedChannelId = null
                                    lastPlayedSlotStartMs = null
                                }
                                AreGuideCell(
                                    title = slot.title,
                                    time = timeLabel,
                                    onClick = {
                                        // A catch-up-eligible past cell opens the action menu (watch from
                                        // start / go live); any other cell plays the live channel as before.
                                        if (slot.catchupEligible) {
                                            catchupMenu = row.channel to slot
                                        } else {
                                            lastPlayedChannelId = row.channel.id
                                            lastPlayedSlotStartMs = slot.startMs
                                            onChannelSelected(row.channel.id)
                                        }
                                    },
                                    live = slot.isNow,
                                    now = slot.isNow,
                                    catchup = slot.catchupEligible,
                                    width = cell.width,
                                    onFocusChange = { isFocused -> if (isFocused) viewModel.setFocused(GuideFocusedInfo(row.channel, slot)) },
                                    // Absolutely positioned rather than sequentially laid out: with cells
                                    // culled there is no longer a complete run of siblings for
                                    // `spacedBy` to space, so each cell carries the x it would have had.
                                    modifier = Modifier
                                        .offset(x = cell.x)
                                        .focusRequester(focusRequester),
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

    if (categoryPickerOpen) {
        GuideCategoryDialog(
            categories = state.groupCounts,
            pinned = state.pinnedGroups,
            selected = state.selectedGroup,
            onSelect = {
                viewModel.selectGroup(it)
                categoryPickerOpen = false
            },
            onTogglePin = viewModel::togglePinnedGroup,
            onDismiss = { categoryPickerOpen = false },
        )
    }

    catchupMenu?.let { (channel, slot) ->
        CatchupActionDialog(
            channelName = channel.name,
            programTitle = slot.title,
            programStartMs = slot.programStartMs,
            programEndMs = slot.programEndMs,
            onWatchFromStart = {
                catchupMenu = null
                lastPlayedChannelId = channel.id
                lastPlayedSlotStartMs = slot.startMs
                onCatchup(channel.id, slot.programStartMs, slot.programEndMs)
            },
            onGoLive = {
                catchupMenu = null
                lastPlayedChannelId = channel.id
                lastPlayedSlotStartMs = slot.startMs
                onChannelSelected(channel.id)
            },
            onDismiss = { catchupMenu = null },
        )
    }
}

/** Combining marks left by an NFD decomposition -- stripping them is what makes the category
 *  filter diacritic-insensitive ("Deportes" matches "Déportes", "Turkiye" matches "Türkiye"). */
private val CombiningMarks = Regex("\\p{Mn}+")

private fun String.foldForSearch(): String =
    java.text.Normalizer.normalize(this, java.text.Normalizer.Form.NFD).replace(CombiningMarks, "").lowercase()

/**
 * The Guide's category picker: search field + the category list with pinned entries floated to the
 * top (hold OK on a row to pin/unpin). Replaces the old chip strip -- see the header Row above.
 *
 * A real [androidx.compose.ui.window.Dialog] window, not an inline overlay, so D-pad focus is
 * trapped and can't leak into the guide grid behind it (CLAUDE.md's dialog rule).
 */
@Composable
private fun GuideCategoryDialog(
    categories: List<com.arashrahimi46.iptv.data.db.CategoryCount>,
    pinned: Set<String>,
    selected: String,
    onSelect: (String) -> Unit,
    onTogglePin: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = AreIptvTheme.colors
    var query by remember { mutableStateOf("") }
    val searchFocus = remember { FocusRequester() }

    // Folded once per catalog, not once per keystroke -- Normalizer over a few hundred names on
    // every character typed is the kind of per-frame work that shows up as jank on a TV.
    val folded = remember(categories) { categories.map { it.name.foldForSearch() } }
    val visible = remember(folded, query, pinned) {
        val needle = query.foldForSearch().trim()
        categories.indices
            .filter { needle.isEmpty() || folded[it].contains(needle) }
            // Pinned first, otherwise the provider's own order is preserved (it is usually
            // meaningful) -- so this is a stable partition, not a re-sort.
            .sortedBy { if (categories[it].name in pinned) 0 else 1 }
            .map { categories[it] }
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
    ) {
        AreDialog(
            onDismiss = onDismiss,
            title = stringResource(R.string.browse_categories_header),
            width = 520.dp,
            // No action row. A Close button here bought nothing -- Back already dismisses, and
            // picking a category dismisses -- while AreDialog's fixed 32dp gap above the actions
            // rendered as a dead black band under the list.
        ) {
            // activateOnClick: D-pad'ing DOWN past this field into the list must NOT pop the IME
            // (CLAUDE.md's TV text-input rule). OK enters edit mode, Back/Done leaves it.
            com.arashrahimi46.iptv.ui.components.AreTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = stringResource(R.string.browse_category_search_placeholder),
                icon = Icons.Filled.Search,
                activateOnClick = true,
                modifier = Modifier.focusRequester(searchFocus),
            )
            Box(Modifier.height(8.dp))
            if (visible.isEmpty()) {
                Text(
                    text = stringResource(R.string.browse_category_search_empty),
                    style = AreIptvTheme.typography.body,
                    color = colors.textSecondary,
                )
            } else {
                // Lazy, and height-capped: a real playlist has hundreds of groups, and eagerly
                // composing every one (each a focusable + glow) is what makes a fast flick stutter.
                LazyColumn(
                    // heightIn(max), NOT a fixed height. Capped because the dialog also carries a
                    // title and the search field, and an uncapped list on a 300-category playlist
                    // pushes the card off a 540dp-tall screen. Bounded rather than fixed because a
                    // filtered list of two results must shrink to two rows -- a fixed height left
                    // the rest as a black band under them. 264dp = exactly four 56dp rows + gaps.
                    modifier = Modifier.fillMaxWidth().heightIn(max = 264.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    // Room INSIDE the viewport at both ends so a focused row's ring/glow is never
                    // shaved off against the list edge -- the ring is drawn outside the row's bounds.
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp),
                ) {
                    // Keyed by name, not position: pinning floats a row, and the filter renumbers
                    // the list, so index-keyed slots would reuse BY POSITION and leave each row's
                    // remembered interaction source attached to the wrong category.
                    items(visible, key = { it.name }, contentType = { "category" }) { category ->
                        com.arashrahimi46.iptv.ui.components.AreCategoryRow(
                            name = category.name,
                            onClick = { onSelect(category.name) },
                            count = category.count,
                            active = category.name == selected,
                            pinned = category.name in pinned,
                            onLongClick = { onTogglePin(category.name) },
                        )
                    }
                }
            }
        }
    }
    // Focus the search field on open -- typing is the fast path through a few hundred categories.
    LaunchedEffect(Unit) { runCatching { searchFocus.requestFocus() } }
}

/**
 * Catch-up action menu (docs/catchup-v1-design.md, D4). A focus-trapped [AreDialog] wrapped in a real
 * [Dialog] window (same convention as [com.arashrahimi46.iptv.ui.home.HomeAddSectionDialog]); default
 * focus lands on "Watch from start". "Record" is shown disabled as the seam for the future DVR work.
 */
@Composable
private fun CatchupActionDialog(
    channelName: String,
    programTitle: String,
    programStartMs: Long,
    programEndMs: Long,
    onWatchFromStart: () -> Unit,
    onGoLive: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = AreIptvTheme.colors
    val zone = ZoneId.systemDefault()
    val timeFormatter = rememberClockFormatter()
    val start = Instant.ofEpochMilli(programStartMs).atZone(zone).format(timeFormatter)
    val end = Instant.ofEpochMilli(programEndMs).atZone(zone).format(timeFormatter)
    val watchFocus = remember { androidx.compose.ui.focus.FocusRequester() }
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
    ) {
        AreDialog(
            onDismiss = onDismiss,
            title = programTitle,
            width = 440.dp,
            actions = {
                com.arashrahimi46.iptv.ui.components.AreButton(
                    text = stringResource(R.string.action_close),
                    onClick = onDismiss,
                    variant = com.arashrahimi46.iptv.ui.components.AreButtonVariant.Ghost,
                )
            },
        ) {
            Text(
                text = "⟲ ${stringResource(R.string.guide_catchup_label)} · $channelName",
                style = AreIptvTheme.typography.caption,
                color = colors.success,
            )
            Box(Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.guide_catchup_aired, start, end),
                style = AreIptvTheme.typography.caption,
                color = colors.textTertiary,
            )
            Box(Modifier.height(AreIptvTheme.spacing.sp5))
            com.arashrahimi46.iptv.ui.components.AreButton(
                text = stringResource(R.string.guide_catchup_watch_from_start),
                onClick = onWatchFromStart,
                variant = com.arashrahimi46.iptv.ui.components.AreButtonVariant.Primary,
                full = true,
                modifier = Modifier.fillMaxWidth().focusRequester(watchFocus),
            )
            Box(Modifier.height(8.dp))
            com.arashrahimi46.iptv.ui.components.AreButton(
                text = stringResource(R.string.guide_catchup_go_live),
                onClick = onGoLive,
                variant = com.arashrahimi46.iptv.ui.components.AreButtonVariant.Secondary,
                full = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Box(Modifier.height(8.dp))
            com.arashrahimi46.iptv.ui.components.AreButton(
                text = stringResource(R.string.guide_catchup_record_soon),
                onClick = {},
                variant = com.arashrahimi46.iptv.ui.components.AreButtonVariant.Secondary,
                full = true,
                disabled = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
    LaunchedEffect(Unit) { runCatching { watchFocus.requestFocus() } }
}

@Composable
private fun TimelineHeader(windowStartMs: Long, windowEndMs: Long, zone: ZoneId, scrollState: ScrollState) {
    val timeFormatter = rememberClockFormatter()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Fixed spacer over the pinned channel column -- keeps the first time mark aligned
        // to where the programme lane begins (the rail's own styling names it as channels).
        Box(Modifier.width(AreIptvTheme.spacing.guideChannelWidth))
        // Only the time marks scroll -- shares scrollState with the rows below.
        Row(modifier = Modifier.weight(1f).horizontalScroll(scrollState)) {
            var mark = windowStartMs
            while (mark < windowEndMs) {
                Box(Modifier.width(DpPerMinute * 30)) {
                    Text(
                        text = Instant.ofEpochMilli(mark).atZone(zone).format(timeFormatter),
                        style = AreIptvTheme.typography.mono,
                        color = AreIptvTheme.colors.textTertiary,
                    )
                }
                mark += 30 * 60_000L
            }
        }
    }
}

/**
 * One programme lane's geometry, precomputed once per channel's slot list.
 *
 * The Guide's cost was never the rows -- those went lazy in P0.2 -- it was that each visible row then
 * composed EVERY cell of the whole ~6h window regardless of horizontal scroll position. At a dozen-plus
 * cells per lane and 2-3 `Text`s per cell, a Guide entry was several hundred text measures, which the
 * XL95 charges ~1-1.5ms each (docs/glass-render-perf-findings.md). This is what lets a lane skip the
 * cells that are off screen.
 *
 * Positions are explicit because they have to be: sequential layout derives each cell's x from its
 * predecessors, which stops working the moment the predecessors aren't composed. The x values here are
 * exactly what `spacedBy(6.dp)` produced, so the grid is pixel-identical.
 */
private class GuideLane(
    val cells: List<GuideLaneCell>,
    val totalWidth: Dp,
    private val xPx: IntArray,
    private val endPx: IntArray,
) {
    /**
     * Indices of the cells to compose for a viewport of [viewportPx] scrolled to [scrollPx].
     *
     * Padded by a full viewport on each side, and that margin is load-bearing rather than a comfort
     * buffer: D-pad focus can only travel to a composed focusable, so a cell the user is about to
     * step onto must already exist. One viewport of slack means the next cell is always composed --
     * reaching an unbuilt one would take a single keypress crossing the entire visible width.
     */
    fun visibleRange(scrollPx: Int, viewportPx: Int): IntRange {
        if (viewportPx <= 0 || cells.isEmpty()) return cells.indices
        val from = scrollPx - viewportPx
        val to = scrollPx + viewportPx * 2
        var first = -1
        var last = -1
        for (i in cells.indices) {
            if (endPx[i] >= from && xPx[i] <= to) {
                if (first < 0) first = i
                last = i
            }
        }
        return if (first < 0) IntRange.EMPTY else first..last
    }
}

private class GuideLaneCell(val slot: GuideProgramSlot, val x: Dp, val width: Dp)

/** Lays [slots] out at the same widths and 6dp gaps the lane used when it was a plain `Row`. */
@Composable
private fun rememberGuideLane(slots: List<GuideProgramSlot>): GuideLane {
    val density = LocalDensity.current
    return remember(slots, density) {
        var x = 0.dp
        val cells = slots.map { slot ->
            val durationMinutes = ((slot.endMs - slot.startMs) / 60000L).coerceAtLeast(1L)
            // Clamp so window-clipped boundary programmes (1-2 min) don't compute a <=0.dp,
            // invisible/unfocusable cell.
            val width = ((DpPerMinute * durationMinutes.toInt()) - 6.dp).coerceAtLeast(24.dp)
            GuideLaneCell(slot, x, width).also { x += width + 6.dp }
        }
        // Trailing gap trimmed: `spacedBy` puts gaps BETWEEN cells, not after the last one.
        val total = (x - 6.dp).coerceAtLeast(0.dp)
        with(density) {
            GuideLane(
                cells = cells,
                totalWidth = total,
                xPx = IntArray(cells.size) { cells[it].x.roundToPx() },
                endPx = IntArray(cells.size) { (cells[it].x + cells[it].width).roundToPx() },
            )
        }
    }
}

@Composable
private fun ChannelHeaderCell(channel: com.arashrahimi46.iptv.data.model.Channel) {
    val colors = AreIptvTheme.colors
    val shape = RoundedCornerShape(AreIptvTheme.radius.sm)
    Row(
        modifier = Modifier
            .width(AreIptvTheme.spacing.guideChannelWidth)
            .height(AreIptvTheme.spacing.guideRowHeight)
            // surface2 + a defined border deliberately set the pinned rail apart from the
            // programme cells (surface1) beside it -- and give the rail a visible edge on the
            // near-white light-theme background, where a borderless white-on-white cell vanished.
            .glassSurface(shape)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ChannelLogo(logoUrl = channel.logoUrl, name = channel.name)
        Column(modifier = Modifier.weight(1f)) {
            if (channel.number != null) {
                Text(text = channel.number, style = AreIptvTheme.typography.mono, color = colors.textTertiary, maxLines = 1)
            }
            // caption (14sp), not label (16sp): at the narrowed rail width the name is the thing
            // being traded against programme space, and 14sp keeps ~2 more characters visible.
            Text(text = channel.name, style = AreIptvTheme.typography.caption, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/**
 * The channel's real logo, falling back to its initials only while (or if) the logo doesn't
 * resolve. The guide rail used to show initials unconditionally -- the provider logo was never
 * requested at all, so every row read as grey alt-text.
 *
 * [rememberTileArtwork] is the same decoder [com.arashrahimi46.iptv.ui.components.AreChannelTile]
 * uses: Coil-cached and size-bounded, so a guide row shares the tile's already-decoded bitmap
 * rather than issuing a second request to a provider that rate-limits by IP.
 */
@Composable
private fun ChannelLogo(logoUrl: String?, name: String, size: Dp = 34.dp) {
    val colors = AreIptvTheme.colors
    val shape = RoundedCornerShape(AreIptvTheme.radius.xs)
    Box(
        // logoWellScrim, not surface3: it is the luminance floor that keeps the white-on-transparent
        // logos most providers ship visible -- in BOTH themes (see Color.kt:logoWell).
        modifier = Modifier.size(size).background(colors.logoWellScrim, shape),
        contentAlignment = Alignment.Center,
    ) {
        val logo = rememberTileArtwork(logoUrl, maxDimension = size)
        if (logo == null) {
            Text(text = name.take(3).uppercase(), style = AreIptvTheme.typography.caption, color = colors.logoWellText)
        } else {
            androidx.compose.foundation.Image(
                bitmap = logo,
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(3.dp),
            )
        }
    }
}

/**
 * Takes the [GuideViewModel], not the value: collecting `focused` HERE keeps the recomposition that
 * every D-pad move triggers confined to this one Row. Read at the screen root it invalidated the
 * whole Guide -- rows, cells, timeline -- once per keypress, which is what made fast cell-to-cell
 * travel stutter on a TV SoC.
 */
@Composable
private fun FocusedInfoBar(viewModel: GuideViewModel) {
    // `.value` rather than `by`: the null checks below smart-cast off a plain val, not a delegate.
    val info = viewModel.focused.collectAsState().value
    val colors = AreIptvTheme.colors
    val zone = ZoneId.systemDefault()
    val timeFormatter = rememberClockFormatter()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glassSurface(RoundedCornerShape(AreIptvTheme.radius.md))
            // 8dp/32dp, down from 12dp/40dp: this bar sits between the header and the grid, so every
            // dp it takes is a dp the channel rows don't get on a 540dp-tall screen.
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ChannelLogo(
            logoUrl = info?.channel?.logoUrl,
            name = info?.channel?.name ?: stringResource(R.string.guide_no_channel_placeholder),
            size = 32.dp,
        )
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = info?.slot?.title ?: stringResource(R.string.guide_focus_hint),
                    // label (16sp) rather than h3 (21sp) -- one line of the bar's height back.
                    style = AreIptvTheme.typography.label,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (info?.slot?.isNow == true) {
                    Box(Modifier.size(6.dp).background(colors.live, CircleShape))
                    Text(text = stringResource(R.string.guide_live_badge), style = AreIptvTheme.typography.caption, color = colors.live)
                }
            }
            if (info != null) {
                val start = Instant.ofEpochMilli(info.slot.startMs).atZone(zone).format(timeFormatter)
                val end = Instant.ofEpochMilli(info.slot.endMs).atZone(zone).format(timeFormatter)
                Text(
                    text = "${info.channel.name} · $start – $end",
                    style = AreIptvTheme.typography.caption,
                    color = colors.textSecondary,
                )
            }
        }
    }
}
