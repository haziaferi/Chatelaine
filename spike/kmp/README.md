# Stage 0 KMP spike

Throwaway proof that one Compose Multiplatform screen runs on Android (minSdk 33) and on Windows (SPEC R13, R102). Not the app's layout; stage 1 starts its own project.

- Desktop: `./gradlew :desktopApp:run`
- Android: `./gradlew :androidApp:assembleDebug`, then install `androidApp/build/outputs/apk/debug/androidApp-debug.apk`

Needs a git-ignored `local.properties` with `sdk.dir` set to the Android SDK. Results, versions and build times: [`docs/evidence/stage0-kmp.md`](../../docs/evidence/stage0-kmp.md).
