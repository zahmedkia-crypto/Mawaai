package com.mawaai.love.app.ui.letters

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mawaai.love.app.core.components.ParticleHeartSystem
import com.mawaai.love.app.core.components.RomanticTopBar
import com.mawaai.love.app.core.theme.CairoFamily
import com.mawaai.love.app.core.theme.MawaaiColors
import com.mawaai.love.app.core.utils.DateUtils
import com.mawaai.love.app.data.model.LoveLetter
import androidx.compose.ui.tooling.preview.Preview
import com.mawaai.love.app.R
import androidx.compose.ui.res.stringResource

@Composable
fun LettersScreen(
    onLetterClick: (Long) -> Unit,
    onComposeLetter: () -> Unit,
    onBack: () -> Unit,
    viewModel: LettersViewModel = hiltViewModel()
) {
    val letters by viewModel.letters.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            RomanticTopBar(title = stringResource(R.string.topbar_letters), onBack = onBack)
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onComposeLetter,
                containerColor = MawaaiColors.DeepRose,
                contentColor = MawaaiColors.PearlWhite,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("رسالة جديدة") }
            )
        },
        containerColor = androidx.compose.ui.graphics.Color.Transparent
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            ParticleHeartSystem(particleCount = 5)

            Column(modifier = Modifier.fillMaxSize()) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MawaaiColors.SurfaceDark,
                    contentColor = MawaaiColors.RoseGold,
                    divider = {}
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { viewModel.selectTab(0) },
                        text = { Text("رسائلي لكِ", fontFamily = CairoFamily) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { viewModel.selectTab(1) },
                        text = { Text("المفضلة", fontFamily = CairoFamily) }
                    )
                }

                if (letters.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (selectedTab == 0) "لم تكتب أي رسالة بعد 💕" else "لا توجد رسائل مفضلة",
                            color = MawaaiColors.TextSecondary,
                            fontFamily = CairoFamily
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(letters, key = { it.id }) { letter ->
                            LetterCard(letter = letter, onClick = { onLetterClick(letter.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LetterCard(letter: LoveLetter, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(4.dp), // Slightly sharp corners for paper look
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8F0)) // Cream paper color
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Paper texture/lines could be added here
            
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = letter.title,
                        fontFamily = CairoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF4A3728), // Dark brown text
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    
                    if (letter.isFavorite) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = Color(0xFFE0294A),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = letter.body,
                    fontFamily = CairoFamily,
                    fontSize = 14.sp,
                    color = Color(0xFF5D4037),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 22.sp
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = DateUtils.formatArabicDate(letter.createdAt),
                    fontFamily = CairoFamily,
                    fontSize = 11.sp,
                    color = Color(0xFF8D6E63)
                )
            }
            
            // Gold Seal in corner
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 12.dp, end = 12.dp)
                    .size(24.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(MawaaiColors.GradGold),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1209)
@Composable
private fun LettersScreenPreview() {
    LettersScreen(onLetterClick = {}, onComposeLetter = {}, onBack = {})
}