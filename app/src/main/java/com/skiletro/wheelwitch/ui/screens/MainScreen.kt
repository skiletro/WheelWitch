package com.skiletro.wheelwitch.ui.screens

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.skiletro.wheelwitch.R
import com.skiletro.wheelwitch.ui.launchers.AppLaunchers
import com.skiletro.wheelwitch.ui.launchers.BACKUP_SUGGESTED_NAME
import com.skiletro.wheelwitch.ui.launchers.ISO_MIME_TYPES
import com.skiletro.wheelwitch.ui.launchers.buildStoragePermissionIntent
import com.skiletro.wheelwitch.ui.launchers.isExternalStorageManager
import com.skiletro.wheelwitch.ui.launchers.rememberAppLaunchers
import com.skiletro.wheelwitch.ui.theme.AppTheme
import com.skiletro.wheelwitch.ui.theme.ThemeMode
import com.skiletro.wheelwitch.util.DolphinLauncher
import com.skiletro.wheelwitch.util.Prefs
import com.skiletro.wheelwitch.util.PrefsKeys
import com.skiletro.wheelwitch.viewmodel.MiiMakerViewModel
import com.skiletro.wheelwitch.viewmodel.OnlineViewModel
import com.skiletro.wheelwitch.viewmodel.PackUpdateViewModel
import com.skiletro.wheelwitch.viewmodel.SaveDataViewModel

/** Duration of the onboarding fade-in/out crossfade. */
private const val ONBOARDING_TRANSITION_MS = 300

/**
 * Top-level composable: chooses between the onboarding wizard, the
 * quick-launch entry point, and the home/settings overlay stack. Hosts
 * the SAF launchers and the WBFS disclaimer dialog.
 */
@Composable
fun MainScreen(
  quickLaunchFromIntent: Boolean = false,
  packUpdate: PackUpdateViewModel = viewModel(),
  saveData: SaveDataViewModel = viewModel(),
  miiMaker: MiiMakerViewModel = viewModel(),
  onlineViewModel: OnlineViewModel = viewModel(),
  appTheme: AppTheme = AppTheme.Hex,
  onChangeAppTheme: (AppTheme) -> Unit = {},
  themeMode: ThemeMode = ThemeMode.System,
  onChangeThemeMode: (ThemeMode) -> Unit = {},
) {
  val context = LocalContext.current
  val prefs = remember { Prefs.main(context) }
  val onboardingPrefs = remember { Prefs.settings(context) }
  var quickLaunchMode by remember { mutableStateOf(quickLaunchFromIntent) }
  var onboardingComplete by remember {
    mutableStateOf(onboardingPrefs.getBoolean(PrefsKeys.ONBOARDING_COMPLETED_KEY, false))
  }
  var onboardingStorageSelected by remember { mutableStateOf(false) }
  var onboardingIsoSelected by remember { mutableStateOf(false) }
  var showWbfsDisclaimer by remember { mutableStateOf(false) }
  var permissionGranted by remember { mutableStateOf(isExternalStorageManager()) }

  val launchers =
    rememberAppLaunchers(
      context = context,
      packUpdate = packUpdate,
      saveData = saveData,
      onIsoPicked = { path, isWbfs ->
        packUpdate.setGameIsoPath(path)
        onboardingIsoSelected = true
        if (isWbfs) showWbfsDisclaimer = true
      },
      onIsoRejected = {
        Toast.makeText(context, R.string.home_rom_invalid, Toast.LENGTH_SHORT).show()
      },
      onStorageUriPicked = {
        onboardingStorageSelected = true
        permissionGranted = isExternalStorageManager()
      },
    )

  var showSettings by remember { mutableStateOf(false) }

  BackHandler(enabled = showSettings) {
    showSettings = false
  }

  if (showWbfsDisclaimer) {
    AlertDialog(
      onDismissRequest = { showWbfsDisclaimer = false },
      title = { Text(stringResource(R.string.home_rom_wbfs_dialog_title)) },
      text = { Text(stringResource(R.string.home_rom_wbfs_disclaimer)) },
      confirmButton = {
        TextButton(onClick = { showWbfsDisclaimer = false }) {
          Text(stringResource(R.string.action_ok))
        }
      },
    )
  }

  Box(Modifier.fillMaxSize()) {
    if (quickLaunchMode) {
      QuickLaunchScreen(
        viewModel = packUpdate,
        onFinish = { (context as? Activity)?.finish() },
      )
    } else {
      AnimatedContent(
        targetState = onboardingComplete,
        transitionSpec = {
          fadeIn(tween(ONBOARDING_TRANSITION_MS)) togetherWith
            fadeOut(tween(ONBOARDING_TRANSITION_MS))
        },
        label = "onboarding_transition",
      ) { completed ->
        if (completed) {
          // HomeScreen is removed from composition when settings is open (if-guard);
          // SettingsScreen uses AnimatedVisibility so it can slide in/out. The two
          // mechanisms are intentionally different: home disappears instantly to
          // avoid double-draw during the slide animation.
          if (!showSettings) {
            HomeScreen(
              packUpdate = packUpdate,
              saveData = saveData,
              miiMaker = miiMaker,
              onlineViewModel = onlineViewModel,
              onPickIso = { launchers.isoFile.launch(ISO_MIME_TYPES) },
              onOpenSettings = { showSettings = true },
            )
          }

          AnimatedVisibility(
            visible = showSettings,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut(),
          ) {
            SettingsScreen(
              packUpdate = packUpdate,
              saveData = saveData,
              miiMaker = miiMaker,
              onBackupSave = { launchers.backupSave.launch(BACKUP_SUGGESTED_NAME) },
              onRestoreSave = { launchers.restoreSave.launch(ISO_MIME_TYPES) },
              onDeleteSave = { saveData.deleteSave() },
              onClose = { showSettings = false },
              appTheme = appTheme,
              onChangeAppTheme = onChangeAppTheme,
              themeMode = themeMode,
              onChangeThemeMode = onChangeThemeMode,
              onPickIso = { launchers.isoFile.launch(ISO_MIME_TYPES) },
              onSimulateQuickLaunch = { quickLaunchMode = true },
              onRelaunchOnboarding = {
                onboardingPrefs
                  .edit()
                  .putBoolean(PrefsKeys.ONBOARDING_COMPLETED_KEY, false)
                  .apply()
                onboardingComplete = false
                onboardingStorageSelected = false
                onboardingIsoSelected = false
                showSettings = false
              },
            )
          }
        } else {
          OnboardingScreen(
            storageSelected = onboardingStorageSelected,
            isoSelected = onboardingIsoSelected,
            storageConfigured = packUpdate.storageRootPath != null,
            isoConfigured = DolphinLauncher.getGameIsoPath(context) != null,
            miiMaker = miiMaker,
            onPickStorage = { launchers.storageTree.launch(null) },
            onPickIso = { launchers.isoFile.launch(ISO_MIME_TYPES) },
            onSkipIso = { onboardingIsoSelected = true },
            onRequestStoragePermission = {
              launchers.storagePermission.launch(buildStoragePermissionIntent())
            },
            onComplete = {
              onboardingPrefs
                .edit()
                .putBoolean(PrefsKeys.ONBOARDING_COMPLETED_KEY, true)
                .apply()
              onboardingComplete = true
            },
          )
        }
      }
    }
  }
}
