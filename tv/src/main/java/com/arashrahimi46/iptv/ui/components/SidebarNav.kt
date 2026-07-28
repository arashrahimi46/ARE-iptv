package com.arashrahimi46.iptv.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material.icons.outlined.Theaters
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.arashrahimi46.iptv.R
import com.arashrahimi46.iptv.data.settings.SidebarStyle
import com.arashrahimi46.iptv.ui.theme.AreIptvColors
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.theme.LocalGlassTier
import com.arashrahimi46.iptv.ui.theme.TvFocusable
import com.arashrahimi46.iptv.ui.theme.accentGradientBrush
import com.arashrahimi46.iptv.ui.theme.softShadow
import com.arashrahimi46.iptv.ui.theme.glassBorderBrush
import com.arashrahimi46.iptv.ui.theme.glassLens
import com.arashrahimi46.iptv.ui.theme.glassSurface
import com.arashrahimi46.iptv.ui.theme.lensContentColor

data class SidebarNavItem(val id: String, val labelRes: Int, val icon: ImageVector)

/** Default nav items per the app shell spec (app.jsx `navItems`), with placeholder Material icons. */
val DefaultSidebarNavItems = listOf(
    SidebarNavItem("home", R.string.nav_home, Icons.Outlined.Home),
    SidebarNavItem("live", R.string.nav_live_tv, Icons.Outlined.LiveTv),
    SidebarNavItem("guide", R.string.nav_tv_guide, Icons.Outlined.TableChart),
    SidebarNavItem("movies", R.string.nav_movies, Icons.Outlined.Movie),
    SidebarNavItem("series", R.string.nav_series, Icons.Outlined.Theaters),
    SidebarNavItem("search", R.string.nav_search, Icons.Outlined.Search),
    SidebarNavItem("favorites", R.string.nav_favorites, Icons.Outlined.Favorite),
    SidebarNavItem("recordings", R.string.nav_recordings, Icons.Outlined.VideoLibrary),
    SidebarNavItem("streams", R.string.nav_streams, Icons.Outlined.Link),
    SidebarNavItem("settings", R.string.nav_settings, Icons.Outlined.Settings),
)

/**
 * SidebarNav — the persistent left rail nav (SidebarNav.jsx). Collapses to icons and expands to
 * labels while any item inside holds D-pad focus. Two container styles ([SidebarStyle]): a
 * [SidebarStyle.FLOATING] glass box inset off the screen edge (default), or the [SidebarStyle.EDGE]
 * full-height rail flush to the bezel. Identical items, focus model and expand trigger either way —
 * only the surface and shape differ.
 */
@Composable
fun AreSidebarNav(
    active: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    items: List<SidebarNavItem> = DefaultSidebarNavItems,
    brand: String = "ARE",
    /** Nav ids that show a small "!" attention badge on their icon (e.g. "settings" when the
     * active playlist hasn't been refreshed in over two weeks). */
    badgedIds: Set<String> = emptySet(),
    style: SidebarStyle = SidebarStyle.FLOATING,
) {
    val colors = AreIptvTheme.colors
    val spacing = AreIptvTheme.spacing
    val motion = AreIptvTheme.motion
    val floating = style == SidebarStyle.FLOATING

    // Tracks which item currently holds D-pad focus (null = none), driving expand/collapse.
    var focusedItemId by remember { mutableStateOf<String?>(null) }
    // Per-row focus requesters, kept so a row can be focused programmatically if needed.
    // NOTE: we deliberately do NOT auto-request focus onto the active row here. The sidebar is
    // persistent across tab switches (the shell swaps only the inner content), so this composable
    // only re-enters composition when returning from a full-bleed overlay (player/detail) -- and
    // that is exactly when the browse screen restores D-pad focus to the tile the user launched
    // from (see rememberPlaybackFocusRequester). An auto-focus here stole that focus back to the
    // sidebar, which was the reported "Back always lands on the sidebar" bug. The style/inset change
    // touches nothing here -- do not add a restorer.
    val focusRequesters = remember(items) { items.associate { it.id to FocusRequester() } }
    // derivedStateOf, not a plain `focusedItemId != null`: `focusedItemId` changes on EVERY D-pad step
    // inside the rail (row -> row), and a direct read here would recompose this whole composable --
    // and, through it, SidebarNavBody's brand mark, selection lens and all ten rows -- on every one of
    // those steps, purely to recompute a Boolean that did not change. Derived, the rail recomposes
    // exactly twice per visit: once when focus enters and once when it leaves.
    val expanded by remember { derivedStateOf { focusedItemId != null } }
    val openWidth = if (floating) spacing.sidebarBoxWidthOpen else spacing.sidebarWidthOpen
    // NOTE: deliberately NOT `by` -- the State is passed down and read at LAYOUT/DRAW time. Reading
    // an animated Dp in composition recomposes this whole composable on every one of the ~13 frames
    // of an expand, which is what made the rail stutter on weak TV SoCs.
    val width = animateDpAsState(
        targetValue = when {
            floating && expanded -> spacing.sidebarBoxWidthOpen
            floating -> spacing.sidebarBoxWidth
            expanded -> spacing.sidebarWidthOpen
            else -> spacing.sidebarWidth
        },
        animationSpec = tween(motion.durBaseMs, easing = motion.easeOut),
        label = "sidebarWidth",
    )
    // Same trick for the label reveal: ONE shared alpha read inside `graphicsLayer` at draw time,
    // replacing the 11 per-row `AnimatedVisibility` containers. Those each ran their own animation
    // AND animated their own layout size, so an expand kicked off 11 concurrent layout animations
    // inside a subtree that was already being remeasured by the width animation.
    val labelAlpha = animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = tween(motion.durBaseMs, easing = motion.easeOut),
        label = "sidebarLabelAlpha",
    )
    // The EDGE rail's glass fill and its lit right-edge seam. Hoisted out of the drawBehind below
    // because that node is remeasured (and so redrawn) on every frame of the width tween -- see the
    // PERF note at the call site.
    val railFill = remember(colors.surfaceGlassElevated, colors.surfaceGlass) {
        Brush.verticalGradient(listOf(colors.surfaceGlassElevated, colors.surfaceGlass))
    }
    val railSeam = remember(colors.glassHighlight, colors.borderGlass) {
        Brush.verticalGradient(listOf(colors.glassHighlight, colors.borderGlass))
    }
    val onFocusedChanged: (String, Boolean) -> Unit = { id, focused ->
        focusedItemId = if (focused) id else focusedItemId.takeUnless { it == id }
    }

    if (floating) {
        // The 20dp gap IS the idea: that band of ambient backdrop running behind every edge is what
        // makes the rail read as a glass object floating on the page rather than chrome bolted to the
        // bezel. `glassSurface(elevated)` samples and blurs that backdrop through the 28dp shape
        // exactly as dialogs do (Tier A/B); Tier C falls through to the denser opaque fill and still
        // reads via the inset + shape + shadow.
        val panelRadius = AreIptvTheme.radius.xl
        val panelShape = RoundedCornerShape(panelRadius)
        val edgeBrush = glassBorderBrush()
        Box(modifier = modifier.fillMaxHeight().padding(spacing.sidebarInset)) {
            // The animating glass panel is CHILDLESS, and the nav content below is pinned at the open
            // width. That split is the whole fix for expand/collapse jank: previously the animated
            // width was the content's own container, so every frame remeasured the scroll viewport,
            // all 10 rows, their icons and their labels -- and rebuilt the panel's shadow path and
            // backdrop shape on top. Now a frame measures exactly one empty node; the rows are laid
            // out once and the reveal is pure alpha (a draw-time property). Visually identical: the
            // rail's content is start-aligned with a static icon inset, so it never moved during the
            // animation anyway -- only the panel edge did.
            //
            // NO BACKDROP BLUR HERE, deliberately, and this is the measured decision -- see
            // docs/glass-render-perf-findings.md. The expanded rail used to frost the page behind it
            // via `drawBackdrop`. Profiled on the XL95 (Perfetto, release, 10 expand/collapse cycles,
            // RenderThread Running -- a baseline that reproduces to +-0.6% across batches):
            //
            //   baseline (frosted)            2156 ms   ~60 frames over 16.7ms
            //   blur pass removed             1502 ms    ~4 frames over 16.7ms   <- this
            //   blur radius 28dp -> 14dp      2332 ms   -- WORSE, so it is not radius-bound
            //   frost captured from frame 0   2141 ms   -- total-neutral, per-frame heavier
            //
            // The cost is the blur PASS existing at all, not its radius: it is 30% of the whole
            // RenderThread budget and it turned 4 janky frames into 60. Halving the radius made it
            // worse, so there is no cheap-blur setting to tune -- the choice is have it or not.
            //
            // What replaces it is the same `surfaceGlassSheer` fill the panel already cross-faded
            // from, so the rail is still genuinely translucent (you see the page through it), just not
            // blurred. That also removes the OTHER reported defect for free: there is no longer a late
            // -arriving blur, so nothing pops in after the panel finishes opening, and no
            // cross-dissolve is needed to hide it. One node, one fill, correct from frame 0.
            //
            // The shell's whole page-capture apparatus (LocalPageBackdrop, the gated
            // `layerBackdrop(pageBackdrop)`, the settle delay) existed only to feed this blur and is
            // deleted with it -- which also stops double-drawing the page subtree while the rail is
            // open. Do not reintroduce a backdrop blur here without re-running that measurement.
            Box(
                Modifier
                    .widthFrom(width)
                    .fillMaxHeight()
                    // bake = false: this node's size changes every frame. See `bake`.
                    .softShadow(panelShape, bake = false)
                    .clip(panelShape)
                    .background(colors.surfaceGlassSheer)
                    // The edge is stroked in drawWithContent, not with a plain `.border()`, so it lands
                    // ON TOP of the fill above rather than beneath it (modifier draws go under a node's
                    // children, and a background is drawn before them).
                    .drawWithContent {
                        drawContent()
                        val hair = 1.dp.toPx()
                        drawRoundRect(
                            brush = edgeBrush,
                            topLeft = Offset(hair / 2f, hair / 2f),
                            size = Size(size.width - hair, size.height - hair),
                            cornerRadius = CornerRadius(panelRadius.toPx() - hair / 2f),
                            style = Stroke(hair),
                        )
                    },
            )
            Column(
                modifier = Modifier
                    .width(openWidth)
                    .fillMaxHeight()
                    .clipToPanel(width)
                    .padding(vertical = spacing.sp8),
            ) {
                SidebarNavBody(
                    floating = true,
                    items = items,
                    active = active,
                    brand = brand,
                    badgedIds = badgedIds,
                    focusRequesters = focusRequesters,
                    panelWidth = width,
                    labelAlpha = labelAlpha,
                    onSelect = onSelect,
                    onFocusedChanged = onFocusedChanged,
                )
            }
        }
    } else {
        Box(modifier = modifier.fillMaxHeight()) {
            // Same childless-panel split as FLOATING (see the comment above).
            Box(
                Modifier
                    .widthFrom(width)
                    .fillMaxHeight()
                    // The rail is a glass panel, not a flat block: a faint top-lit vertical sheen plus a
                    // lit hairline right edge (the glass seam separating rail from content). Drawn behind
                    // the nav rows. V2: the fill is the TRANSLUCENT glass token, not the opaque surface
                    // ramp -- an opaque rail killed the ambient backdrop down the whole left edge, which
                    // is the most persistent chrome in the app and so the most visible place to get wrong.
                    // PERF: both brushes are hoisted (see railFill/railSeam above) rather than built
                    // inside the lambda. This node carries .widthFrom(width), so its measure -- and
                    // therefore its draw -- is invalidated on every frame of the open/close tween;
                    // allocating two gradients plus their lists per frame put GC pressure exactly
                    // where the animation needed the budget. Size-independent gradients, so hoisting
                    // is pixel-identical. Same treatment softShadow/tileWash already document.
                    .drawBehind {
                        drawRect(railFill)
                        val edge = 1.dp.toPx()
                        drawRect(
                            brush = railSeam,
                            topLeft = Offset(size.width - edge, 0f),
                            size = Size(edge, size.height),
                        )
                    },
            )
            Column(
                modifier = Modifier
                    .width(openWidth)
                    .fillMaxHeight()
                    .clipToPanel(width)
                    .padding(vertical = spacing.sp8),
            ) {
                SidebarNavBody(
                    floating = false,
                    items = items,
                    active = active,
                    brand = brand,
                    badgedIds = badgedIds,
                    focusRequesters = focusRequesters,
                    panelWidth = width,
                    labelAlpha = labelAlpha,
                    onSelect = onSelect,
                    onFocusedChanged = onFocusedChanged,
                )
            }
        }
    }
}

/**
 * Fixes this node's width from [width] at LAYOUT time, minus [inset].
 *
 * The point is what it does NOT do: reading an animated `Dp` via `by` and passing it to
 * `Modifier.width()` makes the *composable* recompose on every animation frame. Reading the [State]
 * inside a `layout` block moves that read into the measure pass, so an animation frame invalidates
 * one node's measurement and nothing recomposes.
 */
/**
 * Clips this node to the panel's current animating [width], at DRAW time.
 *
 * The nav content is pinned at the OPEN width so an animation frame never remeasures a row (see
 * [widthFrom]) -- but that also means a focused row's ring, fill and label are drawn at full width
 * from frame 0, while the glass panel behind them is still collapsed. For the ~13 frames of an expand
 * the focus ring floated *outside* the panel, over the page content. This wipes the content in with
 * the advancing edge instead, which is what a panel sliding open should look like.
 *
 * A `clipRect`, not a rounded clip: the rows are inset from the panel's 28dp corners by their own
 * horizontal/vertical padding, so a rectangular right-edge clip is indistinguishable from the panel's
 * own rounded one -- and it costs no per-frame `Path`. Read at draw time, so this adds no layout work
 * to the tween at all; at rest (`width` == open width) it clips nothing.
 */
private fun Modifier.clipToPanel(width: State<Dp>): Modifier = drawWithContent {
    clipRect(right = width.value.toPx()) { this@drawWithContent.drawContent() }
}

private fun Modifier.widthFrom(width: State<Dp>, inset: Dp = 0.dp): Modifier = layout { measurable, constraints ->
    val w = (width.value - inset).roundToPx().coerceAtLeast(0)
    val placeable = measurable.measure(constraints.copy(minWidth = w, maxWidth = w))
    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
}

/** Brand header + the scrolling item column, shared by both container styles. */
@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
private fun ColumnScope.SidebarNavBody(
    // PERF: deliberately does NOT take `expanded`. It took it and never read it, and because an
    // unused parameter still blocks skipping, the flag flipping re-executed this whole body -- brand
    // mark, selection lens, all ten rows and their TvFocusable modifier chains -- on frame 0 of every
    // open/close tween, which is exactly where the width animation could least afford it. Everything
    // visible is driven by `panelWidth` (layout) and `labelAlpha` (draw), neither of which recomposes.
    floating: Boolean,
    items: List<SidebarNavItem>,
    active: String,
    brand: String,
    badgedIds: Set<String>,
    focusRequesters: Map<String, FocusRequester>,
    /** The animating panel width, read at layout time so the selection lens tracks the panel edge
     *  without the content being remeasured (see [widthFrom]). */
    panelWidth: State<Dp>,
    /** Shared 0..1 label reveal, read at draw time inside `graphicsLayer`. */
    labelAlpha: State<Float>,
    onSelect: (String) -> Unit,
    onFocusedChanged: (String, Boolean) -> Unit,
) {
    val colors = AreIptvTheme.colors
    val spacing = AreIptvTheme.spacing
    val hPad = if (floating) 10.dp else 16.dp
    // Blur-capable tiers keep the true alpha-mask fade (the glass is translucent, so only DstIn can
    // dissolve rows into it). Opaque tiers (older TVs) get the cheap equivalent -- a gradient in the
    // surface fill painted over the edges -- so they never pay for a per-frame offscreen layer.
    val fadeFill = if (LocalGlassTier.current.hasBackdropBlur) null else colors.surfaceGlassElevated

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            // Floating: 32dp start centres the 32dp brand mark in the 96dp collapsed capsule
            // ((96-32)/2); kept static so it doesn't shift as the label fades in on expand.
            // Edge: the original flush-rail inset.
            .padding(start = if (floating) 32.dp else 26.dp, end = if (floating) 16.dp else 26.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BrandMark(brand = brand, colors = colors)
        Text(
            text = "$brand iptv",
            style = AreIptvTheme.typography.h3,
            color = colors.textPrimary,
            maxLines = 1,
            modifier = Modifier.graphicsLayer { alpha = labelAlpha.value },
        )
    }

    Box(Modifier.height(spacing.sp10))

    // Measured top-Y (px) of each row within the list Box, so the selection lens knows where to slide.
    val rowTops = remember { mutableStateMapOf<String, Float>() }
    val activeTop = rowTops[active]
    // Buttery bit: a single glass-lens pill that SPRINGS between rows when the active tab changes,
    // instead of the lens hard-cutting to the new row. Same feel as AreSegmentedControl's indicator.
    val slide = spring<Float>(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
    // Not `by`: read inside the `offset` lambda below, i.e. at layout time. The spring settles over
    // ~20 frames and each one would otherwise recompose this whole body.
    val lensY = animateFloatAsState(activeTop ?: 0f, animationSpec = slide, label = "sidebarLensY")
    val scrollState = rememberScrollState()
    // The offscreen compositing layer the alpha-mask fade needs is a per-frame render-to-texture, and
    // 10 items fit above the fold on most TV viewports -- so there is usually nothing to dissolve.
    // Pay for the layer only when the list can actually scroll.
    val overflows by remember { derivedStateOf { scrollState.maxValue > 0 } }

    // HIGH QA defect: a fixed-height Column with no scroll clips/hides items (Settings included)
    // below the fold on shorter/denser TV viewports (e.g. the 540dp-effective Television_1080p AVD).
    // .weight(1f) takes only the space left after the header, and verticalScroll (with focusable row
    // items) lets D-pad focus auto-scroll a below-the-fold item into view.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            // Floating box only: the rounded glassSurface clip would hard-slice the first/last row at
            // the 28dp corner. An alpha mask on the scrolled content dissolves the rows INTO the glass
            // instead -- a solid fade colour can't, the surface is translucent, so the fade has to
            // subtract alpha (DstIn) and let the blurred backdrop show through.
            .then(if (floating && overflows) Modifier.scrollEdgeFade(spacing.sp6, fadeFill) else Modifier)
            .verticalScroll(scrollState)
            // vertical padding inside the scroll content so the first/last item's focus glow isn't
            // clipped at the viewport edge (the reported "top of the Home focus ring disappeared").
            .padding(horizontal = hPad, vertical = 14.dp),
    ) {
        Box(Modifier.fillMaxWidth()) {
            // The sliding selection lens, BEHIND the rows. Rows carry a transparent fill so it shows
            // through; the active row's own icon/label still take lensContentColor. It scrolls with
            // the list because it lives inside the same scrolled Box.
            if (activeTop != null) {
                Box(
                    modifier = Modifier
                        .offset { IntOffset(0, lensY.value.roundToInt()) }
                        // Tracks the panel edge instead of the (now fixed, open-width) content, so a
                        // collapsed rail still shows the lens as a pill inside the glass rather than a
                        // full-width bar spilling out of it.
                        .widthFrom(panelWidth, inset = hPad * 2)
                        .height(44.dp)
                        .glassLens(RoundedCornerShape(AreIptvTheme.radius.lg)),
                )
            }
            Column(
                // Entering the rail from the content (LEFT) used to resolve by nearest-neighbour
                // geometry, so it landed on whichever row happened to sit at the focused tile's
                // height -- pressing LEFT from a Home poster put focus on "TV Guide" while Home was
                // the current screen. The rail's entry point is a property of the rail, not of where
                // the user came from: entering always lands on the row for the screen you are on.
                // Row-to-row travel INSIDE the group doesn't re-trigger `enter`, so this costs
                // nothing once you're in.
                modifier = Modifier
                    .focusProperties { enter = { focusRequesters.getValue(active) } }
                    .focusGroup(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items.forEach { item ->
                    val itemInteractionSource = remember { MutableInteractionSource() }
                    SidebarNavRow(
                        item = item,
                        active = item.id == active,
                        labelAlpha = labelAlpha,
                        badged = item.id in badgedIds,
                        // Start inset centres the 22dp icon in each container's own collapsed width.
                        startInset = if (floating) 26.dp else 23.dp,
                        interactionSource = itemInteractionSource,
                        focusRequester = focusRequesters.getValue(item.id),
                        onClick = { onSelect(item.id) },
                        onFocusedChanged = { focused -> onFocusedChanged(item.id, focused) },
                        // Guarded: onGloballyPositioned fires on every layout pass, and an
                        // unconditional put into a SnapshotStateMap records a write even for an
                        // unchanged value -- recomposing this body (and re-reading rowTops) each time.
                        onPositioned = { top -> if (rowTops[item.id] != top) rowTops[item.id] = top },
                    )
                }
            }
        }
    }
}

/**
 * Fades the scrolled content to transparent over [fade] at the top and bottom edges, so nav rows
 * dissolve into the glass box at its rounded corners instead of being sliced by the surface clip.
 *
 * Two implementations by cost. When [opaqueFill] is null (blur-capable tiers, translucent glass) the
 * only correct way is an offscreen layer + a DstIn vertical-gradient mask that subtracts alpha from
 * the content so the blurred glass shows through -- a solid fade rectangle can't, the surface is
 * translucent. When [opaqueFill] is set (opaque tiers) the surface is a flat colour, so painting that
 * colour as a top/bottom gradient over the rows gives the same dissolve WITHOUT an offscreen layer --
 * the per-frame render-to-texture that older TV GPUs pay for continuously, scrolling or not.
 *
 * PERF: `drawWithCache`, not `drawBehind`/`drawWithContent`. These gradients depend on `size.height`,
 * so they can't be hoisted into a plain `remember` -- but built inside the draw lambda they were minted
 * (brush + colour-stop list, twice on the opaque path) on every DRAW pass, and this node is the rail's
 * scroll viewport: it redraws on every focus step through the rail AND on every frame of the width
 * tween. Same treatment `glassWell` and `railFill`/`railSeam` already document, for the same reason;
 * this call site was simply missed in that pass. The cache re-runs only on a size change.
 */
private fun Modifier.scrollEdgeFade(fade: Dp, opaqueFill: Color?): Modifier =
    if (opaqueFill == null) {
        this
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .drawWithCache {
                val f = (fade.toPx()).coerceAtMost(size.height / 2f)
                val mask = if (f <= 0f) null else Brush.verticalGradient(
                    0f to Color.Transparent,
                    f / size.height to Color.Black,
                    1f - f / size.height to Color.Black,
                    1f to Color.Transparent,
                )
                onDrawWithContent {
                    drawContent()
                    if (mask != null) drawRect(brush = mask, blendMode = BlendMode.DstIn)
                }
            }
    } else {
        this.drawWithCache {
            val f = (fade.toPx()).coerceAtMost(size.height / 2f)
            val top = if (f <= 0f) null else
                Brush.verticalGradient(0f to opaqueFill, f / size.height to Color.Transparent)
            val bottom = if (f <= 0f) null else
                Brush.verticalGradient(0f to Color.Transparent, 1f to opaqueFill)
            val edge = Size(size.width, f)
            val bottomAt = Offset(0f, size.height - f)
            onDrawWithContent {
                drawContent()
                if (top != null && bottom != null) {
                    drawRect(brush = top, size = edge)
                    drawRect(brush = bottom, topLeft = bottomAt, size = edge)
                }
            }
        }
    }

@Composable
private fun BrandMark(brand: String, colors: AreIptvColors) {
    val shape = RoundedCornerShape(AreIptvTheme.radius.sm)
    Box(
        modifier = Modifier
            .size(32.dp)
            // Solid accent gradient, NOT the selection lens: the brand mark is identity, not a
            // selected state. Turning it translucent drained it in the light theme, where the lens
            // is a white-over-accent wash. No glow: it read as a heavy halo.
            .background(accentGradientBrush(), shape),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = brand.take(1), style = AreIptvTheme.typography.h3, color = colors.accentFg)
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SidebarNavRow(
    item: SidebarNavItem,
    active: Boolean,
    labelAlpha: State<Float>,
    badged: Boolean,
    startInset: Dp,
    interactionSource: MutableInteractionSource,
    focusRequester: FocusRequester,
    onClick: () -> Unit,
    onFocusedChanged: (Boolean) -> Unit,
    onPositioned: (Float) -> Unit,
) {
    val colors = AreIptvTheme.colors
    val label = stringResource(item.labelRes)
    // Two states rendered on the row itself (design §6b): rest = transparent, focused = glass fill +
    // lit-edge gradient (on top of the TvFocusable ring). The third -- current screen = accent lens --
    // is drawn by the sliding pill BEHIND the rows (see SidebarNavBody) so selection glides, so the
    // row stays transparent when active and only lends its icon/label the lens content colour.
    val focused by interactionSource.collectIsFocusedAsState()

    TvFocusable(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            // Smaller, tighter rows per the design artifact (rail-item: 22px icon, ~44dp tall) --
            // the rail reads lighter and shinier occupying less vertical space than before.
            .height(44.dp)
            .onGloballyPositioned { onPositioned(it.positionInParent().y) }
            .focusRequester(focusRequester),
        interactionSource = interactionSource,
        shape = RoundedCornerShape(AreIptvTheme.radius.lg),
        // rest = transparent, focused = glass fill + lit edge. Active fill/rim come from the sliding
        // lens, so the row adds nothing when active-and-unfocused. No shadow -- the lens sits on the
        // flat rail, so a drop shadow reads as heavy; the lens rim marks it.
        backgroundColor = if (focused && !active) colors.surfaceGlass else Color.Transparent,
        backgroundBrush = null,
        borderBrush = if (focused) glassBorderBrush() else null,
        showFocusSheen = false,
        // The selection lens is drawn behind the rows and does NOT scale, so a 6% focus grow would
        // enlarge the ring past the lens and open a gap around an active+focused row. Full-width rail
        // rows don't need the grow anyway -- the ring + glow carry focus.
        disableScale = true,
    ) { isFocused, _ ->
        LaunchedEffect(isFocused) { onFocusedChanged(isFocused) }
        Row(
            modifier = Modifier
                // fillMaxHeight so this content Row spans the full 52dp row height and
                // CenterVertically can actually center the icon+label. Without it the Row
                // wraps to the 26dp icon height and TvFocusable's Box places it at the top,
                // leaving the content visually stuck to the top edge of the focus ring while
                // the (offset) glow pools in the empty lower half.
                .fillMaxWidth()
                .fillMaxHeight()
                // [startInset] centres the 22dp icon in the collapsed container's own width. Kept
                // static (not switched by `expanded`) so the icon stays put through the width
                // animation -- a conditional arrangement made it jump between centred and
                // left-aligned mid-animation. Trailing inset is much smaller than the leading one:
                // the start inset exists only to centre the icon, and mirroring it on the end stole
                // width from the label -- enough that long RTL labels ("راهنمای تلویزیون") wrapped to
                // two lines. Start-aligned content, so the icon does not move.
                .padding(start = startInset, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(contentAlignment = Alignment.TopEnd) {
                Icon(
                    item.icon,
                    contentDescription = label,
                    tint = if (active) lensContentColor() else colors.textTertiary,
                    modifier = Modifier.size(22.dp),
                )
                // "!" attention badge -- e.g. the active playlist is overdue for a refresh. Amber,
                // nudge-not-alarm; sits ON the icon's top-right corner (a small inward offset) so it
                // stays inside the tile instead of spilling past the tile's rounded corner and getting
                // clipped, and so it's still visible in the collapsed rail.
                if (badged) {
                    Box(
                        modifier = Modifier
                            .offset(x = (-2).dp, y = (-2).dp)
                            .size(14.dp)
                            .background(colors.warning, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = "!", style = AreIptvTheme.typography.caption.copy(fontSize = 10.sp), color = colors.accentFg)
                    }
                }
            }
            Text(
                text = label,
                // 14sp rather than the 16sp label role: nav labels are icon-paired and live in a
                // fixed 212dp rail, so the legibility floor is relaxed here to keep long
                // translations on one line.
                style = AreIptvTheme.typography.label.copy(fontSize = 14.sp),
                color = if (active) lensContentColor() else colors.textSecondary,
                maxLines = 1,
                // Always in the tree at the rail's open width, revealed by alpha alone. See the
                // `labelAlpha` note in AreSidebarNav: this replaces a per-row AnimatedVisibility whose
                // layout animation fought the container's width animation every frame.
                modifier = Modifier.graphicsLayer { alpha = labelAlpha.value },
            )
        }
    }
}

@Preview(widthDp = 320, heightDp = 900, showBackground = true)
@Composable
private fun AreSidebarNavPreview() {
    AreIptvTheme {
        AreSidebarNav(active = "home", onSelect = {})
    }
}
