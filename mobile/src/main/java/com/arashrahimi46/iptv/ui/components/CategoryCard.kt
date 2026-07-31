package com.arashrahimi46.iptv.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Theaters
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.arashrahimi46.iptv.core.R
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import com.arashrahimi46.iptv.ui.interaction.AreInteractive
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme
import com.arashrahimi46.iptv.ui.theme.glassBorderBrush
import com.arashrahimi46.iptv.ui.theme.glassChild
import com.arashrahimi46.iptv.ui.theme.rememberTileWashHue
import com.arashrahimi46.iptv.ui.theme.tileWash

/** Content kind, drives the folder icon/watermark (CategoryCard.jsx `kind`) and the localized
 * count-unit string ("%1$d channels" etc.) shown under the name. */
enum class AreCategoryKind(val icon: ImageVector, @StringRes val countRes: Int) {
    Live(Icons.Filled.LiveTv, R.string.live_count_channels),
    Movies(Icons.Filled.Movie, R.string.movies_count_titles),
    Series(Icons.Filled.Theaters, R.string.movies_count_titles),
    Guide(Icons.Filled.TableChart, R.string.live_count_channels),
    Default(Icons.Filled.Folder, R.string.category_count_items),
}

/**
 * CategoryCard — folder-style browse tile for an IPTV category (CategoryCard.jsx).
 * Artwork is optional and often absent in real playlists, so the no-image state
 * is a first-class clean folder tile (kind-icon watermark + label).
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AreCategoryCard(
    name: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    count: Int? = null,
    kind: AreCategoryKind = AreCategoryKind.Default,
    smart: Boolean = false,
    compact: Boolean = false,
    width: Dp = AreIptvTheme.spacing.tileLandWidth,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    // Denser fill for a card sitting inside a modal over media (e.g. HomeAddSectionDialog) --
    // same purpose as glassSurface's own `elevated` (see Glass.kt): plain `surfaceGlass` is tuned
    // for sitting directly on the ambient backdrop (Home's own Categories rail) and reads as
    // near-transparent over the busy poster art already showing through the dialog's own panel.
    elevated: Boolean = false,
) {
    val colors = AreIptvTheme.colors
    val shape = RoundedCornerShape(AreIptvTheme.radius.md)
    // A category card is identified by its name, so the name itself is the blur key (an adult
    // category is obscured under the parental "blur" mode; see [LocalParentalBlur]).
    val blur = LocalParentalBlur.current
    val obscured = blur.isObscured(name)
    // Category tiles carry no artwork, so the flat surfaceGlass fill read as a dead grey slab. A
    // name-seeded wash (§6.3) gives each card its own faint refractive tint -- the same cheap glass
    // the channel tile uses -- so the surface looks alive without a per-tile backdrop blur.
    val washHue = rememberTileWashHue(logoUrl = null, seed = name)

    AreInteractive(
        onClick = if (obscured) blur.onReveal else onClick,
        modifier = modifier.width(width).aspectRatio(if (compact) 16f / 7f else 16f / 10f),
        interactionSource = interactionSource,
        shape = shape,
        // Glass fill + lit-edge gradient reads as a distinct card in both themes (the gradient's
        // dark bottom stop supplies the light-theme edge the fill can't).
        backgroundColor = if (elevated) colors.surfaceGlassElevated else colors.surfaceGlass,
        borderBrush = glassBorderBrush(),
    ) { _, _ ->
      Box(Modifier.fillMaxSize().clip(shape).tileWash(RectangleShape, washHue)) {
        Box(Modifier.fillMaxSize().then(if (obscured) Modifier.blur(16.dp) else Modifier)) {
            Icon(
                kind.icon,
                contentDescription = null,
                tint = colors.textPrimary.copy(alpha = 0.05f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(if (compact) 96.dp else 132.dp),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
                    .size(if (compact) 30.dp else 36.dp)
                    // Was a solid accentWash square -- the one opaque sticker on a translucent card.
                    // Frosted glassChild tint keeps the accent glyph but lets the card read through it.
                    .glassChild(RoundedCornerShape(AreIptvTheme.radius.sm)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(kind.icon, contentDescription = null, tint = colors.accent, modifier = Modifier.size(if (compact) 16.dp else 20.dp))
            }
            if (smart) {
                Box(Modifier.align(Alignment.TopEnd).padding(10.dp)) {
                    AreBadge("Smart", tone = AreBadgeTone.Smart, glow = true)
                }
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 10.dp)
                    .padding(bottom = if (compact) 10.dp else 12.dp),
            ) {
                Text(
                    text = name,
                    style = if (compact) AreIptvTheme.typography.label else AreIptvTheme.typography.h3,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (count != null) {
                    Text(
                        text = stringResource(kind.countRes, count),
                        style = AreIptvTheme.typography.caption,
                        color = colors.textSecondary,
                    )
                }
            }
        }
        if (obscured) ParentalLockOverlay(shape)
      }
    }
}

@Preview(widthDp = 700, heightDp = 260, showBackground = true)
@Composable
private fun AreCategoryCardPreview() {
    AreIptvTheme {
        Row(modifier = Modifier.padding(24.dp)) {
            AreCategoryCard(name = "Sports", onClick = {}, count = 42, kind = AreCategoryKind.Live, smart = true)
        }
    }
}
