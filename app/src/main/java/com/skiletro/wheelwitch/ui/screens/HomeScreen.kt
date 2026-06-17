package com.skiletro.wheelwitch.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import com.skiletro.wheelwitch.model.PackStatus
import com.skiletro.wheelwitch.model.ServerConnectivity
import com.skiletro.wheelwitch.viewmodel.MiiMakerState
import com.skiletro.wheelwitch.viewmodel.UiState
import com.skiletro.wheelwitch.viewmodel.UpdateViewModel
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.cos
import kotlin.math.sin
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
    onOpenSettings: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val miiMakerState by viewModel.miiMakerState.collectAsState()
    val playerCount by viewModel.playerCount.collectAsState()
    val serverConnectivity by viewModel.serverConnectivity.collectAsState()
    val rooms by viewModel.rooms.collectAsState()
    val isLoadingRooms by viewModel.isLoadingRooms.collectAsState()
    val roomsError by viewModel.roomsError.collectAsState()

    var showRooms by remember { mutableStateOf(false) }

    LaunchedEffect(successMessage) {
        if (successMessage != null) {
            delay(3000)
            viewModel.dismissSuccess()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshMiiMakerState()
    }

    val isChecking = state is UiState.Checking
    val showBottomLaunch = when (val s = state) {
        is UiState.ReadyToLaunch -> true
        is UiState.Ready -> isInstalled(s.status)
        is UiState.Checking -> true
        else -> false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (showRooms) {
            RoomsScreen(
                rooms = rooms,
                isLoading = isLoadingRooms,
                errorMessage = roomsError,
                onRefresh = { viewModel.fetchRooms() },
                onClose = { showRooms = false }
            )
        } else if (showBottomLaunch) {
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
                            roomsEnabled = serverConnectivity is ServerConnectivity.Online
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
                                isChecking = isChecking
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
                    roomsEnabled = serverConnectivity is ServerConnectivity.Online
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
                                is UiState.NoStorage -> NoStorageContent(onPickStorage)
                                is UiState.Ready -> ReadyContent(
                                    status = currentState.status,
                                    viewModel = viewModel
                                )
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
                                is UiState.ReadyToLaunch -> ReadyToLaunchContent(
                                    version = currentState.version
                                )
                                is UiState.Error -> ErrorContent(
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
    }
}

private fun isInstalled(status: PackStatus): Boolean {
    return status is PackStatus.Installed || status is PackStatus.UpToDate
}

@Composable
private fun TopBar(
    onOpenSettings: () -> Unit,
    onLaunchMiiMaker: () -> Unit,
    miiMakerEnabled: Boolean,
    onOpenNetplay: () -> Unit,
    roomsEnabled: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "bob")
    val bobOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bobOffset"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.offset(y = bobOffset.dp),
            contentAlignment = Alignment.Center
        ) {
            SparkleOverlay()
            Icon(
                painter = painterResource(com.skiletro.wheelwitch.R.drawable.ic_hat_wizard),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(38.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
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
        IconButton(onClick = onOpenNetplay, enabled = roomsEnabled) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Online Rooms",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        TextButton(
            onClick = onLaunchMiiMaker,
            enabled = miiMakerEnabled
        ) {
            Icon(
                painter = painterResource(com.skiletro.wheelwitch.R.drawable.ic_tshirt),
                contentDescription = "Mii Maker",
                tint = if (miiMakerEnabled) MaterialTheme.colorScheme.onSurfaceVariant
                       else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                modifier = Modifier.size(24.dp)
            )
        }
        TextButton(onClick = onOpenSettings) {
            Text(
                text = "\u2699",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        ClockText()
    }
}

@Composable
private fun SparkleOverlay() {
    val phaseState = remember { mutableFloatStateOf(0f) }
    val tint = MaterialTheme.colorScheme.primary

    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { nanos ->
                phaseState.floatValue = (nanos / 1_000_000 % 3000) / 3000f
            }
        }
    }

    Box(
        modifier = Modifier
            .size(68.dp)
            .drawBehind {
                val sparklePhase = phaseState.floatValue
                val sparkleCount = 6
                val radius = 22.dp.toPx()
                val sparkleSize = 3.dp.toPx()
                val centerX = size.width / 2
                val centerY = size.height / 2
                val strokeW = 2.dp.toPx()

                for (i in 0 until sparkleCount) {
                    val rawPhase = (sparklePhase + i.toFloat() / sparkleCount) % 1f
                    val alpha = when {
                        rawPhase < 0.35f -> rawPhase / 0.35f
                        rawPhase < 0.65f -> 1f
                        else -> 1f - (rawPhase - 0.65f) / 0.35f
                    }

                    val angle = i.toFloat() / sparkleCount * 2f * kotlin.math.PI.toFloat()
                    val x = centerX + radius * cos(angle)
                    val y = centerY + radius * sin(angle)

                    drawLine(tint.copy(alpha = alpha), Offset(x - sparkleSize, y), Offset(x + sparkleSize, y), strokeW)
                    drawLine(tint.copy(alpha = alpha), Offset(x, y - sparkleSize), Offset(x, y + sparkleSize), strokeW)
                }
            }
    )
}

@Composable
private fun ClockText() {
    val timeText = remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            val now = LocalTime.now()
            timeText.value = now.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
            delay(60_000)
        }
    }

    Text(
        text = timeText.value,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
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
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
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
    enabled: Boolean = true,
) {
    var isFocused by remember { mutableStateOf(false) }

    Button(
        onClick = onClick,
        enabled = enabled,
        shape = buttonShape,
        modifier = modifier
            .height(56.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .then(
                if (isFocused) Modifier.border(
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
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun BottomLaunchBar(
    onLaunch: () -> Unit,
    onRefresh: () -> Unit,
    versionInfo: String? = null,
    playerCount: Int? = null,
    serverConnectivity: ServerConnectivity = ServerConnectivity.Unknown,
    isChecking: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (serverConnectivity != ServerConnectivity.Unknown) {
            ServerStatusIndicator(playerCount = playerCount, connectivity = serverConnectivity)
            Spacer(modifier = Modifier.width(16.dp))
        }
        Spacer(modifier = Modifier.weight(1f))
        OutlinedButton(
            onClick = onRefresh,
            enabled = !isChecking,
            shape = buttonShape,
            modifier = Modifier.height(56.dp)
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
        PrimaryActionButton(text = "Launch", onClick = onLaunch, enabled = !isChecking)
    }
}

@Composable
private fun ServerStatusIndicator(
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
        ServerConnectivity.Unknown -> return
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
private fun VersionHistoryWebView(modifier: Modifier = Modifier) {
    AndroidView(
        factory = { context ->
            try {
                WebView(context).apply {
                    isFocusable = false
                    isFocusableInTouchMode = false
                    settings.javaScriptEnabled = true
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                            if (url.startsWith("https://wiki.tockdom.com/wiki/Retro_Rewind")) {
                                return false
                            }
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            return true
                        }
                    }
                    loadUrl("https://wiki.tockdom.com/wiki/Retro_Rewind#Version_History")
                }
            } catch (_: Exception) {
                android.widget.TextView(context).apply {
                    text = "Version history unavailable"
                    gravity = android.view.Gravity.CENTER
                    textSize = 14f
                }
            }
        },
        modifier = modifier
    )
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

@Composable
private fun ReadyContent(
    status: PackStatus,
    viewModel: UpdateViewModel
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
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ProgressContent(
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

@Preview(showBackground = true)
@Composable
private fun ReadyToLaunchContent(
    version: String = "3.2.6"
) {
    ContentSection {
        SectionTitle("Ready to play!", color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(6.dp))
        BodyText("$PACK_NAME v$version")
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
