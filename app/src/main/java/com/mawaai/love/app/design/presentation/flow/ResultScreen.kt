package com.mawaai.love.app.design.presentation.flow

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mawaai.love.app.R
import com.mawaai.love.app.core.theme.CairoFamily
import com.mawaai.love.app.core.theme.MawaaiColors
import com.mawaai.love.app.design.presentation.main.DesignRoute
import java.io.File
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController

@Composable
fun ResultScreen(
    nav: NavController,
    sessionId: String,
    viewModel: ResultViewModel = hiltViewModel()
) {
    LaunchedEffect(sessionId) { viewModel.load(sessionId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val savedMsg = stringResource(R.string.result_saved_toast)
    val errorPrefix = stringResource(R.string.result_save_failed)

    LaunchedEffect(state.savedUri) {
        if (state.savedUri != null) {
            Toast.makeText(context, savedMsg, Toast.LENGTH_SHORT).show()
        }
    }
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            Toast.makeText(context, "$errorPrefix: $it", Toast.LENGTH_LONG).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.screen_result),
            fontFamily = CairoFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = MawaaiColors.DesignTextLight
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(18.dp))
                .background(MawaaiColors.DesignSurface)
                .border(1.dp, MawaaiColors.DesignGold.copy(alpha = 0.4f), RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            val imageUri = state.imageUri
            if (imageUri != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imageUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = stringResource(R.string.screen_result),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = null,
                    tint = MawaaiColors.DesignGold
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = { viewModel.saveToGallery(sessionId, "result") },
            enabled = !state.isSaving && state.imageUri != null,
            colors = ButtonDefaults.buttonColors(
                containerColor = MawaaiColors.DesignGold,
                contentColor = MawaaiColors.DesignBgDark
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.height(18.dp).width(18.dp),
                    color = MawaaiColors.DesignBgDark,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(8.dp))
            } else {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
            }
            Text(stringResource(R.string.action_save), fontFamily = CairoFamily, fontWeight = FontWeight.Bold)
        }

        OutlinedButton(
            onClick = { nav.navigate(DesignRoute.Customize.create(sessionId)) },
            enabled = state.imageUri != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Palette, contentDescription = null, tint = MawaaiColors.DesignGold)
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.action_customize_color),
                fontFamily = CairoFamily,
                fontWeight = FontWeight.Bold,
                color = MawaaiColors.DesignGold
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            SecondaryAction(
                icon = Icons.Default.Share,
                labelRes = R.string.action_share,
                onClick = {
                    val shareUri = shareableUri(context, state.savedUri ?: state.imageUri)
                    if (shareUri != null) {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "image/png"
                            putExtra(Intent.EXTRA_STREAM, shareUri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, null))
                    }
                },
                modifier = Modifier.weight(1f)
            )
            SecondaryAction(
                icon = Icons.Default.Edit,
                labelRes = R.string.action_edit_again,
                onClick = {
                    nav.popBackStack(
                        route = DesignRoute.Suggestions.route,
                        inclusive = false
                    )
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SecondaryAction(
    icon: ImageVector,
    labelRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
        modifier = modifier
    ) {
        Icon(icon, contentDescription = null, tint = MawaaiColors.DesignGold)
        Spacer(Modifier.width(6.dp))
        Text(
            text = stringResource(labelRes),
            fontFamily = CairoFamily,
            color = MawaaiColors.DesignGold,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
    }
}

private fun shareableUri(context: android.content.Context, uri: Uri?): Uri? {
    if (uri == null) return null
    return when (uri.scheme) {
        "content" -> uri
        "file" -> uri.path?.let { File(it) }?.takeIf { it.exists() }?.let { file ->
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }
        else -> null
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1209)
@Composable
private fun ResultScreenPreview() {
    ResultScreen(nav = rememberNavController(), sessionId = "preview")
}
