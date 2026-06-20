package com.skiletro.wheelwitch.ui.screens.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.skiletro.wheelwitch.R
import com.skiletro.wheelwitch.ui.components.SettingsCategoryHeader
import com.skiletro.wheelwitch.ui.components.SettingsItem
import com.skiletro.wheelwitch.ui.theme.AppTheme
import com.skiletro.wheelwitch.ui.theme.ThemeMode

/**
 * Appearance section: app theme picker and dark-mode picker. Each row
 * opens its own dropdown; selection writes through to the host via
 * [onChangeAppTheme] / [onChangeThemeMode].
 */
@Composable
fun AppearanceSection(
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
