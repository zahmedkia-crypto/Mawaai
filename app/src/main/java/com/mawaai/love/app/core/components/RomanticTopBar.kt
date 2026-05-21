package com.mawaai.love.app.core.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mawaai.love.app.core.theme.CairoFamily
import com.mawaai.love.app.core.theme.MawaaiColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RomanticTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = title,
                fontFamily = CairoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = MawaaiColors.RoseGold
            )
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MawaaiColors.RoseGold
                    )
                }
            }
        },
        actions = actions,
        // Transparent container so the ThemedBackground gradient flows from
        // the notch all the way down to the content with no chrome band
        // in between. Title + icons stay readable thanks to their gold tint
        // against the dark scrim of the ThemedBackground.
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent,
            titleContentColor = MawaaiColors.RoseGold,
            navigationIconContentColor = MawaaiColors.RoseGold,
            actionIconContentColor = MawaaiColors.RoseGold
        )
    )
}
