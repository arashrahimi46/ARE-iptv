package com.arashrahimi46.iptv.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
 */
@Composable
fun AreIptvAppShell(
    activeNav: String,
    onNavSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = { AreTopBar() },
    content: @Composable () -> Unit,
) {
    val colors = AreIptvTheme.colors
    Row(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bgBase),
    ) {
        AreSidebarNav(active = activeNav, onSelect = onNavSelect)
        Column(modifier = Modifier.weight(1f)) {
            topBar()
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                content()
            }
        }
    }
}
