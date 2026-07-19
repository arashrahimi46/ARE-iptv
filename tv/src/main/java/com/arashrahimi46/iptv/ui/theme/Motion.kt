package com.arashrahimi46.iptv.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Motion tokens, mirroring tokens/motion.css. Durations/scales collapse to
 * near-zero when [LocalReducedMotion] resolves true, matching the CSS
 * `prefers-reduced-motion: reduce` block.
 */
data class AreIptvMotion(
    val durInstantMs: Int,
    val durFastMs: Int,
    val durBaseMs: Int,
    val durSlowMs: Int,
    val durSlowerMs: Int,
    val focusScale: Float,
    val pressScale: Float,
    val easeOut: Easing,
    val easeInOut: Easing,
    val easeEmph: Easing,
)

/** cubic-bezier(0.16, 1, 0.3, 1) */
val EaseOut: Easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

/** cubic-bezier(0.65, 0, 0.35, 1) */
val EaseInOut: Easing = CubicBezierEasing(0.65f, 0f, 0.35f, 1f)

/** cubic-bezier(0.22, 1.2, 0.36, 1) — slight overshoot, used for focus travel. */
val EaseEmph: Easing = CubicBezierEasing(0.22f, 1.2f, 0.36f, 1f)

val AreIptvMotionDefault = AreIptvMotion(
    durInstantMs = 90,
    durFastMs = 150,
    durBaseMs = 220,
    durSlowMs = 360,
    durSlowerMs = 560,
    focusScale = 1.06f,
    pressScale = 0.97f,
    easeOut = EaseOut,
    easeInOut = EaseInOut,
    easeEmph = EaseEmph,
)

val AreIptvMotionReduced = AreIptvMotion(
    durInstantMs = 0,
    durFastMs = 0,
    durBaseMs = 50,
    durSlowMs = 50,
    durSlowerMs = 50,
    focusScale = 1.02f,
    pressScale = 1f,
    easeOut = EaseOut,
    easeInOut = EaseInOut,
    easeEmph = EaseEmph,
)

/**
 * Global "reduce motion" preference. Not yet backed by a real Settings
 * screen/DataStore (Phase 1+) — defaults to false in-memory so every
 * animation in the component library is already wired to respect it.
 */
val LocalReducedMotion = staticCompositionLocalOf { false }

/** Convenience CompositionLocal exposing the resolved motion token set. */
val LocalAreIptvMotion = compositionLocalOf { AreIptvMotionDefault }
