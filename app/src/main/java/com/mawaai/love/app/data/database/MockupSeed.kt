package com.mawaai.love.app.data.database

import com.mawaai.love.app.data.database.entities.ProductMockupEntity

object MockupSeed {
    val ALL: List<ProductMockupEntity> = listOf(
        // ─── Henna ───
        ProductMockupEntity(
            id = "mockup-henna-bridal-palm",
            name = "Bridal palm",
            category = "HENNA",
            surfaceMatchCsv = "skin_palm,skin_hand_full",
            scene = "A bride's open palm and fingers rested on a silk cushion, gold bangles at the wrist, soft bokeh of marigold petals behind",
            lighting = "warm golden-hour window light",
            perspective = "overhead 3/4",
            accentColor = "#b86b3a",
            sortOrder = 10
        ),
        ProductMockupEntity(
            id = "mockup-henna-foot-rug",
            name = "Foot pose on rug",
            category = "HENNA",
            surfaceMatchCsv = "skin_foot",
            scene = "A bare foot with anklet resting on a deep red Persian rug, traditional setting",
            lighting = "soft morning daylight",
            perspective = "side eye-level",
            accentColor = "#7a2b1f",
            sortOrder = 20
        ),
        ProductMockupEntity(
            id = "mockup-henna-closeup",
            name = "Henna close-up",
            category = "HENNA",
            surfaceMatchCsv = "skin_palm,skin_hand_full,skin_foot",
            scene = "Tight macro of freshly applied henna with subtle paste sheen on skin",
            lighting = "soft diffused studio light",
            perspective = "macro overhead",
            accentColor = "#6b3a2a",
            sortOrder = 30
        ),
        // ─── Garments ───
        ProductMockupEntity(
            id = "mockup-garment-abaya-flatlay",
            name = "Flat-lay abaya",
            category = "GARMENT",
            surfaceMatchCsv = "fabric_abaya",
            scene = "A flowing black abaya laid flat on a marble surface with gold thread shimmering, perfume bottle and pearls nearby",
            lighting = "soft north-window light",
            perspective = "overhead",
            accentColor = "#d4af37",
            sortOrder = 10
        ),
        ProductMockupEntity(
            id = "mockup-garment-thobe-hanger",
            name = "Thobe on hanger",
            category = "GARMENT",
            surfaceMatchCsv = "fabric_thobe",
            scene = "A crisp white thobe on a wooden hanger against a warm sandstone wall",
            lighting = "warm late-afternoon light",
            perspective = "front eye-level",
            accentColor = "#c9a87a",
            sortOrder = 20
        ),
        ProductMockupEntity(
            id = "mockup-garment-toub-shoulder",
            name = "Toub on model shoulder",
            category = "GARMENT",
            surfaceMatchCsv = "fabric_toub",
            scene = "A translucent Sudanese toub draped over a model's shoulder, soft folds catching light",
            lighting = "soft window light",
            perspective = "3/4 portrait",
            accentColor = "#b87a4a",
            sortOrder = 30
        ),
        // ─── Walls ───
        ProductMockupEntity(
            id = "mockup-wall-majlis",
            name = "Majlis wall",
            category = "WALL",
            surfaceMatchCsv = "wall_plaster,wall_stone",
            scene = "An interior majlis with low cushions, brass lantern, and a feature wall ready for ornament",
            lighting = "warm interior lantern + daylight mix",
            perspective = "wide eye-level",
            accentColor = "#a87a3a",
            sortOrder = 10
        ),
        ProductMockupEntity(
            id = "mockup-wall-arch-niche",
            name = "Carved arch niche",
            category = "WALL",
            surfaceMatchCsv = "wall_arch,wall_stone",
            scene = "A pointed arch niche in a sandstone wall with soft side shadow",
            lighting = "golden-hour raking light",
            perspective = "straight-on eye-level",
            accentColor = "#c08a4a",
            sortOrder = 20
        ),
        ProductMockupEntity(
            id = "mockup-wall-gallery",
            name = "Gallery plaster wall",
            category = "WALL",
            surfaceMatchCsv = "wall_plaster",
            scene = "A minimalist gallery wall in white lime plaster with subtle texture",
            lighting = "soft skylight",
            perspective = "eye-level",
            accentColor = "#d8c7a8",
            sortOrder = 30
        ),
        // ─── Ceramics ───
        ProductMockupEntity(
            id = "mockup-ceramic-mug",
            name = "Stoneware mug on linen",
            category = "CERAMIC",
            surfaceMatchCsv = "ceramic_mug",
            scene = "A stoneware mug on cream linen next to a sprig of dried thyme",
            lighting = "soft diffused morning light",
            perspective = "3/4 product",
            accentColor = "#3a5a4a",
            sortOrder = 10
        ),
        ProductMockupEntity(
            id = "mockup-ceramic-tile",
            name = "Tile in grid",
            category = "CERAMIC",
            surfaceMatchCsv = "ceramic_tile",
            scene = "A single hand-painted tile centered in a quiet grid of plain tiles on a wall",
            lighting = "soft side light",
            perspective = "straight-on",
            accentColor = "#2d5f7a",
            sortOrder = 20
        ),
        ProductMockupEntity(
            id = "mockup-ceramic-plate",
            name = "Display plate on table",
            category = "CERAMIC",
            surfaceMatchCsv = "ceramic_plate",
            scene = "A round display plate on a dark walnut table with brass accents",
            lighting = "warm spot + soft fill",
            perspective = "overhead",
            accentColor = "#8a4a2d",
            sortOrder = 30
        ),
    )
}
