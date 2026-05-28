# Mawaai — Developer Setup

Last updated for **MT-021** (2026-05-28).

## Prerequisites

- Android Studio **Hedgehog (2023.1.1)** or newer — bundles a Java 21 JBR.
- Git, GitHub access to `zahmedkia-crypto/Mawaai`.
- Android SDK platform-tools 34, build-tools 34.x.

The project pins:

- Kotlin **2.1.0**
- AGP **8.7.3**
- `jvmTarget = 17`, `jvmToolchain(21)`
- `compileSdk = 34`, `minSdk = 26`, `targetSdk = 34`

Gradle will resolve a Java 21 toolchain automatically via the `org.gradle.java.installations.auto-*` settings in `gradle.properties`. On a fresh machine the first build downloads the toolchain (one-time cost).

## API keys (`local.properties`)

The build pulls AI keys from `local.properties` via `BuildConfig` fields. Create the file at the **repo root** (it is `.gitignore`'d) with whatever subset of keys you have:

```properties
GEMINI_API_KEY=...
HUGGINGFACE_API_KEY=...
CLOUDFLARE_ACCOUNT_ID=...
CLOUDFLARE_API_TOKEN=...
REMOVE_BG_API_KEY=...
OPENROUTER_API_KEY=...
GROQ_API_KEY=...
PEXELS_API_KEY=...     # currently unused; see MT-PEXELS-001
```

If a key is missing the corresponding feature degrades gracefully (the client classes guard on `BuildConfig.<KEY>.isNotBlank()`).

### Release signing (optional)

If you need to produce a signed release build, also add:

```properties
RELEASE_STORE_FILE=path/to/keystore.jks
RELEASE_STORE_PASSWORD=...
RELEASE_KEY_ALIAS=...
RELEASE_KEY_PASSWORD=...
```

Without these, `./gradlew :app:assembleRelease` falls back to the debug signing config (see `app/build.gradle.kts`).

## Pinning a custom JDK (rare)

**Don't edit the repo's `gradle.properties`.** If Android Studio's bundled JBR is broken on your machine, override it in your *user-level* Gradle properties:

| OS | File path |
|---|---|
| Windows | `%USERPROFILE%\.gradle\gradle.properties` |
| macOS | `~/.gradle/gradle.properties` |
| Linux | `~/.gradle/gradle.properties` |

Example contents (Windows):

```properties
org.gradle.java.home=C:/Program Files/Android/Android Studio/jbr
```

This keeps the repo portable while still letting any individual developer pin a working JDK.

## Common build commands

```bash
./gradlew :app:assembleDebug          # Build debug APK
./gradlew :app:installDebug           # Install on connected device
./gradlew test                        # JVM unit tests
./gradlew connectedDebugAndroidTest   # Instrumented tests (requires device/emulator)
./gradlew lint                        # Lint analysis
```

## CI

CI runners do not require Android Studio — they just need a JDK and the Android command-line SDK tools. With the MT-021 settings in this PR, Gradle's toolchain provisioner will download Java 21 itself if it isn't already on the runner.

## Troubleshooting

- **`Could not find a Java toolchain matching JavaLanguageVersion[21]`**
  Either set `JAVA_HOME` to a JDK 21 install or trust auto-download (default).
- **`Cannot run jlink.exe`** on Windows
  Your bundled JBR is corrupt. Pin a working JDK 21 in your user-level `gradle.properties` (see above).
- **`SDK location not found`**
  Create `local.properties` and add `sdk.dir=...` to your Android SDK install.
