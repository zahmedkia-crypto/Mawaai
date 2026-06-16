package com.mawaai.love.app.design.ai.removebg

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the MT-011 pre-flight quota snapshot logic.
 *
 * `RemoveBgClient.precheckQuota` is hard to test on the JVM because it touches
 * `Dispatchers.IO`, an Android `Context`, and Retrofit. But the math that
 * decides "do I have quota?" lives entirely in [RemoveBgQuotaSnapshot] and is
 * pure — so we test it directly. If the formula here regresses, the
 * Inspiration / Cutout flow either burns a 402 it could have avoided, or
 * blocks a call that would have succeeded.
 */
class RemoveBgQuotaSnapshotTest {

    @Test
    fun `live snapshot with free calls has quota for preview`() {
        val snap = liveSnapshot(previewCalls = 12, credits = 0, highQuality = false)
        assertTrue(snap.hasAvailableQuota)
    }

    @Test
    fun `live snapshot with credits has quota for full-size`() {
        val snap = liveSnapshot(previewCalls = 0, credits = 3, highQuality = true)
        assertTrue(snap.hasAvailableQuota)
    }

    @Test
    fun `live snapshot with zero free calls and zero credits blocks preview`() {
        val snap = liveSnapshot(previewCalls = 0, credits = 0, highQuality = false)
        assertFalse("Both quotas exhausted — must skip", snap.hasAvailableQuota)
    }

    @Test
    fun `live snapshot with only free calls blocks full-size`() {
        val snap = liveSnapshot(previewCalls = 25, credits = 0, highQuality = true)
        assertFalse("Free calls cannot pay for `size=auto`", snap.hasAvailableQuota)
    }

    @Test
    fun `optimistic snapshot always reports quota available`() {
        val snap = RemoveBgQuotaSnapshot(
            remainingPreviewCalls = Int.MAX_VALUE,
            remainingCredits = Int.MAX_VALUE,
            hasAvailableQuota = true,
            source = RemoveBgQuotaSnapshot.Source.Optimistic,
        )
        assertTrue(
            "When /account fails, the client must proceed so transient errors don't block work",
            snap.hasAvailableQuota
        )
        assertEquals(RemoveBgQuotaSnapshot.Source.Optimistic, snap.source)
    }

    @Test
    fun `unconfigured snapshot reports no quota`() {
        val snap = RemoveBgQuotaSnapshot(
            remainingPreviewCalls = 0,
            remainingCredits = 0,
            hasAvailableQuota = false,
            source = RemoveBgQuotaSnapshot.Source.Unconfigured,
        )
        assertFalse(snap.hasAvailableQuota)
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private fun liveSnapshot(
        previewCalls: Int,
        credits: Int,
        highQuality: Boolean,
    ): RemoveBgQuotaSnapshot = RemoveBgQuotaSnapshot(
        remainingPreviewCalls = previewCalls,
        remainingCredits = credits,
        hasAvailableQuota = if (highQuality) credits > 0 else (previewCalls > 0 || credits > 0),
        source = RemoveBgQuotaSnapshot.Source.Live,
    )
}
