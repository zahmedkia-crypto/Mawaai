package com.mawaai.love.app.ui.memories

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import com.mawaai.love.app.R
import com.mawaai.love.app.core.components.RomanticTopBar
import com.mawaai.love.app.core.theme.AmiriFamily
import com.mawaai.love.app.core.theme.CairoFamily
import com.mawaai.love.app.core.theme.MawaaiColors
import com.mawaai.love.app.core.utils.DateUtils
import com.mawaai.love.app.data.model.Memory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import java.io.File
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun MemoryDetailScreen(
    memoryId: Long,
    onBack: () -> Unit,
    viewModel: MemoriesViewModel = hiltViewModel()
) {
    var memory by remember { mutableStateOf<Memory?>(null) }
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    var dominantColor by remember { mutableStateOf(MawaaiColors.DeepNight) }
    val shareChooser = stringResource(R.string.memory_share_chooser)
    val shareTextTemplate = stringResource(R.string.memory_share_text)

    LaunchedEffect(memoryId) {
        memory = viewModel.getMemoryById(memoryId)
        memory?.imagePath?.let { path ->
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(path)
                .allowHardware(false) // Required for Palette
                .build()
            
            val result = (loader.execute(request) as? SuccessResult)?.drawable
            val bitmap = (result as? BitmapDrawable)?.bitmap
            bitmap?.let {
                Palette.from(it).generate { palette ->
                    palette?.dominantSwatch?.rgb?.let { color ->
                        dominantColor = Color(color)
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            RomanticTopBar(
                title = memory?.title ?: "تفاصيل الذكرى",
                onBack = onBack,
                actions = {
                    IconButton(onClick = {
                        memory?.let { m ->
                            val shareUri = memoryShareUri(context, m.imagePath)
                            val text = shareTextTemplate.format(m.title, m.description)
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                if (shareUri != null) {
                                    type = "image/jpeg"
                                    putExtra(Intent.EXTRA_STREAM, shareUri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                } else {
                                    type = "text/plain"
                                }
                                putExtra(Intent.EXTRA_SUBJECT, m.title)
                                putExtra(Intent.EXTRA_TEXT, text)
                            }
                            context.startActivity(Intent.createChooser(intent, shareChooser))
                        }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = MawaaiColors.RoseGold)
                    }
                    IconButton(onClick = { 
                        memory?.let { 
                            viewModel.deleteMemory(it)
                            onBack()
                        }
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = MawaaiColors.RoseGold)
                    }
                }
            )
        },
        containerColor = dominantColor.copy(alpha = 0.5f)
    ) { innerPadding ->
        memory?.let { m ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(Brush.verticalGradient(listOf(dominantColor.copy(alpha = 0.3f), MawaaiColors.DeepNight)))
                    .verticalScroll(scrollState)
            ) {
                // Parallax-ish Image
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                ) {
                    AsyncImage(
                        model = m.imagePath,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    
                    // Gradient Overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, MawaaiColors.DeepNight),
                                    startY = 200f
                                )
                            )
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .offset(y = (-40).dp)
                ) {
                    // Date
                    Text(
                        text = DateUtils.formatArabicDate(m.date),
                        fontFamily = AmiriFamily,
                        fontSize = 20.sp,
                        color = MawaaiColors.ChampagneGold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Title
                    Text(
                        text = m.title,
                        fontFamily = CairoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        color = MawaaiColors.PearlWhite
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Category & Mood Tags
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text(m.category.name, fontFamily = CairoFamily) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                labelColor = MawaaiColors.RoseGold,
                                containerColor = MawaaiColors.CardDark
                            )
                        )
                        SuggestionChip(
                            onClick = {},
                            label = { Text("${m.mood.emoji} ${m.mood.label}", fontFamily = CairoFamily) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                labelColor = MawaaiColors.RoseGold,
                                containerColor = MawaaiColors.CardDark
                            )
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Description
                    Text(
                        text = m.description,
                        fontFamily = CairoFamily,
                        fontSize = 18.sp,
                        color = MawaaiColors.TextSecondary,
                        lineHeight = 32.sp
                    )
                }
            }
        } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MawaaiColors.RoseGold)
        }
    }
}

/**
 * Wrap the local memory image path in a `content://` URI via the app's
 * existing FileProvider. Returns null if the path is missing or unreadable —
 * the share intent then falls back to text-only.
 */
private fun memoryShareUri(context: android.content.Context, imagePath: String?): Uri? {
    if (imagePath.isNullOrBlank()) return null
    val file = File(imagePath)
    if (!file.exists() || !file.canRead()) return null
    return runCatching {
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }.getOrNull()
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1209)
@Composable
private fun MemoryDetailScreenPreview() {
    MemoryDetailScreen(memoryId = 0L, onBack = {})
}
