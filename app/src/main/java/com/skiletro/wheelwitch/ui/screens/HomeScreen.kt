package com.skiletro.wheelwitch.ui.screens


import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dontsaybojio.rollingnumbers.RollingNumbers
import com.skiletro.wheelwitch.R
import com.skiletro.wheelwitch.model.LicenseInfo
import com.skiletro.wheelwitch.model.PackStatus
import com.skiletro.wheelwitch.model.ServerConnectivity
import com.skiletro.wheelwitch.ui.components.MiiFace
import com.skiletro.wheelwitch.ui.components.PrimaryActionButton
import com.skiletro.wheelwitch.ui.components.TopBar
import com.skiletro.wheelwitch.ui.components.buttonShape
import com.skiletro.wheelwitch.ui.components.focusBorder
import com.skiletro.wheelwitch.ui.components.formatBytesPerSecond
import com.skiletro.wheelwitch.ui.components.formatDownloadProgress
import com.skiletro.wheelwitch.ui.components.sectionShape
import com.skiletro.wheelwitch.ui.theme.CtmkfFontFamily
import com.skiletro.wheelwitch.viewmodel.MiiMakerViewModel
import com.skiletro.wheelwitch.viewmodel.OnlineViewModel
import com.skiletro.wheelwitch.viewmodel.PackUpdateViewModel
import com.skiletro.wheelwitch.viewmodel.RoomsState
import com.skiletro.wheelwitch.viewmodel.SaveDataViewModel
import com.skiletro.wheelwitch.viewmodel.UiState

@Composable
fun HomeScreen(
    packUpdate: PackUpdateViewModel,
    saveData: SaveDataViewModel,
    miiMaker: MiiMakerViewModel,
    onlineViewModel: OnlineViewModel,
    onPickIso: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val state by packUpdate.state.collectAsState()
    val successMessage by packUpdate.successMessage.collectAsState()
    val hasWad by miiMaker.hasWad.collectAsState()
    val roomsState by onlineViewModel.roomsState.collectAsState()
    val saveInfoState by saveData.saveInfoState.collectAsState()
    val selectedSlotIndex by saveData.selectedSlotIndex.collectAsState()
    val activeLicenseInfo by saveData.activeLicenseInfo.collectAsState()
    val cachedLeaderboardVrs by saveData.cachedLeaderboardVrs.collectAsState()
    val vrMultiplier by onlineViewModel.vrMultiplier.collectAsState()
    val currentIsoPath by packUpdate.currentIsoPath.collectAsState()
    val hasIso = currentIsoPath != null

    val playerCount = (roomsState as? RoomsState.Success)?.playerCount
    val serverConnectivity =
        (roomsState as? RoomsState.Success)?.serverConnectivity ?: ServerConnectivity.Unknown

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                onlineViewModel.fetchRooms()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var showOnlineMenu by remember { mutableStateOf(false) }
    var showSaveInfo by remember { mutableStateOf(false) }
    var showChangelog by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = !(showOnlineMenu || showSaveInfo || showChangelog)) {
        showExitDialog = true
    }

    val activity = LocalActivity.current

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text(stringResource(R.string.home_exit_title)) },
            text = { Text(stringResource(R.string.home_exit_message)) },
            confirmButton = {
                TextButton(onClick = { activity?.finish() }) {
                    Text(stringResource(R.string.action_exit))
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    val isBusy =
        state is UiState.Downloading || state is UiState.Extracting || state is UiState.ApplyingUpdate

    val context = LocalContext.current

    LaunchedEffect(successMessage) {
        if (successMessage != null) {
            Toast.makeText(context, successMessage, Toast.LENGTH_SHORT).show()
            packUpdate.dismissSuccess()
        }
    }

    LaunchedEffect(state) {
        if (state is UiState.Error) {
            val error = (state as UiState.Error).message
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
        }
    }

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
            is PackStatus.NotInstalled -> ({ packUpdate.downloadOrUpdate(s) })
            is PackStatus.UpdateAvailable -> ({ packUpdate.downloadOrUpdate(s) })
            else -> null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (!(showOnlineMenu || showSaveInfo || showChangelog)) {
            Scaffold(
                topBar = {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp
                    ) {
                        TopBar(
                            onOpenSettings = onOpenSettings,
                            onLaunchMiiMaker = { miiMaker.launchMiiMaker() },
                            miiMakerEnabled = hasWad,
                            onOpenOnlineMenu = {
                                showOnlineMenu = true
                            },
                            onlineMenuEnabled = serverConnectivity is ServerConnectivity.Online,
                            onOpenSaveInfo = {
                                saveData.refreshSaveFileInfo()
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
                                activeLicense = activeLicenseInfo,
                                cachedLeaderboardVrs = cachedLeaderboardVrs,
                                vrMultiplier = vrMultiplier,
                                playerCount = playerCount,
                                serverConnectivity = serverConnectivity,
                                isBusy = isBusy,
                                hasIso = hasIso,
                                onLaunch = { packUpdate.launchDolphin() },
                                onCheck = { packUpdate.checkStatus() },
                                onRetry = { packUpdate.clearError() },
                                onInstallOrUpdate = onInstallOrUpdate,
                                onPickIso = onPickIso
                            )
                        }
                    }
                }
            ) { padding ->
                var changelogFocused by remember { mutableStateOf(false) }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(padding)
                        .clickable { showChangelog = true }
                        .focusable()
                        .onFocusChanged { changelogFocused = it.isFocused }
                        .focusBorder(changelogFocused, sectionShape),
                    contentAlignment = Alignment.Center
                ) {
                    VersionHistoryContent(modifier = Modifier.fillMaxSize())
                }
            }
        }

        BackHandler(enabled = showChangelog) {
            showChangelog = false
        }

        AnimatedVisibility(
            visible = showChangelog,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
            ChangelogDetailScreen(onClose = { showChangelog = false })
        }

        AnimatedVisibility(
            visible = showOnlineMenu,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
            OnlineMenuScreen(
                viewModel = onlineViewModel,
                onClose = { showOnlineMenu = false }
            )
        }

        AnimatedVisibility(
            visible = showSaveInfo,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
            SaveInfoScreen(
                saveInfoState = saveInfoState,
                selectedSlotIndex = selectedSlotIndex,
                cachedLeaderboardVrs = cachedLeaderboardVrs,
                onSelectSlot = { saveData.selectSlot(it) },
                onRefresh = { saveData.refreshSaveFileInfo() },
                onClose = { showSaveInfo = false }
            )
        }
    }
}

@Composable
private fun HomeBottomBar(
    state: UiState,
    activeLicense: LicenseInfo?,
    cachedLeaderboardVrs: Map<Int, Int>,
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
    var checkButtonFocused by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (activeLicense != null) {
            ActivePlayerCard(
                license = activeLicense,
                cachedLeaderboardVr = cachedLeaderboardVrs[activeLicense.slotIndex],
                vrMultiplier = vrMultiplier
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        AnimatedContent(
            targetState = state,
            contentKey = { it::class },
            transitionSpec = {
                (fadeIn(animationSpec = androidx.compose.animation.core.tween(250)) +
                        scaleIn(
                            initialScale = 0.92f,
                            animationSpec = androidx.compose.animation.core.tween(250)
                        ))
                    .togetherWith(fadeOut(animationSpec = androidx.compose.animation.core.tween(150)))
            },
            label = "primary_action"
        ) { currentState ->
            when (currentState) {
                is UiState.Downloading -> {
                    ProgressButton(
                        currentState.progress,
                        currentState.message,
                        currentState.bytesPerSecond,
                        currentState.bytesDownloaded,
                        currentState.totalBytes,
                    )
                }

                is UiState.Extracting -> {
                    ProgressButton(
                        currentState.progress,
                        stringResource(R.string.status_extracting)
                    )
                }

                is UiState.ApplyingUpdate -> {
                    ProgressButton(
                        currentState.progress,
                        stringResource(
                            R.string.home_update_step_format,
                            currentState.index,
                            currentState.total,
                            currentState.description
                        )
                    )
                }

                is UiState.Checking -> {
                    FilledTonalButton(
                        onClick = {},
                        enabled = false,
                        shape = buttonShape,
                        modifier = Modifier.height(56.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.status_checking),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                is UiState.Error -> {
                    FilledTonalButton(
                        onClick = onRetry,
                        shape = buttonShape,
                        modifier = Modifier.height(56.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.action_retry),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                is UiState.Ready -> {
                    val status = currentState.status

                    val checkSubtitle = when (status) {
                        is PackStatus.UpToDate -> stringResource(
                            R.string.home_up_to_date,
                            status.currentVersion
                        )

                        is PackStatus.UpdateAvailable -> stringResource(
                            R.string.home_update_format,
                            status.currentVersion,
                            status.latestVersion
                        )

                        is PackStatus.Installed -> stringResource(
                            R.string.home_version_installed,
                            status.version
                        )

                        else -> null
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilledTonalButton(
                            onClick = onCheck,
                            enabled = !isBusy,
                            shape = buttonShape,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            modifier = Modifier
                                .height(56.dp)
                                .onFocusChanged { checkButtonFocused = it.isFocused }
                                .focusBorder(checkButtonFocused)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = stringResource(R.string.home_check_for_updates),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                if (checkSubtitle != null) {
                                    Text(
                                        text = checkSubtitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Normal
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        when (status) {
                            is PackStatus.NotInstalled -> {
                                PrimaryActionButton(
                                    text = stringResource(R.string.action_install),
                                    onClick = onInstallOrUpdate ?: {}
                                )
                            }

                            is PackStatus.UpdateAvailable -> {
                                PrimaryActionButton(
                                    text = stringResource(
                                        R.string.home_update_to,
                                        status.latestVersion
                                    ),
                                    onClick = onInstallOrUpdate ?: {}
                                )
                            }

                            else -> {
                                val bullet = "\u2022 "
                                val launchSubText = when (serverConnectivity) {
                                    ServerConnectivity.Online -> {
                                        val count = playerCount
                                        if (count != null) "$bullet${
                                            stringResource(
                                                R.string.home_racers_online,
                                                count
                                            )
                                        }" else null
                                    }

                                    ServerConnectivity.Offline -> "$bullet${stringResource(R.string.home_offline)}"
                                    ServerConnectivity.NoInternet -> "$bullet${stringResource(R.string.status_no_internet)}"
                                    ServerConnectivity.Unknown -> null
                                }
                                if (hasIso) {
                                    PrimaryActionButton(
                                        text = stringResource(R.string.home_launch_retro_rewind),
                                        onClick = onLaunch,
                                        subText = launchSubText
                                    )
                                } else {
                                    PrimaryActionButton(
                                        text = stringResource(R.string.home_select_rom),
                                        onClick = onPickIso
                                    )
                                }
                            }
                        }
                    }
                }

                is UiState.NoStorage -> {
                    Text(
                        text = stringResource(R.string.error_storage_not_configured),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivePlayerCard(
    license: LicenseInfo,
    cachedLeaderboardVr: Int?,
    vrMultiplier: Float?
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        MiiFace(
            imageBase64 = license.leaderboardMiiImageBase64,
            miiDataBase64 = license.miiDataBase64,
            modifier = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = license.miiName ?: stringResource(R.string.player_default),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                fontFamily = CtmkfFontFamily
            )
            val vr = license.leaderboardVr ?: cachedLeaderboardVr ?: license.vr
            if (vr != null) {
                val showActive = vrMultiplier != null && vrMultiplier > 1.0f
                val multText = if (showActive) {
                    if (vrMultiplier == vrMultiplier.toInt().toFloat()) {
                        vrMultiplier.toInt().toString()
                    } else {
                        vrMultiplier.toString()
                    }
                } else null
                val suffix = if (multText != null) {
                    stringResource(R.string.home_vr_active_suffix, multText)
                } else {
                    stringResource(R.string.home_vr_suffix)
                }
                val numberStyle = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RollingNumbers(
                        text = vr.toString(),
                        textStyle = numberStyle
                    )
                    Text(
                        text = suffix,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ProgressButton(
    progress: Float,
    label: String,
    bytesPerSecond: Long? = null,
    bytesDownloaded: Long = 0L,
    totalBytes: Long = 0L,
) {
    val percent = (progress.coerceIn(0f, 1f) * 100f).toInt()
    val showSize = bytesPerSecond != null && (bytesDownloaded > 0L || totalBytes > 0L)
    val sizeText = if (showSize) formatDownloadProgress(bytesDownloaded, totalBytes) else ""
    Column(
        modifier = Modifier
            .widthIn(min = 220.dp),
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.download_percent_format, percent),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
            if (showSize) {
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = sizeText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
            }
            if (bytesPerSecond != null) {
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = formatBytesPerSecond(bytesPerSecond),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            fontWeight = FontWeight.Medium
        )
    }
}
