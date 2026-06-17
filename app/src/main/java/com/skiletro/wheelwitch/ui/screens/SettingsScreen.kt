package com.skiletro.wheelwitch.ui.screens

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.automirrored.filled.Shortcut
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.skiletro.wheelwitch.ui.components.cacheSize
import com.skiletro.wheelwitch.ui.components.formatBytes
import com.skiletro.wheelwitch.ui.theme.ThemeMode
import com.skiletro.wheelwitch.viewmodel.UpdateViewModel

@Composable
fun SettingsScreen(
    viewModel: UpdateViewModel,
    onBackupSave: () -> Unit,
    onRestoreSave: () -> Unit,
    onDeleteSave: () -> Unit,
    onClose: () -> Unit,
    useDynamicColor: Boolean,
    onToggleDynamicColor: (Boolean) -> Unit,
    themeMode: ThemeMode,
    onChangeThemeMode: (ThemeMode) -> Unit,
    onPickIso: () -> Unit,
    onSimulateQuickLaunch: () -> Unit,
    onRelaunchOnboarding: () -> Unit
) {
    val saveState by viewModel.saveState.collectAsState()
    val miiMakerState by viewModel.miiMakerState.collectAsState()
    val isInstallingWad by viewModel.isInstallingWad.collectAsState()
    val miiMakerError by viewModel.miiMakerError.collectAsState()
    val isoPath by viewModel.currentIsoPath.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showWadDeleteConfirm by remember { mutableStateOf(false) }
    var showThemeDropdown by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Save Data") },
            text = { Text("Are you sure you want to delete the save file? This cannot be undone.") },
            confirmButton = {
                Button(onClick = { onDeleteSave(); showDeleteConfirm = false }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showWadDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showWadDeleteConfirm = false },
            title = { Text("Delete Mii Channel WAD") },
            text = { Text("Delete the cached Mii Channel WAD file? It will be re-downloaded the next time you use Mii Maker.") },
            confirmButton = {
                Button(onClick = { viewModel.deleteWad(); showWadDeleteConfirm = false }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showWadDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Save Data
            item {
                SettingsCategoryHeader("Save Data")
            }
            item {
                SettingsItem(
                    icon = Icons.Filled.SaveAlt,
                    title = "Backup",
                    summary = if (saveState.hasSave) "Save file found" else "No save file found",
                    trailing = {
                        Button(
                            onClick = onBackupSave,
                            enabled = saveState.hasSave,
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = ButtonDefaults.TextButtonContentPadding,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) { Text("Backup", fontWeight = FontWeight.Medium) }
                    }
                )
                SettingsItem(
                    icon = Icons.Filled.Restore,
                    title = "Restore",
                    summary = "Replace current save with a backup",
                    trailing = {
                        Button(
                            onClick = onRestoreSave,
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = ButtonDefaults.TextButtonContentPadding,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) { Text("Restore", fontWeight = FontWeight.Medium) }
                    }
                )
                if (saveState.hasSave) {
                    SettingsItem(
                        icon = Icons.Filled.Delete,
                        title = "Delete save data",
                        titleColor = MaterialTheme.colorScheme.error,
                        summary = "This cannot be undone",
                        trailing = {
                            TextButton(onClick = { showDeleteConfirm = true }) {
                                Text("Delete", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    )
                }
            }

            // Appearance
            item {
                SettingsCategoryHeader("Appearance")
            }
            item {
                SettingsItem(
                    icon = Icons.Filled.Palette,
                    title = "Dynamic color",
                    summary = if (useDynamicColor) "Using wallpaper colors" else "Using custom theme color",
                    trailing = {
                        Switch(checked = useDynamicColor, onCheckedChange = onToggleDynamicColor)
                    }
                )
                SettingsItem(
                    icon = Icons.Filled.Nightlight,
                    title = "Dark mode",
                    summary = when (themeMode) {
                        ThemeMode.Light -> "Always light"
                        ThemeMode.Dark -> "Always dark"
                        ThemeMode.System -> "Follow system"
                    },
                    trailing = {
                        Box {
                            TextButton(
                                onClick = { showThemeDropdown = true },
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text(
                                    text = when (themeMode) {
                                        ThemeMode.Light -> "Light"
                                        ThemeMode.Dark -> "Dark"
                                        ThemeMode.System -> "System"
                                    }
                                )
                            }
                            DropdownMenu(
                                expanded = showThemeDropdown,
                                onDismissRequest = { showThemeDropdown = false }
                            ) {
                                ThemeMode.entries.forEach { mode ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                when (mode) {
                                                    ThemeMode.Light -> "Light"
                                                    ThemeMode.Dark -> "Dark"
                                                    ThemeMode.System -> "System"
                                                }
                                            )
                                        },
                                        onClick = {
                                            onChangeThemeMode(mode)
                                            showThemeDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                )
            }

            // Retro Rewind
            item {
                SettingsCategoryHeader("Retro Rewind")
            }
            item {
                val fileName = isoPath?.substringAfterLast('/')?.ifBlank { null }
                SettingsItem(
                    icon = Icons.Filled.Gamepad,
                    title = "Mario Kart Wii",
                    summary = fileName ?: "ROM not selected",
                    trailing = {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(
                                onClick = onPickIso,
                                shape = RoundedCornerShape(14.dp)
                            ) { Text("Pick") }
                            if (fileName != null) {
                                TextButton(
                                    onClick = { viewModel.clearIsoPath() },
                                    shape = RoundedCornerShape(14.dp)
                                ) { Text("Clear") }
                            }
                        }
                    }
                )
                SettingsItem(
                    icon = Icons.Filled.Dns,
                    title = "Pack storage",
                    summary = viewModel.storageRootPath ?: "Not configured",
                    trailing = null
                )
            }

            // Mii Maker
            item {
                SettingsCategoryHeader("Mii Maker")
            }
            item {
                val wadStatus = if (miiMakerState.hasWad) "Installed" else "Not installed"
                SettingsItem(
                    icon = Icons.Filled.Checkroom,
                    title = "Mii Channel WAD",
                    summary = if (miiMakerError != null) miiMakerError else wadStatus,
                    summaryColor = if (miiMakerError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    trailing = {
                        if (isInstallingWad) {
                            Text("Installing...", style = MaterialTheme.typography.bodySmall)
                        } else if (miiMakerState.hasWad) {
                            TextButton(onClick = { showWadDeleteConfirm = true }) {
                                Text("Delete", color = MaterialTheme.colorScheme.error)
                            }
                        } else {
                            Button(
                                onClick = { viewModel.installMiiMakerWad() },
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = ButtonDefaults.TextButtonContentPadding
                            ) { Text("Install") }
                        }
                    }
                )
            }

            // Advanced
            item {
                SettingsCategoryHeader("Advanced")
            }
            item {
                val cacheDir = remember { java.io.File(context.cacheDir, "rewind_pack_downloads") }
                var cacheSizeBytes by remember { mutableStateOf(cacheSize(cacheDir)) }
                SettingsItem(
                    icon = Icons.Filled.Cached,
                    title = "Download cache",
                    summary = formatBytes(cacheSizeBytes),
                    trailing = {
                        TextButton(
                            onClick = {
                                cacheDir.deleteRecursively()
                                cacheSizeBytes = 0
                            },
                            enabled = cacheSizeBytes > 0,
                            shape = RoundedCornerShape(14.dp)
                        ) { Text("Clear") }
                    }
                )
            }
            item {
                var miiCacheSizeBytes by remember {
                    mutableStateOf(com.skiletro.wheelwitch.util.MiiFaceCache.cacheSize())
                }
                SettingsItem(
                    icon = Icons.Filled.Cached,
                    title = "Mii face cache",
                    summary = formatBytes(miiCacheSizeBytes),
                    trailing = {
                        TextButton(
                            onClick = {
                                com.skiletro.wheelwitch.util.MiiFaceCache.clear()
                                miiCacheSizeBytes = 0
                            },
                            enabled = miiCacheSizeBytes > 0,
                            shape = RoundedCornerShape(14.dp)
                        ) { Text("Clear") }
                    }
                )
            }
            item {
                SettingsItem(
                    icon = Icons.AutoMirrored.Filled.Shortcut,
                    title = "Quick launch",
                    summary = "Launch Retro Rewind directly from a home screen shortcut",
                    trailing = {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(
                                onClick = onSimulateQuickLaunch,
                                shape = RoundedCornerShape(14.dp)
                            ) { Text("Simulate") }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                val shortcutManager = remember { context.getSystemService(Context.SHORTCUT_SERVICE) as? ShortcutManager }
                                if (shortcutManager?.isRequestPinShortcutSupported == true) {
                                    TextButton(
                                        onClick = {
                                            val intent = Intent("com.skiletro.wheelwitch.action.QUICK_LAUNCH").apply {
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                                `package` = context.packageName
                                            }
                                            val shortcut = ShortcutInfo.Builder(context, "quick_launch")
                                                .setShortLabel("Quick Launch")
                                                .setLongLabel("Launch Retro Rewind")
                                                .setIcon(Icon.createWithResource(context, com.skiletro.wheelwitch.R.mipmap.ic_launcher))
                                                .setIntent(intent)
                                                .build()
                                            shortcutManager.requestPinShortcut(shortcut, null)
                                        },
                                        shape = RoundedCornerShape(14.dp)
                                    ) { Text("Shortcut") }
                                }
                            }
                        }
                    }
                )
            }
            item {
                SettingsItem(
                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                    title = "Onboarding",
                    summary = "Re-run the initial setup wizard",
                    trailing = {
                        TextButton(
                            onClick = onRelaunchOnboarding,
                            shape = RoundedCornerShape(14.dp)
                        ) { Text("Relaunch") }
                    }
                )
            }

            // About
            item {
                SettingsCategoryHeader("About")
            }
            item {
                val version = if (com.skiletro.wheelwitch.BuildConfig.DEBUG)
                    "v${com.skiletro.wheelwitch.BuildConfig.VERSION_NAME}-debug-${com.skiletro.wheelwitch.BuildConfig.GIT_HASH}"
                else
                    "v${com.skiletro.wheelwitch.BuildConfig.VERSION_NAME}"
                SettingsItem(
                    icon = Icons.Filled.Info,
                    title = "Wheel Witch",
                    summary = "$version \u2014 Manages Retro Rewind Pack for MKWii",
                    trailing = null
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SettingsCategoryHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    summary: String? = null,
    iconTint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant,
    titleColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    summaryColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (summary != null) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = summaryColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(12.dp))
            trailing()
        }
    }
}