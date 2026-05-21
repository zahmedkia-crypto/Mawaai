package com.mawaai.love.app.ui.mood

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mawaai.love.app.core.components.ParticleHeartSystem
import com.mawaai.love.app.core.components.RomanticTopBar
import com.mawaai.love.app.core.components.RoseGlassCard
import com.mawaai.love.app.core.theme.CairoFamily
import com.mawaai.love.app.core.theme.MawaaiColors
import com.mawaai.love.app.data.model.MoodEntry
import com.mawaai.love.app.data.model.MoodType
import androidx.compose.ui.tooling.preview.Preview
import com.mawaai.love.app.R
import androidx.compose.ui.res.stringResource

@Composable
fun MoodScreen(
    onBack: () -> Unit,
    viewModel: MoodViewModel = hiltViewModel()
) {
    val moods by viewModel.moods.collectAsStateWithLifecycle()
    val latestMood by viewModel.latestMood.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            RomanticTopBar(title = stringResource(R.string.topbar_mood), onBack = onBack)
        },
        containerColor = androidx.compose.ui.graphics.Color.Transparent
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            ParticleHeartSystem(particleCount = 5)

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(24.dp)
            ) {
                item {
                    Text(
                        text = "كيف تشعرين اليوم يا رزان؟",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MawaaiColors.PearlWhite,
                        fontFamily = CairoFamily,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    MoodSelector(
                        selectedMood = latestMood?.mood,
                        onMoodSelected = { viewModel.addMood(it) }
                    )
                }
                
                item {
                    Spacer(modifier = Modifier.height(48.dp))
                    
                    Text(
                        text = "مخطط مشاعرك للأيام الماضية",
                        style = MaterialTheme.typography.titleMedium,
                        color = MawaaiColors.RoseGold,
                        fontFamily = CairoFamily
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    MoodChart(moods = moods.take(7).reversed())
                }
                
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    RoseGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = getMoodEncouragement(latestMood?.mood),
                            modifier = Modifier.padding(16.dp),
                            color = MawaaiColors.TextPrimary,
                            fontFamily = CairoFamily,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MoodSelector(
    selectedMood: MoodType?,
    onMoodSelected: (MoodType) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        MoodType.values().forEach { type ->
            val scale by animateFloatAsState(
                targetValue = if (selectedMood == type) 1.4f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)
            )
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onMoodSelected(type) }
                    .padding(8.dp)
            ) {
                Text(
                    text = type.emoji,
                    fontSize = 40.sp,
                    modifier = Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = type.label,
                    color = if (selectedMood == type) MawaaiColors.RoseGold else MawaaiColors.TextHint,
                    fontFamily = CairoFamily,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun MoodChart(moods: List<MoodEntry>) {
    if (moods.isEmpty()) return
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .background(MawaaiColors.CardDark, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val stepX = width / (moods.size.coerceAtLeast(2) - 1).toFloat()
            
            val path = Path()
            moods.forEachIndexed { index, entry ->
                val x = index * stepX
                // Map mood to height (5 types)
                val moodValue = entry.mood.ordinal.toFloat()
                val y = height - (moodValue / 4f * height)
                
                if (index == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
                
                drawCircle(
                    color = MawaaiColors.RoseGold,
                    radius = 4.dp.toPx(),
                    center = Offset(x, y)
                )
            }
            
            drawPath(
                path = path,
                color = MawaaiColors.RoseGold,
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}

fun getMoodEncouragement(mood: MoodType?): String {
    return when (mood) {
        MoodType.HAPPY -> "سعادتك هي سعادتي... دامت هذه الابتسامة 💕"
        MoodType.LOVING -> "أشعر بحبك في كل نبضة من قلبي 🥰"
        MoodType.AMAZED -> "الحياة معكِ مليئة بالمفاجآت الجميلة 😍"
        MoodType.GRATEFUL -> "أنا الممتن لوجودكِ في حياتي يا رزان 💍"
        MoodType.EXCITED -> "الحماس معكِ له طعم آخر... لننطلق! 💫"
        else -> "مهما كان شعورك، أنا دائماً هنا بجانبكِ 💕"
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1209)
@Composable
private fun MoodScreenPreview() {
    MoodScreen(onBack = {})
}