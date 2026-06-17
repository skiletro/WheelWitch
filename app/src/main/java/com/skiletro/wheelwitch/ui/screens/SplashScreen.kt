package com.skiletro.wheelwitch.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.skiletro.wheelwitch.service.PackStatus
import com.skiletro.wheelwitch.viewmodel.SaveState
import com.skiletro.wheelwitch.viewmodel.UiState
import com.skiletro.wheelwitch.viewmodel.UpdateViewModel
import kotlinx.coroutines.delay

private const val APP_NAME = "Wheel Witch"
private const val PACK_NAME = "Retro Rewind Pack"
private const val SUBTITLE = "$PACK_NAME Manager"

private val cardShape = RoundedCornerShape(16.dp)
private val buttonShape = RoundedCornerShape(12.dp)

@Composable
fun SplashScreen(
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

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = APP_NAME,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = SUBTITLE,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(32.dp))

            if (successMessage != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = cardShape,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        text = successMessage!!,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            when (val currentState = state) {
                is UiState.NoStorage -> NoStorageContent(onPickStorage)
                is UiState.Checking -> CheckingContent()
                is UiState.Ready -> {
                    val configuration = LocalConfiguration.current
                    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                    if (isLandscape) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
                        ) {
                            ReadyContent(currentState.status, viewModel, Modifier.weight(1f))
                            SaveBackupCard(Modifier.weight(1f), saveState, onBackupSave, onRestoreSave, { showDeleteConfirm = true })
                        }
                    } else {
                        ReadyContent(currentState.status, viewModel)
                        Spacer(modifier = Modifier.height(12.dp))
                        SaveBackupCard(saveState = saveState, onBackup = onBackupSave, onRestore = onRestoreSave, onDelete = { showDeleteConfirm = true })
                    }
                }
                is UiState.Downloading -> ProgressCard(
                    progress = currentState.progress,
                    message = currentState.message
                )
                is UiState.Extracting -> ProgressCard(
                    progress = currentState.progress,
                    message = "Extracting files..."
                )
                is UiState.ApplyingUpdate -> ProgressCard(
                    progress = currentState.progress,
                    message = "Applying update ${currentState.index}/${currentState.total}: ${currentState.description}"
                )
                is UiState.ReadyToLaunch -> {
                    val configuration = LocalConfiguration.current
                    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                    val mainContent = ReadyToLaunchContent(
                        version = currentState.version,
                        onLaunch = { viewModel.launchDolphin() },
                        onCheckAgain = { viewModel.checkStatus() }
                    )
                    if (isLandscape) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
                        ) {
                            mainContent
                            SaveBackupCard(Modifier.weight(1f), saveState, onBackupSave, onRestoreSave, { showDeleteConfirm = true })
                        }
                    } else {
                        mainContent
                        Spacer(modifier = Modifier.height(12.dp))
                        SaveBackupCard(saveState = saveState, onBackup = onBackupSave, onRestore = onRestoreSave, onDelete = { showDeleteConfirm = true })
                    }
                }
                is UiState.Error -> ErrorContent(
                    message = currentState.message,
                    onRetry = { viewModel.clearError() },
                    onPickIso = onPickIso
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ContentCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = cardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            content()
        }
    }
}

@Composable
private fun ActionButton(
    text: String,
    onClick: () -> Unit,
    isPrimary: Boolean = true
) {
    if (isPrimary) {
        Button(onClick = onClick, shape = buttonShape, modifier = Modifier.height(48.dp)) {
            Text(text)
        }
    } else {
        OutlinedButton(onClick = onClick, shape = buttonShape, modifier = Modifier.height(48.dp)) {
            Text(text)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NoStorageContent(onPickStorage: () -> Unit = {}) {
    ContentCard {
        SectionTitle("Welcome!")
        Spacer(modifier = Modifier.height(8.dp))
        BodyText("Choose where to store the $PACK_NAME files. Pick a folder that Dolphin Emulator can access, or anywhere you prefer.")
        Spacer(modifier = Modifier.height(20.dp))
        ActionButton(text = "Select Storage Folder", onClick = onPickStorage)
    }
}

@Preview(showBackground = true)
@Composable
private fun CheckingContent() {
    ContentCard {
        CircularProgressIndicator(
            modifier = Modifier.size(32.dp),
            strokeWidth = 3.dp,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
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

    ContentCard(modifier = modifier) {
        SectionTitle(title)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(20.dp))

        ButtonRow {
            OutlinedButton(
                onClick = { viewModel.checkStatus() },
                shape = buttonShape,
                modifier = Modifier.height(48.dp)
            ) { Text("Check Again") }

            if (status is PackStatus.UpToDate || status is PackStatus.Installed) {
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = { viewModel.launchDolphin() },
                    shape = buttonShape,
                    modifier = Modifier.height(48.dp)
                ) { Text("Launch Dolphin") }
            }

            if (actionText.isNotEmpty()) {
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = { viewModel.downloadOrUpdate(status) },
                    shape = buttonShape,
                    modifier = Modifier.height(48.dp)
                ) { Text(actionText) }
            }
        }
    }
}

@Composable
private fun ButtonRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Preview(showBackground = true)
@Composable
private fun ProgressCard(progress: Float = -1f, message: String = "Downloading...") {
    ContentCard {
        BodyText(message)
        Spacer(modifier = Modifier.height(16.dp))
        if (progress >= 0f) {
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        } else {
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
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
    onCheckAgain: () -> Unit = {}
) {
    ContentCard {
        SectionTitle("Ready!", color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(4.dp))
        BodyText("$PACK_NAME v$version is ready to play.")
        Spacer(modifier = Modifier.height(20.dp))
        ButtonRow {
            Button(
                onClick = onLaunch,
                shape = buttonShape,
                modifier = Modifier.height(48.dp)
            ) { Text("Launch Dolphin") }
            Spacer(modifier = Modifier.width(12.dp))
            OutlinedButton(
                onClick = onCheckAgain,
                shape = buttonShape,
                modifier = Modifier.height(48.dp)
            ) { Text("Check Again") }
        }
    }
}

@Composable
private fun SaveBackupCard(
    modifier: Modifier = Modifier,
    saveState: SaveState,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    ContentCard(modifier = modifier) {
        SectionTitle("Save Data", color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))
        BodyText(
            if (saveState.hasSave) "Save file found" else "No save file found"
        )
        Spacer(modifier = Modifier.height(16.dp))
        ButtonRow {
            Button(
                onClick = onBackup,
                enabled = saveState.hasSave,
                shape = buttonShape,
                modifier = Modifier.height(48.dp)
            ) { Text("Backup") }
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = onRestore,
                shape = buttonShape,
                modifier = Modifier.height(48.dp)
            ) { Text("Restore") }
        }
        if (saveState.hasSave) {
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onDelete) {
                Text("Delete save data", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit, onPickIso: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = cardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
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
            Spacer(modifier = Modifier.height(20.dp))
            if (message.contains("ROM file", ignoreCase = true)) {
                OutlinedButton(
                    onClick = onPickIso,
                    shape = buttonShape,
                    modifier = Modifier.height(48.dp)
                ) { Text("Select Mario Kart Wii ROM") }
            } else {
                Button(
                    onClick = onRetry,
                    shape = buttonShape,
                    modifier = Modifier.height(48.dp)
                ) { Text("Try Again") }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String, color: Color = MaterialTheme.colorScheme.onSurface) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
        color = color
    )
}

@Composable
private fun BodyText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Preview(showBackground = true)
@Composable
private fun PreviewReadyContent() {
    ContentCard {
        SectionTitle(PACK_NAME)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "v3.2.6 (latest is v3.2.6)",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(20.dp))
        ButtonRow {
            OutlinedButton(
                onClick = {},
                shape = buttonShape,
                modifier = Modifier.height(48.dp)
            ) { Text("Check Again") }
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = {},
                shape = buttonShape,
                modifier = Modifier.height(48.dp)
            ) { Text("Launch Dolphin") }
        }
    }
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
