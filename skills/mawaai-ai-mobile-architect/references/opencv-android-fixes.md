# OpenCV / Native Library Android Fixes

Canonical playbook for MAWAAI's most common stability failures.

## Decision: How to Load OpenCV

Prefer **statically linked** OpenCV (bundled `.aar` with `.so` inside).

```kotlin
class MawaaiApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (!OpenCVLoader.initLocal()) {
            // Fallback or fatal — never silently continue
            Log.e("MAWAAI", "OpenCV initLocal failed")
        }
    }
}
```

Avoid `initAsync` with `OpenCVManager` — deprecated path; users won't have the manager APK.

## abiFilters

In `app/build.gradle.kts`:

```kotlin
android {
    defaultConfig {
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
    }
    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
    }
}
```

Drop `x86` / `x86_64` for production unless emulator distribution required.

## Common Failures

### `UnsatisfiedLinkError: libopencv_java4.so`
- `abiFilters` missing
- `.so` not bundled (OpenCV `.aar` not in deps or `implementation` vs `api` mistake)
- ProGuard stripped `org.opencv.**` — add keep rule

```proguard
-keep class org.opencv.** { *; }
-keep class org.opencv.android.** { *; }
```

### `Mat` constructors returning empty
- Bitmap config not `ARGB_8888` — convert before `Utils.bitmapToMat`
- Bitmap recycled before use

### Native crash inside processing
- Running on main thread — move to `Dispatchers.Default` or `Dispatchers.IO`
- Mat not released — wrap in `use { }` or release explicitly in `finally`

### Build fails: `Duplicate class org.opencv.*`
- Two OpenCV deps. Pick one (official `org.opencv:opencv` or a vendored AAR), exclude the other.

### Kotlin-OpenCV interop crashes
- Always check `Mat.empty()` before ops
- Never pass nullable `Bitmap` into JNI — assert non-null at the boundary

## Verification After Fix

1. Clean build: `./gradlew clean assembleDebug`
2. Install + launch — confirm `OpenCV initLocal` succeeds in logcat
3. Run one minimal Mat op (e.g., `Mat(100, 100, CvType.CV_8UC4)`) at startup behind a debug flag
4. Confirm APK contains `.so` for each ABI: `unzip -l app-debug.apk | grep libopencv_java4`
