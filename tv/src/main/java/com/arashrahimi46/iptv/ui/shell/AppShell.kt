package com.arashrahimi46.iptv.ui.shell

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.arashrahimi46.iptv.data.settings.SidebarStyle
import com.arashrahimi46.iptv.ui.components.AreSidebarNav
import com.arashrahimi46.iptv.ui.theme.AmbientBackdrop
import com.arashrahimi46.iptv.ui.theme.LocalAmbientArtwork
import com.arashrahimi46.iptv.ui.theme.LocalAppBackdrop
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
    CompositionLocalProvider(
        LocalAmbientArtwork provides artwork,
        LocalAppBackdrop provides backdrop,
    ) {
        Box(modifier = modifier.fillMaxSize()) {
            AmbientBackdrop(Modifier.layerBackdrop(backdrop))
            Row(modifier = Modifier.fillMaxSize()) {
                AreSidebarNav(active = activeNav, onSelect = onNavSelect, badgedIds = badgedNavIds, style = sidebarStyle)
                Column(modifier = Modifier.weight(1f)) {
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
        }
    }
}
