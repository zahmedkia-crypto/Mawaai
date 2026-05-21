package com.mawaai.love.app.design.domain.model

import android.net.Uri

data class DesignSession(
    val id: String,
    val categoryId: String? = null,
    val subTypeId: String? = null,
    val inputMethod: InputMethod? = null,
    val inputImageUri: Uri? = null,
    val styleId: String? = null,
    val colorThemeId: String? = null,
    val skinToneId: String? = null,
    val fabricToneId: String? = null,
    val processedImageUri: Uri? = null,
    val selectedTemplateId: String? = null,
    val isConverterFlow: Boolean = false
)
