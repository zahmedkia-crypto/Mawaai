package com.mawaai.love.app.design.presentation.common

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mawaai.love.app.core.theme.CairoFamily
import com.mawaai.love.app.core.theme.MawaaiColors

@Composable
fun DesignTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // Transparent chrome — the ThemedBackground / DesignSurface
            // gradient flows from the notch all the way down with no dark
            // band in between. `statusBarsPadding` still pushes the back
            // button + title BELOW the system clock so touch targets are
            // not clipped behind the notch in edge-to-edge mode.
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = MawaaiColors.DesignGold
                )
            }
        } else {
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = title,
            fontFamily = CairoFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = MawaaiColors.DesignTextLight,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
        )
        actions()
    }
}
