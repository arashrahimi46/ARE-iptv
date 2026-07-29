package com.arashrahimi46.iptv.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.round
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.arashrahimi46.iptv.core.R
import com.arashrahimi46.iptv.data.model.Channel
import com.arashrahimi46.iptv.data.model.VodTitle
import com.arashrahimi46.iptv.ui.components.AreBadge
import com.arashrahimi46.iptv.ui.components.AreBadgeTone
import com.arashrahimi46.iptv.ui.components.AreButton
import com.arashrahimi46.iptv.ui.components.AreButtonVariant
import com.arashrahimi46.iptv.ui.components.AreCategoryCard
import com.arashrahimi46.iptv.ui.components.AreCategoryKind
import com.arashrahimi46.iptv.ui.components.AreChannelTile
import com.arashrahimi46.iptv.ui.components.AreDialog
import com.arashrahimi46.iptv.ui.components.ArePosterTile
import com.arashrahimi46.iptv.ui.components.AreRail
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.theme.rememberPlaybackFocusRequester
import com.arashrahimi46.iptv.ui.theme.requestFocusWhenReady

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
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onChannelSelected: (Channel) -> Unit,
    onTitleSelected: (VodTitle) -> Unit,
    modifier: Modifier = Modifier,
    // Hoisted to the shell so the top-bar Customize action can drive it (see MainActivity).
    editMode: Boolean = false,
    onCategorySelected: (String) -> Unit = {},
    // "See all" on a built-in rail -> jump to that full tab (route id, e.g. "live"/"movies"/"series").
    onSeeAll: (String) -> Unit = {},
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

    // Step 4: local editable copy of the layout, seeded from state.sections and re-synced
    // whenever a fresh layout arrives -- EXCEPT mid-grab, where the in-progress reorder is the
    // source of truth until it's dropped (persisted) and the DataStore flow catches back up.
    var workingSections by remember { mutableStateOf(state.sections) }
    var grabbedIndex by remember { mutableStateOf<Int?>(null) }
    var showAddSectionDialog by remember { mutableStateOf(false) }
    // Long-press target for the "Remove from Continue Watching" confirm dialog (null = closed).
    var removeTarget by remember { mutableStateOf<HomeContinueWatchingItem?>(null) }
    // editMode is hoisted (a param); when it's turned off from the top bar, drop any in-progress grab.
    LaunchedEffect(editMode) { if (!editMode) grabbedIndex = null }
    LaunchedEffect(state.sections) {
        if (grabbedIndex == null) workingSections = state.sections
    }
    // One FocusRequester per edit-mode row, recreated whenever the row count changes -- used to
    // keep D-pad focus on the grabbed row as it moves to a new list position (see onMove below;
    // a plain forEachIndexed loop has no per-item key, so recomposition alone doesn't move focus
    // for us the way a keyed lazy list might).
    val rowFocusRequesters = remember(workingSections.size) { List(workingSections.size) { FocusRequester() } }
    // Attached to the grabbed row so the scroll container follows it. The focused node MOVES to a
    // new slot without a fresh focus event, so verticalScroll's on-focus auto-scroll never
    // re-fires when a section is pushed past the viewport edge -- we pull it into view explicitly.
    val bringGrabbedIntoView = remember { BringIntoViewRequester() }
    LaunchedEffect(grabbedIndex) {
        grabbedIndex?.let { index ->
            runCatching { rowFocusRequesters.getOrNull(index)?.requestFocus() }
            // The row springs to its new slot via animatePlacement. A bringIntoView fired now sees
            // the row still at its OLD, on-screen position and no-ops; by the time it settles below
            // the fold nothing has scrolled. Re-issue across the spring's lifetime so a call lands
            // once the row has actually moved off-screen and scrolls it back into view (each call
            // is a cheap no-op while the row is already visible).
            repeat(6) {
                delay(60)
                runCatching { bringGrabbedIntoView.bringIntoView() }
            }
        }
    }

    // "Recommended" is now a real personalized best-of across movies+series, ranked by the
    // categories the user pins/favorites/watches (see [HomeRailCurator]); falls back to top-rated
    // "Popular" before any signal exists. Drop titles already on the Continue Watching rail above
    // so this stays discovery, not a repeat of what you're mid-watching.
    //
    // PERF: remembered against its two inputs. Recomputed inline it minted a fresh Set + List on
    // every Home recomposition (an EPG tick, a resume-progress write), and since it is a rail's
    // `items()` argument that new identity invalidated that LazyRow's whole item provider.
    val recommended = remember(state.recommended, state.continueWatching) {
        val continuingIds = state.continueWatching.mapNotNull { it.vodTitleId }.toSet()
        state.recommended.filterNot { it.id in continuingIds }
    }
    // The rails that actually render, resolved once instead of re-filtered per recomposition --
    // this is the LazyColumn's item list, so a stable identity is what keeps it from re-running
    // its item provider on every state emission.
    val visibleSections = remember(state, recommended) {
        state.sections.filter { !it.hidden && sectionHasContent(it, state, recommended) }
    }

    // Initial D-pad focus, matching every other tab: the persistent shell leaves focus on the
    // sidebar, so Home reads as dead until the user blindly presses RIGHT. Lands on the first tile
    // of the first rail. Stands down when returning from the player, where
    // rememberPlaybackFocusRequester puts focus back on the tile that started playback, and in edit
    // mode, whose reorder choreography drives focus itself.
    val contentFocusRequester = remember { FocusRequester() }
    LaunchedEffect(editMode, visibleSections.isEmpty()) {
        if (!editMode && lastPlayedChannelId == null && visibleSections.isNotEmpty()) {
            contentFocusRequester.requestFocusWhenReady()
        }
    }

    // Wraps the whole screen (rails + the step 6 dialog below) so the dialog actually
    // overlays on top instead of being laid out as an invisible/misplaced sibling -- HomeScreen's
    // caller gives it a single-child slot, so two top-level composables side by side here (rails,
    // then a bare conditional dialog) had no shared layout parent to overlay them within.
    Box(modifier = modifier.fillMaxSize()) {
    // Design review: the big top-of-screen Hero banner is gone -- rail tiles/cards
    // below are sized down instead (see the explicit `width =` overrides on each rail
    // item further down) so more of the catalog fits on screen without scrolling as much.
    if (!editMode) {
        // PERF: a real LazyColumn, not an eager Column inside the shell's verticalScroll (Home is
        // a FullSizeTab now, see MainActivity). Every rail used to compose, measure and draw up
        // front and then stay in the display list -- with the default layout that is 6-10 rails
        // whose LazyRows each hold ~7 live glass tiles (wash + gradient border + softShadow path +
        // AsyncImage), i.e. ~40-50 tiles alive at once, all re-recorded on every scroll frame.
        // A desktop-GPU emulator absorbs that; a TV SoC drops frames on it. Rails below the fold
        // now simply do not exist until they scroll in.
        LazyColumn(contentPadding = PaddingValues(bottom = spacing.sp16)) {
            if (!state.hasSource || (state.channels.isEmpty() && state.movies.isEmpty())) {
                item(key = "hero") {
                    // QA LOW defect: a real source existed but Room hadn't emitted its first
                    // catalog read yet (cold-start DB open on a large catalog can take several
                    // seconds) -- showing EmptyHero() here read as "your playlist vanished"
                    // rather than "still loading".
                    Column(modifier = Modifier.padding(horizontal = spacing.safeX, vertical = spacing.sp10)) {
                        if (state.isInitializing) LoadingHero() else EmptyHero()
                    }
                }
            }
            // Home layout customization (step 1-3): rails are driven by state.sections instead of
            // being hardcoded here. With the default layout this renders byte-for-byte the same
            // rails, in the same order, as the old fixed sequence did.
            itemsIndexed(
                visibleSections,
                key = { _, section -> homeSectionKey(section) },
                contentType = { _, section -> homeSectionContentType(section) },
            ) { index, section ->
                HomeSectionContent(
                    section = section,
                    contentFocusRequester = contentFocusRequester.takeIf { index == 0 },
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
                    onRequestRemove = { removeTarget = it },
                    showSeeAll = true,
                    onSeeAll = onSeeAll,
                )
            }
        }
    } else {
        // Edit mode stays eager on purpose: it shows every section (including hidden ones), so
        // there is nothing to virtualize away, and its reorder choreography (animatePlacement +
        // the grabbed row's focus/bringIntoView) depends on rows staying composed while they move
        // -- a LazyColumn would dispose the grabbed row the moment it left the viewport.
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(bottom = spacing.sp16),
        ) {
            workingSections.forEachIndexed { index, section ->
                // Stable per-section identity so a reorder MOVES the row's node to its new slot
                // (rather than swapping data between fixed slots) -- that node movement is what
                // [animatePlacement] animates. Keyless forEachIndexed gave no such movement.
                key(homeSectionKey(section)) {
                HomeSectionEditRow(
                    modifier = Modifier.animatePlacement(),
                    headerModifier = if (grabbedIndex == index) Modifier.bringIntoViewRequester(bringGrabbedIntoView) else Modifier,
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
                    onDelete = if (section is HomeSection.Category) {
                        {
                            val updated = workingSections.toMutableList().apply { removeAt(index) }
                            workingSections = updated
                            viewModel.updateLayout(updated)
                        }
                    } else {
                        null
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
                        onRequestRemove = { removeTarget = it },
                        showSeeAll = false,
                        onSeeAll = {},
                    )
                }
                }
            }
            Box(modifier = Modifier.padding(horizontal = spacing.safeX, vertical = spacing.sp4)) {
                AreButton(
                    text = stringResource(R.string.home_add_section),
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

    removeTarget?.let { target ->
        // Rendered in a real Dialog WINDOW (not inline) so it traps D-pad focus -- an inline
        // overlay let focus leak to the tiles behind it (they stayed selectable) and the modal's
        // own buttons never received focus. Same pattern as HomeAddSectionDialog / the shell exit
        // dialog. Focus lands on Remove so the modal is immediately actionable.
        val removeFocus = remember { FocusRequester() }
        Dialog(
            onDismissRequest = { removeTarget = null },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            LaunchedEffect(Unit) { runCatching { removeFocus.requestFocus() } }
            AreDialog(
                onDismiss = { removeTarget = null },
                title = stringResource(R.string.home_remove_continue_watching_title),
                actions = {
                    AreButton(stringResource(R.string.action_cancel), onClick = { removeTarget = null }, variant = AreButtonVariant.Ghost)
                    AreButton(
                        stringResource(R.string.action_remove),
                        onClick = {
                            viewModel.removeContinueWatching(target)
                            removeTarget = null
                        },
                        variant = AreButtonVariant.Danger,
                        modifier = Modifier.focusRequester(removeFocus),
                    )
                },
            ) {
                Text(
                    text = stringResource(R.string.home_remove_continue_watching_body, target.title),
                    style = AreIptvTheme.typography.body,
                    color = AreIptvTheme.colors.textSecondary,
                )
            }
        }
    }
    }
}

/** Stable identity for an edit-mode section row, invariant across hide-toggles and reorders, so
 * Compose keeps (and moves) the same node when the layout order changes -- required for the
 * [animatePlacement] slide. */
private fun homeSectionKey(section: HomeSection): String = when (section) {
    is HomeSection.Builtin -> "b:${section.key}"
    is HomeSection.Category -> "c:${homeCategoryRailKey(section.kind, section.name)}"
}

/** PERF: which tile shape a rail renders -- poster (2:3), channel (16:9), or category card.
 * Without this, every rail in the outer LazyColumn shares the default null contentType, so
 * Compose's slot-reuse pool can't tell a poster rail apart from a channel rail and may try to
 * reuse a disposed node of the wrong shape when a differently-shaped rail scrolls into view,
 * forcing a full rebuild instead of a cheap skip. */
private fun homeSectionContentType(section: HomeSection): String = when (section) {
    is HomeSection.Builtin -> when (section.key) {
        BuiltinSection.LIVE_NOW -> "channel"
        BuiltinSection.CONTINUE_WATCHING, BuiltinSection.RECOMMENDED, BuiltinSection.MOVIES, BuiltinSection.SERIES -> "poster"
    }
    is HomeSection.Category -> if (section.kind == CategoryKind.LIVE) "channel" else "poster"
}

/** Animates a child to its new position within its parent whenever layout re-places it (e.g. an
 * edit-mode reorder). Standard Compose recipe: track the placed position and spring the visual
 * [offset] from the old spot to the new one. */
private fun Modifier.animatePlacement(): Modifier = composed {
    val scope = rememberCoroutineScope()
    var targetOffset by remember { mutableStateOf(IntOffset.Zero) }
    var animatable by remember { mutableStateOf<Animatable<IntOffset, AnimationVector2D>?>(null) }
    this
        .onPlaced { targetOffset = it.positionInParent().round() }
        .offset {
            val anim = animatable ?: Animatable(targetOffset, IntOffset.VectorConverter).also { animatable = it }
            if (anim.targetValue != targetOffset) {
                scope.launch { anim.animateTo(targetOffset, spring(stiffness = Spring.StiffnessMediumLow)) }
            }
            anim.value - targetOffset
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
    onRequestRemove: (HomeContinueWatchingItem) -> Unit,
    showSeeAll: Boolean,
    onSeeAll: (String) -> Unit,
    // Non-null only for the FIRST visible section, so opening Home lands on its first tile.
    contentFocusRequester: FocusRequester? = null,
) {
    // PERF: each rail title is resolved INSIDE its own branch. Resolving all six up front meant
    // every section paid six resource lookups (five of them discarded) on every recomposition --
    // with the default layout, ~36 wasted lookups per Home pass.
    when (section) {
        is HomeSection.Builtin -> when (section.key) {
            BuiltinSection.CONTINUE_WATCHING -> {
                // P1.2: hidden when empty, same as every other rail below.
                if (state.continueWatching.isNotEmpty()) {
                    AreRail(title = stringResource(R.string.home_section_continue_watching), seeAll = false, contentFocusRequester = contentFocusRequester) {
                        items(state.continueWatching, key = { it.vodTitleId?.let { id -> "v$id" } ?: "e${it.seriesEpisodeId}" }) { item ->
                            // Same portrait movie/series tile as the other rails: shows real poster art,
                            // resume progress, and (for series) an on-poster season/episode badge. Hold OK
                            // to remove the bookmark. Resume targets the exact movie/episode, not Detail.
                            ArePosterTile(
                                title = item.title,
                                onClick = {
                                    if (item.vodTitleId != null) onResumeVod(item.vodTitleId) else item.seriesEpisodeId?.let(onResumeEpisode)
                                },
                                posterUrl = item.posterUrl,
                                meta = item.meta,
                                progress = item.progress,
                                badges = item.badgeText?.let { badge -> { AreBadge(badge, tone = AreBadgeTone.Neutral) } },
                                onLongClick = { onRequestRemove(item) },
                                width = 168.dp,
                            )
                        }
                    }
                }
            }
            BuiltinSection.LIVE_NOW -> {
                if (state.channels.isNotEmpty()) {
                    // PERF: `take(20)` remembered -- inline it minted a new List identity on every
                    // recomposition, which invalidates this LazyRow's whole item provider.
                    val channels = remember(state.channels) { state.channels.take(20) }
                    AreRail(title = stringResource(R.string.home_section_live_now), seeAll = showSeeAll, onSeeAll = { onSeeAll("live") }, contentFocusRequester = contentFocusRequester) {
                        items(channels, key = { it.id }) { channel ->
                            val focusRequester = rememberPlaybackFocusRequester(lastPlayedChannelId, channel.id) { onChannelPlayed(null) }
                            AreChannelTile(
                                channel = channel.name,
                                onClick = { onChannelPlayed(channel.id); onChannelSelected(channel) },
                                number = channel.number,
                                now = nowPlayingTitles[channel.id],
                                category = channel.categoryName,
                                isRadio = channel.isRadio,
                                logoUrl = channel.logoUrl,
                                width = 260.dp,
                                modifier = Modifier.focusRequester(focusRequester),
                                lockCategory = channel.categoryName,
                            )
                        }
                    }
                }
            }
            BuiltinSection.RECOMMENDED -> {
                if (recommended.isNotEmpty()) {
                    val recommendedTitle = when (val l = state.recommendedLabel) {
                        is RecommendedLabel.BecauseYouLike -> stringResource(R.string.home_section_because_you_like, l.category)
                        RecommendedLabel.ForYou -> stringResource(R.string.home_section_for_you)
                        RecommendedLabel.Popular -> stringResource(R.string.home_section_popular)
                    }
                    AreRail(title = recommendedTitle, seeAll = showSeeAll, onSeeAll = { onSeeAll("movies") }, contentFocusRequester = contentFocusRequester) {
                        items(recommended, key = { it.id }) { title ->
                            ArePosterTile(title = title.name, onClick = { onTitleSelected(title) }, meta = listOfNotNull(title.year, title.categoryName).joinToString(" · "), rating = title.rating, posterUrl = title.posterUrl, width = 168.dp, lockCategory = title.categoryName)
                        }
                    }
                }
            }
            BuiltinSection.MOVIES -> {
                if (state.movies.isNotEmpty()) {
                    val movies = remember(state.movies) { state.movies.take(20) }
                    AreRail(title = stringResource(R.string.home_section_movies), seeAll = showSeeAll, onSeeAll = { onSeeAll("movies") }, contentFocusRequester = contentFocusRequester) {
                        items(movies, key = { it.id }) { movie ->
                            ArePosterTile(title = movie.name, onClick = { onTitleSelected(movie) }, meta = listOfNotNull(movie.year, movie.categoryName).joinToString(" · "), rating = movie.rating, posterUrl = movie.posterUrl, width = 168.dp, lockCategory = movie.categoryName)
                        }
                    }
                }
            }
            BuiltinSection.SERIES -> {
                if (state.series.isNotEmpty()) {
                    val series = remember(state.series) { state.series.take(20) }
                    AreRail(title = stringResource(R.string.home_section_series), seeAll = showSeeAll, onSeeAll = { onSeeAll("series") }, contentFocusRequester = contentFocusRequester) {
                        items(series, key = { it.id }) { show ->
                            ArePosterTile(title = show.name, onClick = { onTitleSelected(show) }, meta = show.categoryName, rating = show.rating, posterUrl = show.posterUrl, width = 168.dp, lockCategory = show.categoryName)
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
                        AreRail(title = section.name, seeAll = showSeeAll, onSeeAll = { onCategorySelected(section.name) }, contentFocusRequester = contentFocusRequester) {
                            items(content.channels, key = { it.id }) { channel ->
                                val focusRequester = rememberPlaybackFocusRequester(lastPlayedChannelId, channel.id) { onChannelPlayed(null) }
                                AreChannelTile(
                                    channel = channel.name,
                                    onClick = { onChannelPlayed(channel.id); onChannelSelected(channel) },
                                    number = channel.number,
                                    now = nowPlayingTitles[channel.id],
                                    category = channel.categoryName,
                                    isRadio = channel.isRadio,
                                    logoUrl = channel.logoUrl,
                                    width = 260.dp,
                                    modifier = Modifier.focusRequester(focusRequester),
                                    lockCategory = channel.categoryName,
                                )
                            }
                        }
                    }
                }
                is HomeCategoryContent.Vod -> {
                    if (content.titles.isNotEmpty()) {
                        AreRail(title = section.name, seeAll = showSeeAll, onSeeAll = { onCategorySelected(section.name) }, contentFocusRequester = contentFocusRequester) {
                            items(content.titles, key = { it.id }) { title ->
                                ArePosterTile(title = title.name, onClick = { onTitleSelected(title) }, meta = listOfNotNull(title.year, title.categoryName).joinToString(" · "), rating = title.rating, posterUrl = title.posterUrl, width = 168.dp, lockCategory = title.categoryName)
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
        Text(text = stringResource(R.string.home_loading_catalog), style = AreIptvTheme.typography.h1, color = colors.textPrimary)
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
        Text(text = stringResource(R.string.home_no_playlist_title), style = AreIptvTheme.typography.h1, color = colors.textPrimary)
        Box(Modifier.padding(top = 8.dp))
        Text(
            text = stringResource(R.string.home_no_playlist_body),
            style = AreIptvTheme.typography.body,
            color = colors.textSecondary,
        )
    }
}
