package com.mawaai.love.app.design.canvas.model

import android.graphics.Bitmap

data class Layer(
    val id: Int,
    val name: String,
    val bitmap: Bitmap,
    val visible: Boolean = true,
    val opacity: Float = 1f,
    val blend: BlendMode = BlendMode.NORMAL,
    val locked: Boolean = false
)
