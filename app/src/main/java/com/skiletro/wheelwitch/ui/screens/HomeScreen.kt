package com.skiletro.wheelwitch.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.skiletro.wheelwitch.R
import com.skiletro.wheelwitch.model.LicenseInfo
import com.skiletro.wheelwitch.model.PackStatus
import com.skiletro.wheelwitch.model.SemVersion
import com.skiletro.wheelwitch.model.ServerConnectivity
import com.skiletro.wheelwitch.ui.components.ActivePlayerCard
import com.skiletro.wheelwitch.ui.components.PrimaryActionButton
import com.skiletro.wheelwitch.ui.components.ProgressButton
import com.skiletro.wheelwitch.ui.components.TopBar
import com.skiletro.wheelwitch.ui.components.focusBorder
import com.skiletro.wheelwitch.ui.theme.buttonShape
import com.skiletro.wheelwitch.util.io.DownloadProgress
import com.skiletro.wheelwitch.util.launcher.DolphinLauncher
import com.skiletro.wheelwitch.viewmodel.MiiMakerViewModel
import com.skiletro.wheelwitch.viewmodel.OnlineViewModel
import com.skiletro.wheelwitch.viewmodel.PackUpdateViewModel
import com.skiletro.wheelwitch.viewmodel.RoomsState
import com.skiletro.wheelwitch.viewmodel.SaveDataViewModel
import com.skiletro.wheelwitch.viewmodel.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Top-level home screen: top bar, version-history placeholder, and
 * the check-for-updates / install / launch bottom bar. The save-data
 * "Licenses" top-bar entry opens [SaveInfoScreen].
 */
@Composable
fun HomeScreen(
  packUpdate: PackUpdateViewModel,
  miiMaker: MiiMakerViewModel,
  onlineViewModel: OnlineViewModel,
  saveData: SaveDataViewModel,
  onOpenSettings: () -> Unit,
) {
  val state by packUpdate.state.collectAsState()
  val installProgress by packUpdate.installProgress.collectAsState()
  val hasWad by miiMaker.hasWad.collectAsState()
  val roomsState by onlineViewModel.roomsState.collectAsState()
  val activeLicense by saveData.activeLicense.collectAsState()
  val cachedLeaderboardVrs by saveData.cachedLeaderboardVrs.collectAsState()

  val playerCount = (roomsState as? RoomsState.Success)?.playerCount
  val serverConnectivity =
    (roomsState as? RoomsState.Success)?.serverConnectivity ?: ServerConnectivity.Unknown

  val lifecycleOwner = LocalLifecycleOwner.current
  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        onlineViewModel.fetchRooms()
        saveData.refreshIfStale()
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  var showOnlineMenu by remember { mutableStateOf(false) }
  var showSaveInfo by remember { mutableStateOf(false) }
  var showExitDialog by remember { mutableStateOf(false) }

  BackHandler(enabled = !(showOnlineMenu || showSaveInfo)) {
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
  val errorMessage = (state as? UiState.Error)?.message
  LaunchedEffect(errorMessage) {
    val message = errorMessage
    if (message != null) {
      Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
  }

  Box(modifier = Modifier.fillMaxSize()) {
    val scope = rememberCoroutineScope()
    val launchDolphinNotInstalled = stringResource(R.string.home_launch_dolphin_not_installed)
    val launchNoRom = stringResource(R.string.home_launch_no_rom)
    val launchFallback = stringResource(R.string.home_launch_fallback)
    val launchStorageNotConfigured = stringResource(R.string.error_storage_not_configured)

    if (!(showOnlineMenu || showSaveInfo)) {
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
                installProgress = installProgress,
                activeLicense = activeLicense,
                cachedLeaderboardVrs = cachedLeaderboardVrs,
                playerCount = playerCount,
                serverConnectivity = serverConnectivity,
                isBusy = state is UiState.Installing,
                onCheck = { packUpdate.checkStatus() },
                onRetry = { packUpdate.clearError() },
                onInstall = { packUpdate.installLatest() },
                onUpdate = { packUpdate.update() },
                onLaunch = {
                  scope.launch {
                    val result =
                      withContext(Dispatchers.IO) {
                        DolphinLauncher.launchRetroRewind(context)
                      }
                    val message =
                      when (result) {
                        is DolphinLauncher.LaunchResult.AutoStarted -> null
                        is DolphinLauncher.LaunchResult.FallbackStarted ->
                          launchFallback
                        DolphinLauncher.LaunchResult.DolphinNotInstalled ->
                          launchDolphinNotInstalled
                        DolphinLauncher.LaunchResult.StorageNotConfigured ->
                          launchStorageNotConfigured
                        DolphinLauncher.LaunchResult.NoRom -> launchNoRom
                      }
                    if (message != null) {
                      Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    }
                  }
                },
              )
            }
          }
        },
      ) { padding ->
        Box(
          modifier =
            Modifier.fillMaxSize()
              .background(MaterialTheme.colorScheme.background)
              .padding(padding),
          contentAlignment = Alignment.Center,
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = stringResource(R.string.home_version_history_coming_soon),
              style = MaterialTheme.typography.headlineSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = stringResource(R.string.home_version_history_coming_soon_body),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center,
            )
          }
        }
      }
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
      SaveInfoScreen(viewModel = saveData, onClose = { showSaveInfo = false })
    }
  }
}

/**
 * Bottom bar of the home screen: check-for-updates on the left,
 * status-dependent primary action on the right. The install / update
 * actions call into [PackUpdateViewModel]; the launch action calls
 * [DolphinLauncher.launchRetroRewind] which handles the full
 * pre-launch sequence (Dolphin.ini upsert + launch descriptor write
 * + intent + fallback) and reports via [DolphinLauncher.LaunchResult].
 *
 * [installProgress] is read from
 * [PackUpdateViewModel.installProgress] so the [ProgressButton] only
 * recomposes when the per-byte download progress changes; the
 * [AnimatedContent] content lambda still re-dispatches on every
 * state class transition, but the `Installing.Downloading` branch is
 * extracted into [DownloadingProgressButton] so the rest of the UI
 * does not pay the recomposition cost on each tick.
 */
@Composable
private fun HomeBottomBar(
  state: UiState,
  installProgress: DownloadProgress?,
  activeLicense: LicenseInfo?,
  cachedLeaderboardVrs: Map<Int, Int>,
  playerCount: Int?,
  serverConnectivity: ServerConnectivity,
  isBusy: Boolean,
  onCheck: () -> Unit,
  onRetry: () -> Unit,
  onInstall: () -> Unit,
  onUpdate: () -> Unit,
  onLaunch: () -> Unit,
) {
  var checkButtonFocused by remember { mutableStateOf(false) }
  var skipInitialTransition by remember { mutableStateOf(true) }

  Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    if (activeLicense != null && state !is UiState.Installing) {
      ActivePlayerCard(
        license = activeLicense,
        cachedLeaderboardVr = cachedLeaderboardVrs[activeLicense.slotIndex],
      )
    }

    Spacer(modifier = Modifier.weight(1f))

    AnimatedContent(
      targetState = state,
      contentKey = { it::class },
      transitionSpec = {
        if (skipInitialTransition) {
          EnterTransition.None togetherWith ExitTransition.None
        } else {
          (fadeIn(animationSpec = tween(250)) +
              scaleIn(initialScale = 0.92f, animationSpec = tween(250))) togetherWith
            fadeOut(animationSpec = tween(150))
        }
      },
      label = "primary_action",
    ) { currentState ->
      when (currentState) {
        is UiState.Installing.Downloading -> {
          // Hot path: a new DownloadProgress event flows in here ~5x
          // per second. Extracted into its own composable so the
          // rest of the bar (stringResource lookups, sibling
          // branches) does not recompose.
          DownloadingProgressButton(installProgress ?: currentState.progress)
        }
        is UiState.Installing.Extracting ->
          ExtractingProgressButton(
            filesDone = currentState.filesDone,
            filesTotal = currentState.filesTotal,
            currentFile = currentState.currentFile,
            phase = currentState.phase,
          )
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
        is UiState.Checking -> {
          val previous = currentState.previousStatus
          when {
            previous == null ->
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
            previous is PackStatus.CheckFailed ->
              CheckFailedButton(
                installed = previous.installedVersion,
                onCheck = onCheck,
                enabled = false,
                showSpinner = true,
                checkButtonFocused = checkButtonFocused,
                onFocusChanged = { checkButtonFocused = it },
              )
            else ->
              StatusRow(
                status = previous,
                isChecking = true,
                isBusy = isBusy,
                serverConnectivity = serverConnectivity,
                playerCount = playerCount,
                checkButtonFocused = checkButtonFocused,
                onFocusChanged = { checkButtonFocused = it },
                onCheck = onCheck,
                onInstall = onInstall,
                onUpdate = onUpdate,
                onLaunch = onLaunch,
              )
          }
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
          if (status is PackStatus.CheckFailed) {
            CheckFailedButton(
              installed = status.installedVersion,
              onCheck = onCheck,
              enabled = !isBusy,
              showSpinner = false,
              checkButtonFocused = checkButtonFocused,
              onFocusChanged = { checkButtonFocused = it },
            )
          } else {
            StatusRow(
              status = status,
              isChecking = false,
              isBusy = isBusy,
              serverConnectivity = serverConnectivity,
              playerCount = playerCount,
              checkButtonFocused = checkButtonFocused,
              onFocusChanged = { checkButtonFocused = it },
              onCheck = onCheck,
              onInstall = onInstall,
              onUpdate = onUpdate,
              onLaunch = onLaunch,
            )
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
  LaunchedEffect(Unit) { skipInitialTransition = false }
}

/**
 * Full-width error-styled "couldn't check for updates" button. Used
 * both by [UiState.Ready] when the last check failed (clickable) and
 * by [UiState.Checking] when a re-check is in flight after a failure
 * (disabled, with a small spinner).
 */
@Composable
private fun CheckFailedButton(
  installed: SemVersion?,
  onCheck: () -> Unit,
  enabled: Boolean,
  showSpinner: Boolean,
  checkButtonFocused: Boolean,
  onFocusChanged: (Boolean) -> Unit,
) {
  val title = stringResource(R.string.home_check_failed)
  val subtitle =
    installed?.let { stringResource(R.string.home_check_failed_installed_format, it) }
  FilledTonalButton(
    onClick = onCheck,
    enabled = enabled,
    shape = buttonShape,
    colors =
      ButtonDefaults.filledTonalButtonColors(
        containerColor = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
      ),
    modifier =
      Modifier.height(56.dp)
        .onFocusChanged { onFocusChanged(it.isFocused) }
        .focusBorder(checkButtonFocused),
  ) {
    if (showSpinner) {
      androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(
          modifier = Modifier.size(20.dp),
          color = MaterialTheme.colorScheme.onErrorContainer,
          strokeWidth = 2.dp,
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Medium,
        )
      }
    } else {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Medium,
        )
        if (subtitle != null) {
          Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Normal,
          )
        }
      }
    }
  }
}

/**
 * The "check + primary action" row used by both [UiState.Ready] and
 * [UiState.Checking] (when a previous [PackStatus] is known). When
 * [isChecking] is true, the left button swaps to a 20dp spinner +
 * "Checking..." and the right primary action is disabled. The
 * [checkButtonFocused] state is hoisted to [HomeBottomBar] so the
 * focus border persists across [UiState] transitions.
 */
@Composable
private fun StatusRow(
  status: PackStatus,
  isChecking: Boolean,
  isBusy: Boolean,
  serverConnectivity: ServerConnectivity,
  playerCount: Int?,
  checkButtonFocused: Boolean,
  onFocusChanged: (Boolean) -> Unit,
  onCheck: () -> Unit,
  onInstall: () -> Unit,
  onUpdate: () -> Unit,
  onLaunch: () -> Unit,
) {
  val checkSubtitle =
    if (isChecking) {
      null
    } else {
      when (status) {
        is PackStatus.UpToDate ->
          stringResource(R.string.home_up_to_date, status.currentVersion)
        is PackStatus.UpdateAvailable ->
          stringResource(
            R.string.home_update_format,
            status.currentVersion,
            status.latestVersion,
          )
        else -> null
      }
    }
  val rightEnabled = !isBusy && !isChecking
  val leftEnabled = !isBusy && !isChecking

  Row(verticalAlignment = Alignment.CenterVertically) {
    FilledTonalButton(
      onClick = onCheck,
      enabled = leftEnabled,
      shape = buttonShape,
      colors =
        ButtonDefaults.filledTonalButtonColors(
          containerColor = MaterialTheme.colorScheme.secondaryContainer,
          contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
      modifier =
        Modifier.height(56.dp)
          .onFocusChanged { onFocusChanged(it.isFocused) }
          .focusBorder(checkButtonFocused),
    ) {
      if (isChecking) {
        androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
          CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            strokeWidth = 2.dp,
          )
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = stringResource(R.string.status_checking),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
          )
        }
      } else {
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
    }
    Spacer(modifier = Modifier.width(12.dp))
    when (status) {
      is PackStatus.NotInstalled ->
        PrimaryActionButton(
          text = stringResource(R.string.action_install),
          onClick = onInstall,
          enabled = rightEnabled,
        )
      is PackStatus.UpdateAvailable ->
        PrimaryActionButton(
          text = stringResource(R.string.home_update_to, status.latestVersion),
          onClick = onUpdate,
          enabled = rightEnabled,
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
          onClick = onLaunch,
          enabled = rightEnabled,
          subText = launchSubText,
        )
      }
    }
  }
}

/**
 * Determinate progress bar shown while a pack zip is being downloaded.
 * Owns its own [animateFloatAsState] so the 1%-throttled progress
 * events from the downloader animate smoothly. Recomposes on every
 * progress tick; siblings of this composable in [HomeBottomBar] are
 * not affected because they live outside this composable's scope.
 */
@Composable
private fun DownloadingProgressButton(progress: DownloadProgress) {
  val animatedFraction by
    animateFloatAsState(
      targetValue = progress.progress.coerceIn(0f, 1f),
      animationSpec = tween(durationMillis = 200),
      label = "install_progress",
    )
  ProgressButton(
    progress = animatedFraction,
    label = stringResource(R.string.status_installing),
    bytesPerSecond = progress.bytesPerSecond,
    bytesDownloaded = progress.bytesDownloaded,
    totalBytes = progress.totalBytes,
    filesDone = 0,
    filesTotal = 0,
    currentFile = null,
  )
}

/**
 * Determinate progress bar shown while a pack zip is being extracted
 * into the SAF tree. Fraction is the file-count basis; the
 * current-file name is shown alongside the bar.
 */
@Composable
private fun ExtractingProgressButton(
  filesDone: Int,
  filesTotal: Int,
  currentFile: String?,
  phase: com.skiletro.wheelwitch.data.ExtractingPhase,
) {
  val fraction = if (filesTotal <= 0) 0f else filesDone.toFloat() / filesTotal.toFloat()
  val animatedFraction by
    animateFloatAsState(
      targetValue = fraction.coerceIn(0f, 1f),
      animationSpec = tween(durationMillis = 200),
      label = "extract_progress",
    )
  val label =
    when (phase) {
      com.skiletro.wheelwitch.data.ExtractingPhase.PreparingFolders ->
        stringResource(R.string.status_extracting_preparing_folders)
      com.skiletro.wheelwitch.data.ExtractingPhase.WritingFiles ->
        stringResource(R.string.status_extracting)
    }
  ProgressButton(
    progress = animatedFraction,
    label = label,
    bytesPerSecond = 0L,
    bytesDownloaded = 0L,
    totalBytes = 0L,
    filesDone = filesDone,
    filesTotal = filesTotal,
    currentFile = currentFile,
  )
}
