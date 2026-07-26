package com.arashrahimi46.iptv.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.arashrahimi46.iptv.ui.theme.TvFocusable
import com.arashrahimi46.iptv.ui.theme.accentGradientBrush
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
    val expanded = focusedItemId != null
    val width by animateDpAsState(
        targetValue = when {
            floating && expanded -> spacing.sidebarBoxWidthOpen
            floating -> spacing.sidebarBoxWidth
            expanded -> spacing.sidebarWidthOpen
            else -> spacing.sidebarWidth
        },
        animationSpec = tween(motion.durBaseMs, easing = motion.easeOut),
        label = "sidebarWidth",
    )
    val onFocusedChanged: (String, Boolean) -> Unit = { id, focused ->
        focusedItemId = if (focused) id else focusedItemId.takeUnless { it == id }
    }

    if (floating) {
        // The 20dp gap IS the idea: that band of ambient backdrop running behind every edge is what
        // makes the rail read as a glass object floating on the page rather than chrome bolted to the
        // bezel. `glassSurface(elevated)` samples and blurs that backdrop through the 28dp shape
        // exactly as dialogs do (Tier A/B); Tier C falls through to the denser opaque fill and still
        // reads via the inset + shape + shadow.
        Box(modifier = modifier.fillMaxHeight().padding(spacing.sidebarInset)) {
            Column(
                modifier = Modifier
                    .width(width)
                    .fillMaxHeight()
                    .glassSurface(RoundedCornerShape(AreIptvTheme.radius.xl), elevated = true)
                    .padding(vertical = spacing.sp8),
            ) {
                SidebarNavBody(
                    expanded = expanded,
                    floating = true,
                    items = items,
                    active = active,
                    brand = brand,
                    badgedIds = badgedIds,
                    focusRequesters = focusRequesters,
                    onSelect = onSelect,
                    onFocusedChanged = onFocusedChanged,
                )
            }
        }
    } else {
        Column(
            modifier = modifier
                .width(width)
                .fillMaxHeight()
                // The rail is a glass panel, not a flat block: a faint top-lit vertical sheen plus a
                // lit hairline right edge (the glass seam separating rail from content). Drawn behind
                // the nav rows. V2: the fill is the TRANSLUCENT glass token, not the opaque surface
                // ramp -- an opaque rail killed the ambient backdrop down the whole left edge, which
                // is the most persistent chrome in the app and so the most visible place to get wrong.
                .drawBehind {
                    drawRect(
                        Brush.verticalGradient(
                            listOf(colors.surfaceGlassElevated, colors.surfaceGlass),
                        ),
                    )
                    val edge = 1.dp.toPx()
                    drawRect(
                        brush = Brush.verticalGradient(listOf(colors.glassHighlight, colors.borderGlass)),
                        topLeft = Offset(size.width - edge, 0f),
                        size = Size(edge, size.height),
                    )
                }
                .padding(vertical = spacing.sp8),
        ) {
            SidebarNavBody(
                expanded = expanded,
                floating = false,
                items = items,
                active = active,
                brand = brand,
                badgedIds = badgedIds,
                focusRequesters = focusRequesters,
                onSelect = onSelect,
                onFocusedChanged = onFocusedChanged,
            )
        }
    }
}

/** Brand header + the scrolling item column, shared by both container styles. */
@Composable
private fun ColumnScope.SidebarNavBody(
    expanded: Boolean,
    floating: Boolean,
    items: List<SidebarNavItem>,
    active: String,
    brand: String,
    badgedIds: Set<String>,
    focusRequesters: Map<String, FocusRequester>,
    onSelect: (String) -> Unit,
    onFocusedChanged: (String, Boolean) -> Unit,
) {
    val colors = AreIptvTheme.colors
    val spacing = AreIptvTheme.spacing

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            // Floating: 28dp start centres the 40dp brand mark in the 96dp collapsed capsule
            // ((96-40)/2), matching the icons below; kept static so it doesn't shift as the label
            // fades in on expand. Edge: the original flush-rail inset.
            .padding(start = if (floating) 28.dp else 26.dp, end = if (floating) 20.dp else 26.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BrandMark(brand = brand, colors = colors)
        AnimatedVisibility(visible = expanded, enter = fadeIn(), exit = fadeOut()) {
            Text(text = "$brand iptv", style = AreIptvTheme.typography.h3, color = colors.textPrimary)
        }
    }

    Box(Modifier.height(spacing.sp10))

    // Measured top-Y (px) of each row within the list Box, so the selection lens knows where to slide.
    val rowTops = remember { mutableStateMapOf<String, Float>() }
    val activeTop = rowTops[active]
    // Buttery bit: a single glass-lens pill that SPRINGS between rows when the active tab changes,
    // instead of the lens hard-cutting to the new row. Same feel as AreSegmentedControl's indicator.
    val slide = spring<Float>(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
    val lensY by animateFloatAsState(activeTop ?: 0f, animationSpec = slide, label = "sidebarLensY")

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
            .then(if (floating) Modifier.scrollEdgeFade(spacing.sp6) else Modifier)
            .verticalScroll(rememberScrollState())
            // vertical padding inside the scroll content so the first/last item's focus glow isn't
            // clipped at the viewport edge (the reported "top of the Home focus ring disappeared").
            .padding(horizontal = if (floating) 10.dp else 16.dp, vertical = 14.dp),
    ) {
        Box(Modifier.fillMaxWidth()) {
            // The sliding selection lens, BEHIND the rows. Rows carry a transparent fill so it shows
            // through; the active row's own icon/label still take lensContentColor. It scrolls with
            // the list because it lives inside the same scrolled Box.
            if (activeTop != null) {
                Box(
                    modifier = Modifier
                        .offset { IntOffset(0, lensY.roundToInt()) }
                        .fillMaxWidth()
                        .height(44.dp)
                        .glassLens(RoundedCornerShape(AreIptvTheme.radius.lg)),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items.forEach { item ->
                    val itemInteractionSource = remember { MutableInteractionSource() }
                    SidebarNavRow(
                        item = item,
                        active = item.id == active,
                        expanded = expanded,
                        badged = item.id in badgedIds,
                        // Start inset centres the 22dp icon in each container's own collapsed width.
                        startInset = if (floating) 26.dp else 23.dp,
                        interactionSource = itemInteractionSource,
                        focusRequester = focusRequesters.getValue(item.id),
                        onClick = { onSelect(item.id) },
                        onFocusedChanged = { focused -> onFocusedChanged(item.id, focused) },
                        onPositioned = { top -> rowTops[item.id] = top },
                    )
                }
            }
        }
    }
}

/**
 * Fades the scrolled content to transparent over [fade] at the top and bottom edges, so nav rows
 * dissolve into the glass box at its rounded corners instead of being sliced by the surface clip.
 * An offscreen layer + a DstIn vertical-gradient mask: it subtracts alpha from the content, revealing
 * the blurred glass behind (a solid fade rectangle can't -- the surface is translucent).
 */
private fun Modifier.scrollEdgeFade(fade: Dp): Modifier = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        val f = (fade.toPx()).coerceAtMost(size.height / 2f)
        if (f <= 0f) return@drawWithContent
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Transparent,
                f / size.height to Color.Black,
                1f - f / size.height to Color.Black,
                1f to Color.Transparent,
            ),
            blendMode = BlendMode.DstIn,
        )
    }

@Composable
private fun BrandMark(brand: String, colors: AreIptvColors) {
    val shape = RoundedCornerShape(AreIptvTheme.radius.sm)
    Box(
        modifier = Modifier
            .size(40.dp)
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
    expanded: Boolean,
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
            AnimatedVisibility(visible = expanded, enter = fadeIn(), exit = fadeOut()) {
                Text(
                    text = label,
                    // 14sp rather than the 16sp label role: nav labels are icon-paired and live in a
                    // fixed 212dp rail, so the legibility floor is relaxed here to keep long
                    // translations on one line.
                    style = AreIptvTheme.typography.label.copy(fontSize = 14.sp),
                    color = if (active) lensContentColor() else colors.textSecondary,
                )
            }
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
