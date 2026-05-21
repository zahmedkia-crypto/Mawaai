package com.mawaai.love.app.design.ai

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.mawaai.love.app.data.model.Artwork
import com.mawaai.love.app.data.repository.ArtworkRepository
import com.mawaai.love.app.design.domain.model.DrawingAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Applies a [DrawingAction] in place on a saved artwork's flattened PNG.
 * Destructive — the artwork's `fullImagePath` is rewritten and its
 * thumbnail regenerated — but a single in-memory undo entry preserves the
 * previous bytes so the Recommendations screen can offer one-step
 * Revert. The undo buffer is global (one entry per process), so applying
 * a new action on any artwork supersedes the previous undo.
 *
 * Why in-memory instead of a `.bak` file on disk:
 *  - The 1-step undo guarantee means we never need more than one snapshot
 *    at a time; a few MB of PNG bytes in the heap is cheaper than a
 *    second disk round-trip per Apply.
 *  - The buffer is cleared on process death — acceptable for an
 *    enhancement workflow where the user can always re-apply.
 *
 * Threading: all I/O runs on [Dispatchers.IO]; bitmap composition runs on
 * [Dispatchers.Default]. The undo lock is a plain `synchronized` block
 * because the operations inside are constant-time and we want
 * [snapshot]/[clearUndo] to be callable from non-suspend contexts (e.g.
 * `ViewModel.onCleared`).
 *
 * The per-action bitmap transformations live in [DrawingActionRenderers]
 * — pure Bitmap → Bitmap functions with no class state.
 */
@Singleton
class DrawingActionEngine @Inject constructor(
    private val artworkRepository: ArtworkRepository
) {

    private data class UndoEntry(
        val artworkId: Long,
        val fullBytes: ByteArray,
        val thumbBytes: ByteArray,
        /**
         * True iff the thumbnail file existed before [apply] wrote a new
         * one. False means Apply *generated* the thumbnail and Revert
         * should `delete()` it rather than restore the empty
         * `thumbBytes`. Closes the audit's "missing-thumbnail" edge case.
         */
        val thumbExistedPreApply: Boolean
    )

    private val undoLock = Any()
    private var lastApplied: UndoEntry? = null

    /**
     * Applies [action] to the artwork with id [artworkId]. Returns the
     * updated [Artwork] entity on success (with bumped `updatedAt`), or
     * a failure with the cause. The previous PNG bytes are cached so a
     * subsequent [revert] call restores the artwork to the pre-apply
     * state.
     *
     * Failure modes:
     *  - Artwork not in Room → `IllegalStateException("Artwork not found")`
     *  - Full PNG or thumbnail file missing on disk → IOException from the
     *    underlying `BitmapFactory.decodeFile` returning null, surfaced
     *    as `IllegalStateException("Could not decode artwork bitmap")`.
     *  - Disk write failure → propagates the underlying IOException.
     */
    suspend fun apply(artworkId: Long, action: DrawingAction): Result<Artwork> = runCatching {
        val artwork = artworkRepository.getById(artworkId)
            ?: error("Artwork not found")
        val fullFile = File(artwork.fullImagePath)
        val thumbFile = File(artwork.thumbnailPath)
        if (!fullFile.exists()) error("Artwork bitmap missing on disk")

        val input = withContext(Dispatchers.IO) { BitmapFactory.decodeFile(fullFile.absolutePath) }
            ?: error("Could not decode artwork bitmap")

        // Snapshot the previous bytes BEFORE writing — Revert restores
        // these. Read from disk rather than re-encoding [input] so the
        // snapshot is byte-identical to whatever the user had saved.
        val previousFullBytes = withContext(Dispatchers.IO) { fullFile.readBytes() }
        val thumbExistedPreApply = thumbFile.exists()
        val previousThumbBytes = withContext(Dispatchers.IO) {
            if (thumbExistedPreApply) thumbFile.readBytes() else ByteArray(0)
        }

        // Cap the in-memory undo footprint. For very large artworks the
        // PNG bytes plus the thumbnail bytes can swell past UNDO_BYTE_CAP;
        // when that happens we still apply the action but DROP the undo
        // entry — Revert will surface "Nothing to revert" and the user
        // accepts a destructive change. The cap protects low-RAM devices
        // from holding an extra 30+ MB of bytes per chained Apply.
        val totalUndoBytes = previousFullBytes.size.toLong() +
            previousThumbBytes.size.toLong()
        val undoEligible = totalUndoBytes <= UNDO_BYTE_CAP

        val output = withContext(Dispatchers.Default) { applyAction(input, action) }
        if (output !== input) input.recycle()

        withContext(Dispatchers.IO) {
            FileOutputStream(fullFile).use { out ->
                output.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            writeThumbnail(output, thumbFile)
        }
        if (!output.isRecycled) output.recycle()

        if (undoEligible) {
            snapshot(
                artworkId = artworkId,
                fullBytes = previousFullBytes,
                thumbBytes = previousThumbBytes,
                thumbExistedPreApply = thumbExistedPreApply
            )
        } else {
            // Bytes captured but not retained — release them and clear
            // any stale entry so canUndo() reports false honestly.
            clearUndo()
        }

        val updated = artwork.copy(updatedAt = System.currentTimeMillis())
        artworkRepository.update(updated)
        updated
    }

    /**
     * Restores the artwork to the state it had immediately before the
     * most recent [apply]. Returns the updated [Artwork] entity (with
     * `updatedAt` bumped to now) or a failure if no undo is available
     * for [artworkId]. Clears the undo entry on success — Revert is
     * idempotent only across a single Apply.
     */
    suspend fun revert(artworkId: Long): Result<Artwork> = runCatching {
        val snapshot = synchronized(undoLock) { lastApplied }
            ?: error("Nothing to revert")
        if (snapshot.artworkId != artworkId) error("Undo buffer belongs to a different artwork")

        val artwork = artworkRepository.getById(artworkId)
            ?: error("Artwork not found")
        val fullFile = File(artwork.fullImagePath)
        val thumbFile = File(artwork.thumbnailPath)

        withContext(Dispatchers.IO) {
            FileOutputStream(fullFile).use { it.write(snapshot.fullBytes) }
            // Three thumbnail cases:
            //  - Original existed AND we cached its bytes: write them back.
            //  - Original existed but somehow had zero-length bytes (rare,
            //    e.g. the file existed empty pre-Apply): write the empty
            //    bytes anyway to mirror the original state.
            //  - Original did NOT exist pre-Apply: delete() the thumb that
            //    Apply generated, restoring the missing state. Closes the
            //    audit's revert-with-no-original-thumb edge case.
            if (snapshot.thumbExistedPreApply) {
                FileOutputStream(thumbFile).use { it.write(snapshot.thumbBytes) }
            } else if (thumbFile.exists()) {
                thumbFile.delete()
            }
        }

        clearUndo()

        val updated = artwork.copy(updatedAt = System.currentTimeMillis())
        artworkRepository.update(updated)
        updated
    }

    /** True if a Revert is available for [artworkId]. */
    fun canUndo(artworkId: Long): Boolean = synchronized(undoLock) {
        lastApplied?.artworkId == artworkId
    }

    /**
     * Forgets any pending undo. Called from the recommendations VM's
     * `onCleared` so a fresh navigation into a different artwork doesn't
     * see a stale undo offer.
     */
    fun clearUndo() = synchronized(undoLock) { lastApplied = null }

    private fun snapshot(
        artworkId: Long,
        fullBytes: ByteArray,
        thumbBytes: ByteArray,
        thumbExistedPreApply: Boolean
    ) = synchronized(undoLock) {
        lastApplied = UndoEntry(artworkId, fullBytes, thumbBytes, thumbExistedPreApply)
    }

    private fun applyAction(input: Bitmap, action: DrawingAction): Bitmap = when (action) {
        DrawingAction.AddSolidBackground -> drawSolidBackground(input)
        DrawingAction.AddGradientBackground -> drawGradientBackground(input)
        DrawingAction.DarkenEdges -> drawVignette(input)
        DrawingAction.MirrorHorizontally -> drawMirrorLeftToRight(input)
        DrawingAction.AddAccentColor -> drawAccentTint(input)
        DrawingAction.LightenCanvas -> drawLightenColorMatrix(input)
        DrawingAction.ThickenThinStrokes -> drawThickenStrokes(input)
        DrawingAction.FixSymmetry -> drawFixSymmetry(input)
        DrawingAction.BalancePalette -> drawBalancePalette(input)
    }

    private fun writeThumbnail(source: Bitmap, thumbFile: File) {
        val target = THUMB_SIZE
        val ratio = source.width.toFloat() / source.height.toFloat()
        val (tw, th) = if (ratio >= 1f) target to (target / ratio).toInt()
        else (target * ratio).toInt() to target
        val thumb = Bitmap.createScaledBitmap(source, tw.coerceAtLeast(1), th.coerceAtLeast(1), true)
        val tmpStream = ByteArrayOutputStream()
        thumb.compress(Bitmap.CompressFormat.JPEG, 85, tmpStream)
        FileOutputStream(thumbFile).use { it.write(tmpStream.toByteArray()) }
        if (thumb !== source) thumb.recycle()
    }

    private companion object {
        const val THUMB_SIZE = 256

        /**
         * Maximum bytes the in-memory undo entry may consume. Sized for
         * mid-tier devices: full PNG up to ~6 MB plus thumb up to ~50 KB
         * fits, but a 4K artwork weighing ~20 MB does not — that case
         * skips the undo and Apply becomes irreversible.
         */
        const val UNDO_BYTE_CAP: Long = 8L * 1024L * 1024L
    }
}
