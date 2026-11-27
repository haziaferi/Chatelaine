// :desktopApp -- the Windows host: one Compose Desktop window.
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.kotlin.compose)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":data"))
    implementation(project(":ui"))
    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutines.swing)
    testImplementation(kotlin("test"))
}

compose.desktop {
    application {
        mainClass = "com.hazia.chatelaine.MainKt"
    }
}

// The stage 1 debug harness (Harness.kt): `./gradlew :desktopApp:harness --args="--data build/harness-data list"`.
tasks.register<JavaExec>("harness") {
    group = "verification"
    description = "Runs the provisional stage 1 debug harness; pass the action with --args."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass = "com.hazia.chatelaine.HarnessKt"
    workingDir = rootProject.projectDir
}
