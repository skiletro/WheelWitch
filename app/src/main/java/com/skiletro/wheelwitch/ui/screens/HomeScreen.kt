package com.skiletro.wheelwitch.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

    LaunchedEffect(successMessage) {
        if (successMessage != null) {
            delay(3000)
            viewModel.dismissSuccess()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshMiiMakerState()
    }

    val showBottomLaunch = when (val s = state) {
        is UiState.ReadyToLaunch -> true
        is UiState.Ready -> isInstalled(s.status)
        else -> false
    }

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
                        miiMakerEnabled = miiMakerState.hasWad
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
                            serverConnectivity = serverConnectivity
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
                miiMakerEnabled = miiMakerState.hasWad
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

                    when (val currentState = state) {
                        is UiState.NoStorage -> NoStorageContent(onPickStorage)
                        is UiState.Checking -> CheckingContent()
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
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
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
    miiMakerEnabled: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(com.skiletro.wheelwitch.R.drawable.ic_hat_wizard),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(38.dp)
        )
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
    enabled: Boolean = true
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
private fun SecondaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var isFocused by remember { mutableStateOf(false) }

    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = buttonShape,
        modifier = modifier
            .height(48.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .then(
                if (isFocused) Modifier.border(
                    width = 3.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = buttonShape
                ) else Modifier
            ),
        border = if (isFocused) null else ButtonDefaults.outlinedButtonBorder(enabled)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun BottomLaunchBar(
    onLaunch: () -> Unit,
    onRefresh: () -> Unit,
    versionInfo: String? = null,
    playerCount: Int? = null,
    serverConnectivity: ServerConnectivity = ServerConnectivity.Unknown
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
            shape = buttonShape,
            modifier = Modifier.height(56.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Check for updates",
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
        PrimaryActionButton(text = "Launch", onClick = onLaunch)
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
private fun PreviewErrorContent() {
    ErrorContent(
        message = "Please select your Mario Kart Wii ROM file first.",
        onRetry = {},
        onPickIso = {}
    )
}
