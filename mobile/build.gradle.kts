plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    // namespace intentionally differs from :tv's "com.arashrahimi46.iptv" -- :mobile depends on
    // :tv (reuses its data layer as-is), and AGP dexes both modules' R classes into one APK. Same
    // namespace on both would generate two classes with the identical FQN
    // (com.arashrahimi46.iptv.R) and fail with a duplicate-class error at merge time. applicationId
    // is untouched (still collides with :tv's -- flagged separately to release-manager as a
    // packaging/store concern, not a compile-time one).
    namespace = "com.arashrahimi46.iptv.mobile"
    // Matches :tv's compileSdk: :tv's `backdrop` (glass blur) dependency's AAR metadata requires
    // compileSdk 37+ from every module on the classpath, including consumers like :mobile.
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        // MUST differ from :tv's "com.arashrahimi46.iptv" -- this isn't just a future Play Store
        // collision (as originally flagged to release-manager): with an *identical* applicationId,
        // AGP's resource linker treats :mobile as if it were a dynamic-feature split of :tv and
        // processDebugResources hard-fails ("not configured to use dynamic features"). Sharing an
        // id across two independent .application modules isn't supported at all, so this had to
        // be resolved locally to get ANY build; release-manager still owns the real Play Store
        // identity/signing decision for the final applicationId.
        applicationId = "com.arashrahimi46.iptv.mobile"
        // 26 (Android 8.0/Oreo), not :tv's 36 -- a phone app needs real device-market reach, and
        // PictureInPictureParams (real Android PiP, per the v1 spec) requires API 26 anyway.
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Release signing is intentionally NOT committed, mirroring :tv's convention. Populate via
    // env vars or gradle.properties (local, git-ignored) when a real release build is needed:
    //   MOBILE_RELEASE_KEYSTORE_PATH, MOBILE_RELEASE_KEYSTORE_PASSWORD,
    //   MOBILE_RELEASE_KEY_ALIAS, MOBILE_RELEASE_KEY_PASSWORD
    // Separate var names from :tv's TV_RELEASE_* because :mobile ships as its own Play Store
    // listing (applicationId com.arashrahimi46.iptv.mobile) with its own upload key -- reusing
    // :tv's keystore/alias here would be silently wrong if the two apps end up on different keys.
    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("MOBILE_RELEASE_KEYSTORE_PATH")
                ?: findProperty("MOBILE_RELEASE_KEYSTORE_PATH") as String?
            if (keystorePath != null) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("MOBILE_RELEASE_KEYSTORE_PASSWORD")
                    ?: findProperty("MOBILE_RELEASE_KEYSTORE_PASSWORD") as String?
                keyAlias = System.getenv("MOBILE_RELEASE_KEY_ALIAS")
                    ?: findProperty("MOBILE_RELEASE_KEY_ALIAS") as String?
                keyPassword = System.getenv("MOBILE_RELEASE_KEY_PASSWORD")
                    ?: findProperty("MOBILE_RELEASE_KEY_PASSWORD") as String?
            }
        }
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
            // Sign with the real release key when configured, otherwise fall back to the debug
            // key so a release build is still installable locally without release credentials.
            val releaseSigning = signingConfigs.getByName("release")
            signingConfig = if (releaseSigning.storeFile != null) releaseSigning else signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // The shared data layer (data/repository, data/db, data/parser, data/model, data/settings,
    // data/player, CredentialsStore), extracted from :tv into :core -- NOT a direct
    // implementation(project(":tv")) as originally planned. :tv is a com.android.application
    // module, and depending on another .application module compiles fine but breaks AAPT2
    // resource linking ("This application is not configured to use dynamic features" -- AGP
    // treats a second .application on the classpath as an unconfigured dynamic-feature split).
    // :core is a proper com.android.library, which is the supported way to share code between
    // two installable apps. See core/build.gradle.kts's top comment for the full story.
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

    // Needed to compile against :tv's public data-layer API surface (Room entities/DAOs, DataStore
    // settings, encrypted credentials, parsers, player helpers) -- implementation deps on :tv don't
    // transit here automatically.
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.paging)
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

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
