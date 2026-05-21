package com.mawaai.love.app.design.ai.processors

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import com.mawaai.love.app.design.ai.ModelMissingException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Delegate
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.nnapi.NnApiDelegate
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

/**
 * ESRGAN-based super-resolution. The shipped TFLite model accepts a fixed
 * [TILE_INPUT]×[TILE_INPUT] tile and produces a [TILE_OUTPUT]×[TILE_OUTPUT]
 * tile ([SCALE]×). For arbitrary-sized inputs we tile across the image with
 * [STRIDE]-pixel steps (8-pixel overlap), run inference per tile, and blend
 * overlapping output regions with linear-edge feathering so the seams are
 * invisible. The resulting bitmap is [SCALE]× the input dimensions and
 * preserves aspect ratio. NNAPI delegate is attempted first; failures fall
 * back transparently to the CPU interpreter.
 */
@Singleton
class SuperResolutionProcessor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var interpreter: Interpreter? = null
    private var nnApiDelegate: NnApiDelegate? = null
    private var initFailed = false

    private fun ensureInterpreter() {
        if (initFailed || interpreter != null) return
        try {
            interpreter = buildInterpreter(loadModel())
        } catch (t: Throwable) {
            initFailed = true
            interpreter?.close(); interpreter = null
            nnApiDelegate?.close(); nnApiDelegate = null
            throw ModelMissingException("esrgan")
        }
    }

    private fun buildInterpreter(model: MappedByteBuffer): Interpreter {
        return runCatching {
            val nn = NnApiDelegate()
            val options = Interpreter.Options().apply { addDelegate(nn as Delegate) }
            nnApiDelegate = nn
            Interpreter(model, options)
        }.getOrElse {
            Log.w(TAG, "NNAPI delegate unavailable for ESRGAN; using CPU", it)
            nnApiDelegate?.close(); nnApiDelegate = null
            Interpreter(model)
        }
    }

    private fun loadModel(): MappedByteBuffer {
        val fd = context.assets.openFd("models/$MODEL_NAME")
        FileInputStream(fd.fileDescriptor).use { fis ->
            return fis.channel.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
        }
    }

    /**
     * Returns a fresh bitmap that is [SCALE]× the input dimensions. Caller owns
     * the result. The original [input] is not consumed. Tiles smaller than the
     * model's minimum are bicubic-upscaled then re-tiled.
     */
    suspend fun upscale(input: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        ensureInterpreter()
        val tflite = interpreter ?: throw ModelMissingException("esrgan")

        val sanitized = ensureSrTilable(input)
        // Guarantees the upscaled intermediate is freed on any exception path
        // (tile inference, accumulation, pixel composition), not just success.
        try {
            val w = sanitized.width
            val h = sanitized.height
            val outW = w * SCALE
            val outH = h * SCALE

            // Cap output area so a 64 MP+ allocation never lands on a low-RAM device.
            if (outW.toLong() * outH > MAX_OUTPUT_PIXELS) {
                val scaledDown = downscaleToFit(sanitized, MAX_OUTPUT_PIXELS / SCALE / SCALE)
                if (scaledDown !== sanitized) {
                    return@withContext try {
                        upscale(scaledDown)
                    } finally {
                        if (!scaledDown.isRecycled) scaledDown.recycle()
                    }
                }
            }

            val accumulator = FloatArray(outW * outH * 4)
            val inputBuffer = ByteBuffer.allocateDirect(4 * TILE_INPUT * TILE_INPUT * 3)
                .order(ByteOrder.nativeOrder())
            val outputBuffer = ByteBuffer.allocateDirect(4 * TILE_OUTPUT * TILE_OUTPUT * 3)
                .order(ByteOrder.nativeOrder())
            val tilePixels = IntArray(TILE_INPUT * TILE_INPUT)
            val featherWeights = featherWeights()

            var y = 0
            var didFinalY = false
            while (!didFinalY) {
                val ty = if (y + TILE_INPUT >= h) {
                    didFinalY = true
                    max(0, h - TILE_INPUT)
                } else y

                var x = 0
                var didFinalX = false
                while (!didFinalX) {
                    val tx = if (x + TILE_INPUT >= w) {
                        didFinalX = true
                        max(0, w - TILE_INPUT)
                    } else x

                    runTile(tflite, sanitized, tx, ty, tilePixels, inputBuffer, outputBuffer)
                    accumulateTile(accumulator, outW, outH, tx * SCALE, ty * SCALE, outputBuffer, featherWeights)

                    if (didFinalX) break
                    x += STRIDE
                }

                if (didFinalY) break
                y += STRIDE
            }

            val outPixels = IntArray(outW * outH)
            var i = 0
            var p = 0
            while (p < outPixels.size) {
                val wT = accumulator[i + 3]
                if (wT > 0f) {
                    val r = (accumulator[i] / wT).coerceIn(0f, 255f).toInt()
                    val g = (accumulator[i + 1] / wT).coerceIn(0f, 255f).toInt()
                    val b = (accumulator[i + 2] / wT).coerceIn(0f, 255f).toInt()
                    outPixels[p] = Color.rgb(r, g, b)
                } else {
                    outPixels[p] = Color.BLACK
                }
                i += 4
                p += 1
            }

            Bitmap.createBitmap(outPixels, outW, outH, Bitmap.Config.ARGB_8888)
        } finally {
            if (sanitized !== input && !sanitized.isRecycled) sanitized.recycle()
        }
    }

    private fun runTile(
        tflite: Interpreter,
        source: Bitmap,
        tx: Int,
        ty: Int,
        tilePixels: IntArray,
        inputBuffer: ByteBuffer,
        outputBuffer: ByteBuffer
    ) {
        source.getPixels(tilePixels, 0, TILE_INPUT, tx, ty, TILE_INPUT, TILE_INPUT)
        inputBuffer.rewind()
        for (pixel in tilePixels) {
            inputBuffer.putFloat(((pixel shr 16) and 0xFF).toFloat())
            inputBuffer.putFloat(((pixel shr 8) and 0xFF).toFloat())
            inputBuffer.putFloat((pixel and 0xFF).toFloat())
        }
        inputBuffer.rewind()
        outputBuffer.rewind()
        tflite.run(inputBuffer, outputBuffer)
        outputBuffer.rewind()
    }

    private fun accumulateTile(
        accumulator: FloatArray,
        outW: Int,
        outH: Int,
        originX: Int,
        originY: Int,
        outputBuffer: ByteBuffer,
        featherWeights: FloatArray
    ) {
        outputBuffer.rewind()
        var weightIdx = 0
        for (yy in 0 until TILE_OUTPUT) {
            val outRow = originY + yy
            if (outRow >= outH) {
                weightIdx += TILE_OUTPUT
                outputBuffer.position(outputBuffer.position() + 4 * 3 * TILE_OUTPUT)
                continue
            }
            for (xx in 0 until TILE_OUTPUT) {
                val rChan = outputBuffer.float
                val gChan = outputBuffer.float
                val bChan = outputBuffer.float
                val outCol = originX + xx
                if (outCol >= outW) {
                    weightIdx += 1
                    continue
                }
                val w = featherWeights[weightIdx++]
                val accBase = (outRow * outW + outCol) * 4
                accumulator[accBase] += rChan * w
                accumulator[accBase + 1] += gChan * w
                accumulator[accBase + 2] += bChan * w
                accumulator[accBase + 3] += w
            }
        }
    }

    /**
     * Pre-computed linear feather mask: 1.0 in the tile core, ramps down to
     * [EDGE_MIN_WEIGHT] across the outermost [TILE_OVERLAP_OUT] pixels.
     */
    private fun featherWeights(): FloatArray {
        val arr = FloatArray(TILE_OUTPUT * TILE_OUTPUT)
        val overlap = TILE_OVERLAP_OUT.toFloat()
        for (y in 0 until TILE_OUTPUT) {
            val distYTop = y.toFloat()
            val distYBottom = (TILE_OUTPUT - 1 - y).toFloat()
            val wy = min(min(distYTop, distYBottom) / overlap, 1f)
                .coerceAtLeast(EDGE_MIN_WEIGHT)
            for (x in 0 until TILE_OUTPUT) {
                val distXLeft = x.toFloat()
                val distXRight = (TILE_OUTPUT - 1 - x).toFloat()
                val wx = min(min(distXLeft, distXRight) / overlap, 1f)
                    .coerceAtLeast(EDGE_MIN_WEIGHT)
                arr[y * TILE_OUTPUT + x] = wx * wy
            }
        }
        return arr
    }

    /**
     * If the bitmap is smaller than one tile, bilinear-upscale so we have at
     * least one tile in each dimension. The model's minimum input is fixed by
     * [TILE_INPUT].
     */
    private fun ensureSrTilable(input: Bitmap): Bitmap {
        val needsUp = input.width < TILE_INPUT || input.height < TILE_INPUT
        if (!needsUp) return input
        val w = max(TILE_INPUT, input.width)
        val h = max(TILE_INPUT, input.height)
        return Bitmap.createScaledBitmap(input, w, h, true)
    }

    private fun downscaleToFit(input: Bitmap, maxPixels: Long): Bitmap {
        val current = input.width.toLong() * input.height
        if (current <= maxPixels) return input
        val ratio = kotlin.math.sqrt(maxPixels.toDouble() / current).toFloat()
        val w = max(TILE_INPUT, (input.width * ratio).toInt())
        val h = max(TILE_INPUT, (input.height * ratio).toInt())
        return Bitmap.createScaledBitmap(input, w, h, true)
    }

    fun release() {
        interpreter?.close(); interpreter = null
        nnApiDelegate?.close(); nnApiDelegate = null
    }

    companion object {
        private const val TAG = "SuperResolution"
        private const val MODEL_NAME = "esrgan.tflite"
        private const val TILE_INPUT = 50
        private const val TILE_OUTPUT = 200
        const val SCALE = TILE_OUTPUT / TILE_INPUT
        private const val TILE_OVERLAP_IN = 8
        private const val STRIDE = TILE_INPUT - TILE_OVERLAP_IN
        private const val TILE_OVERLAP_OUT = TILE_OVERLAP_IN * (TILE_OUTPUT / TILE_INPUT)
        private const val EDGE_MIN_WEIGHT = 0.08f

        // ~16 MP cap on SR output. Above this, downscale the input to keep
        // memory in check (a 16 MP ARGB_8888 bitmap is already ~64 MB).
        private const val MAX_OUTPUT_PIXELS = 16L * 1024 * 1024
    }
}
