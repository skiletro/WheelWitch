package com.skiletro.wheelwitch.ui.screens.settings

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.skiletro.wheelwitch.R
import com.skiletro.wheelwitch.ui.components.SettingsCategoryHeader
import com.skiletro.wheelwitch.ui.components.SettingsItem
import com.skiletro.wheelwitch.util.BugReportLauncher
import com.skiletro.wheelwitch.util.Prefs
import com.skiletro.wheelwitch.util.PrefsKeys

/**
 * Logging section: toggle the on-disk log file via [PrefsKeys.LOGGING_TO_FILE_KEY],
 * and launch the bug-report chooser via [BugReportLauncher].
 */
@Composable
fun LoggingSection() {
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
