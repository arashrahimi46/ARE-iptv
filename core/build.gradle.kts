plugins {
    alias(libs.plugins.android.library)
}

// :core is a RESOURCES-ONLY module: the 24 strings.xml locale files (base + 23 translations) and
// nothing else. No Kotlin, no Compose, no Room, no dependencies.
//
// It used to hold the shared data layer and the whole Are* component set so :mobile could reuse
// :tv's code. That was unwound deliberately: sharing rendering/interaction code between a D-pad TV
// app and a touch phone app meant every :mobile change had to be re-verified on :tv, and several
// real TV regressions (focus-ring geometry, segmented-control selection, dialog scroll, a Settings
// crash) came in through exactly that seam. :tv and :mobile now each own their full source tree.
//
// Strings stayed shared because they are inert -- a translation cannot regress TV behaviour, and
// keeping them in one place means a new user-facing key is added to 24 files instead of 48.
// Consumers reference them as `com.arashrahimi46.iptv.core.R.string.*` (AGP's non-transitive R
// class), aliased to `CoreR` in the handful of files that also use their own module's R.
//
// DO NOT add Kotlin sources, resources other than values*/strings.xml, or dependencies here.
android {
    namespace = "com.arashrahimi46.iptv.core"
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        // Lowest of the two consumers (:tv is 36, :mobile is 26) so neither is constrained.
        minSdk = 23
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
