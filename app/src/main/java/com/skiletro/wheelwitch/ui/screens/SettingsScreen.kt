package com.skiletro.wheelwitch.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.skiletro.wheelwitch.R
import com.skiletro.wheelwitch.ui.screens.settings.AboutSection
import com.skiletro.wheelwitch.ui.screens.settings.AdvancedSection
import com.skiletro.wheelwitch.ui.screens.settings.AppearanceSection
import com.skiletro.wheelwitch.ui.screens.settings.LoggingSection
import com.skiletro.wheelwitch.ui.screens.settings.MiiMakerSection
import com.skiletro.wheelwitch.ui.screens.settings.RetroRewindSection
import com.skiletro.wheelwitch.ui.screens.settings.RiivolutionSection
import com.skiletro.wheelwitch.ui.screens.settings.SaveDataSection
import com.skiletro.wheelwitch.ui.theme.AppTheme
import com.skiletro.wheelwitch.ui.theme.ThemeMode
import com.skiletro.wheelwitch.viewmodel.MiiMakerViewModel
import com.skiletro.wheelwitch.viewmodel.PackUpdateViewModel
import com.skiletro.wheelwitch.viewmodel.SaveDataViewModel

/**
 * Top of the settings overlay. Owns the back button, the top bar, the
 * delete-confirmation dialogs, and the [LazyColumn] that lays out the
 * eight category sections. Each section lives in its own file under
 * `settings/` and exposes just the dependencies it needs.
 */
@Composable
fun SettingsScreen(
  packUpdate: PackUpdateViewModel,
  saveData: SaveDataViewModel,
  miiMaker: MiiMakerViewModel,
  onBackupSave: () -> Unit,
  onRestoreSave: () -> Unit,
  onDeleteSave: () -> Unit,
  onClose: () -> Unit,
  appTheme: AppTheme,
  onChangeAppTheme: (AppTheme) -> Unit,
  themeMode: ThemeMode,
  onChangeThemeMode: (ThemeMode) -> Unit,
  onPickIso: () -> Unit,
  onSimulateQuickLaunch: () -> Unit,
  onRelaunchOnboarding: () -> Unit,
) {
  val hasSave by saveData.hasSave.collectAsState()
  val hasWad by miiMaker.hasWad.collectAsState()
  val isInstallingWad by miiMaker.isInstallingWad.collectAsState()
  val miiMakerError by miiMaker.miiMakerError.collectAsState()
  val isoPath by packUpdate.currentIsoPath.collectAsState()
  val gameInfo by packUpdate.gameInfo.collectAsState()
  val myStuffMode by packUpdate.myStuffMode.collectAsState()

  var showDeleteConfirm by remember { mutableStateOf(false) }
  var showWadDeleteConfirm by remember { mutableStateOf(false) }

  if (showDeleteConfirm) {
    AlertDialog(
      onDismissRequest = { showDeleteConfirm = false },
      title = { Text(stringResource(R.string.settings_delete_save_dialog_title)) },
      text = { Text(stringResource(R.string.settings_delete_save_dialog_body)) },
      confirmButton = {
        Button(onClick = { onDeleteSave(); showDeleteConfirm = false }) {
          Text(stringResource(R.string.settings_delete))
        }
      },
      dismissButton = {
        TextButton(onClick = { showDeleteConfirm = false }) {
          Text(stringResource(R.string.action_cancel))
        }
      },
    )
  }

  if (showWadDeleteConfirm) {
    AlertDialog(
      onDismissRequest = { showWadDeleteConfirm = false },
      title = { Text(stringResource(R.string.settings_delete_wad_dialog_title)) },
      text = { Text(stringResource(R.string.settings_delete_wad_dialog_body)) },
      confirmButton = {
        Button(onClick = { miiMaker.deleteWad(); showWadDeleteConfirm = false }) {
          Text(stringResource(R.string.settings_delete))
        }
      },
      dismissButton = {
        TextButton(onClick = { showWadDeleteConfirm = false }) {
          Text(stringResource(R.string.action_cancel))
        }
      },
    )
  }

  Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 16.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      IconButton(onClick = onClose) {
        Icon(
          imageVector = ImageVector.vectorResource(R.drawable.ic_arrow_back),
          contentDescription = stringResource(R.string.cd_back),
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = stringResource(R.string.settings_title),
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
      )
    }

    LazyColumn(
      modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
      item {
        SaveDataSection(
          hasSave = hasSave,
          onBackupSave = onBackupSave,
          onRestoreSave = onRestoreSave,
          onRequestDelete = { showDeleteConfirm = true },
        )
      }
      item {
        AppearanceSection(
          appTheme = appTheme,
          onChangeAppTheme = onChangeAppTheme,
          themeMode = themeMode,
          onChangeThemeMode = onChangeThemeMode,
        )
      }
      item {
        RetroRewindSection(
          isoPath = isoPath,
          gameInfo = gameInfo,
          storageRootPath = packUpdate.storageRootPath,
          onPickIso = onPickIso,
          onClearIso = { packUpdate.clearIsoPath() },
        )
      }
      item {
        RiivolutionSection(
          myStuffMode = myStuffMode,
          onSetMode = packUpdate::setMyStuffMode,
        )
      }
      item {
        MiiMakerSection(
          hasWad = hasWad,
          isInstallingWad = isInstallingWad,
          miiMakerError = miiMakerError,
          onInstall = miiMaker::installMiiMakerWad,
          onRequestDelete = { showWadDeleteConfirm = true },
        )
      }
      item { LoggingSection() }
      item {
        AdvancedSection(
          onSimulateQuickLaunch = onSimulateQuickLaunch,
          onRelaunchOnboarding = onRelaunchOnboarding,
        )
      }
      item { AboutSection() }
      item { Spacer(modifier = Modifier.height(24.dp)) }
    }
  }
}
