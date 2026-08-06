plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.baselineprofile)
    // Reads tv/google-services.json and generates the Firebase config resources. Applying this is
    // what wakes up the otherwise-dormant Analytics facade: with a real FirebaseApp on the classpath
    // its null check passes and events start flowing (see analytics/Analytics.kt).
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.arashrahimi46.iptv"
    compileSdk {
        // 37 is required by io.github.kyant0:backdrop (AAR metadata gate), not a
        // preference -- see docs/glass-v2-design.md §4. targetSdk stays 36.
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        // Play Store identity, and permanent once published -- it cannot be changed for the life of
        // the listing. Deliberately NOT the same as `namespace` above: namespace is the internal
        // Kotlin/R package (277 source files), which no store surface ever shows, so rebranding the
        // product only requires this line. Suffixed `.tv` because :mobile ships as its own listing
        // (com.areiptv.mobile) -- two .application modules cannot share an applicationId.
        applicationId = "com.areiptv.tv"
        minSdk = 23
        //noinspection OldTargetApi
        targetSdk = 36
        versionCode = 12
        versionName = "1.8.1"

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
        // See the afterEvaluate block at the bottom of this file: `nonMinifiedRelease` has to have
        // R8 turned off there, not here.
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
    // :core is a resources-ONLY library: the 24 locale strings.xml files, nothing else. It exists
    // so :tv and :mobile add a user-facing string in one place instead of two. Do NOT put Kotlin
    // back in here -- the previous shared-code :core made every :mobile change a :tv regression
    // risk, which is why it was unwound. :tv references these as `com.arashrahimi46.iptv.core.R`
    // (AGP's non-transitive R class), aliased to CoreR in the three files that also use :tv's own
    // R.drawable/R.font.
    implementation(project(":core"))

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
    // Liquid-glass optics (blur + vibrancy + lens) as tested AGSL -- see docs/glass-v2-design.md §4.
    implementation(libs.backdrop)
    // QR code generation for the "Send feedback" screen — renders the phone-form link on the TV.
    implementation(libs.zxing.core)
    // Crash/ANR reporting + performance monitoring. DSN + auto-init flag live in the manifest;
    // IptvApp tunes release/environment (see SentryAndroid.init there).
    implementation(libs.sentry.android)
    // Product analytics (Firebase/GA4). Stays dormant until a google-services.json is added and the
    // google-services plugin applied — the Analytics wrapper no-ops when no FirebaseApp is configured.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    // Remote Config backs the kill switch in config/RemoteFlags.kt. Same dormancy contract as
    // Analytics: with no FirebaseApp it simply never activates and the baked-in defaults win.
    implementation(libs.firebase.config)
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

/*
 * Turn R8 OFF for the two variants the Baseline Profile is generated from.
 *
 * `finalizeDsl` is load-bearing here -- it is the one hook that runs in the right window, and both
 * obvious alternatives miss it:
 *
 *   - `buildTypes { all { ... } }` runs too EARLY. The baselineprofile plugin creates
 *     `nonMinifiedRelease`/`benchmarkRelease` with `initWith(release)`, which copies release's
 *     `optimization.enable = true` straight back over anything set there. Probed: the `all` block
 *     did set false, and by afterEvaluate the value was true again.
 *   - `project.afterEvaluate { }` runs too LATE. AGP registers its own DSL finalization when it is
 *     applied -- i.e. before this file's callback -- so by the time ours runs the value is already
 *     locked and Gradle reports every task UP-TO-DATE.
 *
 * `finalizeDsl` sits between the two: after all DSL configuration (so the plugin's copy has already
 * happened) and before variants are created (so AGP still reads it).
 *
 * Note AGP 9 has TWO switches and only one of them matters: `release` reports
 * `isMinifyEnabled == false` yet is fully minified, because `optimization { enable }` is what now
 * drives R8. The legacy flag the baselineprofile plugin clears is the one with no effect, which is
 * why the plugin does not get this right on its own.
 *
 * Getting it wrong fails silently rather than loudly, which is how it survived this long: R8 ran on
 * the profiled variant, the collected rules came back as obfuscated names (`La0;`) from a mapping
 * the shipped build does not share, and every one was dropped when merged into the release profile.
 * What shipped had 204 app rules -- 193 Room DAOs (whose names R8 keeps) and 8 MainActivity entries
 * -- and ZERO composable, theme, focus or tile code, so nothing the UI actually spends its first
 * paint in was AOT-compiled. The profile must be collected UNMINIFIED; R8 rewrites it through the
 * release mapping when the shipping APK is built.
 */
androidComponents {
    finalizeDsl { ext ->
        listOf("nonMinifiedRelease", "benchmarkRelease").forEach { variant ->
            ext.buildTypes.findByName(variant)?.optimization?.enable = false
        }
    }
}
