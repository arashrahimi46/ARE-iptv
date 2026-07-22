package com.arashrahimi46.iptv.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import com.arashrahimi46.iptv.ui.components.AreIconButton
import com.arashrahimi46.iptv.ui.components.AreIconButtonVariant
import com.arashrahimi46.iptv.ui.theme.TvFocusable
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
    onMultiView: () -> Unit = {},
    onSearch: () -> Unit = {},
    onAddPlaylist: () -> Unit = {},
    onAvatar: () -> Unit = {},
) {
    val colors = AreIptvTheme.colors
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
        Box(Modifier.weight(1f))
        // Multi-view action temporarily removed (feature not ready). onMultiView is kept on the
        // signature so the button can be restored here without re-plumbing the shell.
        AreIconButton(Icons.Filled.Search, "Search", onClick = onSearch, variant = AreIconButtonVariant.Solid)
        // Add playlist -- opens the onboarding/add-source flow. (v1 has no multi-playlist
        // management UI, so this effectively adds/replaces the active source.)
        AreIconButton(Icons.Filled.Add, "Add playlist", onClick = onAddPlaylist, variant = AreIconButtonVariant.Solid)
        // QA MEDIUM defect: this was a plain Box -- not focusable at all, D-pad couldn't
        // land on it and onAvatar was declared but never attached to anything.
        TvFocusable(
            onClick = onAvatar,
            modifier = Modifier.size(44.dp),
            shape = CircleShape,
            backgroundColor = colors.surface2,
        ) { _, _ ->
            Box(
                modifier = Modifier.fillMaxSize().border(2.dp, colors.borderStrong, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.AccountCircle, contentDescription = "Account", tint = colors.textSecondary, modifier = Modifier.size(28.dp))
            }
        }
    }
}

@Preview(widthDp = 1600, heightDp = 120, showBackground = true)
@Composable
private fun AreTopBarPreview() {
    AreIptvTheme {
        AreTopBar()
    }
}
