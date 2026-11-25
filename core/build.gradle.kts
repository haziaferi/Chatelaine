plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
}

kotlin {
    jvmToolchain(21)

    android {
        namespace = "com.hazia.chatelaine.core"
        compileSdk = libs.versions.compileSdk.get().toInt()
        minSdk = libs.versions.minSdk.get().toInt()
        withHostTest {}
    }

    jvm("desktop")

    applyDefaultHierarchyTemplate()

    sourceSets {
        // Android and the desktop share the JVM: code that needs only the JDK lives here once.
        val jvmShared = create("jvmShared") { dependsOn(commonMain.get()) }
        androidMain { dependsOn(jvmShared) }
        getByName("desktopMain") { dependsOn(jvmShared) }

        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
