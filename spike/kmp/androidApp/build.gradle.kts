// :androidApp -- the Android host: one Activity that shows :shared's screen.
// AGP 9 compiles Kotlin itself, so no Kotlin Android plugin is applied.
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "app.chatelaine.spike"
    compileSdk = 37

    defaultConfig {
        applicationId = "app.chatelaine.spike"
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = "0.0.1"
    }

    kotlin {
        jvmToolchain(21)
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.androidx.activity.compose)
}
