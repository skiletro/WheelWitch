package com.skiletro.wheelwitch.ui.screens.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.skiletro.wheelwitch.R
import com.skiletro.wheelwitch.ui.components.SettingsCategoryHeader
import com.skiletro.wheelwitch.ui.components.SettingsItem
import com.skiletro.wheelwitch.ui.components.formatBytes
import com.skiletro.wheelwitch.util.MiiFaceCache
import com.skiletro.wheelwitch.util.cacheSize

/**
 * Advanced section. The rewind download cache row and the quick-launch
 * shortcut pin have been removed along with their underlying flows. The
 * Mii face cache row and the "relaunch onboarding" escape hatch stay
 * — the latter is useful for re-running the onboarding wizard during
 * development.
 */
@Composable
fun AdvancedSection(
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
