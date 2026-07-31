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
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import com.arashrahimi46.iptv.mobile.R
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
    val colors = AreIptvTheme.colors
    ProvideOnGlass(true) {
        TopAppBar(
            // Not `glassSurface`: that draws a 1dp border around the WHOLE shape, and on a
            // full-bleed rectangle the result is a boxed outline running down both sides and
            // across the top, so the bar read as a floating card rather than page chrome. Page
            // chrome only wants the fill plus the hairline where it meets the content.
            modifier = modifier
                .background(colors.surfaceGlass)
                .drawBehind {
                    val stroke = 1.dp.toPx()
                    drawLine(
                        color = colors.borderDefault,
                        start = Offset(0f, size.height - stroke / 2f),
                        end = Offset(size.width, size.height - stroke / 2f),
                        strokeWidth = stroke,
                    )
                },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // The brand mark, only where there is no Back arrow -- i.e. on the five tab
                    // roots. A child screen's chrome is "where am I", not "whose app is this", and
                    // pairing the logo with a back arrow reads as a button.
                    if (onBack == null) {
                        Image(
                            painter = painterResource(R.drawable.ic_logo_mark),
                            contentDescription = null,
                            modifier = Modifier
                                .padding(end = 10.dp)
                                .size(26.dp),
                        )
                    }
                    Text(
                        text = title,
                        style = AreIptvTheme.typography.h3,
                        color = AreIptvTheme.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
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
