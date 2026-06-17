package com.skiletro.wheelwitch.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.skiletro.wheelwitch.model.PackStatus
import com.skiletro.wheelwitch.model.ServerConnectivity
import com.skiletro.wheelwitch.viewmodel.RoomsState
import com.skiletro.wheelwitch.viewmodel.UiState
import com.skiletro.wheelwitch.viewmodel.UpdateViewModel
import kotlinx.coroutines.delay

private const val PACK_NAME = "Retro Rewind Pack"
private val sectionShape = RoundedCornerShape(20.dp)
private val buttonShape = RoundedCornerShape(14.dp)

@Composable
fun HomeScreen(
    viewModel: UpdateViewModel,
    onPickStorage: () -> Unit,
    onPickIso: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val miiMakerState by viewModel.miiMakerState.collectAsState()
    val roomsState by viewModel.roomsState.collectAsState()
    val saveInfoState by viewModel.saveInfoState.collectAsState()
    val vrMultiplier by viewModel.vrMultiplier.collectAsState()

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

    val isChecking = state is UiState.Checking
    val showBottomLaunch = when (val s = state) {
        is UiState.ReadyToLaunch -> true
        is UiState.Ready -> isInstalled(s.status)
        is UiState.Checking -> true
        else -> false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (showBottomLaunch) {
            val onLaunch = when (val s = state) {
                is UiState.ReadyToLaunch -> ({ viewModel.launchDolphin() })
                is UiState.Ready -> ({ viewModel.launchDolphin() })
                else -> ({})
            }
            val versionInfo = when (val s = state) {
                is UiState.ReadyToLaunch -> "v${s.version}"
                is UiState.Ready -> when (val st = s.status) {
                    is PackStatus.Installed -> "v${st.version}"
                    is PackStatus.UpToDate -> "v${st.currentVersion} (latest v${st.latestVersion})"
                    else -> null
                }
                else -> null
            }

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
                        Column(
                            modifier = Modifier.padding(vertical = 12.dp)
                        ) {
                            BottomLaunchBar(
                                onLaunch = onLaunch,
                                onRefresh = { viewModel.checkStatus() },
                                versionInfo = versionInfo,
                                playerCount = playerCount,
                                serverConnectivity = serverConnectivity,
                                isChecking = isChecking,
                                vrMultiplier = vrMultiplier
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
                    VersionHistoryWebView(modifier = Modifier.fillMaxSize())
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
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

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(bottom = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (successMessage != null) {
                            SuccessBanner(successMessage!!)
                        }

                        AnimatedContent(
                            targetState = state,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "stateTransition"
                        ) { currentState ->
                            when (currentState) {
                                is UiState.NoStorage -> HomeNoStorageContent(onPickStorage)
                                is UiState.Ready -> HomeReadyContent(
                                    status = currentState.status,
                                    viewModel = viewModel,
                                    vrMultiplier = vrMultiplier
                                )
                                is UiState.Downloading -> HomeProgressContent(
                                    currentState.progress,
                                    currentState.message
                                )
                                is UiState.Extracting -> HomeProgressContent(
                                    currentState.progress,
                                    "Extracting files..."
                                )
                                is UiState.ApplyingUpdate -> HomeProgressContent(
                                    currentState.progress,
                                    "Applying update ${currentState.index}/${currentState.total}: ${currentState.description}"
                                )
                                is UiState.ReadyToLaunch -> HomeReadyToLaunchContent(
                                    version = currentState.version
                                )
                                is UiState.Error -> HomeErrorContent(
                                    currentState.message,
                                    onRetry = { viewModel.clearError() },
                                    onPickIso = onPickIso
                                )
                                else -> {}
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
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

private fun isInstalled(status: PackStatus): Boolean {
    return status is PackStatus.Installed || status is PackStatus.UpToDate
}

@Composable
fun SuccessBanner(message: String) {
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
fun BottomLaunchBar(
    onLaunch: () -> Unit,
    onRefresh: () -> Unit,
    versionInfo: String? = null,
    playerCount: Int? = null,
    serverConnectivity: ServerConnectivity = ServerConnectivity.Unknown,
    isChecking: Boolean = false,
    vrMultiplier: Float? = null
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
        var refreshFocused by remember { mutableStateOf(false) }
        OutlinedButton(
            onClick = onRefresh,
            enabled = !isChecking,
            shape = buttonShape,
            modifier = Modifier
                .height(56.dp)
                .onFocusChanged { refreshFocused = it.isFocused }
                .then(
                    if (refreshFocused) Modifier.border(
                        width = 3.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = buttonShape
                    ) else Modifier
                )
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (isChecking) "Checking..." else "Check for updates",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                if (versionInfo != null) {
                    Text(
                        text = versionInfo,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        PrimaryActionButton(
            text = "Launch",
            onClick = onLaunch,
            enabled = !isChecking,
            badgeText = vrMultiplier?.let { m ->
                if (m > 1.0f) {
                    if (m == m.toInt().toFloat()) "${m.toInt()}x VR" else "${m}x VR"
                } else null
            }
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

@Composable
fun HomeNoStorageContent(onPickStorage: () -> Unit = {}) {
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

@Composable
fun HomeReadyContent(
    status: PackStatus,
    viewModel: UpdateViewModel,
    vrMultiplier: Float? = null
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

    ContentSection {
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
                modifier = Modifier.fillMaxWidth(),
                badgeText = vrMultiplier?.let { m ->
                    if (m > 1.0f) {
                    if (m == m.toInt().toFloat()) "${m.toInt()}x VR" else "${m}x VR"
                    } else null
                }
            )
        }
    }
}

@Composable
fun HomeProgressContent(
    progress: Float = -1f,
    message: String = "Downloading..."
) {
    val infiniteTransition = rememberInfiniteTransition(label = "progress")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    ContentSection {
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(20.dp))
        Box(
            modifier = Modifier.size(80.dp),
            contentAlignment = Alignment.Center
        ) {
            if (progress >= 0f) {
                CircularProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 4.dp,
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 4.dp,
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
            Icon(
                painter = painterResource(com.skiletro.wheelwitch.R.drawable.ic_hat_wizard),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(36.dp)
                    .rotate(rotation)
            )
        }
        if (progress >= 0f) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun HomeReadyToLaunchContent(
    version: String = "3.2.6"
) {
    ContentSection {
        SectionTitle("Ready to play!", color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(6.dp))
        BodyText("$PACK_NAME v$version")
    }
}

@Composable
fun HomeErrorContent(
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


