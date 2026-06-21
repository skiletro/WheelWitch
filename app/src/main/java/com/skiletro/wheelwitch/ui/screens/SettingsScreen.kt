package com.skiletro.wheelwitch.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.skiletro.wheelwitch.BuildConfig
import com.skiletro.wheelwitch.R
import com.skiletro.wheelwitch.ui.components.SettingsCategoryHeader
import com.skiletro.wheelwitch.ui.components.SettingsItem
import com.skiletro.wheelwitch.ui.theme.AppTheme
import com.skiletro.wheelwitch.ui.theme.ThemeMode
import com.skiletro.wheelwitch.util.launcher.BugReportLauncher
import com.skiletro.wheelwitch.util.mii.MiiFaceCache
import com.skiletro.wheelwitch.util.prefs.Prefs
import com.skiletro.wheelwitch.util.prefs.PrefsKeys
import com.skiletro.wheelwitch.util.io.cacheSize
import com.skiletro.wheelwitch.util.formatBytes
import com.skiletro.wheelwitch.viewmodel.MiiMakerViewModel

/**
 * Top of the settings overlay. Save Data / Retro Rewind / Riivolution
 * sections have been removed along with the install / launch / save
 * data flow. Sections, in order: Appearance, Mii Maker, Logging,
 * Advanced, About.
 */
@Composable
fun SettingsScreen(
  miiMaker: MiiMakerViewModel,
  onClose: () -> Unit,
  appTheme: AppTheme,
  onChangeAppTheme: (AppTheme) -> Unit,
  themeMode: ThemeMode,
  onChangeThemeMode: (ThemeMode) -> Unit,
  onRelaunchOnboarding: () -> Unit,
) {
  val hasWad by miiMaker.hasWad.collectAsState()
  val isInstallingWad by miiMaker.isInstallingWad.collectAsState()
  val miiMakerError by miiMaker.miiMakerError.collectAsState()

  var showWadDeleteConfirm by remember { mutableStateOf(false) }

  if (showWadDeleteConfirm) {
    AlertDialog(
      onDismissRequest = { showWadDeleteConfirm = false },
      title = { Text(stringResource(R.string.settings_delete_wad_dialog_title)) },
      text = { Text(stringResource(R.string.settings_delete_wad_dialog_body)) },
      confirmButton = {
        Button(
          onClick = { miiMaker.deleteWad(); showWadDeleteConfirm = false },
        ) {
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
        AppearanceSection(
          appTheme = appTheme,
          onChangeAppTheme = onChangeAppTheme,
          themeMode = themeMode,
          onChangeThemeMode = onChangeThemeMode,
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
          onRelaunchOnboarding = onRelaunchOnboarding,
        )
      }
      item { AboutSection() }
      item { Spacer(modifier = Modifier.height(24.dp)) }
    }
  }
}

/** Appearance section: app theme picker and dark-mode picker. */
@Composable
private fun AppearanceSection(
  appTheme: AppTheme,
  onChangeAppTheme: (AppTheme) -> Unit,
  themeMode: ThemeMode,
  onChangeThemeMode: (ThemeMode) -> Unit,
) {
  SettingsCategoryHeader(stringResource(R.string.settings_appearance))
  var showAppThemeDropdown by remember { mutableStateOf(false) }
  var showThemeDropdown by remember { mutableStateOf(false) }
  val appThemeLabel = stringResource(appTheme.labelRes)
  SettingsItem(
    icon = ImageVector.vectorResource(R.drawable.ic_palette),
    title = stringResource(R.string.settings_app_theme),
    summary = appThemeLabel,
    trailing = {
      Box {
        TextButton(
          onClick = { showAppThemeDropdown = true },
          shape = RoundedCornerShape(14.dp),
        ) {
          Text(text = appThemeLabel)
        }
        DropdownMenu(
          expanded = showAppThemeDropdown,
          onDismissRequest = { showAppThemeDropdown = false },
        ) {
          AppTheme.entries.forEach { theme ->
            DropdownMenuItem(
              text = { Text(stringResource(theme.labelRes)) },
              onClick = {
                onChangeAppTheme(theme)
                showAppThemeDropdown = false
              },
            )
          }
        }
      }
    },
  )
  SettingsItem(
    icon = ImageVector.vectorResource(R.drawable.ic_nightlight),
    title = stringResource(R.string.settings_dark_mode),
    summary =
      when (themeMode) {
        ThemeMode.Light -> stringResource(R.string.settings_always_light)
        ThemeMode.Dark -> stringResource(R.string.settings_always_dark)
        ThemeMode.Oled -> stringResource(R.string.settings_oled)
        ThemeMode.System -> stringResource(R.string.settings_follow_system)
      },
    trailing = {
      Box {
        TextButton(
          onClick = { showThemeDropdown = true },
          shape = RoundedCornerShape(14.dp),
        ) {
          Text(
            text =
              when (themeMode) {
                ThemeMode.Light -> stringResource(R.string.settings_theme_light)
                ThemeMode.Dark -> stringResource(R.string.settings_theme_dark)
                ThemeMode.Oled -> stringResource(R.string.settings_theme_oled)
                ThemeMode.System -> stringResource(R.string.settings_theme_system)
              }
          )
        }
        DropdownMenu(
          expanded = showThemeDropdown,
          onDismissRequest = { showThemeDropdown = false },
        ) {
          ThemeMode.entries.forEach { mode ->
            DropdownMenuItem(
              text = {
                Text(
                  when (mode) {
                    ThemeMode.Light -> stringResource(R.string.settings_theme_light)
                    ThemeMode.Dark -> stringResource(R.string.settings_theme_dark)
                    ThemeMode.Oled -> stringResource(R.string.settings_theme_oled)
                    ThemeMode.System -> stringResource(R.string.settings_theme_system)
                  }
                )
              },
              onClick = {
                onChangeThemeMode(mode)
                showThemeDropdown = false
              },
            )
          }
        }
      }
    },
  )
}

/** Mii Channel WAD section: install or delete the cached WAD. */
@Composable
private fun MiiMakerSection(
  hasWad: Boolean,
  isInstallingWad: Boolean,
  miiMakerError: String?,
  onInstall: () -> Unit,
  onRequestDelete: () -> Unit,
) {
  SettingsCategoryHeader(stringResource(R.string.settings_mii_maker_section))
  val wadStatus =
    if (hasWad) stringResource(R.string.status_installed)
    else stringResource(R.string.status_not_installed)
  SettingsItem(
    icon = ImageVector.vectorResource(R.drawable.ic_face_up),
    title = stringResource(R.string.settings_mii_channel_wad),
    summary = miiMakerError ?: wadStatus,
    summaryColor =
      if (miiMakerError != null) MaterialTheme.colorScheme.error
      else MaterialTheme.colorScheme.onSurfaceVariant,
    trailing = {
      when {
        isInstallingWad -> {
          Text(
            stringResource(R.string.settings_installing),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
          )
        }
        hasWad -> {
          TextButton(onClick = onRequestDelete) {
            Text(stringResource(R.string.settings_delete), color = MaterialTheme.colorScheme.error)
          }
        }
        else -> {
          Button(
            onClick = onInstall,
            shape = RoundedCornerShape(14.dp),
            contentPadding = ButtonDefaults.TextButtonContentPadding,
          ) {
            Text(stringResource(R.string.action_install))
          }
        }
      }
    },
  )
}

/** Logging section: toggle the on-disk log file and launch the bug-report chooser. */
@Composable
private fun LoggingSection() {
  val context = LocalContext.current
  val loggingPrefs = remember { Prefs.main(context) }
  SettingsCategoryHeader(stringResource(R.string.settings_logging))
  var loggingToFile by remember {
    mutableStateOf(loggingPrefs.getBoolean(PrefsKeys.LOGGING_TO_FILE_KEY, false))
  }
  SettingsItem(
    icon = ImageVector.vectorResource(R.drawable.ic_save),
    title = stringResource(R.string.settings_logging_to_file),
    summary = stringResource(R.string.settings_logging_to_file_sub),
    trailing = {
      Switch(
        checked = loggingToFile,
        onCheckedChange = { enabled ->
          loggingToFile = enabled
          loggingPrefs.edit().putBoolean(PrefsKeys.LOGGING_TO_FILE_KEY, enabled).apply()
        },
      )
    },
  )
  SettingsItem(
    icon = ImageVector.vectorResource(R.drawable.ic_bug_report),
    title = stringResource(R.string.settings_report_bug),
    summary = stringResource(R.string.settings_report_bug_sub),
    trailing = {
      Button(
        onClick = { BugReportLauncher.launch(context) },
        shape = RoundedCornerShape(14.dp),
        contentPadding = ButtonDefaults.TextButtonContentPadding,
        colors = ButtonDefaults.filledTonalButtonColors(
          containerColor = MaterialTheme.colorScheme.secondaryContainer,
          contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
      ) {
        Text(stringResource(R.string.settings_report), fontWeight = FontWeight.Medium)
      }
    },
  )
}

/** Advanced section: Mii face cache row + relaunch-onboarding escape hatch. */
@Composable
private fun AdvancedSection(
  onRelaunchOnboarding: () -> Unit,
) {
  SettingsCategoryHeader(stringResource(R.string.settings_advanced))
  MiiCacheRow()
  RelaunchOnboardingRow(onRelaunchOnboarding)
}

@Composable
private fun MiiCacheRow() {
  var miiCacheSizeBytes by remember { mutableStateOf(MiiFaceCache.cacheSize()) }
  SettingsItem(
    icon = ImageVector.vectorResource(R.drawable.ic_cached),
    title = stringResource(R.string.settings_mii_face_cache),
    summary = formatBytes(miiCacheSizeBytes),
    trailing = {
      TextButton(
        onClick = {
          MiiFaceCache.clear()
          miiCacheSizeBytes = 0
        },
        enabled = miiCacheSizeBytes > 0,
        shape = RoundedCornerShape(14.dp),
      ) {
        Text(
          text = stringResource(R.string.settings_clear),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    },
  )
  Spacer(modifier = Modifier.height(4.dp))
}

@Composable
private fun RelaunchOnboardingRow(onRelaunch: () -> Unit) {
  SettingsItem(
    icon = ImageVector.vectorResource(R.drawable.ic_exit_to_app),
    title = stringResource(R.string.settings_onboarding),
    summary = stringResource(R.string.settings_relaunch_onboarding),
    trailing = {
      TextButton(onClick = onRelaunch, shape = RoundedCornerShape(14.dp)) {
        Text(stringResource(R.string.settings_relaunch))
      }
    },
  )
}

/** About section: app name + version + tagline. */
@Composable
private fun AboutSection() {
  SettingsCategoryHeader(stringResource(R.string.settings_about))
  val version =
    if (BuildConfig.DEBUG)
      stringResource(
        R.string.settings_version_debug,
        BuildConfig.VERSION_NAME,
        BuildConfig.GIT_HASH,
      )
    else stringResource(R.string.settings_version_release, BuildConfig.VERSION_NAME)
  SettingsItem(
    icon = ImageVector.vectorResource(R.drawable.ic_info),
    title = stringResource(R.string.settings_wheel_witch),
    summary =
      stringResource(
        R.string.settings_about_summary,
        version,
        stringResource(R.string.settings_app_subtitle),
      ),
    trailing = null,
  )
}
