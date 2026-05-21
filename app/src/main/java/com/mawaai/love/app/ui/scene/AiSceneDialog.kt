package com.mawaai.love.app.ui.scene

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mawaai.love.app.R
import com.mawaai.love.app.core.theme.AmiriFamily
import com.mawaai.love.app.core.theme.CairoFamily
import com.mawaai.love.app.core.theme.MawaaiColors

/**
 * Modal dialog that drives the AI Scene Generator end to end:
 *  - Prompt entry (single-line, IME action = Go)
 *  - Generate / regenerate button
 *  - Generation progress + error
 *  - Result preview + Save-to-gallery button
 *
 * The dialog is dismissable via [onDismiss] (system back, outside tap)
 * and wires save-status feedback through a Toast on success/failure
 * since the Scaffold-level snackbar host isn't reachable from here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSceneDialog(
    onDismiss: () -> Unit,
    viewModel: AiSceneViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    var prompt by rememberSaveable { mutableStateOf("") }

    val savedToast = stringResource(R.string.ai_scene_status_saved)
    val saveFailedToast = stringResource(R.string.ai_scene_status_save_failed)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            shape = RoundedCornerShape(24.dp),
            color = MawaaiColors.CardElevated,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.ai_scene_dialog_title),
                    fontFamily = AmiriFamily,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MawaaiColors.RoseGold
                )

                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    placeholder = {
                        Text(
                            text = stringResource(R.string.ai_scene_prompt_hint),
                            fontFamily = CairoFamily,
                            color = MawaaiColors.TextHint
                        )
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    enabled = state !is SceneState.Generating,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MawaaiColors.RoseGold,
                        unfocusedBorderColor = MawaaiColors.RoseGoldDim,
                        focusedTextColor = MawaaiColors.TextPrimary,
                        unfocusedTextColor = MawaaiColors.TextPrimary,
                        cursorColor = MawaaiColors.RoseGold
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Result panel — renders one of: idle hint, spinner,
                // ready bitmap, or error text. Fixed minimum height so
                // the dialog doesn't jump as state transitions.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 180.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MawaaiColors.CardDark),
                    contentAlignment = Alignment.Center
                ) {
                    when (val s = state) {
                        SceneState.Idle -> Text(
                            text = stringResource(R.string.ai_scene_card_subtitle),
                            color = MawaaiColors.TextHint,
                            fontFamily = CairoFamily,
                            fontSize = 14.sp
                        )
                        SceneState.Generating -> Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(color = MawaaiColors.RoseGold)
                            Text(
                                text = stringResource(R.string.ai_scene_status_generating),
                                color = MawaaiColors.TextSecondary,
                                fontFamily = CairoFamily,
                                fontSize = 14.sp
                            )
                        }
                        is SceneState.Ready -> Image(
                            bitmap = s.bitmap.asImageBitmap(),
                            contentDescription = s.prompt
                        )
                        is SceneState.Error -> Text(
                            text = stringResource(s.reasonRes),
                            color = MawaaiColors.DeepRose,
                            fontFamily = CairoFamily,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                // Action row — context-sensitive primary button + close.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = stringResource(R.string.ai_scene_button_close),
                            color = MawaaiColors.TextSecondary,
                            fontFamily = CairoFamily
                        )
                    }

                    when (state) {
                        is SceneState.Ready -> {
                            Button(
                                onClick = {
                                    viewModel.saveToGallery { uri ->
                                        Toast.makeText(
                                            context,
                                            if (uri != null) savedToast else saveFailedToast,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MawaaiColors.ChampagneGold,
                                    contentColor = MawaaiColors.DeepNight
                                ),
                                modifier = Modifier.weight(2f)
                            ) {
                                Text(
                                    text = stringResource(R.string.ai_scene_button_save),
                                    fontFamily = CairoFamily,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        else -> {
                            Button(
                                onClick = { viewModel.generate(prompt) },
                                enabled = state !is SceneState.Generating,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MawaaiColors.RoseGold,
                                    contentColor = MawaaiColors.DeepNight
                                ),
                                modifier = Modifier.weight(2f)
                            ) {
                                Text(
                                    text = stringResource(
                                        if (state is SceneState.Error) R.string.ai_scene_button_retry
                                        else R.string.ai_scene_button_generate
                                    ),
                                    fontFamily = CairoFamily,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Fresh dialog instance starts in Idle so the previous run's bitmap
    // doesn't flash on top of a new prompt entry. The ViewModel is
    // already scoped to the dialog's nav entry by `hiltViewModel()`,
    // so this only fires when the user reopens the dialog after
    // dismissing it.
    DisposableEffect(Unit) {
        onDispose { viewModel.reset() }
    }
}

/**
 * Local Image wrapper so we don't pull in the full Coil + AsyncImage
 * dependency chain for what is just a static bitmap render. Sized
 * via Modifier defaults — the dialog's panel constrains the height.
 */
@Composable
private fun Image(
    bitmap: androidx.compose.ui.graphics.ImageBitmap,
    contentDescription: String?
) {
    androidx.compose.foundation.Image(
        bitmap = bitmap,
        contentDescription = contentDescription,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 240.dp, max = 420.dp)
            .clip(RoundedCornerShape(12.dp)),
        contentScale = androidx.compose.ui.layout.ContentScale.Fit,
        // Subtle tint on the night-mode background to prevent a pure
        // white SDXL output from blowing out against the deep card.
        colorFilter = null
    )
}
