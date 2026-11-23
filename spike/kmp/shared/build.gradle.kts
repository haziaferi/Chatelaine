// :shared -- the one composable both hosts show. Common code only (SPEC D-R14).
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.kotlin.compose)
}

kotlin {
    jvmToolchain(21)

    android {
        namespace = "app.chatelaine.spike.shared"
        compileSdk = 37
        minSdk = 33
    }

    jvm("desktop")

    sourceSets {
        commonMain.dependencies {
            api(libs.compose.runtime)
            api(libs.compose.foundation)
            api(libs.compose.material3)
        }
    }
}
