package com.skiletro.wheelwitch.ui.screens

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import android.widget.Toast
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
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.skiletro.wheelwitch.R
import com.skiletro.wheelwitch.ui.components.formatBytes
import com.skiletro.wheelwitch.ui.theme.AppTheme
import com.skiletro.wheelwitch.ui.theme.ThemeMode
import com.skiletro.wheelwitch.util.BugReportLauncher
import com.skiletro.wheelwitch.util.DolphinLauncher
import com.skiletro.wheelwitch.util.MiiFaceCache
import com.skiletro.wheelwitch.util.PrefsKeys
import com.skiletro.wheelwitch.util.cacheSize
import com.skiletro.wheelwitch.viewmodel.MiiMakerViewModel
import com.skiletro.wheelwitch.viewmodel.PackUpdateViewModel
import com.skiletro.wheelwitch.viewmodel.SaveDataViewModel

@Composable
fun SettingsScreen(
    packUpdate: PackUpdateViewModel,
    saveData: SaveDataViewModel,
    miiMaker: MiiMakerViewModel,
    onBackupSave: () -> Unit,
    onRestoreSave: () -> Unit,
    onDeleteSave: () -> Unit,
    onClose: () -> Unit,
    appTheme: AppTheme,
    onChangeAppTheme: (AppTheme) -> Unit,
    themeMode: ThemeMode,
    onChangeThemeMode: (ThemeMode) -> Unit,
    onPickIso: () -> Unit,
    onSimulateQuickLaunch: () -> Unit,
    onRelaunchOnboarding: () -> Unit
) {
    val hasSave by saveData.hasSave.collectAsState()
    val hasWad by miiMaker.hasWad.collectAsState()
    val isInstallingWad by miiMaker.isInstallingWad.collectAsState()
    val miiMakerError by miiMaker.miiMakerError.collectAsState()
    val isoPath by packUpdate.currentIsoPath.collectAsState()
    val gameInfo by packUpdate.gameInfo.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showWadDeleteConfirm by remember { mutableStateOf(false) }
    var showAppThemeDropdown by remember { mutableStateOf(false) }
    var showThemeDropdown by remember { mutableStateOf(false) }
    var showMyStuffDropdown by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.settings_delete_save_dialog_title)) },
            text = { Text(stringResource(R.string.settings_delete_save_dialog_body)) },
            confirmButton = {
                Button(onClick = { onDeleteSave(); showDeleteConfirm = false }) {
                    Text(
                        stringResource(R.string.settings_delete)
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    if (showWadDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showWadDeleteConfirm = false },
            title = { Text(stringResource(R.string.settings_delete_wad_dialog_title)) },
            text = { Text(stringResource(R.string.settings_delete_wad_dialog_body)) },
            confirmButton = {
                Button(onClick = { miiMaker.deleteWad(); showWadDeleteConfirm = false }) {
                    Text(
                        stringResource(R.string.settings_delete)
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showWadDeleteConfirm = false
                }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_arrow_back),
                    contentDescription = stringResource(R.string.cd_back),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Save Data
            item {
                SettingsCategoryHeader(stringResource(R.string.settings_save_data))
            }
            item {
                SettingsItem(
                    icon = ImageVector.vectorResource(R.drawable.ic_save_alt),
                    title = stringResource(R.string.settings_backup),
                    summary = if (hasSave) stringResource(R.string.settings_save_found) else stringResource(
                        R.string.status_save_not_found
                    ),
                    trailing = {
                        Button(
                            onClick = onBackupSave,
                            enabled = hasSave,
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = ButtonDefaults.TextButtonContentPadding,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Text(
                                stringResource(R.string.settings_backup),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
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
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Text(
                                stringResource(R.string.settings_restore),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                )
                if (hasSave) {
                    SettingsItem(
                        icon = ImageVector.vectorResource(R.drawable.ic_delete),
                        title = stringResource(R.string.settings_delete_save),
                        titleColor = MaterialTheme.colorScheme.error,
                        summary = stringResource(R.string.settings_delete_save_sub),
                        trailing = {
                            TextButton(onClick = { showDeleteConfirm = true }) {
                                Text(
                                    stringResource(R.string.settings_delete),
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    )
                }
            }

            // Appearance
            item {
                SettingsCategoryHeader(stringResource(R.string.settings_appearance))
            }
            item {
                val appThemeLabel = stringResource(appTheme.labelRes)
                SettingsItem(
                    icon = ImageVector.vectorResource(R.drawable.ic_palette),
                    title = stringResource(R.string.settings_app_theme),
                    summary = appThemeLabel,
                    trailing = {
                        Box {
                            TextButton(
                                onClick = { showAppThemeDropdown = true },
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text(text = appThemeLabel)
                            }
                            DropdownMenu(
                                expanded = showAppThemeDropdown,
                                onDismissRequest = { showAppThemeDropdown = false }
                            ) {
                                AppTheme.entries.forEach { theme ->
                                    DropdownMenuItem(
                                        text = { Text(stringResource(theme.labelRes)) },
                                        onClick = {
                                            onChangeAppTheme(theme)
                                            showAppThemeDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                )
                SettingsItem(
                    icon = ImageVector.vectorResource(R.drawable.ic_nightlight),
                    title = stringResource(R.string.settings_dark_mode),
                    summary = when (themeMode) {
                        ThemeMode.Light -> stringResource(R.string.settings_always_light)
                        ThemeMode.Dark -> stringResource(R.string.settings_always_dark)
                        ThemeMode.Oled -> stringResource(R.string.settings_oled)
                        ThemeMode.System -> stringResource(R.string.settings_follow_system)
                    },
                    trailing = {
                        Box {
                            TextButton(
                                onClick = { showThemeDropdown = true },
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text(
                                    text = when (themeMode) {
                                        ThemeMode.Light -> stringResource(R.string.settings_theme_light)
                                        ThemeMode.Dark -> stringResource(R.string.settings_theme_dark)
                                        ThemeMode.Oled -> stringResource(R.string.settings_theme_oled)
                                        ThemeMode.System -> stringResource(R.string.settings_theme_system)
                                    }
                                )
                            }
                            DropdownMenu(
                                expanded = showThemeDropdown,
                                onDismissRequest = { showThemeDropdown = false }
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
                                        }
                                    )
                                }
                            }
                        }
                    }
                )
            }

            // Retro Rewind
            item {
                SettingsCategoryHeader(stringResource(R.string.settings_retro_rewind))
            }
            item {
                val fileName = isoPath?.substringAfterLast('/')?.ifBlank { null }
                val fileSummary = if (fileName != null) {
                    val gi = gameInfo
                    if (gi != null) "$fileName\n${gi.format.name.uppercase()} \u00b7 ${gi.gameId}"
                    else fileName
                } else null
                SettingsItem(
                    icon = ImageVector.vectorResource(R.drawable.ic_gamepad),
                    title = stringResource(R.string.settings_mario_kart_wii),
                    summary = fileSummary ?: stringResource(R.string.settings_rom_not_selected),
                    trailing = {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(
                                onClick = onPickIso,
                                shape = RoundedCornerShape(14.dp)
                            ) { Text(stringResource(R.string.settings_pick)) }
                            if (fileName != null) {
                                TextButton(
                                    onClick = { packUpdate.clearIsoPath() },
                                    shape = RoundedCornerShape(14.dp)
                                ) { Text(stringResource(R.string.settings_clear)) }
                            }
                        }
                    }
                )
                SettingsItem(
                    icon = ImageVector.vectorResource(R.drawable.ic_dns),
                    title = stringResource(R.string.settings_pack_storage),
                    summary = packUpdate.storageRootPath
                        ?: stringResource(R.string.error_storage_not_configured),
                    trailing = null
                )
            }

            // Riivolution Options
            item {
                SettingsCategoryHeader(stringResource(R.string.settings_riivolution_options))
            }
            item {
                val myStuffMode by packUpdate.myStuffMode.collectAsState()
                SettingsItem(
                    icon = ImageVector.vectorResource(R.drawable.ic_folder),
                    title = stringResource(R.string.settings_my_stuff),
                    summary = stringResource(R.string.settings_my_stuff_summary),
                    trailing = {
                        Box {
                            TextButton(
                                onClick = { showMyStuffDropdown = true },
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text(
                                    text = when (myStuffMode) {
                                        DolphinLauncher.MyStuffMode.Disabled -> stringResource(R.string.settings_my_stuff_disabled)
                                        DolphinLauncher.MyStuffMode.MusicOnly -> stringResource(R.string.settings_my_stuff_music)
                                        DolphinLauncher.MyStuffMode.Everything -> stringResource(R.string.settings_my_stuff_everything)
                                    }
                                )
                            }
                            DropdownMenu(
                                expanded = showMyStuffDropdown,
                                onDismissRequest = { showMyStuffDropdown = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.settings_my_stuff_disabled)) },
                                    onClick = {
                                        packUpdate.setMyStuffMode(DolphinLauncher.MyStuffMode.Disabled)
                                        showMyStuffDropdown = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.settings_my_stuff_music)) },
                                    onClick = {
                                        packUpdate.setMyStuffMode(DolphinLauncher.MyStuffMode.MusicOnly)
                                        showMyStuffDropdown = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.settings_my_stuff_everything)) },
                                    onClick = {
                                        packUpdate.setMyStuffMode(DolphinLauncher.MyStuffMode.Everything)
                                        showMyStuffDropdown = false
                                    }
                                )
                            }
                        }
                    }
                )
            }

            // Mii Maker
            item {
                SettingsCategoryHeader(stringResource(R.string.settings_mii_maker_section))
            }
            item {
                val wadStatus =
                    if (hasWad) stringResource(R.string.status_installed) else stringResource(R.string.status_not_installed)
                SettingsItem(
                    icon = ImageVector.vectorResource(R.drawable.ic_face_up),
                    title = stringResource(R.string.settings_mii_channel_wad),
                    summary = if (miiMakerError != null) miiMakerError else wadStatus,
                    summaryColor = if (miiMakerError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    trailing = {
                        if (isInstallingWad) {
                            Text(
                                stringResource(R.string.settings_installing),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        } else if (hasWad) {
                            TextButton(onClick = { showWadDeleteConfirm = true }) {
                                Text(
                                    stringResource(R.string.settings_delete),
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        } else {
                            Button(
                                onClick = { miiMaker.installMiiMakerWad() },
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = ButtonDefaults.TextButtonContentPadding
                            ) { Text(stringResource(R.string.action_install)) }
                        }
                    }
                )
            }

            // Logging
            item {
                SettingsCategoryHeader(stringResource(R.string.settings_logging))
            }
            item {
                val loggingPrefs = remember { context.getSharedPreferences(PrefsKeys.PREFS_NAME, Context.MODE_PRIVATE) }
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
                            }
                        )
                    }
                )
            }
            item {
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
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) { Text(stringResource(R.string.settings_report), fontWeight = FontWeight.Medium) }
                    }
                )
            }

            // Advanced
            item {
                SettingsCategoryHeader(stringResource(R.string.settings_advanced))
            }
            item {
                val cacheDir = remember { java.io.File(context.cacheDir, "rewind_pack_downloads") }
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
                            shape = RoundedCornerShape(14.dp)
                        ) { Text(stringResource(R.string.settings_clear)) }
                    }
                )
            }
            item {
                var miiCacheSizeBytes by remember {
                    mutableStateOf(MiiFaceCache.cacheSize())
                }
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
                            shape = RoundedCornerShape(14.dp)
                        ) { Text(stringResource(R.string.settings_clear)) }
                    }
                )
            }
            item {
                SettingsItem(
                    icon = ImageVector.vectorResource(R.drawable.ic_shortcut),
                    title = stringResource(R.string.settings_quick_launch),
                    summary = stringResource(R.string.settings_quick_launch_sub),
                    trailing = {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(
                                onClick = onSimulateQuickLaunch,
                                shape = RoundedCornerShape(14.dp)
                            ) { Text(stringResource(R.string.settings_simulate)) }
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
                                                .setIcon(
                                                    Icon.createWithResource(
                                                        context,
                                                        R.mipmap.ic_launcher
                                                    )
                                                )
                                                .setIntent(intent)
                                                .build()
                                        shortcutManager.requestPinShortcut(shortcut, null)
                                    } else {
                                        Toast.makeText(
                                            context,
                                            R.string.shortcut_pin_unsupported,
                                            Toast.LENGTH_LONG,
                                        ).show()
                                    }
                                },
                                shape = RoundedCornerShape(14.dp)
                            ) { Text(stringResource(R.string.settings_shortcut)) }
                        }
                    }
                )
            }
            item {
                SettingsItem(
                    icon = ImageVector.vectorResource(R.drawable.ic_exit_to_app),
                    title = stringResource(R.string.settings_onboarding),
                    summary = stringResource(R.string.settings_relaunch_onboarding),
                    trailing = {
                        TextButton(
                            onClick = onRelaunchOnboarding,
                            shape = RoundedCornerShape(14.dp)
                        ) { Text(stringResource(R.string.settings_relaunch)) }
                    }
                )
            }

            // About
            item {
                SettingsCategoryHeader(stringResource(R.string.settings_about))
            }
            item {
                val version = if (com.skiletro.wheelwitch.BuildConfig.DEBUG)
                    stringResource(
                        R.string.settings_version_debug,
                        com.skiletro.wheelwitch.BuildConfig.VERSION_NAME,
                        com.skiletro.wheelwitch.BuildConfig.GIT_HASH
                    )
                else
                    stringResource(
                        R.string.settings_version_release,
                        com.skiletro.wheelwitch.BuildConfig.VERSION_NAME
                    )
                SettingsItem(
                    icon = ImageVector.vectorResource(R.drawable.ic_info),
                    title = stringResource(R.string.settings_wheel_witch),
                    summary = stringResource(
                        R.string.settings_about_summary,
                        version,
                        stringResource(R.string.settings_app_subtitle)
                    ),
                    trailing = null
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SettingsCategoryHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    summary: String? = null,
    iconTint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant,
    titleColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    summaryColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (summary != null) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = summaryColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(12.dp))
            trailing()
        }
    }
}