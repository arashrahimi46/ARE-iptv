package com.arashrahimi46.iptv.ui.player

import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.arashrahimi46.iptv.ui.components.AreIconButton
import com.arashrahimi46.iptv.ui.components.AreIconButtonVariant
import com.arashrahimi46.iptv.ui.components.ArePlayerControls
import com.arashrahimi46.iptv.ui.components.AreStreamHealth
import com.arashrahimi46.iptv.ui.components.AreStreamHealthLevel
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme

/**
 * Real Live TV playback (LivePlayer.jsx chrome, real Media3/ExoPlayer video --
 * the prototype's static background image is replaced end-to-end). Full-bleed,
 * outside [com.arashrahimi46.iptv.ui.shell.AreIptvAppShell] (own NavHost
 * destination), System Back dismisses via [BackHandler] rather than falling
 * through to app-exit or the underlying screen.
 */
@Composable
fun LivePlayerScreen(channelId: Long, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val viewModel: LivePlayerViewModel = viewModel(
        factory = LivePlayerViewModel.factory(context.applicationContext as android.app.Application, channelId),
    )
    val state by viewModel.uiState.collectAsState()

    BackHandler(onBack = onBack)

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        val channel = state.channel
        if (state.loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                BufferingIndicator()
            }
        } else if (channel == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                PlayerErrorState(message = state.errorMessage ?: "Channel not found", onBack = onBack)
            }
        } else {
            var playing by remember { mutableStateOf(true) }
            var isBuffering by remember { mutableStateOf(true) }
            var playerError by remember { mutableStateOf<String?>(null) }

            val exoPlayer = remember(channel.streamUrl) {
                ExoPlayer.Builder(context).build().apply {
                    // No hardcoded "HLS-only" assumption -- Media3's DefaultMediaSourceFactory
                    // (used implicitly by setMediaItem/prepare) inspects the URI to pick
                    // HlsMediaSource for .m3u8 (media3-exoplayer-hls is on the classpath)
                    // or falls back to ProgressiveMediaSource (bundled TS/MP4/etc extractors)
                    // for raw .ts / other URLs -- covers both shapes real M3U/Xtream sources hand back.
                    setMediaItem(MediaItem.fromUri(channel.streamUrl))
                    playWhenReady = true
                    prepare()
                }
            }

            DisposableEffect(exoPlayer) {
                val listener = object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        playing = isPlaying
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        isBuffering = playbackState == Player.STATE_BUFFERING
                        if (playbackState == Player.STATE_READY) playerError = null
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        playerError = error.errorCodeName.replace('_', ' ').lowercase()
                            .replaceFirstChar { it.uppercase() }
                            .ifBlank { "Playback error" }
                    }
                }
                exoPlayer.addListener(listener)
                onDispose {
                    exoPlayer.removeListener(listener)
                    exoPlayer.release()
                }
            }

            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner, exoPlayer) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_STOP -> exoPlayer.pause()
                        Lifecycle.Event.ON_START -> if (playerError == null) exoPlayer.play()
                        else -> Unit
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = {
                    PlayerView(context).apply {
                        useController = false
                        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                        player = exoPlayer
                    }
                },
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xB2060709), Color(0x00060709)),
                            endY = 400f,
                        ),
                    ),
            )

            // Top bar (back + stream health) / center error-or-buffering / bottom transport HUD.
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxWidth().padding(28.dp, 24.dp)) {
                    AreIconButton(
                        icon = Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        onClick = onBack,
                        variant = AreIconButtonVariant.Glass,
                    )
                    Box(Modifier.align(Alignment.CenterEnd)) {
                        AreStreamHealth(
                            level = when {
                                playerError != null -> AreStreamHealthLevel.Poor
                                isBuffering -> AreStreamHealthLevel.Moderate
                                else -> AreStreamHealthLevel.Stable
                            },
                        )
                    }
                }
                Box(Modifier.fillMaxSize().padding(horizontal = 28.dp), contentAlignment = Alignment.Center) {
                    if (playerError != null) {
                        PlayerErrorState(message = playerError!!, onBack = onBack)
                    } else if (isBuffering) {
                        BufferingIndicator()
                    }
                }
                Box(Modifier.fillMaxWidth().padding(28.dp, 24.dp)) {
                    ArePlayerControls(
                        title = channel.name,
                        subtitle = channel.categoryName,
                        live = true,
                        playing = playing,
                        channelLogoInitials = channel.name.take(3).uppercase(),
                        onPlayPause = {
                            if (playing) exoPlayer.pause() else exoPlayer.play()
                        },
                    )
                }
            }
        }
    }
}

/** Minimal buffering affordance -- a rotating icon + label, not a new shared component (single-use here). */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun BufferingIndicator() {
    val colors = AreIptvTheme.colors
    val transition = rememberInfiniteTransition(label = "buffering")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart),
        label = "bufferingAngle",
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Filled.Autorenew, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.rotate(angle))
        Box(Modifier.padding(top = 8.dp))
        Text(text = "Buffering…", style = AreIptvTheme.typography.caption, color = colors.textSecondary)
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun PlayerErrorState(message: String, onBack: () -> Unit) {
    val colors = AreIptvTheme.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            Icons.Filled.ErrorOutline,
            contentDescription = null,
            tint = colors.danger,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        Text(text = "Playback failed", style = AreIptvTheme.typography.h2, color = colors.textPrimary)
        Box(Modifier.padding(top = 6.dp))
        Text(text = message, style = AreIptvTheme.typography.body, color = colors.textSecondary)
        Box(Modifier.padding(top = 20.dp))
        AreIconButton(icon = Icons.Filled.ArrowBack, contentDescription = "Back", onClick = onBack, variant = AreIconButtonVariant.Solid)
    }
}
