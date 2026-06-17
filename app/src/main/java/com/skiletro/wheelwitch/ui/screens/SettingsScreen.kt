package com.skiletro.wheelwitch.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.skiletro.wheelwitch.viewmodel.MiiMakerState
import com.skiletro.wheelwitch.viewmodel.SaveState
import com.skiletro.wheelwitch.viewmodel.UpdateViewModel

private val buttonShape = RoundedCornerShape(14.dp)

@Composable
fun SettingsScreen(
    viewModel: UpdateViewModel,
    onBackupSave: () -> Unit,
    onRestoreSave: () -> Unit,
    onDeleteSave: () -> Unit,
    onClose: () -> Unit
) {
    val saveState by viewModel.saveState.collectAsState()
    val miiMakerState by viewModel.miiMakerState.collectAsState()
    val isInstallingWad by viewModel.isInstallingWad.collectAsState()
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

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            MiiMakerSection(
                miiMakerState = miiMakerState,
                isInstallingWad = isInstallingWad,
                onInstallWad = { viewModel.installMiiMakerWad() },
                onDeleteWad = { showWadDeleteConfirm = true }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SaveDataSection(
    saveState: SaveState,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    Text(
        text = "Save Data",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = if (saveState.hasSave) "Save file found" else "No save file found",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(16.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        var backupFocused by remember { mutableStateOf(false) }
        Button(
            onClick = onBackup,
            enabled = saveState.hasSave,
            shape = buttonShape,
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .onFocusChanged { backupFocused = it.isFocused }
                .then(
                    if (backupFocused) Modifier.border(
                        width = 3.dp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        shape = buttonShape
                    ) else Modifier
                ),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        ) {
            Text(
                text = "Backup",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
        var restoreFocused by remember { mutableStateOf(false) }
        Button(
            onClick = onRestore,
            shape = buttonShape,
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .onFocusChanged { restoreFocused = it.isFocused }
                .then(
                    if (restoreFocused) Modifier.border(
                        width = 3.dp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        shape = buttonShape
                    ) else Modifier
                ),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        ) {
            Text(
                text = "Restore",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
    if (saveState.hasSave) {
        Spacer(modifier = Modifier.height(8.dp))
        var deleteFocused by remember { mutableStateOf(false) }
        TextButton(
            onClick = onDelete,
            modifier = Modifier
                .onFocusChanged { deleteFocused = it.isFocused }
                .then(
                    if (deleteFocused) Modifier.border(
                        width = 3.dp,
                        color = MaterialTheme.colorScheme.error,
                        shape = RoundedCornerShape(8.dp)
                    ) else Modifier
                )
        ) {
            Text(
                "Delete save data",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun MiiMakerSection(
    miiMakerState: MiiMakerState,
    isInstallingWad: Boolean,
    onInstallWad: () -> Unit,
    onDeleteWad: () -> Unit
) {
    Text(
        text = "Mii Channel WAD",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(modifier = Modifier.height(4.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "Status: ",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = if (miiMakerState.hasWad) "Installed" else "Not installed",
            style = MaterialTheme.typography.bodyMedium,
            color = if (miiMakerState.hasWad) Color(0xFF66BB6A) else MaterialTheme.colorScheme.error
        )
    }
    Spacer(modifier = Modifier.height(16.dp))
    if (isInstallingWad) {
        LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary
        )
    } else if (miiMakerState.hasWad) {
        var deleteFocused by remember { mutableStateOf(false) }
        Button(
            onClick = onDeleteWad,
            shape = buttonShape,
            modifier = Modifier
                .onFocusChanged { deleteFocused = it.isFocused }
                .then(
                    if (deleteFocused) Modifier.border(
                        width = 3.dp,
                        color = MaterialTheme.colorScheme.onError,
                        shape = buttonShape
                    ) else Modifier
                ),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            )
        ) {
            Text(
                "Delete cached WAD",
                fontWeight = FontWeight.Medium
            )
        }
    } else {
        var installFocused by remember { mutableStateOf(false) }
        Button(
            onClick = onInstallWad,
            shape = buttonShape,
            modifier = Modifier
                .height(56.dp)
                .fillMaxWidth()
                .onFocusChanged { installFocused = it.isFocused }
                .then(
                    if (installFocused) Modifier.border(
                        width = 3.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                        shape = buttonShape
                    ) else Modifier
                ),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(
                "Install Mii Channel",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
