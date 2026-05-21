package com.mawaai.love.app

import android.graphics.Color as AndroidColor
import android.os.Bundle
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.mawaai.love.app.core.lifecycle.ForegroundResumeTracker
import com.mawaai.love.app.core.responsive.LocalWindowSizeClass
import com.mawaai.love.app.core.theme.MawaaiTheme
import com.mawaai.love.app.core.theme.ThemeViewModel
import com.mawaai.love.app.core.theme.ThemedBackground
import com.mawaai.love.app.data.repository.ProfileRepository
import com.mawaai.love.app.ui.navigation.MawaaiNavGraph
import com.mawaai.love.app.ui.navigation.Screen
import com.mawaai.love.app.ui.privacy.BiometricHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject lateinit var profileRepository: ProfileRepository
    @Inject lateinit var foregroundResumeTracker: ForegroundResumeTracker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Force a fully-transparent status + navigation bar with LIGHT icons.
        // The XML activity theme inherits from `Theme.Material.Light.NoActionBar`,
        // so the default `auto` style would paint a light scrim under the status
        // bar and render dark icons — which on top of the dark Compose theme
        // shows as a visible light band at the very top of the screen and an
        // unreadable status clock. Forcing `SystemBarStyle.dark` aligns the
        // bars with the dark `MawaaiColorScheme` used inside Compose.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        )

        lifecycleScope.launch {
            val profile = profileRepository.getProfile().first()
            val helper = BiometricHelper(this@MainActivity)
            if (profile?.biometricEnabled == true && helper.canAuthenticate()) {
                helper.authenticate(
                    onSuccess = ::renderApp,
                    onError = { msg ->
                        Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                        finish()
                    },
                    onFailed = { /* allow user to retry on the prompt */ }
                )
            } else {
                renderApp()
            }
        }
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    private fun renderApp() {
        setContent {
            // Phase 24: compute the window size class once at the activity
            // root and provide it through `LocalWindowSizeClass`. Down-
            // stream screens that adapt their layout (HomeScreen,
            // CustomizeScreen, DesignMainScreen) read it from the
            // CompositionLocal instead of re-computing per recomposition.
            val windowSizeClass = calculateWindowSizeClass(this)
            MawaaiTheme {
                CompositionLocalProvider(LocalWindowSizeClass provides windowSizeClass) {
                    MawaaiAppContent(foregroundResumeTracker = foregroundResumeTracker)
                }
            }
        }
    }
}

@Composable
private fun MawaaiAppContent(
    foregroundResumeTracker: ForegroundResumeTracker,
    themeViewModel: ThemeViewModel = hiltViewModel()
) {
    val themeMode by themeViewModel.themeMode.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    ReplayIntroOnForegroundResume(navController, foregroundResumeTracker)
    ThemedBackground(mode = themeMode) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Transparent
        ) {
            MawaaiNavGraph(navController = navController)
        }
    }
}

/**
 * Observes the activity's resume event and — if the process was
 * backgrounded for at least
 * [ForegroundResumeTracker.REPLAY_INTRO_AFTER_MS] — pushes the user back
 * to the Intro video. The cold-start path already shows the intro via
 * `Screen.Splash`, so this hook only fires on warm resumes.
 */
@Composable
private fun ReplayIntroOnForegroundResume(
    navController: NavHostController,
    tracker: ForegroundResumeTracker
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                if (!tracker.consumeShouldReplayIntro()) return
                val current = navController.currentBackStackEntry?.destination?.route
                if (current == Screen.Intro.route || current == Screen.Splash.route) return
                navController.navigate(Screen.Intro.route) {
                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}
