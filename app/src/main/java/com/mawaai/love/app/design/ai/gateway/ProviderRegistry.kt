package com.mawaai.love.app.design.ai.gateway

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Hilt-injectable registry that assembles a [VisionFallbackChain] or
 * [TextFallbackChain] for the current user-configured mode + order.
 *
 * Modes:
 * - "AUTO" (default): use the user-defined order (default = [ProviderId.DEFAULT_VISION_ORDER])
 * - Any [ProviderId.name]: pin a single provider, the chain contains only that one.
 *
 * Preferences are persisted via the shared `mawaai_prefs` DataStore.
 */
@Singleton
class ProviderRegistry @Inject constructor(
    private val visionProviders: Set<@JvmSuppressWildcards VisionProvider>,
    private val textProviders: Set<@JvmSuppressWildcards TextProvider>,
    private val prefs: DataStore<Preferences>,
) {

    /**
     * Build the vision chain for the current user preferences. The default
     * chain (when nothing is persisted) is [ProviderId.DEFAULT_VISION_ORDER].
     */
    suspend fun activeVisionChain(): VisionFallbackChain {
        val ordered = resolveOrder(
            modeKey = MODE_KEY_VISION,
            orderKey = ORDER_KEY_VISION,
            defaultOrder = ProviderId.DEFAULT_VISION_ORDER,
        )
        val chain = ordered.mapNotNull { id -> visionProviders.firstOrNull { it.id == id } }
        return VisionFallbackChain(chain)
    }

    suspend fun activeTextChain(): TextFallbackChain {
        val ordered = resolveOrder(
            modeKey = MODE_KEY_TEXT,
            orderKey = ORDER_KEY_TEXT,
            defaultOrder = ProviderId.DEFAULT_TEXT_ORDER,
        )
        val chain = ordered.mapNotNull { id -> textProviders.firstOrNull { it.id == id } }
        return TextFallbackChain(chain)
    }

    /** Snapshot of which providers are wired up + configured right now. */
    fun knownVisionProviders(): List<VisionProviderStatus> =
        visionProviders.sortedBy { it.id.ordinal }.map { p ->
            VisionProviderStatus(id = p.id, isConfigured = p.isConfigured)
        }

    /** Persist the user's vision mode selection ("AUTO" or [ProviderId.name]). */
    suspend fun setVisionMode(mode: String) = prefs.edit { it[MODE_KEY_VISION] = mode }

    /** Persist the user's text mode selection. */
    suspend fun setTextMode(mode: String) = prefs.edit { it[MODE_KEY_TEXT] = mode }

    /** Persist the user's vision chain order (only honored when mode is "AUTO"). */
    suspend fun setVisionOrder(order: List<ProviderId>) =
        prefs.edit { it[ORDER_KEY_VISION] = order.map(ProviderId::name).toSet() }

    suspend fun setTextOrder(order: List<ProviderId>) =
        prefs.edit { it[ORDER_KEY_TEXT] = order.map(ProviderId::name).toSet() }

    // ---- internals ----

    private suspend fun resolveOrder(
        modeKey: Preferences.Key<String>,
        orderKey: Preferences.Key<Set<String>>,
        defaultOrder: List<ProviderId>,
    ): List<ProviderId> {
        val snapshot = prefs.data.first()
        val mode = snapshot[modeKey] ?: MODE_AUTO

        if (mode != MODE_AUTO) {
            val pinned = runCatching { ProviderId.valueOf(mode) }.getOrNull()
            return listOfNotNull(pinned)
        }

        // AUTO mode — use the user-defined order, falling back to default.
        val rawOrder = snapshot[orderKey]
        if (rawOrder.isNullOrEmpty()) return defaultOrder

        val parsed = rawOrder.mapNotNull { runCatching { ProviderId.valueOf(it) }.getOrNull() }
        if (parsed.isEmpty()) return defaultOrder

        // Append any providers the user didn't include so they remain
        // reachable in AUTO mode (e.g. a newly-released provider added after
        // the user last touched Settings).
        val seen = parsed.toSet()
        val tail = defaultOrder.filter { it !in seen }
        return parsed + tail
    }

    companion object {
        const val MODE_AUTO = "AUTO"

        val MODE_KEY_VISION = stringPreferencesKey("ai_vision_provider_mode")
        val MODE_KEY_TEXT = stringPreferencesKey("ai_text_provider_mode")
        val ORDER_KEY_VISION = stringSetPreferencesKey("ai_vision_provider_order")
        val ORDER_KEY_TEXT = stringSetPreferencesKey("ai_text_provider_order")
    }
}

/** Lightweight view of a provider for Settings + diagnostics. */
data class VisionProviderStatus(
    val id: ProviderId,
    val isConfigured: Boolean,
)
