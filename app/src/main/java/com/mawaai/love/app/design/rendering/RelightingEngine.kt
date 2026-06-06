package com.mawaai.love.app.design.rendering

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.Shader
import com.mawaai.love.app.design.ai.intelligence.SurfaceProfile
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RelightingEngine @Inject constructor() {

    fun relight(source: Bitmap, profile: SurfaceProfile): Bitmap {
        val out = source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(out)
        paintAmbientOcclusion(canvas, out.width, out.height, profile)
        paintSurfaceHighlights(canvas, out.width, out.height, profile)
        paintMaterialDepth(canvas, out.width, out.height, profile)
        return out
    }

    private fun paintAmbientOcclusion(canvas: Canvas, width: Int, height: Int, profile: SurfaceProfile) {
        val alpha = when (profile) {
            SurfaceProfile.SkinPalm,
            SurfaceProfile.SkinHandFull,
            SurfaceProfile.SkinFoot -> 24
            SurfaceProfile.FabricAbaya,
            SurfaceProfile.FabricThobe,
            SurfaceProfile.FabricToub -> 34
            SurfaceProfile.WallStone,
            SurfaceProfile.WallPlaster,
            SurfaceProfile.WallArch -> 42
            SurfaceProfile.CeramicPlate,
            SurfaceProfile.CeramicTile,
            SurfaceProfile.CeramicMug -> 28
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                width / 2f,
                height / 2f,
                maxOf(width, height) * 0.58f,
                intArrayOf(Color.TRANSPARENT, Color.argb(alpha, 0, 0, 0)),
                floatArrayOf(0.62f, 1f),
                Shader.TileMode.CLAMP
            )
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    }

    private fun paintSurfaceHighlights(canvas: Canvas, width: Int, height: Int, profile: SurfaceProfile) {
        val alpha = when (profile) {
            SurfaceProfile.CeramicPlate,
            SurfaceProfile.CeramicTile,
            SurfaceProfile.CeramicMug -> 70
            SurfaceProfile.FabricThobe -> 34
            SurfaceProfile.FabricAbaya,
            SurfaceProfile.FabricToub -> 24
            SurfaceProfile.SkinPalm,
            SurfaceProfile.SkinHandFull,
            SurfaceProfile.SkinFoot -> 28
            SurfaceProfile.WallStone,
            SurfaceProfile.WallPlaster,
            SurfaceProfile.WallArch -> 18
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f,
                0f,
                width.toFloat(),
                height * 0.55f,
                Color.argb(alpha, 255, 255, 255),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    }

    private fun paintMaterialDepth(canvas: Canvas, width: Int, height: Int, profile: SurfaceProfile) {
        val alpha = when (profile) {
            SurfaceProfile.FabricAbaya,
            SurfaceProfile.FabricThobe,
            SurfaceProfile.FabricToub -> 18
            SurfaceProfile.WallStone,
            SurfaceProfile.WallPlaster,
            SurfaceProfile.WallArch -> 26
            SurfaceProfile.SkinPalm,
            SurfaceProfile.SkinHandFull,
            SurfaceProfile.SkinFoot -> 12
            SurfaceProfile.CeramicPlate,
            SurfaceProfile.CeramicTile,
            SurfaceProfile.CeramicMug -> 10
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(alpha, 0, 0, 0)
            xfermode = PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
        }
        val stripeHeight = maxOf(2, height / 90)
        var y = 0
        while (y < height) {
            canvas.drawRect(Rect(0, y, width, y + stripeHeight), paint)
            y += stripeHeight * 3
        }
    }
}
