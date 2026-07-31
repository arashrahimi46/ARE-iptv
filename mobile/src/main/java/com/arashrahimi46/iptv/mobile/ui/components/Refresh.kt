package com.arashrahimi46.iptv.mobile.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme

/**
 * Pull-to-refresh for the catalog screens (Home, Live, Movies, Series, Guide, Favorites, Series
 * detail). Detail screens for movies, Settings, Recordings, Streams and the player deliberately
 * have none -- there is nothing there a pull could re-fetch.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AreRefreshBox(
    refreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val state = rememberPullToRefreshState()
    val colors = AreIptvTheme.colors
    if (!enabled) {
        Box(modifier, content = content)
        return
    }
    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = onRefresh,
        modifier = modifier,
        state = state,
        // The indicator is the one place a refresh is visible, so it carries the accent on a glass
        // plate rather than Material's default surface disc.
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = state,
                isRefreshing = refreshing,
                modifier = Modifier.align(Alignment.TopCenter),
                containerColor = colors.surfaceGlassElevated,
                color = colors.accent,
            )
        },
        content = content,
    )
}
