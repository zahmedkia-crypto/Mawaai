package com.mawaai.love.app.ui.memories

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mawaai.love.app.core.components.ParticleHeartSystem
import com.mawaai.love.app.core.components.RomanticTopBar
import com.mawaai.love.app.core.theme.MawaaiColors
import com.mawaai.love.app.data.model.MemoryCategory
import com.mawaai.love.app.ui.memories.components.MemoryCard
import androidx.compose.ui.tooling.preview.Preview
import com.mawaai.love.app.R
import androidx.compose.ui.res.stringResource

@Composable
fun MemoriesScreen(
    onMemoryClick: (Long) -> Unit,
    onAddMemory: () -> Unit,
    onBack: () -> Unit,
    viewModel: MemoriesViewModel = hiltViewModel()
) {
    val memories by viewModel.memories.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            RomanticTopBar(
                title = stringResource(R.string.topbar_memories),
                onBack = onBack
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddMemory,
                containerColor = MawaaiColors.DeepRose,
                contentColor = MawaaiColors.PearlWhite,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("ذكرى جديدة") }
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
                // Category Chips
                ScrollableTabRow(
                    selectedTabIndex = if (selectedCategory == null) 0 else MemoryCategory.entries.indexOf(selectedCategory) + 1,
                    containerColor = MawaaiColors.SurfaceDark,
                    contentColor = MawaaiColors.RoseGold,
                    edgePadding = 16.dp,
                    divider = {}
                ) {
                    Tab(
                        selected = selectedCategory == null,
                        onClick = { viewModel.selectCategory(null) },
                        text = { Text("الكل") }
                    )
                    MemoryCategory.entries.forEach { category ->
                        Tab(
                            selected = selectedCategory == category,
                            onClick = { viewModel.selectCategory(category) },
                            text = { 
                                val label = when(category) {
                                    MemoryCategory.ROMANTIC -> "رومانسي"
                                    MemoryCategory.TRAVEL -> "سفر"
                                    MemoryCategory.FOOD -> "أكل"
                                    MemoryCategory.SPECIAL_DAY -> "يوم خاص"
                                    MemoryCategory.GENERAL -> "عام"
                                }
                                Text(label) 
                            }
                        )
                    }
                }

                if (memories.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        Text(
                            text = "أضيفي أولى ذكرياتكما 💕",
                            color = MawaaiColors.TextSecondary,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                } else {
                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalItemSpacing = 8.dp
                    ) {
                        items(memories, key = { it.id }) { memory ->
                            MemoryCard(
                                memory = memory,
                                onClick = { onMemoryClick(memory.id) }
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
private fun MemoriesScreenPreview() {
    MemoriesScreen(onMemoryClick = {}, onAddMemory = {}, onBack = {})
}