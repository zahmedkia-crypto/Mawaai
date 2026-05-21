package com.mawaai.love.app.design.presentation.flow

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.mawaai.love.app.R
import com.mawaai.love.app.core.theme.CairoFamily
import com.mawaai.love.app.core.theme.MawaaiColors
import com.mawaai.love.app.design.domain.model.ConversionStyle
import com.mawaai.love.app.design.presentation.main.DesignRoute
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController

@Composable
fun StyleSelectionScreen(
    nav: NavController,
    sessionId: String,
    viewModel: StyleSelectionViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.converter_subheadline),
            fontFamily = CairoFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = MawaaiColors.DesignTextLight,
            modifier = Modifier.padding(20.dp)
        )
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(state.styles, key = { it.id }) { style ->
                StyleRow(
                    style = style,
                    selected = state.selectedStyleId == style.id,
                    onClick = { viewModel.select(style.id) }
                )
            }
        }
        Button(
            onClick = {
                viewModel.persistSelection(sessionId)
                nav.navigate(DesignRoute.Processing.create(sessionId))
            },
            enabled = state.selectedStyleId != null,
            colors = ButtonDefaults.buttonColors(
                containerColor = MawaaiColors.DesignGold,
                contentColor = MawaaiColors.DesignBgDark
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
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

@Composable
private fun StyleRow(
    style: ConversionStyle,
    selected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (selected) MawaaiColors.DesignGold else MawaaiColors.DesignGold.copy(alpha = 0.25f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MawaaiColors.DesignSurface)
            .border(if (selected) 2.dp else 1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = style.nameAr,
                fontFamily = CairoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MawaaiColors.DesignTextLight
            )
            Text(
                text = style.descriptionAr,
                fontFamily = CairoFamily,
                fontSize = 12.sp,
                color = MawaaiColors.DesignHennaLight
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1209)
@Composable
private fun StyleSelectionScreenPreview() {
    StyleSelectionScreen(nav = rememberNavController(), sessionId = "preview")
}
