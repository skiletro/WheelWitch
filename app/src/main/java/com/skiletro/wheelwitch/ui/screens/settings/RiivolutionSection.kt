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
import com.skiletro.wheelwitch.util.DolphinLauncher

/**
 * Riivolution "My Stuff" mode picker. The selected mode controls which
 * custom content Riivolution loads on launch; the choice is persisted
 * by the host via [onSetMode].
 */
@Composable
fun RiivolutionSection(myStuffMode: DolphinLauncher.MyStuffMode, onSetMode: (DolphinLauncher.MyStuffMode) -> Unit) {
  SettingsCategoryHeader(stringResource(R.string.settings_riivolution_options))
  var showDropdown by remember { mutableStateOf(false) }
  SettingsItem(
    icon = ImageVector.vectorResource(R.drawable.ic_folder),
    title = stringResource(R.string.settings_my_stuff),
    summary = stringResource(R.string.settings_my_stuff_summary),
    trailing = {
      Box {
        TextButton(onClick = { showDropdown = true }, shape = RoundedCornerShape(14.dp)) {
          Text(
            text =
              when (myStuffMode) {
                DolphinLauncher.MyStuffMode.Disabled -> stringResource(R.string.settings_my_stuff_disabled)
                DolphinLauncher.MyStuffMode.MusicOnly -> stringResource(R.string.settings_my_stuff_music)
                DolphinLauncher.MyStuffMode.Everything -> stringResource(R.string.settings_my_stuff_everything)
              }
          )
        }
        DropdownMenu(expanded = showDropdown, onDismissRequest = { showDropdown = false }) {
          DropdownMenuItem(
            text = { Text(stringResource(R.string.settings_my_stuff_disabled)) },
            onClick = {
              onSetMode(DolphinLauncher.MyStuffMode.Disabled)
              showDropdown = false
            },
          )
          DropdownMenuItem(
            text = { Text(stringResource(R.string.settings_my_stuff_music)) },
            onClick = {
              onSetMode(DolphinLauncher.MyStuffMode.MusicOnly)
              showDropdown = false
            },
          )
          DropdownMenuItem(
            text = { Text(stringResource(R.string.settings_my_stuff_everything)) },
            onClick = {
              onSetMode(DolphinLauncher.MyStuffMode.Everything)
              showDropdown = false
            },
          )
        }
      }
    },
  )
}
