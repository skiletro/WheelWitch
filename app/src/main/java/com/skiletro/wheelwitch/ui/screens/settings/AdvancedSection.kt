package com.skiletro.wheelwitch.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.skiletro.wheelwitch.R
import com.skiletro.wheelwitch.ui.components.SettingsCategoryHeader
import com.skiletro.wheelwitch.ui.components.SettingsItem
import com.skiletro.wheelwitch.ui.components.formatBytes
import com.skiletro.wheelwitch.util.MiiFaceCache
import com.skiletro.wheelwitch.util.cacheSize
import java.io.File

/**
 * Advanced section: download cache, Mii face cache, quick-launch
 * shortcut pin, and the relaunch-onboarding escape hatch.
 */
@Composable
fun AdvancedSection(
  onSimulateQuickLaunch: () -> Unit,
  onRelaunchOnboarding: () -> Unit,
) {
  val context = LocalContext.current
  SettingsCategoryHeader(stringResource(R.string.settings_advanced))
  DownloadCacheRow(context)
  MiiCacheRow()
  QuickLaunchRow(context, onSimulateQuickLaunch)
  RelaunchOnboardingRow(onRelaunchOnboarding)
}

@Composable
private fun DownloadCacheRow(context: Context) {
  val cacheDir = remember { File(context.cacheDir, "rewind_pack_downloads") }
  var cacheSizeBytes by remember { mutableStateOf(cacheSize(cacheDir)) }
  SettingsItem(
    icon = ImageVector.vectorResource(R.drawable.ic_cached),
    title = stringResource(R.string.settings_download_cache),
    summary = formatBytes(cacheSizeBytes),
    trailing = {
      TextButton(
        onClick = {
          cacheDir.deleteRecursively()
          cacheSizeBytes = 0
        },
        enabled = cacheSizeBytes > 0,
        shape = RoundedCornerShape(14.dp),
      ) {
        Text(stringResource(R.string.settings_clear))
      }
    },
  )
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
        Text(stringResource(R.string.settings_clear))
      }
    },
  )
}

@Composable
private fun QuickLaunchRow(context: Context, onSimulate: () -> Unit) {
  val shortcutManager =
    remember {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.getSystemService(Context.SHORTCUT_SERVICE) as? ShortcutManager
      } else {
        null
      }
    }
  val canPinShortcut = shortcutManager?.isRequestPinShortcutSupported == true
  val shortcutLabel = stringResource(R.string.settings_shortcut_short)
  val shortcutLongLabel = stringResource(R.string.settings_shortcut_long)
  SettingsItem(
    icon = ImageVector.vectorResource(R.drawable.ic_shortcut),
    title = stringResource(R.string.settings_quick_launch),
    summary = stringResource(R.string.settings_quick_launch_sub),
    trailing = {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = onSimulate, shape = RoundedCornerShape(14.dp)) {
          Text(stringResource(R.string.settings_simulate))
        }
        TextButton(
          onClick = {
            if (canPinShortcut && shortcutManager != null) {
              val intent =
                Intent("com.skiletro.wheelwitch.action.QUICK_LAUNCH").apply {
                  addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                  `package` = context.packageName
                }
              val shortcut =
                ShortcutInfo.Builder(context, "quick_launch")
                  .setShortLabel(shortcutLabel)
                  .setLongLabel(shortcutLongLabel)
                  .setIcon(Icon.createWithResource(context, R.mipmap.ic_launcher))
                  .setIntent(intent)
                  .build()
              shortcutManager.requestPinShortcut(shortcut, null)
            } else {
              Toast.makeText(context, R.string.shortcut_pin_unsupported, Toast.LENGTH_LONG).show()
            }
          },
          shape = RoundedCornerShape(14.dp),
        ) {
          Text(stringResource(R.string.settings_shortcut))
        }
      }
    },
  )
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
