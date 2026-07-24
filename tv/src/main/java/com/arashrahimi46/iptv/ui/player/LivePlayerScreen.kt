package com.arashrahimi46.iptv.ui.player

import android.os.SystemClock
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
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
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.TeeDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.video.VideoFrameMetadataListener
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.arashrahimi46.iptv.R
import com.arashrahimi46.iptv.data.repository.SubtitleRepository
import com.arashrahimi46.iptv.data.settings.UserSettings
import com.arashrahimi46.iptv.ui.components.AreButton
import com.arashrahimi46.iptv.ui.components.AreButtonVariant
import com.arashrahimi46.iptv.ui.components.AreDialog
import com.arashrahimi46.iptv.ui.components.AreIconButton
import com.arashrahimi46.iptv.ui.components.AreIconButtonVariant
import com.arashrahimi46.iptv.ui.components.ArePlayerControls
import com.arashrahimi46.iptv.ui.components.RecordingIndicator
import com.arashrahimi46.iptv.ui.components.rememberClockFormatter
import com.arashrahimi46.iptv.ui.components.AreStreamHealth
import com.arashrahimi46.iptv.ui.components.AreStreamHealthLevel
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.theme.Ink950
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import kotlin.math.roundToInt

/** Idle time before the transport HUD auto-hides, whether focus is on the video or a control. */
private const val CONTROLS_IDLE_HIDE_MS = 4000L

/** VOD D-pad scrub ladder (ms). Consecutive Left/Right presses climb the ladder so the blue
 * progress bar crosses a long movie in a handful of presses instead of hundreds of 10s nudges;
 * a pause longer than [SCRUB_RESET_MS] drops back to the first (finest) rung. Movies/series only. */
private val SCRUB_STEPS_MS = longArrayOf(10_000L, 30_000L, 60_000L, 120_000L, 300_000L)
private const val SCRUB_RESET_MS = 800L

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

    // Live TV Recording (V1): the capture tee feeds the recording sink alongside the screen. The
    // sink no-ops until a recording starts, so this is wired unconditionally and costs nothing idle.
    val recordingState by viewModel.recordingState.collectAsState()
    val pendingRecord by viewModel.pendingRecord.collectAsState()
    val recordingSink = viewModel.recordingSink
    val mediaSourceFactory = remember(recordingSink) {
        val upstream = DefaultDataSource.Factory(context)
        val teeFactory = DataSource.Factory { TeeDataSource(upstream.createDataSource(), recordingSink) }
        DefaultMediaSourceFactory(teeFactory)
    }
    val recordingFailedGeneric = stringResource(R.string.recording_error_generic)
    val recordingErrorNotWritable = stringResource(R.string.recording_error_not_writable)
    val recordingErrorLowSpace = stringResource(R.string.recording_error_low_space)
    val recordingErrorHls = stringResource(R.string.recording_error_hls)
    val recordingStartedToast = stringResource(R.string.recording_started_toast)
    // Drive selection for recording. Android TV ships no SAF file picker (OPEN_DOCUMENT_TREE only
    // resolves to a stub), so instead of the OS picker the user chooses among the app's own volumes
    // (internal + any mounted USB/SD). Pre-flight failures surface as a toast (no ghost REC).
    val onRecordResult: (com.arashrahimi46.iptv.data.recording.RecordingSupervisor.StartResult) -> Unit = { result ->
        val msg = when (result) {
            is com.arashrahimi46.iptv.data.recording.RecordingSupervisor.StartResult.Started -> recordingStartedToast
            is com.arashrahimi46.iptv.data.recording.RecordingSupervisor.StartResult.Failed -> when (result.reason) {
                com.arashrahimi46.iptv.data.recording.RecordingSupervisor.FailReason.NOT_WRITABLE -> recordingErrorNotWritable
                com.arashrahimi46.iptv.data.recording.RecordingSupervisor.FailReason.LOW_SPACE -> recordingErrorLowSpace
                com.arashrahimi46.iptv.data.recording.RecordingSupervisor.FailReason.HLS_STREAM -> recordingErrorHls
                com.arashrahimi46.iptv.data.recording.RecordingSupervisor.FailReason.CREATE_FAILED -> recordingFailedGeneric
            }
        }
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
    }
    // Volumes to choose from when >1; null hides the picker. With only internal storage we skip the
    // dialog and record straight there.
    var recordVolumes by remember {
        mutableStateOf<List<com.arashrahimi46.iptv.data.recording.RecordingStorage.Volume>?>(null)
    }
    val onRecordPressed: () -> Unit = {
        val volumes = viewModel.availableVolumes()
        if (volumes.size > 1) {
            recordVolumes = volumes
        } else {
            volumes.firstOrNull()?.let { viewModel.startRecording(it.treeUri, onRecordResult) }
        }
    }
    // Surface a fault-driven stop (drive pulled / disk full / stream lost) as a toast so the user
    // learns why capture ended -- the "red dot never lies" also means telling them when it stops.
    LaunchedEffect(recordingState.stoppedReason) {
        recordingState.stoppedReason?.let { reason ->
            Toast.makeText(context, context.getString(R.string.recording_stopped_reason, reason), Toast.LENGTH_LONG).show()
        }
    }
    val settings = remember { UserSettings(context) }
    val hardwareDecoding by settings.isHardwareDecoding.collectAsState(initial = true)
    // Always-on recording badge preference (default on) + reduced-motion for its pulse.
    val showRecordingIndicator by settings.isRecordingIndicatorEnabled.collectAsState(initial = true)
    val reducedMotion by settings.isReducedMotion.collectAsState(initial = false)
    val preferredSubLang by settings.subtitleLanguage.collectAsState(initial = "en")
    val preferredAudioLang by settings.preferredAudioLanguage.collectAsState(initial = "")
    val autoplayNextDelay by settings.autoplayNextDelaySeconds.collectAsState(initial = 0L)
    val subtitleTextScale by settings.subtitleTextScale.collectAsState(initial = com.arashrahimi46.iptv.data.settings.SubtitleTextScale.MEDIUM)
    val subtitleEdge by settings.subtitleEdge.collectAsState(initial = com.arashrahimi46.iptv.data.settings.SubtitleEdge.BOX)
    val subtitleColor by settings.subtitleColor.collectAsState(initial = com.arashrahimi46.iptv.data.settings.SubtitleColorChoice.WHITE)
    val subtitleFont by settings.subtitleFont.collectAsState(initial = com.arashrahimi46.iptv.data.settings.SubtitleFontChoice.DEFAULT)
    // Video aspect/resize mode: seeded from the saved preference, then overridden locally as the user
    // cycles it from the HUD (the override wins so the change is instant; it's persisted below).
    // Live channels remember their own aspect (per channel), so switching channels re-seeds from that
    // channel's saved choice; VOD (no channel id) falls back to the single global default.
    val persistedAspect by settings.videoAspectMode.collectAsState(initial = "FIT")
    val aspectByChannel by settings.videoAspectByChannel.collectAsState(initial = emptyMap())
    val aspectChannelId = state.currentChannelId
    // Reset the local override per channel so the new channel's own saved aspect is shown.
    var aspectOverride by remember(aspectChannelId) { mutableStateOf<AspectMode?>(null) }
    val persistedForChannel = aspectChannelId?.let { aspectByChannel[it.toString()] } ?: persistedAspect
    val aspectMode = aspectOverride ?: AspectMode.fromName(persistedForChannel)
    LaunchedEffect(aspectOverride) {
        aspectOverride?.let { mode ->
            // Live (channel id present) remembers the choice per channel; VOD keeps the global default.
            if (aspectChannelId != null) settings.setChannelAspectMode(aspectChannelId.toString(), mode.name)
            else settings.setVideoAspectMode(mode.name)
        }
    }
    // Online subtitles need BOTH the API key (search) and the account login (download). If either is
    // missing the feature is unusable, so the "Search online" entry is hidden until both are set.
    val subsKeyConnected by settings.openSubsCredential.collectAsState(initial = null)
    val subsSignedIn by settings.openSubsUsername.collectAsState(initial = null)
    val onlineSubsReady = subsKeyConnected != null && subsSignedIn != null

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
    // On a channel switch the panel "flashes" info then auto-hides, but only while
    // focus is still on the video (panelFocused == false) -- never while the user is
    // actively navigating inside it. Starts HIDDEN on first open: the player begins
    // fullscreen and the HUD appears only once the user presses something (Up/Down/OK/
    // Left/Right all reveal it), rather than flashing unprompted on every entry.
    var controlsVisible by remember { mutableStateOf(false) }
    var panelFocused by remember { mutableStateOf(false) }
    var interactionTick by remember { mutableStateOf(0) }
    // VOD scrub: the blue progress bar is a focusable control (movies/series only). The outer key
    // handler (declared before the video content, which owns the ExoPlayer) reaches the live player
    // through this holder and, while the bar is the focused row, ramps the seek step up over
    // consecutive Left/Right presses. See SCRUB_STEPS_MS.
    val scrubPlayer = remember { mutableStateOf<ExoPlayer?>(null) }
    var scrubStepIndex by remember { mutableStateOf(0) }
    var lastScrubAt by remember { mutableStateOf(0L) }
    // The seek bar is the top focus row inside the panel; the button row sits below it. Up/Down
    // move between them (driven explicitly by the handler so focus can't escape the panel).
    val seekBarFocusRequester = remember { FocusRequester() }
    var seekBarFocused by remember { mutableStateOf(false) }
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
            runCatching {
                // Both VOD and live open on the Play button -- the seek bar is one Up press away
                // (VOD) so the user isn't dropped straight onto the scrub row by surprise.
                hudFocusRequester.requestFocus()
            }
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

    val previousChannelLabel = stringResource(R.string.player_previous_channel)
    val nextChannelLabel = stringResource(R.string.player_next_channel)
    val previousEpisodeLabel = stringResource(R.string.player_previous_episode)
    val nextEpisodeLabel = stringResource(R.string.player_next_episode)
    val back10MinLabel = stringResource(R.string.player_back_10_min)
    val forward10MinLabel = stringResource(R.string.player_forward_10_min)
    // Synthetic error surfaced (P0.1) when auto-retry/fallback gives up on a pure-buffering
    // degradation (no real playerError) with nothing left to fall back to; also doubles as the
    // "already gave up" sentinel compared against below, same role as the old English-only literal.
    val playbackFailedText = stringResource(R.string.player_playback_failed)
    val contentNotFoundText = stringResource(R.string.player_content_not_found)

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
                    val isVod = state.media?.isLive == false
                    when (keyEvent.key) {
                        // Panel CLOSED: Up/Down switch channel (live only) -- a movie has no channel
                        // to switch to, so they just open the panel. Panel OPEN: Up/Down move between
                        // the two focus rows (seek bar on top, transport buttons below) for VOD, and
                        // are consumed no-ops on live's single row so focus can't escape the panel.
                        Key.DirectionUp -> {
                            if (!panelFocused) {
                                if (!isVod) viewModel.switchChannel(-1)
                                controlsVisible = true
                            } else if (isVod && !seekBarFocused) {
                                // Button row -> back up to the seek bar.
                                runCatching { seekBarFocusRequester.requestFocus() }
                            }
                            true
                        }
                        Key.DirectionDown -> {
                            if (!panelFocused) {
                                if (isVod) panelFocused = true
                                else {
                                    viewModel.switchChannel(1)
                                    controlsVisible = true
                                }
                            } else if (isVod && seekBarFocused) {
                                // Seek bar -> drop to the transport buttons (Play).
                                runCatching { hudFocusRequester.requestFocus() }
                            }
                            true
                        }
                        // Left/Right scrub the blue bar ONLY when it's the focused row (VOD). Panel
                        // closed -> open the panel. Panel open with a button focused -> fall through
                        // so the focus system moves between the transport buttons.
                        Key.DirectionLeft, Key.DirectionRight -> {
                            val player = scrubPlayer.value
                            if (panelFocused && seekBarFocused && player != null) {
                                val now = SystemClock.elapsedRealtime()
                                scrubStepIndex = if (now - lastScrubAt <= SCRUB_RESET_MS) {
                                    (scrubStepIndex + 1).coerceAtMost(SCRUB_STEPS_MS.lastIndex)
                                } else {
                                    0
                                }
                                lastScrubAt = now
                                val step = SCRUB_STEPS_MS[scrubStepIndex]
                                val delta = if (keyEvent.key == Key.DirectionRight) step else -step
                                // Prefer our known recorded length (see effectiveDurationMs) so a
                                // scrub can reach the true end of a recording, not ExoPlayer's short
                                // TS estimate.
                                val dur = state.media?.knownDurationMs ?: player.duration
                                val target = player.currentPosition + delta
                                player.seekTo(
                                    if (dur > 0 && dur != C.TIME_UNSET) target.coerceIn(0L, dur)
                                    else target.coerceAtLeast(0L),
                                )
                                controlsVisible = true
                                true
                            } else if (!panelFocused) {
                                panelFocused = true
                                true
                            } else {
                                false
                            }
                        }
                        // OK opens the panel; once open it falls through to the focused control.
                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                            if (!panelFocused) {
                                panelFocused = true
                                true
                            } else {
                                false
                            }
                        }
                        else -> false
                    }
                } else if (keyEvent.type == KeyEventType.KeyDown &&
                    state.media?.isLive == false && panelFocused && seekBarFocused &&
                    (keyEvent.key == Key.DirectionLeft || keyEvent.key == Key.DirectionRight)
                ) {
                    // Consume Left/Right on the DOWN edge while the seek bar is focused. Our seek
                    // runs on KeyUp; if the down edge isn't consumed the focus system moves focus
                    // first -- Right had no focusable neighbour so it stayed and "worked", but Left
                    // jumped to the Back button, dropping the KeyUp seek (the "can't go back" bug).
                    true
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
                PlayerErrorState(message = state.errorMessage ?: contentNotFoundText, onBack = handleBack, focusRequester = errorFocusRequester)
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
            // Audio-track state, reset per stream: the tracks the current stream exposes and the user's
            // forced pick (null = the player's automatic choice). Applied by the effect further down.
            val audioTracks = remember(tracks) { audioTracksFrom(tracks) }
            var audioChoice by remember(media.streamUrl) { mutableStateOf<AudioTrack?>(null) }
            var showAudio by remember { mutableStateOf(false) }
            // Playback-speed state, reset per stream (session-local, not persisted): the active rate
            // and whether the picker is open. VOD/recordings only -- the HUD hides it on live.
            var playbackSpeed by remember(media.streamUrl) { mutableStateOf(1f) }
            var showSpeed by remember { mutableStateOf(false) }
            // Audio-delay state, reset per stream (session-local, not persisted): the offset in ms
            // applied through the Media3 audio sink (see DelayAudioProcessor) and whether the stepper is
            // open. The processor instance is stream-scoped so a channel/title switch starts back in sync.
            val audioDelayProcessor = remember(media.streamUrl) { DelayAudioProcessor() }
            var audioOffsetMs by remember(media.streamUrl) { mutableStateOf(0) }
            var showAudioDelay by remember { mutableStateOf(false) }
            // Language sniffed from the selected track's rendered text (rowKey -> language), for
            // tracks the container left untagged. Reset per stream. Read live in the cue listener.
            val detectedLangs = remember(media.streamUrl) { mutableStateMapOf<String, String>() }
            val currentSubtitleChoice by rememberUpdatedState(subtitleChoice)

            // Online subtitle search (OpenSubtitles). Only VOD (a searchable title name) offers it.
            val searchName = media.searchName
            val subtitleRepo = remember { SubtitleRepository(context) }
            var showSubtitleSearch by remember { mutableStateOf(false) }
            // Subtitles downloaded this session are sideloaded by rebuilding the MediaItem with these
            // configs; a pending flag then auto-selects the newly-added track once it's parsed. Both
            // reset per stream so a channel/title switch starts clean.
            var externalSubs by remember(media.streamUrl) { mutableStateOf<List<MediaItem.SubtitleConfiguration>>(emptyList()) }
            var pendingSelectExternal by remember(media.streamUrl) { mutableStateOf(false) }

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
                    // DelayRenderersFactory swaps only the audio sink so the "Audio sync" control can
                    // shift audio vs. video at runtime (audioDelayProcessor); otherwise it's stock.
                    val renderersFactory = DelayRenderersFactory(context, audioDelayProcessor).apply {
                        setExtensionRendererMode(
                            if (hardwareDecoding) DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF
                            else DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON,
                        )
                        setEnableDecoderFallback(true)
                    }
                    ExoPlayer.Builder(context, renderersFactory)
                        // Tee playback bytes to the recording sink (Live TV Recording V1). Media3's
                        // built-in TeeDataSource copies every read byte to the sink; the sink no-ops
                        // until a recording is active, so playback is untouched when idle.
                        .setMediaSourceFactory(mediaSourceFactory)
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

            // Autoplay-next (Phase 2): the player listener flips [playbackEnded] true on STATE_ENDED;
            // keyed to the player instance so switching episodes (a fresh player) clears it.
            // [autoAdvanceRemaining] drives the countdown dialog (null = inactive).
            var playbackEnded by remember(exoPlayer) { mutableStateOf(false) }
            var autoAdvanceRemaining by remember(exoPlayer) { mutableStateOf<Int?>(null) }

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

            // Counts frames actually rendered, incremented on the playback thread by the frame
            // metadata listener below. The poll derives a measured fps from its delta -- most
            // HLS/TS streams never declare frameRate in their Format, so we can't read it directly.
            val renderedFrames = remember { java.util.concurrent.atomic.AtomicLong(0L) }

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
                        // Arm autoplay-next; the LaunchedEffect below decides if it actually applies.
                        if (playbackState == Player.STATE_ENDED) playbackEnded = true
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
                                "$playbackFailedText: HTTP ${cause.responseCode}"
                            is androidx.media3.datasource.HttpDataSource.HttpDataSourceException ->
                                playbackFailedText
                            else -> error.errorCodeName.replace('_', ' ').lowercase()
                                .replaceFirstChar { it.uppercase() }
                                .ifBlank { playbackFailedText }
                        }
                    }
                }
                exoPlayer.addListener(listener)
                // Measure the real rendered frame rate (see renderedFrames): fires once per output
                // frame on the playback thread, so a simple atomic increment is all that's needed.
                renderedFrames.set(0L)
                val frameListener = VideoFrameMetadataListener { _, _, _, _ -> renderedFrames.incrementAndGet() }
                exoPlayer.setVideoFrameMetadataListener(frameListener)
                onDispose {
                    // Save the VOD bookmark before this instance is torn down -- covers leaving the
                    // screen AND a retry/fallback rebuild (P2): the rebuilt player's resume-seek
                    // above then restores this position instead of restarting at 0:00.
                    viewModel.saveProgress(exoPlayer.currentPosition, exoPlayer.duration)
                    exoPlayer.removeListener(listener)
                    exoPlayer.clearVideoFrameMetadataListener(frameListener)
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
                // QA nit: once we've already given up (set the synthetic "playback failed" error
                // below), don't re-query for an alternate every time this effect re-runs on an
                // unrelated key change -- the answer won't change until autoRetryAttempt resets
                // (new source/manual retry), which already re-triggers this branch fresh.
                if (playerError == playbackFailedText) return@LaunchedEffect
                if (autoRetryAttempt >= StreamRetryPolicy.MAX_RETRIES) {
                    val fellBack = viewModel.fallbackToAlternateSource()
                    if (!fellBack && playerError == null) {
                        playerError = playbackFailedText
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

            // Option B recording handshake: once playback has switched to the Xtream `.ts` variant
            // and is actually live (ready, not buffering), start the tee -- so the recorded file
            // begins on a real TS byte instead of a torn-down HLS tail. If the `.ts` variant errors
            // (server is HLS-only / off-air), revert to the original HLS stream so the user keeps
            // watching. No-op when nothing is pending.
            LaunchedEffect(exoPlayer, playing, isBuffering, pendingRecord, playerError) {
                if (pendingRecord == null) return@LaunchedEffect
                if (playerError != null) viewModel.abandonPendingRecording()
                else if (playing && !isBuffering) viewModel.beginPendingRecording()
            }
            // Safety net: if the `.ts` variant never reaches a playing state (silent stall, no hard
            // error), revert after a grace period rather than leaving playback stuck on it.
            LaunchedEffect(pendingRecord) {
                if (pendingRecord != null) {
                    delay(15_000)
                    viewModel.abandonPendingRecording()
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
            // Live only: the real decoded resolution + frame rate ("1080p · 50fps"), read from the
            // player's current video Format in the poll below. Null until the format is known.
            var streamInfo by remember(media.streamUrl) { mutableStateOf<String?>(null) }
            LaunchedEffect(exoPlayer) {
                // Expose the current player to the outer D-pad handler for VOD scrubbing; it's
                // rebuilt on retry/fallback, so keep the holder pointed at the live instance.
                scrubPlayer.value = exoPlayer
                // P1.2: restore the VOD resume bookmark onto this freshly-built player (no-op /
                // returns 0 for live). Runs for every exoPlayer instance -- so an auto-retry /
                // fallback rebuild (P2) also lands back at the last saved position rather than 0:00.
                val resume = viewModel.resumePositionMs()
                if (resume > 0) exoPlayer.seekTo(resume)
                var saveTick = 0
                // Measured-fps window: recompute every ~2s (4 polls) for a stable reading -- a 500ms
                // window jitters between e.g. 24/26 for a 25fps feed, while 2s settles on 25.
                var fpsTick = 0
                var lastFrames = 0L
                var lastFpsAt = SystemClock.elapsedRealtime()
                var measuredFps = 0
                while (true) {
                    positionMs = exoPlayer.currentPosition
                    durationMs = exoPlayer.duration
                    bufferedPositionMs = exoPlayer.bufferedPosition
                    if (media.isLive) {
                        if (++fpsTick >= 4) {
                            val now = SystemClock.elapsedRealtime()
                            val frames = renderedFrames.get()
                            val dt = now - lastFpsAt
                            if (dt > 0) measuredFps = ((frames - lastFrames) * 1000.0 / dt).roundToInt()
                            lastFrames = frames
                            lastFpsAt = now
                            fpsTick = 0
                        }
                        streamInfo = formatStreamInfo(exoPlayer.videoFormat, measuredFps)
                    }
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
            LaunchedEffect(exoPlayer, subtitleChoice, textTracks, audioChoice, audioTracks, preferredAudioLang) {
                exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                    .withSubtitle(subtitleChoice)
                    .withAudioTrack(audioChoice)
                    // Preferred audio language only applies when the user hasn't force-picked a track
                    // in the player HUD (an explicit override always wins). Blank pref = automatic.
                    .buildUpon()
                    .setPreferredAudioLanguage(if (audioChoice == null) preferredAudioLang.ifBlank { null } else null)
                    .build()
            }
            // Apply the playback speed to the player. Re-runs on a player rebuild (retry/channel
            // switch) so the chosen rate survives, and when the user changes it.
            LaunchedEffect(exoPlayer, playbackSpeed) {
                exoPlayer.setPlaybackSpeed(playbackSpeed)
            }
            // Push the audio-delay offset into the sink's processor. Re-runs when the user retunes it;
            // the processor reads delayMs live off the audio thread so no player rebuild is needed.
            LaunchedEffect(audioDelayProcessor, audioOffsetMs) {
                audioDelayProcessor.delayMs = audioOffsetMs
            }
            // Autoplay-next: when a VOD episode ends, the delay is set, and a next episode exists,
            // count down then advance. Keyed to playbackEnded (reset on the fresh player) so it fires
            // once per episode; a manual skip/leave rebuilds the player and cancels this.
            LaunchedEffect(playbackEnded, autoplayNextDelay) {
                if (!playbackEnded) { autoAdvanceRemaining = null; return@LaunchedEffect }
                val idx = state.siblingEpisodeIds.indexOf(state.currentEpisodeId)
                val hasNext = state.currentEpisodeId != null && idx in 0 until state.siblingEpisodeIds.lastIndex
                if (state.media?.isLive != false || autoplayNextDelay <= 0L || !hasNext) return@LaunchedEffect
                for (s in autoplayNextDelay.toInt() downTo 1) {
                    autoAdvanceRemaining = s
                    delay(1000)
                }
                autoAdvanceRemaining = null
                viewModel.switchEpisode(1)
            }

            // Sideload downloaded subtitles by re-setting the MediaItem with them attached, seeking
            // back so playback continues in place. Re-runs on a player rebuild (retry/channel switch)
            // so the sideloaded track survives -- externalSubs is keyed per stream.
            LaunchedEffect(exoPlayer, externalSubs) {
                if (externalSubs.isEmpty()) return@LaunchedEffect
                val pos = exoPlayer.currentPosition
                val wasPlaying = exoPlayer.playWhenReady
                exoPlayer.setMediaItem(
                    MediaItem.Builder().setUri(media.streamUrl).setSubtitleConfigurations(externalSubs).build(),
                    pos,
                )
                exoPlayer.prepare()
                exoPlayer.playWhenReady = wasPlaying
            }

            // Auto-select a just-downloaded subtitle once it shows up as a text track (the user asked
            // for it, so turn it on immediately). Matched by our sentinel "OpenSubtitles" label.
            LaunchedEffect(textTracks, pendingSelectExternal) {
                if (!pendingSelectExternal) return@LaunchedEffect
                val ext = textTracks.firstOrNull { it.label.contains("OpenSubtitles", ignoreCase = true) }
                if (ext != null) {
                    subtitleChoice = ext
                    pendingSelectExternal = false
                }
            }

            // Live streams' HLS timeline usually reports no fixed duration (C.TIME_UNSET) --
            // there's no "total" to divide by, so the seek bar reads as parked at the live
            // edge (matches the design's TimeShift-seek-bar-at-live-edge default) rather than
            // computing a meaningless ratio.
            // Prefer a duration we already know (recordings carry their measured wall-clock length)
            // over ExoPlayer's own estimate -- the latter is garbage for a raw recorded live-TS
            // (PTS resets on ad loops), which is what made a 57-min recording read as a few seconds.
            // Real VOD has no knownDurationMs, so it still uses the player's (accurate) duration.
            val effectiveDurationMs = media.knownDurationMs ?: durationMs
            val hasKnownDuration = effectiveDurationMs > 0 && effectiveDurationMs != C.TIME_UNSET
            val seekPosition = if (hasKnownDuration) (positionMs.toFloat() / effectiveDurationMs).coerceIn(0f, 1f) else 1f
            val seekBuffered = if (hasKnownDuration) (bufferedPositionMs.toFloat() / effectiveDurationMs).coerceIn(0f, 1f) else 1f
            val elapsedLabel = formatPlaybackTime(positionMs)
            val totalLabel = if (hasKnownDuration) formatPlaybackTime(effectiveDurationMs) else elapsedLabel

            // ⏮/⏭ transport-skip pair, resolved per content type:
            //  - live: previous/next channel (switchChannel wraps the sibling list).
            //  - series episode: previous/next episode -- hidden (null) at the first/last, no wrap.
            //  - movie: −10min/+10min seeks (coerced within the movie's duration).
            val episodeIndex = state.siblingEpisodeIds.indexOf(state.currentEpisodeId)
            val isEpisode = state.currentEpisodeId != null
            val skipPrevious: (() -> Unit)?
            val skipNext: (() -> Unit)?
            val skipPreviousLabel: String
            val skipNextLabel: String
            when {
                media.isLive -> {
                    skipPrevious = { viewModel.switchChannel(-1) }
                    skipNext = { viewModel.switchChannel(1) }
                    skipPreviousLabel = previousChannelLabel
                    skipNextLabel = nextChannelLabel
                }
                isEpisode -> {
                    skipPrevious = if (episodeIndex > 0) ({ viewModel.switchEpisode(-1) }) else null
                    skipNext = if (episodeIndex in 0 until state.siblingEpisodeIds.lastIndex) ({ viewModel.switchEpisode(1) }) else null
                    skipPreviousLabel = previousEpisodeLabel
                    skipNextLabel = nextEpisodeLabel
                }
                else -> {
                    skipPrevious = { exoPlayer.seekTo((exoPlayer.currentPosition - 600_000).coerceAtLeast(0)) }
                    skipNext = {
                        val target = exoPlayer.currentPosition + 600_000
                        exoPlayer.seekTo(if (hasKnownDuration) target.coerceAtMost(effectiveDurationMs) else target)
                    }
                    skipPreviousLabel = back10MinLabel
                    skipNextLabel = forward10MinLabel
                }
            }

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

            // User subtitle appearance (Phase 2): our style overrides embedded caption styling so it
            // applies consistently. Rebuilt only when a subtitle-appearance pref changes.
            val captionStyle = remember(subtitleEdge, subtitleColor, subtitleFont) {
                buildCaptionStyle(context, subtitleEdge, subtitleColor, subtitleFont)
            }
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = {
                    PlayerView(context).apply {
                        useController = false
                        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                        player = exoPlayer
                        resizeMode = aspectMode.resizeMode
                        subtitleView?.apply {
                            setApplyEmbeddedStyles(false)
                            setStyle(captionStyle)
                            setFractionalTextSize(subtitleTextScale.fraction)
                        }
                    }
                },
                // exoPlayer is remember(streamUrl, retryCount, hardwareDecoding) -- a NEW instance
                // on every channel switch, retry, fallback or decoder toggle. The one-shot factory
                // only binds the first one, so without this the surface stays attached to the
                // released old player (black video). Rebind the surface to the current instance.
                update = { view ->
                    view.player = exoPlayer
                    view.resizeMode = aspectMode.resizeMode
                    view.subtitleView?.apply {
                        setApplyEmbeddedStyles(false)
                        setStyle(captionStyle)
                        setFractionalTextSize(subtitleTextScale.fraction)
                    }
                },
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
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.action_back),
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
                                // Stalker (resolve-on-play): re-mint the short-lived link via the
                                // resolver rather than replaying a possibly-expired URL. The reload
                                // changes media.streamUrl, which rebuilds the player on its own; other
                                // sources just bump retryCount to rebuild with the same (static) URL.
                                if (state.resolveOnPlay) viewModel.reload() else retryCount++
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
                        streamInfo = streamInfo,
                        live = media.isLive,
                        playing = playing,
                        position = seekPosition,
                        buffered = seekBuffered,
                        elapsed = elapsedLabel,
                        total = totalLabel,
                        channelLogoInitials = media.title.take(3).uppercase(),
                        // Movies/series only: make the progress bar a focusable scrub control (shows
                        // a thumb when focused). Null on live -> bar stays display-only.
                        seekBarFocusRequester = if (media.isLive) null else seekBarFocusRequester,
                        onSeekBarFocusChanged = { seekBarFocused = it },
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
                            exoPlayer.seekTo(if (hasKnownDuration) target.coerceAtMost(effectiveDurationMs) else target)
                        },
                        // ⏮/⏭ are context-dependent: live -> prev/next channel; series episode ->
                        // prev/next episode (hidden at the first/last); movie -> −10min/+10min. See
                        // the resolved values built just above the HUD.
                        onSkipPrevious = skipPrevious,
                        onSkipNext = skipNext,
                        skipPreviousLabel = skipPreviousLabel,
                        skipNextLabel = skipNextLabel,
                        onOpenGuide = onOpenGuide,
                        onUpNext = { showUpNext = true },
                        onMultiView = onMultiView,
                        // Add-to-multi-view: live channels only (movies/series don't belong there).
                        onAddToMultiView = if (media.isLive && state.currentChannelId != null) {
                            {
                                viewModel.addCurrentChannelToMultiView()
                                Toast.makeText(context, context.getString(R.string.multiview_added_toast), Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            null
                        },
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
                        // Audio track: only offer the picker when there's an actual choice (>1 track).
                        onAudioTrack = if (audioTracks.size > 1) {
                            { showAudio = true }
                        } else {
                            null
                        },
                        audioTrackActive = audioChoice != null,
                        // Audio sync: always available -- a ±ms audio-delay stepper to fix out-of-sync
                        // audio. Show the active offset on the button face only when it's non-zero.
                        onAudioDelay = { showAudioDelay = true },
                        audioDelayLabel = if (audioOffsetMs != 0) formatAudioDelay(audioOffsetMs) else null,
                        // Playback speed: VOD/recordings only (hidden on live). Show the active rate on
                        // the button face only when it's non-default.
                        onPlaybackSpeed = if (!media.isLive) {
                            { showSpeed = true }
                        } else {
                            null
                        },
                        playbackSpeedLabel = if (playbackSpeed != 1f) formatPlaybackSpeed(playbackSpeed) else null,
                        // Aspect ratio: cycle Fit -> Fill -> Stretch, persist, and toast the new mode.
                        onAspectRatio = {
                            val next = aspectMode.next()
                            aspectOverride = next
                            Toast.makeText(
                                context,
                                context.getString(R.string.player_aspect_toast, context.getString(next.labelRes)),
                                Toast.LENGTH_SHORT,
                            ).show()
                        },
                        // ● REC (Live TV Recording V1): recordable live .ts channels only. Recording
                        // stops in place; idle opens the drive picker. Non-.ts -> dimmed placeholder.
                        onToggleRecord = if (viewModel.isCurrentStreamRecordable) {
                            {
                                if (recordingState.phase == com.arashrahimi46.iptv.data.recording.RecordingSupervisor.Phase.Recording ||
                                    recordingState.phase == com.arashrahimi46.iptv.data.recording.RecordingSupervisor.Phase.Reconnecting
                                ) {
                                    viewModel.stopRecording()
                                } else {
                                    onRecordPressed()
                                }
                            }
                        } else {
                            null
                        },
                        recordingActive = recordingState.phase == com.arashrahimi46.iptv.data.recording.RecordingSupervisor.Phase.Recording ||
                            recordingState.phase == com.arashrahimi46.iptv.data.recording.RecordingSupervisor.Phase.Reconnecting,
                        recordingReconnecting = recordingState.phase == com.arashrahimi46.iptv.data.recording.RecordingSupervisor.Phase.Reconnecting,
                        playPauseFocusRequester = hudFocusRequester,
                    )
                }
                }
            }

            // Recording badge: OUTSIDE the auto-hiding HUD so it stays visible the whole time a
            // recording runs (the user must never forget). Top-center avoids the back button (left)
            // and stream-health chip (right). Hidden when the user turns the indicator off.
            if (showRecordingIndicator &&
                (recordingState.phase == com.arashrahimi46.iptv.data.recording.RecordingSupervisor.Phase.Recording ||
                    recordingState.phase == com.arashrahimi46.iptv.data.recording.RecordingSupervisor.Phase.Reconnecting)
            ) {
                RecordingIndicator(
                    reconnecting = recordingState.phase == com.arashrahimi46.iptv.data.recording.RecordingSupervisor.Phase.Reconnecting,
                    elapsedMs = recordingState.elapsedMs,
                    reducedMotion = reducedMotion,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 24.dp),
                )
            }

            recordVolumes?.let { volumes ->
                RecordVolumeDialog(
                    volumes = volumes,
                    onPick = { volume ->
                        recordVolumes = null
                        viewModel.startRecording(volume.treeUri, onRecordResult)
                    },
                    onDismiss = { recordVolumes = null },
                )
            }

            if (showSpeed) {
                PlaybackSpeedMenuDialog(
                    current = playbackSpeed,
                    onDismiss = { showSpeed = false },
                    onSelect = { playbackSpeed = it; showSpeed = false },
                )
            }
            if (showAudioDelay) {
                // Stepper stays open across nudges so the user can retune the offset while watching.
                AudioDelayMenuDialog(
                    currentMs = audioOffsetMs,
                    onDismiss = { showAudioDelay = false },
                    onChange = { audioOffsetMs = it },
                )
            }
            if (showAudio) {
                AudioTrackMenuDialog(
                    tracks = audioTracks,
                    selectedKey = audioChoice?.rowKey(),
                    onSelectTrack = { audioChoice = it; showAudio = false },
                    onDismiss = { showAudio = false },
                )
            }
            if (showSubtitles) {
                SubtitleMenuDialog(
                    tracks = textTracks,
                    selectedKey = (subtitleChoice as? SubtitleTrack.Embedded)?.rowKey(),
                    onSelectOff = { subtitleChoice = SubtitleTrack.Off; showSubtitles = false },
                    onSelectTrack = { subtitleChoice = it; showSubtitles = false },
                    onDismiss = { showSubtitles = false },
                    detectedLangs = detectedLangs,
                    onSearchOnline = if (searchName != null && onlineSubsReady) {
                        { showSubtitles = false; showSubtitleSearch = true }
                    } else {
                        null
                    },
                )
            }

            if (showSubtitleSearch && searchName != null) {
                OnlineSubtitleSearchDialog(
                    initialQuery = searchName,
                    defaultLang = preferredSubLang,
                    onSearch = { q, lang ->
                        subtitleRepo.search(q, lang, year = media.year, season = media.season, episode = media.episode)
                    },
                    onPick = { sub ->
                        subtitleRepo.download(sub).map { sideloaded ->
                            val config = MediaItem.SubtitleConfiguration.Builder(sideloaded.uri)
                                .setMimeType(sideloaded.mimeType)
                                .setLanguage(sideloaded.languageCode)
                                .setLabel("OpenSubtitles")
                                .build()
                            externalSubs = externalSubs + config
                            pendingSelectExternal = true
                        }
                    },
                    onDismiss = { showSubtitleSearch = false },
                )
            }

            // Autoplay-next countdown -- a focus-trapping dialog (TV convention) so the remote can
            // cancel or skip the wait. Dismiss/Cancel stops the auto-advance; Play now advances now.
            val remaining = autoAdvanceRemaining
            if (remaining != null) {
                Dialog(
                    onDismissRequest = { playbackEnded = false; autoAdvanceRemaining = null },
                    properties = DialogProperties(usePlatformDefaultWidth = false),
                ) {
                    AreDialog(
                        onDismiss = { playbackEnded = false; autoAdvanceRemaining = null },
                        title = stringResource(R.string.player_autoplay_next_title),
                        actions = {
                            AreButton(
                                stringResource(R.string.action_cancel),
                                onClick = { playbackEnded = false; autoAdvanceRemaining = null },
                                variant = AreButtonVariant.Ghost,
                            )
                            AreButton(
                                stringResource(R.string.player_autoplay_play_now),
                                onClick = { autoAdvanceRemaining = null; viewModel.switchEpisode(1) },
                                variant = AreButtonVariant.Primary,
                            )
                        },
                    ) {
                        Text(
                            text = stringResource(R.string.player_autoplay_next_in, remaining),
                            style = AreIptvTheme.typography.body,
                            color = AreIptvTheme.colors.textSecondary,
                        )
                    }
                }
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
    val timeFormatter = rememberClockFormatter()
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val title = channelTitle?.let { stringResource(R.string.player_up_next_channel, it) } ?: stringResource(R.string.player_up_next)
        AreDialog(onDismiss = onDismiss, title = title) {
            if (programs.isEmpty()) {
                Text(text = stringResource(R.string.player_no_programme_data), style = AreIptvTheme.typography.body, color = colors.textSecondary)
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

/**
 * "1080p · 50fps" from the live stream's actual decoded video [androidx.media3.common.Format] --
 * resolution from the frame height. Frame rate prefers the container-declared rate when present,
 * else falls back to [measuredFps] (counted rendered frames). Null until the format is known; omits
 * the fps segment when neither source has a value yet.
 */
private fun formatStreamInfo(format: androidx.media3.common.Format?, measuredFps: Int): String? {
    val height = format?.height?.takeIf { it > 0 } ?: return null
    val fps = format.frameRate.takeIf { it > 0f }?.roundToInt() ?: measuredFps.takeIf { it > 0 }
    return if (fps != null) "${height}p · ${fps}fps" else "${height}p"
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
        Text(text = stringResource(R.string.player_buffering), style = AreIptvTheme.typography.caption, color = colors.textSecondary)
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
        Text(text = stringResource(R.string.player_playback_failed), style = AreIptvTheme.typography.h2, color = colors.textPrimary)
        Box(Modifier.padding(top = 6.dp))
        Text(text = message, style = AreIptvTheme.typography.body, color = colors.textSecondary)
        Box(Modifier.padding(top = 20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AreIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.action_back),
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
                    contentDescription = stringResource(R.string.action_retry),
                    onClick = onRetry,
                    variant = AreIconButtonVariant.Solid,
                    modifier = if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier,
                )
            }
        }
    }
}

/** Builds the Media3 [CaptionStyleCompat] from the user's subtitle-appearance prefs (Phase 2).
 * Foreground = chosen color; edge/background follow the style; typeface from the font choice
 * (Vazirmatn is the bundled face that renders Persian/Arabic subtitles well). */
private fun buildCaptionStyle(
    context: android.content.Context,
    edge: com.arashrahimi46.iptv.data.settings.SubtitleEdge,
    color: com.arashrahimi46.iptv.data.settings.SubtitleColorChoice,
    font: com.arashrahimi46.iptv.data.settings.SubtitleFontChoice,
): CaptionStyleCompat {
    val transparent = android.graphics.Color.TRANSPARENT
    val black = android.graphics.Color.BLACK
    val boxBg = 0xCC000000.toInt()
    val (bg, edgeType) = when (edge) {
        com.arashrahimi46.iptv.data.settings.SubtitleEdge.BOX -> boxBg to CaptionStyleCompat.EDGE_TYPE_NONE
        com.arashrahimi46.iptv.data.settings.SubtitleEdge.OUTLINE -> transparent to CaptionStyleCompat.EDGE_TYPE_OUTLINE
        com.arashrahimi46.iptv.data.settings.SubtitleEdge.SHADOW -> transparent to CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW
    }
    val typeface = when (font) {
        com.arashrahimi46.iptv.data.settings.SubtitleFontChoice.DEFAULT -> null
        com.arashrahimi46.iptv.data.settings.SubtitleFontChoice.SANS -> android.graphics.Typeface.SANS_SERIF
        com.arashrahimi46.iptv.data.settings.SubtitleFontChoice.SERIF -> android.graphics.Typeface.SERIF
        com.arashrahimi46.iptv.data.settings.SubtitleFontChoice.MONO -> android.graphics.Typeface.MONOSPACE
        com.arashrahimi46.iptv.data.settings.SubtitleFontChoice.VAZIRMATN ->
            androidx.core.content.res.ResourcesCompat.getFont(context, R.font.vazirmatn_regular)
    }
    return CaptionStyleCompat(color.argb, bg, transparent, edgeType, black, typeface)
}

/**
 * Video aspect/resize modes offered by the HUD's aspect button, each mapping to a Media3
 * [AspectRatioFrameLayout] resize mode. The choice is persisted (see UserSettings.videoAspectMode)
 * so it survives across streams and app restarts. Three honest modes, each a distinct native
 * behaviour: [FIT] shows the whole picture (letterbox/pillarbox), [FILL] crops to fill the screen
 * with no distortion, [STRETCH] fills by distorting.
 */
enum class AspectMode(val resizeMode: Int, val labelRes: Int) {
    FIT(AspectRatioFrameLayout.RESIZE_MODE_FIT, R.string.player_aspect_fit),
    FILL(AspectRatioFrameLayout.RESIZE_MODE_ZOOM, R.string.player_aspect_fill),
    STRETCH(AspectRatioFrameLayout.RESIZE_MODE_FILL, R.string.player_aspect_stretch);

    fun next(): AspectMode = entries[(ordinal + 1) % entries.size]

    companion object {
        /** Parses a persisted enum name back to a mode, falling back to [FIT] for unknown values. */
        fun fromName(name: String): AspectMode = entries.firstOrNull { it.name == name } ?: FIT
    }
}
