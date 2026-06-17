package com.skiletro.wheelwitch

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.skiletro.wheelwitch.ui.screens.HomeScreen
import com.skiletro.wheelwitch.ui.screens.SettingsScreen
import com.skiletro.wheelwitch.ui.theme.ThemeMode
import com.skiletro.wheelwitch.ui.theme.WheelWitchTheme
import com.skiletro.wheelwitch.viewmodel.UpdateViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContent {
            val prefs = remember { this@MainActivity.getSharedPreferences("settings", Context.MODE_PRIVATE) }
            var useDynamicColor by remember { mutableStateOf(prefs.getBoolean("dynamic_color", false)) }
            var themeMode by remember {
                mutableStateOf(ThemeMode.valueOf(prefs.getString("theme_mode", ThemeMode.System.name) ?: ThemeMode.System.name))
            }

            WheelWitchTheme(
                themeMode = themeMode,
                dynamicColor = useDynamicColor
            ) {
                MainScreen(
                    useDynamicColor = useDynamicColor,
                    onToggleDynamicColor = { enabled ->
                        useDynamicColor = enabled
                        prefs.edit().putBoolean("dynamic_color", enabled).apply()
                    },
                    themeMode = themeMode,
                    onChangeThemeMode = { mode ->
                        themeMode = mode
                        prefs.edit().putString("theme_mode", mode.name).apply()
                    }
                )
            }
        }
    }
}

@Composable
private fun MainScreen(
    viewModel: UpdateViewModel = viewModel(),
    useDynamicColor: Boolean = false,
    onToggleDynamicColor: (Boolean) -> Unit = {},
    themeMode: ThemeMode = ThemeMode.System,
    onChangeThemeMode: (ThemeMode) -> Unit = {}
) {
    val context = LocalContext.current

    val storagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            viewModel.setStorageUri(uri)
        }
    }

    val isoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val path = resolveContentUriToPath(uri)
            if (path != null) {
                viewModel.setGameIsoPath(path)
            }
        }
    }

    val backupPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.backupSave(uri)
        }
    }

    val restorePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.restoreSave(uri)
        }
    }

    var showSettings by remember { mutableStateOf(false) }

    BackHandler(enabled = showSettings) {
        showSettings = false
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            HomeScreen(
                viewModel = viewModel,
                onPickStorage = { storagePicker.launch(null) },
                onPickIso = { isoPicker.launch(arrayOf("application/octet-stream", "*/*")) },
                onOpenSettings = { showSettings = true }
            )

            AnimatedVisibility(
                visible = showSettings,
                enter = slideInHorizontally() + fadeIn(),
                exit = slideOutHorizontally() + fadeOut()
            ) {
                SettingsScreen(
                    viewModel = viewModel,
                    onBackupSave = { backupPicker.launch("rksys.dat") },
                    onRestoreSave = { restorePicker.launch(arrayOf("application/octet-stream", "*/*")) },
                    onDeleteSave = { viewModel.deleteSave() },
                    onClose = { showSettings = false },
                    useDynamicColor = useDynamicColor,
                    onToggleDynamicColor = onToggleDynamicColor,
                    themeMode = themeMode,
                    onChangeThemeMode = onChangeThemeMode
                )
            }
        }
    }
}

private fun resolveContentUriToPath(uri: Uri): String? {
    val docId = try {
        android.provider.DocumentsContract.getDocumentId(uri)
    } catch (e: Exception) {
        return uri.path
    }
    val parts = docId.split(":")
    if (parts.size < 2) return uri.path
    return when {
        parts[0].equals("primary", ignoreCase = true) ->
            "/storage/emulated/0/${parts[1]}"
        else ->
            "/storage/${parts[0]}/${parts[1]}"
    }
}
