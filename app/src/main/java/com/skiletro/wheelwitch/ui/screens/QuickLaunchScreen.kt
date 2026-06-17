package com.skiletro.wheelwitch.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.skiletro.wheelwitch.model.PackStatus
import com.skiletro.wheelwitch.ui.components.buttonShape
import com.skiletro.wheelwitch.viewmodel.PackUpdateViewModel
import com.skiletro.wheelwitch.viewmodel.UiState
import kotlinx.coroutines.delay

@Composable
fun QuickLaunchScreen(
    viewModel: PackUpdateViewModel,
    onFinish: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var actionTaken by remember { mutableStateOf(false) }
    var showCountdown by remember { mutableStateOf(false) }

    LaunchedEffect(state) {
        if (actionTaken) return@LaunchedEffect
        val s = state
        if (s is UiState.Ready) {
            actionTaken = true
            when (s.status) {
                is PackStatus.NotInstalled -> viewModel.downloadOrUpdate(s.status)
                is PackStatus.UpdateAvailable -> viewModel.downloadOrUpdate(s.status)
                is PackStatus.UpToDate -> showCountdown = true
                else -> {}
            }
        } else if (s is UiState.NoStorage || s is UiState.Error) {
            actionTaken = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (showCountdown) {
            CountdownPhase(onLaunch = {
                viewModel.launchDolphin()
                onFinish()
            })
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 400.dp)
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                when (val s = state) {
                    is UiState.NoStorage -> {
                        StatusMessage("Storage not configured.\nOpen Wheel Witch first to set up your pack location.", isError = true)
                        Spacer(modifier = Modifier.height(24.dp))
                        ExitButton(onClick = onFinish)
                    }
                    is UiState.Error -> {
                        StatusMessage(s.message, isError = true)
                        Spacer(modifier = Modifier.height(24.dp))
                        ExitButton(onClick = onFinish)
                    }
                    is UiState.Checking -> {
                        StatusMessage("Checking for updates...")
                    }
                    is UiState.Downloading -> {
                        StatusMessage(s.message)
                        Spacer(modifier = Modifier.height(24.dp))
                        ProgressBar(s.progress)
                    }
                    is UiState.Extracting -> {
                        StatusMessage("Extracting files...")
                        Spacer(modifier = Modifier.height(24.dp))
                        ProgressBar(s.progress)
                    }
                    is UiState.ApplyingUpdate -> {
                        StatusMessage(s.description)
                        Spacer(modifier = Modifier.height(24.dp))
                        ProgressBar(s.progress)
                    }
                    is UiState.Ready -> {
                        when (s.status) {
                            is PackStatus.Installed -> {
                                StatusMessage("Cannot reach the update server.\nCheck your internet connection.", isError = true)
                                Spacer(modifier = Modifier.height(24.dp))
                                ExitButton(onClick = onFinish)
                            }
                            else -> StatusMessage("Preparing...")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusMessage(text: String, isError: Boolean = false) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 32.dp)
    )
}

@Composable
private fun ProgressBar(progress: Float) {
    LinearProgressIndicator(
        progress = { progress.coerceIn(0f, 1f) },
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 300.dp)
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp)),
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
    )
}

@Composable
private fun ExitButton(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        shape = buttonShape,
        modifier = Modifier.height(48.dp)
    ) {
        Text("Exit", fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun CountdownPhase(onLaunch: () -> Unit) {
    var count by remember { mutableStateOf(3) }

    LaunchedEffect(Unit) {
        while (count > 0) {
            delay(1000)
            count--
        }
        onLaunch()
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(com.skiletro.wheelwitch.R.drawable.ic_hat_wizard),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Everything's up to date!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = if (count > 0) "Starting in $count..." else "Launching!",
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        if (count == 0) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Launching Dolphin Emulator...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
