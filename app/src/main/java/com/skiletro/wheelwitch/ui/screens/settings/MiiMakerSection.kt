package com.skiletro.wheelwitch.ui.screens.settings

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.skiletro.wheelwitch.R
import com.skiletro.wheelwitch.ui.components.SettingsCategoryHeader
import com.skiletro.wheelwitch.ui.components.SettingsItem

/**
 * Mii Channel WAD section: install or delete the cached WAD. Shows an
 * in-progress label while a download is running, an error summary when
 * the last install failed, and a confirmation-gated delete row when
 * the WAD is already present.
 */
@Composable
fun MiiMakerSection(
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
