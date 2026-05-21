package com.mawaai.love.app.design.presentation.flow

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.mawaai.love.app.R
import com.mawaai.love.app.design.domain.model.InputMethod
import com.mawaai.love.app.design.presentation.common.DesignActionCard
import com.mawaai.love.app.design.presentation.main.DesignRoute
import java.io.File
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController

@Composable
fun InputMethodScreen(
    nav: NavController,
    categoryId: String?,
    subTypeId: String?,
    isConverterFlow: Boolean,
    viewModel: InputMethodViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val pendingCameraUri = remember { mutableStateOf<Uri?>(null) }
    val pendingSessionId = remember { mutableStateOf<String?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val sessionId = pendingSessionId.value ?: return@rememberLauncherForActivityResult
        pendingSessionId.value = null
        if (uri != null) {
            viewModel.setInputUri(sessionId, uri)
            nav.navigate(DesignRoute.Preview.create(sessionId))
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val sessionId = pendingSessionId.value ?: return@rememberLauncherForActivityResult
        val uri = pendingCameraUri.value
        pendingSessionId.value = null
        pendingCameraUri.value = null
        if (success && uri != null) {
            viewModel.setInputUri(sessionId, uri)
            nav.navigate(DesignRoute.Preview.create(sessionId))
        }
    }

    fun launchCameraCapture() {
        val sessionId = pendingSessionId.value ?: return
        val cacheDir = File(context.cacheDir, "captures").apply { mkdirs() }
        val outFile = File(cacheDir, "capture-${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            outFile
        )
        pendingCameraUri.value = uri
        cameraLauncher.launch(uri)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchCameraCapture()
        } else {
            pendingSessionId.value = null
            Toast.makeText(
                context,
                context.getString(R.string.permission_camera_denied),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val onPick: (InputMethod) -> Unit = { method ->
        val sessionId = viewModel.createSession(
            categoryId = categoryId,
            subTypeId = subTypeId,
            method = method,
            isConverterFlow = isConverterFlow
        )
        when (method) {
            InputMethod.DRAW -> nav.navigate(DesignRoute.Canvas.create(sessionId))
            InputMethod.UPLOAD -> {
                pendingSessionId.value = sessionId
                photoPicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
            InputMethod.CAMERA -> {
                pendingSessionId.value = sessionId
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
                if (granted) {
                    launchCameraCapture()
                } else {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        DesignActionCard(
            icon = Icons.Default.Edit,
            title = stringResource(R.string.input_draw_title),
            subtitle = stringResource(R.string.input_draw_subtitle),
            onClick = { onPick(InputMethod.DRAW) }
        )
        DesignActionCard(
            icon = Icons.Default.Image,
            title = stringResource(R.string.input_upload_title),
            subtitle = stringResource(R.string.input_upload_subtitle),
            onClick = { onPick(InputMethod.UPLOAD) }
        )
        DesignActionCard(
            icon = Icons.Default.CameraAlt,
            title = stringResource(R.string.input_camera_title),
            subtitle = stringResource(R.string.input_camera_subtitle),
            onClick = { onPick(InputMethod.CAMERA) }
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1209)
@Composable
private fun InputMethodScreenPreview() {
    InputMethodScreen(nav = rememberNavController(), categoryId = null, subTypeId = null, isConverterFlow = false)
}
