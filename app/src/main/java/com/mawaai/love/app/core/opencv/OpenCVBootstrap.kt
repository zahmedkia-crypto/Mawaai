package com.mawaai.love.app.core.opencv

import android.content.Context
import android.os.Build
import android.util.Log
import org.opencv.android.OpenCVLoader
import org.opencv.core.Mat
import java.io.File

/**
 * Process-wide OpenCV initialization gate.
 *
 * Why this exists: the AAR `org.opencv:opencv:4.9.0` ships `libopencv_java4.so`,
 * but the native library has to be `dlopen`-ed before *any* `Mat`/`Imgproc`/`Core`
 * call. Previously this happened lazily inside `AIEngineImpl.ensureInit()`, which
 * meant the path `Canvas → "Pick Template" → TemplateGalleryViewModel.apply →
 * TemplateCompositor.compose → PerspectiveWarpProcessor.warp → Mat()` ran *before*
 * any AIEngine call site and crashed with `UnsatisfiedLinkError: Mat.n_Mat`.
 *
 * The bootstrap is invoked eagerly from `MawaaiApp.onCreate()` and is also
 * called at the entry of every processor that touches an `org.opencv.core.Mat`,
 * so direct callers (TemplateCompositor, GarmentColorEngine, …) cannot bypass
 * initialization. The call is idempotent and synchronized; subsequent invocations
 * return the cached boolean without touching JNI again.
 *
 * On a hard failure (`UnsatisfiedLinkError`, missing ABI, broken AAR), the
 * bootstrap flips `available = false`, every processor returns its input
 * untouched, and the user sees a graceful quality degradation instead of a
 * crash. Treat the boolean as a feature flag, not an assertion.
 *
 * Phase 17 hardening (2026-05-17):
 *  1. **Direct `System.loadLibrary("opencv_java4")` BEFORE `initLocal()`.** The
 *     loader's reflection-based path can silently mismatch the bundled `.so`
 *     against the Java bindings on some 4.9.0 Maven Central distributions,
 *     leaving `Mat.n_Mat()` un-bound. Loading the library directly first forces
 *     the linker to register the JNI symbols against the `Mat` class before
 *     `initLocal()` does anything Java-side.
 *  2. **Synchronous `Mat()` smoke test** after `initLocal()` returns true. If
 *     the loader claims success but the smoke test throws `UnsatisfiedLinkError`,
 *     we flip `available = false` here instead of crashing on the first feature
 *     call. The cause is logged for triage.
 *  3. **Diagnostic logging** at startup: dumps `Build.SUPPORTED_ABIS` and every
 *     `.so` actually present in `applicationInfo.nativeLibraryDir`. The two
 *     together let a logcat snapshot pinpoint a missing-ABI vs. broken-AAR
 *     vs. JNI-version-mismatch failure mode without needing a debugger.
 */
object OpenCVBootstrap {

    private const val TAG = "OpenCVBootstrap"
    private const val NATIVE_LIB = "opencv_java4"

    @Volatile private var initialized: Boolean = false
    @Volatile private var available: Boolean = false

    /**
     * Initializes OpenCV with full diagnostic logging. Called once from
     * [com.mawaai.love.app.MawaaiApp.onCreate]. Returns true iff OpenCV is
     * usable for the rest of the process lifetime.
     *
     * The [context] is used only to enumerate the actual `.so` files present
     * under `applicationInfo.nativeLibraryDir` for diagnostics — it is NOT
     * retained beyond this call.
     */
    @Synchronized
    fun init(context: Context): Boolean {
        if (initialized) return available
        logEnvironment(context)
        available = loadAndVerify()
        initialized = true
        return available
    }

    /**
     * Loads `libopencv_java4.so` if it has not been loaded yet. Returns true
     * iff OpenCV is usable for the rest of the process lifetime. Safe to call
     * from any thread, any number of times.
     *
     * Defensive late-init path: if a processor calls this before
     * `MawaaiApp.onCreate()` has wired [init], we still attempt the load —
     * just without the per-startup diagnostic dump. The verification step
     * (System.loadLibrary + initLocal + Mat smoke test) is identical.
     */
    @Synchronized
    fun ensureLoaded(): Boolean {
        if (initialized) return available
        Log.w(TAG, "ensureLoaded() called before init(context); running context-less fallback")
        available = loadAndVerify()
        initialized = true
        return available
    }

    /** Cached availability flag. Returns false until [init] or [ensureLoaded] has run. */
    val isAvailable: Boolean get() = initialized && available

    /**
     * Three-step verified load:
     *  1. `System.loadLibrary("opencv_java4")` — direct JNI symbol registration.
     *     Bypasses `OpenCVLoader`'s reflection path which has been observed to
     *     return "success" without actually wiring up `Mat.n_Mat()` on certain
     *     4.9.0 Maven Central builds.
     *  2. `OpenCVLoader.initLocal()` — sets up the Java-side state OpenCVManager
     *     used to provide on older versions; harmless idempotent on top of (1).
     *  3. Synchronous `Mat()` allocation + release. The single most reliable
     *     way to confirm the JNI bindings are actually live: a `Mat` ctor goes
     *     straight to `n_Mat()`. If this throws, the previous "success" claims
     *     were lying and we mark OpenCV unavailable for the rest of the session.
     */
    private fun loadAndVerify(): Boolean {
        val direct = runCatching { System.loadLibrary(NATIVE_LIB) }
        if (direct.isFailure) {
            Log.e(TAG, "System.loadLibrary($NATIVE_LIB) failed", direct.exceptionOrNull())
            return false
        }
        Log.i(TAG, "System.loadLibrary($NATIVE_LIB) = OK")

        val initOk = try {
            val ok = OpenCVLoader.initLocal()
            Log.i(TAG, "OpenCVLoader.initLocal() = $ok")
            ok
        } catch (t: UnsatisfiedLinkError) {
            Log.e(TAG, "OpenCVLoader.initLocal() threw UnsatisfiedLinkError", t)
            false
        } catch (t: Throwable) {
            Log.e(TAG, "OpenCVLoader.initLocal() threw", t)
            false
        }
        if (!initOk) return false

        return runCatching {
            val mat = Mat()
            val rows = mat.rows()
            mat.release()
            Log.i(TAG, "Mat() smoke test passed (rows=$rows)")
            true
        }.getOrElse { t ->
            Log.e(TAG, "Mat() smoke test FAILED despite initLocal=true — JNI binding mismatch", t)
            false
        }
    }

    /**
     * Dumps the device ABI list and every `.so` actually present in the APK's
     * native library dir. The two together pinpoint a missing-ABI failure
     * (`Build.SUPPORTED_ABIS = [arm64-v8a]` but `nativeLibraryDir` only contains
     * an `armeabi-v7a` build) or a broken-AAR failure (`nativeLibraryDir` is
     * empty for the device's ABI but the `abiFilters` claims it should be
     * there).
     */
    private fun logEnvironment(context: Context) {
        Log.i(TAG, "Build.SUPPORTED_ABIS = ${Build.SUPPORTED_ABIS.joinToString()}")
        val nativeDir = context.applicationInfo.nativeLibraryDir
        Log.i(TAG, "applicationInfo.nativeLibraryDir = $nativeDir")
        runCatching {
            val files = File(nativeDir).listFiles()
            if (files.isNullOrEmpty()) {
                Log.w(TAG, "nativeLibraryDir is empty — APK ships no .so files for this ABI")
            } else {
                files.forEach { Log.i(TAG, "  native lib: ${it.name} (${it.length()} bytes)") }
            }
        }.onFailure { Log.w(TAG, "Could not enumerate $nativeDir", it) }
    }
}
