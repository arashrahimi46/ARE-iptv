plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
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
        minSdk = 36
        targetSdk = 36
        versionCode = 3
        versionName = "1.2.0"

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
            signingConfig = signingConfigs.getByName("release")
            optimization {
                enable = false
            }
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
    implementation(libs.coil.compose)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)
    // Only needed so XmlTvParserTest can exercise android.util.Xml.newPullParser() on the
    // JVM (unmocked otherwise) -- see XmlTvParserTest's @RunWith(RobolectricTestRunner::class).
    testImplementation(libs.robolectric)
}
