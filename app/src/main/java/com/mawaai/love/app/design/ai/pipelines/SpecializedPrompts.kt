package com.mawaai.love.app.design.ai.pipelines

/**
 * Phase 24 — Specialized-flow ControlNet support.
 *
 * Build a category-aware prompt that turns the user's sketch into a
 * **photorealistic design** instead of a TFLite-stylised version of
 * itself. The prompt names the medium ("henna ink on skin",
 * "embroidered fabric", "carved plaster") and references the
 * subType so the AI renders the right family of motifs.
 *
 * The output is the *pattern/design* — the [TemplateCompositor]
 * later warps + blends it onto the chosen template (model wearing
 * a toub, a bare hand, a bare wall). That two-step path gives the
 * end user "drawing → real design → on a real subject" instead of
 * the previous "drawing → filter → on a real subject".
 */
internal fun specializedPromptFor(categoryId: String, subTypeId: String?, styleId: String?): String {
    val styleTail = specializedStyleTail(styleId)
    return when (categoryId) {
        "henna" -> {
            val area = when (subTypeId) {
                "palm" -> "the palm of a hand"
                "hand" -> "the back of a hand"
                "foot" -> "the top of a foot"
                "full_arm" -> "a forearm and wrist"
                else -> "skin"
            }
            val regional = when (styleId) {
                "sudanese" -> "traditional Sudanese motifs"
                "arabic" -> "elegant Khaleeji Arabic motifs"
                "indian" -> "intricate Indian mehndi motifs"
                else -> "traditional henna motifs"
            }
            "intricate henna mehndi design rendered on $area, " +
                "rich dark mahogany ink with subtle reddish-brown undertones, " +
                "$regional, fine detail, clean line work, " +
                "studio macro photography, soft natural lighting, sharp focus, " +
                "$styleTail, isolated on neutral skin background, professional photo"
        }

        "abaya" -> {
            val technique = when (subTypeId) {
                "abaya_classic" -> "classic flowing black silk fabric, restrained gold trim"
                "abaya_embroidered" -> "lavish gold thread embroidery, dense floral and geometric motifs"
                "abaya_beaded" -> "hand-set crystal and pearl beadwork, sparkling detail"
                "abaya_modern" -> "modern minimalist cut, refined contemporary detailing"
                "abaya_kaftan" -> "luxurious kaftan style, generous draping, ornate trim"
                else -> "elegant fabric detail"
            }
            "ornate abaya fabric panel, $technique, " +
                "deep midnight black silk base with luminous metallic accents, " +
                "photorealistic textile detail, fine weave visible, " +
                "studio textile photography, soft directional lighting, " +
                "$styleTail, sharp focus, premium fashion catalog quality"
        }

        "thob_sudani" -> {
            val technique = when (subTypeId) {
                "toub_raqma" -> "rich raqma embroidery in coloured thread, traditional Sudanese motifs"
                "toub_fatla" -> "ornate fatla gold-thread work, intricate fine lattice"
                "toub_farda" -> "ceremonial farda silk panel, opulent gold and crimson detail"
                "toub_zaraf" -> "lustrous zaraf silk weave, subtle iridescent sheen"
                else -> "traditional Sudanese textile detail"
            }
            "Sudanese toub fabric panel, $technique, " +
                "lightweight draped fabric with visible weave, photorealistic textile, " +
                "warm gold and ivory palette with deep accent colours, " +
                "studio textile photography, soft directional lighting, sharp focus, " +
                "$styleTail, premium fashion catalog quality"
        }

        "walls" -> {
            val surface = when (subTypeId) {
                "interior_wall" -> "interior plaster wall, warm cream finish"
                "exterior_wall" -> "exterior carved sandstone wall, weathered patina"
                "majlis" -> "Arabic majlis feature wall, rich earthy tones"
                "mihrab" -> "ornate mihrab niche with calligraphic frame"
                "ceiling" -> "ornate Arabic ceiling panel, geometric coffer detail"
                "exhibition" -> "gallery exhibition wall panel, museum-quality"
                else -> "ornate wall surface"
            }
            "$surface adorned with intricate Arabic geometric and calligraphic ornament, " +
                "carved relief detail, hand-painted gold and lapis accents, " +
                "photorealistic architectural photography, " +
                "soft directional lighting that picks out the relief, sharp focus, " +
                "$styleTail, premium architecture catalog quality"
        }

        else ->
            "professional Arabic decorative design, intricate ornamental detail, " +
                "rich layered composition, photorealistic rendering, " +
                "$styleTail, premium catalog quality"
    }
}

/**
 * Negative prompt for [specializedPromptFor]. Bans the failure modes
 * we have observed in earlier prototypes: pencil sketches leaking
 * through fabric prompts, white office paper backgrounds appearing
 * behind walls, cartoonish output for henna, etc.
 */
internal fun specializedNegativePromptFor(categoryId: String): String {
    val universal =
        "blurry, low quality, watermark, signature, text, jpeg artefacts, " +
        "deformed, ugly, distorted, extra limbs, cropped, out of frame"
    val specific = when (categoryId) {
        "henna" ->
            "ink on paper, fabric texture, painting on canvas, drawing on notebook, " +
            "cartoon, anime, flat shading, low detail"
        "abaya", "thob_sudani" ->
            "paper sketch, pencil drawing, line art, mockup, costume, low-poly, " +
            "cartoon, plastic look, low fabric detail"
        "walls" ->
            "paper sketch, fabric texture, person, model, costume, " +
            "ink on paper, white office background, cartoon"
        else ->
            "paper sketch, pencil drawing, low detail, cartoon, amateur"
    }
    return "$universal, $specific"
}

/** Style descriptor appended to specialized prompts. Mirrors the
 *  converter's per-style language so the renderer's behaviour stays
 *  consistent across the two flows. */
internal fun specializedStyleTail(styleId: String?): String = when (styleId) {
    "vector_clean" -> "clean crisp lines, balanced composition"
    "artistic" -> "expressive painterly detail, refined finish"
    "minimalist" -> "minimal restrained motifs, generous negative space"
    "realistic" -> "photorealistic, true-to-life materials, 4k detail"
    else -> "rich detail, balanced composition"
}

/**
 * Builds the prompt for the compose-refine pass. The prompt
 * describes the FINAL desired look — a photorealistic photo with
 * the design naturally integrated into the subject. The img2img
 * call uses low strength so the layout from the input composite
 * is preserved; the prompt mostly steers the refinement of fabric
 * folds, lighting, and edges.
 */
internal fun compositeRefinePromptFor(categoryId: String, subTypeId: String?): String {
    return when (categoryId) {
        "henna" -> {
            val area = when (subTypeId) {
                "palm" -> "the palm of a hand"
                "hand" -> "the back of a hand"
                "foot" -> "the top of a foot"
                "full_arm" -> "a forearm and wrist"
                else -> "skin"
            }
            "photorealistic close-up photo of $area with intricate henna mehndi design, " +
                "rich dark mahogany ink applied to natural skin, " +
                "soft natural lighting, subtle skin pores and natural skin tone variation, " +
                "the henna design follows the natural contours of the skin, " +
                "professional macro photography, sharp focus, high detail"
        }

        "abaya" -> {
            "photorealistic photo of a woman wearing an elegant abaya, " +
                "ornate embroidered design naturally integrated into the fabric, " +
                "natural fabric folds and draping, soft studio lighting, " +
                "consistent fabric weave and texture across the entire garment, " +
                "professional fashion catalog photography, sharp focus"
        }

        "thob_sudani" -> {
            "photorealistic photo of a Sudanese woman wearing a traditional toub, " +
                "the embroidered pattern flows naturally across the lightweight draped fabric, " +
                "warm natural lighting, visible fabric texture and gentle folds, " +
                "the design respects the fabric's drape and movement, " +
                "professional cultural fashion photography, sharp focus"
        }

        "walls" -> {
            "photorealistic architectural photo of an Arabic ornamental wall, " +
                "the decorative pattern is naturally part of the wall surface, " +
                "consistent surface texture, soft directional lighting picking out relief detail, " +
                "subtle ambient shadows, no visible seams or pasted overlay, " +
                "professional architectural photography, sharp focus"
        }

        else ->
            "photorealistic photo with the decorative design naturally integrated, " +
                "natural lighting, consistent texture, sharp focus, professional photography"
    }
}
