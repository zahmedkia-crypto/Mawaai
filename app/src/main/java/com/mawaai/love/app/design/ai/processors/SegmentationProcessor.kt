package com.mawaai.love.app.design.ai.processors

import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenter
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Thin wrapper around ML Kit's Subject Segmentation client.
 *
 * MT-017 (2026-05-28): the ML Kit `subject-segmentation` artifact has been
 * stuck on `16.0.0-beta1` for an extended period and could change shape (or
 * stop publishing) without notice. Three failure modes we have to survive:
 *
 *   1. `SubjectSegmentation.getClient(options)` throws or returns a client
 *      whose `process` immediately calls `onFailure` (e.g., the on-device
 *      model is not yet downloaded and the device is offline).
 *   2. The artifact is removed from the build at a future date — handled by
 *      the build system; not this class's job.
 *   3. The segmenter returns a foreground bitmap that's smaller than the
 *      input (rare, but the API does not guarantee equal dimensions).
 *
 * The mitigation is to `runCatching` the client construction and degrade to
 * a no-op when initialisation fails. Callers (AIEngineImpl already gates
 * on `subjectSegmenterAvailable`; other callers just receive the input
 * bitmap unchanged) see no exception.
 *
 * [isAvailable] lets the rest of the engine decide whether to attempt
 * segmentation at all — useful for UI ("Auto-cutout disabled on this
 * device") and for testing without an emulator.
 */
@Singleton
class SegmentationProcessor @Inject constructor() {

    /**
     * Lazily-constructed ML Kit client. `null` when ML Kit refused to hand
     * us a client — we treat that as "no subject segmentation on this
     * device" and degrade rather than crash.
     */
    private val segmenter: SubjectSegmenter? by lazy {
        runCatching {
            val options = SubjectSegmenterOptions.Builder()
                .enableForegroundConfidenceMask()
                .enableForegroundBitmap()
                .build()
            SubjectSegmentation.getClient(options)
        }.onFailure { err ->
            Log.w(
                TAG,
                "SubjectSegmentation.getClient failed — auto-cutout will degrade to " +
                    "returning the input bitmap unchanged. This is expected on devices " +
                    "where Google Play Services / ML Kit on-device models are missing.",
                err,
            )
        }.getOrNull()
    }

    /**
     * True when the underlying ML Kit client was constructed successfully.
     * Touching this property forces lazy init — call from a background
     * thread (e.g., during `AIEngineImpl.warmUp`).
     */
    val isAvailable: Boolean get() = segmenter != null

    /**
     * Extracts the foreground subject from [input]. On any failure — null
     * client, ML Kit error, missing on-device model — returns [input]
     * unchanged so the caller's downstream pipeline can keep going.
     */
    suspend fun extractForeground(input: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        val client = segmenter ?: return@withContext input

        val result = runCatching {
            val image = InputImage.fromBitmap(input, 0)
            suspendCancellableCoroutine<com.google.mlkit.vision.segmentation.subject.SubjectSegmentationResult> { cont ->
                client.process(image)
                    .addOnSuccessListener { cont.resume(it) }
                    .addOnFailureListener { cont.resumeWithException(it) }
            }
        }.getOrElse { err ->
            Log.w(TAG, "Subject segmentation failed at process() — returning input unchanged", err)
            return@withContext input
        }

        result.foregroundBitmap ?: input
    }

    private companion object {
        const val TAG = "SegmentationProcessor"
    }
}
