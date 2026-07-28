package com.arashrahimi46.iptv.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** 8dp-grid spacing scale, mirroring tokens/spacing.css. */
data class AreIptvSpacing(
    val sp0: Dp = 0.dp,
    val sp1: Dp = 4.dp,
    val sp2: Dp = 8.dp,
    val sp3: Dp = 12.dp,
    val sp4: Dp = 16.dp,
    val sp5: Dp = 20.dp,
    val sp6: Dp = 24.dp,
    val sp8: Dp = 32.dp,
    val sp10: Dp = 40.dp,
    val sp12: Dp = 48.dp,
    val sp16: Dp = 64.dp,
    val sp20: Dp = 80.dp,
    val sp24: Dp = 96.dp,

    // TV overscan safe area
    val safeX: Dp = 64.dp,
    val safeY: Dp = 40.dp,

    // Rails
    val railGap: Dp = 20.dp,
    val railPeek: Dp = 80.dp,

    // Common sizing
    val sidebarWidth: Dp = 104.dp,
    val sidebarWidthOpen: Dp = 188.dp,
    // Floating-glass sidebar (SidebarStyle.FLOATING): a box inset off the screen edge, so it runs a
    // touch narrower than the flush rail and reserves box-width + 2×inset in the shell Row.
    val sidebarInset: Dp = 20.dp,
    val sidebarBoxWidth: Dp = 96.dp,
    val sidebarBoxWidthOpen: Dp = 184.dp,
    val tilePosterWidth: Dp = 208.dp,
    val tilePosterHeight: Dp = 312.dp,
    val tileLandWidth: Dp = 320.dp,
    val tileLandHeight: Dp = 180.dp,
    val guideRowHeight: Dp = 64.dp,
    val guideChannelWidth: Dp = 220.dp,
)

val AreIptvSpacingDefault = AreIptvSpacing()
