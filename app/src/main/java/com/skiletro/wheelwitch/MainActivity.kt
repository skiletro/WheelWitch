package com.skiletro.wheelwitch

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.skiletro.wheelwitch.ui.screens.MainScreen
import com.skiletro.wheelwitch.ui.theme.ThemeController
import com.skiletro.wheelwitch.ui.theme.WheelWitchTheme

/**
 * Single-activity entry point. Hosts [MainScreen] which orchestrates
 * the onboarding wizard, settings navigation, and save info / online
 * overlays. Receives the QUICK_LAUNCH intent to skip onboarding and
 * jump straight to the quick-launch flow.
 */
class MainActivity : ComponentActivity() {
  private var pendingQuickLaunch by mutableStateOf(false)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    pendingQuickLaunch = intent?.action == ACTION_QUICK_LAUNCH
    WindowCompat.setDecorFitsSystemWindows(window, false)
    val controller = WindowCompat.getInsetsController(window, window.decorView)
    controller.systemBarsBehavior =
      WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    controller.hide(WindowInsetsCompat.Type.systemBars())
    setContent {
      val theme = ThemeController.remember(this@MainActivity)
      WheelWitchTheme(themeMode = theme.themeMode, appTheme = theme.appTheme) {
        MainScreen(
          quickLaunchFromIntent = pendingQuickLaunch,
          appTheme = theme.appTheme,
          onChangeAppTheme = theme::setAppTheme,
          themeMode = theme.themeMode,
          onChangeThemeMode = theme::setThemeMode,
        )
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    if (intent.action == ACTION_QUICK_LAUNCH) {
      pendingQuickLaunch = true
    }
  }

  companion object {
    const val ACTION_QUICK_LAUNCH = "com.skiletro.wheelwitch.action.QUICK_LAUNCH"
  }
}
