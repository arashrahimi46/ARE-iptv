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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.runtime.withFrameNanos

import androidx.compose.ui.Modifier
import com.arashrahimi46.iptv.data.settings.SidebarStyle
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import kotlinx.coroutines.delay
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
    // Whether focus is genuinely INSIDE the content group. requestFocusWhenReady can't tell us this:
    // it treats "the requester is attached" as success, and this Box -- a bare focusGroup that always
    // exists -- is attached from frame 0. So the request "succeeded" on attempt 0 while the group had
    // no focusable child yet (the NavHost cross-fade still showing the outgoing screen, paging not
    // past its first page), focus went nowhere, and Compose's fallback handed it to the first
    // focusable in the window -- the top bar's "+"/search. That is the reported "switching tab drops
    // me on the search icon", and it reads far worse in RTL, where the top bar sits at the far LEFT,
    // so the D-pad press that should enter the content appears to fly across the whole screen.
    var contentHasFocus by remember { mutableStateOf(false) }
    LaunchedEffect(contentFocusRequests) {
        if (contentFocusRequests == 0) return@LaunchedEffect
        // Keep asking until focus ACTUALLY lands in the content, not until the requester exists.
        // Bounded so a screen with genuinely nothing focusable (empty catalogue) stops instead of
        // spinning, and short enough that it can't fight a user who has already moved on.
        repeat(CONTENT_FOCUS_ATTEMPTS) {
            withFrameNanos { }
            runCatching { contentFocus.requestFocus() }
            if (contentHasFocus) return@LaunchedEffect
            delay(CONTENT_FOCUS_GAP_MS)
        }
    }

    // Nav is CONCURRENT: picking an item swaps the screen on the very same frame as the key press,
    // and the rail's collapse tween runs alongside the destination fading in.
    //
    // It used to be sequenced -- raise `collapseNow`, wait out the ~220ms tween, and only then call
    // onNavSelect -- so that the collapse animation never had to share a frame with the cost of
    // mounting a screen. That bought a smoother 220ms of sidebar at the price of the whole app being
    // unresponsive for it: with the NavHost's own 150ms cross-fade on top, nothing was focusable for
    // ~370ms after the press, which reads as a dead remote. Responsiveness wins over a clean tween:
    // the destination now starts composing immediately and the rail closes over the top of it.
    //
    // `collapseNow` is still raised (and held for the length of the tween) so the rail *animates*
    // closed rather than snapping shut the instant focus leaves it -- it overrides the rail's
    // focus-driven expansion, which matters because the row the user just pressed still holds focus
    // for the frame or two it takes the content focus request below to land. By the time the flag
    // drops, focus is in the content, so `expandedByFocus` is already false and the rail stays closed.
    var navigatingTo by remember { mutableStateOf<String?>(null) }
    val navCollapseMs = AreIptvTheme.motion.durBaseMs.toLong()
    LaunchedEffect(navigatingTo) {
        if (navigatingTo == null) return@LaunchedEffect
        delay(navCollapseMs)
        navigatingTo = null
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
                            .onFocusChanged { contentHasFocus = it.hasFocus }
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
                // Navigate on the SAME frame as the press -- see the note on `navigatingTo`. The
                // collapse flag and the focus handoff are raised in the same batch, so the
                // destination mounts, the rail starts closing and focus starts moving together.
                onSelect = { id ->
                    navigatingTo = id
                    onNavSelect(id)
                    contentFocusRequests++
                },
                collapseNow = navigatingTo != null,
                badgedIds = badgedNavIds,
                style = sidebarStyle,
                modifier = Modifier.align(Alignment.TopStart),
            )
        }
    }
}

/** ~640ms of retrying for the content to actually take focus after a nav selection. */
private const val CONTENT_FOCUS_ATTEMPTS = 16
private const val CONTENT_FOCUS_GAP_MS = 40L
