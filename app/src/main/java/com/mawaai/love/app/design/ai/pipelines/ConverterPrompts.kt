package com.mawaai.love.app.design.ai.pipelines

/**
 * Per-style sampling parameters for ControlNet. Different styles
 * benefit from different (steps, guidance) sweet spots:
 *  - Realistic needs MORE guidance (8.5) to lock in lifelike detail.
 *  - Artistic / minimalist do well at lower guidance (6.5–7.0)
 *    because over-guiding kills the painterly feel.
 *  - Vector wants the default (7.5) — strict edge adherence already
 *    comes from ControlNet's conditioning channel.
 *  - Step count stays at 30 for all styles except realistic (40) —
 *    extra steps disproportionately help skin / fabric / wood
 *    micro-detail.
 */
internal data class CnParams(val steps: Int, val guidance: Double)

/**
 * Maps a [styleId] from the design catalog to a rich English prompt
 * for ControlNet. Style ids must match `conversionStyles` in
 * `assets/data/design_categories.json`. Prompts are intentionally
 * detailed (15+ descriptors each) — ControlNet's output quality is
 * very sensitive to prompt richness, and the previous 4-word prompts
 * landed almost every render in the "generic else" branch which
 * produced muddy, low-detail output.
 *
 * The descriptors follow the "subject, medium, style modifiers,
 * lighting, quality tail" pattern that the SD-1.5 community has
 * converged on. Cultural anchors ("Arabic calligraphy",
 * "Khaleeji aesthetic") are included sparingly so the renderer
 * picks up the Mawaai-flavour without over-constraining the
 * artistic interpretation of the user's sketch.
 */
internal fun stylePromptFor(styleId: String?): String = when (styleId) {
    "vector_clean" ->
        "clean vector illustration, crisp geometric edges, flat shading, " +
        "limited tasteful palette, modern Arabic design sensibility, " +
        "smooth curves, scalable artwork, professional graphic design, " +
        "balanced negative space, premium portfolio quality"
    "artistic" ->
        "expressive artistic illustration, rich textural brushwork, " +
        "painterly digital art, warm Khaleeji color palette, " +
        "elegant flowing composition, refined detail rendering, " +
        "gallery-quality fine art, cinematic atmosphere, " +
        "soft natural lighting, masterpiece composition"
    "minimalist" ->
        "minimalist line art, single accent color, generous negative space, " +
        "delicate continuous linework, modern Scandinavian-meets-Arabic " +
        "aesthetic, elegant simplicity, premium editorial illustration, " +
        "balanced composition, refined typography hints, clean studio finish"
    "realistic" ->
        "photorealistic rendering, natural soft lighting, detailed surface " +
        "textures, accurate material properties, depth of field, " +
        "premium product photography, gallery print quality, true-to-life " +
        "colors, subtle ambient shadows, sharp focal point, 4k detail"
    // "auto" and unknown ids both land here — the prompt is built to
    // produce a strong default that still benefits the user when they
    // don't pick a specific style.
    else ->
        "professional Arabic digital artwork, intricate ornamental detail, " +
        "rich layered composition, balanced warm-and-jewel-tone palette, " +
        "elegant calligraphic line quality, refined cultural motifs, " +
        "high-resolution gallery print, premium illustration finish, " +
        "soft cinematic lighting, masterful detail rendering"
}

/**
 * Banned tokens passed to ControlNet alongside the positive prompt.
 * Beyond the universal failure modes (blurry, watermarks, extra
 * limbs) each style gets its own bans for the artefacts that style
 * is uniquely prone to — minimalist refuses clutter, realistic
 * refuses cartoon flatness, vector refuses painted texture.
 */
internal fun negativePromptFor(styleId: String?): String {
    val base =
        "blurry, low quality, watermark, signature, text, jpeg artefacts, " +
        "deformed, ugly, distorted, extra limbs, mutated, cropped, " +
        "out of frame, harsh shadows, oversaturated, overexposed"
    val styleBans = when (styleId) {
        "vector_clean" -> "painted texture, brush strokes, photographic noise, gradient banding"
        "artistic" -> "flat boring shading, sterile geometric vector look, plastic surface"
        "minimalist" -> "cluttered, busy background, multiple competing colors, ornate flourish"
        "realistic" -> "cartoon, anime, flat shading, illustrated, sketchy, low detail"
        else -> "amateur, childish, low effort, sketchy"
    }
    return "$base, $styleBans"
}

internal fun controlNetParamsFor(styleId: String?): CnParams = when (styleId) {
    "realistic" -> CnParams(steps = 40, guidance = 8.5)
    "artistic" -> CnParams(steps = 30, guidance = 6.8)
    "minimalist" -> CnParams(steps = 28, guidance = 6.5)
    "vector_clean" -> CnParams(steps = 30, guidance = 7.5)
    else -> CnParams(steps = 30, guidance = 7.5)
}
