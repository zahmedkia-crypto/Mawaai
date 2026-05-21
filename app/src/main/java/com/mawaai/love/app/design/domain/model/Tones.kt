package com.mawaai.love.app.design.domain.model

enum class SkinTone(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val argb: Int
) {
    LIGHT("skin_light", "بشرة فاتحة", "Light", 0xFFF3D6BA.toInt()),
    MEDIUM_LIGHT("skin_medium_light", "بشرة متوسطة فاتحة", "Medium-Light", 0xFFE0B58A.toInt()),
    MEDIUM("skin_medium", "بشرة متوسطة", "Medium", 0xFFC08A5A.toInt()),
    MEDIUM_DARK("skin_medium_dark", "بشرة متوسطة داكنة", "Medium-Dark", 0xFF8B5A36.toInt()),
    DEEP("skin_deep", "بشرة داكنة", "Deep", 0xFF4E2A1A.toInt());

    companion object {
        fun fromId(id: String?): SkinTone? = entries.firstOrNull { it.id == id }
    }
}

enum class FabricTone(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val argb: Int
) {
    WHITE("fabric_white", "أبيض", "White", 0xFFFAF7F2.toInt()),
    BEIGE("fabric_beige", "بيج", "Beige", 0xFFD9C5A0.toInt()),
    GOLD("fabric_gold", "ذهبي", "Gold", 0xFFC8860A.toInt()),
    NAVY("fabric_navy", "كحلي", "Navy", 0xFF1B1B3A.toInt()),
    BLACK("fabric_black", "أسود", "Black", 0xFF111111.toInt()),
    BURGUNDY("fabric_burgundy", "خمري", "Burgundy", 0xFF7A1F2B.toInt());

    companion object {
        fun fromId(id: String?): FabricTone? = entries.firstOrNull { it.id == id }
    }
}
