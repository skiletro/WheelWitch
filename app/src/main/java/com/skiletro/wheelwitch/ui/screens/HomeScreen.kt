package com.skiletro.wheelwitch.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.skiletro.wheelwitch.R
import com.skiletro.wheelwitch.model.PackStatus
import com.skiletro.wheelwitch.model.ServerConnectivity
import com.skiletro.wheelwitch.ui.components.PrimaryActionButton
import com.skiletro.wheelwitch.ui.components.TopBar
import com.skiletro.wheelwitch.ui.components.focusBorder
import com.skiletro.wheelwitch.ui.theme.buttonShape
import com.skiletro.wheelwitch.ui.theme.sectionShape
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

/**
 * Bottom bar of the home screen: check-for-updates on the left,
 * status-dependent primary action on the right. The install / launch
 * actions are stubbed for now — they surface a Snackbar saying the
 * flow isn't implemented yet, while the check-for-updates path
 * remains fully functional.
 */
@Composable
private fun HomeBottomBar(
  state: UiState,
  vrMultiplier: Float?,
  playerCount: Int?,
  serverConnectivity: ServerConnectivity,
  isBusy: Boolean,
  onCheck: () -> Unit,
  onRetry: () -> Unit,
) {
  val context = LocalContext.current
  val installNotImplementedMessage = stringResource(R.string.home_install_not_implemented)
  val launchNotImplementedMessage = stringResource(R.string.home_launch_not_implemented)
  var checkButtonFocused by remember { mutableStateOf(false) }

  Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Spacer(modifier = Modifier.weight(1f))

    AnimatedContent(
      targetState = state,
      contentKey = { it::class },
      transitionSpec = {
        (fadeIn(animationSpec = tween(250)) +
            scaleIn(initialScale = 0.92f, animationSpec = tween(250))) togetherWith
          fadeOut(animationSpec = tween(150))
      },
      label = "primary_action",
    ) { currentState ->
      when (currentState) {
        is UiState.Installing -> {
          FilledTonalButton(
            onClick = {},
            enabled = false,
            shape = buttonShape,
            modifier = Modifier.height(56.dp),
          ) {
            Text(
              text = stringResource(R.string.status_installing),
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Medium,
            )
          }
        }
        is UiState.Installed -> {
          // Brief flash before the VM auto-transitions to Checking.
          // Show the same UI as UpToDate.
          FilledTonalButton(
            onClick = {},
            enabled = false,
            shape = buttonShape,
            modifier = Modifier.height(56.dp),
          ) {
            Text(
              text = stringResource(R.string.status_installed),
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Medium,
            )
          }
        }
        is UiState.Checking ->
          FilledTonalButton(
            onClick = {},
            enabled = false,
            shape = buttonShape,
            modifier = Modifier.height(56.dp),
          ) {
            Text(
              text = stringResource(R.string.status_checking),
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Medium,
            )
          }
        is UiState.Error ->
          FilledTonalButton(onClick = onRetry, shape = buttonShape, modifier = Modifier.height(56.dp)) {
            Text(
              text = stringResource(R.string.action_retry),
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Medium,
            )
          }
        is UiState.Ready -> {
          val status = currentState.status
          val checkSubtitle =
            when (status) {
              is PackStatus.UpToDate ->
                stringResource(R.string.home_up_to_date, status.currentVersion)
              is PackStatus.UpdateAvailable ->
                stringResource(
                  R.string.home_update_format,
                  status.currentVersion,
                  status.latestVersion,
                )
              is PackStatus.Installed ->
                stringResource(R.string.home_version_installed, status.version)
              else -> null
            }
          Row(verticalAlignment = Alignment.CenterVertically) {
            FilledTonalButton(
              onClick = onCheck,
              enabled = !isBusy,
              shape = buttonShape,
              colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
              ),
              modifier =
                Modifier.height(56.dp)
                  .onFocusChanged { checkButtonFocused = it.isFocused }
                  .focusBorder(checkButtonFocused),
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                  text = stringResource(R.string.home_check_for_updates),
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Medium,
                )
                if (checkSubtitle != null) {
                  Text(
                    text = checkSubtitle,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Normal,
                  )
                }
              }
            }
            Spacer(modifier = Modifier.width(12.dp))
            when (status) {
              is PackStatus.NotInstalled ->
                PrimaryActionButton(
                  text = stringResource(R.string.action_install),
                  onClick = {
                    Toast.makeText(context, installNotImplementedMessage, Toast.LENGTH_SHORT).show()
                  },
                )
              is PackStatus.UpdateAvailable ->
                PrimaryActionButton(
                  text = stringResource(R.string.home_update_to, status.latestVersion),
                  onClick = {
                    Toast.makeText(context, installNotImplementedMessage, Toast.LENGTH_SHORT).show()
                  },
                )
              else -> {
                val bullet = "\u2022 "
                val launchSubText =
                  when (serverConnectivity) {
                    ServerConnectivity.Online -> {
                      val count = playerCount
                      if (count != null) "$bullet${stringResource(R.string.home_racers_online, count)}"
                      else null
                    }
                    ServerConnectivity.Offline -> "$bullet${stringResource(R.string.home_offline)}"
                    ServerConnectivity.NoInternet ->
                      "$bullet${stringResource(R.string.status_no_internet)}"
                    ServerConnectivity.Unknown -> null
                  }
                PrimaryActionButton(
                  text = stringResource(R.string.home_launch_retro_rewind),
                  onClick = {
                    Toast.makeText(context, launchNotImplementedMessage, Toast.LENGTH_SHORT).show()
                  },
                  subText = launchSubText,
                )
              }
            }
          }
        }
        is UiState.Idle -> {
          FilledTonalButton(
            onClick = onCheck,
            enabled = !isBusy,
            shape = buttonShape,
            modifier = Modifier.height(56.dp),
          ) {
            Text(
              text = stringResource(R.string.home_check_for_updates),
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Medium,
            )
          }
        }
      }
    }
  }
}
