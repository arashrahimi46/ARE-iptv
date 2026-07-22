package com.arashrahimi46.iptv.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.arashrahimi46.iptv.data.model.Channel
import com.arashrahimi46.iptv.data.model.VodTitle
import com.arashrahimi46.iptv.ui.components.AreButton
import com.arashrahimi46.iptv.ui.components.AreButtonVariant
import com.arashrahimi46.iptv.ui.components.AreCategoryCard
import com.arashrahimi46.iptv.ui.components.AreCategoryKind
import com.arashrahimi46.iptv.ui.components.AreChannelTile
import com.arashrahimi46.iptv.ui.components.AreContinueCard
import com.arashrahimi46.iptv.ui.components.ArePosterTile
import com.arashrahimi46.iptv.ui.components.AreRail
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.theme.rememberPlaybackFocusRequester

/**
 * Real Home dashboard (Home.jsx): rails (no top Hero banner -- removed per
 * design review so more of the catalog fits without scrolling), sourced from the active
 * playlist's parsed catalog via [HomeViewModel]. Rails degrade gracefully to
 * an empty/onboarding-prompt state rather than crashing on a fresh install
 * with no (or an empty) catalog. Channel/poster tiles open the real
 * [com.arashrahimi46.iptv.ui.player.LivePlayerScreen]/[com.arashrahimi46.iptv.ui.detail.DetailScreen]
 * for that exact row's id -- Home is one of the entry points the spec
 * requires to prove Detail is genuinely content-id-driven, not a single
 * hardcoded record.
 *
 * Home layout customization (steps 4-6): a "Customize" affordance toggles [editMode], which
 * renders every section (including hidden ones, dimmed) wrapped in [HomeSectionEditRow] for
 * reorder/hide, plus a trailing "+ Add section" tile that opens [HomeAddSectionDialog] to pin a
 * real source category. Normal mode is unchanged: hidden sections are omitted and empty sections
 * auto-hide, same as before customization existed.
 */
@Composable
fun HomeScreen(
    onChannelSelected: (Channel) -> Unit,
    onTitleSelected: (VodTitle) -> Unit,
    modifier: Modifier = Modifier,
    onCategorySelected: (String) -> Unit = {},
    // P1.2: resumes playback directly (bypassing Detail) for a Continue Watching tile --
    // it's the exact bookmarked movie/episode, not "go pick from this title's episode list".
    onResumeVod: (Long) -> Unit = {},
    onResumeEpisode: (Long) -> Unit = {},
) {
    val context = LocalContext.current
    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.factory(context.applicationContext as android.app.Application),
    )
    val state by viewModel.uiState.collectAsState()
    val nowPlayingTitles by viewModel.nowPlayingTitles.collectAsState()
    val spacing = AreIptvTheme.spacing
    // Issue #5: which channel tile started playback -- survives the screen being paused
    // while the player is pushed on top (see [rememberPlaybackFocusRequester]) so Back
    // can restore D-pad focus to it instead of leaving focus on the sidebar.
    var lastPlayedChannelId by rememberSaveable { mutableStateOf<Long?>(null) }

    var editMode by rememberSaveable { mutableStateOf(false) }
    // Step 4: local editable copy of the layout, seeded from state.sections and re-synced
    // whenever a fresh layout arrives -- EXCEPT mid-grab, where the in-progress reorder is the
    // source of truth until it's dropped (persisted) and the DataStore flow catches back up.
    var workingSections by remember { mutableStateOf(state.sections) }
    var grabbedIndex by remember { mutableStateOf<Int?>(null) }
    var showAddSectionDialog by remember { mutableStateOf(false) }
    LaunchedEffect(state.sections) {
        if (grabbedIndex == null) workingSections = state.sections
    }
    // One FocusRequester per edit-mode row, recreated whenever the row count changes -- used to
    // keep D-pad focus on the grabbed row as it moves to a new list position (see onMove below;
    // a plain forEachIndexed loop has no per-item key, so recomposition alone doesn't move focus
    // for us the way a keyed lazy list might).
    val rowFocusRequesters = remember(workingSections.size) { List(workingSections.size) { FocusRequester() } }
    LaunchedEffect(grabbedIndex) {
        grabbedIndex?.let { index -> runCatching { rowFocusRequesters.getOrNull(index)?.requestFocus() } }
    }

    // Wraps the whole screen (rails Column + the step 6 dialog below) so the dialog actually
    // overlays on top instead of being laid out as an invisible/misplaced sibling -- HomeScreen's
    // caller gives it a single-child slot, so two top-level composables side by side here (Column,
    // then a bare conditional dialog) had no shared layout parent to overlay them within.
    Box(modifier = modifier) {
    // Design review: the big top-of-screen Hero banner is gone -- rail tiles/cards
    // below are sized down instead (see the explicit `width =` overrides on each rail
    // item further down) so more of the catalog fits on screen without scrolling as much.
    Column(modifier = Modifier.padding(bottom = spacing.sp16)) {
        Box(modifier = Modifier.padding(horizontal = spacing.safeX, vertical = spacing.sp4)) {
            AreButton(
                text = if (editMode) "Done" else "Customize",
                onClick = { editMode = !editMode; grabbedIndex = null },
                variant = if (editMode) AreButtonVariant.Primary else AreButtonVariant.Secondary,
                icon = Icons.Filled.Edit,
            )
        }

        if (!state.hasSource || (state.channels.isEmpty() && state.movies.isEmpty())) {
            Box(modifier = Modifier.padding(horizontal = spacing.safeX)) {
                // QA LOW defect: a real source existed but Room hadn't emitted its first
                // catalog read yet (cold-start DB open on a large catalog can take several
                // seconds) -- showing EmptyHero() here read as "your playlist vanished"
                // rather than "still loading".
                if (state.isInitializing) LoadingHero() else EmptyHero()
            }
            Box(Modifier.padding(top = spacing.sp10))
        }

        // "Recommended" has no real personalization engine yet -- shown as a
        // catalog sample (first titles across movies+series) rather than
        // hardcoded mock data, clearly a placeholder pending a real Phase 4+ engine.
        // Copy and the "Smart"/AI badge are both neutral until there's a real
        // recommender behind this rail (product-lead ruling on qa's Phase 1 finding).
        val recommended = (state.movies + state.series).take(12)

        if (!editMode) {
            // Home layout customization (step 1-3): rails are driven by state.sections instead of
            // being hardcoded here. With the default layout this loop renders byte-for-byte the same
            // rails, in the same order, as the old fixed sequence below did.
            state.sections.forEach { section ->
                if (section.hidden) return@forEach
                if (!sectionHasContent(section, state, recommended)) return@forEach
                HomeSectionContent(
                    section = section,
                    state = state,
                    recommended = recommended,
                    nowPlayingTitles = nowPlayingTitles,
                    lastPlayedChannelId = lastPlayedChannelId,
                    onChannelPlayed = { lastPlayedChannelId = it },
                    onChannelSelected = onChannelSelected,
                    onTitleSelected = onTitleSelected,
                    onCategorySelected = onCategorySelected,
                    onResumeVod = onResumeVod,
                    onResumeEpisode = onResumeEpisode,
                )
            }
        } else {
            workingSections.forEachIndexed { index, section ->
                HomeSectionEditRow(
                    label = homeSectionLabel(section),
                    position = index,
                    total = workingSections.size,
                    hidden = section.hidden,
                    grabbed = grabbedIndex == index,
                    focusRequester = rowFocusRequesters[index],
                    onGrabToggle = { grabbedIndex = index },
                    onMove = { delta ->
                        val newIndex = (index + delta).coerceIn(0, workingSections.lastIndex)
                        if (newIndex != index) {
                            workingSections = workingSections.toMutableList().apply { add(newIndex, removeAt(index)) }
                            grabbedIndex = newIndex
                        }
                    },
                    onDrop = {
                        viewModel.updateLayout(workingSections)
                        grabbedIndex = null
                    },
                    onToggleHidden = {
                        val updated = workingSections.toMutableList()
                        updated[index] = when (val s = updated[index]) {
                            is HomeSection.Builtin -> s.copy(hidden = !s.hidden)
                            is HomeSection.Category -> s.copy(hidden = !s.hidden)
                        }
                        workingSections = updated
                        viewModel.updateLayout(updated)
                    },
                ) {
                    HomeSectionContent(
                        section = section,
                        state = state,
                        recommended = recommended,
                        nowPlayingTitles = nowPlayingTitles,
                        lastPlayedChannelId = lastPlayedChannelId,
                        onChannelPlayed = { lastPlayedChannelId = it },
                        onChannelSelected = onChannelSelected,
                        onTitleSelected = onTitleSelected,
                        onCategorySelected = onCategorySelected,
                        onResumeVod = onResumeVod,
                        onResumeEpisode = onResumeEpisode,
                    )
                }
            }
            Box(modifier = Modifier.padding(horizontal = spacing.safeX, vertical = spacing.sp4)) {
                AreButton(
                    text = "+ Add section",
                    onClick = { showAddSectionDialog = true },
                    variant = AreButtonVariant.Secondary,
                    icon = Icons.Filled.Add,
                )
            }
        }
    }

    if (showAddSectionDialog) {
        HomeAddSectionDialog(
            categories = state.availableCategories,
            onPick = { category ->
                viewModel.addCategorySection(category.kind, category.name)
                showAddSectionDialog = false
            },
            onDismiss = { showAddSectionDialog = false },
        )
    }
    }
}

/** Whether [section] would render anything in normal (non-edit) mode -- the same per-rail
 * `isNotEmpty()` auto-hide guards this codebase always applied, just factored out so edit mode
 * can deliberately skip them (a pinned-but-empty category still needs to show, dimmed, so the
 * user has something to unpin). */
private fun sectionHasContent(section: HomeSection, state: HomeUiState, recommended: List<VodTitle>): Boolean = when (section) {
    is HomeSection.Builtin -> when (section.key) {
        BuiltinSection.CONTINUE_WATCHING -> state.continueWatching.isNotEmpty()
        BuiltinSection.LIVE_NOW -> state.channels.isNotEmpty()
        BuiltinSection.CATEGORIES -> state.categories.isNotEmpty()
        BuiltinSection.RECOMMENDED -> recommended.isNotEmpty()
        BuiltinSection.MOVIES -> state.movies.isNotEmpty()
        BuiltinSection.SERIES -> state.series.isNotEmpty()
    }
    is HomeSection.Category -> when (val content = state.categoryRails[homeCategoryRailKey(section.kind, section.name)]) {
        is HomeCategoryContent.Live -> content.channels.isNotEmpty()
        is HomeCategoryContent.Vod -> content.titles.isNotEmpty()
        null -> false
    }
}

/** Renders one Home section's real rail -- the built-in arm is the original hardcoded-rail
 * sequence, unchanged (step 1-3), and the category arm is step 5: a single-type
 * [AreRail]/[AreChannelTile] or [AreRail]/[ArePosterTile] rail for a pinned real source category. */
@Composable
private fun HomeSectionContent(
    section: HomeSection,
    state: HomeUiState,
    recommended: List<VodTitle>,
    nowPlayingTitles: Map<Long, String>,
    lastPlayedChannelId: Long?,
    onChannelPlayed: (Long?) -> Unit,
    onChannelSelected: (Channel) -> Unit,
    onTitleSelected: (VodTitle) -> Unit,
    onCategorySelected: (String) -> Unit,
    onResumeVod: (Long) -> Unit,
    onResumeEpisode: (Long) -> Unit,
) {
    when (section) {
        is HomeSection.Builtin -> when (section.key) {
            BuiltinSection.CONTINUE_WATCHING -> {
                // P1.2: hidden when empty, same as every other rail below.
                if (state.continueWatching.isNotEmpty()) {
                    AreRail(title = "Continue Watching", seeAll = false) {
                        items(state.continueWatching, key = { it.vodTitleId?.let { id -> "v$id" } ?: "e${it.seriesEpisodeId}" }) { item ->
                            AreContinueCard(
                                title = item.title,
                                onClick = {
                                    if (item.vodTitleId != null) onResumeVod(item.vodTitleId) else item.seriesEpisodeId?.let(onResumeEpisode)
                                },
                                meta = item.meta,
                                progress = item.progress,
                                width = 260.dp,
                            )
                        }
                    }
                }
            }
            BuiltinSection.LIVE_NOW -> {
                if (state.channels.isNotEmpty()) {
                    AreRail(title = "Live now") {
                        items(state.channels.take(20), key = { it.id }) { channel ->
                            val focusRequester = rememberPlaybackFocusRequester(lastPlayedChannelId, channel.id) { onChannelPlayed(null) }
                            AreChannelTile(
                                channel = channel.name,
                                onClick = { onChannelPlayed(channel.id); onChannelSelected(channel) },
                                number = channel.number,
                                now = nowPlayingTitles[channel.id],
                                logoUrl = channel.logoUrl,
                                width = 260.dp,
                                modifier = Modifier.focusRequester(focusRequester),
                            )
                        }
                    }
                }
            }
            BuiltinSection.CATEGORIES -> {
                if (state.categories.isNotEmpty()) {
                    AreRail(title = "Browse by category") {
                        items(state.categories.take(20), key = { it.name }) { category ->
                            AreCategoryCard(name = category.name, onClick = { onCategorySelected(category.name) }, count = category.count, kind = AreCategoryKind.Default, width = 260.dp)
                        }
                    }
                }
            }
            BuiltinSection.RECOMMENDED -> {
                if (recommended.isNotEmpty()) {
                    AreRail(title = "Browse movies & series") {
                        items(recommended, key = { it.id }) { title ->
                            ArePosterTile(title = title.name, onClick = { onTitleSelected(title) }, meta = listOfNotNull(title.year, title.categoryName).joinToString(" · "), rating = title.rating, posterUrl = title.posterUrl, width = 168.dp)
                        }
                    }
                }
            }
            BuiltinSection.MOVIES -> {
                if (state.movies.isNotEmpty()) {
                    AreRail(title = "Movies") {
                        items(state.movies.take(20), key = { it.id }) { movie ->
                            ArePosterTile(title = movie.name, onClick = { onTitleSelected(movie) }, meta = listOfNotNull(movie.year, movie.categoryName).joinToString(" · "), rating = movie.rating, posterUrl = movie.posterUrl, width = 168.dp)
                        }
                    }
                }
            }
            BuiltinSection.SERIES -> {
                if (state.series.isNotEmpty()) {
                    AreRail(title = "Series") {
                        items(state.series.take(20), key = { it.id }) { show ->
                            ArePosterTile(title = show.name, onClick = { onTitleSelected(show) }, meta = show.categoryName, rating = show.rating, posterUrl = show.posterUrl, width = 168.dp)
                        }
                    }
                }
            }
        }
        is HomeSection.Category -> {
            // Step 5: single-type pinned category rail -- LIVE renders channel tiles, MOVIE/SERIES
            // render poster tiles, reusing the exact built-in Live-now/Movies tile setup above.
            when (val content = state.categoryRails[homeCategoryRailKey(section.kind, section.name)]) {
                is HomeCategoryContent.Live -> {
                    if (content.channels.isNotEmpty()) {
                        AreRail(title = section.name) {
                            items(content.channels, key = { it.id }) { channel ->
                                val focusRequester = rememberPlaybackFocusRequester(lastPlayedChannelId, channel.id) { onChannelPlayed(null) }
                                AreChannelTile(
                                    channel = channel.name,
                                    onClick = { onChannelPlayed(channel.id); onChannelSelected(channel) },
                                    number = channel.number,
                                    now = nowPlayingTitles[channel.id],
                                    logoUrl = channel.logoUrl,
                                    width = 260.dp,
                                    modifier = Modifier.focusRequester(focusRequester),
                                )
                            }
                        }
                    }
                }
                is HomeCategoryContent.Vod -> {
                    if (content.titles.isNotEmpty()) {
                        AreRail(title = section.name) {
                            items(content.titles, key = { it.id }) { title ->
                                ArePosterTile(title = title.name, onClick = { onTitleSelected(title) }, meta = listOfNotNull(title.year, title.categoryName).joinToString(" · "), rating = title.rating, posterUrl = title.posterUrl, width = 168.dp)
                            }
                        }
                    }
                }
                null -> Unit
            }
        }
    }
}

@Composable
private fun LoadingHero() {
    val colors = AreIptvTheme.colors
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 64.dp)) {
        Text(text = "Loading your catalog…", style = AreIptvTheme.typography.h1, color = colors.textPrimary)
    }
}

@Composable
private fun EmptyHero() {
    val colors = AreIptvTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 64.dp),
    ) {
        Text(text = "No playlist yet", style = AreIptvTheme.typography.h1, color = colors.textPrimary)
        Box(Modifier.padding(top = 8.dp))
        Text(
            text = "Add a playlist from the sidebar to see your channels, movies and series here.",
            style = AreIptvTheme.typography.body,
            color = colors.textSecondary,
        )
    }
}
