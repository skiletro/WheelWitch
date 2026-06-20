package com.skiletro.wheelwitch.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.skiletro.wheelwitch.util.Prefs
import com.skiletro.wheelwitch.util.PrefsKeys

/**
 * Owns the app/theme and light/dark/oled mode state plus the
 * SharedPreferences reads/writes that persist them. Created once at
 * the activity root and threaded down via [ThemeController].
 */
class ThemeController(
  initialAppTheme: AppTheme,
  initialThemeMode: ThemeMode,
  private val onChangeAppTheme: (AppTheme) -> Unit,
  private val onChangeThemeMode: (ThemeMode) -> Unit,
) {
  private var _appTheme by mutableStateOf(initialAppTheme)
  val appTheme: AppTheme
    get() = _appTheme

  private var _themeMode by mutableStateOf(initialThemeMode)
  val themeMode: ThemeMode
    get() = _themeMode

  fun setAppTheme(theme: AppTheme) {
    _appTheme = theme
    onChangeAppTheme(theme)
  }

  fun setThemeMode(mode: ThemeMode) {
    _themeMode = mode
    onChangeThemeMode(mode)
  }

  companion object {
    /**
     * Reads the persisted theme choices and returns a [ThemeController]
     * whose setters also write back to [PrefsKeys.SETTINGS_PREFS].
     */
    @Composable
    fun remember(prefs: android.content.SharedPreferences): ThemeController {
      val savedTheme =
        prefs.getString(PrefsKeys.APP_THEME_KEY, AppTheme.Hex.name) ?: AppTheme.Hex.name
      val savedMode =
        prefs.getString(PrefsKeys.THEME_MODE_KEY, ThemeMode.System.name) ?: ThemeMode.System.name
      val initialTheme =
        runCatching { AppTheme.valueOf(savedTheme) }.getOrDefault(AppTheme.Hex)
      val initialMode =
        runCatching { ThemeMode.valueOf(savedMode) }.getOrDefault(ThemeMode.System)
      return remember {
        ThemeController(
          initialAppTheme = initialTheme,
          initialThemeMode = initialMode,
          onChangeAppTheme = { theme -> prefs.edit().putString(PrefsKeys.APP_THEME_KEY, theme.name).apply() },
          onChangeThemeMode = { mode -> prefs.edit().putString(PrefsKeys.THEME_MODE_KEY, mode.name).apply() },
        )
      }
    }

    /**
     * Convenience: builds a [ThemeController] from the canonical
     * settings prefs ([Prefs.settings]). Used by the activity root.
     */
    @Composable
    fun remember(context: android.content.Context): ThemeController {
      val prefs = remember { Prefs.settings(context) }
      return remember(prefs)
    }
  }
}
