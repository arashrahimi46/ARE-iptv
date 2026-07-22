package com.arashrahimi46.iptv.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test

/**
 * Generates a Baseline Profile that AOT-compiles the app's two hottest journeys -- Home
 * scroll/focus travel and sidebar tab switching -- the exact flows that felt janky. Run it
 * on a connected API 28+ device (a real TV or a Play/AOSP emulator):
 *
 *   ./gradlew :tv:generateReleaseBaselineProfile
 *
 * The profile is written to tv/src/release/generated/baselineProfiles and shipped in the APK;
 * ProfileInstaller applies it on first launch. Re-generate after significant UI changes.
 */
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() = rule.collect(packageName = PACKAGE_NAME) {
        pressHome()
        startActivityAndWait()

        // Let the splash gate (SPLASH_DURATION_MS) clear and the shell settle.
        device.waitForIdle()
        device.wait(Until.hasObject(By.pkg(PACKAGE_NAME).depth(0)), 5_000)

        // Home vertical focus travel + scroll: D-pad down through the rails, then back up.
        // Driving focus with the D-pad is what actually exercises the tvFocusable/glow +
        // LazyRow reveal paths we want compiled.
        repeat(8) { device.pressDPadDown(); device.waitForIdle() }
        repeat(8) { device.pressDPadUp(); device.waitForIdle() }

        // Horizontal rail scroll on whatever row currently has focus.
        repeat(10) { device.pressDPadRight(); device.waitForIdle() }

        // Sidebar tab switching: move focus to the left rail and step through the tabs so the
        // per-tab content composition + first paging/DB read paths get profiled.
        repeat(6) { device.pressDPadLeft(); device.waitForIdle() }
        listOf("live", "movies", "series", "favorites", "home").forEach { _ ->
            device.pressDPadDown(); device.waitForIdle()
            device.pressDPadCenter(); device.waitForIdle()
            // Give the swapped-in tab a moment to compose and load its first page.
            device.wait(Until.hasObject(By.pkg(PACKAGE_NAME).depth(0)), 3_000)
            // Return focus to the sidebar for the next selection.
            repeat(4) { device.pressDPadLeft(); device.waitForIdle() }
        }
    }

    private companion object {
        const val PACKAGE_NAME = "com.arashrahimi46.iptv"
    }
}
