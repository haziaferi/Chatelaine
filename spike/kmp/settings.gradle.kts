// Stage 0 KMP spike (SPEC O-R102): throwaway, kept out of the app's future layout.
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "chatelaine-kmp-spike"
include(":shared", ":androidApp", ":desktopApp")
