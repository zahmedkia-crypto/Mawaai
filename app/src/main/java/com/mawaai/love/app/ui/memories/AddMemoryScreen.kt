package com.mawaai.love.app.ui.memories

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.mawaai.love.app.core.components.HeartButton
import com.mawaai.love.app.core.components.RomanticTopBar
import com.mawaai.love.app.core.theme.CairoFamily
import com.mawaai.love.app.core.theme.MawaaiColors
import com.mawaai.love.app.data.model.Memory
import com.mawaai.love.app.data.model.MemoryCategory
import com.mawaai.love.app.data.model.MoodType
import com.mawaai.love.app.ui.home.components.MoodWidget
import androidx.compose.ui.tooling.preview.Preview
import com.mawaai.love.app.R
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMemoryScreen(
    onBack: () -> Unit,
    viewModel: MemoriesViewModel = hiltViewModel()
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedCategory by remember { mutableStateOf(MemoryCategory.GENERAL) }
    var selectedMood by remember { mutableStateOf(MoodType.HAPPY) }
    val date by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var isSaving by remember { mutableStateOf(false) }
    var titleError by remember { mutableStateOf(false) }

    val context = LocalContext.current
    // The save runs on the application-scoped CoroutineScope (see
    // MemoriesViewModel.addMemory) so it survives popping this screen.
    // Toasts must therefore use the application context — if we used the
    // Activity context here, a fast back-press could race the Toast and
    // produce a leaked WindowContext warning on some devices.
    val appContext = context.applicationContext
    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    val errTitleRequired = stringResource(R.string.add_memory_title_required)
    val errSaveFailed = stringResource(R.string.add_memory_save_failed)
    val msgSaved = stringResource(R.string.add_memory_saved)

    Scaffold(
        topBar = {
            RomanticTopBar(title = stringResource(R.string.topbar_add_memory), onBack = onBack)
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Image Picker
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MawaaiColors.CardDark)
                    .clickable { imageLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (selectedImageUri != null) {
                    Box {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = { selectedImageUri = null },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
                        }
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = MawaaiColors.RoseGold,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            "اضغطي لإضافة صورة 📷",
                            fontFamily = CairoFamily,
                            color = MawaaiColors.TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Title
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    if (titleError && it.isNotBlank()) titleError = false
                },
                label = { Text("عنوان اللحظة", fontFamily = CairoFamily) },
                isError = titleError,
                supportingText = if (titleError) {
                    { Text(errTitleRequired, fontFamily = CairoFamily, color = MawaaiColors.DeepRose) }
                } else null,
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = MawaaiColors.RoseGold,
                    unfocusedBorderColor = MawaaiColors.CardElevated,
                    focusedLabelColor = MawaaiColors.RoseGold,
                    cursorColor = MawaaiColors.RoseGold,
                    errorBorderColor = MawaaiColors.DeepRose,
                    errorLabelColor = MawaaiColors.DeepRose
                ),
                textStyle = LocalTextStyle.current.copy(fontFamily = CairoFamily, fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("ماذا حدث في هذه اللحظة؟", fontFamily = CairoFamily) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = MawaaiColors.RoseGold,
                    unfocusedBorderColor = MawaaiColors.CardElevated,
                    focusedLabelColor = MawaaiColors.RoseGold,
                    cursorColor = MawaaiColors.RoseGold
                ),
                textStyle = LocalTextStyle.current.copy(fontFamily = CairoFamily)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Category
            Text(
                "التصنيف",
                fontFamily = CairoFamily,
                fontWeight = FontWeight.Bold,
                color = MawaaiColors.RoseGold,
                modifier = Modifier.align(Alignment.Start)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MemoryCategory.entries.forEach { category ->
                    val isSelected = selectedCategory == category
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = category },
                        label = {
                            val label = when(category) {
                                MemoryCategory.ROMANTIC -> "رومانسي"
                                MemoryCategory.TRAVEL -> "سفر"
                                MemoryCategory.FOOD -> "أكل"
                                MemoryCategory.SPECIAL_DAY -> "يوم خاص"
                                MemoryCategory.GENERAL -> "عام"
                            }
                            Text(label, fontFamily = CairoFamily)
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MawaaiColors.DeepRose,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mood
            MoodWidget(selectedMood = selectedMood, onMoodSelected = { selectedMood = it })

            Spacer(modifier = Modifier.height(32.dp))

            // Save Button
            HeartButton(
                text = "حفظ الذكرى 💕",
                isLoading = isSaving,
                onClick = {
                    val trimmed = title.trim()
                    if (trimmed.isBlank()) {
                        titleError = true
                        Toast.makeText(appContext, errTitleRequired, Toast.LENGTH_SHORT).show()
                        return@HeartButton
                    }
                    isSaving = true
                    val memory = Memory(
                        title = trimmed,
                        description = description.trim(),
                        imagePath = null, // Repo copies the image to internal storage and rewrites this.
                        date = date,
                        category = selectedCategory,
                        mood = selectedMood
                    )
                    // Pop back immediately for snappy UX; the save runs on the
                    // application scope so the user can keep navigating while
                    // the insert finishes. Toast confirmation comes from the
                    // app context so it doesn't depend on this screen being
                    // alive when the result arrives.
                    onBack()
                    viewModel.addMemory(memory, selectedImageUri) { result ->
                        isSaving = false
                        result
                            .onSuccess {
                                Toast.makeText(appContext, msgSaved, Toast.LENGTH_SHORT).show()
                            }
                            .onFailure { err ->
                                val msg = err.localizedMessage?.takeIf { it.isNotBlank() } ?: errSaveFailed
                                Toast.makeText(appContext, msg, Toast.LENGTH_LONG).show()
                            }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1209)
@Composable
private fun AddMemoryScreenPreview() {
    AddMemoryScreen(onBack = {})
}
