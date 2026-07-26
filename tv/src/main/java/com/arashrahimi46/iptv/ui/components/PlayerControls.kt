package com.arashrahimi46.iptv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddToQueue
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.arashrahimi46.iptv.R
import com.arashrahimi46.iptv.ui.player.DEFAULT_HUD_LAYOUT
import com.arashrahimi46.iptv.ui.player.HudControl
import com.arashrahimi46.iptv.ui.player.HudGroup
import com.arashrahimi46.iptv.ui.player.HudSlot
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.theme.Ink950
import com.arashrahimi46.iptv.ui.theme.ProvideOnGlass
import com.arashrahimi46.iptv.ui.theme.TvFocusable
import com.arashrahimi46.iptv.ui.theme.glassSurface
import com.arashrahimi46.iptv.ui.theme.glassTrack

/**
 * PlayerControls — glass transport HUD overlaid on live video / VOD
 * (PlayerControls.jsx). Shows program title + now/next, a TimeShift-aware
 * seek bar (buffered region vs live edge), transport buttons and quick actions.
 */
@Composable
fun ArePlayerControls(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    /** Live only: the playing stream's real resolution + frame rate (e.g. "1080p · 50fps"), read
     * from Media3's decoded video Format. Null hides the tag (VOD, or before the format is known). */
    streamInfo: String? = null,
    live: Boolean = true,
    /** Catch-up/archive playback: shows a ⟲ CATCH-UP pill in place of the LIVE badge (the stream is a
     * rewound live channel, played seekably). See docs/catchup-v1-design.md. */
    catchup: Boolean = false,
    /** D7: jumps from catch-up to the live edge (plays the underlying live channel). Null hides the
     * button -- only present during catch-up playback. */
    onGoLive: (() -> Unit)? = null,
    playing: Boolean = true,
    position: Float = 0.62f,
    buffered: Float = 0.8f,
    elapsed: String = "20:28",
    total: String = "20:45",
    channelLogoInitials: String = "SKY",
    onPlayPause: () -> Unit = {},
    onRewind: () -> Unit = {},
    onFastForward: () -> Unit = {},
    // Transport skip pair (⏮/⏭), context-dependent: previous/next channel (live), previous/next
    // episode (series), or −10min/+10min (movie). Null hides that side -- e.g. a series' first/last
    // episode. Labels carry the a11y meaning since the icon is the same across contexts.
    onSkipPrevious: (() -> Unit)? = null,
    onSkipNext: (() -> Unit)? = null,
    skipPreviousLabel: String? = null,
    skipNextLabel: String? = null,
    onOpenGuide: () -> Unit = {},
    onUpNext: () -> Unit = {},
    // Multi-view is temporarily disabled (feature not ready) -- the param stays so the button
    // can be re-enabled later without re-plumbing; it is simply not rendered for now.
    onMultiView: () -> Unit = {},
    /** Adds the current live channel to the multi-view list. Null for VOD (movies/series have no
     * place in multi-view) -- the button is then not rendered. */
    onAddToMultiView: (() -> Unit)? = null,
    /** Favorite state of whatever is playing; null hides the favorite affordance entirely. */
    isFavorite: Boolean? = null,
    onToggleFavorite: (() -> Unit)? = null,
    /** Minimizes the fullscreen player to the in-app corner mini-player (live channels only).
     * Null for VOD / when unavailable -- the PiP glyph stays a dimmed, non-focusable placeholder. */
    onPictureInPicture: (() -> Unit)? = null,
    /** Opens the subtitle picker (Off / embedded tracks / online search). Null keeps the CC glyph
     * a dimmed placeholder (e.g. before a stream's tracks are known). */
    onSubtitles: (() -> Unit)? = null,
    /** True when a subtitle track is currently active -- lights the CC button so the user can see subtitles are on. */
    subtitlesActive: Boolean = false,
    /** Opens the audio-track picker; null keeps the glyph a dimmed placeholder (e.g. a stream with a
     * single audio track, where there's nothing to pick). */
    onAudioTrack: (() -> Unit)? = null,
    /** True when the user has forced a non-default audio track -- lights the button. */
    audioTrackActive: Boolean = false,
    /** Opens the audio-delay ("Audio sync") stepper; null keeps the button hidden. */
    onAudioDelay: (() -> Unit)? = null,
    /** The active offset rendered on the audio-delay button face (e.g. "+150 ms"); null when in sync. */
    audioDelayLabel: String? = null,
    /** Cycles the video aspect/resize mode (Fit -> Fill -> Stretch); null keeps a dimmed placeholder. */
    onAspectRatio: (() -> Unit)? = null,
    /** Opens the playback-speed picker (VOD/recordings only); null hides the button (e.g. live). */
    onPlaybackSpeed: (() -> Unit)? = null,
    /** The active rate rendered on the speed button face (e.g. "1.5×"); null shows the generic label. */
    playbackSpeedLabel: String? = null,
    /** Live TV Recording (V1): toggles record for the current channel. Null => the stream isn't
     * recordable (non-`.ts`) -> a dimmed placeholder with a "not supported" hint. */
    onToggleRecord: (() -> Unit)? = null,
    /** True while a recording is capturing -- lights the REC dot red ("the red dot never lies"). */
    recordingActive: Boolean = false,
    /** True while capture is reconnecting after a stall -- amber dot instead of red. */
    recordingReconnecting: Boolean = false,
    // When set, the play/pause button carries this requester so the screen can move
    // focus straight onto it when the panel is opened.
    playPauseFocusRequester: FocusRequester? = null,
    // VOD only: when non-null the progress bar becomes a focusable scrub control -- it carries this
    // requester (so the screen can focus it) and renders a draggable thumb at the current position
    // while focused. Null (live) keeps the bar display-only. Left/Right seeking itself is handled by
    // the screen's D-pad model while this bar holds focus.
    seekBarFocusRequester: FocusRequester? = null,
    onSeekBarFocusChanged: (Boolean) -> Unit = {},
    /** User-customized HUD button order + per-button visibility (see [HudSlot]). Defaults to the
     * canonical order; locked core transport (rewind/play-pause/FF) always renders regardless. */
    slots: List<HudSlot> = DEFAULT_HUD_LAYOUT,
) {
    val colors = AreIptvTheme.colors

    // The transport HUD stays LTR in every locale: rewind/play/fast-forward and the seek bar are
    // media-transport conventions, not reading-order layout, so mirroring them (RTL locales) reads
    // wrong. Pin the whole control surface to LTR; subtitle/title TEXT still shapes RTL via bidi.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            // Elevated glass: denser fill + the lit-edge gradient border so text stays legible over
            // busy video (design option B — Cinematic Unified). Real blur is impossible here (video
            // is a SurfaceView); the translucent fill carries the glass look.
            .glassSurface(RoundedCornerShape(AreIptvTheme.radius.xl), elevated = true)
            .padding(AreIptvTheme.spacing.sp6),
    ) {
        // The HUD bar is a glass surface, so its controls are nested children (§6): every icon button
        // resolves to tint + hairline instead of laying a second glass fill on the bar (the opaque-
        // square defect). The provider logo well below stays a solid dark plate on purpose -- it sits
        // over arbitrary video, not over the glass, so it keeps its own explicit surfaceOverlay fill.
        ProvideOnGlass {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(colors.surfaceOverlay, RoundedCornerShape(AreIptvTheme.radius.sm))
                    .border(1.dp, colors.borderDefault, RoundedCornerShape(AreIptvTheme.radius.sm)),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = channelLogoInitials, style = AreIptvTheme.typography.label, color = colors.textPrimary)
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (live) {
                        AreBadge(stringResource(R.string.badge_live), tone = AreBadgeTone.Live)
                    }
                    if (catchup) {
                        AreBadge("⟲ ${stringResource(R.string.badge_catchup)}", tone = AreBadgeTone.Catchup)
                    }
                    Text(
                        text = title,
                        style = AreIptvTheme.typography.h3,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (subtitle != null) {
                    Text(text = subtitle, style = AreIptvTheme.typography.caption, color = colors.textSecondary)
                }
                if (streamInfo != null) {
                    Text(
                        text = streamInfo,
                        style = AreIptvTheme.typography.mono,
                        color = colors.accentHover,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .background(colors.accentWash, RoundedCornerShape(AreIptvTheme.radius.xs))
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                    )
                }
            }
            // Live has no fixed duration -- an HLS DVR window reports a rolling "total" that reads
            // as a meaningless 0:33 / 1:00 countdown. Only VOD (movies/series) shows the time label.
            if (!live) {
                Text(
                    text = "$elapsed / $total",
                    style = AreIptvTheme.typography.mono,
                    color = colors.textSecondary,
                )
            }
        }

        Box(Modifier.height(AreIptvTheme.spacing.sp4))

        // The progress/seek bar is likewise VOD-only: for live there's nothing meaningful to fill.
        if (!live) {
        // TimeShift / VOD seek bar. On VOD (seekBarFocusRequester != null) it's a focusable scrub
        // control: while focused the track thickens and a thumb rides the end of the blue fill so
        // the user sees exactly where a Left/Right seek will land.
        var barFocused by remember { mutableStateOf(false) }
        val pill = RoundedCornerShape(AreIptvTheme.radius.pill)
        val interactive = seekBarFocusRequester != null
        val trackHeight = if (barFocused) 8.dp else 6.dp
        val pos = position.coerceIn(0f, 1f)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                // Reserve room for the thumb so the row height doesn't jump when it appears.
                .height(20.dp)
                // Selected state: an accent wash + ring around the whole bar so arriving from the
                // button row (Up) reads unmistakably as "the scrub bar is now selected" -- not just
                // the thin thumb.
                .then(
                    if (barFocused) {
                        Modifier
                            .background(colors.accentWash, pill)
                            .border(1.dp, colors.accent, pill)
                    } else {
                        Modifier
                    },
                )
                .then(
                    if (interactive) {
                        Modifier
                            .focusRequester(seekBarFocusRequester!!)
                            .onFocusChanged {
                                barFocused = it.isFocused
                                onSeekBarFocusChanged(it.isFocused)
                            }
                            .focusable()
                    } else {
                        Modifier
                    },
                ),
            contentAlignment = Alignment.CenterStart,
        ) {
            // The empty rail is nested in the glass HUD, so it's glassTrack (tint + lit edge), not
            // an opaque surface3 slug punched through the panel (V2 §6). The buffered/accent fills
            // below keep their solid colours -- they're the meaningful, high-contrast layer.
            Box(Modifier.fillMaxWidth().height(trackHeight).glassTrack(pill))
            Box(
                Modifier
                    .fillMaxWidth(buffered.coerceIn(0f, 1f))
                    .height(trackHeight)
                    .background(colors.borderStrong, pill),
            )
            Box(
                Modifier
                    .fillMaxWidth(pos)
                    .height(trackHeight)
                    .background(colors.accent, pill),
            )
            // Thumb at the end of the blue fill -- only while the bar is focused (selected).
            if (barFocused) {
                Box(Modifier.fillMaxWidth(pos).fillMaxHeight(), contentAlignment = Alignment.CenterEnd) {
                    Box(
                        Modifier
                            .size(16.dp)
                            .background(colors.accent, CircleShape)
                            .border(2.dp, colors.textPrimary, CircleShape),
                    )
                }
            }
        }

        Box(Modifier.height(AreIptvTheme.spacing.sp4))
        }

        // Block C — the reorderable button row. Each control renders only when context allows it
        // (nullable callbacks, live vs VOD); on top of that, the user's [slots] decide order and
        // per-button hide. Locked core transport (rewind/play-pause/FF) always renders, first.
        // A single glyph renderer maps a control id to its existing button, invoked per ordered slot.
        val renderable = fun(c: HudControl): Boolean = when (c) {
            HudControl.REWIND, HudControl.PLAY_PAUSE, HudControl.FAST_FORWARD -> true
            HudControl.SKIP_PREVIOUS -> onSkipPrevious != null
            HudControl.SKIP_NEXT -> onSkipNext != null
            HudControl.GO_LIVE -> onGoLive != null
            HudControl.PLAYBACK_SPEED -> onPlaybackSpeed != null
            HudControl.ASPECT_RATIO, HudControl.AUDIO_TRACK, HudControl.SUBTITLES -> true
            HudControl.AUDIO_DELAY -> onAudioDelay != null
            HudControl.FAVORITE -> onToggleFavorite != null
            HudControl.RECORD, HudControl.PICTURE_IN_PICTURE, HudControl.UP_NEXT, HudControl.OPEN_GUIDE -> live
            HudControl.ADD_MULTIVIEW -> live && onAddToMultiView != null
        }

        @Composable
        fun HudButton(c: HudControl) {
            when (c) {
                HudControl.REWIND ->
                    AreIconButton(Icons.Filled.FastRewind, stringResource(R.string.player_rewind), onClick = onRewind, variant = AreIconButtonVariant.Glass)
                HudControl.PLAY_PAUSE ->
                    AreIconButton(
                        if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        if (playing) stringResource(R.string.player_pause) else stringResource(R.string.player_play),
                        onClick = onPlayPause,
                        variant = AreIconButtonVariant.Glass,
                        active = true,
                        modifier = if (playPauseFocusRequester != null) Modifier.focusRequester(playPauseFocusRequester) else Modifier,
                    )
                HudControl.FAST_FORWARD ->
                    AreIconButton(Icons.Filled.FastForward, stringResource(R.string.player_fast_forward), onClick = onFastForward, variant = AreIconButtonVariant.Glass)
                HudControl.SKIP_PREVIOUS -> onSkipPrevious?.let {
                    AreIconButton(Icons.Filled.SkipPrevious, skipPreviousLabel ?: "", onClick = it, variant = AreIconButtonVariant.Glass)
                }
                HudControl.SKIP_NEXT -> onSkipNext?.let {
                    AreIconButton(Icons.Filled.SkipNext, skipNextLabel ?: "", onClick = it, variant = AreIconButtonVariant.Glass)
                }
                // Catch-up only (D7): leaves the archive and jumps to the live edge.
                HudControl.GO_LIVE -> onGoLive?.let {
                    AreIconButton(Icons.Filled.LiveTv, stringResource(R.string.guide_catchup_go_live), onClick = it, variant = AreIconButtonVariant.Glass)
                }
                // Speed: VOD/recordings only. Face shows the active rate once non-default.
                HudControl.PLAYBACK_SPEED -> onPlaybackSpeed?.let {
                    AreIconButton(Icons.Filled.Speed, playbackSpeedLabel ?: stringResource(R.string.player_playback_speed), onClick = it, variant = AreIconButtonVariant.Glass, active = playbackSpeedLabel != null)
                }
                // Aspect: cycles Fit->Fill->Stretch; dimmed placeholder until the player can report a mode.
                HudControl.ASPECT_RATIO ->
                    if (onAspectRatio != null) AreIconButton(Icons.Filled.AspectRatio, stringResource(R.string.player_aspect_ratio), onClick = onAspectRatio, variant = AreIconButtonVariant.Glass)
                    else StaticGlyph(Icons.Filled.AspectRatio, stringResource(R.string.player_aspect_ratio))
                // Audio track: real picker when >1 track; dimmed placeholder otherwise. Lit when forced.
                HudControl.AUDIO_TRACK ->
                    if (onAudioTrack != null) AreIconButton(Icons.Filled.Audiotrack, stringResource(R.string.player_audio_track), onClick = onAudioTrack, variant = AreIconButtonVariant.Glass, active = audioTrackActive)
                    else StaticGlyph(Icons.Filled.Audiotrack, stringResource(R.string.player_audio_track))
                // Audio sync: ±ms delay stepper. Face shows the active offset; lit when set.
                HudControl.AUDIO_DELAY -> onAudioDelay?.let {
                    AreIconButton(Icons.Filled.Sync, audioDelayLabel ?: stringResource(R.string.player_audio_delay), onClick = it, variant = AreIconButtonVariant.Glass, active = audioDelayLabel != null)
                }
                // Subtitles: real picker once tracks are known; dimmed placeholder before then. Lit when active.
                HudControl.SUBTITLES ->
                    if (onSubtitles != null) AreIconButton(Icons.Filled.ClosedCaption, stringResource(R.string.player_subtitles), onClick = onSubtitles, variant = AreIconButtonVariant.Glass, active = subtitlesActive)
                    else StaticGlyph(Icons.Filled.ClosedCaption, stringResource(R.string.player_subtitles))
                HudControl.FAVORITE -> onToggleFavorite?.let {
                    AreIconButton(
                        icon = if (isFavorite == true) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = if (isFavorite == true) stringResource(R.string.detail_remove_from_favorites) else stringResource(R.string.detail_add_to_favorites),
                        onClick = it,
                        variant = AreIconButtonVariant.Glass,
                    )
                }
                // ● REC (Live TV Recording V1). Red while capturing, amber while reconnecting; a
                // focusable dimmed hint glyph for non-.ts streams explaining WHY it's unavailable.
                HudControl.RECORD ->
                    if (onToggleRecord != null) AreIconButton(
                        Icons.Filled.FiberManualRecord,
                        contentDescription = if (recordingActive) stringResource(R.string.recording_stop) else stringResource(R.string.recording_start),
                        onClick = onToggleRecord,
                        variant = AreIconButtonVariant.Glass,
                        contentTint = when {
                            recordingReconnecting -> colors.warning
                            recordingActive -> colors.danger
                            else -> null
                        },
                    ) else DisabledHintGlyph(Icons.Filled.FiberManualRecord, stringResource(R.string.recording_not_supported))
                // Minimize to the corner mini-player; dimmed placeholder when the controller isn't ready.
                HudControl.PICTURE_IN_PICTURE ->
                    if (onPictureInPicture != null) AreIconButton(Icons.Filled.PictureInPicture, stringResource(R.string.player_minimize_to_corner), onClick = onPictureInPicture, variant = AreIconButtonVariant.Glass)
                    else StaticGlyph(Icons.Filled.PictureInPicture, stringResource(R.string.player_mini_player))
                HudControl.ADD_MULTIVIEW -> onAddToMultiView?.let {
                    AreIconButton(Icons.Filled.AddToQueue, stringResource(R.string.multiview_add_to), onClick = it, variant = AreIconButtonVariant.Glass)
                }
                HudControl.UP_NEXT ->
                    AreIconButton(Icons.Filled.Schedule, stringResource(R.string.player_up_next_action), onClick = onUpNext, variant = AreIconButtonVariant.Glass)
                HudControl.OPEN_GUIDE ->
                    AreIconButton(Icons.Filled.GridView, stringResource(R.string.player_open_guide), onClick = onOpenGuide, variant = AreIconButtonVariant.Glass)
            }
        }

        // Resolve the ordered, visible, context-available controls per cluster. Locked controls are
        // always shown and pinned to their default order (they can't be moved/hidden in the editor).
        val shown = slots.filter { (it.control.locked || it.visible) && renderable(it.control) }
        val transportCore = shown.filter { it.control.group == HudGroup.TRANSPORT && it.control.locked }
            .sortedBy { it.control.order }
        val transportExtra = shown.filter { it.control.group == HudGroup.TRANSPORT && !it.control.locked }
        val utilities = shown.filter { it.control.group == HudGroup.UTILITIES }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            transportCore.forEach { HudButton(it.control) }
            if (transportExtra.isNotEmpty()) {
                Box(Modifier.width(1.dp).height(32.dp).background(colors.borderDefault))
                transportExtra.forEach { HudButton(it.control) }
            }
            Box(Modifier.weight(1f))
            utilities.forEach { HudButton(it.control) }
        }
        }
    }
    }
}

/** A dimmed, non-interactive glyph -- same footprint as [AreIconButton] but deliberately outside
 * the focus chain, for HUD actions not built yet (see call sites' comments for why). */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun StaticGlyph(icon: androidx.compose.ui.graphics.vector.ImageVector, contentDescription: String) {
    val colors = AreIptvTheme.colors
    Box(modifier = Modifier.size(52.dp), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = contentDescription, tint = colors.textTertiary, modifier = Modifier.size(24.dp))
    }
}

/** A dimmed but FOCUSABLE glyph that pops a one-line [hint] above itself while focused. For an
 * affordance that's disabled for a real reason the user should understand (vs. not-built-yet), so
 * D-pad landing on it explains the "why" instead of reading as a broken control. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun DisabledHintGlyph(icon: androidx.compose.ui.graphics.vector.ImageVector, hint: String) {
    val colors = AreIptvTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Box(contentAlignment = Alignment.Center) {
        TvFocusable(
            onClick = {},
            interactionSource = interaction,
            modifier = Modifier.size(52.dp),
            shape = CircleShape,
        ) { _, _ ->
            Box(Modifier.fillMaxHeight().fillMaxWidth(), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = hint, tint = colors.textTertiary, modifier = Modifier.size(24.dp))
            }
        }
        if (focused) {
            // Tooltip lifted above the button (offset in px; the glyph is 52dp tall).
            Popup(alignment = Alignment.TopCenter, offset = IntOffset(0, -150)) {
                Box(
                    modifier = Modifier
                        .background(Ink950.copy(alpha = 0.94f), RoundedCornerShape(AreIptvTheme.radius.md))
                        .border(1.dp, colors.borderDefault, RoundedCornerShape(AreIptvTheme.radius.md))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .widthIn(max = 320.dp),
                ) {
                    Text(text = hint, style = AreIptvTheme.typography.caption, color = Color.White)
                }
            }
        }
    }
}

@Preview(widthDp = 1400, heightDp = 260, showBackground = true, backgroundColor = 0xFF06070A)
@Composable
private fun ArePlayerControlsPreview() {
    AreIptvTheme {
        Box(Modifier.padding(24.dp)) {
            ArePlayerControls(title = "Sky Sports Main Event", subtitle = "Premier League: Arsenal vs Chelsea")
        }
    }
}
