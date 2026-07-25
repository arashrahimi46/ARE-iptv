package com.arashrahimi46.iptv.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.arashrahimi46.iptv.R
import com.arashrahimi46.iptv.ui.theme.AreIptvColors
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.theme.TvFocusable
import com.arashrahimi46.iptv.ui.theme.accentLensBrush
import com.arashrahimi46.iptv.ui.theme.glassBorderBrush
import com.arashrahimi46.iptv.ui.theme.lensBorderBrush
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
 * SidebarNav — the persistent left rail nav (SidebarNav.jsx). Collapsed to
 * icons (104dp); expands to labels (280dp) while any item inside has focus.
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
) {
    val colors = AreIptvTheme.colors
    val spacing = AreIptvTheme.spacing
    val motion = AreIptvTheme.motion

    // Tracks which item currently holds D-pad focus (null = none), driving expand/collapse.
    var focusedItemId by remember { mutableStateOf<String?>(null) }
    // Per-row focus requesters, kept so a row can be focused programmatically if needed.
    // NOTE: we deliberately do NOT auto-request focus onto the active row here. The sidebar is
    // persistent across tab switches (the shell swaps only the inner content), so this composable
    // only re-enters composition when returning from a full-bleed overlay (player/detail) -- and
    // that is exactly when the browse screen restores D-pad focus to the tile the user launched
    // from (see rememberPlaybackFocusRequester). An auto-focus here stole that focus back to the
    // sidebar, which was the reported "Back always lands on the sidebar" bug.
    val focusRequesters = remember(items) { items.associate { it.id to FocusRequester() } }
    val expanded = focusedItemId != null
    val width by animateDpAsState(
        targetValue = if (expanded) spacing.sidebarWidthOpen else spacing.sidebarWidth,
        animationSpec = tween(motion.durBaseMs, easing = motion.easeOut),
        label = "sidebarWidth",
    )

    Column(
        modifier = modifier
            .width(width)
            .fillMaxHeight()
            // The rail is a glass panel, not a flat block: a faint top-lit vertical sheen plus a
            // lit hairline right edge (the glass seam separating rail from content). Drawn behind
            // the nav rows. Over the solid page -- no blur (design §6).
            .drawBehind {
                drawRect(Brush.verticalGradient(listOf(colors.surface2, colors.surface1)))
                val edge = 1.dp.toPx()
                drawRect(
                    brush = Brush.verticalGradient(listOf(colors.glassHighlight, colors.borderGlass)),
                    topLeft = Offset(size.width - edge, 0f),
                    size = Size(edge, size.height),
                )
            }
            .padding(vertical = spacing.sp8),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .padding(horizontal = 26.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BrandMark(brand = brand, colors = colors)
            AnimatedVisibility(visible = expanded, enter = fadeIn(), exit = fadeOut()) {
                Text(text = "$brand iptv", style = AreIptvTheme.typography.h3, color = colors.textPrimary)
            }
        }

        Box(Modifier.height(spacing.sp10))

        // HIGH QA defect: fixed-height Column with no scroll clipped/unreachable items
        // (Settings included) below the fold on shorter/denser TV viewports (e.g. the
        // 540dp-effective-height Television_1080p AVD profile with all 8 items + header).
        // .weight(1f) lets this take only the space left after the brand header, and
        // verticalScroll (with focusable() row items) lets D-pad focus auto-scroll a
        // below-the-fold item into view instead of just stopping at the last visible one.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                // vertical padding inside the scroll content so the first/last item's
                // focus glow isn't clipped at the scroll viewport edge (the reported
                // "top of the Home focus ring disappeared").
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items.forEach { item ->
                val itemInteractionSource = remember { MutableInteractionSource() }
                SidebarNavRow(
                    item = item,
                    active = item.id == active,
                    expanded = expanded,
                    badged = item.id in badgedIds,
                    interactionSource = itemInteractionSource,
                    focusRequester = focusRequesters.getValue(item.id),
                    onClick = { onSelect(item.id) },
                    onFocusedChanged = { focused ->
                        focusedItemId = if (focused) item.id else focusedItemId.takeUnless { it == item.id }
                    },
                )
            }
        }
    }
}

@Composable
private fun BrandMark(brand: String, colors: AreIptvColors) {
    val shape = RoundedCornerShape(AreIptvTheme.radius.sm)
    Box(
        modifier = Modifier
            .size(40.dp)
            // Accent-lens frosted tile (translucent accent + a brighter lens rim), echoing the same
            // selection material as the rest of the shell. No glow: it read as a heavy pink halo;
            // the lens fill + lit rim is enough.
            .background(accentLensBrush(), shape)
            .border(1.dp, lensBorderBrush(), shape),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = brand.take(1), style = AreIptvTheme.typography.h3, color = lensContentColor())
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SidebarNavRow(
    item: SidebarNavItem,
    active: Boolean,
    expanded: Boolean,
    badged: Boolean,
    interactionSource: MutableInteractionSource,
    focusRequester: FocusRequester,
    onClick: () -> Unit,
    onFocusedChanged: (Boolean) -> Unit,
) {
    val colors = AreIptvTheme.colors
    val label = stringResource(item.labelRes)
    // Three visually-distinct states (design §6b): rest = transparent, focused = glass fill +
    // lit-edge gradient (on top of the TvFocusable ring), current screen = accent lens.
    val focused by interactionSource.collectIsFocusedAsState()

    TvFocusable(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            // Smaller, tighter rows per the design artifact (rail-item: 22px icon, ~44dp tall) --
            // the rail reads lighter and shinier occupying less vertical space than before.
            .height(44.dp)
            .focusRequester(focusRequester),
        interactionSource = interactionSource,
        shape = RoundedCornerShape(AreIptvTheme.radius.lg),
        // Three distinct states (design §6b): rest = transparent, focused = glass fill + lit edge,
        // current screen = accent lens (more glass + a brighter rim, not opaque paint). No shadow --
        // the lens sits on the flat rail, so a drop shadow reads as heavy; the lens rim marks it.
        backgroundColor = if (focused && !active) colors.surfaceGlass else Color.Transparent,
        backgroundBrush = if (active) accentLensBrush() else null,
        borderBrush = when {
            active -> lensBorderBrush()
            focused -> glassBorderBrush()
            else -> null
        },
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
                // 23dp start inset centres the 26dp icon in the collapsed rail (104dp wide minus
                // the 16dp column padding = 72dp; (72-26)/2 = 23). Kept static (not switched by
                // `expanded`) so the icon stays put through the width animation -- a conditional
                // arrangement made it jump between centred and left-aligned mid-animation.
                // Trailing inset is much smaller than the leading one: the 23dp start inset exists
                // only to centre the icon in the collapsed rail, and mirroring it on the end just
                // stole width from the label -- enough that long RTL labels ("راهنمای تلویزیون")
                // wrapped to two lines. Start-aligned content, so the icon does not move.
                .padding(start = 23.dp, end = 10.dp),
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
