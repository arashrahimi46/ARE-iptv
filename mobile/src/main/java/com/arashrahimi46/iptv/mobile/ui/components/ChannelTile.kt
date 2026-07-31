package com.arashrahimi46.iptv.mobile.ui.components

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arashrahimi46.iptv.core.R as CoreR
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.theme.glassBorderBrush
import com.arashrahimi46.iptv.ui.theme.glassChild
import com.arashrahimi46.iptv.ui.theme.rememberTileArtwork
import com.arashrahimi46.iptv.ui.theme.rememberTileWashHue
import com.arashrahimi46.iptv.ui.theme.sampleTileWashHue
import com.arashrahimi46.iptv.ui.theme.tileWash

/** Stream reliability level. Declared here because the tile is its primary consumer. */
enum class AreStreamHealthLevel { Stable, Moderate, Poor }

/** Fallback decode bound when the tile fills a grid cell whose width isn't known in composition. */
private val GridLogoDecodeWidth = 220.dp

/** Trailing "HD"/" HD" that provider channel names carry; stripped before deriving initials. */
private val HdSuffix = Regex(" ?HD$", RegexOption.IGNORE_CASE)

/**
 * Logo-first live-TV tile. IPTV providers can't reliably serve stream previews, so the tile leads
 * with a logo/initials well over an info panel.
 *
 * [width] `null` fills the parent (grid cell); an explicit Dp is a rail. [health] is nullable now
 * and the dot is drawn only when it is known. The now/next block renders only when `now != null` --
 * the TV build reserved empty line heights to stop focus-ring jitter, which no longer exists.
 */
@Composable
fun AreChannelTile(
    channel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    logoUrl: String? = null,
    number: String? = null,
    now: String? = null,
    next: String? = null,
    category: String? = null,
    isRadio: Boolean = false,
    health: AreStreamHealthLevel? = null,
    quality: String? = null,
    catchup: Boolean = false,
    width: Dp? = null,
    isFavorite: Boolean? = null,
    onToggleFavorite: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    lockCategory: String? = null,
    interactionSource: MutableInteractionSource? = null,
) {
    val colors = AreIptvTheme.colors
    val shape = RoundedCornerShape(AreIptvTheme.radius.lg)
    val washHue = rememberTileWashHue(logoUrl, seed = channel)
    val blur = LocalParentalBlur.current
    val obscured = blur.isObscured(lockCategory)
    val initials = remember(channel) {
        HdSuffix.replace(channel, "")
            .split(" ").take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")
    }
    val decodeWidth = width ?: GridLogoDecodeWidth

    Box(
        modifier = modifier
            .then(if (width != null) Modifier.width(width) else Modifier.fillMaxWidth())
            .areTouch(
                onClick = if (obscured) blur.onReveal else onClick,
                shape = shape,
                onLongClick = if (obscured) null else onLongClick,
                interactionSource = interactionSource,
                backgroundColor = colors.surfaceGlass,
                // Phase-3 follow-up 6: the glass hairline is a light-on-dark sheen, so on the light
                // theme a near-white tile dissolved into the off-white page. Light mode gets the
                // solid `borderDefault` edge the project convention calls for; dark keeps the sheen.
                borderBrush = if (AreIptvTheme.isDark) glassBorderBrush() else null,
                borderColor = if (AreIptvTheme.isDark) null else colors.borderDefault,
                minTouchTarget = 0.dp,
                disableScale = true,
            )
            // Clip the content to the tile shape so the info panel's square fill doesn't poke
            // square corners through the rounded edge (a light-theme defect).
            .clip(shape),
    ) {
        Column(Modifier.fillMaxWidth().then(if (obscured) Modifier.blur(16.dp) else Modifier)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .tileWash(RectangleShape, washHue),
            ) {
                Row(
                    modifier = Modifier.align(Alignment.TopStart).padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    AreBadge(stringResource(CoreR.string.badge_live), tone = AreBadgeTone.Live)
                    if (catchup) AreBadge(stringResource(CoreR.string.badge_catchup), tone = AreBadgeTone.Catchup)
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
                    if (health != null) {
                        // WCAG 1.4.1: the dot is a colour-only cue to a sighted user, so the same
                        // status goes out as a contentDescription for TalkBack.
                        val healthLabel = when (health) {
                            AreStreamHealthLevel.Stable -> stringResource(CoreR.string.health_stable)
                            AreStreamHealthLevel.Moderate -> stringResource(CoreR.string.health_moderate)
                            AreStreamHealthLevel.Poor -> stringResource(CoreR.string.health_poor)
                        }
                        val healthColor = when (health) {
                            AreStreamHealthLevel.Stable -> colors.healthStable
                            AreStreamHealthLevel.Moderate -> colors.healthModerate
                            AreStreamHealthLevel.Poor -> colors.healthPoor
                        }
                        Box(
                            Modifier
                                .size(9.dp)
                                .background(healthColor, CircleShape)
                                .semantics { contentDescription = healthLabel },
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxHeight(0.78f)
                        .aspectRatio(1f)
                        // Glass well over a fixed dark scrim floor: the scrim is the luminance floor
                        // that keeps white-on-transparent provider logos visible in BOTH themes.
                        .background(colors.logoWellScrim, RoundedCornerShape(AreIptvTheme.radius.sm))
                        .glassChild(RoundedCornerShape(AreIptvTheme.radius.sm)),
                    contentAlignment = Alignment.Center,
                ) {
                    val logo = rememberTileArtwork(logoUrl, maxDimension = decodeWidth) { bitmap ->
                        // Wash hue comes from the logo already decoded here -- never a second
                        // request; the provider rate-limits by IP.
                        sampleTileWashHue(logoUrl, bitmap)
                    }
                    if (logo == null) {
                        Text(text = initials, style = AreIptvTheme.typography.h3, color = colors.logoWellText)
                    } else {
                        Image(
                            bitmap = logo,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize().padding(6.dp),
                        )
                    }
                }
                if (onToggleFavorite != null) {
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
                        modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp),
                    )
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surfaceGlass)
                    .padding(top = 12.dp, start = 12.dp, end = 12.dp, bottom = 11.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (number != null) {
                        Text(text = number, style = AreIptvTheme.typography.mono, color = colors.textTertiary)
                    }
                    Text(
                        text = channel,
                        style = AreIptvTheme.typography.tile,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (isRadio || category != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        if (isRadio) {
                            AreBadge(
                                text = stringResource(CoreR.string.badge_radio),
                                tone = AreBadgeTone.Smart,
                                icon = Icons.Filled.Radio,
                            )
                        }
                        if (category != null) {
                            AreBadge(
                                text = category,
                                tone = AreBadgeTone.Neutral,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                        }
                    }
                }
                if (now != null) {
                    Text(
                        text = stringResource(CoreR.string.channel_tile_now, now),
                        style = AreIptvTheme.typography.caption,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    if (next != null) {
                        Text(
                            text = stringResource(CoreR.string.channel_tile_next, next),
                            style = AreIptvTheme.typography.caption,
                            color = colors.textTertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
        if (obscured) ParentalLockOverlay(shape)
    }
}
