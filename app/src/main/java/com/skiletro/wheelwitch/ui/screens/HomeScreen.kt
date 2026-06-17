package com.skiletro.wheelwitch.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.skiletro.wheelwitch.model.PackStatus
import com.skiletro.wheelwitch.viewmodel.SaveState
import com.skiletro.wheelwitch.viewmodel.UiState
import com.skiletro.wheelwitch.viewmodel.UpdateViewModel
import kotlinx.coroutines.delay

private const val APP_NAME = "Wheel Witch"
private const val PACK_NAME = "Retro Rewind Pack"
private const val SUBTITLE = "$PACK_NAME Manager"

private val sectionShape = RoundedCornerShape(20.dp)
private val buttonShape = RoundedCornerShape(14.dp)

@Composable
fun HomeScreen(
    viewModel: UpdateViewModel,
    onPickStorage: () -> Unit,
    onPickIso: () -> Unit,
    onBackupSave: () -> Unit,
    onRestoreSave: () -> Unit,
    onDeleteSave: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val saveState by viewModel.saveState.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(successMessage) {
        if (successMessage != null) {
            delay(3000)
            viewModel.dismissSuccess()
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Save Data") },
            text = { Text("Are you sure you want to delete the save file? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteSave()
                        showDeleteConfirm = false
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopBar()

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(bottom = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (successMessage != null) {
                    SuccessBanner(successMessage!!)
                }

                when (val currentState = state) {
                    is UiState.NoStorage -> NoStorageContent(onPickStorage)
                    is UiState.Checking -> CheckingContent()
                    is UiState.Ready -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(
                                20.dp,
                                Alignment.CenterHorizontally
                            )
                        ) {
                            ReadyContent(
                                currentState.status,
                                viewModel,
                                Modifier.weight(1f)
                            )
                            SaveBackupContent(
                                Modifier.weight(1f),
                                saveState,
                                onBackupSave,
                                onRestoreSave,
                                { showDeleteConfirm = true }
                            )
                        }
                    }
                    is UiState.Downloading -> ProgressContent(
                        currentState.progress,
                        currentState.message
                    )
                    is UiState.Extracting -> ProgressContent(
                        currentState.progress,
                        "Extracting files..."
                    )
                    is UiState.ApplyingUpdate -> ProgressContent(
                        currentState.progress,
                        "Applying update ${currentState.index}/${currentState.total}: ${currentState.description}"
                    )
                    is UiState.ReadyToLaunch -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(
                                20.dp,
                                Alignment.CenterHorizontally
                            )
                        ) {
                            ReadyToLaunchContent(
                                version = currentState.version,
                                onLaunch = { viewModel.launchDolphin() },
                                onCheckAgain = { viewModel.checkStatus() },
                                modifier = Modifier.weight(1f)
                            )
                            SaveBackupContent(
                                Modifier.weight(1f),
                                saveState,
                                onBackupSave,
                                onRestoreSave,
                                { showDeleteConfirm = true }
                            )
                        }
                    }
                    is UiState.Error -> ErrorContent(
                        currentState.message,
                        onRetry = { viewModel.clearError() },
                        onPickIso = onPickIso
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun TopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = APP_NAME,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = SUBTITLE,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SuccessBanner(message: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        shape = sectionShape,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun ContentSection(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = sectionShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content
        )
    }
}

@Composable
private fun PrimaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = buttonShape,
        modifier = modifier.height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SecondaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = buttonShape,
        modifier = modifier.height(48.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun TonalActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = buttonShape,
        modifier = modifier.height(48.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NoStorageContent(onPickStorage: () -> Unit = {}) {
    ContentSection {
        SectionTitle("Welcome!")
        Spacer(modifier = Modifier.height(8.dp))
        BodyText(
            "Choose where to store the $PACK_NAME files. " +
                    "Pick a folder that Dolphin Emulator can access, or anywhere you prefer."
        )
        Spacer(modifier = Modifier.height(24.dp))
        PrimaryActionButton(
            text = "Select Storage Folder",
            onClick = onPickStorage,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CheckingContent() {
    ContentSection {
        CircularProgressIndicator(
            modifier = Modifier.size(36.dp),
            strokeWidth = 3.dp,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(20.dp))
        BodyText("Checking for updates...")
    }
}

@Composable
private fun ReadyContent(
    status: PackStatus,
    viewModel: UpdateViewModel,
    modifier: Modifier = Modifier
) {
    val (title, subtitle, actionText) = when (status) {
        is PackStatus.NotInstalled -> Triple(
            PACK_NAME, "Not installed", "Download & Install"
        )
        is PackStatus.Installed -> Triple(
            PACK_NAME, "Version ${status.version} (offline)", ""
        )
        is PackStatus.UpdateAvailable -> Triple(
            "Update Available",
            "${status.currentVersion} \u2192 ${status.latestVersion}",
            "Update to v${status.latestVersion}"
        )
        is PackStatus.UpToDate -> Triple(
            PACK_NAME,
            "v${status.currentVersion} (latest is v${status.latestVersion})",
            ""
        )
    }

    ContentSection(modifier = modifier) {
        SectionTitle(title)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))

        if (actionText.isNotEmpty()) {
            PrimaryActionButton(
                text = actionText,
                onClick = { viewModel.downloadOrUpdate(status) },
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            PrimaryActionButton(
                text = "Launch Dolphin",
                onClick = { viewModel.launchDolphin() },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        SecondaryActionButton(
            text = "Check Again",
            onClick = { viewModel.checkStatus() }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ProgressContent(
    progress: Float = -1f,
    message: String = "Downloading..."
) {
    ContentSection {
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(20.dp))
        if (progress >= 0f) {
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surface,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        } else {
            CircularProgressIndicator(
                modifier = Modifier.size(36.dp),
                strokeWidth = 3.dp,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ReadyToLaunchContent(
    version: String = "3.2.6",
    onLaunch: () -> Unit = {},
    onCheckAgain: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    ContentSection(modifier = modifier) {
        SectionTitle("Ready to play!", color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(6.dp))
        BodyText("$PACK_NAME v$version")
        Spacer(modifier = Modifier.height(24.dp))
        PrimaryActionButton(
            text = "Launch Dolphin",
            onClick = onLaunch,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        SecondaryActionButton(
            text = "Check Again",
            onClick = onCheckAgain
        )
    }
}

@Composable
private fun SaveBackupContent(
    modifier: Modifier = Modifier,
    saveState: SaveState,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    ContentSection(modifier = modifier) {
        SectionTitle("Save Data")
        Spacer(modifier = Modifier.height(8.dp))
        BodyText(
            if (saveState.hasSave) "Save file found" else "No save file found"
        )
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TonalActionButton(
                text = "Backup",
                onClick = onBackup,
                enabled = saveState.hasSave,
                modifier = Modifier.weight(1f)
            )
            TonalActionButton(
                text = "Restore",
                onClick = onRestore,
                modifier = Modifier.weight(1f)
            )
        }
        if (saveState.hasSave) {
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(onClick = onDelete) {
                Text(
                    "Delete save data",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    onPickIso: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = sectionShape,
        color = MaterialTheme.colorScheme.errorContainer
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SectionTitle("Error", color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(24.dp))
            if (message.contains("ROM file", ignoreCase = true)) {
                PrimaryActionButton(
                    text = "Select Mario Kart Wii ROM",
                    onClick = onPickIso
                )
            } else {
                PrimaryActionButton(
                    text = "Try Again",
                    onClick = onRetry
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String, color: Color = MaterialTheme.colorScheme.onSurface) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = color
    )
}

@Composable
private fun BodyText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Preview(showBackground = true)
@Composable
private fun PreviewReadyContent() {
    ContentSection {
        SectionTitle(PACK_NAME)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "v3.2.6 (latest is v3.2.6)",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        PrimaryActionButton(
            text = "Launch Dolphin",
            onClick = {},
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        SecondaryActionButton(
            text = "Check Again",
            onClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewSaveBackupContent() {
    SaveBackupContent(
        saveState = SaveState(hasSave = true),
        onBackup = {},
        onRestore = {},
        onDelete = {}
    )
}

@Preview(showBackground = true)
@Composable
private fun PreviewErrorContent() {
    ErrorContent(
        message = "Please select your Mario Kart Wii ROM file first.",
        onRetry = {},
        onPickIso = {}
    )
}
