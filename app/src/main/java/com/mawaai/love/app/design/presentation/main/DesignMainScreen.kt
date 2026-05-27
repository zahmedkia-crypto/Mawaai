package com.mawaai.love.app.design.presentation.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mawaai.love.app.R
import com.mawaai.love.app.design.presentation.common.DesignSurface
import com.mawaai.love.app.design.presentation.common.DesignTopBar
import com.mawaai.love.app.design.canvas.ui.DesignCanvasScreen
import com.mawaai.love.app.design.presentation.canvas.CanvasRecommendationsScreen
import com.mawaai.love.app.design.presentation.flow.CreativeIntelligenceScreen
import com.mawaai.love.app.design.presentation.flow.CustomizeScreen
import com.mawaai.love.app.design.presentation.flow.InputMethodScreen
import com.mawaai.love.app.design.showcase.ui.ShowcaseScreen
import com.mawaai.love.app.design.presentation.flow.PreviewScreen
import com.mawaai.love.app.design.presentation.flow.ProcessingScreen
import com.mawaai.love.app.design.presentation.flow.ResultScreen
import com.mawaai.love.app.design.presentation.flow.StyleSelectionScreen
import com.mawaai.love.app.design.presentation.flow.SuggestionsScreen
import com.mawaai.love.app.design.presentation.flow.TemplateGalleryScreen
import com.mawaai.love.app.design.presentation.tab1.SpecializedHomeScreen
import com.mawaai.love.app.design.presentation.tab2.ConverterHomeScreen
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun DesignMainScreen(onExit: () -> Unit) {
    val innerNav = rememberNavController()
    val backStack by innerNav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    val isTabRoot = currentRoute == DesignRoute.SpecializedHome.route ||
        currentRoute == DesignRoute.ConverterHome.route ||
        currentRoute == null

    val currentTab = when (currentRoute) {
        DesignRoute.ConverterHome.route -> DesignTab.CONVERTER
        else -> DesignTab.SPECIALIZED
    }

    val title = when (currentRoute) {
        DesignRoute.ConverterHome.route -> stringResource(R.string.design_tab_converter)
        else -> stringResource(R.string.design_tab_specialized)
    }

    DesignSurface {
        Scaffold(
            topBar = {
                DesignTopBar(
                    title = title,
                    onBack = {
                        if (isTabRoot) onExit() else innerNav.popBackStack()
                    }
                )
            },
            bottomBar = {
                if (isTabRoot) {
                    DesignBottomBar(
                        current = currentTab,
                        onSelect = { tab ->
                            val target = when (tab) {
                                DesignTab.SPECIALIZED -> DesignRoute.SpecializedHome.route
                                DesignTab.CONVERTER -> DesignRoute.ConverterHome.route
                            }
                            if (currentRoute != target) {
                                innerNav.navigate(target) {
                                    popUpTo(DesignRoute.SpecializedHome.route) { inclusive = false }
                                    launchSingleTop = true
                                }
                            }
                        }
                    )
                }
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Box(Modifier.fillMaxSize()) {
                    NavHost(
                        navController = innerNav,
                        startDestination = DesignRoute.SpecializedHome.route
                    ) {
                        composable(DesignRoute.SpecializedHome.route) {
                            SpecializedHomeScreen(nav = innerNav)
                        }
                        composable(DesignRoute.ConverterHome.route) {
                            ConverterHomeScreen(nav = innerNav)
                        }

                        composable(
                            route = DesignRoute.InputMethod.route,
                            arguments = listOf(
                                navArgument("categoryId") { type = NavType.StringType },
                                navArgument("subTypeId") { type = NavType.StringType }
                            )
                        ) { entry ->
                            val categoryId = entry.arguments?.getString("categoryId") ?: ""
                            val subTypeId = entry.arguments?.getString("subTypeId") ?: ""
                            InputMethodScreen(
                                nav = innerNav,
                                categoryId = categoryId,
                                subTypeId = subTypeId,
                                isConverterFlow = false
                            )
                        }

                        composable(DesignRoute.ConverterInput.route) {
                            InputMethodScreen(
                                nav = innerNav,
                                categoryId = null,
                                subTypeId = null,
                                isConverterFlow = true
                            )
                        }

                        composable(
                            route = DesignRoute.Canvas.route,
                            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
                        ) { entry ->
                            DesignCanvasScreen(
                                nav = innerNav,
                                sessionId = entry.arguments?.getString("sessionId") ?: ""
                            )
                        }
                        composable(
                            route = DesignRoute.Preview.route,
                            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
                        ) { entry ->
                            PreviewScreen(
                                nav = innerNav,
                                sessionId = entry.arguments?.getString("sessionId") ?: ""
                            )
                        }
                        composable(
                            route = DesignRoute.Suggestions.route,
                            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
                        ) { entry ->
                            SuggestionsScreen(
                                nav = innerNav,
                                sessionId = entry.arguments?.getString("sessionId") ?: ""
                            )
                        }
                        composable(
                            route = DesignRoute.Intelligence.route,
                            arguments = listOf(navArgument("projectId") { type = NavType.StringType })
                        ) { entry ->
                            CreativeIntelligenceScreen(
                                nav = innerNav,
                                projectId = entry.arguments?.getString("projectId") ?: ""
                            )
                        }
                        composable(
                            route = DesignRoute.StyleSelect.route,
                            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
                        ) { entry ->
                            StyleSelectionScreen(
                                nav = innerNav,
                                sessionId = entry.arguments?.getString("sessionId") ?: ""
                            )
                        }
                        composable(
                            route = DesignRoute.Processing.route,
                            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
                        ) { entry ->
                            ProcessingScreen(
                                nav = innerNav,
                                sessionId = entry.arguments?.getString("sessionId") ?: ""
                            )
                        }
                        composable(
                            route = DesignRoute.TemplateGallery.route,
                            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
                        ) { entry ->
                            TemplateGalleryScreen(
                                nav = innerNav,
                                sessionId = entry.arguments?.getString("sessionId") ?: ""
                            )
                        }
                        composable(
                            route = DesignRoute.Customize.route,
                            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
                        ) { entry ->
                            CustomizeScreen(
                                nav = innerNav,
                                sessionId = entry.arguments?.getString("sessionId") ?: ""
                            )
                        }
                        composable(
                            route = DesignRoute.Result.route,
                            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
                        ) { entry ->
                            ResultScreen(
                                nav = innerNav,
                                sessionId = entry.arguments?.getString("sessionId") ?: ""
                            )
                        }
                        composable(
                            route = DesignRoute.Showcase.route,
                            arguments = listOf(navArgument("artworkId") { type = NavType.LongType })
                        ) { entry ->
                            ShowcaseScreen(
                                nav = innerNav,
                                artworkId = entry.arguments?.getLong("artworkId") ?: -1L
                            )
                        }
                        composable(
                            route = DesignRoute.Recommendations.route,
                            arguments = listOf(navArgument("artworkId") { type = NavType.LongType })
                        ) { entry ->
                            CanvasRecommendationsScreen(
                                nav = innerNav,
                                artworkId = entry.arguments?.getLong("artworkId") ?: -1L
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1209)
@Composable
private fun DesignMainScreenPreview() {
    DesignMainScreen(onExit = {})
}
