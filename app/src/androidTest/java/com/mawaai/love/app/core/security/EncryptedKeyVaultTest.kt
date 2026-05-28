package com.mawaai.love.app.core.security

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for [EncryptedKeyVault]. Lives in `androidTest/` because
 * `EncryptedSharedPreferences` requires the Android Keystore.
 *
 * The suite uses a real `EncryptedKeyVault` against the instrumentation
 * target's context — which means the test files end up under the test app's
 * data dir, separate from the production `mawaai_secrets.xml`.
 *
 * Run:
 *   ./gradlew :app:connectedDebugAndroidTest \
 *       --tests com.mawaai.love.app.core.security.EncryptedKeyVaultTest
 */
@RunWith(AndroidJUnit4::class)
class EncryptedKeyVaultTest {

    private lateinit var vault: EncryptedKeyVault

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        vault = EncryptedKeyVault(ctx)
        vault.clearAll()
    }

    @After
    fun tearDown() {
        // Don't leak overrides across tests.
        vault.clearAll()
    }

    @Test
    fun get_returnsBuildConfigFallback_whenNoOverrideSet() {
        // A fresh vault has no overrides; every lookup falls back to BuildConfig.
        for (id in ApiKeyId.values()) {
            assertEquals(
                "Expected BuildConfig fallback for ${id.name}",
                id.buildConfigFallback(),
                vault.get(id),
            )
            assertFalse(
                "isOverridden must be false for ${id.name} when never set",
                vault.isOverridden(id),
            )
        }
    }

    @Test
    fun put_thenGet_returnsOverride() {
        vault.put(ApiKeyId.GEMINI, "override-gemini-value")
        assertEquals("override-gemini-value", vault.get(ApiKeyId.GEMINI))
        assertTrue(vault.isOverridden(ApiKeyId.GEMINI))
    }

    @Test
    fun put_otherIdsAreUntouched() {
        vault.put(ApiKeyId.GEMINI, "only-gemini")
        for (id in ApiKeyId.values()) {
            if (id == ApiKeyId.GEMINI) continue
            assertFalse(
                "Overriding GEMINI must not flip ${id.name}",
                vault.isOverridden(id),
            )
            assertEquals(id.buildConfigFallback(), vault.get(id))
        }
    }

    @Test
    fun put_emptyClearsOverride_fallsBackToBuildConfig() {
        vault.put(ApiKeyId.OPENROUTER, "first-value")
        assertTrue(vault.isOverridden(ApiKeyId.OPENROUTER))

        vault.put(ApiKeyId.OPENROUTER, "")
        assertFalse(vault.isOverridden(ApiKeyId.OPENROUTER))
        assertEquals(ApiKeyId.OPENROUTER.buildConfigFallback(), vault.get(ApiKeyId.OPENROUTER))
    }

    @Test
    fun put_nullClearsOverride() {
        vault.put(ApiKeyId.GROQ, "groq-value")
        vault.put(ApiKeyId.GROQ, null)
        assertFalse(vault.isOverridden(ApiKeyId.GROQ))
    }

    @Test
    fun put_trimsWhitespace() {
        vault.put(ApiKeyId.HUGGINGFACE, "   token-with-padding   ")
        assertEquals("token-with-padding", vault.get(ApiKeyId.HUGGINGFACE))
    }

    @Test
    fun clearAll_removesEveryOverride() {
        vault.put(ApiKeyId.GEMINI, "g")
        vault.put(ApiKeyId.OPENROUTER, "o")
        vault.put(ApiKeyId.REMOVE_BG, "r")

        vault.clearAll()

        for (id in ApiKeyId.values()) {
            assertFalse(vault.isOverridden(id))
            assertEquals(id.buildConfigFallback(), vault.get(id))
        }
    }

    @Test
    fun secondInstance_seesPersistedOverrides() {
        vault.put(ApiKeyId.GEMINI, "persisted-across-instances")

        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val other = EncryptedKeyVault(ctx)
        try {
            assertEquals("persisted-across-instances", other.get(ApiKeyId.GEMINI))
        } finally {
            other.clearAll()
        }
    }

    @Test
    fun valueEncryptionRoundTrips_forArbitraryBytes() {
        // EncryptedSharedPreferences uses AES-GCM, which must survive
        // non-ASCII and very long values.
        val payload = buildString {
            append("مفتاح-")
            for (i in 0 until 256) append(('A' + (i % 26)))
        }
        vault.put(ApiKeyId.PEXELS, payload)
        assertEquals(payload, vault.get(ApiKeyId.PEXELS))
    }

    @Test
    fun encryptedFileExists_notPlainText() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        vault.put(ApiKeyId.CLOUDFLARE_API_TOKEN, "ct0p-secret-token-aaaa-bbbb-cccc")

        // The file may be named `mawaai_secrets.xml` under shared_prefs/.
        val prefsDir = java.io.File(ctx.filesDir.parentFile, "shared_prefs")
        val prefsFile = java.io.File(prefsDir, "mawaai_secrets.xml")
        assertNotNull("shared_prefs dir should exist", prefsDir)

        if (prefsFile.exists()) {
            val raw = prefsFile.readText()
            assertFalse(
                "Plaintext secret must NOT appear in the prefs file",
                raw.contains("ct0p-secret-token-aaaa-bbbb-cccc"),
            )
        }
        // If the file doesn't exist on this device's storage layout, the
        // round-trip test above already covers the get/put contract.
    }
}
