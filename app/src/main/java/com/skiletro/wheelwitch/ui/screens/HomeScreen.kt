package com.skiletro.wheelwitch.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import com.skiletro.wheelwitch.model.LicenseInfo
import com.skiletro.wheelwitch.ui.theme.CtmkfFontFamily
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.skiletro.wheelwitch.R
import com.skiletro.wheelwitch.model.PackStatus
import com.skiletro.wheelwitch.model.ServerConnectivity
import com.skiletro.wheelwitch.viewmodel.OnlineViewModel
import com.skiletro.wheelwitch.viewmodel.PackUpdateViewModel
import com.skiletro.wheelwitch.viewmodel.RoomsState
import com.skiletro.wheelwitch.viewmodel.SaveDataViewModel
import com.skiletro.wheelwitch.viewmodel.UiState
import com.skiletro.wheelwitch.viewmodel.MiiMakerViewModel
import com.skiletro.wheelwitch.ui.components.MiiFace
import com.skiletro.wheelwitch.ui.components.buttonShape
import com.skiletro.wheelwitch.ui.components.focusBorder
import com.skiletro.wheelwitch.ui.components.sectionShape
import kotlinx.coroutines.delay
import android.view.WindowManager

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
    val miiMakerState by miiMaker.miiMakerState.collectAsState()
    val roomsState by onlineViewModel.roomsState.collectAsState()
    val saveInfoState by saveData.saveInfoState.collectAsState()
    val selectedSlotIndex by saveData.selectedSlotIndex.collectAsState()
    val activeLicenseInfo by saveData.activeLicenseInfo.collectAsState()
    val vrMultiplier by onlineViewModel.vrMultiplier.collectAsState()
    val currentIsoPath by packUpdate.currentIsoPath.collectAsState()
    val hasIso = currentIsoPath != null

    val playerCount = (roomsState as? RoomsState.Success)?.playerCount
    val serverConnectivity = (roomsState as? RoomsState.Success)?.serverConnectivity ?: ServerConnectivity.Unknown

    var showOnlineMenu by remember { mutableStateOf(false) }
    var showSaveInfo by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = !(showOnlineMenu || showSaveInfo)) {
        showExitDialog = true
    }

    val activity = (LocalContext.current as? android.app.Activity)

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text(stringResource(R.string.home_exit_title)) },
            text = { Text(stringResource(R.string.home_exit_message)) },
            confirmButton = {
                TextButton(onClick = { activity?.finish() }) {
                    Text(stringResource(R.string.home_exit_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text(stringResource(R.string.home_exit_cancel))
                }
            }
        )
    }

    LaunchedEffect(successMessage) {
        if (successMessage != null) {
            delay(3000)
            packUpdate.dismissSuccess()
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
            is PackStatus.NotInstalled -> ({ packUpdate.downloadOrUpdate(s) })
            is PackStatus.UpdateAvailable -> ({ packUpdate.downloadOrUpdate(s) })
            else -> null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (!(showOnlineMenu || showSaveInfo)) {
            Scaffold(
                topBar = {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    TopBar(
                        onOpenSettings = onOpenSettings,
                        onLaunchMiiMaker = { miiMaker.launchMiiMaker() },
                        miiMakerEnabled = miiMakerState.hasWad,
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(padding)
            ) {
                VersionHistoryContent(modifier = Modifier.fillMaxSize())

                AnimatedVisibility(
                    visible = successMessage != null,
                    enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                    modifier = Modifier.align(Alignment.TopCenter)
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = sectionShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = successMessage.orEmpty(),
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                AnimatedVisibility(
                    visible = state is UiState.Error,
                    enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                    modifier = Modifier.align(Alignment.TopCenter)
                ) {
                    val error = (state as? UiState.Error)?.message ?: return@AnimatedVisibility
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
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
            ActivePlayerCard(license = activeLicense, vrMultiplier = vrMultiplier)
        }

        Spacer(modifier = Modifier.weight(1f))

        AnimatedContent(
            targetState = state,
            transitionSpec = {
                (fadeIn(animationSpec = androidx.compose.animation.core.tween(250)) +
                    scaleIn(initialScale = 0.92f, animationSpec = androidx.compose.animation.core.tween(250)))
                    .togetherWith(fadeOut(animationSpec = androidx.compose.animation.core.tween(150)))
            },
            label = "primary_action"
        ) { currentState ->
            when (currentState) {
                is UiState.Downloading -> {
                    ProgressButton(currentState.progress, currentState.message)
                }
                is UiState.Extracting -> {
                    ProgressButton(currentState.progress, stringResource(R.string.home_extracting))
                }
                is UiState.ApplyingUpdate -> {
                    ProgressButton(currentState.progress, stringResource(R.string.home_update_step_format, currentState.index, currentState.total, currentState.description))
                }
                is UiState.Checking -> {
                    OutlinedButton(
                        onClick = {},
                        enabled = false,
                        shape = buttonShape,
                        modifier = Modifier.height(56.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.home_checking),
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
                            text = stringResource(R.string.home_try_again),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                is UiState.Ready -> {
                    val status = currentState.status

                    val checkSubtitle = when (status) {
                        is PackStatus.UpToDate -> stringResource(R.string.home_up_to_date, status.currentVersion)
                        is PackStatus.UpdateAvailable -> stringResource(R.string.home_update_format, status.currentVersion, status.latestVersion)
                        is PackStatus.Installed -> stringResource(R.string.home_version_installed, status.version)
                        else -> null
                    }
                    OutlinedButton(
                        onClick = onCheck,
                        enabled = !isBusy,
                        shape = buttonShape,
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
                                    fontWeight = FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    when (status) {
                        is PackStatus.NotInstalled -> {
                            PrimaryActionButton(
                                text = stringResource(R.string.home_install),
                                onClick = onInstallOrUpdate ?: {}
                            )
                        }
                        is PackStatus.UpdateAvailable -> {
                            PrimaryActionButton(
                                text = stringResource(R.string.home_update_to, status.latestVersion),
                                onClick = onInstallOrUpdate ?: {}
                            )
                        }
                        else -> {
                            val launchSubText = when (serverConnectivity) {
                                ServerConnectivity.Online -> {
                                    val count = playerCount
                                    if (count != null) stringResource(R.string.home_racers_online, count) else null
                                }
                                ServerConnectivity.Offline -> stringResource(R.string.home_offline)
                                ServerConnectivity.NoInternet -> stringResource(R.string.home_no_internet)
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
                is UiState.NoStorage -> {
                    Text(
                        text = stringResource(R.string.home_storage_not_configured),
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
    vrMultiplier: Float?
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        MiiFace(
            imageBase64 = license.leaderboard?.miiImageBase64,
            miiDataBase64 = license.miiDataBase64,
            modifier = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = license.miiName ?: stringResource(R.string.home_player_default),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                fontFamily = CtmkfFontFamily
            )
            val vr = license.leaderboard?.vr ?: license.vr
            val vrText = if (vr != null && vrMultiplier != null && vrMultiplier > 1.0f) {
                val mult = if (vrMultiplier == vrMultiplier.toInt().toFloat()) {
                    vrMultiplier.toInt().toString()
                } else {
                    vrMultiplier.toString()
                }
                stringResource(R.string.home_vr_active_format, vr, mult)
            } else if (vr != null) {
                stringResource(R.string.home_vr_format, vr)
            } else {
                ""
            }
            if (vrText.isNotEmpty()) {
                Text(
                    text = vrText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ProgressButton(progress: Float, label: String) {
    Column(
        modifier = Modifier
            .widthIn(min = 220.dp)
            .animateContentSize(animationSpec = androidx.compose.animation.core.tween(300)),
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
