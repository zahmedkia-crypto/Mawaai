package com.mawaai.love.app.design.ai.gateway

import android.graphics.Bitmap
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Canonical 5-case test suite for [VisionFallbackChain].
 *
 * These cases pin the chain's contract:
 *   1. First provider succeeds → chain short-circuits, later providers untouched.
 *   2. Recoverable error → chain advances and the next provider's result wins.
 *   3. Fatal error → chain stops, propagates the fatal error.
 *   4. All recoverable → chain returns aggregated failure.
 *   5. Unconfigured providers → silently skipped, not counted as failures.
 *
 * If any of these break, the chain has regressed. Do NOT modify a test to
 * match new behaviour — instead, decide whether the behaviour change is
 * intentional and update the documented contract in AiProvider.kt first.
 */
class VisionFallbackChainTest {

    private lateinit var bitmap: Bitmap

    @Before
    fun setUp() {
        // Bitmap is referenced by VisionProvider.visionAnalyze but the fakes
        // never actually inspect it, so a non-null placeholder is enough.
        // We cannot construct a real Bitmap in a pure JVM unit test without
        // Robolectric; the fakes are designed to accept any non-null instance.
        bitmap = NullBitmap
    }

    @Test
    fun `first provider success short-circuits chain`() = runTest {
        val p1 = FakeProvider(
            id = ProviderId.GEMINI,
            configured = true,
            result = Result.success("ok-from-gemini")
        )
        val p2 = FakeProvider(
            id = ProviderId.OPENROUTER,
            configured = true,
            result = Result.success("should-not-be-reached")
        )
        val chain = VisionFallbackChain(listOf(p1, p2))

        val result = chain.analyze("test", bitmap)

        assertTrue("Expected success", result.isSuccess)
        assertEquals("ok-from-gemini", result.getOrNull())
        assertEquals("p1 should be called once", 1, p1.callCount)
        assertEquals("p2 should be untouched", 0, p2.callCount)
    }

    @Test
    fun `recoverable error advances to next provider`() = runTest {
        val p1 = FakeProvider(
            id = ProviderId.GEMINI,
            configured = true,
            result = Result.failure(
                ProviderRecoverableError.NotFound("model deprecated")
            )
        )
        val p2 = FakeProvider(
            id = ProviderId.OPENROUTER,
            configured = true,
            result = Result.success("recovered-via-openrouter")
        )
        val chain = VisionFallbackChain(listOf(p1, p2))

        val result = chain.analyze("test", bitmap)

        assertTrue(result.isSuccess)
        assertEquals("recovered-via-openrouter", result.getOrNull())
        assertEquals(1, p1.callCount)
        assertEquals(1, p2.callCount)
    }

    @Test
    fun `fatal error stops chain immediately`() = runTest {
        val p1 = FakeProvider(
            id = ProviderId.GEMINI,
            configured = true,
            result = Result.failure(
                ProviderFatalError.InvalidKey("401 unauthorized")
            )
        )
        val p2 = FakeProvider(
            id = ProviderId.OPENROUTER,
            configured = true,
            result = Result.success("would-recover-but-fatal-stopped-chain")
        )
        val chain = VisionFallbackChain(listOf(p1, p2))

        val result = chain.analyze("test", bitmap)

        assertTrue("Expected failure", result.isFailure)
        val err = result.exceptionOrNull()
        assertNotNull(err)
        assertTrue(
            "Fatal error must propagate as ProviderFatalError.InvalidKey",
            err is ProviderFatalError.InvalidKey
        )
        assertEquals(0, p2.callCount)
    }

    @Test
    fun `all-recoverable returns aggregated failure`() = runTest {
        val p1 = FakeProvider(
            id = ProviderId.GEMINI,
            configured = true,
            result = Result.failure(
                ProviderRecoverableError.NotFound("404")
            )
        )
        val p2 = FakeProvider(
            id = ProviderId.OPENROUTER,
            configured = true,
            result = Result.failure(
                ProviderRecoverableError.RateLimited("429")
            )
        )
        val chain = VisionFallbackChain(listOf(p1, p2))

        val result = chain.analyze("test", bitmap)

        assertTrue(result.isFailure)
        val err = result.exceptionOrNull()
        assertNotNull(err)
        assertTrue(
            "Aggregated message must reference both providers",
            err!!.message!!.contains("Gemini", ignoreCase = true) &&
                err.message!!.contains("OpenRouter", ignoreCase = true)
        )
    }

    @Test
    fun `unconfigured providers are skipped silently`() = runTest {
        val p1 = FakeProvider(
            id = ProviderId.GEMINI,
            configured = false,
            result = Result.failure(IllegalStateException("should not call unconfigured"))
        )
        val p2 = FakeProvider(
            id = ProviderId.OPENROUTER,
            configured = true,
            result = Result.success("ok-from-openrouter")
        )
        val chain = VisionFallbackChain(listOf(p1, p2))

        val result = chain.analyze("test", bitmap)

        assertTrue(result.isSuccess)
        assertEquals("ok-from-openrouter", result.getOrNull())
        assertEquals("Unconfigured provider must not be called", 0, p1.callCount)
        assertEquals(1, p2.callCount)
    }

    @Test
    fun `empty chain returns descriptive failure`() = runTest {
        val chain = VisionFallbackChain(emptyList())

        val result = chain.analyze("test", bitmap)

        assertTrue(result.isFailure)
        assertTrue(
            "Message must hint at missing API keys",
            result.exceptionOrNull()!!.message!!.contains("API key", ignoreCase = true)
        )
    }

    // ---- Test helpers ----

    /**
     * Minimal [VisionProvider] fake. Records call count, returns a canned result.
     */
    private class FakeProvider(
        override val id: ProviderId,
        configured: Boolean,
        private val result: Result<String>,
    ) : VisionProvider {
        override val isConfigured: Boolean = configured
        var callCount: Int = 0
            private set

        override suspend fun visionAnalyze(prompt: String, image: Bitmap): Result<String> {
            callCount += 1
            return result
        }
    }

    /**
     * Unit tests run on the JVM where android.graphics.Bitmap is a stub class
     * with no public constructor. The fake providers never read this object,
     * so passing a null cast is acceptable.
     *
     * If you ever add a provider that actually serializes the bitmap inside a
     * unit test (rather than going through the network), introduce Robolectric
     * or migrate that test to androidTest/.
     */
    @Suppress("UNCHECKED_CAST")
    private val NullBitmap: Bitmap = null as Bitmap
}
