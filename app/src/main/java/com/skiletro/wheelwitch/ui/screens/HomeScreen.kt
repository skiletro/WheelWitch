package com.skiletro.wheelwitch.ui.screens

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
import android.widget.Toast
import com.skiletro.wheelwitch.R
import com.skiletro.wheelwitch.model.ServerConnectivity
import com.skiletro.wheelwitch.ui.components.TopBar
import com.skiletro.wheelwitch.ui.components.focusBorder
import com.skiletro.wheelwitch.ui.components.sectionShape
import com.skiletro.wheelwitch.ui.screens.home.HomeBottomBar
import com.skiletro.wheelwitch.viewmodel.MiiMakerViewModel
import com.skiletro.wheelwitch.viewmodel.OnlineViewModel
import com.skiletro.wheelwitch.viewmodel.PackUpdateViewModel
import com.skiletro.wheelwitch.viewmodel.RoomsState
import com.skiletro.wheelwitch.viewmodel.UiState

/**
 * Top-level home screen: top bar, version-history card, and the
 * check-for-updates / install / launch bottom bar. The save-data
 * "Licenses" top-bar entry still routes to a stubbed [SaveInfoScreen].
 */
@Composable
fun HomeScreen(
  packUpdate: PackUpdateViewModel,
  miiMaker: MiiMakerViewModel,
  onlineViewModel: OnlineViewModel,
  onOpenSettings: () -> Unit,
) {
  val state by packUpdate.state.collectAsState()
  val hasWad by miiMaker.hasWad.collectAsState()
  val roomsState by onlineViewModel.roomsState.collectAsState()
  val vrMultiplier by onlineViewModel.vrMultiplier.collectAsState()

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

  val context = LocalContext.current
  LaunchedEffect(state) {
    if (state is UiState.Error) {
      val error = (state as UiState.Error).message
      Toast.makeText(context, error, Toast.LENGTH_LONG).show()
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
              onOpenSaveInfo = { showSaveInfo = true },
            )
          }
        },
        bottomBar = {
          Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
            Column(modifier = Modifier.padding(vertical = 12.dp)) {
              HomeBottomBar(
                state = state,
                vrMultiplier = vrMultiplier,
                playerCount = playerCount,
                serverConnectivity = serverConnectivity,
                isBusy = false,
                onCheck = { packUpdate.checkStatus() },
                onRetry = { packUpdate.clearError() },
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
      SaveInfoScreen(onClose = { showSaveInfo = false })
    }
  }
}
