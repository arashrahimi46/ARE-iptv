package com.arashrahimi46.iptv.mobile.ui.player

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.os.Build
import android.util.Rational
import android.widget.FrameLayout
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.arashrahimi46.iptv.core.R as CoreR
import com.arashrahimi46.iptv.mobile.ui.components.AreBadge
import com.arashrahimi46.iptv.mobile.ui.components.AreBadgeTone
import com.arashrahimi46.iptv.mobile.ui.components.AreBottomSheet
import com.arashrahimi46.iptv.mobile.ui.components.AreButton
import com.arashrahimi46.iptv.mobile.ui.components.AreButtonSize
import com.arashrahimi46.iptv.mobile.ui.components.AreButtonVariant
import com.arashrahimi46.iptv.mobile.ui.components.AreChoiceSheet
import com.arashrahimi46.iptv.mobile.ui.components.AreErrorState
import com.arashrahimi46.iptv.mobile.ui.components.AreLoadingState
import com.arashrahimi46.iptv.mobile.ui.components.ArePlayerControls
import com.arashrahimi46.iptv.mobile.design.AreIptvTheme
import com.arashrahimi46.iptv.mobile.design.ProvideOnGlass
import com.arashrahimi46.iptv.mobile.design.glassSurface
import com.arashrahimi46.iptv.mobile.design.glassTrack
import java.util.Locale
import kotlin.math.abs

private const val HUD_AUTO_HIDE_MS = 3500L
private const val SEEK_STEP_MS = 10_000L
private val PLAYBACK_SPEEDS = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)

/** Which transient overlay a vertical drag is currently painting. */
private enum class GaugeKind { Brightness, Volume }

private data class Gauge(val kind: GaugeKind, val value: Float)

/** One selectable audio/text track. A null [group] is the leading [NoneRow]. */
private data class TrackChoice(val label: String, val group: Tracks.Group?, val index: Int)

/**
 * What the first row of a track sheet means. Subtitles have a real [Off] -- the user is asking for
 * no text track at all. Audio does NOT: its first row is "Auto", meaning "drop my override and let
 * the selector pick", and disabling the audio track type there would silently mute the stream.
 */
private enum class NoneRow { Off, Auto }

/** Which sheet, if any, is open. */
private enum class PlayerSheet { Subtitles, Audio, Speed }

/**
 * Touch-first player. `PlayerView` renders video only (`useController = false`) -- every affordance
 * comes from [ArePlayerControls] plus the gesture layer below, which is where all gesture handling
 * lives per the rewrite spec (never inside the controls composable).
 *
 * Real Android Picture-in-Picture, unlike :tv's UI-only corner player: the HUD's PiP button and the
 * hosting Activity's `onUserLeaveHint()` both call [Activity.enterPictureInPictureMode].
 */
@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(target: PlayerTarget, viewModel: PlayerViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val colors = AreIptvTheme.colors
    val motion = AreIptvTheme.motion
    val density = LocalDensity.current
    val activity = context as? Activity
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val player = viewModel.player

    LaunchedEffect(target) { viewModel.load(target) }

    // Pause when the app goes to the background, UNLESS we went there into PiP (where continuing to
    // play is the whole point). Nothing did this before: the only leave-app reaction was
    // MainActivity.onUserLeaveHint -> PiP, which does not fire for the Overview gesture, for
    // screen-off, or when PiP is unavailable/disabled. In those cases video kept decoding and audio
    // kept playing indefinitely in the background, draining the battery.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, activity) {
        val observer = LifecycleEventObserver { _, event ->
            val inPip = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
                activity?.isInPictureInPictureMode == true
            if (event == Lifecycle.Event.ON_STOP && !inPip) player.pause()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // ---- immersive + orientation, both restored on the way out ----
    DisposableEffect(activity) {
        val window = activity?.window
        val previousOrientation = activity?.requestedOrientation
        val controller = window?.let { WindowInsetsControllerCompat(it, it.decorView) }
        controller?.apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
            activity?.requestedOrientation =
                previousOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Screen brightness is an Activity-window attribute; restore whatever it was on the way out so
    // a drag inside the player never leaks into the rest of the app.
    DisposableEffect(activity) {
        val window = activity?.window
        val original = window?.attributes?.screenBrightness
        onDispose {
            if (window != null && original != null) {
                window.attributes = window.attributes.apply { screenBrightness = original }
            }
        }
    }

    var hudVisible by remember { mutableStateOf(true) }
    var interactionTick by remember { mutableStateOf(0) }
    var sheet by remember { mutableStateOf<PlayerSheet?>(null) }
    var gauge by remember { mutableStateOf<Gauge?>(null) }
    var seekFeedback by remember { mutableStateOf<Long?>(null) }
    var speed by remember { mutableFloatStateOf(1f) }
    var resizeMode by remember { mutableStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    var playerView by remember { mutableStateOf<PlayerView?>(null) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var tracks by remember { mutableStateOf(Tracks.EMPTY) }

    // Clock, sampled rather than observed: ExoPlayer has no position flow, and 250ms is well under
    // what the eye reads on a scrubber.
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var bufferedMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(player) {
        while (true) {
            positionMs = player.currentPosition.coerceAtLeast(0L)
            durationMs = player.duration.takeIf { it > 0 } ?: 0L
            bufferedMs = player.bufferedPosition.coerceAtLeast(0L)
            tracks = player.currentTracks
            kotlinx.coroutines.delay(250)
        }
    }

    // Auto-hide, but never while paused -- a paused frame with no chrome reads as a crash.
    LaunchedEffect(hudVisible, state.playing, interactionTick, sheet) {
        if (hudVisible && state.playing && sheet == null) {
            kotlinx.coroutines.delay(HUD_AUTO_HIDE_MS)
            hudVisible = false
        }
    }
    LaunchedEffect(seekFeedback) {
        if (seekFeedback != null) {
            kotlinx.coroutines.delay(700)
            seekFeedback = null
        }
    }
    LaunchedEffect(gauge) {
        if (gauge != null) {
            kotlinx.coroutines.delay(900)
            gauge = null
        }
    }
    // On an error the transport cluster is suppressed and only the top bar carries the back button --
    // so the HUD must be up, or a failed stream (e.g. a 403 from the provider) strands the viewer on a
    // black screen with no visible way back. The auto-hide can't fight this: it only runs while
    // playing, and an error is never playing.
    LaunchedEffect(state.error) { if (state.error != null) hudVisible = true }

    LaunchedEffect(playerView, resizeMode) { playerView?.resizeMode = resizeMode }

    val audioManager = remember(context) { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val currentHudVisible by rememberUpdatedState(hudVisible)

    fun bump() {
        interactionTick++
    }

    fun seekBy(deltaMs: Long) {
        val duration = player.duration
        val next = (player.currentPosition + deltaMs).coerceAtLeast(0L)
        player.seekTo(if (duration > 0) next.coerceAtMost(duration) else next)
        seekFeedback = deltaMs
    }

    fun back() = backDispatcher?.onBackPressed() ?: Unit

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onSizeChanged { containerSize = it }
            // Taps: single = toggle HUD, double = seek / play-pause by third.
            .pointerInput(containerSize) {
                detectTapGestures(
                    onTap = {
                        hudVisible = !hudVisible
                        bump()
                    },
                    onDoubleTap = { offset ->
                        val third = containerSize.width / 3f
                        when {
                            containerSize.width == 0 -> Unit
                            // Seek only where there IS a timeline. A live stream has no duration,
                            // so seekBy couldn't clamp and issued an unbounded seekTo on a
                            // non-seekable window -- a buffering hitch or a jump to the live edge.
                            // The HUD already hides the scrubber for live; the gesture ignored it.
                            state.isLive -> if (player.isPlaying) player.pause() else player.play()
                            offset.x < third -> seekBy(-SEEK_STEP_MS)
                            offset.x > third * 2 -> seekBy(SEEK_STEP_MS)
                            else -> {
                                if (player.isPlaying) player.pause() else player.play()
                                bump()
                            }
                        }
                    },
                )
            }
            // Vertical drag = brightness (left half) / volume (right half); a downward drag that
            // starts in the top quarter while the HUD is up dismisses the player; two fingers = pinch.
            .pointerInput(containerSize) {
                val slop = viewConfiguration.touchSlop
                val dismissThresholdPx = with(density) { 96.dp.toPx() }
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startX = down.position.x
                    val startY = down.position.y
                    val leftHalf = startX < containerSize.width / 2f
                    val fromTopQuarter = startY < containerSize.height / 4f
                    var mode = GestureMode.Undecided
                    var pinchFired = false
                    var dismissed = false

                    while (true) {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.filter { it.pressed }
                        if (pressed.isEmpty()) break
                        if (pressed.size >= 2) {
                            mode = GestureMode.Pinch
                            if (!pinchFired) {
                                val zoom = event.calculateZoom()
                                if (zoom > 1.08f || zoom < 0.92f) {
                                    resizeMode = if (zoom > 1f) {
                                        AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                    } else {
                                        AspectRatioFrameLayout.RESIZE_MODE_FIT
                                    }
                                    pinchFired = true
                                }
                            }
                            pressed.forEach { it.consume() }
                            continue
                        }
                        val change = pressed.first()
                        val totalY = change.position.y - startY
                        val totalX = change.position.x - startX
                        if (mode == GestureMode.Undecided) {
                            if (abs(totalY) > slop && abs(totalY) > abs(totalX)) {
                                mode = if (fromTopQuarter && totalY > 0 && currentHudVisible) {
                                    GestureMode.Dismiss
                                } else {
                                    GestureMode.Vertical
                                }
                            } else if (abs(totalX) > slop) {
                                mode = GestureMode.Ignored
                            }
                        }
                        when (mode) {
                            GestureMode.Vertical -> {
                                val dy = change.positionChange().y
                                // A full screen height of travel = the full range; that is the
                                // pace every phone player uses and it stays usable one-handed.
                                val step = -dy / containerSize.height.coerceAtLeast(1)
                                if (leftHalf) {
                                    val window = activity?.window
                                    if (window != null) {
                                        val attrs = window.attributes
                                        val current = attrs.screenBrightness.takeIf { it >= 0f } ?: 0.5f
                                        val next = (current + step).coerceIn(0.01f, 1f)
                                        window.attributes = attrs.apply { screenBrightness = next }
                                        gauge = Gauge(GaugeKind.Brightness, next)
                                    }
                                } else {
                                    val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                    val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                    val next = (current + step * max).coerceIn(0f, max.toFloat())
                                    audioManager.setStreamVolume(
                                        AudioManager.STREAM_MUSIC,
                                        next.toInt(),
                                        0,
                                    )
                                    gauge = Gauge(GaugeKind.Volume, if (max == 0) 0f else next / max)
                                }
                                change.consume()
                            }
                            GestureMode.Dismiss -> {
                                if (!dismissed && totalY > dismissThresholdPx) {
                                    dismissed = true
                                    back()
                                }
                                change.consume()
                            }
                            else -> Unit
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        // Hold the screen awake while actually playing, as :tv does (LivePlayerScreen.kt:601).
        // PlayerView does not manage this itself, so without it the display slept at the system
        // timeout part-way through a film you weren't touching. Cleared on pause so a long pause
        // can still let the device sleep, and on dispose so it never leaks past the player.
        val view = LocalView.current
        DisposableEffect(state.playing) {
            view.keepScreenOn = state.playing
            onDispose { view.keepScreenOn = false }
        }

        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT,
                    )
                    useController = false
                    this.player = viewModel.player
                    playerView = this
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        if (state.isLoading && state.error == null) {
            AreLoadingState(message = stringResource(CoreR.string.player_buffering))
        }

        state.error?.let { message ->
            AreErrorState(message = message, onRetry = { viewModel.retry() })
        }

        // ---- transient double-tap seek badge ----
        // Forced LTR: the gesture that produced it was decided in RAW PIXELS (`offset.x < third`),
        // so its feedback must land on the same PHYSICAL half. `Alignment.CenterStart` is
        // direction-aware and would flip to the right edge in fa/ar. Same reason
        // `ArePlayerControls` forces LTR on its transport row.
        seekFeedback?.let { delta ->
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 48.dp),
                    contentAlignment = if (delta < 0) Alignment.CenterStart else Alignment.CenterEnd,
                ) {
                    ProvideOnGlass(true) {
                        Box(
                            modifier = Modifier
                                .glassSurface(RoundedCornerShape(AreIptvTheme.radius.pill), elevated = true)
                                .padding(horizontal = 18.dp, vertical = 10.dp),
                        ) {
                            Text(
                                // A signed second count is a numeral, not a sentence -- no locale
                                // copy to translate, and it must read the same in RTL.
                                text = if (delta < 0) "−10s" else "+10s",
                                style = AreIptvTheme.typography.h3,
                                color = colors.textPrimary,
                            )
                        }
                    }
                }
            }
        }

        // ---- transient brightness / volume gauge ----
        // Forced LTR for the same reason: `leftHalf` above is `startX < width / 2f`, a physical-pixel
        // test, so the brightness gauge must always sit on the physical left.
        gauge?.let { g ->
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp),
                    contentAlignment = if (g.kind == GaugeKind.Brightness) {
                        Alignment.CenterStart
                    } else {
                        Alignment.CenterEnd
                    },
                ) {
                    ProvideOnGlass(true) {
                        Column(
                            modifier = Modifier
                                .glassSurface(RoundedCornerShape(AreIptvTheme.radius.lg), elevated = true)
                                .padding(horizontal = 10.dp, vertical = 14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                imageVector = if (g.kind == GaugeKind.Brightness) {
                                    Icons.Filled.BrightnessMedium
                                } else {
                                    Icons.Filled.VolumeUp
                                },
                                contentDescription = null,
                                tint = colors.textPrimary,
                                modifier = Modifier.size(20.dp),
                            )
                            Box(
                                modifier = Modifier
                                    .width(6.dp)
                                    .height(120.dp)
                                    .glassTrack(RoundedCornerShape(AreIptvTheme.radius.pill)),
                                contentAlignment = Alignment.BottomCenter,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(g.value.coerceIn(0f, 1f))
                                        .background(colors.accent, RoundedCornerShape(AreIptvTheme.radius.pill)),
                                )
                            }
                        }
                    }
                }
            }
        }

        // ---- the HUD ----
        AnimatedVisibility(
            visible = hudVisible,
            enter = fadeIn(tween(motion.durFastMs)),
            exit = fadeOut(tween(motion.durFastMs)),
            modifier = Modifier.fillMaxSize(),
        ) {
            ArePlayerControls(
                title = state.title,
                subtitle = state.subtitle,
                live = state.isLive,
                transport = state.error == null && !state.isLoading,
                playing = state.playing,
                buffering = state.buffering,
                position = { positionMs },
                duration = { durationMs },
                buffered = { bufferedMs },
                onSeek = { player.seekTo(it); bump() },
                onSeeking = { bump() },
                onPlayPause = {
                    if (player.isPlaying) player.pause() else player.play()
                    bump()
                },
                onBack = { back() },
                onPrevious = state.prevEpisodeId?.let { id -> { viewModel.playEpisode(id) } },
                onNext = state.nextEpisodeId?.let { id -> { viewModel.playEpisode(id) } },
                onSubtitles = { sheet = PlayerSheet.Subtitles },
                subtitlesActive = tracks.isTypeSelected(C.TRACK_TYPE_TEXT),
                onAudioTrack = { sheet = PlayerSheet.Audio },
                onPlaybackSpeed = { sheet = PlayerSheet.Speed },
                playbackSpeedLabel = if (speed != 1f) speedLabel(speed) else null,
                onAspectRatio = {
                    resizeMode = when (resizeMode) {
                        AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                        else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                    bump()
                },
                onPictureInPicture = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && activity != null) {
                    {
                        activity.enterPictureInPictureMode(
                            PictureInPictureParams.Builder()
                                .setAspectRatio(player.pipAspectRatio())
                                .build(),
                        )
                    }
                } else {
                    null
                },
            )
        }

        // ---- autoplay-next card ----
        state.upNextSeconds?.let { seconds ->
            val next = state.nextEpisodeId
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.BottomEnd) {
                ProvideOnGlass(true) {
                    Column(
                        modifier = Modifier
                            .glassSurface(RoundedCornerShape(AreIptvTheme.radius.lg), elevated = true)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = stringResource(CoreR.string.player_autoplay_next_title),
                            style = AreIptvTheme.typography.h3,
                            color = colors.textPrimary,
                        )
                        Text(
                            text = stringResource(CoreR.string.player_autoplay_next_in, seconds),
                            style = AreIptvTheme.typography.body,
                            color = colors.textSecondary,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            AreButton(
                                text = stringResource(CoreR.string.action_cancel),
                                onClick = { viewModel.cancelUpNext() },
                                variant = AreButtonVariant.Ghost,
                                size = AreButtonSize.Small,
                            )
                            if (next != null) {
                                AreButton(
                                    text = stringResource(CoreR.string.player_autoplay_play_now),
                                    onClick = { viewModel.playEpisode(next) },
                                    variant = AreButtonVariant.Primary,
                                    size = AreButtonSize.Small,
                                )
                            }
                        }
                    }
                }
            }
        }

        // ---- LIVE marker while the HUD is hidden, so a live stream always says so ----
        if (state.isLive && !hudVisible && state.error == null && !state.isLoading) {
            Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.TopEnd) {
                AreBadge(
                    text = stringResource(CoreR.string.player_live_badge),
                    tone = AreBadgeTone.Live,
                    glow = true,
                )
            }
        }
    }

    // ---- sheets ----
    when (sheet) {
        PlayerSheet.Subtitles -> {
            val offLabel = stringResource(CoreR.string.subtitle_off)
            val options = remember(tracks) { trackChoices(tracks, C.TRACK_TYPE_TEXT, offLabel) }
            AreChoiceSheet(
                title = stringResource(CoreR.string.subtitle_picker_title),
                options = options,
                selected = options.firstOrNull { it.group?.isTrackSelected(it.index) == true }
                    ?: options.firstOrNull { it.group == null },
                label = { it.label },
                onSelect = { applyTrack(player, C.TRACK_TYPE_TEXT, it, NoneRow.Off) },
                onDismiss = { sheet = null; bump() },
            )
        }
        PlayerSheet.Audio -> {
            val autoLabel = stringResource(CoreR.string.settings_audio_auto)
            val options = remember(tracks) { trackChoices(tracks, C.TRACK_TYPE_AUDIO, autoLabel) }
            AreChoiceSheet(
                title = stringResource(CoreR.string.player_audio_track),
                options = options,
                selected = options.firstOrNull { it.group?.isTrackSelected(it.index) == true }
                    ?: options.firstOrNull { it.group == null },
                label = { it.label },
                onSelect = { applyTrack(player, C.TRACK_TYPE_AUDIO, it, NoneRow.Auto) },
                onDismiss = { sheet = null; bump() },
            )
        }
        PlayerSheet.Speed -> {
            AreChoiceSheet(
                title = stringResource(CoreR.string.player_playback_speed),
                options = PLAYBACK_SPEEDS,
                selected = speed,
                label = { speedLabel(it) },
                onSelect = {
                    speed = it
                    player.setPlaybackSpeed(it)
                },
                onDismiss = { sheet = null; bump() },
            )
        }
        null -> Unit
    }
}

/** Undecided → one of the four terminal gesture interpretations. */
private enum class GestureMode { Undecided, Vertical, Dismiss, Pinch, Ignored }

/** `1x`, `1.25x` -- a numeral plus the universal multiplier glyph, deliberately not translated. */
private fun speedLabel(speed: Float): String =
    if (speed == speed.toInt().toFloat()) "${speed.toInt()}x" else "${speed}x"

/**
 * Flattens [Tracks] into a pickable list for one track type, with an "off"/"auto" row first.
 * Unsupported tracks are dropped -- offering a track the decoder cannot play is a dead end.
 */
private fun trackChoices(tracks: Tracks, type: Int, noneLabel: String): List<TrackChoice> {
    val out = mutableListOf(TrackChoice(noneLabel, null, -1))
    tracks.groups.filter { it.type == type }.forEach { group ->
        for (i in 0 until group.length) {
            if (!group.isTrackSupported(i)) continue
            val format = group.getTrackFormat(i)
            val label = format.label
                ?: format.language?.takeIf { it.isNotBlank() && it != "und" }
                    ?.let { Locale.forLanguageTag(it).displayLanguage }
                ?: "${out.size}"
            out += TrackChoice(label, group, i)
        }
    }
    return out
}

@OptIn(UnstableApi::class)
private fun applyTrack(
    player: androidx.media3.common.Player,
    type: Int,
    choice: TrackChoice,
    noneRow: NoneRow,
) {
    val builder = player.trackSelectionParameters.buildUpon()
    if (choice.group == null) {
        builder
            .clearOverridesOfType(type)
            .setTrackTypeDisabled(type, noneRow == NoneRow.Off)
    } else {
        builder
            .setTrackTypeDisabled(type, false)
            .setOverrideForType(TrackSelectionOverride(choice.group.mediaTrackGroup, choice.index))
    }
    player.trackSelectionParameters = builder.build()
}

/**
 * The real video aspect ratio for Picture-in-Picture, instead of a hardcoded 16:9 that letterboxed
 * every 4:3, 21:9 or vertical stream inside the PiP window.
 *
 * Falls back to 16:9 before the first frame reports a size, and clamps to the range Android accepts
 * -- `enterPictureInPictureMode` throws IllegalArgumentException outside roughly 1:2.39..2.39:1, and
 * a stream reporting a degenerate size would otherwise crash the app on the PiP button.
 */
private fun Player.pipAspectRatio(): Rational {
    val size = videoSize
    if (size.width <= 0 || size.height <= 0) return Rational(16, 9)
    val ratio = size.width.toDouble() / size.height.toDouble() * (if (size.pixelWidthHeightRatio > 0f) size.pixelWidthHeightRatio else 1f)
    val clamped = ratio.coerceIn(1.0 / 2.39, 2.39)
    return Rational((clamped * 1000).toInt(), 1000)
}
