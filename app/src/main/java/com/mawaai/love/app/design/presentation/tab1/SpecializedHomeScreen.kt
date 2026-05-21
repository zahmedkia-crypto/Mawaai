package com.mawaai.love.app.design.presentation.tab1

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
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
import com.mawaai.love.app.design.domain.model.DesignCategory
import com.mawaai.love.app.design.domain.model.DesignSubType
import com.mawaai.love.app.design.presentation.main.DesignRoute
import kotlinx.coroutines.launch
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpecializedHomeScreen(
    nav: NavController,
    viewModel: SpecializedHomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.design_tab1_headline),
            fontFamily = CairoFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = MawaaiColors.DesignTextLight,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
        )
        Text(
            text = stringResource(R.string.design_tab1_subheadline),
            fontFamily = CairoFamily,
            fontSize = 14.sp,
            color = MawaaiColors.DesignHennaLight,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(Modifier.height(12.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(state.categories, key = { it.id }) { category ->
                CategoryTile(
                    category = category,
                    onClick = { viewModel.selectCategory(category) }
                )
            }
        }
    }

    state.selectedCategory?.let { category ->
        ModalBottomSheet(
            onDismissRequest = { viewModel.dismissSheet() },
            sheetState = sheetState,
            containerColor = MawaaiColors.DesignSurface
        ) {
            SubTypeSheetContent(
                category = category,
                onSubTypeSelected = { subType ->
                    val sessionId = viewModel.createSession(category.id, subType.id)
                    scope.launch {
                        sheetState.hide()
                        viewModel.dismissSheet()
                        nav.navigate(DesignRoute.InputMethod.create(category.id, subType.id))
                    }
                }
            )
        }
    }
}

@Composable
private fun CategoryTile(
    category: DesignCategory,
    onClick: () -> Unit
) {
    val iconRes: Int? = when (category.iconKey) {
        "henna" -> R.drawable.ic_henna
        "abaya" -> R.drawable.ic_abaya
        "walls" -> R.drawable.ic_walls
        "thob_sudani" -> R.drawable.ic_thob_sudani
        else -> null
    }
    val accent = runCatching { Color(android.graphics.Color.parseColor(category.accentColor)) }
        .getOrDefault(MawaaiColors.DesignGold)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MawaaiColors.DesignSurface)
            .border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(accent.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center
        ) {
            if (iconRes != null) {
                androidx.compose.material3.Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = accent
                )
            } else {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = accent
                )
            }
        }
        Column {
            Text(
                text = category.nameAr,
                fontFamily = CairoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = MawaaiColors.DesignTextLight
            )
            Text(
                text = category.nameEn,
                fontFamily = CairoFamily,
                fontSize = 12.sp,
                color = MawaaiColors.DesignHennaLight
            )
        }
    }
}

@Composable
private fun SubTypeSheetContent(
    category: DesignCategory,
    onSubTypeSelected: (DesignSubType) -> Unit
) {
    Column(modifier = Modifier.padding(20.dp)) {
        Text(
            text = category.nameAr,
            fontFamily = CairoFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = MawaaiColors.DesignTextLight
        )
        Text(
            text = category.descriptionAr,
            fontFamily = CairoFamily,
            fontSize = 13.sp,
            color = MawaaiColors.DesignHennaLight
        )
        Spacer(Modifier.height(16.dp))
        category.subTypes.forEach { subType ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MawaaiColors.DesignBgDark)
                    .clickable { onSubTypeSelected(subType) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = subType.nameAr,
                        fontFamily = CairoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MawaaiColors.DesignTextLight
                    )
                    Text(
                        text = subType.nameEn,
                        fontFamily = CairoFamily,
                        fontSize = 12.sp,
                        color = MawaaiColors.DesignHennaLight
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1209)
@Composable
private fun SpecializedHomeScreenPreview() {
    SpecializedHomeScreen(nav = rememberNavController())
}
