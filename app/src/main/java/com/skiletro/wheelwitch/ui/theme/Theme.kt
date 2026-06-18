package com.skiletro.wheelwitch.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import android.app.Activity
import com.skiletro.wheelwitch.R

// Hex scheme: primary/secondary/tertiary roles only; background/surface/etc. use Material defaults.
private val HexDarkColorScheme = darkColorScheme(
    primary = Purple80,
    onPrimary = OnPurple80,
    primaryContainer = Color(0xFF4A0080),
    onPrimaryContainer = Color(0xFFF0E0FF),
    secondary = PurpleGrey80,
    onSecondary = Color(0xFF3A1D4A),
    secondaryContainer = Color(0xFF3E2649),
    onSecondaryContainer = Color(0xFFF0DCF5),
    tertiary = LightPurple80,
    onTertiary = Color(0xFF560030),
    tertiaryContainer = Color(0xFF5C0A37),
    onTertiaryContainer = Color(0xFFFFD9E4)
)

private val HexLightColorScheme = lightColorScheme(
    primary = Purple40,
    onPrimary = OnPurple40,
    primaryContainer = Color(0xFFF0E0FF),
    onPrimaryContainer = Color(0xFF2D0066),
    secondary = PurpleGrey40,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF0DCF5),
    onSecondaryContainer = Color(0xFF2E1D38),
    tertiary = LightPurple40,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFD9E4),
    onTertiaryContainer = Color(0xFF3E0020)
)

// Swamp scheme: primary/secondary/tertiary roles only; background/surface/etc. use Material defaults.
private val SwampDarkColorScheme = darkColorScheme(
    primary = SwampGreen80,
    onPrimary = OnSwampGreen80,
    primaryContainer = Color(0xFF005005),
    onPrimaryContainer = Color(0xFFB2DFB2),
    secondary = SwampGreenGrey80,
    onSecondary = Color(0xFF003300),
    secondaryContainer = Color(0xFF1B5E20),
    onSecondaryContainer = Color(0xFFC8E6C9),
    tertiary = SwampLightGreen80,
    onTertiary = Color(0xFF003300),
    tertiaryContainer = Color(0xFF0A3A0A),
    onTertiaryContainer = Color(0xFFE0F5E0)
)

private val SwampLightColorScheme = lightColorScheme(
    primary = SwampGreen40,
    onPrimary = OnSwampGreen40,
    primaryContainer = Color(0xFFB2DFB2),
    onPrimaryContainer = Color(0xFF002106),
    secondary = SwampGreenGrey40,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC8E6C9),
    onSecondaryContainer = Color(0xFF002106),
    tertiary = SwampLightGreen40,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE0F5E0),
    onTertiaryContainer = Color(0xFF002106)
)

// Wizard scheme: fully specified including background/surface.
private val WizardDarkColorScheme = darkColorScheme(
    primary = WizardTeal80,
    onPrimary = OnWizardTeal80,
    primaryContainer = Color(0xFF004D40),
    onPrimaryContainer = Color(0xFFA7F0E0),
    secondary = WizardTealGrey80,
    onSecondary = Color(0xFF00332E),
    secondaryContainer = Color(0xFF004D45),
    onSecondaryContainer = Color(0xFFA7F0E8),
    tertiary = WizardNeutral80,
    onTertiary = Color(0xFF1A1C2E),
    tertiaryContainer = Color(0xFF2D3044),
    onTertiaryContainer = Color(0xFFD7D9E0),
    background = Color(0xFF24252D),
    onBackground = Color(0xFFE4E4E8),
    surface = Color(0xFF363944),
    onSurface = Color(0xFFE4E4E8),
    surfaceVariant = Color(0xFF3E424E),
    onSurfaceVariant = Color(0xFFB4B8C5),
    outline = Color(0xFF474B5D),
)

private val WizardLightColorScheme = lightColorScheme(
    primary = WizardTeal40,
    onPrimary = OnWizardTeal40,
    primaryContainer = Color(0xFFA7F0E0),
    onPrimaryContainer = Color(0xFF00201C),
    secondary = WizardTealGrey40,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFA7F0E8),
    onSecondaryContainer = Color(0xFF00201C),
    tertiary = WizardNeutral40,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE0E0E8),
    onTertiaryContainer = Color(0xFF1A1C2E),
    background = Color(0xFFF5F5F5),
    onBackground = Color(0xFF1A1C2E),
    surface = Color.White,
    onSurface = Color(0xFF1A1C2E),
    surfaceVariant = Color(0xFFE8E8EC),
    onSurfaceVariant = Color(0xFF474B5D),
    outline = Color(0xFFB4B8C5),
)

// Catppuccin scheme: fully specified, based on the official Mocha/Latte palettes.
private val CatppuccinDarkColorScheme = darkColorScheme(
    primary = CatppuccinMauve80,
    onPrimary = OnCatppuccinMauve80,
    primaryContainer = Color(0xFF4A2D7A),
    onPrimaryContainer = Color(0xFFE8D0FF),
    secondary = CatppuccinLavender80,
    onSecondary = Color(0xFF1C2848),
    secondaryContainer = Color(0xFF364278),
    onSecondaryContainer = Color(0xFFDEE0FF),
    tertiary = CatppuccinSky80,
    onTertiary = Color(0xFF003040),
    tertiaryContainer = Color(0xFF005068),
    onTertiaryContainer = Color(0xFFBBF0FF),
    background = Color(0xFF1E1E2E),
    onBackground = Color(0xFFCDD6F4),
    surface = Color(0xFF181825),
    onSurface = Color(0xFFCDD6F4),
    surfaceVariant = Color(0xFF313244),
    onSurfaceVariant = Color(0xFFBAC2DE),
    outline = Color(0xFF45475A),
)

private val CatppuccinLightColorScheme = lightColorScheme(
    primary = CatppuccinMauve40,
    onPrimary = OnCatppuccinMauve40,
    primaryContainer = Color(0xFFF0E0FF),
    onPrimaryContainer = Color(0xFF2D0070),
    secondary = CatppuccinLavender40,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDEE0FF),
    onSecondaryContainer = Color(0xFF1C2848),
    tertiary = CatppuccinSky40,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFBBF0FF),
    onTertiaryContainer = Color(0xFF003048),
    background = Color(0xFFEFF1F5),
    onBackground = Color(0xFF4C4F69),
    surface = Color(0xFFE6E9EF),
    onSurface = Color(0xFF4C4F69),
    surfaceVariant = Color(0xFFCCD0DA),
    onSurfaceVariant = Color(0xFF6C6F85),
    outline = Color(0xFF9CA0B0),
)

/**
 * How the dark/light scheme is chosen.
 *
 * - [Light]: always light.
 * - [Dark]: always dark.
 * - [Oled]: always dark, plus pure-black background/surface overrides
 *   for AMOLED displays.
 * - [System]: follow the system setting.
 */
enum class ThemeMode { Light, Dark, Oled, System }

enum class AppTheme(val labelRes: Int) {
    Hex(R.string.settings_app_theme_purple),
    Swamp(R.string.settings_app_theme_green),
    Wizard(R.string.settings_app_theme_wizard),
    Catppuccin(R.string.settings_app_theme_catppuccin),
    MaterialYou(R.string.settings_app_theme_material_you);

    @Composable
    fun colorScheme(darkTheme: Boolean): ColorScheme = when (this) {
        Hex -> if (darkTheme) HexDarkColorScheme else HexLightColorScheme
        Swamp -> if (darkTheme) SwampDarkColorScheme else SwampLightColorScheme
        Wizard -> if (darkTheme) WizardDarkColorScheme else WizardLightColorScheme
        Catppuccin -> if (darkTheme) CatppuccinDarkColorScheme else CatppuccinLightColorScheme
        MaterialYou -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
    }
}

@Composable
fun WheelWitchTheme(
    themeMode: ThemeMode = ThemeMode.System,
    appTheme: AppTheme = AppTheme.Hex,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.Light -> false
        ThemeMode.Dark, ThemeMode.Oled -> true
        ThemeMode.System -> isSystemInDarkTheme()
    }
    val colorScheme = appTheme.colorScheme(darkTheme).let { scheme ->
        if (themeMode == ThemeMode.Oled) scheme.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceVariant = OledSurfaceVariant,
            onBackground = OledOnBackground,
            onSurface = OledOnSurface,
            onSurfaceVariant = OledOnSurfaceVariant,
        ) else scheme
    }

    val statusColors = if (darkTheme) DarkStatusColors else LightStatusColors

    val context = LocalContext.current
    // Keep the Activity window background in sync with the theme so the status bar
    // area matches during recomposition and there is no white flash on theme change.
    SideEffect {
        (context as? Activity)?.window?.decorView?.setBackgroundColor(
            colorScheme.background.toArgb()
        )
    }

    CompositionLocalProvider(LocalStatusColors provides statusColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
