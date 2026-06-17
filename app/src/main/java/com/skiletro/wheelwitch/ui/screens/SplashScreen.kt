package com.skiletro.wheelwitch.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.skiletro.wheelwitch.service.PackStatus
import com.skiletro.wheelwitch.viewmodel.UiState
import com.skiletro.wheelwitch.viewmodel.UpdateViewModel

@Composable
fun SplashScreen(
    viewModel: UpdateViewModel,
    onPickStorage: () -> Unit,
    onPickIso: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "WheelWitch",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Retro Rewind Pack Manager",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        when (val currentState = state) {
            is UiState.NoStorage -> NoStorageContent(onPickStorage)
            is UiState.Checking -> CheckingContent()
            is UiState.Ready -> ReadyContent(currentState.status, viewModel)
            is UiState.Downloading -> ProgressContent(
                progress = currentState.progress,
                message = currentState.message
            )
            is UiState.Extracting -> ProgressContent(
                progress = currentState.progress,
                message = "Extracting files..."
            )
            is UiState.ApplyingUpdate -> ProgressContent(
                progress = currentState.progress,
                message = "Applying update ${currentState.index}/${currentState.total}: ${currentState.description}"
            )
            is UiState.ReadyToLaunch -> ReadyToLaunchContent(
                version = currentState.version,
                onLaunch = { viewModel.launchDolphin() },
                onCheckAgain = { viewModel.checkStatus() }
            )
            is UiState.Error -> ErrorContent(
                message = currentState.message,
                onRetry = { viewModel.clearError() },
                onPickIso = onPickIso
            )
        }
    }
}

@Composable
private fun NoStorageContent(onPickStorage: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Welcome!",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Choose where to store the Retro Rewind pack files. " +
                        "Pick a folder that Dolphin Emulator can access, " +
                        "or anywhere you prefer.",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onPickStorage) {
                Text("Select Storage Folder")
            }
        }
    }
}

@Composable
private fun CheckingContent() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Checking for updates...",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ReadyContent(
    status: PackStatus,
    viewModel: UpdateViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val (title, subtitle, actionText) = when (status) {
                is PackStatus.NotInstalled -> Triple(
                    "Retro Rewind Pack",
                    "Not installed",
                    "Download & Install"
                )
                is PackStatus.Installed -> Triple(
                    "Retro Rewind Pack",
                    "Version ${status.version} (offline)",
                    ""
                )
                is PackStatus.UpdateAvailable -> Triple(
                    "Update Available",
                    "${status.currentVersion} → ${status.latestVersion}",
                    "Update to v${status.latestVersion}"
                )
                is PackStatus.UpToDate -> Triple(
                    "Retro Rewind Pack",
                    "Up to date",
                    ""
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(onClick = { viewModel.checkStatus() }) {
                    Text("Check Again")
                }

                if (status is PackStatus.UpToDate || status is PackStatus.Installed) {
                    Button(onClick = { viewModel.launchDolphin() }) {
                        Text("Launch Dolphin")
                    }
                }

                if (actionText.isNotEmpty()) {
                    Button(onClick = { viewModel.downloadOrUpdate(status) }) {
                        Text(actionText)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressContent(progress: Float, message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (progress >= 0f) {
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    strokeWidth = 3.dp
                )
            }
        }
    }
}

@Composable
private fun ReadyToLaunchContent(
    version: String,
    onLaunch: () -> Unit,
    onCheckAgain: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Ready!",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Retro Rewind Pack v$version is ready to play.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onLaunch) {
                    Text("Launch Dolphin")
                }
                OutlinedButton(onClick = onCheckAgain) {
                    Text("Check Again")
                }
            }
        }
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit, onPickIso: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Error",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (message.contains("ROM file", ignoreCase = true)) {
                OutlinedButton(onClick = onPickIso) {
                    Text("Select Mario Kart Wii ROM")
                }
            } else {
                Button(onClick = onRetry) {
                    Text("Retry")
                }
            }
        }
    }
}
