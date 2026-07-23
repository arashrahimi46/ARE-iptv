package com.arashrahimi46.iptv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddToQueue
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeUp
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
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.arashrahimi46.iptv.R
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme

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
) {
    val colors = AreIptvTheme.colors

    // The transport HUD stays LTR in every locale: rewind/play/fast-forward and the seek bar are
    // media-transport conventions, not reading-order layout, so mirroring them (RTL locales) reads
    // wrong. Pin the whole control surface to LTR; subtitle/title TEXT still shapes RTL via bidi.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surfaceGlass, RoundedCornerShape(AreIptvTheme.radius.xl))
            .border(1.dp, colors.borderDefault, RoundedCornerShape(AreIptvTheme.radius.xl))
            .padding(AreIptvTheme.spacing.sp6),
    ) {
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
            Box(Modifier.fillMaxWidth().height(trackHeight).background(colors.surface3, pill))
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

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AreIconButton(Icons.Filled.FastRewind, stringResource(R.string.player_rewind), onClick = onRewind, variant = AreIconButtonVariant.Glass)
            AreIconButton(
                if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                if (playing) stringResource(R.string.player_pause) else stringResource(R.string.player_play),
                onClick = onPlayPause,
                variant = AreIconButtonVariant.Glass,
                size = AreIconButtonSize.Large,
                active = true,
                modifier = if (playPauseFocusRequester != null) Modifier.focusRequester(playPauseFocusRequester) else Modifier,
            )
            AreIconButton(Icons.Filled.FastForward, stringResource(R.string.player_fast_forward), onClick = onFastForward, variant = AreIconButtonVariant.Glass)
            if (onSkipPrevious != null || onSkipNext != null) {
                Box(Modifier.width(1.dp).height(32.dp).background(colors.borderDefault))
                if (onSkipPrevious != null) {
                    AreIconButton(Icons.Filled.SkipPrevious, skipPreviousLabel ?: "", onClick = onSkipPrevious, variant = AreIconButtonVariant.Glass)
                }
                if (onSkipNext != null) {
                    AreIconButton(Icons.Filled.SkipNext, skipNextLabel ?: "", onClick = onSkipNext, variant = AreIconButtonVariant.Glass)
                }
            }
            Box(Modifier.weight(1f))
            // Audio track selection still needs a real track-selector UI -- stays a dimmed,
            // non-focusable placeholder (same treatment as Add Playlist) until built.
            StaticGlyph(Icons.Filled.VolumeUp, stringResource(R.string.player_audio_track))
            // Subtitles: real picker (Off / embedded tracks / online search) once tracks are known;
            // a dimmed placeholder before then (onSubtitles == null). Lit when a track is active.
            if (onSubtitles != null) {
                AreIconButton(
                    Icons.Filled.ClosedCaption,
                    stringResource(R.string.player_subtitles),
                    onClick = onSubtitles,
                    variant = AreIconButtonVariant.Glass,
                    active = subtitlesActive,
                )
            } else {
                StaticGlyph(Icons.Filled.ClosedCaption, stringResource(R.string.player_subtitles))
            }
            // Multi-view button intentionally removed for now -- feature isn't ready. Re-add here
            // (wired to onMultiView) once it ships.
            if (onToggleFavorite != null) {
                AreIconButton(
                    icon = if (isFavorite == true) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = if (isFavorite == true) stringResource(R.string.detail_remove_from_favorites) else stringResource(R.string.detail_add_to_favorites),
                    onClick = onToggleFavorite,
                    variant = AreIconButtonVariant.Glass,
                )
            }
            // PiP + up-next + guide are live-only affordances -- movies/series have no EPG and
            // can't minimize to the corner mini, so drop all three entirely for VOD.
            if (live) {
                // ● REC toggle (Live TV Recording V1). Red dot while capturing, amber while
                // reconnecting, plain when idle+recordable; a dimmed placeholder for non-.ts streams.
                if (onToggleRecord != null) {
                    AreIconButton(
                        Icons.Filled.FiberManualRecord,
                        contentDescription = if (recordingActive) stringResource(R.string.recording_stop) else stringResource(R.string.recording_start),
                        onClick = onToggleRecord,
                        variant = AreIconButtonVariant.Glass,
                        contentTint = when {
                            recordingReconnecting -> colors.warning
                            recordingActive -> colors.danger
                            else -> null
                        },
                    )
                } else {
                    StaticGlyph(Icons.Filled.FiberManualRecord, stringResource(R.string.recording_not_supported))
                }
                // Live: a real button that minimizes to the in-app corner mini-player. Dimmed
                // placeholder only when the controller isn't ready (onPictureInPicture == null).
                if (onPictureInPicture != null) {
                    AreIconButton(Icons.Filled.PictureInPicture, stringResource(R.string.player_minimize_to_corner), onClick = onPictureInPicture, variant = AreIconButtonVariant.Glass)
                } else {
                    StaticGlyph(Icons.Filled.PictureInPicture, stringResource(R.string.player_mini_player))
                }
                // Add this live channel to multi-view (live-only -- movies/series have no place there).
                if (onAddToMultiView != null) {
                    AreIconButton(Icons.Filled.AddToQueue, stringResource(R.string.multiview_add_to), onClick = onAddToMultiView, variant = AreIconButtonVariant.Glass)
                }
                // Mini up-next list scoped to the currently-playing channel -- distinct from "Open
                // guide" (which leaves the player for the full multi-channel TV Guide).
                AreIconButton(Icons.Filled.Schedule, stringResource(R.string.player_up_next_action), onClick = onUpNext, variant = AreIconButtonVariant.Glass)
                AreIconButton(Icons.Filled.GridView, stringResource(R.string.player_open_guide), onClick = onOpenGuide, variant = AreIconButtonVariant.Glass)
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

@Preview(widthDp = 1400, heightDp = 260, showBackground = true, backgroundColor = 0xFF06070A)
@Composable
private fun ArePlayerControlsPreview() {
    AreIptvTheme {
        Box(Modifier.padding(24.dp)) {
            ArePlayerControls(title = "Sky Sports Main Event", subtitle = "Premier League: Arsenal vs Chelsea")
        }
    }
}
