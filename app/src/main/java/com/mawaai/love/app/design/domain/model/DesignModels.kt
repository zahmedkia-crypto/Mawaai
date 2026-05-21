package com.mawaai.love.app.design.domain.model

import com.google.gson.annotations.SerializedName

data class DesignCategory(
    @SerializedName("id") val id: String,
    @SerializedName("nameAr") val nameAr: String,
    @SerializedName("nameEn") val nameEn: String,
    @SerializedName("descriptionAr") val descriptionAr: String,
    @SerializedName("iconKey") val iconKey: String,
    @SerializedName("accentColor") val accentColor: String,
    @SerializedName("subTypes") val subTypes: List<DesignSubType>,
    @SerializedName("styles") val styles: List<DesignStyle> = emptyList()
)

data class DesignSubType(
    @SerializedName("id") val id: String,
    @SerializedName("nameAr") val nameAr: String,
    @SerializedName("nameEn") val nameEn: String
)

data class DesignStyle(
    @SerializedName("id") val id: String,
    @SerializedName("nameAr") val nameAr: String,
    @SerializedName("nameEn") val nameEn: String
)

data class ConversionStyle(
    @SerializedName("id") val id: String,
    @SerializedName("nameAr") val nameAr: String,
    @SerializedName("nameEn") val nameEn: String,
    @SerializedName("descriptionAr") val descriptionAr: String
)

data class ColorTheme(
    @SerializedName("id") val id: String,
    @SerializedName("nameAr") val nameAr: String,
    @SerializedName("nameEn") val nameEn: String
)

data class DesignCatalog(
    @SerializedName("categories") val categories: List<DesignCategory>,
    @SerializedName("conversionStyles") val conversionStyles: List<ConversionStyle>,
    @SerializedName("colorThemes") val colorThemes: List<ColorTheme>
)

enum class InputMethod { DRAW, UPLOAD, CAMERA }
