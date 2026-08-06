package com.arashrahimi46.iptv.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
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
 * Global "reduce motion" preference. Backed by the real Settings "Reduce motion" toggle
 * since Phase 4 (see AreIptvTheme's `reducedMotion` param) -- `false` here is only the
 * CompositionLocal's structural default before a real value is provided.
 */
val LocalReducedMotion = staticCompositionLocalOf { false }

/**
 * Convenience CompositionLocal exposing the resolved motion token set.
 *
 * STATIC, like the other ten theme locals. It was the one non-static local left, so every reader --
 * and `tvFocusable` reads it, i.e. every focusable in the app -- paid for a tracked snapshot
 * subscription it can never benefit from. Its value only flips between the two constant token sets
 * when the user toggles "Reduce motion", which flips the (already static) [LocalReducedMotion] in the
 * same `AreIptvTheme` call, so the subtree recomposes wholesale either way. Non-static is only worth
 * it for a local whose value changes at runtime *independently* of its subtree being rebuilt.
 */
val LocalAreIptvMotion = staticCompositionLocalOf { AreIptvMotionDefault }
