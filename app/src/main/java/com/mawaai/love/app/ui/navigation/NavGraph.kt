package com.mawaai.love.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.mawaai.love.app.ui.intro.IntroScreen
import com.mawaai.love.app.ui.onboarding.OnboardingScreen
import com.mawaai.love.app.ui.splash.SplashScreen
import com.mawaai.love.app.ui.home.HomeScreen
import com.mawaai.love.app.ui.memories.MemoriesScreen
import com.mawaai.love.app.ui.memories.AddMemoryScreen
import com.mawaai.love.app.ui.memories.MemoryDetailScreen
import com.mawaai.love.app.ui.letters.LettersScreen
import com.mawaai.love.app.ui.letters.ComposeLetterScreen
import com.mawaai.love.app.ui.letters.LetterDetailScreen
import com.mawaai.love.app.ui.settings.AiProviderSettingsScreen
import com.mawaai.love.app.ui.settings.AiProviderSettingsViewModel
import com.mawaai.love.app.ui.settings.SettingsScreen
import com.mawaai.love.app.ui.mood.MoodScreen
import com.mawaai.love.app.design.presentation.main.DesignMainScreen
import androidx.navigation.NavType
import androidx.navigation.navArgument

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Intro : Screen("intro")
    object Onboarding : Screen("onboarding")
    object Home : Screen("home")
    object Memories : Screen("memories")
    object AddMemory : Screen("add_memory")
    object MemoryDetail : Screen("memory_detail/{memoryId}") {
        fun createRoute(memoryId: Long) = "memory_detail/$memoryId"
    }
    object Letters : Screen("letters")
    object ComposeLetter : Screen("compose_letter")
    object LetterDetail : Screen("letter_detail/{letterId}") {
        fun createRoute(letterId: Long) = "letter_detail/$letterId"
    }
    object Mood : Screen("mood")
    object Settings : Screen("settings")
    object AiSettings : Screen("ai_settings")
    object Design : Screen("design")
}

@Composable
fun MawaaiNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToIntro = {
                    navController.navigate(Screen.Intro.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Intro.route) {
            IntroScreen(
                onFinish = { isFirstLaunch ->
                    val next = if (isFirstLaunch) Screen.Onboarding.route else Screen.Home.route
                    navController.navigate(next) {
                        popUpTo(Screen.Intro.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onFinish = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToMemories = { navController.navigate(Screen.Memories.route) },
                onNavigateToLetters = { navController.navigate(Screen.Letters.route) },
                onNavigateToMood = { navController.navigate(Screen.Mood.route) },
                onNavigateToDesign = { navController.navigate(Screen.Design.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(Screen.Memories.route) {
            MemoriesScreen(
                onMemoryClick = { id -> navController.navigate(Screen.MemoryDetail.createRoute(id)) },
                onAddMemory = { navController.navigate(Screen.AddMemory.route) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.AddMemory.route) {
            AddMemoryScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.MemoryDetail.route,
            arguments = listOf(navArgument("memoryId") { type = NavType.LongType })
        ) { backStackEntry ->
            val memoryId = backStackEntry.arguments?.getLong("memoryId") ?: 0L
            MemoryDetailScreen(
                memoryId = memoryId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Letters.route) {
            LettersScreen(
                onLetterClick = { id -> navController.navigate(Screen.LetterDetail.createRoute(id)) },
                onComposeLetter = { navController.navigate(Screen.ComposeLetter.route) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.ComposeLetter.route) {
            ComposeLetterScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.LetterDetail.route,
            arguments = listOf(navArgument("letterId") { type = NavType.LongType })
        ) { backStackEntry ->
            val letterId = backStackEntry.arguments?.getLong("letterId") ?: 0L
            LetterDetailScreen(
                letterId = letterId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Mood.route) {
            MoodScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToAiProviders = { navController.navigate(Screen.AiSettings.route) }
            )
        }

        composable(Screen.AiSettings.route) {
            val aiViewModel: AiProviderSettingsViewModel = androidx.hilt.navigation.compose.hiltViewModel()
            AiProviderSettingsScreen(viewModel = aiViewModel)
        }

        composable(Screen.Design.route) {
            DesignMainScreen(
                onExit = { navController.popBackStack() }
            )
        }
    }
}
