package com.arashrahimi46.iptv.ui.shell

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Splitscreen
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.arashrahimi46.iptv.R
import com.arashrahimi46.iptv.ui.components.AreIconButton
import com.arashrahimi46.iptv.ui.components.AreIconButtonVariant
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme

/**
 * Persistent top app bar (app.jsx top bar row): multi-view, search,
 * add-playlist actions plus an avatar. Floats over the content area of every
 * non-overlay screen — the caller is responsible for stacking it above the
 * screen content (see [com.arashrahimi46.iptv.ui.shell.AreIptvAppShell]).
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AreTopBar(
    modifier: Modifier = Modifier,
    /**
     * The current page's title, rendered in the bar's leading slot. Screens used to draw this
     * themselves below the bar, which left this whole row empty on the leading side and pushed
     * every page title ~84dp down the screen for no reason. Null on Home (no page title).
     */
    title: String? = null,
    onMultiView: () -> Unit = {},
    onSearch: () -> Unit = {},
    onAddPlaylist: () -> Unit = {},
    // Home-only: null on every other screen so the Customize action doesn't appear there.
    // [customizeActive] reflects whether Home is currently in edit mode (accent-highlighted).
    onCustomize: (() -> Unit)? = null,
    customizeActive: Boolean = false,
) {
    val spacing = AreIptvTheme.spacing
    // Vertical inset is intentionally the literal 18dp from the source, NOT spacing.safeY (40dp):
    // are-iptv-design-system/.../ui_kits/are-tv/app.jsx top-bar div uses
    // `padding: "18px var(--safe-x)"` -- horizontal safe-x applies, vertical does not use --safe-y.
    // --safe-y (40px) is defined in tokens/spacing.css but is not referenced by any screen or the
    // app shell in the actual prototype source -- flagged to product-lead for confirmation this
    // reading is correct rather than silently deviating from the pixel-perfect source.
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(PaddingValues(horizontal = spacing.safeX, vertical = 18.dp)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (title != null) {
            Text(
                text = title,
                style = AreIptvTheme.typography.h1,
                color = AreIptvTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        } else {
            Box(Modifier.weight(1f))
        }
        // Customize (edit Home layout) -- only present on Home (onCustomize != null); accent-lit
        // while edit mode is active, matching the old inline "Customize/Done" toggle.
        if (onCustomize != null) {
            AreIconButton(Icons.Filled.Edit, stringResource(R.string.shell_customize), onClick = onCustomize, variant = AreIconButtonVariant.Glass, active = customizeActive)
        }
        // Multi-view: opens the multi-channel grid (route + screen already shipped). A free
        // differentiator vs. paywalled rivals, so it gets a permanent entry point here.
        AreIconButton(Icons.Filled.Splitscreen, stringResource(R.string.shell_multi_view_desc), onClick = onMultiView, variant = AreIconButtonVariant.Glass)
        AreIconButton(Icons.Filled.Search, stringResource(R.string.action_search), onClick = onSearch, variant = AreIconButtonVariant.Glass)
        // Add playlist -- opens the onboarding/add-source flow. (v1 has no multi-playlist
        // management UI, so this effectively adds/replaces the active source.)
        AreIconButton(Icons.Filled.Add, stringResource(R.string.shell_add_playlist_desc), onClick = onAddPlaylist, variant = AreIconButtonVariant.Glass)
        // Avatar removed: it only re-routed to Settings, already reachable via the sidebar gear --
        // a redundant, non-functional profile glyph (no account/profile screen exists).
    }
}

@Preview(widthDp = 1600, heightDp = 120, showBackground = true)
@Composable
private fun AreTopBarPreview() {
    AreIptvTheme {
        AreTopBar()
    }
}
