package com.mawaai.love.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mawaai.love.app.core.components.RomanticTopBar
import com.mawaai.love.app.core.theme.CairoFamily
import com.mawaai.love.app.core.theme.MawaaiColors
import com.mawaai.love.app.data.model.BackgroundTheme
import androidx.compose.ui.tooling.preview.Preview
import com.mawaai.love.app.R
import androidx.compose.ui.res.stringResource
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.WbSunny

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToAiProviders: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    var showPartnerNameDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            RomanticTopBar(title = stringResource(R.string.topbar_settings), onBack = onBack)
        },
        containerColor = androidx.compose.ui.graphics.Color.Transparent
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            item {
                SettingsSection(title = stringResource(R.string.settings_section_profile)) {
                    SettingsItem(
                        icon = Icons.Default.Person,
                        title = stringResource(R.string.settings_partner_name),
                        subtitle = profile.partnerName,
                        onClick = { showPartnerNameDialog = true }
                    )
                }
            }

            item {
                SettingsSection(title = "Creative Studio (AI)") {
                    SettingsItem(
                        icon = Icons.Default.Palette,
                        title = "AI Providers",
                        subtitle = "Configure Cloudflare, Groq, and Gemini",
                        onClick = onNavigateToAiProviders
                    )
                }
            }

            item {
                SettingsSection(title = stringResource(R.string.settings_section_notifications)) {
                    SettingsItem(
                        icon = Icons.Default.Notifications,
                        title = stringResource(R.string.settings_enable_notifications),
                        trailing = {
                            Switch(
                                checked = profile.notificationsEnabled,
                                onCheckedChange = { 
                                    viewModel.updateProfile(profile.copy(notificationsEnabled = it))
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MawaaiColors.RoseGold,
                                    checkedTrackColor = MawaaiColors.DeepRose
                                )
                            )
                        }
                    )
                }
            }

            item {
                SettingsSection(title = stringResource(R.string.settings_section_privacy)) {
                    SettingsItem(
                        icon = Icons.Default.Security,
                        title = stringResource(R.string.settings_biometric_lock),
                        trailing = {
                            Switch(
                                checked = profile.biometricEnabled,
                                onCheckedChange = {
                                    viewModel.updateProfile(profile.copy(biometricEnabled = it))
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MawaaiColors.RoseGold,
                                    checkedTrackColor = MawaaiColors.DeepRose
                                )
                            )
                        }
                    )
                }
            }

            item {
                SettingsSection(title = stringResource(R.string.settings_section_theme)) {
                    ThemePickerRow(
                        current = profile.themeMode,
                        onSelect = { mode ->
                            viewModel.updateProfile(profile.copy(themeMode = mode))
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "الإصدار 1.0.0",
                        color = MawaaiColors.TextHint,
                        fontFamily = CairoFamily,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }

    if (showPartnerNameDialog) {
        PartnerNameDialog(
            current = profile.partnerName,
            onDismiss = { showPartnerNameDialog = false },
            onConfirm = { newName ->
                viewModel.updateProfile(profile.copy(partnerName = newName))
                showPartnerNameDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PartnerNameDialog(
    current: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by remember(current) { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.settings_partner_name),
                fontFamily = CairoFamily,
                color = MawaaiColors.RoseGold
            )
        },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = CairoFamily),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = MawaaiColors.RoseGold,
                    unfocusedBorderColor = MawaaiColors.RoseGoldDim,
                    cursorColor = MawaaiColors.RoseGold,
                    focusedTextColor = MawaaiColors.TextPrimary,
                    unfocusedTextColor = MawaaiColors.TextPrimary
                )
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(value.trim().ifEmpty { current }) },
                enabled = value.trim().isNotEmpty()
            ) {
                Text(stringResource(R.string.action_save), fontFamily = CairoFamily, color = MawaaiColors.RoseGold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel), fontFamily = CairoFamily, color = MawaaiColors.TextSecondary)
            }
        },
        containerColor = MawaaiColors.SurfaceDark
    )
}

@Composable
private fun ThemePickerRow(
    current: BackgroundTheme,
    onSelect: (BackgroundTheme) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ThemeChip(
            label = stringResource(R.string.settings_theme_auto),
            icon = Icons.Default.Palette,
            selected = current == BackgroundTheme.AUTO,
            onClick = { onSelect(BackgroundTheme.AUTO) },
            modifier = Modifier.weight(1f)
        )
        ThemeChip(
            label = stringResource(R.string.settings_theme_morning),
            icon = Icons.Default.WbSunny,
            selected = current == BackgroundTheme.MORNING,
            onClick = { onSelect(BackgroundTheme.MORNING) },
            modifier = Modifier.weight(1f)
        )
        ThemeChip(
            label = stringResource(R.string.settings_theme_night),
            icon = Icons.Default.NightsStay,
            selected = current == BackgroundTheme.NIGHT,
            onClick = { onSelect(BackgroundTheme.NIGHT) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ThemeChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (selected) MawaaiColors.RoseGold.copy(alpha = 0.22f) else MawaaiColors.CardDark
    val border = if (selected) MawaaiColors.RoseGold else MawaaiColors.RoseGoldDim.copy(alpha = 0.35f)
    Column(
        modifier = modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
            .background(bg)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = border,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) MawaaiColors.RoseGold else MawaaiColors.TextSecondary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = if (selected) MawaaiColors.RoseGold else MawaaiColors.TextSecondary,
            fontFamily = CairoFamily,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = title,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            color = MawaaiColors.RoseGold,
            fontFamily = CairoFamily,
            style = MaterialTheme.typography.titleMedium
        )
        // Phase 18: translucent card so the morning/night ThemedBackground
        // photo bleeds through the section. Pre-Phase-18 used opaque
        // `MawaaiColors.SurfaceDark` which made the entire Settings screen
        // look like a flat dark slab regardless of the active theme — the
        // user reported "Settings has solid color, no background visible".
        // 0.55 alpha keeps the section readable while showing enough of
        // the photo for the morning/night swap to feel present here.
        Surface(
            color = MawaaiColors.SurfaceDark.copy(alpha = 0.55f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(content = content)
        }
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MawaaiColors.CardDark),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MawaaiColors.RoseGold)
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = MawaaiColors.TextPrimary,
                fontFamily = CairoFamily,
                style = MaterialTheme.typography.bodyLarge
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = MawaaiColors.TextSecondary,
                    fontFamily = CairoFamily,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        if (trailing != null) {
            trailing()
        } else if (onClick != null) {
            Icon(
                Icons.Default.ChevronLeft,
                contentDescription = null,
                tint = MawaaiColors.TextHint
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1209)
@Composable
private fun SettingsScreenPreview() {
    SettingsScreen(onBack = {}, onNavigateToAiProviders = {})
}