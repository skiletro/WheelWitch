package com.skiletro.wheelwitch.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.skiletro.wheelwitch.service.PackStatus
import com.skiletro.wheelwitch.viewmodel.UiState
import com.skiletro.wheelwitch.viewmodel.UpdateViewModel

private val cardShape = RoundedCornerShape(16.dp)
private val buttonShape = RoundedCornerShape(12.dp)

@Composable
fun SplashScreen(
    viewModel: UpdateViewModel,
    onPickStorage: () -> Unit,
    onPickIso: () -> Unit
) {
    val state by viewModel.state.collectAsState()

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
                text = "WheelWitch",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(4.dp))

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
        Button(
            onClick = onClick,
            shape = buttonShape,
            modifier = Modifier.height(48.dp)
        ) {
            Text(text)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            shape = buttonShape,
            modifier = Modifier.height(48.dp)
        ) {
            Text(text)
        }
    }
}

@Composable
private fun NoStorageContent(onPickStorage: () -> Unit) {
    ContentCard {
        Text(
            text = "Welcome!",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Choose where to store the Retro Rewind pack files. Pick a folder that Dolphin Emulator can access, or anywhere you prefer.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(20.dp))
        ActionButton(text = "Select Storage Folder", onClick = onPickStorage)
    }
}

@Composable
private fun CheckingContent() {
    ContentCard {
        CircularProgressIndicator(
            modifier = Modifier.size(32.dp),
            strokeWidth = 3.dp,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Checking for updates...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ReadyContent(
    status: PackStatus,
    viewModel: UpdateViewModel
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
            "${status.currentVersion} \u2192 ${status.latestVersion}",
            "Update to v${status.latestVersion}"
        )
        is PackStatus.UpToDate -> Triple(
            "Retro Rewind Pack",
            "v${status.currentVersion} (latest is v${status.latestVersion})",
            ""
        )
    }

    ContentCard {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { viewModel.checkStatus() },
                shape = buttonShape,
                modifier = Modifier.height(48.dp)
            ) {
                Text("Check Again")
            }

            if (status is PackStatus.UpToDate || status is PackStatus.Installed) {
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = { viewModel.launchDolphin() },
                    shape = buttonShape,
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Launch Dolphin")
                }
            }

            if (actionText.isNotEmpty()) {
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = { viewModel.downloadOrUpdate(status) },
                    shape = buttonShape,
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(actionText)
                }
            }
        }
    }
}

@Composable
private fun ProgressCard(progress: Float, message: String) {
    ContentCard {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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

@Composable
private fun ReadyToLaunchContent(
    version: String,
    onLaunch: () -> Unit,
    onCheckAgain: () -> Unit
) {
    ContentCard {
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
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onLaunch,
                shape = buttonShape,
                modifier = Modifier.height(48.dp)
            ) {
                Text("Launch Dolphin")
            }
            Spacer(modifier = Modifier.width(12.dp))
            OutlinedButton(
                onClick = onCheckAgain,
                shape = buttonShape,
                modifier = Modifier.height(48.dp)
            ) {
                Text("Check Again")
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
            Spacer(modifier = Modifier.height(20.dp))
            if (message.contains("ROM file", ignoreCase = true)) {
                OutlinedButton(
                    onClick = onPickIso,
                    shape = buttonShape,
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Select Mario Kart Wii ROM")
                }
            } else {
                Button(
                    onClick = onRetry,
                    shape = buttonShape,
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Try Again")
                }
            }
        }
    }
}
