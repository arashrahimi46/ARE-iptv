plugins {
    // LIBRARY, not application. The phone UI ships *inside* :tv's single AAB (applicationId
    // com.areiptv.tv) rather than as a second Play listing -- see tv/build.gradle.kts and the
    // "One app, two form factors" section of CLAUDE.md for why. A library has no applicationId,
    // no versionCode/versionName and no signingConfig; :tv owns all four.
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    // Every source file in this module lives under this namespace, and that is load-bearing now
    // that :tv and :mobile dex into ONE APK: both modules own a full, independent copy of the data
    // layer (deliberately -- see CLAUDE.md), and those copies used to sit in the *identical* Kotlin
    // packages (com.arashrahimi46.iptv.data.*, .ui.theme). Two separate APKs never cared; one APK
    // is a duplicate-class failure at dex time. So :mobile's copies were moved under
    // com.arashrahimi46.iptv.mobile.* (the token layer landed in .mobile.design, because
    // .mobile.ui.theme already existed and both Type.kt files declare AreIptvTypographyDefault).
    // Keep it that way: nothing in this module may sit in a package :tv also uses.
    namespace = "com.arashrahimi46.iptv.mobile"
    // Matches :tv's compileSdk: :tv's `backdrop` (glass blur) dependency's AAR metadata requires
    // compileSdk 37+ from every module on the classpath, including consumers like :mobile.
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        // 26 = Android 8.0/Oreo, required by PictureInPictureParams (real Android PiP, per the v1
        // spec). A library cannot require a HIGHER minSdk than the app that consumes it, so :tv's
        // minSdk was raised 23 -> 26 to match. That is the deliberate cost of one-app packaging.
        minSdk = 26

        // An android-library's BuildConfig has no VERSION_NAME field -- that only exists for
        // application modules. AboutSettingsScreen (the version row) and OpenSubtitlesClient (its
        // User-Agent) both need it, so it is injected from the same gradle.properties value :tv
        // reads for its versionName. One source of truth: bump `areVersionName` there, not here.
        buildConfigField(
            "String",
            "VERSION_NAME",
            "\"${providers.gradleProperty("areVersionName").get()}\"",
        )

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // No signingConfigs and no bundleRelease guard here any more: a library produces no installable
    // artifact and nothing to sign. The phone UI reaches Play inside :tv's AAB, so :tv's TV_RELEASE_*
    // guard is the one that protects the upload. The MOBILE_RELEASE_* keystore
    // (~/.android/keystores/are-iptv-mobile-upload.jks) is now unused -- there is no second listing.

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    // :core is now RESOURCES ONLY -- the 24 locale strings.xml files, no code. :mobile owns its
    // full source tree (data layer + Are* components moved in from the old shared :core), so a
    // change here can no longer regress :tv. See core/build.gradle.kts for why that was unwound.
    implementation(project(":core"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    // AppCompatDelegate.setApplicationLocales -- per-app language override, used by
    // LanguageSelectScreen the same way :tv's does.
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    // The data layer now lives in this module (data/db, data/repository, data/parser,
    // data/settings, data/player, data/recording), so :mobile owns Room's annotation processor
    // and schema output too -- not just the runtime artifacts it needed as a :core consumer.
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.paging)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.security.crypto)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.squareup.okhttp)
    implementation(libs.coil.compose)

    // Player (Phase 2): portrait+landscape ExoPlayer with real Android PiP.
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.hls)
    implementation(libs.androidx.media3.exoplayer.dash)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.common)

    // Came in with the components/data layer that moved out of the old shared :core: glass surfaces
    // use `backdrop`, and the recording/import paths use DocumentFile. androidx.tv.material is
    // deliberately absent -- :mobile's Are* components are touch-first androidx.compose.material3.
    implementation(libs.backdrop)
    implementation(libs.androidx.documentfile)

    testImplementation(libs.junit)
    // The 17 data-layer unit tests moved in from :core (parsers, EPG matching, PIN hashing)
    // are Robolectric-backed.
    testImplementation(libs.robolectric)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
