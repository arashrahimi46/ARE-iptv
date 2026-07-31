package com.arashrahimi46.iptv.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.arashrahimi46.iptv.core.R as CoreR
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.theme.ProvideOnGlass

/**
 * The player HUD, drawn as a full-screen OVERLAY on the video -- not a slab. Two soft dark scrims
 * (top and bottom) keep the controls legible while leaving the picture visible between them:
 *
 *   - top bar: back + title/subtitle + Picture-in-Picture, over the top scrim;
 *   - centre: the transport cluster (skip / large play-pause / skip), floating over the picture;
 *   - bottom: the seek row (or a LIVE badge for a live stream) and up to five secondary actions,
 *     over the bottom scrim.
 *
 * A video scrim is always dark regardless of app theme -- the picture behind it is the background --
 * so the gradients use black directly rather than a theme surface token; the CONTROLS resolve their
 * colours [ProvideOnGlass] so glyph/text contrast stays correct.
 *
 * Visibility is the CALLER's: this composable draws, the screen decides when (and fades it).
 * Gestures live in `PlayerScreen.kt`, never here.
 *
 * Cut from the TV build's 45 parameters to 24. The HUD layout editor, multiview, the up-next card
 * and the record toggle are all gone; anything past five secondary actions belongs behind [onMore]
 * in a sheet, not a horizontally scrolling row of thirteen buttons.
 */
@Composable
fun ArePlayerControls(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    live: Boolean = false,
    playing: Boolean = true,
    buffering: Boolean = false,
    position: () -> Long = { 0L },
    duration: () -> Long = { 0L },
    buffered: () -> Long = { 0L },
    onSeek: (Long) -> Unit = {},
    onSeeking: (Long) -> Unit = {},
    onPlayPause: () -> Unit = {},
    onBack: () -> Unit = {},
    onGoLive: (() -> Unit)? = null,
    onPrevious: (() -> Unit)? = null,
    onNext: (() -> Unit)? = null,
    onSubtitles: (() -> Unit)? = null,
    subtitlesActive: Boolean = false,
    onAudioTrack: (() -> Unit)? = null,
    onPlaybackSpeed: (() -> Unit)? = null,
    playbackSpeedLabel: String? = null,
    onAspectRatio: (() -> Unit)? = null,
    onPictureInPicture: (() -> Unit)? = null,
    onMore: (() -> Unit)? = null,
    /** When false, only the scrims and the top bar (back/title/PiP) render. The screen sets this
     * while it is showing its own centred loading spinner or error card, so the transport cluster and
     * seek row don't collide with them -- while keeping the back button reachable throughout. */
    transport: Boolean = true,
) {
    val colors = AreIptvTheme.colors

    ProvideOnGlass(true) {
        Box(modifier = modifier.fillMaxSize()) {
            // ---- scrims: dark at the edges, clear in the middle so the picture reads through ----
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.72f),
                            1f to Color.Transparent,
                        ),
                    ),
            )
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.82f),
                        ),
                    ),
            )

            // ---- top bar: back + title + PiP ----
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .safeDrawingPadding()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                AreIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(CoreR.string.action_back),
                    onClick = onBack,
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = AreIptvTheme.typography.h3,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = AreIptvTheme.typography.caption,
                            color = colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (onPictureInPicture != null) {
                    AreIconButton(
                        icon = Icons.Filled.PictureInPictureAlt,
                        contentDescription = stringResource(CoreR.string.player_mini_player),
                        onClick = onPictureInPicture,
                    )
                }
            }

            // ---- centre: the transport cluster. LTR by convention even in RTL locales -- "back" is
            // the left side of a timeline, not the start of a sentence. Only THIS row is forced. ----
            if (transport) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterHorizontally),
                ) {
                    if (onPrevious != null) {
                        AreIconButton(
                            icon = Icons.Filled.SkipPrevious,
                            contentDescription = stringResource(CoreR.string.hud_ctl_skip_previous),
                            onClick = onPrevious,
                            size = AreIconButtonSize.Large,
                            contentTint = Color.White,
                        )
                    }
                    TransportButton(playing = playing, buffering = buffering, onPlayPause = onPlayPause)
                    if (onNext != null) {
                        AreIconButton(
                            icon = Icons.Filled.SkipNext,
                            contentDescription = stringResource(CoreR.string.hud_ctl_skip_next),
                            onClick = onNext,
                            size = AreIconButtonSize.Large,
                            contentTint = Color.White,
                        )
                    }
                }
            }

            // ---- bottom: seek row (or LIVE) + secondary actions ----
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .safeDrawingPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                // A live stream has no meaningful timeline for the viewer -- an HLS window still
                // reports a finite duration (its DVR buffer), so keying off `duration == 0` wrongly
                // showed a 0:00-0:24 scrubber on live channels. Trust the caller's [live] flag.
                if (live) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        AreBadge(stringResource(CoreR.string.player_live_badge), tone = AreBadgeTone.Live, glow = true)
                        Spacer(Modifier.weight(1f))
                        if (onGoLive != null) {
                            AreButton(
                                text = stringResource(CoreR.string.player_go_live),
                                onClick = onGoLive,
                                variant = AreButtonVariant.Secondary,
                                size = AreButtonSize.Small,
                            )
                        }
                    }
                } else {
                    SeekRow(
                        position = position,
                        duration = duration(),
                        buffered = buffered,
                        onSeek = onSeek,
                        onSeeking = onSeeking,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (onSubtitles != null) {
                        AreIconButton(
                            icon = Icons.Filled.ClosedCaption,
                            contentDescription = stringResource(CoreR.string.player_subtitles),
                            onClick = onSubtitles,
                            active = subtitlesActive,
                        )
                    }
                    if (onAudioTrack != null) {
                        AreIconButton(
                            icon = Icons.Filled.Audiotrack,
                            contentDescription = stringResource(CoreR.string.player_audio_track),
                            onClick = onAudioTrack,
                        )
                    }
                    if (onPlaybackSpeed != null) {
                        AreIconButton(
                            icon = Icons.Filled.Speed,
                            contentDescription = playbackSpeedLabel
                                ?: stringResource(CoreR.string.player_playback_speed),
                            onClick = onPlaybackSpeed,
                            active = playbackSpeedLabel != null,
                        )
                    }
                    if (onAspectRatio != null) {
                        AreIconButton(
                            icon = Icons.Filled.AspectRatio,
                            contentDescription = stringResource(CoreR.string.player_aspect_ratio),
                            onClick = onAspectRatio,
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    if (onMore != null) {
                        AreIconButton(
                            icon = Icons.Filled.MoreVert,
                            contentDescription = stringResource(CoreR.string.common_more),
                            onClick = onMore,
                        )
                    }
                }
            }
            }
        }
    }
}

/**
 * The one large affordance on the screen: a 76dp translucent disc with a 40dp glyph, swapping to a
 * spinner while the pipeline buffers. A plain translucent fill (not a glass backdrop-sample) keeps it
 * cheap and unmistakable over any frame.
 */
@Composable
private fun TransportButton(playing: Boolean, buffering: Boolean, onPlayPause: () -> Unit) {
    if (buffering) {
        Box(Modifier.size(76.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                modifier = Modifier.size(44.dp),
                color = Color.White,
                strokeWidth = 3.dp,
            )
        }
    } else {
        // A frosted light disc so the primary control reads over both a dark and a bright frame --
        // a dark disc vanished against the letterboxed black bars above and below the picture.
        Box(
            Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            AreIconButton(
                icon = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (playing) {
                    stringResource(CoreR.string.player_pause)
                } else {
                    stringResource(CoreR.string.player_play)
                },
                onClick = onPlayPause,
                variant = AreIconButtonVariant.Ghost,
                size = AreIconButtonSize.Large,
                contentTint = Color.White,
            )
        }
    }
}

/**
 * elapsed -- draggable [Slider] -- duration.
 *
 * The slider owns the position WHILE dragging (`scrub`), so the player's own clock can keep ticking
 * underneath without yanking the thumb out from under the finger. `onValueChange` previews via
 * [onSeeking]; only `onValueChangeFinished` commits through [onSeek].
 */
@Composable
private fun SeekRow(
    position: () -> Long,
    duration: Long,
    buffered: () -> Long,
    onSeek: (Long) -> Unit,
    onSeeking: (Long) -> Unit,
) {
    val colors = AreIptvTheme.colors
    var scrubbing by remember { mutableStateOf(false) }
    var scrub by remember { mutableFloatStateOf(0f) }
    val positionMs = position()
    val value = if (scrubbing) scrub else positionMs.toFloat()
    val max = duration.coerceAtLeast(1L).toFloat()

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = formatClock(value.toLong()),
            style = AreIptvTheme.typography.mono,
            color = colors.textPrimary,
            modifier = Modifier.widthIn(min = 52.dp),
        )
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            // Buffered head, drawn UNDER the slider: Material's slider has one track, and a
            // "how much is downloaded" cue is the thing a phone viewer on cellular actually wants.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(AreIptvTheme.radius.pill))
                    .background(colors.glassTrackTint),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth((buffered().toFloat() / max).coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(colors.textTertiary.copy(alpha = 0.55f)),
                )
            }
            Slider(
                value = value.coerceIn(0f, max),
                onValueChange = {
                    scrubbing = true
                    scrub = it
                    onSeeking(it.toLong())
                },
                onValueChangeFinished = {
                    scrubbing = false
                    onSeek(scrub.toLong())
                },
                valueRange = 0f..max,
                colors = SliderDefaults.colors(
                    thumbColor = colors.accent,
                    activeTrackColor = colors.accent,
                    inactiveTrackColor = Color.Transparent,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Text(
            text = formatClock(duration),
            style = AreIptvTheme.typography.mono,
            color = colors.textSecondary,
            modifier = Modifier.widthIn(min = 52.dp),
        )
    }
}

/** m:ss, or h:mm:ss past an hour. */
private fun formatClock(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
