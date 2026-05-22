package com.mawaai.love.app

import com.mawaai.love.app.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit
import kotlin.test.assertTrue

/**
 * Opt-in live API smoke tests for Gemini, HuggingFace, Cloudflare Workers AI,
 * and Remove.bg. **Skipped by default** so CI never burns quota.
 *
 * To run:
 * ```
 * MAWAAI_RUN_LIVE_API_TESTS=1 ./gradlew :app:test \
 *     --tests com.mawaai.love.app.ApiHealthSmokeTest
 * ```
 *
 * Each test reports PASS/FAIL via assertion and stdout latency for triage.
 *
 * Architecture: this is a pure JVM unit test — no Android dependency, no
 * coroutine machinery. It exists to give devs a one-command way to verify
 * end-to-end key validity + endpoint reachability before shipping a build.
 *
 * Origin: MT-011 in PROJECT_SCAN_CONTINUATION_2026-05-22.md / API_HEALTH_2026-05-22.md
 */
class ApiHealthSmokeTest {

    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()

    private val jsonMt = "application/json; charset=utf-8".toMediaType()

    @Before
    fun gateOnOptIn() {
        val on = System.getenv("MAWAAI_RUN_LIVE_API_TESTS") == "1"
        assumeTrue(
            "Set MAWAAI_RUN_LIVE_API_TESTS=1 to run live API smoke tests",
            on
        )
    }

    @Test
    fun gemini_textGeneration_succeeds() {
        val key = BuildConfig.GEMINI_API_KEY
        assumeTrue("GEMINI_API_KEY is empty in local.properties", key.isNotBlank())
        // Use the current stable flash name. -latest suffix is deprecated as of 2026-05.
        val model = "gemini-2.0-flash"
        val body = """{"contents":[{"parts":[{"text":"Reply with just: OK"}]}]}"""
            .toRequestBody(jsonMt)
        val req = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$key")
            .post(body)
            .build()
        val t0 = System.currentTimeMillis()
        http.newCall(req).execute().use { resp ->
            val ms = System.currentTimeMillis() - t0
            println("[GEMINI] http=${resp.code} latency=${ms}ms")
            // 429 = quota exceeded but key valid — still informative; treat as soft pass.
            assertTrue("Gemini failed: ${resp.code}", resp.code in setOf(200, 429))
        }
    }

    @Test
    fun huggingface_modelMetadata_reachable() {
        val key = BuildConfig.HUGGINGFACE_API_KEY
        assumeTrue("HUGGINGFACE_API_KEY is empty in local.properties", key.isNotBlank())
        // Use the model the app actually depends on for BG removal.
        val req = Request.Builder()
            .url("https://huggingface.co/api/models/briaai/RMBG-1.4")
            .header("Authorization", "Bearer $key")
            .get()
            .build()
        val t0 = System.currentTimeMillis()
        http.newCall(req).execute().use { resp ->
            val ms = System.currentTimeMillis() - t0
            println("[HF metadata briaai/RMBG-1.4] http=${resp.code} latency=${ms}ms")
            assertTrue("HF metadata failed: ${resp.code}", resp.isSuccessful)
        }
    }

    @Test
    fun cloudflareWorkersAi_textGeneration_succeeds() {
        val acct = BuildConfig.CLOUDFLARE_ACCOUNT_ID
        val token = BuildConfig.CLOUDFLARE_API_TOKEN
        assumeTrue("CLOUDFLARE creds empty in local.properties", acct.isNotBlank() && token.isNotBlank())
        val body = """{"prompt":"Reply with just: OK","max_tokens":10}""".toRequestBody(jsonMt)
        val req = Request.Builder()
            .url("https://api.cloudflare.com/client/v4/accounts/$acct/ai/run/@cf/meta/llama-3.1-8b-instruct")
            .header("Authorization", "Bearer $token")
            .post(body)
            .build()
        val t0 = System.currentTimeMillis()
        http.newCall(req).execute().use { resp ->
            val ms = System.currentTimeMillis() - t0
            println("[CF Workers AI] http=${resp.code} latency=${ms}ms")
            assertTrue("Cloudflare Workers AI failed: ${resp.code}", resp.isSuccessful)
        }
    }

    @Test
    fun removeBg_accountInfo_succeeds() {
        val key = BuildConfig.REMOVE_BG_API_KEY
        assumeTrue("REMOVE_BG_API_KEY is empty in local.properties", key.isNotBlank())
        val req = Request.Builder()
            .url("https://api.remove.bg/v1.0/account")
            .header("X-Api-Key", key)
            .get()
            .build()
        val t0 = System.currentTimeMillis()
        http.newCall(req).execute().use { resp ->
            val ms = System.currentTimeMillis() - t0
            val body = resp.body?.string() ?: ""
            println("[Remove.bg] http=${resp.code} latency=${ms}ms body_len=${body.length}")
            assertTrue("Remove.bg failed: ${resp.code}", resp.isSuccessful)
            // Soft warning if credits are low; do not fail the test.
            if ("\"total\":1" in body || "\"total\":0" in body) {
                println("[Remove.bg] WARNING: low credit balance — top up before release")
            }
        }
    }
}
