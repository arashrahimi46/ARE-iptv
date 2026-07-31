package com.arashrahimi46.iptv.mobile.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.arashrahimi46.iptv.core.R as CoreR
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.theme.ProvideOnGlass
import com.arashrahimi46.iptv.ui.theme.glassSurface

/**
 * Screen chrome. Every child screen (Guide, Search, Favorites, Recordings, Streams, both detail
 * screens) uses this and therefore gains a visible Back affordance -- on a phone there is no
 * hardware Back key you can rely on being discoverable.
 *
 * `content` is handed the [PaddingValues]; a scrolling body MUST pass them as its lazy list's
 * `contentPadding`, never as `Modifier.padding`, or the list clips instead of scrolling under the
 * bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AreScreenScaffold(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    snackbarHostState: SnackbarHostState? = null,
    bottomBar: (@Composable () -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        contentColor = AreIptvTheme.colors.textPrimary,
        topBar = {
            AreTopBar(
                title = title,
                onBack = onBack,
                actions = actions,
                scrollBehavior = scrollBehavior,
            )
        },
        bottomBar = { bottomBar?.invoke() },
        snackbarHost = { if (snackbarHostState != null) SnackbarHost(snackbarHostState) },
        content = content,
    )
}

/** The glass top bar. Exposed for screens that need a custom body layout around it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AreTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    val backLabel = stringResource(CoreR.string.action_back)
    ProvideOnGlass(true) {
        TopAppBar(
            modifier = modifier.glassSurface(RectangleShape, elevated = false, shadow = false),
            title = {
                Text(
                    text = title,
                    style = AreIptvTheme.typography.h3,
                    color = AreIptvTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            navigationIcon = {
                if (onBack != null) {
                    AreIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = backLabel,
                        onClick = onBack,
                        variant = AreIconButtonVariant.Ghost,
                    )
                }
            },
            actions = { actions?.invoke(this) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent,
                titleContentColor = AreIptvTheme.colors.textPrimary,
                navigationIconContentColor = AreIptvTheme.colors.textPrimary,
                actionIconContentColor = AreIptvTheme.colors.textPrimary,
            ),
            scrollBehavior = scrollBehavior,
        )
    }
}
