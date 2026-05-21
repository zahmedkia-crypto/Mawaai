package com.mawaai.love.app.design.ai.processors

import org.opencv.core.Mat

/**
 * Accumulates OpenCV [Mat] handles allocated within a [matScope] block and
 * releases their native buffers when the block exits — on success *or* on
 * exception. Without this, every OpenCV call site had to chain `.release()`
 * on the happy path only, so a thrown error mid-pipeline would leak native
 * memory that Kotlin's GC never reclaims.
 */
internal class MatScope {
    private val mats = ArrayList<Mat>()

    /** Registers [mat] for auto-release and returns it so calls can chain. */
    fun <M : Mat> take(mat: M): M {
        mats += mat
        return mat
    }

    fun releaseAll() {
        for (m in mats) runCatching { m.release() }
        mats.clear()
    }
}

internal inline fun <T> matScope(block: MatScope.() -> T): T {
    val scope = MatScope()
    try {
        return scope.block()
    } finally {
        scope.releaseAll()
    }
}

/**
 * `dst = 1 - src`, per channel, preserving src's type. OpenCV 4.9.0 does
 * not expose `Core.subtract(Scalar, Mat, Mat)`, so the natural `1 - x`
 * for a float Mat goes through `convertTo` with `alpha = -1, beta = 1`
 * instead.
 *
 * Centralized here after the 2026-05-13 audit found three independent
 * inline copies of this trick across `BlendModeProcessor` and
 * `GarmentColorEngine`. Future blend / mask math should call this helper
 * rather than re-rolling the convertTo signature.
 */
internal fun complement(src: Mat, dst: Mat) {
    src.convertTo(dst, -1, -1.0, 1.0)
}
