// :androidApp -- the Android host. AGP 9 compiles Kotlin itself, so no Kotlin Android plugin is applied.
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.hazia.chatelaine"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.hazia.chatelaine"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "0.0.1"
    }

    kotlin {
        jvmToolchain(21)
    }
}

dependencies {
    implementation(project(":data"))
    implementation(libs.compose.foundation)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.work.runtime.ktx)
    testImplementation(libs.junit)
}
