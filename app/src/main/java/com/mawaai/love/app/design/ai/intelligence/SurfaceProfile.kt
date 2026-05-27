package com.mawaai.love.app.design.ai.intelligence

/**
 * Strongly-typed catalog of every surface MAWAAI can render onto.
 * Ported from Lovable Creative Studio template-intelligence.ts.
 *
 * Each variant is a data object (zero state) so they can be used as map keys
 * and switch arms with exhaustive checks.
 */
sealed interface SurfaceProfile {
    val id: String
    val label: String
    val targetSurface: String
    val constraints: List<String>
    val maskingRules: List<String>
    val perspectiveRules: List<String>
    val materialResponse: String

    // ───── Henna ─────

    data object SkinPalm : SurfaceProfile {
        override val id = "skin_palm"
        override val label = "open palm skin"
        override val targetSurface = "center palm with natural finger-edge falloff"
        override val constraints = listOf(
            "keep dense motifs inside the palm center",
            "avoid crossing hard palm creases with tiny detail",
            "leave breathable negative space near the thumb pad"
        )
        override val maskingRules = listOf(
            "clip artwork to visible skin only",
            "fade line weight near palm edges",
            "do not place motifs outside the hand silhouette"
        )
        override val perspectiveRules = listOf(
            "use a near-flat frontal projection",
            "slightly follow palm curvature and crease direction"
        )
        override val materialResponse =
            "raised reddish-brown henna paste with soft skin absorption and low reflectance"
    }

    data object SkinHandFull : SurfaceProfile {
        override val id = "skin_hand_full"
        override val label = "back of hand and wrist skin"
        override val targetSurface = "hand back, fingers, wrist, and lower forearm zones"
        override val constraints = listOf(
            "align long vines with finger direction",
            "reserve wrist motifs for banded compositions",
            "scale detail down on fingers"
        )
        override val maskingRules = listOf(
            "mask around fingers and wrist contour",
            "wrap strokes over knuckles without breaking structure",
            "preserve motif order across hand zones"
        )
        override val perspectiveRules = listOf(
            "follow hand taper from wrist to fingers",
            "compress artwork slightly across knuckle planes"
        )
        override val materialResponse =
            "fresh henna paste with raised edges, warm skin shadowing, and natural daylight"
    }

    data object SkinFoot : SurfaceProfile {
        override val id = "skin_foot"
        override val label = "top foot and ankle skin"
        override val targetSurface = "top of foot with optional ankle band"
        override val constraints = listOf(
            "keep primary motif on the top-foot plane",
            "use banded detail around the ankle",
            "avoid dense detail over toe gaps"
        )
        override val maskingRules = listOf(
            "clip to foot silhouette",
            "wrap ankle details along the ankle curve",
            "preserve line continuity over the instep"
        )
        override val perspectiveRules = listOf(
            "use shallow perspective along the foot length",
            "slightly narrow artwork toward toes"
        )
        override val materialResponse = "low-sheen henna on skin with realistic instep highlights"
    }

    // ───── Garments ─────

    data object FabricAbaya : SurfaceProfile {
        override val id = "fabric_abaya"
        override val label = "flowing abaya fabric"
        override val targetSurface = "front panels, cuffs, and hem embroidery zones"
        override val constraints = listOf(
            "keep large motifs away from heavy fold valleys",
            "respect seam and hem borders",
            "use embroidery density below coverage limit"
        )
        override val maskingRules = listOf(
            "warp design along fabric folds",
            "hide strokes in fold occlusion",
            "clip to garment panels and seams"
        )
        override val perspectiveRules = listOf(
            "follow vertical drape",
            "compress motif width where fabric turns away"
        )
        override val materialResponse =
            "thread embroidery with subtle sheen over dark crepe-like fabric"
    }

    data object FabricThobe : SurfaceProfile {
        override val id = "fabric_thobe"
        override val label = "crisp thobe cotton"
        override val targetSurface = "collar, placket, cuff, or chest panel"
        override val constraints = listOf(
            "use restrained coverage",
            "keep motifs symmetrical around placket seams",
            "avoid oversized medallions on narrow trim"
        )
        override val maskingRules = listOf(
            "clip to stitched panels",
            "follow collar and cuff boundaries",
            "keep embroidery aligned to garment construction"
        )
        override val perspectiveRules = listOf(
            "use crisp flat projection on cotton panels",
            "curve around collar/cuff cylinders"
        )
        override val materialResponse =
            "clean embroidery on white cotton with bright soft highlights"
    }

    data object FabricToub : SurfaceProfile {
        override val id = "fabric_toub"
        override val label = "Sudanese toub fabric"
        override val targetSurface = "flowing translucent fabric field and border zones"
        override val constraints = listOf(
            "preserve airy spacing",
            "let repeated motifs breathe across drape",
            "keep border motifs aligned with fabric edge"
        )
        override val maskingRules = listOf(
            "warp across translucent folds",
            "let fabric highlights pass over the artwork",
            "clip to visible cloth silhouette"
        )
        override val perspectiveRules = listOf(
            "follow broad flowing fabric direction",
            "soften far-side motif contrast"
        )
        override val materialResponse =
            "printed or woven motif softened by light cotton translucency"
    }

    // ───── Walls ─────

    data object WallStone : SurfaceProfile {
        override val id = "wall_stone"
        override val label = "carved stone wall"
        override val targetSurface = "flat stone mural panel"
        override val constraints = listOf(
            "favor bold cuts over hairline detail",
            "maintain carving depth consistency",
            "preserve masonry margin"
        )
        override val maskingRules = listOf(
            "engrave into the stone plane",
            "use shadows inside carved grooves",
            "do not spill past panel edges"
        )
        override val perspectiveRules = listOf(
            "use architectural frontal perspective",
            "keep geometry square to the panel"
        )
        override val materialResponse =
            "warm carved stone relief with rough grain and directional shadow"
    }

    data object WallPlaster : SurfaceProfile {
        override val id = "wall_plaster"
        override val label = "lime plaster wall"
        override val targetSurface = "smooth plaster panel"
        override val constraints = listOf(
            "keep incised detail readable",
            "avoid overfilling the plaster field",
            "respect panel margins"
        )
        override val maskingRules = listOf(
            "incise or paint into plaster texture",
            "preserve wall plane flatness",
            "clip to panel boundary"
        )
        override val perspectiveRules = listOf(
            "use flat architectural projection",
            "keep repeated geometry aligned to wall axes"
        )
        override val materialResponse = "matte plaster with shallow grooves and diffuse light"
    }

    data object WallArch : SurfaceProfile {
        override val id = "wall_arch"
        override val label = "arched plaster niche"
        override val targetSurface = "arch interior and border spandrel zones"
        override val constraints = listOf(
            "center primary motif in the arch",
            "use border detail along the arch curve",
            "avoid cluttering the spring line"
        )
        override val maskingRules = listOf(
            "mask artwork inside pointed arch silhouette",
            "bend border motifs along arch curve",
            "respect niche depth shadows"
        )
        override val perspectiveRules = listOf(
            "follow the arch curvature",
            "slightly darken recessed surfaces"
        )
        override val materialResponse =
            "painted or carved plaster ornament with warm interior light"
    }

    // ───── Ceramics ─────

    data object CeramicPlate : SurfaceProfile {
        override val id = "ceramic_plate"
        override val label = "glazed ceramic plate"
        override val targetSurface = "round plate center and rim"
        override val constraints = listOf(
            "center radial motifs",
            "reserve border work for the rim",
            "avoid edge distortion of central elements"
        )
        override val maskingRules = listOf(
            "clip to circular plate surface",
            "warp rim motifs around curvature",
            "let glaze highlights sit above paint"
        )
        override val perspectiveRules = listOf(
            "use radial projection",
            "compress artwork toward the curved rim"
        )
        override val materialResponse = "hand-painted glaze beneath glossy ceramic reflections"
    }

    data object CeramicTile : SurfaceProfile {
        override val id = "ceramic_tile"
        override val label = "square glazed tile"
        override val targetSurface = "flat square tile face"
        override val constraints = listOf(
            "keep pattern tileable where possible",
            "maintain clean edge margins",
            "avoid excessive micro-detail under glaze"
        )
        override val maskingRules = listOf(
            "clip to square tile face",
            "keep corners aligned",
            "let glaze highlights overlay the design"
        )
        override val perspectiveRules = listOf(
            "use flat product projection",
            "keep geometry square unless tile is angled"
        )
        override val materialResponse = "painted ceramic pigment under glossy fired glaze"
    }

    data object CeramicMug : SurfaceProfile {
        override val id = "ceramic_mug"
        override val label = "curved ceramic mug"
        override val targetSurface = "visible curved mug wall"
        override val constraints = listOf(
            "keep important motifs away from the handle occlusion",
            "avoid placing text-like detail at side curvature",
            "use wrap-safe spacing"
        )
        override val maskingRules = listOf(
            "wrap artwork around the mug cylinder",
            "hide design behind the visible side edge",
            "preserve glaze reflections above artwork"
        )
        override val perspectiveRules = listOf(
            "cylindrically warp the design",
            "compress motif width near left and right edges"
        )
        override val materialResponse =
            "glossy glaze with curved specular highlights and ceramic thickness"
    }
}
