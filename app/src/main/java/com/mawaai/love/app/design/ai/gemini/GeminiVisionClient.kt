package com.mawaai.love.app.design.ai.gemini

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.mawaai.love.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sends a drawing bitmap to Gemini 1.5 Flash and asks for short Arabic
 * suggestions that *enhance* (not replace) the drawing. Returns an empty
 * list when no API key is configured or the network call fails — the
 * UI then falls back to the local heuristic recommender.
 *
 * **Phase 10 reasoning upgrade.** The model is now asked to *think* in
 * three steps before producing suggestions:
 *  1. Describe what it actually sees in the drawing.
 *  2. Identify the strongest visual element (the one the user clearly
 *     cared most about).
 *  3. Suggest improvements that **build on** that strongest element,
 *     never replace it.
 *
 * This chain-of-thought structure dramatically improves the relevance
 * of suggestions on real drawings: instead of generic tips like "add
 * color in the background" the model produces grounded suggestions
 * like "أضيفي ظلال رفيعة حول البتلات لإبراز التدرج" (add thin shadows
 * around the petals to bring out the gradient).
 *
 * The first two reasoning steps are discarded from the final output —
 * the parser only keeps the suggestion lines tagged with the marker
 * `[SUGGESTION]`. Tag-based parsing is more robust than "last N lines"
 * heuristics across the model's occasional verbose digressions.
 */
@Singleton
class GeminiVisionClient @Inject constructor(
    private val api: GeminiApi
) {

    val isConfigured: Boolean get() = BuildConfig.GEMINI_API_KEY.isNotBlank()

    suspend fun suggestionsForDrawing(
        drawing: Bitmap,
        categoryId: String?,
        count: Int = 5
    ): List<String> {
        if (!isConfigured) return emptyList()

        val categoryHint = when (categoryId) {
            "henna" -> "هذا تصميم حناء، ركّز على نقوش الحناء التقليدية وتدرّج اللون البني والخطوط الدقيقة عند أطراف الأصابع."
            "abaya" -> "هذا تصميم عباية، ركّز على التطريز التقليدي والقصة الإسلامية وثنيات القماش وانسياب القماش."
            "walls" -> "هذا تصميم جدارية، ركّز على التكوين الزخرفي على الجدار، مركزية العنصر الرئيسي، والتوازن مع الفراغ المحيط."
            "thob_sudani" -> "هذا توب سوداني تقليدي، ركّز على الرقمة والفتلة والخيط الذهبي وانسياب القماش."
            else -> "هذا رسم فني عام يحتاج إلى تحسينات تحافظ على روحه."
        }

        val prompt = """
            أنتِ معلّمة فن عربية محترفة. مهمتكِ أن تساعدي الفنانة على تحسين رسمتها دون تغيير فكرتها.

            **خطوات التفكير (مهم):**
            STEP 1: انظري إلى الرسمة بدقة. صفي بسطر واحد ما ترينه فعلياً (الأشكال، الألوان، النمط).
            STEP 2: حدّدي العنصر الأقوى في الرسمة — العنصر الذي يبدو أن الفنانة بذلت فيه أكبر جهد.
            STEP 3: $categoryHint
                ثم اكتبي $count اقتراحات تبني على العنصر الأقوى وتدعمه، ولا تستبدله.

            **قواعد إخراج الاقتراحات:**
            - ابدئي كل اقتراح بالعلامة `[SUGGESTION]` ثم مسافة ثم نص الاقتراح.
            - كل اقتراح من ٥ إلى ١٢ كلمة.
            - الاقتراحات يجب ألا تغيّر هوية الرسم العامة.
            - كوني محدّدة (مثل: "أضيفي ظلاً ناعماً تحت بتلات الزهرة" — وليس "أضيفي ظلالاً").
            - بدون أرقام، بدون شرطات، بدون رموز تعبيرية.

            **الإخراج:**
            STEP 1: <وصف>
            STEP 2: <العنصر الأقوى>
            [SUGGESTION] <اقتراح ١>
            [SUGGESTION] <اقتراح ٢>
            ...
        """.trimIndent()

        // Chain-of-thought config: temperature 0.45 (the prompt does the
        // creative legwork; we want consistent grounded suggestions, not
        // wide exploration), 640 max tokens (headroom for 2 reasoning
        // lines + `count` suggestions).
        val raw = request(drawing, prompt, temperature = 0.45f, maxTokens = 640, errorTag = "suggestions")
            ?: return emptyList()
        return parseSuggestions(raw, count)
    }

    /**
     * Asks Gemini Vision to pick the best converter style for [sketch]
     * out of the four catalog styles. Returns one of `vector_clean`,
     * `artistic`, `minimalist`, `realistic`, or null on any failure
     * (no API key, network error, unparseable response).
     *
     * The local `AutoStylePicker` is fast and deterministic but blind
     * to semantics — it sees coverage / hues / edge sharpness but not
     * "this looks like a flower" or "this looks like a portrait". Vision
     * has the semantic context; pairing the two (Vision first, local
     * fallback) gives the best of both.
     *
     * Model output is constrained to a SINGLE WORD prefixed with
     * `[STYLE] ` so the parser cannot be misled by chatty model output.
     * If the model emits an unknown style id (typo, hallucination),
     * the parser returns null and the AIEngine falls back to the local
     * heuristic — never crashes or routes to a fake style id.
     */
    suspend fun classifyStyle(sketch: Bitmap): String? {
        val prompt = """
            انظري إلى الرسمة المرفقة واختاري أنسب أسلوب فني واحد فقط من القائمة:
            - vector_clean : أشكال هندسية واضحة، حواف حادة، ألوان قليلة (مناسب للوغو وأيقونة).
            - artistic    : ألوان غنية، فرشاة معبّرة، تدرّجات (مناسب للوحة فنية).
            - minimalist  : خطوط بسيطة، ألوان قليلة، مساحات بيضاء (مناسب للرسم الأنيق).
            - realistic   : ظلال وإضاءة وعمق (مناسب لمحاكاة الصور الواقعية).

            القواعد:
            - الإخراج سطر واحد فقط: `[STYLE] <id>` حيث `<id>` كلمة واحدة من القائمة بالإنجليزية.
            - لا تضيفي أي شيء قبل أو بعد السطر.
            - إذا كانت الرسمة فارغة أو غير واضحة، ردّي `[STYLE] auto`.
        """.trimIndent()

        // Temperature 0.1: this is a 1-of-5 classification, not a creative
        // task — we want determinism. 32 tokens is plenty for a one-word
        // answer.
        val raw = request(sketch, prompt, temperature = 0.1f, maxTokens = 32, errorTag = "classifyStyle")
            ?: return null

        val pick = raw.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.startsWith(STYLE_TAG) }
            ?.removePrefix(STYLE_TAG)
            ?.trimStart(':', ' ', '\t')
            ?.trim()
            ?.lowercase()
            ?: return null

        // Whitelist — reject hallucinated styles ("watercolor", "anime",
        // "neon", etc.). The caller treats null as "use local fallback".
        return when (pick) {
            "vector_clean", "artistic", "minimalist", "realistic", "auto" -> pick
            else -> null
        }
    }

    /**
     * Takes a user's sketch + a target visual style and asks Gemini
     * Vision to write a tailored English ControlNet prompt that captures
     * **this specific sketch** rendered in that style. Returns null on
     * any failure so the caller can fall back to the static
     * `stylePromptFor` baseline in `AIEngine`.
     *
     * The model is constrained to a single-line output prefixed with
     * `[PROMPT]` so the parser can ignore any preamble. We never trust
     * the model with the full prompt template — only with the
     * "describe-this-sketch" part — and prefix our own style anchor and
     * quality tail to guarantee minimum quality even if the model
     * returns something tame.
     *
     * Example output (sketch of a flower with three petals, style
     * artistic): the model might emit:
     *   "three soft-edged tulip petals with cascading watercolor wash,
     *    delicate stem, blush-pink palette"
     * and we return:
     *   "expressive artistic illustration, three soft-edged tulip petals
     *    with cascading watercolor wash, delicate stem, blush-pink
     *    palette, warm Khaleeji color palette, refined detail rendering,
     *    gallery-quality fine art, soft natural lighting"
     *
     * This is "thinking before drawing" — Vision sees the sketch and
     * crafts the prompt instead of the engine guessing blindly.
     */
    suspend fun tailoredControlNetPrompt(
        sketch: Bitmap,
        styleId: String
    ): String? {
        val anchor = anchorFor(styleId) ?: return null

        val prompt = """
            انظري إلى الرسمة المرفقة، ثم اكتبي وصفاً قصيراً جداً باللغة الإنجليزية لما تظهره الرسمة فعلياً.
            القواعد:
            - السطر الواحد يبدأ بالعلامة `[PROMPT]` ثم مسافة ثم الوصف.
            - الوصف من ٨ إلى ٢٠ كلمة فقط (إنجليزية).
            - صفي الموضوع والعناصر المرئية فقط (الأشكال، الألوان، الملمس).
            - لا تذكري كلمة "sketch" أو "drawing" أو "image" — اكتبي الموضوع مباشرة.
            - لا تضيفي أي شيء قبل أو بعد السطر الواحد.

            مثال الإخراج:
            [PROMPT] three soft pink tulip petals with delicate stem and warm blush palette
        """.trimIndent()

        val raw = request(sketch, prompt, temperature = 0.35f, maxTokens = 96, errorTag = "tailoredPrompt")
            ?: return null

        val core = raw.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.startsWith(PROMPT_TAG) }
            ?.removePrefix(PROMPT_TAG)
            ?.trimStart(':', ' ', '\t')
            ?.trim()
            ?.takeIf { it.length in 8..220 }
            ?: return null

        return "$anchor, $core, $QUALITY_TAIL"
    }

    /**
     * Sends the user's [sketch] and the ControlNet-generated [output]
     * to Gemini Vision and asks the model to grade how well the output
     * captures the sketch as a `styleId` rendering. Returns an Int in
     * `[1..5]` or null on any failure (no API key, network error,
     * unparseable response).
     *
     * 1 = output looks nothing like the sketch, or has severe artefacts
     * 2 = output is recognisable but loses major elements
     * 3 = decent rendering, minor issues
     * 4 = good — captures intent + style faithfully
     * 5 = excellent — exceeds expectations
     *
     * The AIEngine uses this rating to decide whether to retry with
     * stronger sampling parameters. Cheap (1.5-2s per call) and runs
     * exactly once per render even when retry fires (the retry's own
     * grade gates further retries, capped at one).
     *
     * Model output is constrained to a single line `[GRADE] N` so the
     * parser can ignore any preamble. Temperature 0.1 for determinism;
     * 16 max tokens because the entire response is "[GRADE] 4".
     */
    suspend fun gradeOutput(
        sketch: Bitmap,
        output: Bitmap,
        styleId: String
    ): Int? {
        val styleHint = when (styleId) {
            "vector_clean" -> "نظيف ومتجه، حواف حادة، ألوان قليلة"
            "artistic" -> "فني معبّر، فرشاة وملمس"
            "minimalist" -> "مبسّط، خطوط قليلة، مساحات بيضاء"
            "realistic" -> "واقعي بإضاءة وعمق"
            else -> "أسلوب فني عام"
        }
        val prompt = """
            الصورة الأولى هي رسمة الفنانة الأصلية.
            الصورة الثانية هي إخراج الذكاء الاصطناعي لنفس الرسمة بأسلوب: $styleHint.

            قيّمي مدى جودة الإخراج كنسخة من الرسمة بهذا الأسلوب على مقياس من ١ إلى ٥:
            - 1: الإخراج لا يشبه الرسمة أو فيه تشوّهات شديدة.
            - 2: الإخراج معروف لكن فقد عناصر مهمة.
            - 3: إخراج معقول مع أخطاء بسيطة.
            - 4: إخراج جيّد ويلتقط الفكرة والأسلوب.
            - 5: إخراج ممتاز يفوق التوقعات.

            القواعد:
            - الإخراج سطر واحد فقط: `[GRADE] N` حيث `N` رقم بين 1 و 5.
            - لا تضيفي شرحاً قبل أو بعد السطر.
        """.trimIndent()

        val raw = request(
            bitmaps = listOf(sketch, output),
            prompt = prompt,
            temperature = 0.1f,
            maxTokens = 16,
            errorTag = "gradeOutput"
        ) ?: return null

        val number = raw.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.startsWith(GRADE_TAG) }
            ?.removePrefix(GRADE_TAG)
            ?.trimStart(':', ' ', '\t')
            ?.trim()
            ?.toIntOrNull()
            ?: return null

        return number.coerceIn(1, 5)
    }

    /**
     * Single-image convenience wrapper around the variadic [request]
     * overload. Most callers (suggestions, classification, tailored
     * prompt) send exactly one image; grading sends two.
     */
    private suspend fun request(
        bitmap: Bitmap,
        prompt: String,
        temperature: Float,
        maxTokens: Int,
        errorTag: String
    ): String? = request(listOf(bitmap), prompt, temperature, maxTokens, errorTag)

    /**
     * Single private helper that performs the prompt + image upload
     * round-trip to Gemini Vision. Returns the joined text of the first
     * candidate's parts, or null on any failure (no API key, network
     * error, empty response). Centralises:
     *  - API-key gating (no network call when the key is blank),
     *  - JPEG encoding + base-64 + `image/jpeg` MIME tagging,
     *  - the `runCatching { withContext(IO) { … } }` boilerplate,
     *  - the `Log.w(TAG, errorTag, throwable)` failure trace.
     *
     * Pre-Phase-13 this body was inlined three times across
     * [suggestionsForDrawing], [classifyStyle], and
     * [tailoredControlNetPrompt] — ~120 lines of duplication that drifted
     * over multiple edits. Centralising lets each public method stay
     * focused on its prompt + parser. Phase 16 generalises the bitmap
     * arg to a list so multi-image prompts (e.g. grading
     * `sketch + generated`) can share the same plumbing.
     */
    private suspend fun request(
        bitmaps: List<Bitmap>,
        prompt: String,
        temperature: Float,
        maxTokens: Int,
        errorTag: String
    ): String? {
        val key = BuildConfig.GEMINI_API_KEY
        if (key.isBlank()) return null
        return runCatching {
            withContext(Dispatchers.IO) {
                val parts = mutableListOf<GeminiRequest.Part>(GeminiRequest.Part(text = prompt))
                for (bmp in bitmaps) {
                    val base64 = encodeJpeg(bmp)
                    parts += GeminiRequest.Part(
                        inlineData = GeminiRequest.InlineData(
                            mimeType = "image/jpeg",
                            data = base64
                        )
                    )
                }
                val response = api.generateContent(
                    model = MODEL,
                    apiKey = key,
                    body = GeminiRequest(
                        contents = listOf(GeminiRequest.Content(parts = parts)),
                        generationConfig = GeminiRequest.GenerationConfig(
                            temperature = temperature,
                            maxOutputTokens = maxTokens
                        )
                    )
                )
                response.candidates
                    ?.firstOrNull()
                    ?.content
                    ?.parts
                    ?.mapNotNull { it.text }
                    ?.joinToString(separator = "\n")
                    .orEmpty()
            }
        }.getOrElse {
            Log.w(TAG, "Gemini Vision $errorTag failed", it)
            null
        }
    }

    /**
     * Style-anchor prefix prepended to the tailored description. Mirrors
     * the leading phrase of `AIEngine.stylePromptFor` so the renderer
     * gets the same medium-level guidance whether the prompt came from
     * the static path or the Vision-tailored path.
     */
    private fun anchorFor(styleId: String): String? = when (styleId) {
        "vector_clean" -> "clean vector illustration, crisp geometric edges, flat shading"
        "artistic" -> "expressive artistic illustration, rich textural brushwork, painterly digital art"
        "minimalist" -> "minimalist line art, single accent color, generous negative space"
        "realistic" -> "photorealistic rendering, natural soft lighting, detailed surface textures"
        // The "auto" default doesn't have a single anchor; let the caller
        // fall back to the static prompt instead of guessing.
        else -> null
    }

    /**
     * Pulls `[SUGGESTION]`-tagged lines out of the chain-of-thought
     * response. If the model didn't tag anything (older or off-script
     * responses), falls back to the previous "filter by length" rule so
     * the screen still shows usable suggestions instead of going empty.
     */
    private fun parseSuggestions(raw: String, count: Int): List<String> {
        if (raw.isBlank()) return emptyList()
        val tagged = raw.lineSequence()
            .map { it.trim() }
            .filter { it.startsWith(SUGGESTION_TAG) }
            .map { it.removePrefix(SUGGESTION_TAG).trimStart(':', ' ', '\t').trim() }
            .filter { it.isNotEmpty() && it.length in 5..160 }
            .take(count)
            .toList()
        if (tagged.isNotEmpty()) return tagged
        return raw.lineSequence()
            .map { it.trim().trimStart('-', '*', '•', '·', '.', ' ').trim() }
            .filter { line ->
                line.isNotEmpty() &&
                    line.length in 5..160 &&
                    !line.startsWith("STEP", ignoreCase = true) &&
                    !line.startsWith("##") &&
                    !line.startsWith("**")
            }
            .take(count)
            .toList()
    }

    private fun encodeJpeg(bitmap: Bitmap): String {
        val resized = resizeForUpload(bitmap)
        val stream = ByteArrayOutputStream()
        resized.compress(Bitmap.CompressFormat.JPEG, 80, stream)
        if (resized !== bitmap) resized.recycle()
        return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    }

    /** Downsize to keep upload under ~256 KB; quality is fine for analysis. */
    private fun resizeForUpload(bitmap: Bitmap): Bitmap {
        val max = maxOf(bitmap.width, bitmap.height)
        if (max <= MAX_UPLOAD_DIMENSION) return bitmap
        val scale = MAX_UPLOAD_DIMENSION.toFloat() / max
        val w = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val h = (bitmap.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, w, h, true)
    }

    private companion object {
        const val TAG = "GeminiVisionClient"
        // Google retired the `-latest` alias on this endpoint mid-2025;
        // requests now 404 with that suffix. The canonical name resolves
        // to the current stable build of 1.5-flash without changing the
        // request shape or quota tier.
        const val MODEL = "gemini-1.5-flash"
        const val MAX_UPLOAD_DIMENSION = 768
        // Marker the model emits in front of each suggestion line in
        // the chain-of-thought response. Used by `parseSuggestions` to
        // discard the upstream STEP-1 / STEP-2 reasoning lines.
        const val SUGGESTION_TAG = "[SUGGESTION]"
        // Marker for the tailored ControlNet prompt single-line output.
        const val PROMPT_TAG = "[PROMPT]"
        // Marker for the single-word style classification output.
        const val STYLE_TAG = "[STYLE]"
        // Marker for the single-digit grade output (Phase 16 — Vision
        // self-grades the ControlNet result; AIEngine retries on low
        // grades).
        const val GRADE_TAG = "[GRADE]"
        // Universal quality tail appended to every tailored prompt so
        // the renderer still hits the high-quality lighting / detail
        // tokens we know SD-1.5 responds well to.
        const val QUALITY_TAIL =
            "refined detail rendering, premium illustration finish, " +
            "soft cinematic lighting, gallery print quality, masterpiece composition"
    }
}
