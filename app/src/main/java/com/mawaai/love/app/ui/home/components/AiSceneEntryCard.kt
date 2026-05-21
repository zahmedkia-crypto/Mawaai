package com.mawaai.love.app.ui.home.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mawaai.love.app.R
import com.mawaai.love.app.core.components.RoseGlassCard
import com.mawaai.love.app.core.theme.AmiriFamily
import com.mawaai.love.app.core.theme.CairoFamily
import com.mawaai.love.app.core.theme.MawaaiColors

/**
 * Home-screen entry card for the AI Scene Generator. Tapping the card
 * opens [com.mawaai.love.app.ui.scene.AiSceneDialog]. The card itself
 * is intentionally subtle — slightly smaller than the Memories /
 * Design entries so the romantic-side cards stay primary.
 */
@Composable
fun AiSceneEntryCard(onClick: () -> Unit) {
    RoseGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MawaaiColors.ChampagneGold,
                modifier = Modifier.size(36.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.ai_scene_card_title),
                    fontFamily = AmiriFamily,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MawaaiColors.RoseGold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.ai_scene_card_subtitle),
                    fontFamily = CairoFamily,
                    fontSize = 13.sp,
                    color = MawaaiColors.TextSecondary
                )
            }
        }
    }
}
