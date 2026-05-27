package com.mawaai.love.app.design.ai.intelligence

/**
 * Surface-specific render direction strings. Sent to the image-edit model
 * (Gemini 2.5 Flash Image via the gateway) as the "base direction" for each
 * surface. Ported verbatim from Lovable Creative Studio render.functions.ts.
 */
object SurfaceDirections {

    fun forProfile(profile: SurfaceProfile): String = when (profile) {
        SurfaceProfile.SkinPalm ->
            "Render this sketch as authentic henna (mehndi) freshly applied on a person's open palm and fingers. Deep reddish-brown henna paste with slightly raised wet lines, soft natural skin tones, warm daylight, photographic realism."

        SurfaceProfile.SkinHandFull ->
            "Render this sketch as authentic henna applied across the back of a hand, wrist, and lower forearm. Deep reddish-brown paste, fine raised lines, realistic skin, natural lighting, photographic detail."

        SurfaceProfile.SkinFoot ->
            "Render this sketch as authentic henna applied on the top of a foot and ankle. Deep reddish-brown paste, raised wet lines, realistic skin, soft natural light, photographic realism."

        SurfaceProfile.FabricAbaya ->
            "Render this sketch as fine thread embroidery on a flowing abaya, metallic threads where appropriate, realistic fabric drape with subtle folds, soft studio lighting, photographic realism."

        SurfaceProfile.FabricThobe ->
            "Render this sketch as crisp embroidery on a traditional men's thobe collar, cuff, or chest panel. Realistic cotton weave, soft natural light, photographic detail."

        SurfaceProfile.FabricToub ->
            "Render this sketch as a printed/woven motif on a Sudanese toub, flowing translucent fabric with realistic drape and soft daylight, photographic realism."

        SurfaceProfile.WallStone ->
            "Render this sketch as carved relief in a warm sandstone wall panel, deep chiseled lines, natural shadow play, golden-hour light, architectural photography style."

        SurfaceProfile.WallPlaster ->
            "Render this sketch as hand-incised geometric pattern in white lime plaster on an interior wall, crisp shadows, soft daylight, architectural photography."

        SurfaceProfile.WallArch ->
            "Render this sketch as carved/painted ornament inside a pointed arch niche, traditional Islamic architecture context, warm interior light, photographic realism."

        SurfaceProfile.CeramicPlate ->
            "Render this sketch as hand-painted glaze on a round ceramic display plate, glossy fired finish, soft studio light, product photography."

        SurfaceProfile.CeramicTile ->
            "Render this sketch as hand-painted glazed ceramic tile, glossy finish, slight surface texture, soft natural light, product photography."

        SurfaceProfile.CeramicMug ->
            "Render this sketch as hand-painted glaze wrapping around a stoneware coffee mug, glossy fired finish, soft studio lighting, product photography."
    }

    /**
     * Universal quality tail appended to every render prompt. Triggers the
     * high-quality detail / lighting tokens that diffusion models respond to.
     */
    const val QUALITY_TAIL =
        "refined detail rendering, premium illustration finish, " +
            "soft cinematic lighting, gallery print quality, masterpiece composition"
}
