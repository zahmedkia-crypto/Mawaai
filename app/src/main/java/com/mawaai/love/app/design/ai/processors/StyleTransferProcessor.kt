package com.mawaai.love.app.design.ai.processors

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.Shader
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
 * Two-stage arbitrary style transfer: `style_predict.tflite` extracts a
 * style vector from a reference image; `style_transfer.tflite` applies that
 * style to a [CONTENT_INPUT_SIZE]² content tile and returns a stylized
 * bitmap. Aspect ratio is preserved by letterboxing the content into the
 * model's square input and cropping the active region from the output.
 * NNAPI delegate is attempted first; failures fall back transparently to
 * the CPU interpreter.
 */
@Singleton
class StyleTransferProcessor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var predictInterpreter: Interpreter? = null
    private var transferInterpreter: Interpreter? = null
    private var predictDelegate: NnApiDelegate? = null
    private var transferDelegate: NnApiDelegate? = null
    private var initFailed = false

    private fun ensureInterpreters() {
        if (initFailed || (predictInterpreter != null && transferInterpreter != null)) return
        try {
            predictInterpreter = buildInterpreter(loadModel(MODEL_PREDICT)) { predictDelegate = it }
            transferInterpreter = buildInterpreter(loadModel(MODEL_TRANSFER)) { transferDelegate = it }
        } catch (t: Throwable) {
            initFailed = true
            predictInterpreter?.close(); predictInterpreter = null
            transferInterpreter?.close(); transferInterpreter = null
            predictDelegate?.close(); predictDelegate = null
            transferDelegate?.close(); transferDelegate = null
            throw ModelMissingException("style_transfer")
        }
    }

    private fun buildInterpreter(
        model: MappedByteBuffer,
        store: (NnApiDelegate?) -> Unit
    ): Interpreter {
        return runCatching {
            val nn = NnApiDelegate()
            store(nn)
            val options = Interpreter.Options().apply { addDelegate(nn as Delegate) }
            Interpreter(model, options)
        }.getOrElse {
            Log.w(TAG, "NNAPI delegate unavailable for style transfer; using CPU", it)
            store(null)
            Interpreter(model)
        }
    }

    private fun loadModel(name: String): MappedByteBuffer {
        val fd = context.assets.openFd("models/$name")
        FileInputStream(fd.fileDescriptor).use { fis ->
            return fis.channel.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
        }
    }

    suspend fun stylize(content: Bitmap, styleId: String): Bitmap = withContext(Dispatchers.Default) {
        ensureInterpreters()
        val predict = predictInterpreter ?: throw ModelMissingException("style_predict")
        val transfer = transferInterpreter ?: throw ModelMissingException("style_transfer")

        // 1. Style vector from reference.
        val styleRef = generateStyleReference(styleId)
        val styleVector = Array(1) { Array(1) { Array(1) { FloatArray(STYLE_VECTOR_DIM) } } }
        predict.run(bitmapToFloatBuffer(styleRef, STYLE_INPUT_SIZE, normalize = true), styleVector)
        styleRef.recycle()

        // 2. Letterbox content into the model's square input, keeping aspect.
        val (boxed, activeRect) = letterboxToSquare(content, CONTENT_INPUT_SIZE)
        val contentBuffer = bitmapToFloatBuffer(boxed, CONTENT_INPUT_SIZE, normalize = true)
        if (boxed !== content) boxed.recycle()

        val outputBuffer = ByteBuffer.allocateDirect(4 * CONTENT_INPUT_SIZE * CONTENT_INPUT_SIZE * 3)
            .order(ByteOrder.nativeOrder())
        val inputs = arrayOf<Any>(contentBuffer, styleVector)
        val outputs = HashMap<Int, Any>()
        outputs[0] = outputBuffer
        transfer.runForMultipleInputsOutputs(inputs, outputs)

        // 3. Crop the active letterbox region from the square output.
        val stylizedSquare = bufferToBitmap(outputBuffer, CONTENT_INPUT_SIZE)
        val cropped = if (activeRect.width() == CONTENT_INPUT_SIZE && activeRect.height() == CONTENT_INPUT_SIZE) {
            stylizedSquare
        } else {
            val w = activeRect.width()
            val h = activeRect.height()
            val cut = Bitmap.createBitmap(stylizedSquare, activeRect.left, activeRect.top, w, h)
            stylizedSquare.recycle()
            cut
        }
        cropped
    }

    private fun bitmapToFloatBuffer(bitmap: Bitmap, size: Int, normalize: Boolean): ByteBuffer {
        val resized = if (bitmap.width == size && bitmap.height == size) bitmap
        else Bitmap.createScaledBitmap(bitmap, size, size, true)
        val buffer = ByteBuffer.allocateDirect(4 * size * size * 3).apply {
            order(ByteOrder.nativeOrder())
        }
        val pixels = IntArray(size * size)
        resized.getPixels(pixels, 0, size, 0, 0, size, size)
        if (normalize) {
            for (p in pixels) {
                buffer.putFloat(((p shr 16) and 0xFF) / 255f)
                buffer.putFloat(((p shr 8) and 0xFF) / 255f)
                buffer.putFloat((p and 0xFF) / 255f)
            }
        } else {
            for (p in pixels) {
                buffer.putFloat(((p shr 16) and 0xFF).toFloat())
                buffer.putFloat(((p shr 8) and 0xFF).toFloat())
                buffer.putFloat((p and 0xFF).toFloat())
            }
        }
        if (resized !== bitmap) resized.recycle()
        buffer.rewind()
        return buffer
    }

    private fun bufferToBitmap(buffer: ByteBuffer, size: Int): Bitmap {
        buffer.rewind()
        val pixels = IntArray(size * size)
        for (i in 0 until size * size) {
            val r = (buffer.float.coerceIn(0f, 1f) * 255f).toInt()
            val g = (buffer.float.coerceIn(0f, 1f) * 255f).toInt()
            val b = (buffer.float.coerceIn(0f, 1f) * 255f).toInt()
            pixels[i] = Color.rgb(r, g, b)
        }
        return Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
    }

    /**
     * Scales [source] to fit inside [size]×[size] preserving aspect, then
     * centers it on a black square canvas. Returns the boxed bitmap plus the
     * sub-rect occupied by the actual image (so the caller can crop after
     * inference).
     */
    private fun letterboxToSquare(source: Bitmap, size: Int): Pair<Bitmap, Rect> {
        val srcW = source.width
        val srcH = source.height
        val scale = min(size.toFloat() / srcW, size.toFloat() / srcH)
        val targetW = max(1, (srcW * scale).toInt())
        val targetH = max(1, (srcH * scale).toInt())
        if (targetW == size && targetH == size) {
            return Pair(source, Rect(0, 0, size, size))
        }
        val scaled = Bitmap.createScaledBitmap(source, targetW, targetH, true)
        val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawColor(Color.BLACK)
        val ox = (size - targetW) / 2
        val oy = (size - targetH) / 2
        canvas.drawBitmap(scaled, ox.toFloat(), oy.toFloat(), null)
        if (scaled !== source) scaled.recycle()
        return Pair(out, Rect(ox, oy, ox + targetW, oy + targetH))
    }

    private fun generateStyleReference(styleId: String): Bitmap {
        val size = STYLE_INPUT_SIZE
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        when (styleId) {
            "vector_clean" -> {
                paint.color = Color.WHITE
                canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
                paint.color = Color.BLACK
                paint.strokeWidth = 4f
                paint.style = Paint.Style.STROKE
                for (i in 0..6) {
                    canvas.drawLine(0f, i * size / 6f, size.toFloat(), i * size / 6f, paint)
                }
            }
            "artistic" -> {
                paint.shader = LinearGradient(
                    0f, 0f, size.toFloat(), size.toFloat(),
                    intArrayOf(Color.parseColor("#F4A261"), Color.parseColor("#E76F51"), Color.parseColor("#264653")),
                    null, Shader.TileMode.MIRROR
                )
                canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
            }
            "minimalist" -> {
                paint.color = Color.parseColor("#F2EFEA")
                canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
                paint.color = Color.parseColor("#1B1B3A")
                canvas.drawCircle(size / 2f, size / 2f, size / 4f, paint)
            }
            "realistic" -> {
                paint.shader = RadialGradient(
                    size / 2f, size / 2f, size.toFloat(),
                    intArrayOf(Color.parseColor("#FFE9B0"), Color.parseColor("#7C4F1A"), Color.parseColor("#1A0E04")),
                    null, Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
            }
            else -> {
                paint.shader = LinearGradient(
                    0f, 0f, size.toFloat(), size.toFloat(),
                    intArrayOf(Color.parseColor("#C8860A"), Color.parseColor("#8B2F0F")),
                    null, Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
            }
        }
        return bitmap
    }

    fun release() {
        predictInterpreter?.close(); predictInterpreter = null
        transferInterpreter?.close(); transferInterpreter = null
        predictDelegate?.close(); predictDelegate = null
        transferDelegate?.close(); transferDelegate = null
    }

    companion object {
        private const val TAG = "StyleTransfer"
        private const val MODEL_PREDICT = "style_predict.tflite"
        private const val MODEL_TRANSFER = "style_transfer.tflite"
        private const val STYLE_INPUT_SIZE = 256
        private const val CONTENT_INPUT_SIZE = 384
        private const val STYLE_VECTOR_DIM = 100
    }
}
