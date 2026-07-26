package com.arashrahimi46.iptv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import com.arashrahimi46.iptv.R
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.theme.LocalAmbientArtwork
import com.arashrahimi46.iptv.ui.theme.TvFocusable
import com.arashrahimi46.iptv.ui.theme.glassBorderBrush
import com.arashrahimi46.iptv.ui.theme.glassChild
import com.arashrahimi46.iptv.ui.theme.rememberTileWashHue
import com.arashrahimi46.iptv.ui.theme.sampleTileWashHue
import com.arashrahimi46.iptv.ui.theme.tileWash

/**
 * ChannelTile — logo-first live-TV tile (ChannelTile.jsx). IPTV providers
 * can't reliably serve stream previews, so the tile leads with a logo/initials
 * chip over an in-card info panel: now-playing program + progress, next up,
 * and quality/health info.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AreChannelTile(
    channel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    logoUrl: String? = null,
    number: String? = null,
    now: String? = null,
    next: String? = null,
    health: AreStreamHealthLevel = AreStreamHealthLevel.Stable,
    quality: String? = null,
    codec: String? = null,
    catchup: Boolean = false,
    width: Dp = AreIptvTheme.spacing.tileLandWidth,
    /** Grid mode: fill the cell width (set by [GridCells.Adaptive]) instead of a fixed [width], so the
     * live grid packs a responsive number of columns rather than one fixed-width tile per cell. */
    fillWidth: Boolean = false,
    /** Null hides the favorite affordance entirely -- only screens wired to real favorites persistence pass this. */
    isFavorite: Boolean? = null,
    onToggleFavorite: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    /** Category name used to decide whether this tile is blurred under the parental "blur" mode
     * (see [LocalParentalBlur]); null opts out. */
    lockCategory: String? = null,
) {
    val colors = AreIptvTheme.colors
    val shape = RoundedCornerShape(AreIptvTheme.radius.md)
    val washHue = rememberTileWashHue(logoUrl, seed = channel)
    val ambientArtwork = LocalAmbientArtwork.current
    val blur = LocalParentalBlur.current
    val obscured = blur.isObscured(lockCategory)
    // remember-ed, and the Regex is a file-level val: this compiled a fresh Regex and built ~4
    // intermediate collections per tile per composition -- 40 visible tiles on a live grid, re-run on
    // every focus move.
    val initials = remember(channel) {
        HdSuffix.replace(channel, "")
            .split(" ").take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")
    }
    val healthColor = when (health) {
        AreStreamHealthLevel.Stable -> colors.healthStable
        AreStreamHealthLevel.Moderate -> colors.healthModerate
        AreStreamHealthLevel.Poor -> colors.healthPoor
    }

    TvFocusable(
        onClick = if (obscured) blur.onReveal else onClick,
        modifier = modifier.then(if (fillWidth) Modifier.fillMaxWidth() else Modifier.width(width)),
        interactionSource = interactionSource,
        shape = shape,
        backgroundColor = colors.surfaceGlass,
        // The lit-edge gradient gives the unfocused tile a distinct edge on the off-white
        // light-theme page (replacing the old solid border).
        borderBrush = glassBorderBrush(),
    ) { focused, _ ->
      // Publish this tile's logo as the page's ambient artwork on focus gain. Fires only on the
      // transition, never per recomposition, and never clears on focus loss -- the next focused
      // tile overwrites it, so a fast D-pad move doesn't flicker back to the empty state.
      //
      // PERF: debounced until focus SETTLES. Each publish costs a full-screen Coil decode plus a
      // `blur(72.dp)` RenderEffect over the whole 1080p layer in AmbientBackdrop -- and because that
      // layer is the Backdrop every glass surface samples, it also invalidates all of them. Publishing
      // on every focus transition meant one of those per D-pad step, so HOLDING the D-pad queued a
      // full-screen decode + blur per tile travelled through, all of them discarded. The delay is
      // cancelled by the next focus change (LaunchedEffect re-keys), so a sweep across twelve tiles
      // now costs ONE decode instead of twelve.
      LaunchedEffect(focused) {
          if (focused && logoUrl != null) {
              delay(AmbientArtworkSettleMs)
              ambientArtwork.value = logoUrl
          }
      }
      Box(Modifier.fillMaxWidth().clip(shape)) {
        // Clip the content to the tile shape so the info panel's square surface1 background
        // doesn't poke square corners through the rounded focus ring.
        Column(Modifier.fillMaxWidth().then(if (obscured) Modifier.blur(16.dp) else Modifier)) {
            // logo zone
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 8f)
                    // Two-stop wash in the logo's own dominant hue (§6.3) -- gives the tile's glass
                    // something to refract. Rectangle here; the outer Box already clips to `shape`.
                    .tileWash(RectangleShape, washHue),
            ) {
                Row(
                    modifier = Modifier.align(Alignment.TopStart).padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    AreBadge(stringResource(R.string.badge_live), tone = AreBadgeTone.Live)
                    if (catchup) AreBadge(stringResource(R.string.badge_catchup), tone = AreBadgeTone.Catchup)
                }
                Row(
                    modifier = Modifier.align(Alignment.TopEnd).padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (quality != null) {
                        Text(
                            text = quality,
                            style = AreIptvTheme.typography.mono,
                            color = colors.accentHover,
                            modifier = Modifier
                                .background(colors.accentWash, RoundedCornerShape(AreIptvTheme.radius.xs))
                                .padding(horizontal = 7.dp, vertical = 3.dp),
                        )
                    }
                    // P0.3 (WCAG 1.4.1): stream health was color-only (dot color alone) --
                    // contentDescription exposes the same "Stable/Moderate/Poor" status
                    // AreStreamHealth's own label uses, so it's not color-only for TalkBack.
                    val healthLabel = when (health) {
                        AreStreamHealthLevel.Stable -> stringResource(R.string.health_stable)
                        AreStreamHealthLevel.Moderate -> stringResource(R.string.health_moderate)
                        AreStreamHealthLevel.Poor -> stringResource(R.string.health_poor)
                    }
                    Box(
                        Modifier
                            .size(9.dp)
                            .background(healthColor, CircleShape)
                            .semantics { contentDescription = healthLabel },
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        // Scale the logo chip with the tile (was a fixed 62dp that read as a tiny icon
                        // in the middle of the card): ~78% of the logo zone's height, kept square.
                        .fillMaxHeight(0.78f)
                        .aspectRatio(1f)
                        // Glass well over a fixed dark scrim floor (§6.3): the scrim is the
                        // luminance floor that keeps white-on-transparent provider logos visible
                        // (Color.kt:logoWell), the glass tint above lets the wash read through.
                        .background(colors.logoWellScrim, RoundedCornerShape(AreIptvTheme.radius.sm))
                        .glassChild(RoundedCornerShape(AreIptvTheme.radius.sm)),
                    contentAlignment = Alignment.Center,
                ) {
                    // Initials show only until the logo resolves -- logos often have transparent
                    // pixels, so keeping the initials permanently behind them let the text bleed
                    // through a loaded logo. onState (a lightweight callback, NOT subcomposition
                    // like SubcomposeAsyncImage) flips them off on success, keeping grid scroll smooth.
                    val logoLoaded = remember(logoUrl) { mutableStateOf(false) }
                    if (logoUrl == null || !logoLoaded.value) {
                        Text(text = initials, style = AreIptvTheme.typography.h2, color = colors.logoWellText)
                    }
                    if (logoUrl != null) {
                        AsyncImage(
                            model = logoUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            onState = { state ->
                                logoLoaded.value = state is AsyncImagePainter.State.Success
                                // Wash hue comes from the logo we just decoded -- never a second
                                // request; the provider rate-limits by IP (see sampleTileWashHue).
                                if (state is AsyncImagePainter.State.Success) {
                                    sampleTileWashHue(logoUrl, state.result.drawable)
                                }
                            },
                            modifier = Modifier.fillMaxSize().padding(6.dp),
                        )
                    }
                }
                if (onToggleFavorite != null) {
                    AreIconButton(
                        icon = if (isFavorite == true) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = if (isFavorite == true) stringResource(R.string.detail_remove_from_favorites) else stringResource(R.string.detail_add_to_favorites),
                        onClick = onToggleFavorite,
                        variant = AreIconButtonVariant.Glass,
                        size = AreIconButtonSize.ExtraSmall,
                        modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
                    )
                }
            }
            // info panel
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surfaceGlass)
                    .padding(top = 12.dp, start = 12.dp, end = 12.dp, bottom = 11.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (number != null) {
                        Text(text = number, style = AreIptvTheme.typography.mono, color = colors.textTertiary)
                    }
                    Text(
                        text = channel,
                        style = AreIptvTheme.typography.tile,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).then(if (focused) Modifier.basicMarquee() else Modifier),
                    )
                }
                // Always render the now line (empty when absent) so tiles with no current
                // program stay the same height as tiles that have one -- matches how the
                // `next` row below always reserves its height.
                Text(
                    text = if (now != null) stringResource(R.string.channel_tile_now, now) else "",
                    style = AreIptvTheme.typography.caption,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = if (focused) Modifier.basicMarquee() else Modifier,
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (next != null) stringResource(R.string.channel_tile_next, next) else "",
                        style = AreIptvTheme.typography.caption,
                        color = colors.textTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).then(if (focused) Modifier.basicMarquee() else Modifier),
                    )
                    if (codec != null) {
                        Text(text = codec, style = AreIptvTheme.typography.mono, color = colors.textTertiary)
                    }
                }
            }
        }
        if (obscured) ParentalLockOverlay(shape)
      }
    }
}

@Preview(widthDp = 700, heightDp = 320, showBackground = true)
@Composable
private fun AreChannelTilePreview() {
    AreIptvTheme {
        Row(modifier = Modifier.padding(24.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            AreChannelTile(
                channel = "Sky Sports HD",
                onClick = {},
                number = "101",
                now = "Premier League Live",
                next = "Match of the Day",
                quality = "1080p",
                codec = "H.264",
                catchup = true,
            )
        }
    }
}

/**
 * How long focus must rest on a tile before its artwork becomes the page's ambient backdrop.
 *
 * Sized to be shorter than a deliberate pause and longer than a D-pad repeat interval, so settling on
 * a tile still feels immediate while sweeping past a row costs nothing. See the publish site above for
 * what each publish actually costs.
 */
internal const val AmbientArtworkSettleMs = 280L

/** Trailing "HD"/" HD" that provider channel names carry; stripped before deriving initials. */
private val HdSuffix = Regex(" ?HD$", RegexOption.IGNORE_CASE)
