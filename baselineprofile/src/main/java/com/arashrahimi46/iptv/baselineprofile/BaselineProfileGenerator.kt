package com.arashrahimi46.iptv.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test

/**
 * Generates a Baseline Profile that AOT-compiles the app's hottest journeys -- Home scroll/focus
 * travel, sidebar tab switching, and Settings -- the exact flows that felt janky. Run it with:
 *
 *   ./gradlew :tv:generateReleaseBaselineProfile
 *
 * RUN IT ON AN API 33+ EMULATOR, NOT THE TV. Collection needs API 33+, or a rooted adb session on
 * API 28+. The Sony XL95 this app is tuned against is API 31 and unrootable, so pointing the task at
 * it fails with "Baseline Profile collection requires API 33+..." and leaves the shipped profile at
 * ZERO app classes. The Television_1080p AVD (API 36) is the machine for this.
 *
 * Run it in an LTR locale -- the journey below drives the nav rail with `pressDPadLeft`, which walks
 * the wrong way in fa/ar. The generated profile is locale-independent either way.
 *
 * The profile is written to tv/src/release/generated/baselineProfiles and shipped in the APK;
 * ProfileInstaller applies it on first launch. Re-generate after significant UI changes.
 */
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() = rule.collect(
        packageName = PACKAGE_NAME,
        // Without this the run emits "No startup profile rules were generated" and the app gets a
        // plain baseline profile with no startup ordering -- which is exactly the part that pays off
        // on a slow TV CPU, where class loading dominates time-to-first-frame.
        includeInStartupProfile = true,
    ) {
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

        // Sidebar tab switching: move focus to the nav rail and step through the tabs so the
        // per-tab content composition + first paging/DB read paths get profiled.
        //
        // ASSUMES AN LTR LOCALE on the generating device -- the nav rail sits on the LEFT in LTR and
        // mirrors to the RIGHT in RTL (fa/ar), so in an RTL locale every `pressDPadLeft` below walks
        // AWAY from the rail and the tab half of this journey profiles nothing. See the note on
        // running this in the KDoc above; the profile itself is a list of classes and methods, so it
        // applies identically whatever locale the user later runs the app in.
        repeat(6) { device.pressDPadLeft(); device.waitForIdle() }
        listOf("live", "movies", "series", "favorites", "home").forEach { _ ->
            device.pressDPadDown(); device.waitForIdle()
            device.pressDPadCenter(); device.waitForIdle()
            // Give the swapped-in tab a moment to compose and load its first page.
            device.wait(Until.hasObject(By.pkg(PACKAGE_NAME).depth(0)), 3_000)
            // Return focus to the sidebar for the next selection.
            repeat(4) { device.pressDPadLeft(); device.waitForIdle() }
        }

        // Settings last, and deliberately: it is the app's most expensive first paint -- measured on
        // the real XL95 at a 244ms frame, 125ms of it measure/layout -- and not one of its panes,
        // rows or controls was in the profile before. Walk to the bottom of the rail for the gear,
        // open it, sweep the tab strip so every pane composes once, then drop into the pane itself
        // so the section/row/control code is profiled rather than just the strip.
        repeat(6) { device.pressDPadLeft(); device.waitForIdle() }
        repeat(10) { device.pressDPadDown(); device.waitForIdle() }
        device.pressDPadCenter()
        device.wait(Until.hasObject(By.pkg(PACKAGE_NAME).depth(0)), 3_000)
        repeat(5) { device.pressDPadRight(); device.waitForIdle() }
        repeat(4) { device.pressDPadDown(); device.waitForIdle() }
    }

    private companion object {
        /**
         * Must match tv/build.gradle.kts `applicationId` -- NOT the `namespace`
         * (com.arashrahimi46.iptv), which is what this used to hold. The two were deliberately split
         * when the store identity became `com.areiptv.tv`, and the baselineprofile plugin installs and
         * launches the nonMinifiedRelease APK under the applicationId -- so with the namespace here
         * `BaselineProfileRule.collect` fails with "Unable to find target package" and the profile can
         * never be regenerated. (`tv/src/release/generated/baselineProfiles/` does not exist, so the
         * shipped `assets/dexopt/baseline.prof` is currently 100% library rules and contains ZERO
         * app classes -- no composable, focus or tile code is AOT-compiled on a cold start.)
         */
        const val PACKAGE_NAME = "com.areiptv.tv"
    }
}
