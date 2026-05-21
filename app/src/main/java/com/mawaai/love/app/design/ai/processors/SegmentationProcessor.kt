package com.mawaai.love.app.design.ai.processors

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class SegmentationProcessor @Inject constructor() {

    private val segmenter by lazy {
        val options = SubjectSegmenterOptions.Builder()
            .enableForegroundConfidenceMask()
            .enableForegroundBitmap()
            .build()
        SubjectSegmentation.getClient(options)
    }

    suspend fun extractForeground(input: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        val image = InputImage.fromBitmap(input, 0)
        val result = suspendCancellableCoroutine { cont ->
            segmenter.process(image)
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }
        result.foregroundBitmap ?: input
    }
}
