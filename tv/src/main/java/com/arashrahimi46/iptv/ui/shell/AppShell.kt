package com.arashrahimi46.iptv.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arashrahimi46.iptv.ui.components.AreSidebarNav
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme

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
    content: @Composable () -> Unit,
) {
    val colors = AreIptvTheme.colors
    Row(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bgBase),
    ) {
        AreSidebarNav(active = activeNav, onSelect = onNavSelect, badgedIds = badgedNavIds)
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
