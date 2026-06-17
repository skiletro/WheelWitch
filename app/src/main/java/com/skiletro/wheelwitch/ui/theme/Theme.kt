package com.skiletro.wheelwitch.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
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

private val PurpleDarkColorScheme = darkColorScheme(
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

private val PurpleLightColorScheme = lightColorScheme(
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

enum class ThemeMode { Light, Dark, System }

enum class AppTheme { Hex, Swamp, MaterialYou }

@Composable
fun WheelWitchTheme(
    themeMode: ThemeMode = ThemeMode.System,
    appTheme: AppTheme = AppTheme.Hex,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
        ThemeMode.System -> isSystemInDarkTheme()
    }
    val colorScheme = when (appTheme) {
        AppTheme.Hex -> if (darkTheme) PurpleDarkColorScheme else PurpleLightColorScheme
        AppTheme.Swamp -> if (darkTheme) SwampDarkColorScheme else SwampLightColorScheme
        AppTheme.MaterialYou -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
    }

    val statusColors = if (darkTheme) DarkStatusColors else LightStatusColors

    val context = LocalContext.current
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
