---
name: mobile-performance-guardian
description: Prevents memory leaks, OOM crashes, and performance collapse in image-heavy AI mobile apps. Use for any workflow involving large bitmaps, AI-generated images, OpenCV Mats, video, or background coroutines. Produces bitmap lifecycle rules, downsampling pipelines, Mat-release patterns, coroutine dispatcher policies, caching strategies (memory + disk), and StrictMode + LeakCanary configuration.
icon: gauge
color: Orange
---

# Mobile Performance Guardian

The crash-prevention layer. Every image-heavy feature passes through these rules.

## When to Use

- Adding any feature that processes bitmaps > 1MP
- Adding OpenCV pipelines
- Adding image caching
- Diagnosing `OutOfMemoryError`, jank, ANR
- Pre-release performance audit (paired with `production-readiness-auditor`)

## Bitmap Lifecycle Rules

1. **Downsample at decode**. Always use `BitmapFactory.Options.inSampleSize` for files.
2. **`ARGB_8888` only for display**. Use `RGB_565` for thumbnails when alpha unnecessary.
3. **Release immediately**. Wrap in `use { }`-style helpers; null out references after handoff.
4. **One owner**. Bitmap ownership transfers explicitly — never share across coroutines without a clear owner.
5. **Never store in ViewModel longer than needed**. Use `WeakReference` or content-hash cache.

```kotlin
inline fun <R> Bitmap.useBitmap(block: (Bitmap) -> R): R =
    try { block(this) } finally { if (!isRecycled) recycle() }
```

## OpenCV Mat Rules

```kotlin
inline fun <R> Mat.useMat(block: (Mat) -> R): R =
    try { block(this) } finally { release() }

fun process(input: Bitmap): Bitmap {
    val src = Mat().apply { Utils.bitmapToMat(input, this) }
    return src.useMat { mat ->
        val out = Mat()
        out.useMat {
            Imgproc.cvtColor(mat, out, Imgproc.COLOR_RGBA2GRAY)
            Bitmap.createBitmap(out.cols(), out.rows(), Bitmap.Config.ARGB_8888)
                .also { Utils.matToBitmap(out, it) }
        }
    }
}
```

Never let a `Mat` leak past one function scope.

## Dispatcher Policy

| Work | Dispatcher |
|---|---|
| Network | `Dispatchers.IO` |
| Disk I/O | `Dispatchers.IO` |
| Bitmap decode + resize | `Dispatchers.Default` |
| OpenCV processing | `Dispatchers.Default` |
| UI state updates | `Dispatchers.Main.immediate` |

Use `withContext`, not `launch` for switching contexts inside a use case.

## Coroutine Scope Rules

- `viewModelScope` for ViewModel work — cancels on clear
- One scope per pipeline run — cancellation propagates
- Never use `GlobalScope`
- Never block with `runBlocking` in production code

## Cache Strategy

| Layer | Backed by | Eviction |
|---|---|---|
| L1 — in-memory bitmaps | `LruCache` sized to 1/8 heap | LRU |
| L2 — disk bitmaps | DiskLruCache | LRU + size cap |
| L3 — generated images | Content-hash keyed file cache | TTL + size cap |
| L4 — vision analyses | DataStore JSON, content-hash keyed | TTL |

Cache by content hash, never by session id.

## Memory Budget Heuristics

- One in-flight SDXL output (1024×1024 ARGB_8888) ≈ 4 MB
- Allow at most 3 in-flight bitmaps per pipeline
- For batch processing, serialize — never parallelize bitmap-heavy stages

## Tooling

Enable in debug:

```kotlin
if (BuildConfig.DEBUG) {
    StrictMode.setThreadPolicy(ThreadPolicy.Builder().detectAll().penaltyLog().build())
    StrictMode.setVmPolicy(VmPolicy.Builder().detectAll().penaltyLog().build())
}
```

Add LeakCanary in debug builds only.

## Output

Per micro-task:
- `BitmapExt.kt` and `MatExt.kt` with `useBitmap` / `useMat`
- Updated processing classes with explicit release
- `BitmapCache.kt` with LruCache + content-hash keys
- Dispatcher migration list (files moved off main thread)
- Debug-only StrictMode + LeakCanary wiring

## Anti-Patterns

- Storing `Bitmap` fields in ViewModels indefinitely
- Decoding full-resolution bitmaps for thumbnails
- `GlobalScope` anywhere
- Synchronous OpenCV on main thread
- Sharing a `Bitmap` across coroutines without ownership transfer
- Caching by user session id (defeats content reuse)
- LeakCanary in release builds
