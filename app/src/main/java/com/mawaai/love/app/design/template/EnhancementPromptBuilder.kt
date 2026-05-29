package com.mawaai.love.app.design.template

/**
 * Structured ControlNet / Stable-Diffusion enhancement prompt produced by
 * [EnhancementPromptBuilder].
 *
 * The shape mirrors the master prompt's Phase 2 `buildEnhancementPrompt(…)`
 * pseudocode so the downstream HuggingFace / Cloudflare client can serialise
 * it directly into its provider-specific request body.
 *
 * @property positive       Full positive prompt: style fragment + cultural
 *                          context + design constraints + quality baseline.
 * @property negative       Comma-separated negative concepts. Defaults to
 *                          [SystemPrompts.ENHANCEMENT_NEGATIVE].
 * @property strength       ControlNet strength, `0.0..1.0`. Lower values
 *                          preserve more of the input structure. Default
 *                          `0.65f` per the master prompt.
 * @property guidanceScale  Classifier-free guidance scale. Higher values make
 *                          the model adhere more strictly to [positive].
 *                          Default `7.5f` per the master prompt.
 */
data class EnhancementPrompt(
    val positive: String,
    val negative: String = SystemPrompts.ENHANCEMENT_NEGATIVE,
    val strength: Float = 0.65f,
    val guidanceScale: Float = 7.5f
)

/**
 * Pure-function builder for [EnhancementPrompt] from a [TemplateContext].
 *
 * Composition:
 *  1. A type-specific *style fragment* (henna mehndi vs. abaya embroidery
 *     vs. mural, etc.). Sourced from the master prompt's Phase 2 spec for
 *     the five enumerated families, extended sensibly for the additional
 *     three families we cover ([TemplateType.HENNA_WRIST],
 *     [TemplateType.SAUDI_THOBE], [TemplateType.BISHT]).
 *  2. The [TemplateContext.culturalOrigin] string.
 *  3. The [TemplateContext.designConstraints] list, comma-joined, including
 *     any vision-detected style hint appended by [TemplateAnalyzer].
 *  4. The universal quality baseline ([SystemPrompts.ENHANCEMENT_QUALITY]).
 *
 * The negative prompt and ControlNet / guidance defaults come from
 * [EnhancementPrompt]'s data-class defaults so call sites can override only
 * the bits they need (e.g. lower `strength` for stricter input preservation).
 */
object EnhancementPromptBuilder {

    fun build(context: TemplateContext): EnhancementPrompt {
        val style = styleFragmentFor(context.type)
        val cultural = context.culturalOrigin
        val constraints = context.designConstraints.joinToString(separator = ". ")

        val positive = listOfNotNull(
            style,
            cultural,
            constraints.takeIf { it.isNotBlank() },
            SystemPrompts.ENHANCEMENT_QUALITY
        ).joinToString(separator = ", ")

        return EnhancementPrompt(positive = positive)
    }

    private fun styleFragmentFor(type: TemplateType): String = when (type) {
        TemplateType.HENNA_FOOT ->
            "Traditional henna mehndi design on foot, " +
                "intricate floral arabesque patterns, dark brown natural henna color, " +
                "realistic skin texture integration, professional mehndi artist quality"

        TemplateType.HENNA_PALM ->
            "Traditional palm henna mehndi, mandala center composition, " +
                "finger extension patterns, authentic mehndi artist style"

        TemplateType.HENNA_WRIST ->
            "Traditional wrist henna band, circular flow design, " +
                "delicate accent lines spilling onto the back of the hand"

        TemplateType.SUDANESE_TOUB ->
            "Traditional Sudanese toub embroidery, Nubian geometric patterns, " +
                "gold and silver thread work, authentic needlework texture, " +
                "cultural textile artistry"

        TemplateType.ISLAMIC_ABAYA ->
            "Islamic abaya embroidery design, traditional arabesque motifs, " +
                "luxury gold thread embroidery, professional fashion atelier quality"

        TemplateType.SAUDI_THOBE ->
            "Traditional Saudi thobe embroidery, subtle tonal needlework, " +
                "collar and chest placket focal patterns"

        TemplateType.BISHT ->
            "Traditional Khaleeji bisht with signature gold trim, " +
                "ceremonial cloak craftsmanship, dark wool with vertical front trim"

        TemplateType.MURAL ->
            "Professional wall mural art, architectural integration, " +
                "natural lighting and depth, gallery quality finish"

        TemplateType.CUSTOM ->
            "High quality artistic design, balanced composition, professional finish"
    }
}
