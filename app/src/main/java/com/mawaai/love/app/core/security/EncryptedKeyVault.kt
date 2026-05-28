package com.mawaai.love.app.core.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [KeyVault] backed by [EncryptedSharedPreferences].
 *
 * - Master key: AES-256 in `AES256_GCM` mode, stored inside the Android
 *   Keystore via [MasterKey.Builder].
 * - Per-entry encryption: keys (column names) encrypted via
 *   `AES256_SIV` (deterministic so we can lookup by id); values encrypted
 *   via `AES256_GCM` (authenticated).
 * - File location:
 *   `/data/data/com.mawaai.love.app/shared_prefs/mawaai_secrets.xml`
 *   The file is opaque even with root access — values are AES-GCM ciphertext.
 *
 * On first instantiation the master key is generated and stored in the
 * Android Keystore; subsequent process starts reuse it. The constructor is
 * therefore best invoked on a background thread (Hilt does this once per
 * process when the first injection point requests a [KeyVault]).
 *
 * Threading: [EncryptedSharedPreferences] is internally synchronised. We can
 * call `get`/`put` from any thread.
 */
@Singleton
class EncryptedKeyVault @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context,
) : KeyVault {

    private val prefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                PREFS_FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        } catch (t: Throwable) {
            // Defence in depth: if the Keystore is broken on this device (we
            // have seen this on stale Samsung firmware where the user wiped
            // BiometricPrompt enrolment), fail open so the app keeps working
            // off BuildConfig alone. The clients all degrade gracefully when
            // their key is empty.
            Log.e(TAG, "Could not open EncryptedSharedPreferences — falling back to BuildConfig-only mode", t)
            null
        } ?: context.getSharedPreferences("${PREFS_FILE}_unencrypted_FALLBACK", Context.MODE_PRIVATE)
    }

    override fun get(id: ApiKeyId): String {
        val override = runCatching { prefs.getString(storageKey(id), null) }.getOrNull()
        if (!override.isNullOrBlank()) return override
        return id.buildConfigFallback()
    }

    override fun put(id: ApiKeyId, value: String?) {
        val sanitized = value?.trim().orEmpty()
        runCatching {
            prefs.edit().apply {
                if (sanitized.isEmpty()) {
                    remove(storageKey(id))
                } else {
                    putString(storageKey(id), sanitized)
                }
            }.apply()
        }.onFailure {
            Log.w(TAG, "Failed to persist override for ${id.name}", it)
        }
    }

    override fun isOverridden(id: ApiKeyId): Boolean =
        runCatching { prefs.contains(storageKey(id)) }.getOrDefault(false) &&
            runCatching { !prefs.getString(storageKey(id), null).isNullOrBlank() }.getOrDefault(false)

    override fun clearAll() {
        runCatching {
            prefs.edit().apply {
                for (id in ApiKeyId.values()) {
                    remove(storageKey(id))
                }
            }.apply()
        }.onFailure { Log.w(TAG, "Failed to clear KeyVault overrides", it) }
    }

    private fun storageKey(id: ApiKeyId): String = KeyVault.STORAGE_KEY_PREFIX + id.name

    private companion object {
        const val TAG = "EncryptedKeyVault"
        const val PREFS_FILE = "mawaai_secrets"
    }
}
