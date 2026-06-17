package com.skiletro.wheelwitch.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.skiletro.wheelwitch.model.PackStatus
import com.skiletro.wheelwitch.model.ServerConnectivity
import com.skiletro.wheelwitch.viewmodel.RoomsState
import com.skiletro.wheelwitch.viewmodel.UiState
import com.skiletro.wheelwitch.viewmodel.UpdateViewModel
import kotlinx.coroutines.delay
import android.view.WindowManager

private val sectionShape = RoundedCornerShape(20.dp)
private val buttonShape = RoundedCornerShape(14.dp)

@Composable
fun HomeScreen(
    viewModel: UpdateViewModel,
    onPickIso: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val miiMakerState by viewModel.miiMakerState.collectAsState()
    val roomsState by viewModel.roomsState.collectAsState()
    val saveInfoState by viewModel.saveInfoState.collectAsState()
    val vrMultiplier by viewModel.vrMultiplier.collectAsState()
    val currentIsoPath by viewModel.currentIsoPath.collectAsState()
    val hasIso = currentIsoPath != null

    val playerCount = (roomsState as? RoomsState.Success)?.playerCount
    val serverConnectivity = (roomsState as? RoomsState.Success)?.serverConnectivity ?: ServerConnectivity.Unknown

    var showRooms by remember { mutableStateOf(false) }
    var showSaveInfo by remember { mutableStateOf(false) }

    LaunchedEffect(successMessage) {
        if (successMessage != null) {
            delay(3000)
            viewModel.dismissSuccess()
        }
    }

    val isBusy = state is UiState.Downloading || state is UiState.Extracting || state is UiState.ApplyingUpdate

    val context = LocalContext.current
    LaunchedEffect(isBusy) {
        if (isBusy) {
            (context as? android.app.Activity)?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            (context as? android.app.Activity)?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val packStatus = (state as? UiState.Ready)?.status

    val onInstallOrUpdate: (() -> Unit)? = packStatus?.let { s ->
        when (s) {
            is PackStatus.NotInstalled -> ({ viewModel.downloadOrUpdate(s) })
            is PackStatus.UpdateAvailable -> ({ viewModel.downloadOrUpdate(s) })
            else -> null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    TopBar(
                        onOpenSettings = onOpenSettings,
                        onLaunchMiiMaker = { viewModel.launchMiiMaker() },
                        miiMakerEnabled = miiMakerState.hasWad,
                        onOpenNetplay = {
                            viewModel.fetchRooms()
                            showRooms = true
                        },
                        roomsEnabled = serverConnectivity is ServerConnectivity.Online,
                        onOpenSaveInfo = {
                            viewModel.refreshSaveFileInfo()
                            showSaveInfo = true
                        }
                    )
                }
            },
            bottomBar = {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    Column(modifier = Modifier.padding(vertical = 12.dp)) {
                        HomeBottomBar(
                            state = state,
                            vrMultiplier = vrMultiplier,
                            playerCount = playerCount,
                            serverConnectivity = serverConnectivity,
                            isBusy = isBusy,
                            hasIso = hasIso,
                            onLaunch = { viewModel.launchDolphin() },
                            onCheck = { viewModel.checkStatus() },
                            onRetry = { viewModel.clearError() },
                            onInstallOrUpdate = onInstallOrUpdate,
                            onPickIso = onPickIso
                        )
                    }
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(padding)
            ) {
                VersionHistoryContent(modifier = Modifier.fillMaxSize())

                if (successMessage != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = sectionShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = successMessage!!,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                if (state is UiState.Error) {
                    val error = (state as UiState.Error).message
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = sectionShape,
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showRooms,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
            RoomsScreen(
                roomsState = roomsState,
                onRefresh = { viewModel.fetchRooms() },
                onClose = { showRooms = false }
            )
        }

        AnimatedVisibility(
            visible = showSaveInfo,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
            SaveInfoScreen(
                saveInfoState = saveInfoState,
                onRefresh = { viewModel.refreshSaveFileInfo() },
                onClose = { showSaveInfo = false }
            )
        }
    }
}

@Composable
private fun HomeBottomBar(
    state: UiState,
    vrMultiplier: Float?,
    playerCount: Int?,
    serverConnectivity: ServerConnectivity,
    isBusy: Boolean,
    hasIso: Boolean,
    onLaunch: () -> Unit,
    onCheck: () -> Unit,
    onRetry: () -> Unit,
    onInstallOrUpdate: (() -> Unit)?,
    onPickIso: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ServerStatusIndicator(playerCount = playerCount, connectivity = serverConnectivity)
        Spacer(modifier = Modifier.width(16.dp))
        Spacer(modifier = Modifier.weight(1f))

        when (state) {
            is UiState.Downloading -> {
                ProgressButton(state.progress, state.message)
            }
            is UiState.Extracting -> {
                ProgressButton(state.progress, "Extracting files...")
            }
            is UiState.ApplyingUpdate -> {
                ProgressButton(state.progress, "Update ${state.index}/${state.total}: ${state.description}")
            }
            is UiState.Checking -> {
                OutlinedButton(
                    onClick = {},
                    enabled = false,
                    shape = buttonShape,
                    modifier = Modifier.height(56.dp)
                ) {
                    Text(
                        text = "Checking...",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            is UiState.Error -> {
                OutlinedButton(
                    onClick = onRetry,
                    shape = buttonShape,
                    modifier = Modifier.height(56.dp)
                ) {
                    Text(
                        text = "Try Again",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            is UiState.Ready -> {
                val status = state.status
                var checkButtonFocused by remember { mutableStateOf(false) }
                OutlinedButton(
                    onClick = onCheck,
                    enabled = !isBusy,
                    shape = buttonShape,
                    modifier = Modifier
                        .height(56.dp)
                        .onFocusChanged { checkButtonFocused = it.isFocused }
                        .then(
                            if (checkButtonFocused) Modifier.border(
                                width = 3.dp, color = MaterialTheme.colorScheme.primary, shape = buttonShape
                            ) else Modifier
                        )
                ) {
                    Text(
                        text = "Check for updates",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                when (status) {
                    is PackStatus.NotInstalled -> {
                        PrimaryActionButton(
                            text = "Install",
                            onClick = onInstallOrUpdate ?: {}
                        )
                    }
                    is PackStatus.UpdateAvailable -> {
                        PrimaryActionButton(
                            text = "Update to v${status.latestVersion}",
                            onClick = onInstallOrUpdate ?: {}
                        )
                    }
                    else -> {
                        if (hasIso) {
                            PrimaryActionButton(
                                text = "Launch Retro Rewind",
                                onClick = onLaunch,
                                badgeText = vrMultiplier?.let { m ->
                                    if (m > 1.0f) {
                                        if (m == m.toInt().toFloat()) "${m.toInt()}x VR" else "${m}x VR"
                                    } else null
                                }
                            )
                        } else {
                            PrimaryActionButton(
                                text = "Select Mario Kart Wii ROM",
                                onClick = onPickIso
                            )
                        }
                    }
                }
            }
            is UiState.NoStorage -> {
                Text(
                    text = "Storage not configured",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ProgressButton(progress: Float, label: String) {
    Column(
        modifier = Modifier.widthIn(min = 220.dp),
        horizontalAlignment = Alignment.End
    ) {
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun ServerStatusIndicator(
    playerCount: Int?,
    connectivity: ServerConnectivity
) {
    val dotColor: Color
    val label: String

    when (connectivity) {
        ServerConnectivity.Online -> {
            val count = playerCount ?: return
            dotColor = Color(0xFF4CAF50)
            label = "$count racers online"
        }
        ServerConnectivity.Offline -> {
            dotColor = MaterialTheme.colorScheme.error
            label = "Server offline"
        }
        ServerConnectivity.NoInternet -> {
            dotColor = MaterialTheme.colorScheme.onSurfaceVariant
            label = "No internet"
        }
        ServerConnectivity.Unknown -> {
            dotColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            label = "Checking..."
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
