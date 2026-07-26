package com.arashrahimi46.iptv.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.kyant.backdrop.Backdrop
import kotlin.math.cos
import kotlin.math.sin

/**
 * The artwork the ambient backdrop should currently wash the page with -- normally the poster/logo of
 * whatever the user has focused. Browse screens publish into this from their tiles' focus handlers;
 * screens with no artwork (Settings, Onboarding) simply leave it null and get the accent mesh alone.
 *
 * Deliberately **focused-item only**, not a row-level dominant palette: it is cheaper, it responds
 * immediately, and the cross-fade already absorbs fast D-pad scrolling (design §13).
 */
val LocalAmbientArtwork = staticCompositionLocalOf<MutableState<String?>> {
    error("LocalAmbientArtwork not provided -- AreIptvAppShell owns it")
}

/**
 * The layer [Modifier.glassSurface] samples and blurs. Provided by [AreIptvAppShell] and captured
 * from [AmbientBackdrop] **only** -- deliberately not from the page content, which would feed glass
 * surfaces back into their own backdrop. Null outside the shell (dialogs, the player), where glass
 * falls through to the V1 translucent-fill path.
 */
val LocalAppBackdrop = staticCompositionLocalOf<Backdrop?> { null }

/**
 * The ambient backdrop **plus the page content** -- for the one surface class that genuinely floats
 * OVER the page: the expanded sidebar. Only ever provided while the sidebar is expanded (the shell
 * gates it), and only ever sampled by that panel, so there is no feedback: the sidebar is drawn as a
 * sibling *outside* this layer and so is never inside its own backdrop.
 *
 * Why it exists: [LocalAppBackdrop] captures the ambient wash only, and that wash starts with an
 * opaque `bgBase`. A surface sampling it therefore *repaints the page's own background over the page*
 * inside its bounds -- so however sheer the fill, the expanded sidebar could never show the rails it
 * covers. Arithmetic, not tuning: there was nothing but ambient behind it in the buffer.
 *
 * This is also the one place a per-surface blur is warranted (§4 says blur belongs on the layer that
 * captures *sharp* content, not re-derived per surface): posters and text behind the panel are sharp,
 * and the blur is what turns "transparent window" into frosted glass while keeping the labels legible.
 */
val LocalPageBackdrop = staticCompositionLocalOf<Backdrop?> { null }

/**
 * The thing glass refracts (design spec §3) -- **the single highest-value change in Glass V2**.
 *
 * V1 painted one flat opaque `bgBase` behind everything, so `surfaceGlass` at 55% composited to a
 * flat `#14171E`: glass over an opaque page is arithmetically just a lighter grey, and no border,
 * shadow, blur or shader can recover depth that was never in the buffer. This puts content there.
 *
 * Back to front:
 *  1. `bgBase` -- the contrast floor, unchanged.
 *  2. Artwork wash -- the focused item's image, heavily blurred at low alpha (Tier A/B only; `blur`
 *     is API 31+). Cross-fades on focus change via [AsyncImage]'s own fade.
 *  3. Accent mesh -- wide radial gradients in the active [AccentPreset], drifting very slowly. This
 *     is what keeps screens with no artwork alive.
 *  4. Veil + vignette -- pulls the edges back down so the sidebar and HUD keep a contrast anchor.
 *
 * Drawn ONCE at the shell level. Never per-screen, never per-card.
 */
@Composable
fun AmbientBackdrop(modifier: Modifier = Modifier) {
    val colors = AreIptvTheme.colors
    val tier = LocalGlassTier.current
    val artwork by LocalAmbientArtwork.current

    // V2 max-smoothness: the ambient mesh is STATIC. It used to drift on a 42s loop (Tier A only),
    // but that animation invalidated this backdrop's draw every frame -- and since every glass surface
    // in the app samples this layer, each one had to re-blur continuously, a constant GPU cost even on
    // a fully idle screen, for a motion far too slow to perceive. A fixed phase keeps the exact mesh
    // look while letting an idle screen do zero backdrop work (nothing invalidates, nothing re-blurs).
    val t = 0f

    val meshA = colors.accent.copy(alpha = if (colors.isDark) 0.20f else 0.13f)
    val meshB = colors.accentHover.copy(alpha = if (colors.isDark) 0.13f else 0.09f)

    androidx.compose.foundation.layout.Box(modifier.fillMaxSize().background(colors.bgBase)) {
        if (artwork != null && tier.hasBackdropBlur) {
            AsyncImage(
                model = artwork,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = if (colors.isDark) 0.30f else 0.18f,
                modifier = Modifier.fillMaxSize().blur(72.dp),
            )
        }
        androidx.compose.foundation.layout.Box(
            Modifier.fillMaxSize().drawBehind {
                // Two wide radial lobes, sized well past the viewport
                // so only their soft interiors are ever on screen -- no visible gradient edge.
                val r = size.maxDimension * 0.85f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(meshA, Color.Transparent),
                        center = Offset(
                            size.width * (0.22f + 0.10f * cos(t * 6.2832f)),
                            size.height * (0.18f + 0.08f * sin(t * 6.2832f)),
                        ),
                        radius = r,
                    ),
                    radius = r,
                    center = Offset(
                        size.width * (0.22f + 0.10f * cos(t * 6.2832f)),
                        size.height * (0.18f + 0.08f * sin(t * 6.2832f)),
                    ),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(meshB, Color.Transparent),
                        center = Offset(
                            size.width * (0.86f - 0.09f * sin(t * 6.2832f)),
                            size.height * (0.78f - 0.07f * cos(t * 6.2832f)),
                        ),
                        radius = r * 0.9f,
                    ),
                    radius = r * 0.9f,
                    center = Offset(
                        size.width * (0.86f - 0.09f * sin(t * 6.2832f)),
                        size.height * (0.78f - 0.07f * cos(t * 6.2832f)),
                    ),
                )
                // Veil + vignette: the contrast floor every text token is measured against.
                if (colors.backdropVeil.alpha > 0f) drawRect(colors.backdropVeil)
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(Color.Transparent, colors.bgSunken.copy(alpha = 0.55f)),
                        center = Offset(size.width * 0.5f, size.height * 0.45f),
                        radius = size.maxDimension * 0.72f,
                    ),
                )
            },
        )
    }
}
