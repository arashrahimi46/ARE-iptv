package com.arashrahimi46.iptv.mobile.design

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.getSystemService

/**
 * What the current device can actually render of the glass material (design spec §8). Resolved ONCE
 * at the composition root into [LocalGlassTier]; call sites read the tier instead of scattering
 * `Build.VERSION.SDK_INT` checks through the component library.
 */
enum class GlassTier {
    /** API 33+ and not low-RAM: ambient backdrop + blur + vibrancy + specular (+ optional lens). */
    A,

    /** API 31-32, or low-RAM: ambient backdrop + blur. No AGSL shaders, no drift animation. */
    B,

    /** API <= 30: the V1 look, plus the per-tile wash (§6.3 costs two gradient stops). */
    C,
    ;

    /** Can we put a real blurred backdrop behind glass? Drives the [withBlurredBackdrop] retune. */
    val hasBackdropBlur: Boolean get() = this != C

    /** AGSL / `RuntimeShader` effects (vibrancy, lens, gamma) are API 33+. */
    val hasShaders: Boolean get() = this == A

    /** Ambient drift and other purely decorative animation -- first thing dropped on weak hardware. */
    val hasAmbientDrift: Boolean get() = this == A
}

val LocalGlassTier = staticCompositionLocalOf { GlassTier.C }

/**
 * Resolve the tier for this device. Low-RAM machines are demoted a step even on a new API level:
 * a Fire TV Stick reports API 33 and still cannot afford a full-screen blur per frame.
 */
@Composable
fun rememberGlassTier(): GlassTier {
    val context = LocalContext.current
    return remember(context) { resolveGlassTier(context) }
}

internal fun resolveGlassTier(context: Context): GlassTier {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return GlassTier.C
    val lowRam = context.getSystemService<ActivityManager>()?.isLowRamDevice ?: false
    if (lowRam) return GlassTier.B
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) GlassTier.A else GlassTier.B
}
