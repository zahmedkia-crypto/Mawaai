package com.mawaai.love.app.design.presentation.main

sealed class DesignRoute(val route: String) {
    object SpecializedHome : DesignRoute("design/tab1/home")
    object ConverterHome   : DesignRoute("design/tab2/home")

    object InputMethod : DesignRoute("design/flow/input/{categoryId}/{subTypeId}") {
        fun create(categoryId: String, subTypeId: String) =
            "design/flow/input/$categoryId/$subTypeId"
    }
    object ConverterInput : DesignRoute("design/flow/converter_input")

    object Canvas : DesignRoute("design/flow/canvas/{sessionId}") {
        fun create(sessionId: String) = "design/flow/canvas/$sessionId"
    }
    object Preview : DesignRoute("design/flow/preview/{sessionId}") {
        fun create(sessionId: String) = "design/flow/preview/$sessionId"
    }
    object Suggestions : DesignRoute("design/flow/suggestions/{sessionId}") {
        fun create(sessionId: String) = "design/flow/suggestions/$sessionId"
    }
    object Intelligence : DesignRoute("design/flow/intelligence/{projectId}") {
        fun create(projectId: String) = "design/flow/intelligence/$projectId"
    }
    object StyleSelect : DesignRoute("design/flow/style/{sessionId}") {
        fun create(sessionId: String) = "design/flow/style/$sessionId"
    }
    object Processing : DesignRoute("design/flow/processing/{sessionId}") {
        fun create(sessionId: String) = "design/flow/processing/$sessionId"
    }
    object TemplateGallery : DesignRoute("design/flow/templates/{sessionId}") {
        fun create(sessionId: String) = "design/flow/templates/$sessionId"
    }
    object Customize : DesignRoute("design/flow/customize/{sessionId}") {
        fun create(sessionId: String) = "design/flow/customize/$sessionId"
    }
    object Result : DesignRoute("design/flow/result/{sessionId}") {
        fun create(sessionId: String) = "design/flow/result/$sessionId"
    }
    object Showcase : DesignRoute("design/showcase/{artworkId}") {
        fun create(artworkId: Long) = "design/showcase/$artworkId"
    }
    object Recommendations : DesignRoute("design/recommendations/{artworkId}") {
        fun create(artworkId: Long) = "design/recommendations/$artworkId"
    }
}
