package com.mawaai.love.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mawaai.love.app.core.utils.DateUtils
import com.mawaai.love.app.core.utils.QuoteUtils
import com.mawaai.love.app.data.model.MoodEntry
import com.mawaai.love.app.data.model.MoodType
import com.mawaai.love.app.data.remote.aladhan.AladhanClient
import com.mawaai.love.app.data.remote.zenquotes.ZenQuotesClient
import com.mawaai.love.app.data.repository.*
import com.mawaai.love.app.design.ai.AIEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val memoryRepo: MemoryRepository,
    private val moodRepo: MoodRepository,
    private val profileRepo: ProfileRepository,
    // Phase 23 — public-API enrichments. Both clients return null on
    // any failure so the UI can render its base layout regardless of
    // network state.
    private val zenQuotes: ZenQuotesClient,
    private val aladhan: AladhanClient,
    aiEngine: AIEngine
) : ViewModel() {

    /**
     * Whether the AI Scene Generator card should be visible. Gated on
     * Cloudflare Workers AI being configured — when no T2I provider is
     * available the card hides itself rather than presenting a feature
     * that always errors out. Read once at construction; the keys are
     * static across the process lifetime.
     */
    val aiSceneAvailable: Boolean = aiEngine.cloudTextToImageAvailable

    val dailyQuote = QuoteUtils.getDailyQuote()
    val greeting = DateUtils.getTimeGreeting()

    val recentMemory = memoryRepo.getAllMemories()
        .map { it.firstOrNull() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val todayMood = moodRepo.getLatestMood()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val profile = profileRepo.getProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /**
     * Today's Hijri date. Lazily fetched once on first collection,
     * cached for the rest of the calendar day via the standard
     * stateIn behaviour. Null while loading or when offline; the UI
     * gates rendering on a non-null value.
     */
    val todayHijri: StateFlow<AladhanClient.HijriDay?> =
        flow { emit(aladhan.todayHijri()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /**
     * Inspirational English quote sourced from ZenQuotes. Sits alongside
     * the local Arabic quote — the romantic-side voice (Arabic, locally
     * curated, may name Razan) is intentionally kept distinct from the
     * worldly inspirational voice (English, global). Shown with the
     * required `R.string.zenquotes_attribution` text.
     */
    val internationalQuote: StateFlow<ZenQuotesClient.Quote?> =
        flow { emit(zenQuotes.dailyQuote()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun saveMood(mood: MoodType) {
        viewModelScope.launch {
            moodRepo.addMood(MoodEntry(mood = mood))
        }
    }
}
