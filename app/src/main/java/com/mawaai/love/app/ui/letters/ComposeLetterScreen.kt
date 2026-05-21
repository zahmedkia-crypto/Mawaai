package com.mawaai.love.app.ui.letters

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mawaai.love.app.core.components.HeartButton
import com.mawaai.love.app.core.components.RomanticTopBar
import com.mawaai.love.app.core.theme.CairoFamily
import com.mawaai.love.app.core.theme.MawaaiColors
import com.mawaai.love.app.data.model.LoveLetter
import androidx.compose.ui.tooling.preview.Preview
import com.mawaai.love.app.R
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposeLetterScreen(
    onBack: () -> Unit,
    viewModel: LettersViewModel = hiltViewModel()
) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            RomanticTopBar(title = stringResource(R.string.topbar_compose_letter), onBack = onBack)
        },
        containerColor = Color(0xFFFFF8F0) // Cream paper color
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Paper Texture (Horizontal lines)
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
                    .verticalScroll(rememberScrollState())
            ) {
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("عنوان الرسالة", fontFamily = CairoFamily, color = Color(0xFF8D6E63)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.textFieldColors(
                        containerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = Color(0xFF4A3728)
                    ),
                    textStyle = LocalTextStyle.current.copy(
                        fontFamily = CairoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = Color(0xFF4A3728)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                TextField(
                    value = body,
                    onValueChange = { body = it },
                    placeholder = { Text("اكتب مشاعرك هنا...", fontFamily = CairoFamily, color = Color(0xFF8D6E63)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .defaultMinSize(minHeight = 400.dp),
                    colors = TextFieldDefaults.textFieldColors(
                        containerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = Color(0xFF4A3728)
                    ),
                    textStyle = LocalTextStyle.current.copy(
                        fontFamily = CairoFamily,
                        fontSize = 18.sp,
                        color = Color(0xFF5D4037),
                        lineHeight = 32.sp
                    )
                )

                Spacer(modifier = Modifier.height(32.dp))

                HeartButton(
                    text = "إرسال الرسالة 💕",
                    onClick = {
                        if (title.isNotBlank() && body.isNotBlank()) {
                            val letter = LoveLetter(
                                title = title,
                                body = body
                            )
                            viewModel.addLetter(letter) {
                                onBack()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1209)
@Composable
private fun ComposeLetterScreenPreview() {
    ComposeLetterScreen(onBack = {})
}