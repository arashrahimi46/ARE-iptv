plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "com.arashrahimi46.iptv"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.arashrahimi46.iptv"
        minSdk = 23
        //noinspection OldTargetApi
        targetSdk = 36
        versionCode = 7
        versionName = "1.4.2"

    }

    // Release signing is intentionally NOT committed. Populate via env vars or
    // gradle.properties (local, git-ignored) when a real release build is needed:
    //   TV_RELEASE_KEYSTORE_PATH, TV_RELEASE_KEYSTORE_PASSWORD,
    //   TV_RELEASE_KEY_ALIAS, TV_RELEASE_KEY_PASSWORD
    // Debug builds use the AGP-managed default debug keystore automatically -- no config needed.
    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("TV_RELEASE_KEYSTORE_PATH")
                ?: findProperty("TV_RELEASE_KEYSTORE_PATH") as String?
            if (keystorePath != null) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("TV_RELEASE_KEYSTORE_PASSWORD")
                    ?: findProperty("TV_RELEASE_KEYSTORE_PASSWORD") as String?
                keyAlias = System.getenv("TV_RELEASE_KEY_ALIAS")
                    ?: findProperty("TV_RELEASE_KEY_ALIAS") as String?
                keyPassword = System.getenv("TV_RELEASE_KEY_PASSWORD")
                    ?: findProperty("TV_RELEASE_KEY_PASSWORD") as String?
            }
        }
    }

    buildTypes {
        release {
            // R8 full mode: code + resource shrinking and optimization. This -- not the app
            // code -- is the single biggest smoothness win over a debug build (debug Compose
            // runs 2-4x slower for scroll/focus). The Baseline Profile below layers on top.
            optimization {
                enable = true
            }
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Sign with the real release key when it's configured (env vars / gradle.properties),
            // otherwise fall back to the debug key so an optimized build is installable locally
            // (`:tv:installRelease`) to actually FEEL the difference without release credentials.
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

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.tv.foundation)
    implementation(libs.androidx.tv.material)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.paging)
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.squareup.okhttp)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.hls)
    // QA BLOCKER: real Xtream/M3U sources commonly resolve to .mpd (DASH), not just HLS --
    // DefaultMediaSourceFactory needs this on the classpath or it crashes with
    // ClassNotFoundException on DashMediaSource$Factory the moment a DASH stream is selected.
    implementation(libs.androidx.media3.exoplayer.dash)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.common)
    implementation(libs.androidx.security.crypto)
    // SAF (Storage Access Framework) tree/document handling for Live TV Recording (write to any
    // user-picked internal/USB drive via a persistable treeUri, not a File path).
    implementation(libs.androidx.documentfile)
    implementation(libs.coil.compose)
    // Extracts a still frame from a recorded video file so the Recordings list can show thumbnails.
    implementation(libs.coil.video)
    // QR code generation for the "Send feedback" screen — renders the phone-form link on the TV.
    implementation(libs.zxing.core)
    // Crash/ANR reporting + performance monitoring. DSN + auto-init flag live in the manifest;
    // IptvApp tunes release/environment (see SentryAndroid.init there).
    implementation(libs.sentry.android)
    // Product analytics (Firebase/GA4). Stays dormant until a google-services.json is added and the
    // google-services plugin applied — the Analytics wrapper no-ops when no FirebaseApp is configured.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    // Installs the Baseline Profile (generated by :baselineprofile) at app startup so the hot
    // scroll/focus paths are AOT-compiled from first launch instead of JIT-warming.
    implementation(libs.androidx.profileinstaller)
    baselineProfile(project(":baselineprofile"))
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)
    // Only needed so XmlTvParserTest can exercise android.util.Xml.newPullParser() on the
    // JVM (unmocked otherwise) -- see XmlTvParserTest's @RunWith(RobolectricTestRunner::class).
    testImplementation(libs.robolectric)
}
