package com.mawaai.love.app.design.rendering

import android.graphics.Bitmap
import android.graphics.Matrix
import com.mawaai.love.app.design.ai.intelligence.SurfaceProfile
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SurfaceWarpEngine @Inject constructor() {

    fun warp(source: Bitmap, profile: SurfaceProfile): Bitmap {
        return when (profile) {
            SurfaceProfile.CeramicMug -> cylindricalWarp(source)
            SurfaceProfile.CeramicPlate -> radialWarp(source)
            SurfaceProfile.FabricAbaya,
            SurfaceProfile.FabricThobe,
            SurfaceProfile.FabricToub -> fabricWarp(source)
            else -> source
        }
    }

    private fun cylindricalWarp(bitmap: Bitmap): Bitmap {
        val m = Matrix()
        m.preScale(0.92f, 1f)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, m, true)
    }

    private fun radialWarp(bitmap: Bitmap): Bitmap {
        val m = Matrix()
        m.postScale(0.96f, 0.96f)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, m, true)
    }

    private fun fabricWarp(bitmap: Bitmap): Bitmap {
        val m = Matrix()
        m.postSkew(-0.03f, 0f)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, m, true)
    }
}
