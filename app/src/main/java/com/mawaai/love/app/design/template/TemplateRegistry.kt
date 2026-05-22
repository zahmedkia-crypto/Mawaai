package com.mawaai.love.app.design.template

/**
 * Static lookup from a [TemplateType] to its predefined [TemplateContext].
 *
 * Centralises the mapping so callers (`TemplateAnalyzer`, the upcoming AI
 * orchestrator) don't reach into [HennaTemplates] / [GarmentTemplates]
 * directly and don't duplicate `when (type) { … }` branches.
 *
 * Returns `null` for types that intentionally have no static base context:
 *  - [TemplateType.MURAL]   — murals render through the cinematic Showcase
 *                            system (`design/showcase/`), not the standard
 *                            template compositor, so they don't share a
 *                            base spec.
 *  - [TemplateType.CUSTOM]  — a fallback bucket for user-supplied templates
 *                            that don't fit a known family.
 *
 * Callers must handle `null` (e.g. by emitting a generic prompt or skipping
 * the analyser).
 */
object TemplateRegistry {

    fun contextFor(type: TemplateType): TemplateContext? = when (type) {
        TemplateType.HENNA_FOOT    -> HennaTemplates.FOOT_TEMPLATE
        TemplateType.HENNA_PALM    -> HennaTemplates.PALM_TEMPLATE
        TemplateType.HENNA_WRIST   -> HennaTemplates.WRIST_TEMPLATE
        TemplateType.SUDANESE_TOUB -> GarmentTemplates.SUDANESE_TOUB
        TemplateType.ISLAMIC_ABAYA -> GarmentTemplates.ISLAMIC_ABAYA
        TemplateType.SAUDI_THOBE   -> GarmentTemplates.SAUDI_THOBE
        TemplateType.BISHT         -> GarmentTemplates.BISHT
        TemplateType.MURAL         -> null
        TemplateType.CUSTOM        -> null
    }
}
