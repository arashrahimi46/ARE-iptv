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
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.arashrahimi46.iptv.data.settings.UserSettings
import com.arashrahimi46.iptv.ui.components.AreIconButton
import com.arashrahimi46.iptv.ui.components.AreIconButtonVariant
import com.arashrahimi46.iptv.ui.components.ArePlayerControls
import com.arashrahimi46.iptv.ui.components.AreStreamHealth
import com.arashrahimi46.iptv.ui.components.AreStreamHealthLevel
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import kotlinx.coroutines.delay

/**
 * Real playback screen (LivePlayer.jsx chrome, real Media3/ExoPlayer video --
 * the prototype's static background image is replaced end-to-end). Drives
 * from a [PlaybackSource] rather than a raw streamUrl so both live channels
 * and VOD (movies/episodes, from Detail's Play action) share this exact
 * screen/ExoPlayer lifecycle instead of a duplicated VOD player -- see report
 * for the reuse-vs-duplicate call. Full-bleed, outside
 * [com.arashrahimi46.iptv.ui.shell.AreIptvAppShell] (own NavHost destination),
 * System Back dismisses via [BackHandler] rather than falling through to
 * app-exit or the underlying screen.
 */
@Composable
fun LivePlayerScreen(
    source: PlaybackSource,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onMultiView: () -> Unit = {},
    onOpenGuide: () -> Unit = {},
) {
    val context = LocalContext.current
    val viewModel: LivePlayerViewModel = viewModel(
        factory = LivePlayerViewModel.factory(context.applicationContext as android.app.Application, source),
    )
    val state by viewModel.uiState.collectAsState()
    val settings = remember { UserSettings(context) }
    val hardwareDecoding by settings.isHardwareDecoding.collectAsState(initial = true)

    BackHandler(onBack = onBack)

    // QA MAJOR finding: "no in-player channel switcher". Channel-up/down on the D-pad is the
    // real remote convention for this, and doesn't collide with the transport HUD's Left/Right
    // button navigation (the HUD row has nothing above/below it to focus into) -- so this Box
    // grabs initial focus and claims Up/Down itself; every other key still falls through to
    // Compose's default focus-move into the HUD buttons. No-ops via switchChannel for
    // VOD/episode playback or a single-channel catalog.
    val channelSwitchFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { channelSwitchFocusRequester.requestFocus() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(channelSwitchFocusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyUp) {
                    when (keyEvent.key) {
                        Key.DirectionUp -> {
                            viewModel.switchChannel(1)
                            true
                        }
                        Key.DirectionDown -> {
                            viewModel.switchChannel(-1)
                            true
                        }
                        else -> false
                    }
                } else {
                    false
                }
            },
    ) {
        val media = state.media
        if (state.loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                BufferingIndicator()
            }
        } else if (media == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                PlayerErrorState(message = state.errorMessage ?: "Content not found", onBack = onBack)
            }
        } else {
            var playing by remember { mutableStateOf(true) }
            var isBuffering by remember { mutableStateOf(true) }
            var playerError by remember { mutableStateOf<String?>(null) }
            // Bumped by the error state's Retry action. Changing this changes exoPlayer's
            // remember() key below, which tears down the old instance through the existing
            // DisposableEffect(exoPlayer) cleanup and builds a fresh one -- same single
            // release() call site as backgrounding, just triggered by a different key change.
            var retryCount by remember { mutableStateOf(0) }

            val exoPlayer = remember(media.streamUrl, retryCount, hardwareDecoding) {
                // Real wiring of the Settings "Hardware decoding" preference -- a player
                // configuration flag only, not new playback-surface work. ON (default) keeps
                // Media3's default platform-decoder-only behavior (EXTENSION_RENDERER_MODE_OFF);
                // OFF additionally allows software/extension decoders as a compatibility
                // fallback (EXTENSION_RENDERER_MODE_ON) for streams the platform decoder can't
                // handle, at the cost of more CPU/battery use.
                val renderersFactory = DefaultRenderersFactory(context).apply {
                    setExtensionRendererMode(
                        if (hardwareDecoding) DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF
                        else DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON,
                    )
                    setEnableDecoderFallback(true)
                }
                ExoPlayer.Builder(context, renderersFactory).build().apply {
                    // No hardcoded "HLS-only" assumption -- Media3's DefaultMediaSourceFactory
                    // (used implicitly by setMediaItem/prepare) inspects the URI to pick
                    // HlsMediaSource for .m3u8 (media3-exoplayer-hls is on the classpath)
                    // or falls back to ProgressiveMediaSource (bundled TS/MP4/etc extractors)
                    // for raw .ts / other URLs -- covers both shapes real M3U/Xtream sources hand back.
                    setMediaItem(MediaItem.fromUri(media.streamUrl))
                    playWhenReady = true
                    prepare()
                }.also { playerError = null }
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

            // Real seek-bar/elapsed/buffered data (QA blocker: these were hardcoded
            // ArePlayerControls defaults before, never actually bound to the player).
            // ExoPlayer has no position-changed callback, so this polls at a UI-refresh
            // cadence -- cheap reads (currentPosition/duration/bufferedPosition are all
            // non-blocking getters), stopped via onDispose when the composable leaves.
            var positionMs by remember { mutableStateOf(0L) }
            var durationMs by remember { mutableStateOf(C.TIME_UNSET) }
            var bufferedPositionMs by remember { mutableStateOf(0L) }
            LaunchedEffect(exoPlayer) {
                while (true) {
                    positionMs = exoPlayer.currentPosition
                    durationMs = exoPlayer.duration
                    bufferedPositionMs = exoPlayer.bufferedPosition
                    delay(500)
                }
            }
            // Live streams' HLS timeline usually reports no fixed duration (C.TIME_UNSET) --
            // there's no "total" to divide by, so the seek bar reads as parked at the live
            // edge (matches the design's TimeShift-seek-bar-at-live-edge default) rather than
            // computing a meaningless ratio.
            val hasKnownDuration = durationMs > 0 && durationMs != C.TIME_UNSET
            val seekPosition = if (hasKnownDuration) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 1f
            val seekBuffered = if (hasKnownDuration) (bufferedPositionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 1f
            val elapsedLabel = formatPlaybackTime(positionMs)
            val totalLabel = if (hasKnownDuration) formatPlaybackTime(durationMs) else elapsedLabel

            // Backgrounding exits the player (rather than just pause()-ing in place) so the
            // ExoPlayer instance releases fully through the SAME DisposableEffect(exoPlayer)
            // cleanup path already verified above -- one release() call site, not two racing
            // against each other. TV-box memory is more constrained than mobile and there's no
            // PiP mode yet to justify holding the decoder/audio focus alive with nothing visible
            // (product-lead ruling on qa's Phase 2 finding). PiP, when built later, is the
            // natural place to special-case "stay alive while backgrounded" for that mode only.
            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_STOP) onBack()
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
                        PlayerErrorState(message = playerError!!, onBack = onBack, onRetry = { retryCount++ })
                    } else if (isBuffering) {
                        BufferingIndicator()
                    }
                }
                Box(Modifier.fillMaxWidth().padding(28.dp, 24.dp)) {
                    ArePlayerControls(
                        title = media.title,
                        subtitle = media.subtitle,
                        live = media.isLive,
                        playing = playing,
                        position = seekPosition,
                        buffered = seekBuffered,
                        elapsed = elapsedLabel,
                        total = totalLabel,
                        channelLogoInitials = media.title.take(3).uppercase(),
                        onPlayPause = {
                            if (playing) exoPlayer.pause() else exoPlayer.play()
                        },
                        // Real 10s skip -- coerced within [0, duration] when duration is known
                        // (VOD/episode); live streams with no fixed duration (C.TIME_UNSET) just
                        // skip relative to current position, same as scrubbing a live DVR window.
                        onRewind = {
                            exoPlayer.seekTo((exoPlayer.currentPosition - 10_000).coerceAtLeast(0))
                        },
                        onFastForward = {
                            val target = exoPlayer.currentPosition + 10_000
                            exoPlayer.seekTo(if (hasKnownDuration) target.coerceAtMost(durationMs) else target)
                        },
                        // ExoPlayer resolves "default position" to the live edge for a live window,
                        // and to the start for VOD -- exactly "jump to live" for the live case this
                        // button is meant for.
                        onJumpToLive = { exoPlayer.seekToDefaultPosition() },
                        onOpenGuide = onOpenGuide,
                        onMultiView = onMultiView,
                    )
                }
            }
        }
    }
}

/** Formats a millisecond duration as `m:ss` (or `h:mm:ss` past an hour) for the transport HUD's elapsed/total labels. */
private fun formatPlaybackTime(ms: Long): String {
    val totalSeconds = (ms.coerceAtLeast(0) / 1000)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
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
private fun PlayerErrorState(message: String, onBack: () -> Unit, onRetry: (() -> Unit)? = null) {
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
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AreIconButton(icon = Icons.Filled.ArrowBack, contentDescription = "Back", onClick = onBack, variant = AreIconButtonVariant.Solid)
            // Content-not-found (no resolved media at all) has nothing to retry -- onRetry is
            // omitted for that call site below, so this button only appears for real transient
            // playback failures (network/codec/etc) where rebuilding the player might succeed.
            if (onRetry != null) {
                AreIconButton(icon = Icons.Filled.Autorenew, contentDescription = "Retry", onClick = onRetry, variant = AreIconButtonVariant.Solid)
            }
        }
    }
}
