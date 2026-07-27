package com.arashrahimi46.iptv.ui.shell

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier
import com.arashrahimi46.iptv.data.settings.SidebarStyle
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.components.AreSidebarNav
import com.arashrahimi46.iptv.ui.theme.AmbientBackdrop
import com.arashrahimi46.iptv.ui.theme.LocalAmbientArtwork
import com.arashrahimi46.iptv.ui.theme.LocalAppBackdrop
import com.arashrahimi46.iptv.ui.theme.LocalPageBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.delay

/**
 * App shell scaffold (app.jsx): persistent left [AreSidebarNav] rail at the
 * true screen edge + a persistent [AreTopBar] floating over the content area.
 * Safe-area padding is intentionally NOT applied here — the sidebar sits flush
 * against the panel edge, and downstream content (Rail/Hero) already bakes
 * the 64dp horizontal safe-area into its own edge padding, matching the
 * design source's `--safe-x` usage inside those components rather than a
 * double layer of shell + component padding (see report for this judgment call).
 *
 * QA follow-up (product-lead ruling pending): confirmed against source —
 * are-iptv-design-system/.../components/navigation/SidebarNav.jsx never
 * references --safe-x or --safe-y anywhere in its styles (header padding is a
 * literal "0 26px", item padding "0 20px" / "0 16px") — the prototype's own
 * sidebar is deliberately flush-to-edge chrome, not safe-area-inset content.
 * Applying the full 64dp safe-x to the collapsed 104dp rail's icon inset would
 * also leave only ~20dp for a 26dp icon plus padding — physically doesn't fit
 * the design's own token values together. Flagged back to product-lead with
 * this citation rather than force-applying safe-x here; will update if
 * overruled.
 */
@Composable
fun AreIptvAppShell(
    activeNav: String,
    onNavSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = { AreTopBar() },
    /** Nav ids that show a "!" attention badge (e.g. "settings" when a catalog refresh is overdue). */
    badgedNavIds: Set<String> = emptySet(),
    sidebarStyle: SidebarStyle = SidebarStyle.FLOATING,
    content: @Composable () -> Unit,
) {
    // Glass V2 §3: the shell owns the artwork slot that browse screens publish their focused item
    // into, and paints the ambient backdrop beneath everything. Without this, every glass surface in
    // the app is compositing over one flat opaque colour and can only ever come out a lighter grey.
    val artwork = remember { mutableStateOf<String?>(null) }
    // The blur source (§4). Captures the ambient layer ONLY -- never the page content, which would
    // feed each glass surface back into its own backdrop.
    val backdrop = rememberLayerBackdrop { drawContent() }
    // The expanded sidebar's frost source: ambient AND page content (see LocalPageBackdrop).
    val pageBackdrop = rememberLayerBackdrop { drawContent() }
    // Gated, because `layerBackdrop` draws its subtree TWICE per frame (once to the screen, once into
    // the layer) -- capturing the whole page unconditionally would double the draw cost of every rail
    // scroll and focus move, which is exactly what weak TV SoCs feel. The sidebar is only expanded
    // while it holds focus, i.e. while the content behind is idle and redrawing nothing, so the extra
    // capture is close to free precisely when it's switched on. Collapsed, the rail sits over the
    // reserved strip with no content behind it, so it has nothing to frost anyway.
    var sidebarExpanded by remember { mutableStateOf(false) }
    // NOT gated in time. An earlier pass deferred the capture until the expand tween settled, which
    // did halve frame time but made the blur visibly pop in after the motion finished -- a worse
    // experience than the jank it fixed. The per-frame cost came from the frosted node RESIZING (its
    // `drawBackdrop` re-pulls this layer whenever it draws, and the pull re-rasterizes the whole page,
    // ~14ms). SidebarNav now pins that node at the open width and animates a clip around it instead, so
    // the page is captured once per expand and the frost is there from frame 0. Fix the cost at the
    // consumer, not by taking the frost away.
    // ...and gated in TIME as well as in state, because the capture is by far the most expensive thing
    // the app does. Measured on the XL95 (see docs/glass-render-perf-findings.md): the page capture
    // costs ~14ms of RenderThread per frame, and during the expand tween the panel is remeasured and
    // redrawn every frame, so the whole page subtree is re-recorded every frame -- p50 33ms, ~28fps,
    // which is the "not very buttery" expand the report was about. The unbaked shadow (~0ms) and the
    // 28dp blur (~1ms) are innocent; only the re-capture matters.
    //
    // So: no capture while the width is moving (the panel falls back to its plain sheer fill -- 28dp
    // of blur is not readable on a surface that is travelling 160dp in 220ms anyway), then capture
    // once the tween has settled, which is the state you actually sit and look at. Do NOT try to
    // "capture once and freeze" by detaching the modifier: `layerBackdrop` discards its recording when
    // detached, so that silently turns the frost off entirely and lands exactly on the no-frost
    // ceiling number. It looks like a win in framestats and is a visual regression.
    var capturePage by remember { mutableStateOf(false) }
    val settleMs = AreIptvTheme.motion.durBaseMs.toLong() + 32L
    LaunchedEffect(sidebarExpanded) {
        if (sidebarExpanded) {
            delay(settleMs)
            capturePage = true
        } else {
            capturePage = false
        }
    }
    // The collapsed sidebar footprint the content reserves at the left. The sidebar OVERLAYS content
    // when it expands rather than pushing it: animating the rail's real width in a Row remeasured and
    // relayouted the entire content screen (a full movie grid / guide) on every animation frame, which
    // is the single biggest source of expand/collapse jank on weaker TV SoCs. Reserving only the
    // collapsed width and floating the expanding panel on top costs the content zero per-frame layout.
    // FLOATING already hovers inset off the edge, so overlaying is visually identical; EDGE's expanded
    // rail simply spills over the content's left strip while the sidebar holds focus.
    val spacing = AreIptvTheme.spacing
    val reservedWidth = when (sidebarStyle) {
        SidebarStyle.FLOATING -> spacing.sidebarBoxWidth + spacing.sidebarInset * 2
        SidebarStyle.EDGE -> spacing.sidebarWidth
    }
    CompositionLocalProvider(
        LocalAmbientArtwork provides artwork,
        LocalAppBackdrop provides backdrop,
        LocalPageBackdrop provides pageBackdrop.takeIf { capturePage },
    ) {
        Box(modifier = modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .then(if (capturePage) Modifier.layerBackdrop(pageBackdrop) else Modifier),
            ) {
                AmbientBackdrop(Modifier.layerBackdrop(backdrop))
                Column(modifier = Modifier.fillMaxSize().padding(start = reservedWidth)) {
                    topBar()
                    // Bounded content area (no scroll here). The shell used to own a single
                    // verticalScroll, but that can't host the tab NavHost -- each tab now
                    // provides its own scroll (see MainActivity.ScrollableTab).
                    //
                    // focusGroup only -- deliberately NO focusRestorer here. A shell-level restorer intercepts
                    // right-arrow entry with a directional/nearest search and resolves it to whatever focusable
                    // sits at the sidebar icon's height (the reported "Settings lands on Dark theme, mid-page").
                    // It also overrode each screen's own focusProperties{enter}. Every content screen manages its
                    // own entry focus (Settings pins enter->first row; browse screens request their index-0 tile),
                    // so the group boundary is all the shell needs to provide.
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .focusGroup(),
                    ) {
                        content()
                    }
                }
            }
            // Drawn last so the expanding rail floats over the content's left strip -- and OUTSIDE the
            // captured page layer above, so the panel never samples itself. It sizes itself
            // (fillMaxHeight + wrap width) and sits at the Box's top-start by default; its own style
            // branch handles the inset (FLOATING) or flush edge (EDGE).
            AreSidebarNav(
                active = activeNav,
                onSelect = onNavSelect,
                badgedIds = badgedNavIds,
                style = sidebarStyle,
                onExpandedChange = { sidebarExpanded = it },
                modifier = Modifier.align(Alignment.TopStart),
            )
        }
    }
}
