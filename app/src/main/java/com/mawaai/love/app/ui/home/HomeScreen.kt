package com.mawaai.love.app.ui.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mawaai.love.app.R
import com.mawaai.love.app.core.components.ParticleHeartSystem
import com.mawaai.love.app.core.theme.MawaaiColors
import com.mawaai.love.app.ui.home.components.*
import com.mawaai.love.app.ui.scene.AiSceneDialog
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun HomeScreen(
    onNavigateToMemories: () -> Unit,
    onNavigateToLetters: () -> Unit,
    onNavigateToMood: () -> Unit,
    onNavigateToDesign: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val recentMemory by viewModel.recentMemory.collectAsStateWithLifecycle()
    val todayMood by viewModel.todayMood.collectAsStateWithLifecycle()
    val todayHijri by viewModel.todayHijri.collectAsStateWithLifecycle()
    val internationalQuote by viewModel.internationalQuote.collectAsStateWithLifecycle()

    var showAiScene by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current
    val shareSubject = stringResource(R.string.share_quote_subject)
    val shareChooser = stringResource(R.string.share_quote_chooser)
    val notificationsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* fire-and-forget; user can re-toggle in OS settings */ }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationsPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    Scaffold(
        topBar = {
            // Tight inline top row — keeps the settings icon RIGHT below the
            // notch instead of centered in a 64-dp Material TopAppBar. The
            // status-bar inset pushes the row past the system clock; the
            // 4-dp vertical padding adds just enough breathing room without
            // creating a visible chrome band. RTL: `Arrangement.End` places
            // the icon on the left edge of the screen (matching the original
            // `CenterAlignedTopAppBar` actions slot behaviour).
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateToSettings) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MawaaiColors.RoseGold
                    )
                }
            }
        },
        bottomBar = {
            MawaaiBottomNavBar(
                currentRoute = "home",
                onNavigate = { route ->
                    when (route) {
                        "memories" -> onNavigateToMemories()
                        "letters" -> onNavigateToLetters()
                        "mood" -> onNavigateToMood()
                    }
                }
            )
        },
        containerColor = androidx.compose.ui.graphics.Color.Transparent
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Background particles
            ParticleHeartSystem(particleCount = 8)

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item {
                    WelcomeCard(
                        greeting = viewModel.greeting,
                        partnerName = profile?.partnerName ?: "رزان",
                        hijri = todayHijri
                    )
                }

                item {
                    DailyQuoteCard(
                        quote = viewModel.dailyQuote,
                        onShare = { quote ->
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, shareSubject)
                                putExtra(Intent.EXTRA_TEXT, quote)
                            }
                            context.startActivity(Intent.createChooser(intent, shareChooser))
                        }
                    )
                }

                item {
                    // ZenQuotes secondary card. Hides itself when offline,
                    // so the layout has no empty placeholder when the
                    // network call fails.
                    InternationalQuoteCard(quote = internationalQuote)
                }

                item {
                    MoodWidget(
                        selectedMood = todayMood?.mood,
                        onMoodSelected = { mood -> viewModel.saveMood(mood) }
                    )
                }

                item {
                    DesignEntryCard(onClick = onNavigateToDesign)
                }

                if (viewModel.aiSceneAvailable) {
                    item {
                        AiSceneEntryCard(onClick = { showAiScene = true })
                    }
                }

                item {
                    RecentMemoryCard(
                        memory = recentMemory,
                        onClick = onNavigateToMemories
                    )
                }
            }
        }

        if (showAiScene) {
            AiSceneDialog(onDismiss = { showAiScene = false })
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1209)
@Composable
private fun HomeScreenPreview() {
    HomeScreen(
        onNavigateToMemories = {},
        onNavigateToLetters = {},
        onNavigateToMood = {},
        onNavigateToDesign = {},
        onNavigateToSettings = {}
    )
}
