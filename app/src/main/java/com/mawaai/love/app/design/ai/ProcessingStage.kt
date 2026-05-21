package com.mawaai.love.app.design.ai

sealed class ProcessingStage {
    object Init : ProcessingStage()
    object Segmenting : ProcessingStage()
    object EdgeDetecting : ProcessingStage()
    object Stylizing : ProcessingStage()
    object Upscaling : ProcessingStage()
    object Done : ProcessingStage()
    data class Failed(val cause: Throwable) : ProcessingStage()
}

class ModelMissingException(modelName: String) :
    RuntimeException("AI model '$modelName' is not available on this device")
