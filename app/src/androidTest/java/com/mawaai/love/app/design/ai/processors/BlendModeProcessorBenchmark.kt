package com.mawaai.love.app.design.ai.processors

import android.graphics.Bitmap
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mawaai.love.app.core.opencv.OpenCVBootstrap
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.system.measureTimeMillis

@RunWith(AndroidJUnit4::class)
class BlendModeProcessorBenchmark {

    private lateinit var processor: BlendModeProcessor

    @Before
    fun setup() {
        OpenCVBootstrap.ensureLoaded()
        processor = BlendModeProcessor()
    }

    @Test
    fun benchmarkBlendModes_1024px() = runBlocking {
        val size = 1024
        val base = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
            eraseColor(android.graphics.Color.GRAY)
        }
        val overlay = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
            eraseColor(android.graphics.Color.BLUE)
        }

        val modes = BlendMode.entries
        val results = mutableMapOf<BlendMode, Long>()

        // Warmup
        processor.blend(base, overlay, BlendMode.NORMAL)

        for (mode in modes) {
            val time = measureTimeMillis {
                val result = processor.blend(base, overlay, mode)
                assertNotNull(result)
            }
            results[mode] = time
            Log.i("BlendBenchmark", "Mode $mode took ${time}ms for ${size}px")
        }

        // Assert performance target: < 60ms for 1024px
        results.forEach { (mode, time) ->
            assertTrue("Mode $mode was too slow: ${time}ms", time < 60)
        }
    }
}
