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
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.skiletro.wheelwitch.ui.screens.HomeScreen
import com.skiletro.wheelwitch.ui.screens.OnboardingScreen
import com.skiletro.wheelwitch.ui.screens.QuickLaunchScreen
import com.skiletro.wheelwitch.ui.screens.SettingsScreen
import com.skiletro.wheelwitch.ui.theme.ThemeMode
import com.skiletro.wheelwitch.ui.theme.WheelWitchTheme
import com.skiletro.wheelwitch.data.PackStorage
import com.skiletro.wheelwitch.util.DolphinLauncher
import com.skiletro.wheelwitch.viewmodel.OnlineViewModel
import com.skiletro.wheelwitch.viewmodel.UpdateViewModel

/** Single-activity entry point. Hosts [HomeScreen] with onboarding wizard, settings navigation, and save info/rooms overlays. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initialQuickLaunch = intent?.action == ACTION_QUICK_LAUNCH
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
                    quickLaunchFromIntent = initialQuickLaunch,
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

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        if (intent.action == ACTION_QUICK_LAUNCH) {
            recreate()
        }
    }

    companion object {
        const val ACTION_QUICK_LAUNCH = "com.skiletro.wheelwitch.action.QUICK_LAUNCH"
    }
}

@Composable
private fun MainScreen(
    quickLaunchFromIntent: Boolean = false,
    viewModel: UpdateViewModel = viewModel(),
    onlineViewModel: OnlineViewModel = viewModel(),
    useDynamicColor: Boolean = false,
    onToggleDynamicColor: (Boolean) -> Unit = {},
    themeMode: ThemeMode = ThemeMode.System,
    onChangeThemeMode: (ThemeMode) -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    var quickLaunchMode by remember { mutableStateOf(quickLaunchFromIntent) }
    var onboardingComplete by remember { mutableStateOf(prefs.getBoolean("onboarding_completed", false)) }
    var onboardingStorageSelected by remember { mutableStateOf(false) }
    var onboardingIsoSelected by remember { mutableStateOf(false) }

    val storagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                return@rememberLauncherForActivityResult
            }
            viewModel.setStorageUri(uri)
            onboardingStorageSelected = true
        }
    }

    val isoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val path = PackStorage.resolveContentUriToPath(uri)
            if (path != null) {
                viewModel.setGameIsoPath(path)
                onboardingIsoSelected = true
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

    Box(Modifier.fillMaxSize()) {
        if (quickLaunchMode) {
            QuickLaunchScreen(
                viewModel = viewModel,
                onFinish = { (context as? android.app.Activity)?.finish() }
            )
        } else if (onboardingComplete) {
            if (!showSettings) {
                HomeScreen(
                    viewModel = viewModel,
                    onlineViewModel = onlineViewModel,
                    onPickIso = { isoPicker.launch(arrayOf("application/octet-stream", "*/*")) },
                    onOpenSettings = { showSettings = true }
                )
            }

            AnimatedVisibility(
                visible = showSettings,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
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
                    onChangeThemeMode = onChangeThemeMode,
                    onPickIso = { isoPicker.launch(arrayOf("application/octet-stream", "*/*")) },
                    onSimulateQuickLaunch = { quickLaunchMode = true },
                    onRelaunchOnboarding = {
                        prefs.edit().putBoolean("onboarding_completed", false).apply()
                        onboardingComplete = false
                        onboardingStorageSelected = false
                        onboardingIsoSelected = false
                        showSettings = false
                    }
                )
            }
        } else {
            OnboardingScreen(
                storageSelected = onboardingStorageSelected,
                isoSelected = onboardingIsoSelected,
                storageConfigured = viewModel.storageRootPath != null,
                isoConfigured = DolphinLauncher.getGameIsoPath(context) != null,
                onPickStorage = { storagePicker.launch(null) },
                onSkipStorage = { onboardingStorageSelected = true },
                onPickIso = { isoPicker.launch(arrayOf("application/octet-stream", "*/*")) },
                onSkipIso = { onboardingIsoSelected = true },
                onComplete = {
                    prefs.edit().putBoolean("onboarding_completed", true).apply()
                    onboardingComplete = true
                }
            )
        }
    }
}
