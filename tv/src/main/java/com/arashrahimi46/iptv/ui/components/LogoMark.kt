package com.arashrahimi46.iptv.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.arashrahimi46.iptv.R
import com.arashrahimi46.iptv.ui.theme.AreIptvTheme

/**
 * The brand mark as in-app chrome: the plateless tower glyph, tinted to the user's [AccentPreset]
 * rather than hardwired brand blue. The launcher icon and the splash keep the plated artwork --
 * this is for surfaces sitting on the page, where a navy tile reads as a foreign object once the
 * user has picked, say, amber.
 *
 * Two layers, not one tinted image. The mark has a light face and a darker detail pass (the play
 * triangle and the crossbar), and those details are *drawn*, not cut out -- so a single
 * `tint(accent)` over the whole glyph would flatten it into one solid accent blob with no play
 * triangle at all. Splitting the artwork by tone and tinting each layer from a different rung of
 * the accent ramp reproduces the original relationship at every preset.
 *
 * [accentHover]/[accentPress] rather than raw shades so this follows the theme for free: dark mode
 * resolves them to s400/s600, light to s500/s700 -- a two-step gap either way. Both source assets
 * are alpha-only masks (white RGB), so SrcIn discards their colour entirely.
 */
@Composable
fun AreLogoMark(size: Dp, modifier: Modifier = Modifier) {
    val colors = AreIptvTheme.colors
    Box(modifier.size(size)) {
        Image(
            painter = painterResource(R.drawable.ic_logo_glyph),
            contentDescription = null,
            colorFilter = ColorFilter.tint(colors.accentHover, BlendMode.SrcIn),
            modifier = Modifier.size(size),
        )
        Image(
            painter = painterResource(R.drawable.ic_logo_glyph_detail),
            contentDescription = null,
            colorFilter = ColorFilter.tint(colors.accentPress, BlendMode.SrcIn),
            modifier = Modifier.size(size),
        )
    }
}
