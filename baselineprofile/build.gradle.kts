plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "com.arashrahimi46.iptv.baselineprofile"
    compileSdk = 36

    defaultConfig {
        // Baseline Profile generation needs API 28+ on the device/emulator running the generator.
        // The generated profile still benefits the app all the way down to its own minSdk.
        minSdk = 28
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // The app whose journeys we profile.
    targetProjectPath = ":tv"
}

// Run the generator on a rooted/AOSP or Play-image emulator (or a real TV) via:
//   ./gradlew :tv:generateReleaseBaselineProfile
// The plugin manages the nonMinifiedRelease variant; results land in tv/src/release/generated.
baselineProfile {
    useConnectedDevices = true
}

dependencies {
    implementation(libs.androidx.junit)
    implementation(libs.androidx.espresso.core)
    implementation(libs.androidx.uiautomator)
    implementation(libs.androidx.benchmark.macro.junit4)
}
