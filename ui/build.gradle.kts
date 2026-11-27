// :ui -- what both apps draw: strings, navigation, and (from later tasks) theme, components and screens.
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.kotlin.compose)
}

kotlin {
    jvmToolchain(21)

    android {
        namespace = "com.hazia.chatelaine.ui"
        compileSdk = libs.versions.compileSdk.get().toInt()
        minSdk = libs.versions.minSdk.get().toInt()
    }

    jvm("desktop")

    sourceSets {
        commonMain.dependencies {
            api(project(":data"))
            api(libs.compose.runtime)
            api(libs.compose.ui)
            api(libs.compose.foundation)
            // Navigator exposes StateFlow.
            api(libs.kotlinx.coroutines.core)
        }
        getByName("desktopTest").dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.compose.ui.test)
            implementation(compose.desktop.currentOs)
        }
    }
}
