package com.mawaai.love.app.design.presentation.flow

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.ImageNotSupported
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.mawaai.love.app.R
import com.mawaai.love.app.core.theme.CairoFamily
import com.mawaai.love.app.core.theme.MawaaiColors
import com.mawaai.love.app.design.presentation.main.DesignRoute
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController

@Composable
fun PreviewScreen(
    nav: NavController,
    sessionId: String,
    viewModel: PreviewViewModel = hiltViewModel()
) {
    LaunchedEffect(sessionId) { viewModel.load(sessionId) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.screen_preview),
            fontFamily = CairoFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = MawaaiColors.DesignTextLight,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MawaaiColors.DesignSurface)
                .border(1.dp, MawaaiColors.DesignGold.copy(alpha = 0.4f), RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            val uri = state.inputUri
            if (uri != null) {
                AsyncImage(
                    model = uri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.ImageNotSupported,
                        contentDescription = null,
                        tint = MawaaiColors.DesignHennaLight
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.preview_missing_input),
                        fontFamily = CairoFamily,
                        fontSize = 13.sp,
                        color = MawaaiColors.DesignHennaLight
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                val next = if (state.isConverterFlow) {
                    DesignRoute.StyleSelect.create(sessionId)
                } else {
                    DesignRoute.Suggestions.create(sessionId)
                }
                nav.navigate(next)
            },
            enabled = state.inputUri != null,
            colors = ButtonDefaults.buttonColors(
                containerColor = MawaaiColors.DesignGold,
                contentColor = MawaaiColors.DesignBgDark
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.action_continue),
                fontFamily = CairoFamily,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(6.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                contentDescription = null
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1209)
@Composable
private fun PreviewScreenPreview() {
    PreviewScreen(nav = rememberNavController(), sessionId = "preview")
}
