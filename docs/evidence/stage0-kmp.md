# Stage 0 KMP spike

Evidence for `SPEC.md`, not a source of rules. The rules that rely on it are R13, R14, R100 and R102 in `SPEC.md`. Run on 2026-11-23 on the owner's Windows 11 PC.

## Verdict

R102's KMP acceptance holds: one Compose Multiplatform screen, written once in common code, runs in a window on Windows and in an Activity on Android, from an app built with minSdk 33 (R13). It ran on the emulator (Android 16, API 36) and on a real phone (Android 14, API 34). The screenshots were taken and are kept outside the repository. The button counts taps everywhere, so the shared state works as well as the drawing.

R102 also asks for the model spike timings; those are recorded in `stage0-recognition.md` and are not part of this run.

## What was built

A throwaway project in `spike/kmp/`, kept apart from the app's future layout:

| Module | Holds |
|---|---|
| `shared` | `SpikeScreen`, the one composable: a title, a line naming the platform, a tap counter. Common code only, no platform source sets (R14). |
| `androidApp` | One Activity that calls `SpikeScreen`. minSdk 33, targetSdk 36, compileSdk 37. |
| `desktopApp` | One Compose Desktop `Window` that calls `SpikeScreen`. |

AGP 9 does not allow the Android application plugin inside a Kotlin Multiplatform module, so the Android host is its own module over a `com.android.kotlin.multiplatform.library` shared module. Stage 1 will need the same three-part layout.

## Toolchain

| Tool | Version |
|---|---|
| Kotlin | 2.4.10 |
| Compose Multiplatform | 1.12.0 (Material 3 1.9.0) |
| Android Gradle Plugin | 9.3.2 |
| Gradle | 9.7.1 |
| JDK | Temurin 21.0.11 (Gradle toolchain 21) |
| Android SDK | compileSdk 37, build-tools 37.0.0 |
| Emulator | 37.1.11, AVD `Medium_Phone`: API 36 (Android 16), Google Play x86_64 image |

Every version was already in the PC's Gradle cache, so all builds ran with `--offline`: the times below are compile times, not download times.

## Build and run times

Every build ran in a single-use Gradle daemon (the PC's Gradle settings turn the daemon off), so the times include starting Gradle. The first two builds ran one after the other in a fresh project, with `--no-build-cache`, so every task they list ran. The desktop one came before the build files named the Compose libraries directly; the versions resolved were the same. The third rebuilt the committed project with every task rerun, and showed no warnings, deprecations included.

| Step | Time |
|---|---|
| `gradlew --offline --no-build-cache :desktopApp:jar`, fresh project | 67 s (10 tasks) |
| `gradlew --offline --no-build-cache :androidApp:assembleDebug`, right after | 100 s (46 tasks); debug APK 29 MB |
| `gradlew --offline --rerun-tasks :desktopApp:jar :androidApp:assembleDebug`, both together | 96 s |
| `gradlew --offline :desktopApp:run`, command to window on screen | 49 s (recompiling `shared` after a build-file change) |
| Emulator start, quick boot from its snapshot | 21 s |
| Android cold start, first launch just after the snapshot restore | 12.4 s, and one ANR while the emulator was still busy |
| Android cold start, emulator settled (2 runs) | 3.1 s and 3.2 s |
| Android cold start, real phone on Android 14 (3 runs) | 0.50 to 0.56 s |

The first launch's ANR came while the restored emulator was still settling; the app itself never crashed (no app entry in the crash log), and every later launch was clean. The only crash in the log is the emulator's own Bluetooth service.

## Running it

- Desktop: `./gradlew :desktopApp:run` in `spike/kmp/`.
- Android: `./gradlew :androidApp:assembleDebug`, then `adb install` the APK from `androidApp/build/outputs/apk/debug/`.- `local.properties` (git-ignored) must name the Android SDK with `sdk.dir`, in forward slashes.
- On this PC the emulator quits at boot with "too many emulator instances are running" when started with its default ports. Windows reserves TCP ports 5495 to 5594 (its dynamic range starts at 1024, and Hyper-V's reservations fall there), which covers the emulator's whole default range of 5554 to 5585. Starting it with `-port 5600` and then `adb connect localhost:5601` works, without changing any system setting.

## Not covered

- The app ran on API 34 and 36. minSdk 33 is declared and confirmed in the built APK, but no API 33 device or image was run: none is installed, and adding one costs disk the owner decides on.
- The phone run was a launch and a tap, not a walk: R100 asks for device walks from stage 2 on.
- The desktop app was run from Gradle, not packaged as an installer.
- R14 names the platform code allowed (camera, file access, widgets, speech, model runtimes, printing). The hosts' entry points (an Activity, a `main` with a window) are also platform code, kept here in host modules outside `shared`. Whether R14 should name them is a question for the design pass.
