package com.mawaai.love.app.design.ai.render

import com.mawaai.love.app.design.ai.intelligence.SurfaceProfile

/**
 * Surface-aware realism instructions for image generation.
 *
 * Keep this file deterministic and provider-agnostic: every AI provider should
 * receive the same material, camera, and rejection language so generated
 * drawings move toward physical product photography rather than flat artwork.
 */
object RenderRealismPolicy {

    fun realismDirection(profile: SurfaceProfile): String = when (profile) {
        SurfaceProfile.SkinPalm,
        SurfaceProfile.SkinHandFull,
        SurfaceProfile.SkinFoot ->
            "PHOTOREALISTIC HENNA: Transform the sketch into real henna on human skin. Preserve motif layout, but render raised paste, natural stain edges, pores, tiny skin creases, knuckle/instep curvature, and soft absorption into skin."

        SurfaceProfile.FabricAbaya,
        SurfaceProfile.FabricThobe,
        SurfaceProfile.FabricToub ->
            "PHOTOREALISTIC GARMENT: Transform the sketch into embroidery, print, or woven ornament on real fabric. Preserve composition while following seams, folds, cloth tension, drape direction, and thread/ink interaction with the textile."

        SurfaceProfile.WallStone,
        SurfaceProfile.WallPlaster,
        SurfaceProfile.WallArch ->
            "PHOTOREALISTIC ARCHITECTURAL SURFACE: Transform the sketch into real carved, painted, or incised ornament on a wall. Preserve geometry while adding wall grain, shallow relief, dust, edge wear, groove shadows, and believable architectural lighting."

        SurfaceProfile.CeramicPlate,
        SurfaceProfile.CeramicTile,
        SurfaceProfile.CeramicMug ->
            "PHOTOREALISTIC CERAMIC: Transform the sketch into real fired pigment under glossy glaze. Preserve motif structure while adding glaze depth, curved specular highlights, ceramic thickness, rim/edge curvature, and product-photography reflections."
    }

    fun materialPhysics(profile: SurfaceProfile): String =
        "MATERIAL PHYSICS: ${profile.materialResponse}. ${profile.maskingRules.joinToString(". ")}. ${profile.perspectiveRules.joinToString(". ")}. The design must be integrated into the surface, never floating above it."

    fun cameraAndLighting(profile: SurfaceProfile): String = when (profile) {
        SurfaceProfile.SkinPalm,
        SurfaceProfile.SkinHandFull,
        SurfaceProfile.SkinFoot ->
            "CAMERA + LIGHTING: realistic close-up beauty/product photography, soft window key light, gentle skin speculars, natural shadow in creases, shallow but not blurry depth of field."

        SurfaceProfile.FabricAbaya,
        SurfaceProfile.FabricThobe,
        SurfaceProfile.FabricToub ->
            "CAMERA + LIGHTING: fashion editorial product photo, softbox key light, readable fold shadows, subtle thread highlights, fabric microtexture visible without oversharpening."

        SurfaceProfile.WallStone,
        SurfaceProfile.WallPlaster,
        SurfaceProfile.WallArch ->
            "CAMERA + LIGHTING: architectural interior photography, warm grazing light, ambient occlusion in carved grooves, diffuse wall reflection, straight natural lens perspective."

        SurfaceProfile.CeramicPlate,
        SurfaceProfile.CeramicTile,
        SurfaceProfile.CeramicMug ->
            "CAMERA + LIGHTING: premium ceramic product photography, large softbox reflection, controlled glossy highlights, contact shadows, realistic tabletop/environment reflection."
    }

    fun negativePrompt(profile: SurfaceProfile): String {
        val surfaceSpecific = when (profile) {
            SurfaceProfile.SkinPalm,
            SurfaceProfile.SkinHandFull,
            SurfaceProfile.SkinFoot ->
                "extra fingers, broken anatomy, tattoo-machine look, flat brown sticker, muddy stain"

            SurfaceProfile.FabricAbaya,
            SurfaceProfile.FabricThobe,
            SurfaceProfile.FabricToub ->
                "plastic fabric, flat printed decal, embroidery floating above cloth, impossible folds"

            SurfaceProfile.WallStone,
            SurfaceProfile.WallPlaster,
            SurfaceProfile.WallArch ->
                "paper poster on wall, fake bevel, misaligned architecture, warped tiles"

            SurfaceProfile.CeramicPlate,
            SurfaceProfile.CeramicTile,
            SurfaceProfile.CeramicMug ->
                "sticker on ceramic, matte plastic glaze, broken rim, impossible reflection"
        }
        return "AVOID: cartoon, vector art, flat mockup, sticker look, pasted overlay, floating design, inconsistent shadows, low resolution, blurry motif, oversaturated colors, watermark, logo, text labels, UI, border, collage, $surfaceSpecific."
    }
}