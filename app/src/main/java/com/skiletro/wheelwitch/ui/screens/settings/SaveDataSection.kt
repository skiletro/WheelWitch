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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.skiletro.wheelwitch.R
import com.skiletro.wheelwitch.ui.components.SettingsCategoryHeader
import com.skiletro.wheelwitch.ui.components.SettingsItem

/**
 * Save data section: backup, restore, and (when a save exists) delete.
 * Backup/restore are wired by the host via [onBackupSave]/[onRestoreSave];
 * the delete row triggers a confirmation dialog owned by the host via
 * [onRequestDelete].
 */
@Composable
fun SaveDataSection(
  hasSave: Boolean,
  onBackupSave: () -> Unit,
  onRestoreSave: () -> Unit,
  onRequestDelete: () -> Unit,
) {
  SettingsCategoryHeader(stringResource(R.string.settings_save_data))
  SettingsItem(
    icon = ImageVector.vectorResource(R.drawable.ic_save_alt),
    title = stringResource(R.string.settings_backup),
    summary =
      if (hasSave) stringResource(R.string.settings_save_found)
      else stringResource(R.string.status_save_not_found),
    trailing = {
      Button(
        onClick = onBackupSave,
        enabled = hasSave,
        shape = RoundedCornerShape(14.dp),
        contentPadding = ButtonDefaults.TextButtonContentPadding,
        colors = ButtonDefaults.filledTonalButtonColors(
          containerColor = MaterialTheme.colorScheme.secondaryContainer,
          contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
      ) {
        Text(stringResource(R.string.settings_backup), fontWeight = FontWeight.Medium)
      }
    },
  )
  SettingsItem(
    icon = ImageVector.vectorResource(R.drawable.ic_restore),
    title = stringResource(R.string.settings_restore),
    summary = stringResource(R.string.settings_replace_save_sub),
    trailing = {
      Button(
        onClick = onRestoreSave,
        shape = RoundedCornerShape(14.dp),
        contentPadding = ButtonDefaults.TextButtonContentPadding,
        colors = ButtonDefaults.filledTonalButtonColors(
          containerColor = MaterialTheme.colorScheme.secondaryContainer,
          contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
      ) {
        Text(stringResource(R.string.settings_restore), fontWeight = FontWeight.Medium)
      }
    },
  )
  if (hasSave) {
    SettingsItem(
      icon = ImageVector.vectorResource(R.drawable.ic_delete),
      title = stringResource(R.string.settings_delete_save),
      titleColor = MaterialTheme.colorScheme.error,
      summary = stringResource(R.string.settings_delete_save_sub),
      trailing = {
        TextButton(onClick = onRequestDelete) {
          Text(stringResource(R.string.settings_delete), color = MaterialTheme.colorScheme.error)
        }
      },
    )
  }
}
