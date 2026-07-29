package com.arashrahimi46.iptv.mobile.ui.player

import android.app.Activity
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.ui.PlayerView
import com.arashrahimi46.iptv.mobile.ui.theme.AreIptvMobileTheme

/**
 * Portrait+landscape ExoPlayer with touch gesture controls (PlayerView's built-in tap-to-show
 * overlay + scrubber) and real Android Picture-in-Picture -- unlike :tv's in-app "PiP" corner
 * player (UI-labeled only, per docs/feature-matrix), this uses the actual
 * [android.app.Activity.enterPictureInPictureMode] OS surface, wired from [onRequestPip] which
 * the hosting Activity calls from `onUserLeaveHint()`.
 */
@Composable
fun PlayerScreen(target: PlayerTarget, viewModel: PlayerViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val colors = AreIptvMobileTheme.colors

    LaunchedEffect(target) { viewModel.load(target) }

    DisposableEffect(Unit) {
        val activity = context as? Activity
        val originalOrientation = activity?.requestedOrientation
        onDispose {
            if (activity != null && originalOrientation != null) {
                activity.requestedOrientation = originalOrientation
            }
        }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AndroidView(
            factory = {
                PlayerView(context).apply {
                    layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
                    useController = true
                    player = viewModel.player
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
        if (state.isLoading) {
            CircularProgressIndicator(color = colors.accent)
        }
        state.error?.let {
            Text(it, color = colors.danger, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
