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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester

import androidx.compose.ui.Modifier
import com.arashrahimi46.iptv.data.settings.SidebarStyle
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.components.AreSidebarNav
import com.arashrahimi46.iptv.ui.theme.AmbientBackdrop
import com.arashrahimi46.iptv.ui.theme.LocalAmbientArtwork
import com.arashrahimi46.iptv.ui.theme.LocalAppBackdrop
import com.arashrahimi46.iptv.ui.theme.requestFocusWhenReady
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

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
    // Picking a nav row should hand the screen over: the rail collapses and D-pad focus lands in the
    // content. Focus is what drives the collapse (the rail is expanded exactly while it holds focus),
    // so moving focus out is the single action that does both. Previously nothing moved focus, so the
    // rail stayed open with the row still focused and the newly-opened screen sat there inert until
    // the user pressed RIGHT.
    //
    // Owned by the shell, not by each screen: entry focus is shell-level behaviour and only Home had
    // hand-rolled it. Every screen already declares WHERE focus should land via its own
    // focusProperties{enter} (Settings pins the tab strip, browse screens their first tile), and
    // requesting focus on this focusGroup honours that -- which is also why this is NOT a
    // focusRestorer (see the note at the content Box: a restorer hijacked plain RIGHT-arrow entry
    // with a nearest-neighbour search). This fires only on an explicit nav selection.
    val contentFocus = remember { FocusRequester() }
    // Counter, not a Boolean: re-selecting the tab you are already on must still hand focus over, and
    // a Boolean would not change state on the second press.
    var contentFocusRequests by remember { mutableIntStateOf(0) }
    LaunchedEffect(contentFocusRequests) {
        // requestFocusWhenReady retries across the tab swap AND re-asserts after a settle delay --
        // needed because the sidebar row the user just clicked wins focus back for a few frames as
        // the transition completes.
        if (contentFocusRequests > 0) contentFocus.requestFocusWhenReady()
    }

    val spacing = AreIptvTheme.spacing
    val reservedWidth = when (sidebarStyle) {
        SidebarStyle.FLOATING -> spacing.sidebarBoxWidth + spacing.sidebarInset * 2
        SidebarStyle.EDGE -> spacing.sidebarWidth
    }
    CompositionLocalProvider(
        LocalAmbientArtwork provides artwork,
        LocalAppBackdrop provides backdrop,
    ) {
        Box(modifier = modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize()) {
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
                            .focusRequester(contentFocus)
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
                onSelect = { id ->
                    onNavSelect(id)
                    contentFocusRequests++
                },
                badgedIds = badgedNavIds,
                style = sidebarStyle,
                modifier = Modifier.align(Alignment.TopStart),
            )
        }
    }
}
