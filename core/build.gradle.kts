plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// Shared data layer (data/repository, data/db, data/parser, data/model, data/settings,
// data/player, data/recording, CredentialsStore) + the handful of pure design-token/logic files
// UserSettings and the Home rail curator depend on (ui/theme/Color.kt's AreIptvColors/AccentPreset,
// ui/home/HomeSection.kt + HomeRailCurator.kt, ui/player/HudLayout.kt). Extracted from :tv so that
// :mobile can depend on it too -- see the top of this file's history for why :mobile could not
// depend on :tv directly (com.android.application) -> (com.android.application) project
// dependencies compile fine but AAPT2 fails resource LINKING with "not configured to use dynamic
// features": AGP's resource linker treats a second .application module on the classpath as an
// (unconfigured) dynamic-feature split, not a reusable library. A real library module is the
// supported way to share code+resources between two installable apps.
//
// Packages are UNCHANGED from :tv (still `com.arashrahimi46.iptv.data.*` /
// `com.arashrahimi46.iptv.ui.{theme,home,player}`), so this is a pure file relocation -- zero
// import changes were needed in :tv's remaining ui/ code, which references these same-named
// packages transparently across the module boundary.
android {
    namespace = "com.arashrahimi46.iptv.core"
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        minSdk = 23
        // Not a real "app version" (library modules don't have one) -- just here so
        // BuildConfig.VERSION_NAME exists for OpenSubtitlesClient's user-agent string, unchanged
        // from when it lived in :tv.
        buildConfigField("String", "VERSION_NAME", "\"1.0\"")
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        // OpenSubtitlesClient's user-agent string reads BuildConfig.VERSION_NAME -- moved here
        // unchanged from :tv, so :core needs its own generated BuildConfig too.
        buildConfig = true
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material.icons.extended)
    // Are* components ported into :core in the Step 3 migration still render text/glyphs via
    // androidx.tv.material3.Text/Icon (unchanged from :tv -- see the components' own files for
    // why: only the focus/interaction layer was rebuilt on AreInteractive this step, not this).
    implementation(libs.androidx.tv.material)
    implementation(libs.coil.compose)
    implementation(libs.backdrop)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.paging)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.squareup.okhttp)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.common)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.documentfile)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
}
