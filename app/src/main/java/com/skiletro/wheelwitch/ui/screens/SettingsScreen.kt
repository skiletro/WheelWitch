package com.skiletro.wheelwitch.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.platform.LocalContext
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
    onSimulateQuickLaunch: () -> Unit
) {
    val saveState by viewModel.saveState.collectAsState()
    val miiMakerState by viewModel.miiMakerState.collectAsState()
    val isInstallingWad by viewModel.isInstallingWad.collectAsState()
    val miiMakerError by viewModel.miiMakerError.collectAsState()
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showWadDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Save Data") },
            text = {
                Text("Are you sure you want to delete the save file? This cannot be undone.")
            },
            confirmButton = {
                Button(onClick = {
                    onDeleteSave()
                    showDeleteConfirm = false
                }) { Text("Delete") }
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
            text = {
                Text("Delete the cached Mii Channel WAD file? It will be re-downloaded the next time you use Mii Maker.")
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.deleteWad()
                    showWadDeleteConfirm = false
                }) { Text("Delete") }
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            SaveDataSection(
                saveState = saveState,
                onBackup = onBackupSave,
                onRestore = onRestoreSave,
                onDelete = { showDeleteConfirm = true }
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            MiiMakerSection(
                miiMakerState = miiMakerState,
                isInstallingWad = isInstallingWad,
                miiMakerError = miiMakerError,
                onInstallWad = { viewModel.installMiiMakerWad() },
                onDeleteWad = { showWadDeleteConfirm = true }
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            ThemeSection(
                useDynamicColor = useDynamicColor,
                onToggleDynamicColor = onToggleDynamicColor,
                themeMode = themeMode,
                onChangeThemeMode = onChangeThemeMode
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            IsoSection(viewModel = viewModel, onPickIso = onPickIso)

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            CacheSection()

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            StorageSection(storageRootPath = viewModel.storageRootPath)

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            AboutSection()

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            QuickLaunchSection(onSimulate = onSimulateQuickLaunch)

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun QuickLaunchSection(onSimulate: () -> Unit) {
    val context = LocalContext.current

    Text(
        text = "Quick Launch",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = "Test the quick-launch intent flow from a game launcher.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(
            onClick = onSimulate,
            shape = buttonShape,
            modifier = Modifier.height(48.dp).weight(1f)
        ) {
            Text("Simulate Quick Launch", fontWeight = FontWeight.Medium)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val shortcutManager = remember { context.getSystemService(ShortcutManager::class.java) }
            val canPin = remember { shortcutManager?.isRequestPinShortcutSupported == true }
            if (canPin) {
                OutlinedButton(
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
                            shortcutManager?.requestPinShortcut(shortcut, null)
                    },
                    shape = buttonShape,
                    modifier = Modifier.height(48.dp).weight(1f)
                ) {
                    Text("Add Shortcut", fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
