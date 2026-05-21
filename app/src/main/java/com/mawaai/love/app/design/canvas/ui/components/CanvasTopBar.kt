package com.mawaai.love.app.design.canvas.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mawaai.love.app.R
import com.mawaai.love.app.core.theme.CairoFamily
import com.mawaai.love.app.core.theme.MawaaiColors

@Composable
internal fun CanvasTopBar(
    onBack: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onClear: () -> Unit,
    onAiTips: () -> Unit,
    onPickTemplate: () -> Unit,
    onSave: () -> Unit,
    canUndo: Boolean,
    canRedo: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MawaaiColors.DesignSurface)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.action_back),
                tint = MawaaiColors.DesignGold
            )
        }
        IconButton(onClick = onUndo, enabled = canUndo) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Undo,
                contentDescription = stringResource(R.string.canvas_undo),
                tint = if (canUndo) MawaaiColors.DesignGold else MawaaiColors.DesignTextLight.copy(alpha = 0.4f)
            )
        }
        IconButton(onClick = onRedo, enabled = canRedo) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Redo,
                contentDescription = stringResource(R.string.canvas_redo),
                tint = if (canRedo) MawaaiColors.DesignGold else MawaaiColors.DesignTextLight.copy(alpha = 0.4f)
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = onClear) {
            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.canvas_clear), tint = MawaaiColors.DesignTextLight)
        }
        IconButton(onClick = onAiTips) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = stringResource(R.string.canvas_ai_tips),
                tint = MawaaiColors.DesignGold
            )
        }
        // Pick Template is always available — even from the converter flow
        // or a session without a category, callers fall back to "general"
        // so the gallery still has something to show.
        IconButton(onClick = onPickTemplate) {
            Icon(
                imageVector = Icons.Default.Image,
                contentDescription = stringResource(R.string.canvas_pick_template),
                tint = MawaaiColors.DesignGold
            )
        }
        Button(
            onClick = onSave,
            colors = ButtonDefaults.buttonColors(containerColor = MawaaiColors.DesignGold, contentColor = MawaaiColors.DesignBgDark),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(Icons.Default.Save, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.canvas_save_artwork), fontFamily = CairoFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}
