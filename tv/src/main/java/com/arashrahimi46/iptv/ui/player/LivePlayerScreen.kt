package com.arashrahimi46.iptv.ui.player

import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.text.CueGroup
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.arashrahimi46.iptv.data.settings.UserSettings
import com.arashrahimi46.iptv.ui.components.AreDialog
import com.arashrahimi46.iptv.ui.components.AreIconButton
import com.arashrahimi46.iptv.ui.components.AreIconButtonVariant
import com.arashrahimi46.iptv.ui.components.ArePlayerControls
import com.arashrahimi46.iptv.ui.components.AreStreamHealth
import com.arashrahimi46.iptv.ui.components.AreStreamHealthLevel
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.theme.Ink950
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** P0.1: synthetic error surfaced when auto-retry/fallback gives up on a pure-buffering
 * degradation (no real playerError) with nothing left to fall back to. */
private const val STUCK_ERROR_MESSAGE = "Stream unavailable"

/** Idle time before the transport HUD auto-hides, whether focus is on the video or a control. */
private const val CONTROLS_IDLE_HIDE_MS = 4000L

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
    val upNext by viewModel.upNext.collectAsState()
    var showUpNext by remember { mutableStateOf(false) }
    val settings = remember { UserSettings(context) }
    val hardwareDecoding by settings.isHardwareDecoding.collectAsState(initial = true)

    // In-app mini-player: while a live channel is docked in the corner (LivePlaybackController owns
    // its ExoPlayer), this screen hands its player off on minimize and adopts it back on expand so
    // playback is seamless. Null when the controller isn't provided (minimize simply unavailable).
    val livePlayback = LocalLivePlaybackController.current

    // Hoisted so the outer Box's key handler (declared before the video content) can see whether an
    // error overlay is up: when it is, the outer handler must NOT swallow OK/Up/Down, or the error
    // state's Retry/Back buttons never get focus or activation. Set by the player listener below.
    var playerError by remember { mutableStateOf<String?>(null) }
    val errorFocusRequester = remember { FocusRequester() }
    val errorVisible = playerError != null || (!state.loading && state.media == null)

    // QA MEDIUM defect: BACK from "Playback failed" intermittently left a blank, unrecoverable
    // screen (~4/6 attempts). Root cause: TWO independent paths can call onBack() for the same
    // system-BACK press -- the BackHandler callback below, and the ON_STOP LifecycleEventObserver
    // further down (added so backgrounding tears the player down) -- because popping this
    // destination via BackHandler's popBackStack() can itself trigger an ON_STOP on this
    // composable while it's mid-teardown. A second popBackStack() firing during that transition
    // could pop an extra entry off the NavHost's back stack, leaving it with nothing to render.
    // This guard makes onBack fire at most once per screen instance, regardless of which path
    // reaches it first.
    var backHandled by remember { mutableStateOf(false) }
    val handleBack = remember {
        {
            if (!backHandled) {
                backHandled = true
                onBack()
            }
        }
    }

    // The full-screen Box grabs initial focus and owns the player D-pad model.
    val channelSwitchFocusRequester = remember { FocusRequester() }
    val hudFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { channelSwitchFocusRequester.requestFocus() }

    // Player D-pad model:
    //  - panel CLOSED: Up/Down = prev/next channel; OK/Left/Right = open the control
    //    panel (focus moves inside); Back = leave the player.
    //  - panel OPEN: Left/Right/OK operate the controls; Up/Down still switch channel
    //    (they bubble up from the single control row); Back closes the panel.
    // On a channel switch the panel also "flashes" info then auto-hides, but only while
    // focus is still on the video (panelFocused == false) -- never while the user is
    // actively navigating inside it.
    var controlsVisible by remember { mutableStateOf(true) }
    var panelFocused by remember { mutableStateOf(false) }
    var interactionTick by remember { mutableStateOf(0) }
    // Auto-hide the HUD after a stretch of inactivity -- EVEN while a control is focused. Any
    // D-pad activity bumps interactionTick (see onPreviewKeyEvent), restarting this timer; only
    // genuine idleness hides it. (Previously it never hid once the panel was focused, so a single
    // click left the "console" on screen indefinitely.) On hide, focus returns to the video below.
    LaunchedEffect(interactionTick, controlsVisible) {
        if (controlsVisible) {
            delay(CONTROLS_IDLE_HIDE_MS)
            controlsVisible = false
        }
    }
    LaunchedEffect(controlsVisible) {
        if (!controlsVisible) {
            panelFocused = false
            // Don't yank focus back to the video while an error overlay is up -- its Retry/Back
            // buttons own focus then (see the error-focus effect below).
            if (!errorVisible) channelSwitchFocusRequester.requestFocus()
        }
    }
    // Give the error overlay's primary button initial focus the moment an error appears, and
    // re-grab it if the HUD auto-hides out from under it, so Retry is always reachable by remote.
    LaunchedEffect(errorVisible, controlsVisible) {
        if (errorVisible) {
            delay(50)
            runCatching { errorFocusRequester.requestFocus() }
        }
    }
    LaunchedEffect(panelFocused) {
        if (panelFocused) {
            controlsVisible = true
            delay(50)
            runCatching { hudFocusRequester.requestFocus() }
        }
    }

    // Back closes the open panel first; only then does it leave the player.
    BackHandler {
        if (panelFocused) {
            controlsVisible = false
        } else {
            handleBack()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(channelSwitchFocusRequester)
            .focusable()
            // onPreviewKeyEvent (not onKeyEvent) so Up/Down are intercepted on the way
            // DOWN, before the focus system can consume them to move focus -- otherwise
            // Up/Down with a control focused just moves focus (or escapes the player)
            // instead of switching channel.
            .onPreviewKeyEvent { keyEvent ->
                if (errorVisible) {
                    // Error overlay is up -- let its own Retry/Back buttons receive focus and OK
                    // instead of the player D-pad model consuming these keys.
                    false
                } else if (keyEvent.type == KeyEventType.KeyUp) {
                    // Any key press is "activity" -- reset the idle-hide timer (also captures OK on
                    // a focused HUD button, which passes through preview before the button handles it).
                    interactionTick++
                    when (keyEvent.key) {
                        // Up/Down switch channel ONLY while the panel is closed (focus on the video).
                        // With the panel open they must NOT change channel -- consume them as no-ops
                        // so they neither switch nor let focus escape the single control row.
                        Key.DirectionUp -> {
                            if (!panelFocused) {
                                viewModel.switchChannel(1)
                                controlsVisible = true
                            }
                            true
                        }
                        Key.DirectionDown -> {
                            if (!panelFocused) {
                                viewModel.switchChannel(-1)
                                controlsVisible = true
                            }
                            true
                        }
                        // OK/Left/Right open the panel and move focus into it. Once it's open
                        // (panelFocused) these fall through to the focused control instead.
                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter,
                        Key.DirectionLeft, Key.DirectionRight -> {
                            if (!panelFocused) {
                                panelFocused = true
                                true
                            } else {
                                false
                            }
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
                PlayerErrorState(message = state.errorMessage ?: "Content not found", onBack = handleBack, focusRequester = errorFocusRequester)
            }
        } else {
            var playing by remember { mutableStateOf(true) }
            var isBuffering by remember { mutableStateOf(true) }
            // Bumped by the error state's Retry action (manual) AND by the auto-retry
            // LaunchedEffect below (automatic). Changing this changes exoPlayer's
            // remember() key below, which tears down the old instance through the existing
            // DisposableEffect(exoPlayer) cleanup and builds a fresh one -- same single
            // release() call site as backgrounding, just triggered by a different key change.
            var retryCount by remember { mutableStateOf(0) }
            // P0.1: auto-retry attempts made against the CURRENT source since it last became
            // healthy. Resets per source (media.streamUrl) and on recovery (STATE_READY) --
            // see the listener below and the reset LaunchedEffect -- so a source that recovers
            // gets a fresh retry budget instead of inheriting exhaustion from an earlier stretch.
            var autoRetryAttempt by remember(media.streamUrl) { mutableStateOf(0) }
            // Set true just before minimizing so the DisposableEffect below hands the live player
            // to LivePlaybackController instead of releasing it (seamless dock, no re-buffer).
            var handedOffToMini by remember { mutableStateOf(false) }

            // Subtitle state, reset per stream: the player's current text tracks (updated by the
            // listener below) and the user's pick. Default Off -- subtitles are opt-in, toggled from
            // the HUD's CC button. Applied to the player by the effect further down.
            var tracks by remember(media.streamUrl) { mutableStateOf(Tracks.EMPTY) }
            var subtitleChoice by remember(media.streamUrl) { mutableStateOf<SubtitleTrack>(SubtitleTrack.Off) }
            var showSubtitles by remember { mutableStateOf(false) }
            val textTracks = remember(tracks) { textTracksFrom(tracks) }
            // Language sniffed from the selected track's rendered text (rowKey -> language), for
            // tracks the container left untagged. Reset per stream. Read live in the cue listener.
            val detectedLangs = remember(media.streamUrl) { mutableStateMapOf<String, String>() }
            val currentSubtitleChoice by rememberUpdatedState(subtitleChoice)

            val exoPlayer = remember(media.streamUrl, retryCount, hardwareDecoding) {
                // Seamless EXPAND: if this exact live stream is the one currently docked in the
                // corner mini-player, take that already-running instance back rather than building
                // a fresh one -- no re-buffer, continuous audio/video. Otherwise build normally.
                val adopted = if (media.isLive) livePlayback?.takeIfAdopting(media.streamUrl) else null
                adopted ?: run {
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
                    ExoPlayer.Builder(context, renderersFactory)
                        // Request audio focus and route as media audio so sound actually plays
                        // (and ducks/pauses correctly around other apps) rather than being silent.
                        .setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(C.USAGE_MEDIA)
                                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                                .build(),
                            /* handleAudioFocus = */ true,
                        )
                        .build().apply {
                        // No hardcoded "HLS-only" assumption -- Media3's DefaultMediaSourceFactory
                        // (used implicitly by setMediaItem/prepare) inspects the URI to pick
                        // HlsMediaSource for .m3u8 (media3-exoplayer-hls is on the classpath)
                        // or falls back to ProgressiveMediaSource (bundled TS/MP4/etc extractors)
                        // for raw .ts / other URLs -- covers both shapes real M3U/Xtream sources hand back.
                        setMediaItem(MediaItem.fromUri(media.streamUrl))
                        playWhenReady = true
                        prepare()
                    }
                }.also { playerError = null }
            }

            // Opening a fullscreen player that did NOT adopt the docked instance supersedes it:
            // release the stale mini (a different channel, or any VOD -- the choice to close the
            // live mini when VOD starts). After an adopt, the session is already cleared so this
            // is a no-op.
            LaunchedEffect(media.streamUrl) {
                if (livePlayback?.session?.streamUrl != media.streamUrl) livePlayback?.closeMini()
            }

            // Keep the screen awake while actually playing so the system screensaver / display
            // sleep doesn't kick in mid-stream on an idle remote. Cleared when paused (so a long
            // pause can still sleep) and when the player leaves.
            val view = LocalView.current
            DisposableEffect(playing) {
                view.keepScreenOn = playing
                onDispose { view.keepScreenOn = false }
            }

            DisposableEffect(exoPlayer) {
                val listener = object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        playing = isPlaying
                        // Persist the VOD bookmark on pause (VM no-ops for live) so a pause-then-leave
                        // still resumes where the user stopped, not just the last 5s poll.
                        if (!isPlaying) viewModel.saveProgress(exoPlayer.currentPosition, exoPlayer.duration)
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        isBuffering = playbackState == Player.STATE_BUFFERING
                        if (playbackState == Player.STATE_READY) {
                            playerError = null
                            autoRetryAttempt = 0
                        }
                    }

                    // Feeds the subtitle picker: text tracks become known once the stream is parsed
                    // (and change on a channel switch / MediaItem rebuild).
                    override fun onTracksChanged(newTracks: Tracks) {
                        tracks = newTracks
                    }

                    // Sniff the active track's language from its rendered text -- once per track,
                    // for tracks the container left untagged (see detectSubtitleLanguage). Relabels
                    // the picker row (e.g. "Bamabin.Com" -> "Persian · Bamabin.Com").
                    override fun onCues(cueGroup: CueGroup) {
                        val choice = currentSubtitleChoice as? SubtitleTrack.Embedded ?: return
                        val key = choice.rowKey()
                        if (detectedLangs.containsKey(key)) return
                        val text = cueGroup.cues.joinToString(" ") { it.text?.toString().orEmpty() }
                        detectSubtitleLanguage(text)?.let { detectedLangs[key] = it }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        // Surface the real HTTP status when the server rejects the stream (e.g. a
                        // 461/403 from an expired, connection-limited or IP-locked line) -- far more
                        // actionable than ExoPlayer's generic "bad http status" code name.
                        playerError = when (val cause = error.cause) {
                            is androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException ->
                                "Server refused the stream (HTTP ${cause.responseCode}). The line may be expired, connection-limited, or locked to another IP."
                            is androidx.media3.datasource.HttpDataSource.HttpDataSourceException ->
                                "Couldn't reach the stream -- check your connection."
                            else -> error.errorCodeName.replace('_', ' ').lowercase()
                                .replaceFirstChar { it.uppercase() }
                                .ifBlank { "Playback error" }
                        }
                    }
                }
                exoPlayer.addListener(listener)
                onDispose {
                    // Save the VOD bookmark before this instance is torn down -- covers leaving the
                    // screen AND a retry/fallback rebuild (P2): the rebuilt player's resume-seek
                    // above then restores this position instead of restarting at 0:00.
                    viewModel.saveProgress(exoPlayer.currentPosition, exoPlayer.duration)
                    exoPlayer.removeListener(listener)
                    // Minimizing hands this exact instance to LivePlaybackController (which keeps it
                    // playing in the corner and owns its release), so skip the release here -- else
                    // we'd tear down the very player the mini-player is about to show.
                    if (!handedOffToMini) exoPlayer.release()
                }
            }

            // P0.1: degraded/failed stream health (StreamHealth indicator's Poor/Moderate --
            // playerError or sustained buffering) drives automatic retry with exponential
            // backoff, then falls back to the next available source for this channel once
            // retries are exhausted, instead of leaving the user stuck on a manual-only Retry
            // button. Re-runs whenever health or the attempt count changes; a health recovery
            // (isBuffering/playerError going away) cancels the pending retry via LaunchedEffect's
            // normal key-change cancellation.
            //
            // QA fix: if retries are exhausted and there's no alternate source (fallback
            // returns false), a pure-buffering degradation (no playerError) previously left
            // the screen stuck on the buffering spinner forever -- nothing gates the manual
            // Retry button except playerError being non-null, and none of this effect's keys
            // would ever change again to re-evaluate. Surface a real error message in that case
            // so the existing manual-retry path becomes reachable.
            LaunchedEffect(playerError, isBuffering, autoRetryAttempt) {
                val degraded = playerError != null || isBuffering
                if (!degraded) return@LaunchedEffect
                // QA nit: once we've already given up (set the synthetic "Stream unavailable"
                // error below), don't re-query for an alternate every time this effect re-runs
                // on an unrelated key change -- the answer won't change until autoRetryAttempt
                // resets (new source/manual retry), which already re-triggers this branch fresh.
                if (playerError == STUCK_ERROR_MESSAGE) return@LaunchedEffect
                if (autoRetryAttempt >= StreamRetryPolicy.MAX_RETRIES) {
                    val fellBack = viewModel.fallbackToAlternateSource()
                    if (!fellBack && playerError == null) {
                        playerError = STUCK_ERROR_MESSAGE
                    }
                    return@LaunchedEffect
                }
                delay(if (playerError != null) StreamRetryPolicy.backoffMillis(autoRetryAttempt) else StreamRetryPolicy.BUFFERING_GRACE_MS)
                // P2: persist the VOD position before the rebuild (VM no-ops for live) so the new
                // player's resume-seek restores it instead of restarting the movie/episode at 0:00.
                viewModel.saveProgress(exoPlayer.currentPosition, exoPlayer.duration)
                autoRetryAttempt++
                retryCount++
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
                // P1.2: restore the VOD resume bookmark onto this freshly-built player (no-op /
                // returns 0 for live). Runs for every exoPlayer instance -- so an auto-retry /
                // fallback rebuild (P2) also lands back at the last saved position rather than 0:00.
                val resume = viewModel.resumePositionMs()
                if (resume > 0) exoPlayer.seekTo(resume)
                var saveTick = 0
                while (true) {
                    positionMs = exoPlayer.currentPosition
                    durationMs = exoPlayer.duration
                    bufferedPositionMs = exoPlayer.bufferedPosition
                    // Throttle the bookmark write to ~5s (every 10th 500ms poll); VM gates it to VOD.
                    if (++saveTick >= 10) {
                        saveTick = 0
                        viewModel.saveProgress(positionMs, durationMs)
                    }
                    delay(500)
                }
            }
            // Apply the subtitle pick to the player: Off disables text rendering; an embedded track
            // is force-selected. Re-runs on a player rebuild (channel switch / retry) so the choice
            // survives, and when the user changes it. textTracks is a key so a just-arrived track
            // that matches the current pick gets (re)applied once it's actually available.
            LaunchedEffect(exoPlayer, subtitleChoice, textTracks) {
                exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters.withSubtitle(subtitleChoice)
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

            // Fully backgrounding (ON_STOP) exits the player rather than just pause()-ing in place,
            // so the ExoPlayer instance releases through the SAME DisposableEffect(exoPlayer)
            // cleanup path already verified above -- one release() call site, not two racing
            // against each other. Minimizing to the corner mini-player is NOT this case: it's an
            // in-app nav pop (no ON_STOP), and it hands the player to LivePlaybackController before
            // popping (see handedOffToMini below), so teardown deliberately skips release for it.
            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_STOP) handleBack()
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
                // exoPlayer is remember(streamUrl, retryCount, hardwareDecoding) -- a NEW instance
                // on every channel switch, retry, fallback or decoder toggle. The one-shot factory
                // only binds the first one, so without this the surface stays attached to the
                // released old player (black video). Rebind the surface to the current instance.
                update = { view -> view.player = exoPlayer },
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Ink950.copy(alpha = 0.70f), Ink950.copy(alpha = 0f)),
                            endY = 400f,
                        ),
                    ),
            )

            // Bottom scrim so the transport HUD stays readable over busy video; fades
            // in/out with the controls.
            AnimatedVisibility(
                visible = controlsVisible,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Ink950.copy(alpha = 0f), Ink950.copy(alpha = 0.85f)),
                            ),
                        ),
                )
            }

            // Top bar (back + stream health) / center error-or-buffering / bottom transport HUD.
            Column(Modifier.fillMaxSize()) {
                AnimatedVisibility(visible = controlsVisible, enter = fadeIn(), exit = fadeOut()) {
                Box(Modifier.fillMaxWidth().padding(28.dp, 24.dp)) {
                    AreIconButton(
                        icon = Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        onClick = handleBack,
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
                }
                // QA CRITICAL: fillMaxSize() here claimed the Column's entire remaining
                // height, pushing the transport HUD Box below entirely outside layout
                // bounds -- the HUD has never actually rendered, for any stream. weight(1f)
                // takes only the space left after the top bar and HUD boxes below.
                Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 28.dp), contentAlignment = Alignment.Center) {
                    val currentPlayerError = playerError
                    if (currentPlayerError != null) {
                        PlayerErrorState(
                            message = currentPlayerError,
                            onBack = handleBack,
                            // QA fix: reset the auto-retry budget on manual retry too -- without
                            // this, clicking Retry after auto-retries had already exhausted
                            // immediately re-triggers the exhausted branch of the auto-retry
                            // effect (autoRetryAttempt unchanged, playerError -> null on rebuild),
                            // silently falling back to another source instead of actually
                            // retrying the source the user asked to retry.
                            // P2: save VOD position first so manual retry also resumes, not 0:00.
                            onRetry = {
                                viewModel.saveProgress(exoPlayer.currentPosition, exoPlayer.duration)
                                autoRetryAttempt = 0
                                retryCount++
                            },
                            focusRequester = errorFocusRequester,
                        )
                    } else if (isBuffering) {
                        BufferingIndicator()
                    }
                }
                AnimatedVisibility(visible = controlsVisible, enter = fadeIn(), exit = fadeOut()) {
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
                        onUpNext = { showUpNext = true },
                        onMultiView = onMultiView,
                        isFavorite = state.isFavorite,
                        onToggleFavorite = if (state.favoriteContentType != null) {
                            { viewModel.toggleFavorite() }
                        } else {
                            null
                        },
                        // Minimize to the corner mini-player -- live channels only. Hands this exact
                        // ExoPlayer to the controller (seamless, still playing) then pops back to the
                        // shell where the mini overlay renders it.
                        onPictureInPicture = if (media.isLive && livePlayback != null && state.currentChannelId != null) {
                            {
                                livePlayback.minimize(
                                    exoPlayer,
                                    LiveMiniSession(
                                        channelId = state.currentChannelId!!,
                                        streamUrl = media.streamUrl,
                                        title = media.title,
                                        subtitle = media.subtitle,
                                    ),
                                )
                                handedOffToMini = true
                                handleBack()
                            }
                        } else {
                            null
                        },
                        // Subtitles: enabled once the stream is playing (media resolved); opens the
                        // picker. Lit while an embedded track is active.
                        onSubtitles = { showSubtitles = true },
                        subtitlesActive = subtitleChoice is SubtitleTrack.Embedded,
                        playPauseFocusRequester = hudFocusRequester,
                    )
                }
                }
            }

            if (showSubtitles) {
                SubtitleMenuDialog(
                    tracks = textTracks,
                    selectedKey = (subtitleChoice as? SubtitleTrack.Embedded)?.rowKey(),
                    onSelectOff = { subtitleChoice = SubtitleTrack.Off; showSubtitles = false },
                    onSelectTrack = { subtitleChoice = it; showSubtitles = false },
                    onDismiss = { showSubtitles = false },
                    detectedLangs = detectedLangs,
                )
            }
        }
    }

    if (showUpNext) {
        UpNextDialog(
            channelTitle = state.media?.title,
            programs = upNext,
            onDismiss = { showUpNext = false },
        )
    }
}

/**
 * Mini "up next" EPG panel for the channel currently playing -- reuses [LivePlayerViewModel.upNext]
 * (itself backed by the same [com.arashrahimi46.iptv.data.repository.EpgRepository]/Room data the
 * full TV Guide renders) rather than a second EPG query path. Rendered in a real Dialog window
 * (same pattern as the shell's exit-confirm dialog) so it traps D-pad focus and Back dismisses it.
 */
@Composable
private fun UpNextDialog(channelTitle: String?, programs: List<UpNextProgram>, onDismiss: () -> Unit) {
    val colors = AreIptvTheme.colors
    val zone = ZoneId.systemDefault()
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        AreDialog(onDismiss = onDismiss, title = channelTitle?.let { "Up next -- $it" } ?: "Up next") {
            if (programs.isEmpty()) {
                Text(text = "No programme data", style = AreIptvTheme.typography.body, color = colors.textSecondary)
            } else {
                Column {
                    programs.forEach { program ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                text = Instant.ofEpochMilli(program.startMs).atZone(zone).format(timeFormatter),
                                style = AreIptvTheme.typography.mono,
                                color = if (program.isNow) colors.accentHover else colors.textTertiary,
                            )
                            Text(
                                text = program.title,
                                style = AreIptvTheme.typography.body,
                                color = colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
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
private fun PlayerErrorState(
    message: String,
    onBack: () -> Unit,
    onRetry: (() -> Unit)? = null,
    // When set, focus lands on the primary action (Retry when present, else Back) as the overlay
    // appears -- otherwise the remote can't reach these buttons at all (the outer key handler is
    // gated off while the error is up).
    focusRequester: FocusRequester? = null,
) {
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
            AreIconButton(
                icon = Icons.Filled.ArrowBack,
                contentDescription = "Back",
                onClick = onBack,
                variant = AreIconButtonVariant.Solid,
                modifier = if (onRetry == null && focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier,
            )
            // Content-not-found (no resolved media at all) has nothing to retry -- onRetry is
            // omitted for that call site below, so this button only appears for real transient
            // playback failures (network/codec/etc) where rebuilding the player might succeed.
            if (onRetry != null) {
                AreIconButton(
                    icon = Icons.Filled.Autorenew,
                    contentDescription = "Retry",
                    onClick = onRetry,
                    variant = AreIconButtonVariant.Solid,
                    modifier = if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier,
                )
            }
        }
    }
}
