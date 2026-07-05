package com.skiletro.wheelwitch

import android.hardware.display.DisplayManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider
import com.skiletro.wheelwitch.ui.screens.MainScreen
import com.skiletro.wheelwitch.ui.theme.ThemeController
import com.skiletro.wheelwitch.ui.theme.WheelWitchTheme
import com.skiletro.wheelwitch.util.display.SecondScreenPresentation
import com.skiletro.wheelwitch.viewmodel.OnlineViewModel
import com.skiletro.wheelwitch.viewmodel.PackUpdateViewModel
import com.skiletro.wheelwitch.viewmodel.SaveDataViewModel

/**
 * Single-activity entry point. Hosts [MainScreen] which orchestrates
 * the onboarding wizard and the home/settings overlay stack. Also
 * manages a [SecondScreenPresentation] on the devices second
 * display if available (for DS devices like the Ayn Thor).
 */
class MainActivity : ComponentActivity() {
  private var secondScreen: SecondScreenPresentation? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    WindowCompat.setDecorFitsSystemWindows(window, false)
    val controller = WindowCompat.getInsetsController(window, window.decorView)
    controller.systemBarsBehavior =
      WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    controller.hide(WindowInsetsCompat.Type.systemBars())
    setContent {
      val theme = ThemeController.remember(this@MainActivity)
      WheelWitchTheme(themeMode = theme.themeMode, appTheme = theme.appTheme) {
        MainScreen(
          appTheme = theme.appTheme,
          onChangeAppTheme = theme::setAppTheme,
          themeMode = theme.themeMode,
          onChangeThemeMode = theme::setThemeMode,
        )
      }
    }
  }

  override fun onResume() {
    super.onResume()
    showSecondScreen()
  }

  override fun onPause() {
    super.onPause()
    dismissSecondScreen()
  }

  private fun showSecondScreen() {
    if (secondScreen != null) return
    val displayManager =
      getSystemService(DisplayManager::class.java) ?: return
    val displays =
      displayManager.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION)
    if (displays.isEmpty()) return

    val packUpdate =
      ViewModelProvider(this, PackUpdateViewModel.Factory)
        .get(PackUpdateViewModel::class.java)
    val saveData =
      ViewModelProvider(this, SaveDataViewModel.factory(packUpdate))
        .get(SaveDataViewModel::class.java)
    val onlineVm =
      ViewModelProvider(this)[OnlineViewModel::class.java]

    secondScreen =
      SecondScreenPresentation(
        this,
        displays[0],
        activeLicenseFlow = saveData.activeLicense,
        packStatusFlow = packUpdate.state,
        healthStateFlow = onlineVm.healthState,
      )
    secondScreen?.show()
  }

  private fun dismissSecondScreen() {
    secondScreen?.dismiss()
    secondScreen = null
  }
}
