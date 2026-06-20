package com.skiletro.wheelwitch.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.skiletro.wheelwitch.R
import com.skiletro.wheelwitch.data.GameTypeParser
import com.skiletro.wheelwitch.ui.components.SettingsCategoryHeader
import com.skiletro.wheelwitch.ui.components.SettingsItem

/**
 * Retro Rewind section: the selected Mario Kart Wii ROM (filename +
 * format / game-id summary) and the configured pack storage path.
 * [onPickIso] opens the SAF file picker; [onClearIso] wipes the saved
 * ISO path and RR.json.
 */
@Composable
fun RetroRewindSection(
  isoPath: String?,
  gameInfo: GameTypeParser.GameInfo?,
  storageRootPath: String?,
  onPickIso: () -> Unit,
  onClearIso: () -> Unit,
) {
  SettingsCategoryHeader(stringResource(R.string.settings_retro_rewind))
  val fileName = isoPath?.substringAfterLast('/')?.ifBlank { null }
  val fileSummary =
    if (fileName != null) {
      if (gameInfo != null) "$fileName\n${gameInfo.format.name.uppercase()} \u00b7 ${gameInfo.gameId}"
      else fileName
    } else null
  SettingsItem(
    icon = ImageVector.vectorResource(R.drawable.ic_gamepad),
    title = stringResource(R.string.settings_mario_kart_wii),
    summary = fileSummary ?: stringResource(R.string.settings_rom_not_selected),
    trailing = {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = onPickIso, shape = RoundedCornerShape(14.dp)) {
          Text(stringResource(R.string.settings_pick))
        }
        if (fileName != null) {
          TextButton(onClick = onClearIso, shape = RoundedCornerShape(14.dp)) {
            Text(stringResource(R.string.settings_clear))
          }
        }
      }
    },
  )
  SettingsItem(
    icon = ImageVector.vectorResource(R.drawable.ic_dns),
    title = stringResource(R.string.settings_pack_storage),
    summary = storageRootPath ?: stringResource(R.string.error_storage_not_configured),
    trailing = null,
  )
}
