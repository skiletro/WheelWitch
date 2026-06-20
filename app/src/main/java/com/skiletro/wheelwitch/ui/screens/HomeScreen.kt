package com.skiletro.wheelwitch.ui.screens

import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.skiletro.wheelwitch.R
import com.skiletro.wheelwitch.model.PackStatus
import com.skiletro.wheelwitch.model.ServerConnectivity
import com.skiletro.wheelwitch.ui.components.TopBar
import com.skiletro.wheelwitch.ui.components.focusBorder
import com.skiletro.wheelwitch.ui.components.sectionShape
import com.skiletro.wheelwitch.ui.screens.home.HomeBottomBar
import com.skiletro.wheelwitch.viewmodel.MiiMakerViewModel
import com.skiletro.wheelwitch.viewmodel.OnlineViewModel
import com.skiletro.wheelwitch.viewmodel.PackUpdateViewModel
import com.skiletro.wheelwitch.viewmodel.RoomsState
import com.skiletro.wheelwitch.viewmodel.SaveDataViewModel
import com.skiletro.wheelwitch.viewmodel.UiState

/**
 * Top-level home screen: top bar, version-history card, optional
 * bottom bar with pack install state. The three overlay screens
 * (OnlineMenu, SaveInfo, Changelog) animate in on top via
 * [AnimatedVisibility].
 */
@Composable
fun HomeScreen(
  packUpdate: PackUpdateViewModel,
  saveData: SaveDataViewModel,
  miiMaker: MiiMakerViewModel,
  onlineViewModel: OnlineViewModel,
  onPickIso: () -> Unit,
  onOpenSettings: () -> Unit,
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
      },
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

  val onInstallOrUpdate: (() -> Unit)? =
    packStatus?.let { s ->
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
          Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
            TopBar(
              onOpenSettings = onOpenSettings,
              onLaunchMiiMaker = { miiMaker.launchMiiMaker() },
              miiMakerEnabled = hasWad,
              onOpenOnlineMenu = { showOnlineMenu = true },
              onlineMenuEnabled = serverConnectivity is ServerConnectivity.Online,
              onOpenSaveInfo = {
                saveData.refreshSaveFileInfo()
                showSaveInfo = true
              },
            )
          }
        },
        bottomBar = {
          Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
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
                onPickIso = onPickIso,
              )
            }
          }
        },
      ) { padding ->
        var changelogFocused by remember { mutableStateOf(false) }
        Box(
          modifier =
            Modifier.fillMaxSize()
              .background(MaterialTheme.colorScheme.background)
              .padding(padding)
              .clickable { showChangelog = true }
              .focusable()
              .onFocusChanged { changelogFocused = it.isFocused }
              .focusBorder(changelogFocused, sectionShape),
          contentAlignment = Alignment.Center,
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
      exit = slideOutVertically() + fadeOut(),
    ) {
      ChangelogDetailScreen(onClose = { showChangelog = false })
    }

    AnimatedVisibility(
      visible = showOnlineMenu,
      enter = slideInVertically() + fadeIn(),
      exit = slideOutVertically() + fadeOut(),
    ) {
      OnlineMenuScreen(viewModel = onlineViewModel, onClose = { showOnlineMenu = false })
    }

    AnimatedVisibility(
      visible = showSaveInfo,
      enter = slideInVertically() + fadeIn(),
      exit = slideOutVertically() + fadeOut(),
    ) {
      SaveInfoScreen(
        saveInfoState = saveInfoState,
        selectedSlotIndex = selectedSlotIndex,
        cachedLeaderboardVrs = cachedLeaderboardVrs,
        onSelectSlot = { saveData.selectSlot(it) },
        onRefresh = { saveData.refreshSaveFileInfo() },
        onClose = { showSaveInfo = false },
      )
    }
  }
}
