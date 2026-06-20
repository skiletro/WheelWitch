package com.skiletro.wheelwitch.ui.screens.settings

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.skiletro.wheelwitch.R
import com.skiletro.wheelwitch.BuildConfig
import com.skiletro.wheelwitch.ui.components.SettingsCategoryHeader
import com.skiletro.wheelwitch.ui.components.SettingsItem
import androidx.compose.runtime.Composable

/**
 * About section: app name + version + tagline. Version string varies
 * between debug builds (includes the git hash) and release builds.
 */
@Composable
fun AboutSection() {
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
