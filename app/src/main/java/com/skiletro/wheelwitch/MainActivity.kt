package com.skiletro.wheelwitch

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.skiletro.wheelwitch.ui.screens.HomeScreen
import com.skiletro.wheelwitch.ui.screens.OnboardingScreen
import com.skiletro.wheelwitch.ui.screens.QuickLaunchScreen
import com.skiletro.wheelwitch.ui.screens.SettingsScreen
import com.skiletro.wheelwitch.ui.theme.AppTheme
import com.skiletro.wheelwitch.ui.theme.ThemeMode
import com.skiletro.wheelwitch.ui.theme.WheelWitchTheme
import com.skiletro.wheelwitch.data.PackStorage
import com.skiletro.wheelwitch.util.DolphinLauncher
import com.skiletro.wheelwitch.util.PrefsKeys
import com.skiletro.wheelwitch.viewmodel.MiiMakerViewModel
import com.skiletro.wheelwitch.viewmodel.OnlineViewModel
import com.skiletro.wheelwitch.viewmodel.PackUpdateViewModel
import com.skiletro.wheelwitch.viewmodel.SaveDataViewModel

/** Single-activity entry point. Hosts [HomeScreen] with onboarding wizard, settings navigation, and save info/rooms overlays. */
class MainActivity : ComponentActivity() {
    private var pendingQuickLaunch by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingQuickLaunch = intent?.action == ACTION_QUICK_LAUNCH
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
        setContent {
            val prefs = remember { this@MainActivity.getSharedPreferences(PrefsKeys.SETTINGS_PREFS, Context.MODE_PRIVATE) }
            var appTheme by remember {
                val saved = prefs.getString(PrefsKeys.APP_THEME_KEY, AppTheme.Hex.name) ?: AppTheme.Hex.name
                mutableStateOf(runCatching { AppTheme.valueOf(saved) }.getOrDefault(AppTheme.Hex))
            }
            var themeMode by remember {
                val saved = prefs.getString(PrefsKeys.THEME_MODE_KEY, ThemeMode.System.name) ?: ThemeMode.System.name
                mutableStateOf(runCatching { ThemeMode.valueOf(saved) }.getOrDefault(ThemeMode.System))
            }

            WheelWitchTheme(
                themeMode = themeMode,
                appTheme = appTheme
            ) {
                val initialQuickLaunch = pendingQuickLaunch
                MainScreen(
                    quickLaunchFromIntent = initialQuickLaunch,
                    appTheme = appTheme,
                    onChangeAppTheme = { theme ->
                        appTheme = theme
                        prefs.edit().putString(PrefsKeys.APP_THEME_KEY, theme.name).apply()
                    },
                    themeMode = themeMode,
                    onChangeThemeMode = { mode ->
                        themeMode = mode
                        prefs.edit().putString(PrefsKeys.THEME_MODE_KEY, mode.name).apply()
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        if (intent.action == ACTION_QUICK_LAUNCH) {
            pendingQuickLaunch = true
        }
    }

    companion object {
        const val ACTION_QUICK_LAUNCH = "com.skiletro.wheelwitch.action.QUICK_LAUNCH"
    }
}

@Composable
private fun MainScreen(
    quickLaunchFromIntent: Boolean = false,
    packUpdate: PackUpdateViewModel = viewModel(),
    saveData: SaveDataViewModel = viewModel(),
    miiMaker: MiiMakerViewModel = viewModel(),
    onlineViewModel: OnlineViewModel = viewModel(),
    appTheme: AppTheme = AppTheme.Hex,
    onChangeAppTheme: (AppTheme) -> Unit = {},
    themeMode: ThemeMode = ThemeMode.System,
    onChangeThemeMode: (ThemeMode) -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences(PrefsKeys.SETTINGS_PREFS, Context.MODE_PRIVATE) }
    var quickLaunchMode by remember { mutableStateOf(quickLaunchFromIntent) }
    var onboardingComplete by remember { mutableStateOf(prefs.getBoolean(PrefsKeys.ONBOARDING_COMPLETED_KEY, false)) }
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
            packUpdate.setStorageUri(uri)
            onboardingStorageSelected = true
        }
    }

    val isoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val path = PackStorage.resolveContentUriToPath(uri)
            if (path != null) {
                packUpdate.setGameIsoPath(path)
                onboardingIsoSelected = true
            }
        }
    }

    val backupPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        if (uri != null) {
            saveData.backupSave(uri)
        }
    }

    val restorePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            saveData.restoreSave(uri)
        }
    }

    var showSettings by remember { mutableStateOf(false) }

    BackHandler(enabled = showSettings) {
        showSettings = false
    }

    Box(Modifier.fillMaxSize()) {
        if (quickLaunchMode) {
            QuickLaunchScreen(
                viewModel = packUpdate,
                onFinish = { (context as? android.app.Activity)?.finish() }
            )
        } else {
            AnimatedContent(
                targetState = onboardingComplete,
                transitionSpec = {
                    fadeIn(tween(300)) togetherWith fadeOut(tween(300))
                },
                label = "onboarding_transition"
            ) { completed ->
                if (completed) {
                    if (!showSettings) {
                        HomeScreen(
                            packUpdate = packUpdate,
                            saveData = saveData,
                            miiMaker = miiMaker,
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
                            packUpdate = packUpdate,
                            saveData = saveData,
                            miiMaker = miiMaker,
                            onBackupSave = { backupPicker.launch("rksys.dat") },
                            onRestoreSave = { restorePicker.launch(arrayOf("application/octet-stream", "*/*")) },
                            onDeleteSave = { saveData.deleteSave() },
                            onClose = { showSettings = false },
                            appTheme = appTheme,
                            onChangeAppTheme = onChangeAppTheme,
                            themeMode = themeMode,
                            onChangeThemeMode = onChangeThemeMode,
                            onPickIso = { isoPicker.launch(arrayOf("application/octet-stream", "*/*")) },
                            onSimulateQuickLaunch = { quickLaunchMode = true },
                            onRelaunchOnboarding = {
                                prefs.edit().putBoolean(PrefsKeys.ONBOARDING_COMPLETED_KEY, false).apply()
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
                        storageConfigured = packUpdate.storageRootPath != null,
                        isoConfigured = DolphinLauncher.getGameIsoPath(context) != null,
                        onPickStorage = { storagePicker.launch(null) },
                        onSkipStorage = { onboardingStorageSelected = true },
                        onPickIso = { isoPicker.launch(arrayOf("application/octet-stream", "*/*")) },
                        onSkipIso = { onboardingIsoSelected = true },
                        onComplete = {
                            prefs.edit().putBoolean(PrefsKeys.ONBOARDING_COMPLETED_KEY, true).apply()
                            onboardingComplete = true
                        }
                    )
                }
            }
        }
    }
}
