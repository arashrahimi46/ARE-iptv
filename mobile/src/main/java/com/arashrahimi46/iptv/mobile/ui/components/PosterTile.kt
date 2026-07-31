package com.arashrahimi46.iptv.mobile.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arashrahimi46.iptv.core.R as CoreR
import com.arashrahimi46.iptv.data.parser.OmdbClient
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.theme.glassBorderBrush
import com.arashrahimi46.iptv.ui.theme.glassChild
import com.arashrahimi46.iptv.ui.theme.glassTrack
import com.arashrahimi46.iptv.ui.theme.rememberTileArtwork
import com.arashrahimi46.iptv.ui.theme.rememberTileWashHue
import com.arashrahimi46.iptv.ui.theme.tileWash

/** Fallback decode bound when the tile fills a grid cell whose width isn't known in composition. */
private val GridPosterDecodeWidth = 220.dp

/**
 * Portrait VOD tile for movies/series.
 *
 * [width] `null` means "fill the parent's width" -- a grid caller passes nothing and gets the
 * cell width; a rail caller passes an explicit Dp. The TV `tilePosterWidth` token is never the
 * default (D10): tile size is a per-screen decision.
 *
 * Touch: the poster is the press surface, ripple only (`disableScale = true` -- a tile that shrinks
 * under a finger reads as a glitch, and the ripple already says "pressed"). Long press fires the
 * haptic and [onLongClick]. No focus ring, no marquee, no ambient-artwork publish.
 */
@Composable
fun ArePosterTile(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    posterUrl: String? = null,
    meta: String? = null,
    rating: String? = null,
    progress: Float? = null,
    width: Dp? = null,
    badges: (@Composable RowScope.() -> Unit)? = null,
    isFavorite: Boolean? = null,
    onToggleFavorite: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    lockCategory: String? = null,
    interactionSource: MutableInteractionSource? = null,
) {
    val colors = AreIptvTheme.colors
    val shape = RoundedCornerShape(AreIptvTheme.radius.lg)
    // Cleaned once, here, rather than at each of the six call sites (grids, rails, search,
    // favorites, continue-watching) -- every one of them was passing the provider's raw name.
    val display = remember(title) { OmdbClient.displayTitle(title) }
    // Initials come off the CLEANED name too. On a raw "FR - The Matrix" the old code took the
    // first letters of "FR" and "-", so every French title in the catalog fell back to the same
    // "F-" placeholder instead of "TM".
    val initials = remember(display) {
        display.split(" ").take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")
    }
    val washHue = rememberTileWashHue(posterUrl, seed = title)
    val blur = LocalParentalBlur.current
    val obscured = blur.isObscured(lockCategory)
    val decodeWidth = (width ?: GridPosterDecodeWidth) * 2

    Column(modifier = modifier.then(if (width != null) Modifier.width(width) else Modifier.fillMaxWidth())) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .areTouch(
                    onClick = if (obscured) blur.onReveal else onClick,
                    shape = shape,
                    // Without this the tappable poster is an unlabeled "Button": the title Text
                    // lives outside this Box, so it never merges into the clickable, and the
                    // artwork is correctly contentDescription=null. TalkBack announced the whole
                    // grid as a run of anonymous buttons with the names stranded as separate stops.
                    contentDescription = listOfNotNull(display, meta).joinToString(", "),
                    onLongClick = if (obscured) null else onLongClick,
                    interactionSource = interactionSource,
                    backgroundColor = colors.surfaceGlass,
                    // Phase-3 follow-up 6: the glass hairline is a light-on-dark sheen, so on the
                    // light theme a near-white tile dissolved into the off-white page. Light mode
                    // gets the solid `borderDefault` edge the project convention calls for; dark
                    // mode keeps the lit gradient.
                    borderBrush = if (AreIptvTheme.isDark) glassBorderBrush() else null,
                    borderColor = if (AreIptvTheme.isDark) null else colors.borderDefault,
                    minTouchTarget = 0.dp,
                    disableScale = true,
                )
                // Clip everything the tile draws to its own shape: a square image or a square wash
                // poking through the rounded corners is the light-theme defect this prevents.
                .clip(shape),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .tileWash(shape, washHue)
                    .then(if (obscured) Modifier.blur(16.dp) else Modifier),
            ) {
                val poster = rememberTileArtwork(posterUrl, maxDimension = decodeWidth)
                if (poster == null) {
                    Text(
                        text = initials,
                        style = AreIptvTheme.typography.display,
                        color = colors.textTertiary,
                        modifier = Modifier.align(Alignment.Center),
                    )
                } else {
                    Image(
                        bitmap = poster,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(shape),
                    )
                }
                if (badges != null) {
                    Row(
                        modifier = Modifier.align(Alignment.TopStart).padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        content = badges,
                    )
                }
                if (rating != null) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                            .glassChild(RoundedCornerShape(AreIptvTheme.radius.pill))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = null,
                            tint = colors.ratingStar,
                            modifier = Modifier.size(12.dp),
                        )
                        Text(text = rating, style = AreIptvTheme.typography.caption, color = colors.textPrimary)
                    }
                }
                if (progress != null) {
                    ProgressRail(progress)
                }
                // Hidden while obscured: the tile's click routes to blur.onReveal and its long-press
                // is nulled, but the heart called onToggleFavorite directly -- so a parentally
                // blurred title could still be favorited (and thereby surfaced in the Favorites
                // rail) without ever entering the PIN. The lock leaked through the one control that
                // wasn't gated.
                if (onToggleFavorite != null && !obscured) {
                    AreIconButton(
                        icon = if (isFavorite == true) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = if (isFavorite == true) {
                            stringResource(CoreR.string.detail_remove_from_favorites)
                        } else {
                            stringResource(CoreR.string.detail_add_to_favorites)
                        },
                        onClick = onToggleFavorite,
                        variant = AreIconButtonVariant.Glass,
                        size = AreIconButtonSize.Small,
                        // Both the heart and the resume bar are bottom-anchored, and Continue
                        // Watching is the one rail that shows both, so the heart used to cover the
                        // end of the bar. The bar keeps the full tile width (it is a percentage
                        // readout -- cropping it lies about the position); the heart clears it
                        // instead, by the bar's own height plus its inset plus a 4dp gap.
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(
                                end = 4.dp,
                                bottom = if (progress != null) {
                                    ProgressRailInset + ProgressRailHeight + 4.dp
                                } else {
                                    4.dp
                                },
                            ),
                    )
                }
            }
            if (obscured) ParentalLockOverlay(shape)
        }
        Box(Modifier.height(10.dp))
        Text(
            text = display,
            style = AreIptvTheme.typography.tile,
            // Always two lines tall, even for a one-word title. The block used to size to its
            // content, so neighbours in the same row ended at different heights and every rail and
            // grid had a ragged bottom edge.
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = colors.textPrimary,
        )
        if (meta != null) {
            Text(
                text = meta,
                style = AreIptvTheme.typography.caption,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Geometry of [ProgressRail], shared so a bottom-anchored overlay can clear it instead of covering it. */
internal val ProgressRailHeight = 5.dp
internal val ProgressRailInset = 8.dp

/** The resume bar: a glass track inset from the poster edge so it floats inside the rounded tile. */
@Composable
internal fun BoxScope.ProgressRail(progress: Float) {
    val barShape = RoundedCornerShape(AreIptvTheme.radius.pill)
    Box(
        modifier = Modifier
            .align(Alignment.BottomStart)
            .fillMaxWidth()
            .padding(horizontal = ProgressRailInset, vertical = ProgressRailInset)
            .height(ProgressRailHeight)
            .glassTrack(barShape),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .fillMaxSize()
                .clip(barShape)
                .background(AreIptvTheme.colors.accent),
        )
    }
}
