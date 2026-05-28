package com.mawaai.love.app.design.ai.gateway

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Canonical test suite for [TextFallbackChain]. Mirrors
 * [VisionFallbackChainTest] case-for-case so the two chains stay in lockstep.
 *
 * If any of these break, the chain has regressed. Do NOT modify a test to
 * match new behaviour — instead, decide whether the behaviour change is
 * intentional and update the documented contract in [AiProvider] first.
 *
 * MT-012: this file is the regression guard for the transparent OpenRouter
 * fallback path. Cases 2 and 5 in particular pin the "Gemini 429 → OpenRouter
 * picks up the work" behaviour described in API_HEALTH_2026-05-22.md.
 */
class TextFallbackChainTest {

    @Test
    fun `first provider success short-circuits chain`() = runTest {
        val p1 = FakeTextProvider(
            id = ProviderId.GROQ,
            configured = true,
            result = Result.success("ok-from-groq")
        )
        val p2 = FakeTextProvider(
            id = ProviderId.OPENROUTER,
            configured = true,
            result = Result.success("should-not-be-reached")
        )
        val chain = TextFallbackChain(listOf(p1, p2))

        val result = chain.generate("test")

        assertTrue("Expected success", result.isSuccess)
        assertEquals("ok-from-groq", result.getOrNull())
        assertEquals("p1 should be called once", 1, p1.callCount)
        assertEquals("p2 should be untouched", 0, p2.callCount)
    }

    @Test
    fun `recoverable error advances to next provider — Gemini 429 falls through to OpenRouter`() =
        runTest {
            val gemini = FakeTextProvider(
                id = ProviderId.GEMINI,
                configured = true,
                result = Result.failure(ProviderRecoverableError.RateLimited("HTTP 429"))
            )
            val openRouter = FakeTextProvider(
                id = ProviderId.OPENROUTER,
                configured = true,
                result = Result.success("recovered-via-openrouter")
            )
            val chain = TextFallbackChain(listOf(gemini, openRouter))

            val result = chain.generate("test")

            assertTrue(result.isSuccess)
            assertEquals("recovered-via-openrouter", result.getOrNull())
            assertEquals(1, gemini.callCount)
            assertEquals(1, openRouter.callCount)
        }

    @Test
    fun `fatal error stops chain immediately`() = runTest {
        val gemini = FakeTextProvider(
            id = ProviderId.GEMINI,
            configured = true,
            result = Result.failure(ProviderFatalError.InvalidKey("401 unauthorized"))
        )
        val openRouter = FakeTextProvider(
            id = ProviderId.OPENROUTER,
            configured = true,
            result = Result.success("would-recover-but-fatal-stopped-chain")
        )
        val chain = TextFallbackChain(listOf(gemini, openRouter))

        val result = chain.generate("test")

        assertTrue("Expected failure", result.isFailure)
        val err = result.exceptionOrNull()
        assertNotNull(err)
        assertTrue(
            "Fatal error must propagate as ProviderFatalError.InvalidKey",
            err is ProviderFatalError.InvalidKey
        )
        assertEquals(
            "Fatal-after-first must not touch the second provider",
            0,
            openRouter.callCount
        )
    }

    @Test
    fun `all-recoverable returns aggregated failure`() = runTest {
        val p1 = FakeTextProvider(
            id = ProviderId.GEMINI,
            configured = true,
            result = Result.failure(ProviderRecoverableError.NotFound("404"))
        )
        val p2 = FakeTextProvider(
            id = ProviderId.OPENROUTER,
            configured = true,
            result = Result.failure(ProviderRecoverableError.RateLimited("429"))
        )
        val chain = TextFallbackChain(listOf(p1, p2))

        val result = chain.generate("test")

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
        val gemini = FakeTextProvider(
            id = ProviderId.GEMINI,
            configured = false,
            result = Result.failure(IllegalStateException("should not call unconfigured"))
        )
        val openRouter = FakeTextProvider(
            id = ProviderId.OPENROUTER,
            configured = true,
            result = Result.success("ok-from-openrouter")
        )
        val chain = TextFallbackChain(listOf(gemini, openRouter))

        val result = chain.generate("test")

        assertTrue(result.isSuccess)
        assertEquals("ok-from-openrouter", result.getOrNull())
        assertEquals("Unconfigured provider must not be called", 0, gemini.callCount)
        assertEquals(1, openRouter.callCount)
    }

    @Test
    fun `empty chain returns descriptive failure`() = runTest {
        val chain = TextFallbackChain(emptyList())

        val result = chain.generate("test")

        assertTrue(result.isFailure)
        assertTrue(
            "Message must explain that no provider is configured",
            result.exceptionOrNull()!!.message!!.contains("configured", ignoreCase = true)
        )
    }

    @Test
    fun `system prompt flows through to provider unchanged`() = runTest {
        var receivedSystem: String? = null
        val recording = object : TextProvider {
            override val id: ProviderId = ProviderId.OPENROUTER
            override val isConfigured: Boolean = true
            override suspend fun generateText(prompt: String, systemPrompt: String?): Result<String> {
                receivedSystem = systemPrompt
                return Result.success("ok")
            }
        }
        val chain = TextFallbackChain(listOf(recording))

        chain.generate("user-prompt", systemPrompt = "you-are-a-cat")

        assertEquals("you-are-a-cat", receivedSystem)
    }

    // ─── Test helpers ────────────────────────────────────────────────────────

    private class FakeTextProvider(
        override val id: ProviderId,
        configured: Boolean,
        private val result: Result<String>,
    ) : TextProvider {
        override val isConfigured: Boolean = configured
        var callCount: Int = 0
            private set

        override suspend fun generateText(prompt: String, systemPrompt: String?): Result<String> {
            callCount += 1
            return result
        }
    }
}
