package com.mawaai.love.app.design.ai.intelligence

import com.mawaai.love.app.data.database.entities.TemplateEntity

object SurfaceCatalog {

    private val ALL: List<SurfaceProfile> = listOf(
        SurfaceProfile.SkinPalm, SurfaceProfile.SkinHandFull, SurfaceProfile.SkinFoot,
        SurfaceProfile.FabricAbaya, SurfaceProfile.FabricThobe, SurfaceProfile.FabricToub,
        SurfaceProfile.WallStone, SurfaceProfile.WallPlaster, SurfaceProfile.WallArch,
        SurfaceProfile.CeramicPlate, SurfaceProfile.CeramicTile, SurfaceProfile.CeramicMug,
    )

    private val BY_ID: Map<String, SurfaceProfile> = ALL.associateBy { it.id }

    fun byId(id: String): SurfaceProfile? = BY_ID[id]

    /**
     * Resolve a SurfaceProfile from a TemplateEntity using the same heuristics
     * as Lovable's resolveTemplateSurface(). Falls back to a default profile if
     * no match is found.
     */
    fun forTemplate(template: TemplateEntity): SurfaceProfile {
        val name = template.name.lowercase()
        val category = template.category.lowercase()

        // Henna by sub-surface
        if (category == "henna" && "palm" in name) return SurfaceProfile.SkinPalm
        if (category == "henna" && "hand" in name) return SurfaceProfile.SkinHandFull
        if (category == "henna" && "foot" in name) return SurfaceProfile.SkinFoot

        // Garments by name
        if ("abaya" in name) return SurfaceProfile.FabricAbaya
        if ("thobe" in name || "thob" in name) return SurfaceProfile.FabricThobe
        if ("toub" in name) return SurfaceProfile.FabricToub

        // Walls by name
        if ("stone" in name) return SurfaceProfile.WallStone
        if ("arch" in name) return SurfaceProfile.WallArch
        if ("plaster" in name) return SurfaceProfile.WallPlaster

        // Ceramics by name
        if ("plate" in name) return SurfaceProfile.CeramicPlate
        if ("tile" in name) return SurfaceProfile.CeramicTile
        if ("mug" in name) return SurfaceProfile.CeramicMug

        // Last-resort by explicit surface_type column
        BY_ID[template.surfaceType]?.let { return it }

        // Absolute fallback — pick a permissive default for the category
        return when (category) {
            "henna" -> SurfaceProfile.SkinPalm
            "garment" -> SurfaceProfile.FabricAbaya
            "wall" -> SurfaceProfile.WallPlaster
            "ceramic" -> SurfaceProfile.CeramicTile
            else -> SurfaceProfile.WallPlaster
        }
    }
}
