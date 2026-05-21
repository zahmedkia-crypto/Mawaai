package com.mawaai.love.app.ui.letters

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mawaai.love.app.core.components.RomanticTopBar
import com.mawaai.love.app.core.theme.AmiriFamily
import com.mawaai.love.app.core.theme.CairoFamily
import com.mawaai.love.app.core.theme.MawaaiColors
import com.mawaai.love.app.core.utils.DateUtils
import com.mawaai.love.app.data.model.LoveLetter
import androidx.compose.ui.tooling.preview.Preview
import com.mawaai.love.app.R
import androidx.compose.ui.res.stringResource

@Composable
fun LetterDetailScreen(
    letterId: Long,
    onBack: () -> Unit,
    viewModel: LettersViewModel = hiltViewModel()
) {
    var letter by remember { mutableStateOf<LoveLetter?>(null) }
    val scrollState = rememberScrollState()

    LaunchedEffect(letterId) {
        letter = viewModel.getLetterById(letterId)
    }

    Scaffold(
        topBar = {
            RomanticTopBar(
                title = stringResource(R.string.topbar_letter_detail),
                onBack = onBack,
                actions = {
                    IconButton(onClick = {
                        letter?.let { 
                            val updated = it.copy(isFavorite = !it.isFavorite)
                            viewModel.updateLetter(updated)
                            letter = updated
                        }
                    }) {
                        Icon(
                            imageVector = if (letter?.isFavorite == true) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = MawaaiColors.RoseGold
                        )
                    }
                    IconButton(onClick = {
                        letter?.let { 
                            viewModel.deleteLetter(it)
                            onBack()
                        }
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = MawaaiColors.RoseGold)
                    }
                }
            )
        },
        containerColor = Color(0xFFFFF8F0) // Cream paper color
    ) { innerPadding ->
        letter?.let { l ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Paper Texture
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val lineHeight = 32.dp.toPx()
                    val lineCount = (size.height / lineHeight).toInt()
                    for (i in 1..lineCount) {
                        val y = i * lineHeight
                        drawLine(
                            color = Color(0xFFEFEBE9),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .verticalScroll(scrollState)
                ) {
                    Text(
                        text = DateUtils.formatArabicDate(l.createdAt),
                        fontFamily = AmiriFamily,
                        fontSize = 18.sp,
                        color = Color(0xFF8D6E63)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = l.title,
                        fontFamily = CairoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        color = Color(0xFF4A3728)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = l.body,
                        fontFamily = CairoFamily,
                        fontSize = 18.sp,
                        color = Color(0xFF5D4037),
                        lineHeight = 36.sp
                    )
                    
                    Spacer(modifier = Modifier.height(48.dp))
                    
                    // Signature-like end
                    Text(
                        text = "مع كل الحب،",
                        fontFamily = AmiriFamily,
                        fontSize = 22.sp,
                        color = Color(0xFF4A3728),
                        modifier = Modifier.align(Alignment.End)
                    )
                    Text(
                        text = "مأواي قلبكِ",
                        fontFamily = AmiriFamily,
                        fontSize = 24.sp,
                        color = Color(0xFF4A3728),
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MawaaiColors.RoseGold)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1209)
@Composable
private fun LetterDetailScreenPreview() {
    LetterDetailScreen(letterId = 0L, onBack = {})
}